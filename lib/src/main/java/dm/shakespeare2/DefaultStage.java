package dm.shakespeare2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.actor.Actor.ActorSet;
import dm.shakespeare2.actor.Actor.Envelop;
import dm.shakespeare2.actor.ActorBuilder;
import dm.shakespeare2.actor.Behavior;
import dm.shakespeare2.actor.Stage;
import dm.shakespeare2.executor.ExecutorServices;
import dm.shakespeare2.function.Mapper;
import dm.shakespeare2.function.Provider;
import dm.shakespeare2.function.Tester;
import dm.shakespeare2.log.LogPrinters;
import dm.shakespeare2.log.Logger;
import dm.shakespeare2.util.ConstantConditions;

/**
 * Created by davide-maestroni on 09/12/2018.
 */
class DefaultStage implements Stage {

  private static final Behavior sDefaultBehavior = new Behavior() {

    public void message(final Object message, @NotNull final Envelop envelop,
        @NotNull final Context context) {
    }

    public void start(@NotNull final Context context) {
    }

    public void stop(@NotNull final Context context) {
    }
  };
  private static final Provider<? extends Behavior> sDefaultBehaviorProvider =
      new Provider<Behavior>() {

        public Behavior get() {
          return sDefaultBehavior;
        }
      };
  private static final ScheduledExecutorService sDefaultExecutor =
      ExecutorServices.newDynamicScheduledThreadPool(new ThreadFactory() {

        private final AtomicLong mCount = new AtomicLong();

        public Thread newThread(@NotNull final Runnable runnable) {
          return new Thread(runnable, "shakespeare-thread-" + mCount.getAndIncrement());
        }
      });
  private static final Mapper<? super String, ? extends ExecutorService> sDefaultExecutorMapper =
      new Mapper<String, ExecutorService>() {

        public ExecutorService apply(final String value) {
          return sDefaultExecutor;
        }
      };
  private static final Mapper<? super String, ? extends Logger> sDefaultLoggerMapper =
      new Mapper<String, Logger>() {

        public Logger apply(final String value) {
          return Logger.newLogger(LogPrinters.javaLoggingPrinter(value));
        }
      };

  private final Map<String, Actor> mActors;
  private final Object mMutex = new Object();
  private final String mName;
  private final StageNotifier mStageNotifier;

  private long mCount = Long.MIN_VALUE;

  DefaultStage(@NotNull final String name) {
    this(name, new HashMap<String, Actor>());
  }

  DefaultStage(@NotNull final String name, @NotNull final Map<String, Actor> actorMap) {
    mName = ConstantConditions.notNull("name", name);
    mActors = ConstantConditions.notNull("actorMap", actorMap);
    mStageNotifier = new StageNotifier(name);
  }

  @NotNull
  static Provider<? extends Behavior> defaultBehaviorProvider() {
    return sDefaultBehaviorProvider;
  }

  @NotNull
  static Mapper<? super String, ? extends ExecutorService> defaultExecutorMapper() {
    return sDefaultExecutorMapper;
  }

  @NotNull
  static Mapper<? super String, ? extends Logger> defaultLoggerMapper() {
    return sDefaultLoggerMapper;
  }

  public void addMonitor(@NotNull final Actor monitor) {
    mStageNotifier.addMonitor(monitor);
  }

  @NotNull
  public ActorSet findAll(@NotNull final Pattern idPattern) {
    return findAll(new PatternTester(ConstantConditions.notNull("idPattern", idPattern)));
  }

  @NotNull
  public ActorSet findAll(@NotNull final Tester<? super Actor> tester) {
    ConstantConditions.notNull("tester", tester);
    final HashSet<Actor> actors;
    synchronized (mMutex) {
      actors = new HashSet<Actor>(mActors.values());
    }

    try {
      final Iterator<Actor> iterator = actors.iterator();
      while (iterator.hasNext()) {
        final Actor actor = iterator.next();
        if ((actor != null) && !tester.test(actor)) {
          iterator.remove();
        }
      }

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }

    return new DefaultActorSet(actors);
  }

  @NotNull
  public Actor findAny(@NotNull final Pattern idPattern) {
    return findAny(new PatternTester(ConstantConditions.notNull("idPattern", idPattern)));
  }

  @NotNull
  public Actor findAny(@NotNull final Tester<? super Actor> tester) {
    ConstantConditions.notNull("tester", tester);
    final ArrayList<Actor> actors;
    synchronized (mMutex) {
      actors = new ArrayList<Actor>(mActors.values());
    }

    try {
      for (final Actor actor : actors) {
        if ((actor != null) && tester.test(actor)) {
          return actor;
        }
      }

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }

    throw new IllegalArgumentException("cannot find an actor satisfying the tester: " + tester);
  }

  @NotNull
  public Actor get(@NotNull final String id) {
    ConstantConditions.notNull("id", id);
    final Actor actor;
    synchronized (mMutex) {
      actor = mActors.get(id);
    }

    if (actor == null) {
      throw new IllegalArgumentException("cannot find an actor with ID: " + id);
    }

    return actor;
  }

  @NotNull
  public String getName() {
    return mName;
  }

