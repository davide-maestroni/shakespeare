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
import java.io.ObjectOutputStream;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
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
import dm.shakespeare.actor.ActorSet;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Options;
import dm.shakespeare.actor.Role;
import dm.shakespeare.concurrent.ExecutorServices;
import dm.shakespeare.log.LogMessage;
import dm.shakespeare.log.Logger;
import dm.shakespeare.message.Bounce;
import dm.shakespeare.message.Create;
import dm.shakespeare.message.Dismiss;
import dm.shakespeare.remote.protocol.ActorRef;
import dm.shakespeare.remote.protocol.AddObserverRequest;
import dm.shakespeare.remote.protocol.CreateActorContinue;
import dm.shakespeare.remote.protocol.CreateActorRequest;
import dm.shakespeare.remote.protocol.CreateActorResponse;
import dm.shakespeare.remote.protocol.DescribeRequest;
import dm.shakespeare.remote.protocol.DescribeResponse;
import dm.shakespeare.remote.protocol.DismissActorRequest;
import dm.shakespeare.remote.protocol.Remote;
import dm.shakespeare.remote.protocol.RemoteMessage;
import dm.shakespeare.remote.protocol.RemoveObserverRequest;
import dm.shakespeare.remote.util.ClassLoaderObjectInputStream;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 04/11/2019.
 */
public abstract class RemoteMasterStage extends AbstractStage {

  // TODO: 11/04/2019 addObserver, removeObserver

  private final WeakHashMap<Actor, String> mActorHashes = new WeakHashMap<Actor, String>();
  private final RemoteClassLoader mClassLoader;
  private final Logger mLogger;
  private final Object mMutex = new Object();
  private final HashMap<ActorRef, String> mObservers = new HashMap<ActorRef, String>();
  private final WeakHashMap<Actor, ActorRef> mSenders = new WeakHashMap<Actor, ActorRef>();

  private Actor mActor;
  private Options mOptions;

  public RemoteMasterStage(@NotNull final Logger logger) {
    this(logger, null);
  }

  public RemoteMasterStage(@NotNull final Logger logger,
      @Nullable final ProtectionDomain protectionDomain) {
    mLogger = ConstantConditions.notNull("logger", logger);
    mClassLoader = new RemoteClassLoader(getClass().getClassLoader(), protectionDomain);
  }

