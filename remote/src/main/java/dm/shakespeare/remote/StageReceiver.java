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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import dm.shakespeare.Stage;
import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.ActorSet;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.actor.Role;
import dm.shakespeare.function.Tester;
import dm.shakespeare.log.LogPrinters;
import dm.shakespeare.log.Logger;
import dm.shakespeare.remote.config.RemoteConfig;
import dm.shakespeare.remote.config.StageConfig;
import dm.shakespeare.remote.io.DataStore;
import dm.shakespeare.remote.io.FileDataStore;
import dm.shakespeare.remote.io.JavaSerializer;
import dm.shakespeare.remote.io.RawData;
import dm.shakespeare.remote.io.Serializer;
import dm.shakespeare.remote.transport.connection.Connector;
import dm.shakespeare.remote.transport.connection.Connector.Receiver;
import dm.shakespeare.remote.transport.connection.Connector.Sender;
import dm.shakespeare.remote.transport.message.ActorID;
import dm.shakespeare.remote.transport.message.CreateActorContinue;
import dm.shakespeare.remote.transport.message.CreateActorRequest;
import dm.shakespeare.remote.transport.message.CreateActorResponse;
import dm.shakespeare.remote.transport.message.DismissActorRequest;
import dm.shakespeare.remote.transport.message.DismissActorResponse;
import dm.shakespeare.remote.transport.message.FindRequest;
import dm.shakespeare.remote.transport.message.FindRequest.FilterType;
import dm.shakespeare.remote.transport.message.FindResponse;
import dm.shakespeare.remote.transport.message.MessageContinue;
import dm.shakespeare.remote.transport.message.MessageRequest;
import dm.shakespeare.remote.transport.message.MessageResponse;
import dm.shakespeare.remote.transport.message.RemoteRequest;
import dm.shakespeare.remote.transport.message.RemoteResponse;
import dm.shakespeare.remote.transport.message.UploadRequest;
import dm.shakespeare.remote.transport.message.UploadResponse;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 06/04/2019.
 */
public class StageReceiver {

  private final WeakHashMap<Actor, String> actorToInstanceId = new WeakHashMap<Actor, String>();
  private final RemoteClassLoader classLoader;
  private final Object connectionMutex = new Object();
  private final Connector connector;
  private final RequestHandler<CreateActorRequest> createHandler;
  private final RequestHandler<DismissActorRequest> dismissHandler;
  private final ExecutorService executorService;
  private final Object idsMutex = new Object();
  private final Logger logger;
  private final LinkedList<SenderActor> lruSenders = new LinkedList<SenderActor>();
  private final RequestHandler<MessageRequest> messageHandler;
  private final HashMap<SenderID, SenderActor> senderIdToActor =
      new HashMap<SenderID, SenderActor>();
  private final Integer sendersCacheSize;
  private final Long sendersCacheTimeout;
  private final Object sendersMutex = new Object();
  private final Serializer serializer;
  private final Stage stage;
  private final RequestHandler<UploadRequest> uploadHandler;