  @NotNull
  public Actor getOrCreate(@NotNull final String id,
      @NotNull final Mapper<? super ActorBuilder, ? extends Actor> mapper) {
    ConstantConditions.notNull("id", id);
    ConstantConditions.notNull("mapper", mapper);
    final Map<String, Actor> actors = mActors;
    synchronized (mMutex) {
      Actor actor = actors.get(id);
      if (actor != null) {
        return actor;
      }

      if (actors.containsKey(id)) {
        try {
          do {
            mMutex.wait();
            actor = actors.get(id);
          } while ((actor == null) && actors.containsKey(id));

        } catch (final InterruptedException e) {
          throw new RuntimeException(e);
        }

        if (actor != null) {
          mMutex.notifyAll();
          return actor;
        }

        actors.put(id, null);

      } else {
        actors.put(id, null);
      }
    }

    final Actor actor;
    try {
      actor = mapper.apply(new DefaultActorBuilder(id));

    } catch (final RuntimeException e) {
      final Actor removed;
      synchronized (mMutex) {
        removed = actors.remove(id);
        mMutex.notifyAll();
      }

      if (removed != null) {
        mStageNotifier.remove(id);
      }

      throw e;

    } catch (final Exception e) {
      final Actor removed;
      synchronized (mMutex) {
        removed = actors.remove(id);
        mMutex.notifyAll();
      }

      if (removed != null) {
        mStageNotifier.remove(id);
      }

      throw new RuntimeException(e);
    }

    final Actor old;
    synchronized (mMutex) {
      old = actors.put(id, actor);
      mMutex.notifyAll();
    }

    if (old == null) {
      mStageNotifier.create(id);
    }

    return actor;
  }

  @NotNull
  public ActorBuilder newActor() {
    return new DefaultActorBuilder(null);
  }

  public void removeMonitor(@NotNull final Actor monitor) {
    mStageNotifier.removeMonitor(monitor);
  }

  void removeActor(@NotNull final String id) {
    final Actor removed;
    synchronized (mMutex) {
      removed = mActors.remove(id);
    }

    if (removed != null) {
      mStageNotifier.remove(id);
    }
  }

  private static class PatternTester implements Tester<Actor> {

    private final Pattern mPattern;

    PatternTester(@NotNull final Pattern idPattern) {
      mPattern = idPattern;
    }

    public boolean test(final Actor actor) {
      return mPattern.matcher(actor.getId()).matches();
    }
  }

  private class DefaultActorBuilder implements ActorBuilder {

    private final boolean mAssignedId;

    private String mActorId;
    private Provider<? extends Behavior> mBehaviorProvider = sDefaultBehaviorProvider;
    private Mapper<? super String, ? extends ExecutorService> mExecutorMapper =
        sDefaultExecutorMapper;
    private Mapper<? super String, ? extends Logger> mLoggerMapper = sDefaultLoggerMapper;
    private boolean mMayInterruptIfRunning;
    private boolean mPreventDefault;
    private int mQuota = Integer.MAX_VALUE;

    private DefaultActorBuilder(@Nullable final String id) {
      mActorId = id;
      mAssignedId = (id != null);
    }

    @NotNull
    public ActorBuilder behavior(@NotNull final Provider<? extends Behavior> provider) {
      mBehaviorProvider = ConstantConditions.notNull("provider", provider);
      return this;
    }

    @NotNull
    public Actor build() {
      final Map<String, Actor> actors = mActors;
      String actorId;
      synchronized (mMutex) {
        actorId = mActorId;
        if (actorId == null) {
          do {
            actorId = Actor.class.getName() + "#" + mCount++;
          } while (actors.containsKey(actorId));

        } else if (!mAssignedId && actors.containsKey(actorId)) {
          throw new IllegalStateException("an actor with the same ID already exists: " + actorId);
        }

        actors.put(actorId, null);
      }

      final DefaultActor actor;
      final DefaultContext context;
      try {
        final ExecutorService executor = mExecutorMapper.apply(actorId);
        final Logger logger = mLoggerMapper.apply(actorId);
        actor = new DefaultActor(actorId);
        context =
            new DefaultContext(DefaultStage.this, actor, mBehaviorProvider, mMayInterruptIfRunning,
                mPreventDefault, mQuota, executor, logger);
        actor.setContext(context);
        context.start();

      } catch (final RuntimeException e) {
        final Actor removed;
        synchronized (mMutex) {
          removed = actors.remove(actorId);
          mMutex.notifyAll();
        }

        if (removed != null) {
          mStageNotifier.remove(actorId);
        }

        throw e;

      } catch (final Exception e) {
        final Actor removed;
        synchronized (mMutex) {
          removed = actors.remove(actorId);
          mMutex.notifyAll();
        }

        if (removed != null) {
          mStageNotifier.remove(actorId);
        }

        throw new RuntimeException(e);
      }

      final Actor old;
      synchronized (mMutex) {
        old = actors.put(actorId, actor);
        mMutex.notifyAll();
      }

      if (old == null) {
        mStageNotifier.create(actorId);
      }

      return actor;
    }

    @NotNull
    public ActorBuilder executor(
        @NotNull final Mapper<? super String, ? extends ExecutorService> mapper) {
      mExecutorMapper = ConstantConditions.notNull("mapper", mapper);
      return this;
    }

    @NotNull
    public ActorBuilder id(@NotNull final String id) {
      if (mAssignedId) {
        throw new IllegalStateException("cannot modify the actor ID");
      }

      mActorId = ConstantConditions.notNull("mapper", id);
      return this;
    }

    @NotNull
    public ActorBuilder logger(@NotNull final Mapper<? super String, ? extends Logger> mapper) {
      mLoggerMapper = ConstantConditions.notNull("mapper", mapper);
      return this;
    }

    @NotNull
    public ActorBuilder mayInterruptIfRunning(final boolean interruptIfRunning) {
      mMayInterruptIfRunning = interruptIfRunning;
      return this;
    }

    @NotNull
    public ActorBuilder preventDefault(final boolean prevent) {
      mPreventDefault = prevent;
      return this;
    }

    @NotNull
    public ActorBuilder quota(final int quota) {
      mQuota = ConstantConditions.positive("quota", quota);
      return this;
    }
  }
}