  public final void start() {
    synchronized (mMutex) {
      if (mActor != null) {
        return;
      }
      final String actorId = UUID.randomUUID().toString();
      mOptions = new Options().withReceiptId(actorId);
      mActor = BackStage.newActor(actorId, new Role() {

        @NotNull
        @Override
        public Behavior getBehavior(@NotNull final String id) {
          return new Behavior() {

            public void onMessage(final Object message, @NotNull final Envelop envelop,
                @NotNull final Agent agent) {
              if (message instanceof DescribeRequest) {
                final WeakHashMap<Actor, String> actorHashes = mActorHashes;
                final ArrayList<ActorRef> actorRefs = new ArrayList<ActorRef>();
                final ActorSet actors = getAll();
                for (final Actor actor : actors) {
                  actorRefs.add(
                      new ActorRef().withId(actor.getId()).withHash(actorHashes.get(actor)));
                }
                safeSend(new DescribeResponse().withCapabilities(getCapabilities())
                    .withActors(actorRefs), ((DescribeRequest) message).getSenderId());

              } else if (message instanceof AddObserverRequest) {
                final AddObserverRequest observerRequest = (AddObserverRequest) message;
                mObservers.put(observerRequest.getActorRef(), observerRequest.getSenderId());

              } else if (message instanceof RemoveObserverRequest) {
                final RemoveObserverRequest observerRequest = (RemoveObserverRequest) message;
                mObservers.remove(observerRequest.getActorRef());

              } else if (message instanceof CreateActorRequest) {
                final CreateActorRequest createRequest = (CreateActorRequest) message;
                final ActorRef actorRef = createRequest.getActorRef();
                final Map<String, String> capabilities = getCapabilities();
                if ((capabilities != null) && Boolean.TRUE.toString()
                    .equalsIgnoreCase(capabilities.get(Capabilities.CREATE_REMOTE))) {
                  final byte[] roleData = createRequest.getRoleData();
                  try {
                    final Object role = deserialize(roleData);
                    if ((actorRef == null) || !(role instanceof Role)) {
                      safeSend(new CreateActorResponse().withActorRef(actorRef)
                          .withError(new IllegalStateException()), createRequest.getSenderId());

                    } else {
                      newActor(actorRef.getId(), (Role) role);
                      safeSend(new CreateActorResponse().withActorRef(actorRef),
                          createRequest.getSenderId());
                    }

                  } catch (final RemoteClassNotFoundException e) {
                    if (Boolean.TRUE.toString()
                        .equalsIgnoreCase(capabilities.get(Capabilities.LOAD_REMOTE))) {
                      safeSend(new CreateActorContinue().withActorRef(actorRef)
                          .withRoleData(roleData)
                          .addClassPaths(e.getMessage()), createRequest.getSenderId());

                    } else {
                      safeSend(new CreateActorResponse().withActorRef(actorRef).withError(e),
                          createRequest.getSenderId());
                    }

                  } catch (final Exception e) {
                    safeSend(new CreateActorResponse().withActorRef(actorRef).withError(e),
                        createRequest.getSenderId());
                  }

                } else {
                  safeSend(new CreateActorResponse().withActorRef(actorRef)
                          .withError(new UnsupportedOperationException(Capabilities.CREATE_REMOTE)),
                      createRequest.getSenderId());
                }

              } else if (message instanceof DismissActorRequest) {
                final Map<String, String> capabilities = getCapabilities();
                if ((capabilities != null) && Boolean.TRUE.toString()
                    .equalsIgnoreCase(capabilities.get(Capabilities.DISMISS_REMOTE))) {
                  final DismissActorRequest dismissRequest = (DismissActorRequest) message;
                  final ActorRef actorRef = dismissRequest.getActorRef();
                  if (actorRef != null) {
                    final Actor actor = get(actorRef.getId());
                    final String hash = mActorHashes.get(actor);
                    if ((hash != null) && hash.equals(actorRef.getHash())) {
                      actor.dismiss(false);
                    }
                  }
                }

              } else if (message instanceof RemoteMessage) {
                final RemoteMessage remoteMessage = (RemoteMessage) message;
                final ActorRef actorRef = remoteMessage.getActorRef();
                if (actorRef != null) {
                  try {
                    final Actor actor = get(actorRef.getId());
                    final String hash = mActorHashes.get(actor);
                    if ((hash != null) && hash.equals(actorRef.getHash())) {
                      final Actor sender = getOrCreateSender(remoteMessage.getSenderRef(),
                          remoteMessage.getSenderId());
                      final long offset =
                          remoteMessage.getSentTimestamp() - System.currentTimeMillis();
                      Options options = remoteMessage.getOptions();
                      if (options != null) {
                        options = options.withTimeOffset(options.getTimeOffset() + offset);

                      } else {
                        options = new Options().withTimeOffset(offset);
                      }
                      actor.tell(remoteMessage.getMessage(), options, sender);

                    } else {
                      // TODO: 09/04/2019 send Bounce
                    }

                  } catch (final IllegalArgumentException e) {
                    // TODO: 09/04/2019 send Bounce
                  }

                } else {
                  // TODO: 09/04/2019 send Bounce
                }

              } else if (message instanceof Create) {
                final String hash = UUID.randomUUID().toString();
                mActorHashes.put(envelop.getSender(), hash);
                final RemoteMessage remoteMessage = new RemoteMessage().withMessage(CREATE)
                    .withSenderRef(
                        new ActorRef().withId(envelop.getSender().getId()).withHash(hash));
                for (final Entry<ActorRef, String> entry : mObservers.entrySet()) {
                  safeSend(remoteMessage.withActorRef(entry.getKey()), entry.getValue());
                }

              } else if (message instanceof Dismiss) {
                final String hash = mActorHashes.remove(envelop.getSender());
                final RemoteMessage remoteMessage = new RemoteMessage().withMessage(DISMISS)
                    .withSenderRef(
                        new ActorRef().withId(envelop.getSender().getId()).withHash(hash));
                for (final Entry<ActorRef, String> entry : mObservers.entrySet()) {
                  safeSend(remoteMessage.withActorRef(entry.getKey()), entry.getValue());
                }

              } else if (message instanceof RemoteResponse) {
                final RemoteResponse response = (RemoteResponse) message;
                final Envelop env = response.getEnvelop();
                final Actor sender = env.getSender();
                final ActorRef actorRef = mSenders.get(envelop.getSender());
                final String hash = mActorHashes.get(sender);
                safeSend(new RemoteMessage().withActorRef(actorRef)
                    .withMessage(response.getMessage())
                    .withOptions(env.getOptions())
                    .withSenderRef(new ActorRef().withId(sender.getId()).withHash(hash))
                    .withSentTimestamp(env.getSentAt()), response.getRecipientId());
              }
            }

            public void onStart(@NotNull final Agent agent) throws Exception {
              RemoteMasterStage.this.onStart();
            }

            public void onStop(@NotNull final Agent agent) throws Exception {
              agent.getExecutorService().shutdown();
              RemoteMasterStage.this.onStop();
            }
          };
        }

        @NotNull
        @Override
        public ExecutorService getExecutorService(@NotNull final String id) {
          return Executors.newSingleThreadExecutor();
        }
      });
      addObserver(mActor);
    }
  }

