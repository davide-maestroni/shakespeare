package dm.shakespeare;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Actor.ActorSet;
import dm.shakespeare.actor.Actor.Envelop;
import dm.shakespeare.actor.ActorBuilder;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.actor.BehaviorBuilder.Handler;
import dm.shakespeare.executor.ExecutorServices;
import dm.shakespeare.function.Mapper;
import dm.shakespeare.function.Provider;
import dm.shakespeare.function.Tester;
import dm.shakespeare.log.Logger;
import dm.shakespeare.remote.RemoteAdapter;
import dm.shakespeare.remote.RemoteAdapter.StageDescription;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 10/17/2018.
 */
class RemoteStage extends DefaultStage {

  private static final Tester<String> POSITIVE_TESTER = new Tester<String>() {

    public boolean test(final String value) {
      return true;
    }
  };
  private static final Object STOP_MESSAGE = new Object();

  private final RemoteAdapter mAdapter;
  private final ExecutorService mExecutor;
  private final HashMap<String, String> mHashes = new HashMap<String, String>();
  private final Object mMutex = new Object();
  private final HashMap<String, String> mPaths = new HashMap<String, String>();

  private Map<String, String> mCapabilities = Collections.emptyMap();

  RemoteStage(@NotNull final String name, @NotNull final ExecutorService executor,
      @NotNull final RemoteAdapter adapter) {
    super(name);
    mAdapter = ConstantConditions.notNull("adapter", adapter);
    mExecutor = ExecutorServices.withThrottling(1, executor);
    mExecutor.execute(new Runnable() {

      public void run() {
        try {
          final StageDescription description = adapter.describeStage();
          final Map<String, String> capabilities = description.getCapabilities();
          synchronized (mMutex) {
            if (capabilities != null) {
              mCapabilities = new HashMap<String, String>(capabilities);

            } else {
              mCapabilities = Collections.emptyMap();
            }
          }

          final Collection<String> actors = description.getActors();
          if (actors != null) {
            // stop removed actors
            final ActorSet actorSet = findAll(new ActorsTester(actors));
            actorSet.tell(STOP_MESSAGE, StandInActor.defaultInstance());

            for (final String actorId : actors) {
              try {
                createActor(actorId);

              } catch (final IllegalStateException ignored) {
                // ignored
              }
            }

          } else {
            // stop all actors
            final ActorSet actorSet = getAll();
            actorSet.tell(STOP_MESSAGE, StandInActor.defaultInstance());
          }

        } catch (final Exception ignored) {
          // ignored
        }
      }
    });
  }

  @NotNull
  public ActorBuilder newActor() {
    verifyCanCreate();
    return new RemoteActorBuilder();
  }

