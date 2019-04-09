/*
 * Copyright 2019 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dm.shakespeare.remote;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dm.shakespeare.AbstractStage;
import dm.shakespeare.BackStage;
import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Role;
import dm.shakespeare.concurrent.ExecutorServices;
import dm.shakespeare.log.LogPrinters;
import dm.shakespeare.log.Logger;
import dm.shakespeare.remote.protocol.ActorRef;
import dm.shakespeare.remote.protocol.CreateActorContinue;
import dm.shakespeare.remote.protocol.CreateActorRequest;
import dm.shakespeare.remote.protocol.CreateActorResponse;
import dm.shakespeare.remote.protocol.DescribeRequest;
import dm.shakespeare.remote.protocol.DescribeResponse;
import dm.shakespeare.remote.protocol.Disconnect;
import dm.shakespeare.remote.protocol.DismissActorRequest;
import dm.shakespeare.remote.protocol.Remote;
import dm.shakespeare.remote.protocol.RemoteMessage;
import dm.shakespeare.remote.util.ClassLoaderObjectInputStream;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 04/09/2019.
 */
public abstract class RemoteStage extends AbstractStage {

  private static final Object sMutex = new Object();

  private static Map<String, File> sClassFiles = Collections.emptyMap();

  private final ClassLoader mClassLoader;
  private final WeakHashMap<Actor, String> mHashes = new WeakHashMap<Actor, String>();
  private final Logger mLogger;
  private final Object mMutex = new Object();
  private final WeakHashMap<Actor, String> mSenders = new WeakHashMap<Actor, String>();
  private final HashSet<Integer> mTransferredHashes = new HashSet<Integer>();

  private Actor mActor;

  public RemoteStage(@NotNull final Logger logger) {
    mLogger = ConstantConditions.notNull("logger", logger);
    mClassLoader = null;
  }

  private static void registerFile(@NotNull final File root, @NotNull final File file,
      @NotNull final HashMap<String, File> fileMap) {
    if (file.isDirectory()) {
      final File[] files = file.listFiles();
      if (files != null) {
        for (final File child : files) {
          registerFile(root, child, fileMap);
        }
      }
    } else {
      fileMap.put(file.getPath().substring(root.getPath().length() + 1), file);
    }
  }

  private static void registerFiles() throws IOException {
    synchronized (sMutex) {
      if (sClassFiles.isEmpty()) {
        final HashMap<String, File> fileMap = new HashMap<String, File>();
        final Enumeration<URL> resources = RemoteStage.class.getClassLoader().getResources("");
        while (resources.hasMoreElements()) {
          final URL url = resources.nextElement();
          final File root = new File(url.getPath());
          registerFile(root, root, fileMap);
        }
        sClassFiles = fileMap;
      }
    }
  }

