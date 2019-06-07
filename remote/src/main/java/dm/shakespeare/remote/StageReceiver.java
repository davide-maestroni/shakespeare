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

import java.io.File;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import dm.shakespeare.Stage;
import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.ActorSet;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Options;
import dm.shakespeare.actor.Role;
import dm.shakespeare.function.Tester;
import dm.shakespeare.log.LogPrinters;
import dm.shakespeare.log.Logger;
import dm.shakespeare.remote.config.RemoteConfig;
import dm.shakespeare.remote.config.StageConfig;
import dm.shakespeare.remote.io.Serializer;
import dm.shakespeare.remote.transport.ActorID;
import dm.shakespeare.remote.transport.Connector;
import dm.shakespeare.remote.transport.Connector.Receiver;
import dm.shakespeare.remote.transport.Connector.Sender;
import dm.shakespeare.remote.transport.CreateActorContinue;
import dm.shakespeare.remote.transport.CreateActorRequest;
import dm.shakespeare.remote.transport.CreateActorResponse;
import dm.shakespeare.remote.transport.DismissActorRequest;
import dm.shakespeare.remote.transport.DismissActorResponse;
import dm.shakespeare.remote.transport.FindRequest;
import dm.shakespeare.remote.transport.FindRequest.FilterType;
import dm.shakespeare.remote.transport.FindResponse;
import dm.shakespeare.remote.transport.MessageContinue;
import dm.shakespeare.remote.transport.MessageRequest;
import dm.shakespeare.remote.transport.MessageResponse;
import dm.shakespeare.remote.transport.RemoteRequest;
import dm.shakespeare.remote.transport.RemoteResponse;
import dm.shakespeare.remote.transport.UploadRequest;
import dm.shakespeare.remote.transport.UploadResponse;
import dm.shakespeare.remote.util.SerializableData;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.WeakValueHashMap;

/**
 * Created by davide-maestroni on 06/04/2019.
 */
public class StageReceiver {

  private final WeakValueHashMap<String, Actor> actors = new WeakValueHashMap<String, Actor>();
  private final RemoteClassLoader classLoader;
  private final Object connectionMutex = new Object();
  private final Connector connector;
  private final ExecutorService executorService;
  private final Object idsMutex = new Object();
  private final WeakHashMap<Actor, String> instances = new WeakHashMap<Actor, String>();
  private final Logger logger;
  private final boolean remoteCreateEnabled;
  private final boolean remoteDismissEnabled;
  private final boolean remoteMessagesEnabled;
  private final boolean remoteResourcesEnabled;
  private final boolean remoteRolesEnabled;
  private final WeakValueHashMap<SenderID, Actor> senders = new WeakValueHashMap<SenderID, Actor>();
  private final Object sendersMutex = new Object();
  private final Serializer serializer;
  private final Stage stage;

  private Sender sender;