  private Sender messageSender;

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
    DataStore dataStore =
        config.getOption(DataStore.class, RemoteConfig.KEY_CLASSLOADER_DATA_STORE_CLASS);
    if (dataStore == null) {
      File container = config.getOption(File.class, RemoteConfig.KEY_CLASSLOADER_DIR);
      if (container == null) {
        container = new File(new File(System.getProperty("java.io.tmpdir")), "shakespeare");
        if (!container.isDirectory() && !container.mkdir()) {
          throw new IllegalArgumentException("missing container");
        }
      }
      dataStore = new FileDataStore(container);
    }
    final ProtectionDomain protectionDomain =
        config.getOption(ProtectionDomain.class, RemoteConfig.KEY_PROTECTION_DOMAIN_CLASS);
    if (classLoader != null) {
      this.classLoader = new RemoteClassLoader(classLoader, protectionDomain, dataStore);

    } else {
      this.classLoader = new RemoteClassLoader(protectionDomain, dataStore);
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
    final Integer sendersCacheSize =
        config.getOption(Integer.class, RemoteConfig.KEY_SENDERS_CACHE_MAX_SIZE);
    if (sendersCacheSize != null) {
      this.sendersCacheSize = sendersCacheSize;
    } else {
      this.sendersCacheSize = 10000;
    }
    final Long sendersCacheTimeout =
        config.getOption(Long.class, RemoteConfig.KEY_SENDERS_CACHE_TIMEOUT);
    if (sendersCacheTimeout != null) {
      this.sendersCacheTimeout = sendersCacheTimeout;
    } else {
      this.sendersCacheTimeout = TimeUnit.MINUTES.toMillis(15);
    }
    // handlers
    final Boolean remoteCreateEnabled =
        config.getOption(Boolean.class, RemoteConfig.KEY_REMOTE_CREATE_ENABLE);
    if ((remoteCreateEnabled != null) && remoteCreateEnabled) {
      final Boolean remoteRolesEnabled =
          config.getOption(Boolean.class, RemoteConfig.KEY_REMOTE_ROLES_ENABLE);
      if ((remoteRolesEnabled != null) && remoteRolesEnabled) {
        this.createHandler = new CreateWithResourcesHandler();

      } else {
        this.createHandler = new CreateHandler();
      }

    } else {
      this.createHandler = new CreateUnsupportedHandler();
    }
    final Boolean remoteDismissEnabled =
        config.getOption(Boolean.class, RemoteConfig.KEY_REMOTE_DISMISS_ENABLE);
    if ((remoteDismissEnabled != null) && remoteDismissEnabled) {
      this.dismissHandler = new DismissHandler();

    } else {
      this.dismissHandler = new DismissUnsupportedHandler();
    }
    final Boolean remoteMessagesEnabled =
        config.getOption(Boolean.class, RemoteConfig.KEY_REMOTE_MESSAGES_ENABLE);
    if ((remoteMessagesEnabled != null) && remoteMessagesEnabled) {
      this.messageHandler = new MessageWithResourcesHandler();

    } else {
      this.messageHandler = new MessageHandler();
    }
    final Boolean remoteResourcesEnabled =
        config.getOption(Boolean.class, RemoteConfig.KEY_REMOTE_RESOURCES_ENABLE);
    if ((remoteResourcesEnabled != null) && remoteResourcesEnabled) {
      this.uploadHandler = new UploadHandler();

    } else {
      this.uploadHandler = new UploadUnsupportedHandler();
    }
    this.logger.dbg("[%s] created receiver with config: %s", this, config);
  }

  @NotNull
  private static String createInstanceId() {
    return "remote:" + UUID.randomUUID().toString();
  }

  private static boolean isNotEmpty(@Nullable final String string) {
    return ((string != null) && (string.trim().length() > 0));
  }

  public void connect() throws Exception {
    logger.dbg("[%s] connecting", this);
    final RemoteReceiver receiver = new RemoteReceiver();
    final Sender sender = connector.connect(receiver);
    synchronized (connectionMutex) {
      if (this.messageSender != null) {
        throw new IllegalStateException("stage is already connected");
      }
      this.messageSender = sender;
    }
  }

  public void disconnect() {
    final Sender sender;
    synchronized (connectionMutex) {
      sender = this.messageSender;
      this.messageSender = null;
    }

    if (sender != null) {
      logger.dbg("[%s] disconnecting", this);
      sender.disconnect();
    }
  }

  private void flushSenders() {
    final LinkedList<SenderActor> lruSenders = this.lruSenders;
    final HashMap<SenderID, SenderActor> senderIdToActor = this.senderIdToActor;
    int toRemove = lruSenders.size() - sendersCacheSize;
    final long timeout = System.currentTimeMillis() - sendersCacheTimeout;
    final Iterator<SenderActor> iterator = lruSenders.iterator();
    while (iterator.hasNext()) {
      final SenderActor senderActor = iterator.next();
      if ((toRemove <= 0) && (senderActor.getTimestamp() >= timeout)) {
        break;
      }
      senderIdToActor.remove(senderActor.getSenderId());
      iterator.remove();
      --toRemove;
    }
  }

  @Nullable
  private String getInstanceId(@NotNull final Actor actor) {
    synchronized (idsMutex) {
      return actorToInstanceId.get(actor);
    }
  }

  @NotNull
  private Sender getMessageSender() {
    synchronized (connectionMutex) {
      final Sender sender = this.messageSender;
      if (sender == null) {
        throw new IllegalStateException("stage is not connected");
      }
      return sender;
    }
  }