  public void start() {
    synchronized (mMutex) {
      if (mActor != null) {
        return;
      }

      mActor = BackStage.newActor(new Role() {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new Behavior() {

            public void onMessage(final Object message, @NotNull final Envelop envelop,
                @NotNull final Agent agent) throws Exception {
              if (message instanceof DescribeResponse) {
                final List<ActorRef> actors = ((DescribeResponse) message).getActors();
                if (actors != null) {
                  for (final ActorRef actor : actors) {
                    try {
                      final String hash = actor.getHash();
                      final Actor localActor = createActor(actor.getId(), new RemoteRole(hash));
                      mHashes.put(localActor, hash);

                    } catch (final Exception e) {
                      mLogger.err(e, "failed to create remote actor");
                    }
                  }
                }

              } else if (message instanceof DescribeRequest) {
                send(serialize(new DescribeResponse().withActors(null) // TODO: 09/04/2019 actors
                    .withCapabilities(getCapabilities())));

              } else if (message instanceof CreateActorRequest) {
                // TODO: 09/04/2019 check capabilities
                mHashes.put(envelop.getSender(),
                    ((CreateActorRequest) message).getActor().getHash());

              } else if (message instanceof CreateActorContinue) {
                // TODO: 09/04/2019 send classes

              } else if (message instanceof CreateActorResponse) {
                // TODO: 09/04/2019 check response

              } else if (message instanceof DismissActorRequest) {
                // TODO: 09/04/2019 check request

              } else if (message instanceof RemoteMessage) {
                // TODO: 09/04/2019 get sender + dispatch

              } else if (message instanceof RemoteEnvelop) {
                final RemoteEnvelop remoteEnvelop = (RemoteEnvelop) message;
                final Remote remoteMessage = remoteEnvelop.getRemoteMessage();
                if (remoteMessage instanceof RemoteMessage) {
                  final Actor sender = envelop.getSender();
                  final WeakHashMap<Actor, String> senders = mSenders;
                  String hash = senders.get(sender);
                  if (hash == null) {
                    hash = UUID.randomUUID().toString();
                    ((RemoteMessage) remoteMessage).setSenderId(hash);
                    senders.put(sender, hash);
                  }
                }
                send(serialize(remoteMessage));
              }
            }

            public void onStart(@NotNull final Agent agent) throws Exception {
              registerFiles();
              send(serialize(new DescribeRequest().withCapabilities(getCapabilities())));
            }

            public void onStop(@NotNull final Agent agent) throws Exception {
              agent.getExecutorService().shutdown();
              send(serialize(new Disconnect()));
            }
          };
        }

        @NotNull
        @Override
        public ExecutorService getExecutorService(@NotNull final String id) {
          return Executors.newSingleThreadExecutor();
        }
      });
    }
  }

  public void stop() {
    synchronized (mMutex) {
      if (mActor == null) {
        throw new IllegalStateException("not started");
      }
    }
    mActor.dismiss(false);
  }

  @NotNull
  @Override
  protected Actor createActor(@NotNull final String id, @NotNull final Role role) throws Exception {
    synchronized (mMutex) {
      if (mActor == null) {
        throw new IllegalStateException("not started");
      }
    }
    return BackStage.newActor(id, new RemoteLocalRole(role));
  }

  @NotNull
  protected Object deserialize(@NotNull final ClassLoader classLoader,
      @NotNull final byte[] data) throws Exception {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
    final ClassLoaderObjectInputStream objectInputStream =
        new ClassLoaderObjectInputStream(classLoader, inputStream);
    final Object object;
    try {
      object = objectInputStream.readObject();

    } finally {
      objectInputStream.close();
    }
    return object;
  }

  protected Map<String, String> getCapabilities() {
    return null;
  }

  @NotNull
  protected Map<String, File> getFiles() {
    return sClassFiles;
  }

  @NotNull
  protected Logger getLogger(@NotNull final String id) throws Exception {
    return Logger.newLogger(LogPrinters.javaLoggingPrinter(getClass().getName() + "." + id));
  }

  protected void receive(@NotNull final byte[] data) throws Exception {
    synchronized (mMutex) {
      if (mActor == null) {
        throw new IllegalStateException("not started");
      }
    }
    mActor.tell(deserialize(mClassLoader, data), null, BackStage.STAND_IN);
  }

  protected abstract void send(@NotNull byte[] data) throws Exception;

  @NotNull
  protected byte[] serialize(final Object object) throws Exception {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
    try {
      objectOutputStream.writeObject(object);

    } finally {
      objectOutputStream.close();
    }
    return outputStream.toByteArray();
  }

  private static class RemoteEnvelop {

    private Remote mRemoteMessage;

    private RemoteEnvelop(@NotNull final Remote remoteMessage) {
      mRemoteMessage = remoteMessage;
    }

    @NotNull
    Remote getRemoteMessage() {
      return mRemoteMessage;
    }
  }

  private class RemoteBehavior extends AbstractBehavior {

    private final ActorRef mActorRef;

    private RemoteBehavior(@NotNull final String id, @NotNull final String hash) {
      mActorRef = new ActorRef().withId(id).withHash(hash);
    }

    @Override
    public void onStop(@NotNull final Agent agent) throws Exception {
      if (agent.isDismissed()) {
        mActor.tell(new RemoteEnvelop(new DismissActorRequest().withActor(mActorRef)), null,
            BackStage.STAND_IN);
      }
    }

    @NotNull
    ActorRef getActorRef() {
      return mActorRef;
    }

    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) throws Exception {
      final Actor sender = envelop.getSender();
      if (sender.equals(mActor)) { // TODO: 09/04/2019 class
        // TODO: 09/04/2019 forward to recipient

      } else {
        mActor.tell(new RemoteEnvelop(new RemoteMessage().withActor(mActorRef)
            .withMessage(message)
            .withOptions(envelop.getOptions())
            .withSentTimestamp(envelop.getSentAt())), null, envelop.getSender());
      }
    }
  }

  private class RemoteLocalRole extends RemoteRole {

    private final byte[] mRoleData;

    private RemoteLocalRole(@NotNull final Role role) throws Exception {
      super(UUID.randomUUID().toString());
      mRoleData = serialize(role);
    }

    @NotNull
    public Behavior getBehavior(@NotNull final String id) {
      final RemoteBehavior behavior = getRemoteBehavior(id);
      return new Behavior() {

        public void onMessage(final Object message, @NotNull final Envelop envelop,
            @NotNull final Agent agent) throws Exception {
          behavior.onMessage(message, envelop, agent);
        }

        public void onStart(@NotNull final Agent agent) throws Exception {
          mActor.tell(new RemoteEnvelop(
                  new CreateActorRequest().withActor(behavior.getActorRef()).withRoleData(mRoleData)),
              null, BackStage.STAND_IN);
          behavior.onStart(agent);
        }

        public void onStop(@NotNull final Agent agent) throws Exception {
          behavior.onStop(agent);
        }
      };
    }

    @NotNull
    @Override
    public ExecutorService getExecutorService(@NotNull final String id) {
      return ExecutorServices.localExecutor();
    }

    @NotNull
    @Override
    public Logger getLogger(@NotNull final String id) throws Exception {
      return RemoteStage.this.getLogger(id);
    }
  }

  private class RemoteRole extends Role {

    private final String mHash;

    private RemoteRole(@NotNull final String hash) {
      mHash = hash;
    }

    @NotNull
    RemoteBehavior getRemoteBehavior(@NotNull final String id) {
      return new RemoteBehavior(id, mHash);
    }

    @NotNull
    public Behavior getBehavior(@NotNull final String id) {
      return getRemoteBehavior(id);
    }

    @NotNull
    @Override
    public ExecutorService getExecutorService(@NotNull final String id) {
      return ExecutorServices.localExecutor();
    }

    @NotNull
    @Override
    public Logger getLogger(@NotNull final String id) throws Exception {
      return RemoteStage.this.getLogger(id);
    }
  }
}