  public StageReceiver(@NotNull final StageConfig config, @NotNull final Stage stage) {
    this.stage = ConstantConditions.notNull("stage", stage);
    // connector
    final Connector connector =
        (this.connector = config.getOption(Connector.class, RemoteConfig.KEY_CONNECTOR_CLASS));
    if (connector == null) {
      throw new IllegalArgumentException("missing connector configuration");
    }
    // serializer
    final Serializer serializer =
        config.getOption(Serializer.class, RemoteConfig.KEY_SERIALIZER_CLASS);
    this.serializer = (serializer != null) ? serializer : new JavaSerializer();
    @SuppressWarnings("unchecked") final List<String> whitelist =
        config.getOption(List.class, RemoteConfig.KEY_SERIALIZER_WHITELIST);
    if (whitelist != null) {
      this.serializer.whitelist(whitelist);
    }
    @SuppressWarnings("unchecked") final List<String> blacklist =
        config.getOption(List.class, RemoteConfig.KEY_SERIALIZER_BLACKLIST);
    if (blacklist != null) {
      this.serializer.blacklist(blacklist);
    }
    // classloader
    final ClassLoader classLoader =
        config.getOption(ClassLoader.class, RemoteConfig.KEY_CLASSLOADER_CLASS);
    File container = config.getOption(File.class, RemoteConfig.KEY_CLASSLOADER_DIR);
    if (container == null) {
      container = new File(new File(System.getProperty("java.io.tmpdir")), "shakespeare");
      if (!container.isDirectory() && !container.mkdir()) {
        throw new IllegalArgumentException("missing container");
      }
    }
    final ProtectionDomain protectionDomain =
        config.getOption(ProtectionDomain.class, RemoteConfig.KEY_PROTECTION_DOMAIN_CLASS);
    if (classLoader != null) {
      this.classLoader = new RemoteClassLoader(classLoader, container, protectionDomain);

    } else {
      this.classLoader = new RemoteClassLoader(container, protectionDomain);
    }
    // executor
    final ExecutorService executorService =
        config.getOption(ExecutorService.class, RemoteConfig.KEY_EXECUTOR_CLASS);
    if (executorService != null) {
      this.executorService = executorService;

    } else {
      this.executorService = Role.defaultExecutorService();
    }
    // logger
    final Logger logger = config.getOption(Logger.class, RemoteConfig.KEY_LOGGER_CLASS);
    if (logger != null) {
      this.logger = logger;

    } else {
      final String loggerName = config.getOption(String.class, RemoteConfig.KEY_LOGGER_NAME);
      this.logger = new Logger(LogPrinters.javaLoggingPrinter(
          isNotEmpty(loggerName) ? loggerName : getClass().getName()));
    }
    // options
    final Boolean remoteRolesEnabled =
        config.getOption(Boolean.class, RemoteConfig.KEY_REMOTE_ROLES_ENABLE);
    this.remoteRolesEnabled = (remoteRolesEnabled != null) ? remoteRolesEnabled : false;
    final Boolean remoteMessagesEnabled =
        config.getOption(Boolean.class, RemoteConfig.KEY_REMOTE_MESSAGES_ENABLE);
    this.remoteMessagesEnabled = (remoteMessagesEnabled != null) ? remoteMessagesEnabled : false;
    final Boolean remoteResourcesEnabled =
        config.getOption(Boolean.class, RemoteConfig.KEY_REMOTE_RESOURCES_ENABLE);
    this.remoteResourcesEnabled = (remoteResourcesEnabled != null) ? remoteResourcesEnabled : false;
    final Boolean remoteCreateEnabled =
        config.getOption(Boolean.class, RemoteConfig.KEY_REMOTE_CREATE_ENABLE);
    this.remoteCreateEnabled = (remoteCreateEnabled != null) ? remoteCreateEnabled : false;
    final Boolean remoteDismissEnabled =
        config.getOption(Boolean.class, RemoteConfig.KEY_REMOTE_DISMISS_ENABLE);
    this.remoteDismissEnabled = (remoteDismissEnabled != null) ? remoteDismissEnabled : false;
  }

  private static boolean isNotEmpty(@Nullable final String string) {
    return ((string != null) && (string.trim().length() > 0));
  }

  public Receiver connect() throws Exception {
    final RemoteReceiver receiver = new RemoteReceiver();
    final Sender sender = connector.connect(receiver);
    synchronized (connectionMutex) {
      if (this.sender != null) {
        throw new IllegalStateException("stage is already connected");
      }
      this.sender = sender;
    }
    return receiver;
  }

  public void disconnect() {
    final Sender sender;
    synchronized (connectionMutex) {
      sender = this.sender;
      this.sender = null;
    }

    if (sender != null) {
      sender.disconnect();
    }
  }

  @Nullable
  private String getInstanceId(@NotNull final Actor actor) {
    synchronized (idsMutex) {
      return instances.get(actor);
    }
  }

  @NotNull
  private Sender getSender() {
    synchronized (connectionMutex) {
      final Sender sender = this.sender;
      if (sender == null) {
        throw new IllegalStateException("stage is not connected");
      }
      return sender;
    }
  }

  @NotNull
  private Actor getSenderActor(@NotNull final ActorID actorID, @NotNull final String senderId) {
    synchronized (sendersMutex) {
      final SenderID senderID = new SenderID(actorID, senderId);
      Actor sender = senders.get(senderID);
      if (sender == null) {
        sender = Stage.newActor(actorID.getActorId(), new SenderRole(actorID, senderId));
        senders.put(senderID, sender);
      }
      return sender;
    }
  }