  public final void stop() {
    synchronized (mMutex) {
      if (mActor == null) {
        throw new IllegalStateException("not started");
      }
    }
    mActor.dismiss(false);
  }

  @NotNull
  protected Actor createActor(@NotNull final String id, @NotNull final Role role) throws Exception {
    synchronized (mMutex) {
      if (mActor == null) {
        throw new IllegalStateException("not started");
      }
    }
    return BackStage.newActor(id, role);
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
  protected Logger getLogger(@NotNull final String id) throws Exception {
    return mLogger;
  }

  protected void onStart() throws Exception {
    // do nothing
  }

  protected void onStop() throws Exception {
    // do nothing
  }

  protected void receiveData(@NotNull final byte[] data) throws Exception {
    synchronized (mMutex) {
      if (mActor == null) {
        throw new IllegalStateException("not started");
      }
    }
    // TODO: 11/04/2019 actor managing bounces
    mActor.tell(deserialize(data), mOptions, BackStage.STAND_IN);
  }

  protected abstract void sendData(@NotNull byte[] data, @NotNull final String recipientId) throws
      Exception;

  protected void sendRemote(@NotNull final Remote remote, @NotNull final String recipientId) throws
      Exception {
    sendData(serialize(remote), recipientId);
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

  @NotNull
  private Actor getOrCreateSender(final ActorRef senderRef, final String senderId) {
    Actor sender = null;
    final WeakHashMap<Actor, ActorRef> senders = mSenders;
    for (final Entry<Actor, ActorRef> entry : senders.entrySet()) {
      if (entry.getValue().equals(senderRef)) {
        sender = entry.getKey();
        break;
      }
    }

    if (sender == null) {
      sender = BackStage.newActor(senderRef.getId(), new RemoteSenderRole(senderId));
      senders.put(sender, senderRef);
    }
    return sender;
  }

  private void safeSend(@NotNull final Remote remote, @NotNull final String recipientId) {
    try {
      sendRemote(remote, recipientId);

    } catch (final Throwable t) {
      mLogger.err(t, "failed to send remote message: %s to %s",
          LogMessage.abbreviate(remote.toString(), 2048), recipientId);
    }
  }

  private static class RemoteResponse {

    private final Envelop mEnvelop;
    private final Object mMessage;
    private final String mRecipientId;

    private RemoteResponse(final String recipientId, final Object message,
        @NotNull final Envelop envelop) {
      mRecipientId = recipientId;
      mMessage = message;
      mEnvelop = envelop;
    }

    @NotNull
    Envelop getEnvelop() {
      return mEnvelop;
    }

    Object getMessage() {
      return mMessage;
    }

    String getRecipientId() {
      return mRecipientId;
    }
  }

  private class RemoteSenderRole extends Role {

    private final String mRecipientId;

    private RemoteSenderRole(final String recipientId) {
      mRecipientId = recipientId;
    }

    @NotNull
    public Behavior getBehavior(@NotNull final String id) {
      final Options options = new Options().withReceiptId(id);
      return new AbstractBehavior() {

        public void onMessage(final Object message, @NotNull final Envelop envelop,
            @NotNull final Agent agent) {
          final Actor actor = mActor;
          if (actor.equals(envelop.getSender())) {
            if (message instanceof Bounce) {
              agent.dismissSelf();
            }

          } else {
            actor.tell(new RemoteResponse(mRecipientId, message, envelop), options,
                agent.getSelf());
          }
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
      return RemoteMasterStage.this.getLogger(id);
    }
  }
}