  @NotNull
  private Actor createActor(@Nullable final String actorId) {
    final ActorBuilder actorBuilder = RemoteStage.super.newActor();
    if (actorId != null) {
      actorBuilder.id(actorId);
    }

    return actorBuilder.behavior(new Provider<Behavior>() {

      public Behavior get() {
        return new DefaultBehaviorBuilder().onMessageEqualTo(STOP_MESSAGE, new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Context context) {
            context.stopSelf();
          }
        }).onNoMatch(new Handler<Object>() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final Context context) throws Exception {
            mAdapter.sendMessage(context.getSelf().getId(), message, envelop);
          }
        }).build();
      }
    }).executor(new Mapper<String, ExecutorService>() {

      public ExecutorService apply(final String value) {
        return mExecutor;
      }
    }).preventDefault(POSITIVE_TESTER).mayInterruptIfRunning(POSITIVE_TESTER).build();
    // TODO: 17/10/2018 interrupt???
  }

  private void verifyCanCreate() {
    synchronized (mMutex) {
      final String actorCreate = mCapabilities.get("actor.create");
      if (!"enabled".equalsIgnoreCase(actorCreate)) {
        throw new UnsupportedOperationException();
      }
    }
  }

  private static class ActorsTester implements Tester<Actor> {

    private final Collection<String> mActors;

    private ActorsTester(@NotNull final Collection<String> actors) {
      mActors = actors;
    }

    public boolean test(final Actor actor) {
      return !mActors.contains(actor.getId());
    }
  }

  private class RemoteActorBuilder implements ActorBuilder {

    private String mActorId;
    private Provider<? extends Behavior> mBehaviorProvider = DefaultStage.defaultBehaviorProvider();
    private Mapper<? super String, ? extends ExecutorService> mExecutorMapper =
        DefaultStage.defaultExecutorMapper();
    private Tester<? super String> mInterruptTester = DefaultStage.defaultInterruptTester();
    private Mapper<? super String, ? extends Logger> mLoggerMapper =
        DefaultStage.defaultLoggerMapper();
    private Tester<? super String> mPreventTester = DefaultStage.defaultPreventTester();
    private Mapper<? super String, ? extends Integer> mQuotaMapper =
        DefaultStage.defaultQuotaMapper();

    @NotNull
    public ActorBuilder behavior(@NotNull final Provider<? extends Behavior> provider) {
      mBehaviorProvider = ConstantConditions.notNull("provider", provider);
      return this;
    }

    @NotNull
    public Actor build() {
      boolean codeInjectionEnabled;
      synchronized (mMutex) {
        codeInjectionEnabled = "enabled".equalsIgnoreCase(mCapabilities.get("code.injection"));
      }

      final RemoteAdapter adapter = mAdapter;
      if (codeInjectionEnabled) {
        boolean needHashes;
        synchronized (mMutex) {
          needHashes = mHashes.isEmpty();
        }

        if (needHashes) {
          try {
            final Enumeration<URL> resources = RemoteStage.class.getClassLoader().getResources("");
            while (resources.hasMoreElements()) {
              final URL url = resources.nextElement();
              final File root = new File(url.getPath());
              addHash(root, root);
            }

          } catch (final IOException e) {
            throw new RuntimeException(e);

          } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
          }

          try {
            @SuppressWarnings("UnnecessaryLocalVariable") final HashMap<String, String> paths =
                mPaths;
            final File tempFile = File.createTempFile("shakespeare-", ".jar");
            tempFile.deleteOnExit();
            final Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            final JarOutputStream outputStream =
                new JarOutputStream(new FileOutputStream(tempFile), manifest);
            try {
              for (final String entry : adapter.getCodeEntries(mHashes)) {
                final String path = paths.get(entry);
                final File file = new File(path);
                final JarEntry jarEntry = new JarEntry(path.replace(File.pathSeparator, "/"));
                jarEntry.setTime(file.lastModified());
                outputStream.putNextEntry(jarEntry);
                BufferedInputStream bis = null;
                try {
                  bis = new BufferedInputStream(new FileInputStream(file));
                  final byte[] bytes = new byte[8 * 1024];
                  int b;
                  while ((b = bis.read(bytes)) > 0) {
                    outputStream.write(bytes, 0, b);
                  }

                  outputStream.closeEntry();

                } finally {
                  if (bis != null) {
                    bis.close();
                  }
                }
              }

            } finally {
              outputStream.close();
            }

            final FileInputStream inputStream = new FileInputStream(tempFile);
            try {
              adapter.sendCodeEntries(inputStream);

            } finally {
              inputStream.close();
            }

          } catch (final RuntimeException e) {
            throw e;

          } catch (final Exception e) {
            throw new RuntimeException(e);
          }
        }
      }

      final String actorId;
      try {
        // TODO: 30/10/2018 serialize
        actorId = adapter.createActor(mActorId, null);

      } catch (final RuntimeException e) {
        throw e;

      } catch (final Exception e) {
        throw new RuntimeException(e);
      }

      return createActor(actorId);
    }

    @NotNull
    public ActorBuilder executor(
        @NotNull final Mapper<? super String, ? extends ExecutorService> mapper) {
      mExecutorMapper = ConstantConditions.notNull("mapper", mapper);
      return this;
    }

    @NotNull
    public ActorBuilder id(@NotNull final String id) {
      mActorId = ConstantConditions.notNull("mapper", id);
      return this;
    }

    @NotNull
    public ActorBuilder logger(@NotNull final Mapper<? super String, ? extends Logger> mapper) {
      mLoggerMapper = ConstantConditions.notNull("mapper", mapper);
      return this;
    }

    @NotNull
    public ActorBuilder mayInterruptIfRunning(@NotNull final Tester<? super String> tester) {
      mInterruptTester = ConstantConditions.notNull("tester", tester);
      return this;
    }

    @NotNull
    public ActorBuilder preventDefault(@NotNull final Tester<? super String> tester) {
      mPreventTester = ConstantConditions.notNull("tester", tester);
      return this;
    }

    @NotNull
    public ActorBuilder quota(@NotNull final Mapper<? super String, ? extends Integer> mapper) {
      mQuotaMapper = ConstantConditions.notNull("mapper", mapper);
      return this;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private void addHash(@NotNull final File root, @NotNull final File file) throws IOException,
        NoSuchAlgorithmException {
      if (file.isDirectory()) {
        final File[] files = file.listFiles();
        if (files != null) {
          for (final File child : files) {
            addHash(root, child);
          }
        }

      } else {
        DigestInputStream dis = null;
        try {
          final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
          dis = new DigestInputStream(new FileInputStream(file), messageDigest);
          final byte[] bytes = new byte[8 * 1024];
          while (dis.read(bytes) > 0) {
          }

          final String className = file.getPath()
              .substring(root.getPath().length() + 1)
              .replaceAll(File.pathSeparator, ".");
          synchronized (mMutex) {
            mPaths.put(className, file.getPath());
            // TODO: 30/10/2018 base64 or hex
            mHashes.put(className, new String(messageDigest.digest()));
          }

        } finally {
          if (dis != null) {
            dis.close();
          }
        }
      }
    }
  }
}