  @NotNull
  private RemoteResponse handleCreate(@NotNull final CreateActorRequest request) {
    final String actorId = request.getActorId();
    if ((actorId == null)) {
      return new CreateActorResponse().withError(new IllegalArgumentException());
    }
    if (remoteCreateEnabled) {
      final SerializableData roleData = request.getRoleData();
      try {
        Set<String> dependencies = null;
        final RemoteClassLoader classLoader = this.classLoader;
        final Map<String, SerializableData> resources = request.getResources();
        if (remoteRolesEnabled) {
          dependencies = classLoader.register(resources);
        }

        if ((dependencies != null) && !dependencies.isEmpty()) {
          return new CreateActorContinue().addAllResourcePaths(dependencies);
        }
        final Object role = serializer.deserialize(roleData, classLoader);
        if (!(role instanceof Role)) {
          return new CreateActorResponse().withError(new IllegalArgumentException());
        }
        final Actor actor = stage.createActor(actorId, (Role) role);
        final String instanceId = UUID.randomUUID().toString();
        synchronized (idsMutex) {
          actors.put(instanceId, actor);
          instances.put(actor, instanceId);
        }
        return new CreateActorResponse().withActorID(
            new ActorID().withActorId(actorId).withInstanceId(instanceId));

      } catch (final RemoteClassNotFoundException e) {
        if (remoteRolesEnabled) {
          return new CreateActorContinue().addResourcePath(e.getMessage());
        }
        return new CreateActorResponse().withError(e);

      } catch (final Exception e) {
        logger.err(e, "error while handling create request");
        return new CreateActorResponse().withError(e);
      }
    }
    return new CreateActorResponse().withError(new UnsupportedOperationException());
  }

  @NotNull
  private DismissActorResponse handleDismiss(@NotNull final DismissActorRequest request) {
    if (remoteDismissEnabled) {
      final ActorID actorID = request.getActorID();
      if (actorID == null) {
        return new DismissActorResponse().withError(new IllegalArgumentException());
      }
      final Actor actor = stage.get(actorID.getActorId());
      if (actor == null) {
        return new DismissActorResponse().withError(new IllegalArgumentException());
      }
      synchronized (idsMutex) {
        if (!actor.equals(actors.get(actorID.getInstanceId()))) {
          return new DismissActorResponse().withError(new IllegalArgumentException());
        }
      }
      actor.dismiss(request.getMayInterruptIfRunning());
    }
    return new DismissActorResponse().withError(new UnsupportedOperationException());
  }

  @NotNull
  private FindResponse handleFind(@NotNull final FindRequest request) {
    final FilterType filterType = request.getFilterType();
    final String pattern = request.getPattern();
    final Tester<? super Actor> tester = request.getTester();
    if (filterType == FilterType.ALL) {
      final ActorSet actorSet;
      if (tester != null) {
        actorSet = stage.findAll(tester);

      } else if (pattern != null) {
        actorSet = stage.findAll(Pattern.compile(pattern));

      } else {
        actorSet = stage.getAll();
      }
      final HashSet<ActorID> actorIds = new HashSet<ActorID>();
      for (final Actor actor : actorSet) {
        actorIds.add(new ActorID().withActorId(actor.getId()).withInstanceId(getInstanceId(actor)));
      }
      return new FindResponse().withActorIDs(actorIds);

    } else if (filterType == FilterType.ANY) {
      final Actor actor;
      if (tester != null) {
        actor = stage.findAny(tester);

      } else if (pattern != null) {
        actor = stage.findAny(Pattern.compile(pattern));

      } else {
        actor = null;
      }
      if (actor != null) {
        final String instanceId = getInstanceId(actor);
        return new FindResponse().withActorIDs(Collections.singleton(
            new ActorID().withActorId(actor.getId()).withInstanceId(instanceId)));
      }

    } else if (filterType == FilterType.EXACT) {
      final Actor actor = stage.get(pattern);
      if (actor != null) {
        return new FindResponse().withActorIDs(Collections.singleton(
            new ActorID().withActorId(actor.getId()).withInstanceId(getInstanceId(actor))));
      }
    }
    return new FindResponse().withActorIDs(Collections.<ActorID>emptySet());
  }

