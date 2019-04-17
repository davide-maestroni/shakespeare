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

import java.util.ArrayList;
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
import dm.shakespeare.log.LogPrinters;
import dm.shakespeare.log.Logger;
import dm.shakespeare.message.Bounce;
import dm.shakespeare.message.Create;
import dm.shakespeare.message.Dismiss;
import dm.shakespeare.remote.Connector.Receiver;
import dm.shakespeare.remote.Connector.Sender;
import dm.shakespeare.remote.config.Capabilities;
import dm.shakespeare.remote.config.RemoteConfig;
import dm.shakespeare.remote.protocol.ActorRef;
import dm.shakespeare.remote.protocol.CreateActorContinue;
import dm.shakespeare.remote.protocol.CreateActorRequest;
import dm.shakespeare.remote.protocol.CreateActorResponse;
import dm.shakespeare.remote.protocol.DescribeRequest;
import dm.shakespeare.remote.protocol.DescribeResponse;
import dm.shakespeare.remote.protocol.DismissActorRequest;
import dm.shakespeare.remote.protocol.Remote;
import dm.shakespeare.remote.protocol.RemoteBounce;
import dm.shakespeare.remote.protocol.RemoteMessage;
import dm.shakespeare.remote.util.SerializableData;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 04/11/2019.
 */
public abstract class RemoteServer extends AbstractStage {

  // TODO: 11/04/2019 addObserver, removeObserver (???)
  // TODO: 16/04/2019 RemoteMessage body serialize/load
  // TODO: 16/04/2019 RemoteStage.start(RemoteServer)

  private final WeakHashMap<Actor, String> mActorHashes = new WeakHashMap<Actor, String>();
  private final RemoteClassLoader mClassLoader;
  private final Connector mConnector;
  private final Logger mLogger;
  private final Object mMutex = new Object();
  private final WeakHashMap<Actor, ActorRef> mSenders = new WeakHashMap<Actor, ActorRef>();
  private final Serializer mSerializer;

  private Actor mActor;
  private Options mOptions;
  private Sender mSender;