  @NotNull
  private String getOrAddInstanceId(@NotNull final Actor actor) {
    synchronized (idsMutex) {
      final WeakHashMap<Actor, String> actorToInstanceId = this.actorToInstanceId;
      String instanceId = actorToInstanceId.get(actor);
      if (instanceId == null) {
        instanceId = createInstanceId();
        actorToInstanceId.put(actor, instanceId);
      }
      return instanceId;
    }
  }

  @Nullable
  private Actor getOrCreateSender(@NotNull final ActorID actorID, final String senderId) {
    if (actorID.getInstanceId() == null) {
      return stage.get(actorID.getActorId());
    }

    final SenderID senderID = new SenderID(actorID, senderId);
    SenderActor senderActor;
    final HashMap<SenderID, SenderActor> senderIdToActor = this.senderIdToActor;
    synchronized (sendersMutex) {
      senderActor = senderIdToActor.get(senderID);
    }

    if (senderActor == null) {
      final Actor actor = Stage.back().createActor(actorID.getActorId(), new SenderRole(senderID));
      senderActor = new SenderActor(actor, senderID);
      synchronized (sendersMutex) {
        if (senderIdToActor.put(senderID, senderActor) == null) {
          lruSenders.add(senderActor);
          flushSenders();
        }
      }

    } else {
      synchronized (sendersMutex) {
        final LinkedList<SenderActor> lruSenders = this.lruSenders;
        lruSenders.remove(senderActor);
        lruSenders.add(senderActor.refresh());
        flushSenders();
      }
    }
    return senderActor.getActor();
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
        actorIds.add(
            new ActorID().withActorId(actor.getId()).withInstanceId(getOrAddInstanceId(actor)));
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
        final String instanceId = getOrAddInstanceId(actor);
        return new FindResponse().withActorIDs(Collections.singleton(
            new ActorID().withActorId(actor.getId()).withInstanceId(instanceId)));
      }

    } else if (filterType == FilterType.EXACT) {
      final Actor actor = stage.get(pattern);
      if (actor != null) {
        return new FindResponse().withActorIDs(Collections.singleton(
            new ActorID().withActorId(actor.getId()).withInstanceId(getOrAddInstanceId(actor))));
      }
    }
    return new FindResponse().withActorIDs(Collections.<ActorID>emptySet());
  }

  @Nullable
  private Actor resolveActor(@NotNull final ActorID actorID, @Nullable final String senderId) {
    synchronized (sendersMutex) {
      final SenderActor senderActor = senderIdToActor.get(new SenderID(actorID, senderId));
      if (senderActor != null) {
        final LinkedList<SenderActor> lruSenders = this.lruSenders;
        lruSenders.remove(senderActor);
        lruSenders.add(senderActor.refresh());
        return senderActor.getActor();
      }
    }
    return stage.get(actorID.getActorId());
  }

  private interface RequestHandler<R extends RemoteRequest> {

    @NotNull
    RemoteResponse handle(@NotNull R request);
  }

  private static class CreateUnsupportedHandler implements RequestHandler<CreateActorRequest> {

    @NotNull
    public RemoteResponse handle(@NotNull final CreateActorRequest request) {
      final String actorId = request.getActorId();
      if (actorId == null) {
        return new CreateActorResponse().withError(
            new IllegalArgumentException("missing actor ID"));
      }
      return new CreateActorResponse().withError(new UnsupportedOperationException("create"));
    }
  }

  private static class DismissUnsupportedHandler implements RequestHandler<DismissActorRequest> {

    @NotNull
    public DismissActorResponse handle(@NotNull final DismissActorRequest request) {
      return new DismissActorResponse().withError(new UnsupportedOperationException("dismiss"));
    }
  }

  private static class SenderActor {

    private final Actor actor;
    private final SenderID senderId;

    private long timestamp;

    private SenderActor(@NotNull final Actor actor, @NotNull final SenderID senderId) {
      this.actor = actor;
      this.senderId = senderId;
      timestamp = System.currentTimeMillis();
    }

    @NotNull
    Actor getActor() {
      return actor;
    }

    @NotNull
    SenderID getSenderId() {
      return senderId;
    }

    long getTimestamp() {
      return timestamp;
    }

    @NotNull
    SenderActor refresh() {
      timestamp = System.currentTimeMillis();
      return this;
    }
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

  private static class UploadUnsupportedHandler implements RequestHandler<UploadRequest> {

    @NotNull
    public UploadResponse handle(@NotNull final UploadRequest request) {
      return new UploadResponse().withError(new UnsupportedOperationException("upload"));
    }
  }

  private class CreateHandler implements RequestHandler<CreateActorRequest> {

    @NotNull
    public RemoteResponse handle(@NotNull final CreateActorRequest request) {
      final String actorId = request.getActorId();
      if (actorId == null) {
        return new CreateActorResponse().withError(
            new IllegalArgumentException("missing actor ID"));
      }

      final RawData roleData = request.getRoleData();
      try {
        final RemoteClassLoader classLoader = StageReceiver.this.classLoader;
        final Object role = serializer.deserialize(roleData, classLoader);
        if (!(role instanceof Role)) {
          return new CreateActorResponse().withError(
              new IllegalArgumentException("invalid role instance"));
        }
        final Actor actor = stage.createActor(actorId, (Role) role);
        final String instanceId = "remote:" + UUID.randomUUID().toString();
        synchronized (idsMutex) {
          actorToInstanceId.put(actor, instanceId);
        }
        return new CreateActorResponse().withActorID(
            new ActorID().withActorId(actorId).withInstanceId(instanceId));

      } catch (final RemoteClassNotFoundException e) {
        logger.err(e, "[%s] error while handling create request", StageReceiver.this);
        return new CreateActorResponse().withError(e);

      } catch (final Exception e) {
        logger.err(e, "[%s] error while handling create request", StageReceiver.this);
        return new CreateActorResponse().withError(e);
      }
    }
  }

  private class CreateWithResourcesHandler implements RequestHandler<CreateActorRequest> {

    @NotNull
    public RemoteResponse handle(@NotNull final CreateActorRequest request) {
      final String actorId = request.getActorId();
      if (actorId == null) {
        return new CreateActorResponse().withError(
            new IllegalArgumentException("missing actor ID"));
      }

      final RawData roleData = request.getRoleData();
      try {
        final RemoteClassLoader classLoader = StageReceiver.this.classLoader;
        final Map<String, RawData> resources = request.getResources();
        final Set<String> dependencies = classLoader.register(resources);
        if (!dependencies.isEmpty()) {
          return new CreateActorContinue().addAllResourcePaths(dependencies);
        }
        final Object role = serializer.deserialize(roleData, classLoader);
        if (!(role instanceof Role)) {
          return new CreateActorResponse().withError(
              new IllegalArgumentException("invalid role instance"));
        }
        final Actor actor = stage.createActor(actorId, (Role) role);
        final String instanceId = createInstanceId();
        synchronized (idsMutex) {
          actorToInstanceId.put(actor, instanceId);
        }
        return new CreateActorResponse().withActorID(
            new ActorID().withActorId(actorId).withInstanceId(instanceId));

      } catch (final RemoteClassNotFoundException e) {
        return new CreateActorContinue().addResourcePath(e.getMessage());

      } catch (final Exception e) {
        logger.err(e, "[%s] error while handling create request", StageReceiver.this);
        return new CreateActorResponse().withError(e);
      }
    }
  }

  private class DismissHandler implements RequestHandler<DismissActorRequest> {

    @NotNull
    public DismissActorResponse handle(@NotNull final DismissActorRequest request) {
      final ActorID actorID = request.getActorID();
      if (actorID == null) {
        return new DismissActorResponse().withError(
            new IllegalArgumentException("missing actor ID"));
      }
      final String senderId = request.getSenderId();
      final Actor actor = resolveActor(actorID, senderId);
      if (actor == null) {
        return new DismissActorResponse().withError(
            new IllegalArgumentException("invalid actor ID: " + actorID));
      }

      synchronized (idsMutex) {
        if (!actorToInstanceId.get(actor).equals(actorID.getInstanceId())) {
          return new DismissActorResponse().withError(
              new IllegalArgumentException("invalid actor ID: " + actorID));
        }
      }

      final boolean dismissed;
      if (request.getMayInterruptIfRunning()) {
        dismissed = actor.dismissNow();

      } else {
        dismissed = actor.dismiss();
      }

      if (dismissed) {
        synchronized (sendersMutex) {
          final SenderActor senderActor = senderIdToActor.remove(new SenderID(actorID, senderId));
          if (senderActor != null) {
            lruSenders.remove(senderActor);
          }
        }
      }

      return new DismissActorResponse();
    }
  }

  private class MessageHandler implements RequestHandler<MessageRequest> {

    @NotNull
    public RemoteResponse handle(@NotNull final MessageRequest request) {
      final String senderId = request.getSenderId();
      final ActorID actorID = request.getActorID();
      final ActorID senderID = request.getSenderActorID();
      if ((actorID == null) || (actorID.getActorId() == null) || (senderID == null) || (
          senderID.getActorId() == null)) {
        return new MessageResponse().withError(new IllegalArgumentException("missing actor ID"));
      }
      final Actor actor = resolveActor(actorID, senderId);
      if (actor == null) {
        return new MessageResponse().withError(
            new IllegalArgumentException("invalid actor ID: " + actorID));
      }
      final String instanceId = getInstanceId(actor);
      if ((instanceId == null) || !instanceId.equals(actorID.getInstanceId())) {
        return new MessageResponse().withError(
            new IllegalArgumentException("invalid actor ID: " + actorID));
      }
      final Actor sender = getOrCreateSender(senderID, senderId);
      if (sender == null) {
        return new MessageResponse().withError(
            new IllegalArgumentException("invalid sender actor ID: " + senderID));
      }

      try {
        final RemoteClassLoader classLoader = StageReceiver.this.classLoader;
        Object msg = serializer.deserialize(request.getMessageData(), classLoader);
        Headers headers = request.getHeaders();
        if (headers != null) {
          headers = headers.asSentAt(request.getSentTimestamp());

        } else {
          headers = Headers.empty().asSentAt(request.getSentTimestamp());
        }
        actor.tell(msg, headers, sender);

      } catch (final RemoteClassNotFoundException e) {
        logger.err(e, "[%s] error while handling message request", StageReceiver.this);
        return new MessageResponse().withError(e);

      } catch (final Exception e) {
        logger.err(e, "[%s] error while handling message request", StageReceiver.this);
        return new MessageResponse().withError(e);
      }
      return new MessageResponse();
    }
  }

  private class MessageWithResourcesHandler implements RequestHandler<MessageRequest> {

    @NotNull
    public RemoteResponse handle(@NotNull final MessageRequest request) {
      final String senderId = request.getSenderId();
      final ActorID actorID = request.getActorID();
      final ActorID senderID = request.getSenderActorID();
      if ((actorID == null) || (actorID.getActorId() == null) || (senderID == null) || (
          senderID.getActorId() == null)) {
        return new MessageResponse().withError(new IllegalArgumentException("missing actor ID"));
      }
      final Actor actor = resolveActor(actorID, senderId);
      if (actor == null) {
        return new MessageResponse().withError(
            new IllegalArgumentException("invalid actor ID: " + actorID));
      }
      final String instanceId = getInstanceId(actor);
      if ((instanceId == null) || !instanceId.equals(actorID.getInstanceId())) {
        return new MessageResponse().withError(
            new IllegalArgumentException("invalid actor ID: " + actorID));
      }
      final Actor sender = getOrCreateSender(senderID, senderId);
      if (sender == null) {
        return new MessageResponse().withError(
            new IllegalArgumentException("invalid sender actor ID: " + actorID));
      }

      try {
        final RemoteClassLoader classLoader = StageReceiver.this.classLoader;
        Set<String> dependencies = null;
        final Map<String, RawData> resources = request.getResources();
        if (resources != null) {
          dependencies = classLoader.register(resources);
        }

        if ((dependencies != null) && !dependencies.isEmpty()) {
          return new MessageContinue().addAllResourcePaths(dependencies);
        }
        Object msg = serializer.deserialize(request.getMessageData(), classLoader);
        Headers headers = request.getHeaders();
        if (headers != null) {
          headers = headers.asSentAt(request.getSentTimestamp());

        } else {
          headers = Headers.empty().asSentAt(request.getSentTimestamp());
        }
        actor.tell(msg, headers, sender);

      } catch (final RemoteClassNotFoundException e) {
        return new MessageContinue().addResourcePath(e.getMessage());

      } catch (final Exception e) {
        logger.err(e, "[%s] error while handling message request", StageReceiver.this);
        return new MessageResponse().withError(e);
      }
      return new MessageResponse();
    }
  }

  private class RemoteReceiver implements Receiver {

    @NotNull
    public RemoteResponse receive(@NotNull final RemoteRequest request) {
      final Logger logger = StageReceiver.this.logger;
      if (request instanceof MessageRequest) {
        logger.dbg("[%s] handling message request: ", StageReceiver.this, request);
        final RemoteResponse response = messageHandler.handle((MessageRequest) request);
        logger.dbg("[%s] message response: ", StageReceiver.this, response);
        return response;

      } else if (request instanceof FindRequest) {
        logger.dbg("[%s] handling find request: ", StageReceiver.this, request);
        final FindResponse response = handleFind((FindRequest) request);
        logger.dbg("[%s] find response: ", StageReceiver.this, response);
        return response;

      } else if (request instanceof CreateActorRequest) {
        logger.dbg("[%s] handling create request: ", StageReceiver.this, request);
        final RemoteResponse response = createHandler.handle((CreateActorRequest) request);
        logger.dbg("[%s] create response: ", StageReceiver.this, response);
        return response;

      } else if (request instanceof DismissActorRequest) {
        logger.dbg("[%s] handling dismiss request: ", StageReceiver.this, request);
        final RemoteResponse response = dismissHandler.handle((DismissActorRequest) request);
        logger.dbg("[%s] dismiss response: ", StageReceiver.this, response);
        return response;

      } else if (request instanceof UploadRequest) {
        logger.dbg("[%s] handling upload request: ", StageReceiver.this, request);
        final RemoteResponse response = uploadHandler.handle((UploadRequest) request);
        logger.dbg("[%s] upload response: ", StageReceiver.this, response);
        return response;
      }
      throw new UnsupportedOperationException("request: " + request);
    }
  }

  private class SenderRole extends Role {

    private final SenderID senderId;

    private SenderRole(@NotNull final SenderID senderId) {
      this.senderId = senderId;
    }

    @NotNull
    public Behavior getBehavior(@NotNull final String id) {
      return new AbstractBehavior() {

        public void onMessage(final Object message, @NotNull final Envelop envelop,
            @NotNull final Agent agent) throws Exception {
          final Actor sender = envelop.getSender();
          final ActorID actorID =
              new ActorID().withActorId(sender.getId()).withInstanceId(getOrAddInstanceId(sender));
          final HashMap<SenderID, SenderActor> senderIdToActor = StageReceiver.this.senderIdToActor;
          synchronized (sendersMutex) {
            final SenderID senderID = new SenderID(actorID, null);
            SenderActor senderActor = senderIdToActor.get(senderID);
            if (senderActor == null) {
              senderActor = new SenderActor(sender, senderID);
              senderIdToActor.put(senderID, senderActor);
              lruSenders.add(senderActor);

            } else {
              final LinkedList<SenderActor> lruSenders = StageReceiver.this.lruSenders;
              lruSenders.remove(senderActor);
              lruSenders.add(senderActor.refresh());
            }
            senderActor = senderIdToActor.get(senderId);
            if (senderActor != null) {
              final LinkedList<SenderActor> lruSenders = StageReceiver.this.lruSenders;
              lruSenders.remove(senderActor);
              lruSenders.add(senderActor.refresh());
            }
            flushSenders();
          }
          final SenderID senderId = SenderRole.this.senderId;
          getMessageSender().send(new MessageRequest().withActorID(senderId.actorID)
              .withSenderActorID(actorID)
              .withHeaders(envelop.getHeaders())
              .withMessageData(RawData.wrap(serializer.serialize(message)))
              .withSentTimestamp(envelop.getSentAt()), senderId.senderId);
          envelop.preventReceipt();
        }

        @Override
        public void onStop(@NotNull final Agent agent) throws Exception {
          if (agent.isDismissed()) {
            synchronized (sendersMutex) {
              final SenderActor senderActor = senderIdToActor.remove(senderId);
              lruSenders.remove(senderActor);
            }

            final SenderID senderId = SenderRole.this.senderId;
            getMessageSender().send(new DismissActorRequest().withActorID(senderId.actorID),
                senderId.senderId);
          }
        }
      };
    }

    @NotNull
    @Override
    public ExecutorService getExecutorService(@NotNull final String id) {
      return executorService;
    }
  }

  private class UploadHandler implements RequestHandler<UploadRequest> {

    @NotNull
    public UploadResponse handle(@NotNull final UploadRequest request) {
      try {
        classLoader.register(request.getResources());

      } catch (final Exception e) {
        logger.err(e, "[%s] error while handling upload request", StageReceiver.this);
        return new UploadResponse().withError(e);
      }
      return new UploadResponse();
    }
  }
}