  @NotNull
  private RemoteResponse handleMessage(@NotNull final MessageRequest request) {
    final String senderId = request.getSenderId();
    final ActorID actorID = request.getActorID();
    final ActorID senderID = request.getSenderActorID();
    if ((actorID == null) || (actorID.getActorId() == null) || (senderID == null) || (
        senderID.getActorId() == null)) {
      return new MessageResponse().withError(new IllegalArgumentException());
    }
    final Actor actor = stage.get(actorID.getActorId());
    if (actor == null) {
      return new MessageResponse().withError(new IllegalArgumentException());
    }
    final String instanceId = getInstanceId(actor);
    if ((instanceId == null) || !instanceId.equals(actorID.getInstanceId())) {
      return new MessageResponse().withError(new IllegalArgumentException());
    }
    final Actor sender = getSenderActor(senderID, senderId);
    try {
      final RemoteClassLoader classLoader = this.classLoader;
      Set<String> dependencies = null;
      final Map<String, SerializableData> resources = request.getResources();
      if ((resources != null) && remoteMessagesEnabled) {
        dependencies = classLoader.register(resources);
      }

      if ((dependencies != null) && !dependencies.isEmpty()) {
        return new MessageContinue().addAllResourcePaths(dependencies);
      }
      Object msg = serializer.deserialize(request.getMessageData(), classLoader);
      Options options = request.getOptions();
      if (options != null) {
        options = options.asSentAt(request.getSentTimestamp());

      } else {
        options = new Options().asSentAt(request.getSentTimestamp());
      }
      actor.tell(msg, options, sender);

    } catch (final RemoteClassNotFoundException e) {
      if (remoteMessagesEnabled) {
        return new MessageContinue().addResourcePath(e.getMessage());
      }
      logger.err(e, "error while handling message request");
      return new MessageResponse().withError(e);

    } catch (final Exception e) {
      logger.err(e, "error while handling message request");
      return new MessageResponse().withError(e);
    }
    return new MessageResponse();
  }

  @NotNull
  private UploadResponse handleUpload(@NotNull final UploadRequest request) {
    if (remoteResourcesEnabled) {
      try {
        classLoader.register(request.getResources());

      } catch (final Exception e) {
        logger.err(e, "error while handling upload request");
        return new UploadResponse().withError(e);
      }
    }
    return new UploadResponse().withError(new UnsupportedOperationException());
  }

  private static class SenderID {

    private final ActorID actorID;
    private final String senderId;

    private SenderID(@NotNull final ActorID actorID, @Nullable final String senderId) {
      this.actorID = actorID;
      this.senderId = senderId;
    }

    @Override
    public int hashCode() {
      int result = actorID.hashCode();
      result = 31 * result + (senderId != null ? senderId.hashCode() : 0);
      return result;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }

      if ((o == null) || getClass() != o.getClass()) {
        return false;
      }

      final SenderID senderID = (SenderID) o;
      return actorID.equals(senderID.actorID) && ((senderId != null) ? senderId.equals(
          senderID.senderId) : senderID.senderId == null);
    }
  }

  private class RemoteReceiver implements Receiver {

    @NotNull
    public RemoteResponse receive(@NotNull final RemoteRequest request) {
      if (request instanceof CreateActorRequest) {
        return handleCreate((CreateActorRequest) request);

      } else if (request instanceof DismissActorRequest) {
        return handleDismiss((DismissActorRequest) request);

      } else if (request instanceof FindRequest) {
        return handleFind((FindRequest) request);

      } else if (request instanceof MessageRequest) {
        return handleMessage((MessageRequest) request);

      } else if (request instanceof UploadRequest) {
        return handleUpload((UploadRequest) request);
      }
      throw new UnsupportedOperationException();
    }
  }

  private class SenderRole extends Role {

    private final ActorID actorID;
    private final String senderId;

    private SenderRole(@NotNull final ActorID actorID, @NotNull final String senderId) {
      this.actorID = actorID;
      this.senderId = senderId;
    }

    @NotNull
    public Behavior getBehavior(@NotNull final String id) {
      return new AbstractBehavior() {

        public void onMessage(final Object message, @NotNull final Envelop envelop,
            @NotNull final Agent agent) throws Exception {
          final Actor sender = envelop.getSender();
          final ActorID senderID =
              new ActorID().withActorId(sender.getId()).withInstanceId(getInstanceId(sender));
          getSender().send(new MessageRequest().withActorID(SenderRole.this.actorID)
              .withSenderActorID(senderID)
              .withOptions(envelop.getOptions())
              .withMessageData(SerializableData.wrap(serializer.serialize(message)))
              .withSentTimestamp(envelop.getSentAt()), senderId);
        }
      };
    }

    @NotNull
    @Override
    public ExecutorService getExecutorService(@NotNull final String id) {
      return executorService;
    }
  }
}