  public RemoteServer(@NotNull final RemoteConfig config) {
    mConnector = ConstantConditions.notNull("connector", config.getConnector());
    mClassLoader = new RemoteClassLoader(getClass().getClassLoader(), config.getProtectionDomain());
    final Serializer serializer = config.getSerializer();
    mSerializer = (serializer != null) ? serializer : new JavaSerializer();
    final Logger logger = config.getLogger();
    mLogger = (logger != null) ? logger
        : Logger.newLogger(LogPrinters.javaLoggingPrinter(getClass().getName()));
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

  protected Map<String, String> getCapabilities() {
    // TODO: 17/04/2019 immutable?
    return null;
  }

  protected void onStart() throws Exception {
    // do nothing
  }

  protected void onStop() throws Exception {
    // do nothing
  }

  final void start() {
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

              } else if (message instanceof CreateActorRequest) {
                final CreateActorRequest createRequest = (CreateActorRequest) message;
                final ActorRef actorRef = createRequest.getRecipientRef();
                if (actorRef == null) {
                  safeSend(new CreateActorResponse().withError(new IllegalArgumentException()),
                      createRequest.getSenderId());

                } else {
                  final Capabilities capabilities = new Capabilities(getCapabilities());
                  if (capabilities.checkTrue(Capabilities.CREATE_REMOTE)) {
                    final RemoteClassLoader classLoader = mClassLoader;
                    final Map<String, SerializableData> resources = createRequest.getResources();
                    if (resources != null) {
                      for (final Entry<String, SerializableData> entry : resources.entrySet()) {
                        classLoader.register(entry.getKey(), entry.getValue());
                      }
                    }
                    final SerializableData roleData = createRequest.getRoleData();
                    try {
                      final Object role = mSerializer.deserialize(roleData, classLoader);
                      if (!(role instanceof Role)) {
                        safeSend(new CreateActorResponse().withRecipientRef(actorRef)
                            .withError(new IllegalStateException()), createRequest.getSenderId());

                      } else {
                        newActor(actorRef.getId(), (Role) role);
                        safeSend(new CreateActorResponse().withRecipientRef(actorRef),
                            createRequest.getSenderId());
                      }

                    } catch (final RemoteClassNotFoundException e) {
                      if (capabilities.checkTrue(Capabilities.LOAD_REMOTE)) {
                        safeSend(new CreateActorContinue().withOriginalRequest(createRequest)
                            .addResourcePath(e.getMessage()), createRequest.getSenderId());

                      } else {
                        safeSend(new CreateActorResponse().withRecipientRef(actorRef).withError(e),
                            createRequest.getSenderId());
                      }

                    } catch (final Exception e) {
                      safeSend(new CreateActorResponse().withRecipientRef(actorRef).withError(e),
                          createRequest.getSenderId());
                    }

                  } else {
                    safeSend(new CreateActorResponse().withRecipientRef(actorRef)
                            .withError(new UnsupportedOperationException(Capabilities.CREATE_REMOTE)),
                        createRequest.getSenderId());
                  }
                }

              } else if (message instanceof DismissActorRequest) {
                final Map<String, String> capabilities = getCapabilities();
                if ((capabilities != null) && Boolean.TRUE.toString()
                    .equalsIgnoreCase(capabilities.get(Capabilities.DISMISS_REMOTE))) {
                  final DismissActorRequest dismissRequest = (DismissActorRequest) message;
                  final ActorRef actorRef = dismissRequest.getRecipientRef();
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
                final ActorRef actorRef = remoteMessage.getRecipientRef();
                if (actorRef == null) {
                  safeSend(new RemoteBounce().withError(new IllegalArgumentException())
                      .withMessage(remoteMessage), remoteMessage.getSenderId());

                } else {
                  final RemoteClassLoader classLoader = mClassLoader;
                  final Map<String, SerializableData> resources = remoteMessage.getResources();
                  if (resources != null) {
                    for (final Entry<String, SerializableData> entry : resources.entrySet()) {
                      classLoader.register(entry.getKey(), entry.getValue());
                    }
                  }
                  try {
                    final Actor actor = get(actorRef.getId());
                    final String hash = mActorHashes.get(actor);
                    if ((hash != null) && hash.equals(actorRef.getHash())) {
                      try {
                        mSerializer.deserialize(remoteMessage.getMessageData(), classLoader);

                      } catch (final Exception e) {
                        safeSend(new RemoteBounce().withError(e).withMessage(remoteMessage),
                            remoteMessage.getSenderId());
                      }

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
                      actor.tell(remoteMessage.getMessageData(), options, sender);

                    } else {
                      safeSend(new RemoteBounce().withMessage(remoteMessage),
                          remoteMessage.getSenderId());
                    }

                  } catch (final IllegalArgumentException e) {
                    safeSend(new RemoteBounce().withError(e).withMessage(remoteMessage),
                        remoteMessage.getSenderId());
                  }
                }

              } else if (message instanceof Create) {
                final String hash = UUID.randomUUID().toString();
                mActorHashes.put(envelop.getSender(), hash);

              } else if (message instanceof Dismiss) {
                mActorHashes.remove(envelop.getSender());

              } else if (message instanceof RemoteResponse) {
                try {
                  final RemoteResponse response = (RemoteResponse) message;
                  final Envelop env = response.getEnvelop();
                  final Actor sender = env.getSender();
                  final ActorRef actorRef = mSenders.get(envelop.getSender());
                  final String hash = mActorHashes.get(sender);
                  safeSend(new RemoteMessage().withRecipientRef(actorRef)
                      .withMessageData(
                          SerializableData.wrap(mSerializer.serialize(response.getMessage())))
                      .withOptions(env.getOptions())
                      .withSenderRef(new ActorRef().withId(sender.getId()).withHash(hash))
                      .withSentTimestamp(env.getSentAt()), response.getRecipientId());

                } catch (final Exception e) {
                  mLogger.err(e, "failed to send message");
                }
              }
            }

            public void onStart(@NotNull final Agent agent) throws Exception {
              mSender = mConnector.connect(new Receiver() {

                public void receive(@NotNull final Remote remote) {
                  mActor.tell(remote, mOptions, BackStage.newActor(new BounceRole()));
                }
              });
              RemoteServer.this.onStart();
            }

            public void onStop(@NotNull final Agent agent) throws Exception {
              agent.getExecutorService().shutdown();
              mSender.disconnect();
              RemoteServer.this.onStop();
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

  final void stop() {
    synchronized (mMutex) {
      if (mActor == null) {
        throw new IllegalStateException("not started");
      }
    }
    mActor.dismiss(false);
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
      mSender.send(remote, recipientId);

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

  private class BounceRole extends Role {

    @NotNull
    public Behavior getBehavior(@NotNull final String id) {
      return new AbstractBehavior() {

        public void onMessage(final Object message, @NotNull final Envelop envelop,
            @NotNull final Agent agent) {
          if (message instanceof Bounce) {
            try {
              final Object bounced = ((Bounce) message).getMessage();
              if (bounced instanceof RemoteMessage) {
                final RemoteMessage remoteMessage = (RemoteMessage) bounced;
                final Options options = (remoteMessage).getOptions();
                if (options != null) {
                  final String receiptId = options.getReceiptId();
                  if (receiptId != null) {
                    safeSend(new RemoteMessage().withRecipientRef(remoteMessage.getSenderRef())
                            .withMessageData(SerializableData.wrap(mSerializer.serialize(message)))
                            .withSenderRef(remoteMessage.getRecipientRef()),
                        remoteMessage.getSenderId());
                  }
                }

              } else if (bounced instanceof Remote) {
                final Remote remote = (Remote) bounced;
                safeSend(new RemoteBounce().withMessage(remote), remote.getSenderId());
              }

            } catch (final Exception e) {
              agent.getLogger().err(e, "failed to send bounce message");
            }
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
    public Logger getLogger(@NotNull final String id) {
      return mLogger;
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
    public Logger getLogger(@NotNull final String id) {
      return mLogger;
    }
  }
}
