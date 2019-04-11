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
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import dm.shakespeare.actor.Options;
import dm.shakespeare.actor.Role;
import dm.shakespeare.concurrent.ExecutorServices;
import dm.shakespeare.log.LogPrinters;
import dm.shakespeare.log.Logger;
import dm.shakespeare.message.DeadLetter;
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

  // TODO: 10/04/2019 master/slave
  // TODO: 10/04/2019 role transfer
  // TODO: 10/04/2019 actors sync

  private static final Object sMutex = new Object();

  private static Map<String, File> sClassFiles = Collections.emptyMap();

  private final RemoteClassLoader mClassLoader;
  private final WeakHashMap<Actor, String> mHashes = new WeakHashMap<Actor, String>();
  private final Logger mLogger;
  private final Object mMutex = new Object();
  private final WeakHashMap<Actor, String> mSenders = new WeakHashMap<Actor, String>();

  private Actor mActor;

  public RemoteStage(@NotNull final Logger logger) {
    this(logger, null);
  }

  public RemoteStage(@NotNull final Logger logger,
      @Nullable final ProtectionDomain protectionDomain) {
    mLogger = ConstantConditions.notNull("logger", logger);
    mClassLoader = new RemoteClassLoader(getClass().getClassLoader(), protectionDomain);
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
                      final Actor localActor = newActor(actor.getId(), new RemoteRole(hash));
                      mHashes.put(localActor, hash);

                    } catch (final Exception e) {
                      mLogger.err(e, "failed to create remote actor");
                    }
                  }
                }

              } else if (message instanceof DescribeRequest) {
                sendRemote(new DescribeResponse().withActors(null) // TODO: 09/04/2019 actors
                        .withCapabilities(getCapabilities()),
                    ((DescribeRequest) message).getSenderId());

              } else if (message instanceof CreateActorRequest) {
                // TODO: 09/04/2019 check capabilities
                final CreateActorRequest createRequest = (CreateActorRequest) message;
                final ActorRef actorRef = createRequest.getActor();
                final byte[] roleData = createRequest.getRoleData();
                try {
                  final Object role = deserialize(roleData);
                  if (!(role instanceof Role)) {
                    sendRemote(new CreateActorResponse().withActor(actorRef)
                        .withError(new IllegalStateException()), createRequest.getSenderId());

                  } else {
                    final Actor actor = newActor(actorRef.getId(), (Role) role);
                    mHashes.put(actor, actorRef.getHash());
                    actor.addObserver(agent.getSelf());
                    sendRemote(new CreateActorResponse().withActor(actorRef),
                        createRequest.getSenderId());
                  }

                } catch (final RemoteClassNotFoundException e) {
                  // TODO: 09/04/2019 check capabilities
                  sendRemote(
                      new CreateActorContinue().withActor(actorRef).addClassPaths(e.getMessage()),
                      createRequest.getSenderId());

                } catch (final Exception e) {
                  sendRemote(new CreateActorResponse().withActor(actorRef).withError(e),
                      createRequest.getSenderId());
                }

              } else if (message instanceof CreateActorContinue) {
                // TODO: 09/04/2019 check capabilities
                // TODO: 09/04/2019 send classes

              } else if (message instanceof CreateActorResponse) {
                // TODO: 09/04/2019 check response

              } else if (message instanceof DismissActorRequest) {
                // TODO: 09/04/2019 check capabilities
                final DismissActorRequest dismissRequest = (DismissActorRequest) message;
                final ActorRef actorRef = dismissRequest.getActor();
                if (actorRef != null) {
                  final Actor actor = get(actorRef.getId());
                  final String hash = mHashes.get(actor);
                  if ((hash != null) && hash.equals(actorRef.getHash())) {
                    actor.dismiss(false);
                  }
                }

              } else if (message instanceof RemoteMessage) {
                final RemoteMessage remoteMessage = (RemoteMessage) message;
                final ActorRef actorRef = remoteMessage.getActor();
                if (actorRef != null) {
                  try {
                    final Actor actor = get(actorRef.getId());
                    final String hash = mHashes.get(actor);
                    if ((hash != null) && hash.equals(actorRef.getHash())) {
                      Actor sender = null;
                      final String senderId = remoteMessage.getSenderId();
                      for (final Entry<Actor, String> entry : mSenders.entrySet()) {
                        if (entry.getValue().equals(senderId)) {
                          sender = entry.getKey();
                          break;
                        }
                      }

                      if (sender != null) {
                        actor.tell(new RemoteResponse(sender, remoteMessage.getMessage(),
                                remoteMessage.getOptions(), remoteMessage.getSentTimestamp()), null,
                            agent.getSelf());

                      } else {
                        // TODO: 09/04/2019 send Bounce
                      }

                    } else {
                      // TODO: 09/04/2019 send Bounce
                    }

                  } catch (final IllegalArgumentException e) {
                    // TODO: 09/04/2019 send Bounce
                  }

                } else {
                  // TODO: 09/04/2019 send Bounce
                }

              } else if (message instanceof RemoteEnvelop) {
                final RemoteEnvelop remoteEnvelop = (RemoteEnvelop) message;
                final Remote remoteMessage = remoteEnvelop.getRemoteMessage();
                if (remoteMessage instanceof RemoteMessage) {
                  final Actor sender = envelop.getSender();
                  final WeakHashMap<Actor, String> senders = mSenders;
                  String hash = senders.get(sender);
                  if (hash == null) {
                    hash = UUID.randomUUID().toString();
                    ((RemoteMessage) remoteMessage).setSenderId(hash); // TODO: 09/04/2019 ???
                    senders.put(sender, hash);
                  }
                }
                sendRemote(remoteMessage, null);

              } else if (message instanceof DeadLetter) {
                final Actor sender = envelop.getSender();
                final String hash = mHashes.get(sender);
                sendRemote(new DismissActorRequest().withActor(
                    new ActorRef().withId(sender.getId()).withHash(hash)), null);
              }

              // TODO: 09/04/2019 error handling
            }

            public void onStart(@NotNull final Agent agent) throws Exception {
              registerFiles();
              sendRemote(new DescribeRequest().withCapabilities(getCapabilities()), null);
            }

            public void onStop(@NotNull final Agent agent) throws Exception {
              agent.getExecutorService().shutdown();
              sendRemote(new Disconnect(), null);
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
    // TODO: 09/04/2019 check capabilities
    return BackStage.newActor(id, new RemoteLocalRole(role));
  }

  protected Object deserialize(@NotNull final byte[] data) throws Exception {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
    final ClassLoaderObjectInputStream objectInputStream =
        new ClassLoaderObjectInputStream(getClassLoader(), inputStream);
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
  protected ClassLoader getClassLoader() {
    return mClassLoader;
  }

  @NotNull
  protected Map<String, File> getFiles() {
    return sClassFiles;
  }

  @NotNull
  protected Logger getLogger(@NotNull final String id) throws Exception {
    return Logger.newLogger(LogPrinters.javaLoggingPrinter(getClass().getName() + "." + id));
  }

  protected void receiveData(@NotNull final byte[] data) throws Exception {
    synchronized (mMutex) {
      if (mActor == null) {
        throw new IllegalStateException("not started");
      }
    }
    mActor.tell(deserialize(data), null, BackStage.STAND_IN);
  }

  protected abstract void sendData(@NotNull byte[] data, final URI uri) throws Exception;

  protected void sendRemote(@NotNull final Remote remote, final URI uri) throws Exception {
    sendData(serialize(remote), uri);
  }

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

  private static class RemoteResponse {

    private final Object mMessage;
    private final Options mOptions;
    private final Actor mRecipient;
    private final long mTimestamp;

    private RemoteResponse(@NotNull final Actor recipient, final Object message,
        final Options options, final long timestamp) {
      mRecipient = recipient;
      mMessage = message;
      mOptions = options;
      mTimestamp = timestamp;
    }

    Object getMessage() {
      return mMessage;
    }

    Options getOptions() {
      return mOptions;
    }

    @NotNull
    Actor getRecipient() {
      return mRecipient;
    }

    long getTimestamp() {
      return mTimestamp;
    }
  }

  private class RemoteBehavior extends AbstractBehavior {

    private final ActorRef mActorRef;

    private RemoteBehavior(@NotNull final String id, @NotNull final String hash) {
      mActorRef = new ActorRef().withId(id).withHash(hash);
    }

    @Override
    public void onStop(@NotNull final Agent agent) {
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
        @NotNull final Agent agent) {
      if (message instanceof RemoteResponse) {
        final RemoteResponse response = (RemoteResponse) message;
        final long offset = response.getTimestamp() - System.currentTimeMillis();
        Options options = response.getOptions();
        if (options != null) {
          options = options.withTimeOffset(options.getTimeOffset() + offset);

        } else {
          options = new Options().withTimeOffset(offset);
        }
        response.getRecipient().tell(response.getMessage(), options, agent.getSelf());

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
            @NotNull final Agent agent) {
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
