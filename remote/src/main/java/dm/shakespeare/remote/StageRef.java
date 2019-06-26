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
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import dm.shakespeare.Stage;
import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.ActorSet;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Behavior.Agent;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.actor.Role;
import dm.shakespeare.function.Tester;
import dm.shakespeare.log.LogPrinters;
import dm.shakespeare.log.Logger;
import dm.shakespeare.remote.config.LocalConfig;
import dm.shakespeare.remote.config.StageConfig;
import dm.shakespeare.remote.io.RawData;
import dm.shakespeare.remote.io.Serializer;
import dm.shakespeare.remote.message.Rejection;
import dm.shakespeare.remote.transport.ActorID;
import dm.shakespeare.remote.transport.Connector;
import dm.shakespeare.remote.transport.Connector.Receiver;
import dm.shakespeare.remote.transport.Connector.Sender;
import dm.shakespeare.remote.transport.CreateActorContinue;
import dm.shakespeare.remote.transport.CreateActorRequest;
import dm.shakespeare.remote.transport.CreateActorResponse;
import dm.shakespeare.remote.transport.DismissActorRequest;
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
import dm.shakespeare.util.CQueue;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 05/27/2019.
 */
public class StageRef extends Stage {

  private static final Object resourcesMutex = new Object();

  private static Map<String, File> resourceFiles = Collections.emptyMap();

  private final long actorsFullSyncTime;
  private final long actorsPartialSyncTime;
  private final Object connectionMutex = new Object();
  private final Connector connector;
  private final ExecutorService executorService;
  private final AtomicLong lastSyncTime = new AtomicLong();
  private final Logger logger;
  private final Syncer presync;
  private final String remoteId;
  private final Integer sendersCacheSize;
  private final Long sendersCacheTimeout;
  private final Serializer serializer;

  private ScheduledExecutorService scheduledExecutorService;
  private Sender sender;

  public StageRef(@NotNull final StageConfig config) {
    this.remoteId = config.getOption(String.class, LocalConfig.KEY_REMOTE_ID);
    // connector
    final Connector connector =
        (this.connector = config.getOption(Connector.class, LocalConfig.KEY_CONNECTOR_CLASS));
    if (connector == null) {
      throw new IllegalArgumentException("missing connector configuration");
    }
    // serializer
    final Serializer serializer =
        config.getOption(Serializer.class, LocalConfig.KEY_SERIALIZER_CLASS);
    this.serializer = (serializer != null) ? serializer : new JavaSerializer();
    @SuppressWarnings("unchecked") final List<String> whitelist =
        config.getOption(List.class, LocalConfig.KEY_SERIALIZER_WHITELIST);
    if (whitelist != null) {
      this.serializer.whitelist(whitelist);
    }
    @SuppressWarnings("unchecked") final List<String> blacklist =
        config.getOption(List.class, LocalConfig.KEY_SERIALIZER_BLACKLIST);
    if (blacklist != null) {
      this.serializer.blacklist(blacklist);
    }
    // executor
    final ExecutorService executorService =
        config.getOption(ExecutorService.class, LocalConfig.KEY_EXECUTOR_CLASS);
    if (executorService != null) {
      this.executorService = executorService;

    } else {
      this.executorService = Role.defaultExecutorService();
    }
    // logger
    final Logger logger = config.getOption(Logger.class, LocalConfig.KEY_LOGGER_CLASS);
    if (logger != null) {
      this.logger = logger;

    } else {
      final String loggerName = config.getOption(String.class, LocalConfig.KEY_LOGGER_NAME);
      this.logger = new Logger(LogPrinters.javaLoggingPrinter(
          isNotEmpty(loggerName) ? loggerName : getClass().getName()));
    }
    // options
    final Integer sendersCacheSize =
        config.getOption(Integer.class, LocalConfig.KEY_SENDERS_CACHE_MAX_SIZE);
    if (sendersCacheSize != null) {
      this.sendersCacheSize = sendersCacheSize;
    } else {
      this.sendersCacheSize = 1000;
    }
    final Long sendersCacheTimeout =
        config.getOption(Long.class, LocalConfig.KEY_SENDERS_CACHE_TIMEOUT);
    if (sendersCacheTimeout != null) {
      this.sendersCacheTimeout = sendersCacheTimeout;
    } else {
      this.sendersCacheTimeout = TimeUnit.MINUTES.toMillis(15);
    }
    // tasks
    final Long actorsPartialSyncTime =
        config.getOption(Long.class, LocalConfig.KEY_ACTORS_PART_SYNC_TIME);
    if (actorsPartialSyncTime != null) {
      this.actorsPartialSyncTime = actorsPartialSyncTime;
    } else {
      this.actorsPartialSyncTime = 0;
    }
    final Long actorsFullSyncTime =
        config.getOption(Long.class, LocalConfig.KEY_ACTORS_FULL_SYNC_TIME);
    if (actorsFullSyncTime != null) {
      this.actorsFullSyncTime = actorsFullSyncTime;
    } else {
      this.actorsFullSyncTime = -1;
    }
    final long fullSyncTime = this.actorsFullSyncTime;
    final long partialSyncTime = this.actorsPartialSyncTime;
    if (fullSyncTime == 0) {
      presync = new Syncer() {

        public boolean sync() throws Exception {
          syncActors(System.currentTimeMillis());
          return true;
        }
      };

    } else {
      final Syncer partialSync;
      if (partialSyncTime < 0) {
        partialSync = new Syncer() {

          public boolean sync() {
            return true;
          }
        };

      } else if (partialSyncTime > 0) {
        partialSync = new Syncer() {

          public boolean sync() {
            final long currentTimeMillis = System.currentTimeMillis();
            final long lastSyncTime = StageRef.this.lastSyncTime.get();
            return (lastSyncTime > (currentTimeMillis - partialSyncTime));
          }
        };

      } else {
        partialSync = new Syncer() {

          public boolean sync() {
            return false;
          }
        };
      }

      if (fullSyncTime < 0) {
        presync = partialSync;

      } else {
        presync = new Syncer() {

          public boolean sync() throws Exception {
            final long currentTimeMillis = System.currentTimeMillis();
            final long lastSyncTime = StageRef.this.lastSyncTime.get();
            if (lastSyncTime <= (currentTimeMillis - fullSyncTime)) {
              syncActors(currentTimeMillis);
              return true;
            }
            return partialSync.sync();
          }
        };
      }
    }
  }

  private static boolean isNotEmpty(@Nullable final String string) {
    return ((string != null) && (string.trim().length() > 0));
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
      fileMap.put(file.getPath().substring(root.getPath().length()), file);
    }
  }

  private static void registerFiles() throws IOException {
    synchronized (resourcesMutex) {
      if (resourceFiles.isEmpty()) {
        final HashMap<String, File> fileMap = new HashMap<String, File>();
        final Enumeration<URL> resources = StageRef.class.getClassLoader().getResources("");
        while (resources.hasMoreElements()) {
          final URL url = resources.nextElement();
          final File root = new File(url.getPath());
          registerFile(root, root, fileMap);
        }
        resourceFiles = fileMap;
      }
    }
  }

  public void connect() throws Exception {
    final Sender sender = connector.connect(new Receiver() {

      @NotNull
      public RemoteResponse receive(@NotNull final RemoteRequest request) {
        if (request instanceof MessageRequest) {
          final MessageRequest messageRequest = (MessageRequest) request;
          final ActorID actorID = messageRequest.getSenderActorID();
          if (actorID != null) {
            final String actorId = actorID.getActorId();
            if (actorId != null) {
              final Actor actor = StageRef.super.get(actorId);
              if (actor != null) {
                actor.tell(request, Headers.EMPTY, Stage.STAND_IN);
                return new MessageResponse();
              }
            }
          }
        }
        return new MessageResponse().withError(new IllegalArgumentException("invalid request"));
      }
    });
    synchronized (connectionMutex) {
      if (this.sender != null) {
        throw new IllegalStateException("stage is already connected");
      }
      this.sender = sender;
      registerFiles();
      final ScheduledExecutorService executorService =
          (this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor());
      final long fullSyncTime = this.actorsFullSyncTime;
      if (fullSyncTime > 0) {
        executorService.schedule(new Runnable() {

          public void run() {
            final long currentTimeMillis = System.currentTimeMillis();
            final long lastSyncTime = StageRef.this.lastSyncTime.get();
            final long nextSyncTime;
            if (lastSyncTime <= (currentTimeMillis - fullSyncTime)) {
              try {
                syncActors(currentTimeMillis);

              } catch (final Exception e) {
                logger.err(e, "failed to sync remote actors");
              }
              nextSyncTime = currentTimeMillis + fullSyncTime;

            } else {
              nextSyncTime = lastSyncTime + fullSyncTime;
            }
            executorService.schedule(this, nextSyncTime, TimeUnit.MILLISECONDS);
          }
        }, fullSyncTime, TimeUnit.MILLISECONDS);
      }
    }
  }

  public void disconnect() {
    final Sender sender;
    synchronized (connectionMutex) {
      sender = this.sender;
      this.sender = null;
      final ScheduledExecutorService executorService = this.scheduledExecutorService;
      if (executorService != null) {
        executorService.shutdown();
        this.scheduledExecutorService = null;
      }
    }

    if (sender != null) {
      sender.disconnect();
    }
  }

  @NotNull
  @Override
  public ActorSet findAll(@NotNull final Pattern idPattern) {
    try {
      if (!presync.sync()) {
        final FindResponse response = (FindResponse) getSender().send(
            new FindRequest().withFilterType(FilterType.ALL).withPattern(idPattern.pattern()),
            remoteId);
        syncActors(response, false);
      }

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
    return super.findAll(idPattern);
  }

  @NotNull
  @Override
  public ActorSet findAll(@NotNull final Tester<? super Actor> tester) {
    try {
      if (!presync.sync()) {
        final FindResponse response = (FindResponse) getSender().send(
            new FindRequest().withFilterType(FilterType.ALL)
                .withTester(ConstantConditions.notNull("tester", tester)), remoteId);
        syncActors(response, false);
      }

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
    return super.findAll(tester);
  }

  @Nullable
  @Override
  public Actor findAny(@NotNull final Pattern idPattern) {
    try {
      if (!presync.sync()) {
        final FindResponse response = (FindResponse) getSender().send(
            new FindRequest().withFilterType(FilterType.ANY).withPattern(idPattern.pattern()),
            remoteId);
        syncActors(response, false);
      }

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
    return super.findAny(idPattern);
  }

  @Nullable
  @Override
  public Actor findAny(@NotNull final Tester<? super Actor> tester) {
    try {
      if (!presync.sync()) {
        final FindResponse response = (FindResponse) getSender().send(
            new FindRequest().withFilterType(FilterType.ANY)
                .withTester(ConstantConditions.notNull("tester", tester)), remoteId);
        syncActors(response, false);
      }

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
    return super.findAny(tester);
  }

  @Nullable
  @Override
  public Actor get(@NotNull final String id) {
    try {
      if (!presync.sync()) {
        final FindResponse response = (FindResponse) getSender().send(
            new FindRequest().withFilterType(FilterType.EXACT)
                .withPattern(ConstantConditions.notNull("id", id)), remoteId);
        syncActors(response, false);
      }

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
    return super.get(id);
  }

  @NotNull
  @Override
  public ActorSet getAll() {
    try {
      if (!presync.sync()) {
        syncActors(System.currentTimeMillis());
      }

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
    return super.getAll();
  }

  @NotNull
  @Override
  protected Actor buildActor(@NotNull final String id, @NotNull final Role role) throws Exception {
    final HashMap<String, RawData> resources = new HashMap<String, RawData>();
    final Map<String, File> resourceFiles = StageRef.resourceFiles;
    final byte[] data = serializer.serialize(role);
    RemoteResponse response = getSender().send(new CreateActorRequest().withActorId(id)
        .withRoleData(RawData.wrap(data))
        .withResources(resources), remoteId);
    while (response instanceof CreateActorContinue) {
      resources.clear();
      final List<String> missingResources = ((CreateActorContinue) response).getResourcePaths();
      if ((missingResources == null) || missingResources.isEmpty()) {
        throw new IllegalStateException("invalid response from remote stage: missing resources");
      }

      for (final String missingResource : missingResources) {
        final File resourceFile = resourceFiles.get(missingResource);
        if (resourceFile != null) {
          resources.put(missingResource, RawData.wrap(resourceFile));
        }
      }

      if (resources.isEmpty()) {
        throw new IllegalStateException(
            "invalid response from remote stage: unknown resources: " + missingResources);
      }
      response = getSender().send(new CreateActorRequest().withActorId(id)
          .withRoleData(RawData.wrap(data))
          .withResources(resources), remoteId);
    }
    final CreateActorResponse actorResponse = (CreateActorResponse) response;
    final Throwable error = actorResponse.getError();
    if (error != null) {
      if (error instanceof Exception) {
        throw (Exception) error;
      }

      throw new Exception(error);
    }
    final ActorID actorID = actorResponse.getActorID();
    return super.buildActor(id, new RemoteRole((actorID != null) ? actorID.getInstanceId() : null));
  }

  public void uploadResources(@NotNull final String pathRegex) {
    final Pattern pattern = Pattern.compile(pathRegex);
    final HashMap<String, RawData> resources = new HashMap<String, RawData>();
    final Map<String, File> resourceFiles = StageRef.resourceFiles;
    for (final Entry<String, File> entry : resourceFiles.entrySet()) {
      final String path = entry.getKey();
      if (pattern.matcher(path).matches()) {
        resources.put(path, RawData.wrap(entry.getValue()));
      }
    }
    if (!resources.isEmpty()) {
      Throwable error;
      try {
        final UploadResponse response =
            (UploadResponse) getSender().send(new UploadRequest().withResources(resources),
                remoteId);
        error = response.getError();

      } catch (final Exception e) {
        error = e;
      }

      if (error != null) {
        if (error instanceof RuntimeException) {
          throw (RuntimeException) error;
        }

        throw new RuntimeException(error);
      }
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

  private void syncActors(@NotNull final FindResponse response, final boolean retainAll) {
    final HashMap<String, ActorID> ids = new HashMap<String, ActorID>();
    for (final ActorID actorID : response.getActorIDs()) {
      ids.put(actorID.getActorId(), actorID);
    }

    for (final Actor actor : super.getAll()) {
      final ActorID actorID = ids.get(actor.getId());
      if (actorID != null) {
        actor.tell(new RefreshInstanceId(actorID.getInstanceId()), Headers.EMPTY, Stage.STAND_IN);

      } else if (retainAll) {
        actor.dismiss();
      }
    }
  }

  private void syncActors(final long syncMillis) throws Exception {
    final FindResponse response =
        (FindResponse) getSender().send(new FindRequest().withFilterType(FilterType.ALL), remoteId);
    syncActors(response, true);
    lastSyncTime.set(syncMillis);
  }

  private interface Syncer {

    boolean sync() throws Exception;
  }

  private static class RefreshInstanceId {

    private final String instanceId;

    private RefreshInstanceId(final String instanceId) {
      this.instanceId = instanceId;
    }

    String getInstanceId() {
      return instanceId;
    }
  }

  private static class SenderActor {

    private final Actor actor;
    private final String instanceId;
    private final long timestamp;

    private SenderActor(@NotNull final Actor actor, @NotNull final String instanceId) {
      this.actor = actor;
      this.instanceId = instanceId;
      timestamp = System.currentTimeMillis();
    }

    @NotNull
    Actor getActor() {
      return actor;
    }

    @NotNull
    String getInstanceId() {
      return instanceId;
    }

    long getTimestamp() {
      return timestamp;
    }
  }

  private class RemoteRole extends Role {

    private final HashMap<Actor, SenderActor> actors = new HashMap<Actor, SenderActor>();
    private final String instanceId;
    private final HashMap<String, SenderActor> instanceIds = new HashMap<String, SenderActor>();
    private final CQueue<SenderActor> senders = new CQueue<SenderActor>();

    private RemoteRole(final String instanceId) {
      this.instanceId = instanceId;
    }

    @NotNull
    public Behavior getBehavior(@NotNull final String id) {
      return new AbstractBehavior() {

        public void onMessage(final Object message, @NotNull final Envelop envelop,
            @NotNull final Agent agent) throws Exception {
          if (message instanceof MessageRequest) {
            final MessageRequest request = (MessageRequest) message;
            if (!receive(request, agent)) {
              sendRejection(request);
            }
            flushSenders();

          } else if (message instanceof RefreshInstanceId) {
            final String instanceId = RemoteRole.this.instanceId;
            final String refreshInstanceId = ((RefreshInstanceId) message).getInstanceId();
            if ((refreshInstanceId != null) ? !refreshInstanceId.equals(instanceId)
                : (instanceId != null)) {
              agent.getSelf().dismiss();
            }

          } else {
            flushSenders();
            if (!send(message, envelop, agent)) {
              final Headers headers = envelop.getHeaders();
              if (headers.getReceiptId() != null) {
                envelop.getSender()
                    .tell(new Rejection(message, headers), headers.threadOnly(), agent.getSelf());
              }
            }
          }
        }

        @Override
        public void onStop(@NotNull final Agent agent) throws Exception {
          if (agent.isDismissed()) {
            getSender().send(new DismissActorRequest().withActorID(
                new ActorID().withActorId(id).withInstanceId(instanceId))
                .withMayInterruptIfRunning(Thread.currentThread().isInterrupted()), remoteId);
          }
        }
      };
    }

    @NotNull
    @Override
    public ExecutorService getExecutorService(@NotNull final String id) {
      return executorService;
    }

    private void flushSenders() {
      final CQueue<SenderActor> senders = this.senders;
      final HashMap<Actor, SenderActor> actors = this.actors;
      final HashMap<String, SenderActor> instanceIds = this.instanceIds;
      int toRemove = senders.size() - sendersCacheSize;
      final long timeout = System.currentTimeMillis() - sendersCacheTimeout;
      final Iterator<SenderActor> iterator = senders.iterator();
      while (iterator.hasNext()) {
        final SenderActor senderActor = iterator.next();
        if ((toRemove <= 0) && (senderActor.getTimestamp() >= timeout)) {
          break;
        }
        actors.remove(senderActor.getActor());
        instanceIds.remove(senderActor.getInstanceId());
        iterator.remove();
        --toRemove;
      }
    }

    private boolean receive(@NotNull final MessageRequest request, @NotNull final Agent agent) {
      final ActorID senderActorID = request.getSenderActorID();
      if (senderActorID != null) {
        final String instanceId = RemoteRole.this.instanceId;
        final String refreshInstanceId = senderActorID.getInstanceId();
        if ((refreshInstanceId != null) ? !refreshInstanceId.equals(instanceId)
            : (instanceId != null)) {
          agent.getSelf().dismiss();
          return false;
        }

      } else {
        return false;
      }
      final ActorID targetActorID = request.getActorID();
      if (targetActorID == null) {
        return false;
      }
      final SenderActor senderActor = instanceIds.get(targetActorID.getInstanceId());
      if (senderActor == null) {
        return false;
      }
      final CQueue<SenderActor> senders = RemoteRole.this.senders;
      senders.remove(senderActor);
      senders.add(senderActor);
      try {
        final RawData messageData = request.getMessageData();
        final Object object;
        if (messageData != null) {
          object = serializer.deserialize(messageData, StageRef.class.getClassLoader());

        } else {
          object = null;
        }
        final Headers headers = request.getHeaders();
        senderActor.getActor()
            .tell(object,
                ((headers != null) ? headers : Headers.EMPTY).asSentAt(request.getSentTimestamp()),
                agent.getSelf());

      } catch (final Exception e) {
        logger.wrn(e, "failed to deserialize message");
        sendRejection(request);
      }
      return true;
    }

    private boolean send(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) throws Exception {
      final String senderInstanceId;
      final Actor sender = envelop.getSender();
      if (sender.equals(StageRef.super.get(sender.getId()))) {
        senderInstanceId = null;

      } else {
        final HashMap<Actor, SenderActor> actors = this.actors;
        final HashMap<String, SenderActor> instanceIds = this.instanceIds;
        final CQueue<SenderActor> senders = RemoteRole.this.senders;
        SenderActor senderActor = actors.get(sender);
        if (senderActor != null) {
          senderInstanceId = senderActor.getInstanceId();
          senders.remove(senderActor);

        } else {
          senderInstanceId = "local:" + UUID.randomUUID().toString();
          senderActor = new SenderActor(sender, senderInstanceId);
          actors.put(sender, senderActor);
          instanceIds.put(senderInstanceId, senderActor);
        }
        senders.add(senderActor);
      }
      RemoteResponse response;
      try {
        final byte[] data = serializer.serialize(message);
        final HashMap<String, RawData> resources = new HashMap<String, RawData>();
        final Map<String, File> resourceFiles = StageRef.resourceFiles;
        final ActorID actorID =
            new ActorID().withActorId(agent.getSelf().getId()).withInstanceId(instanceId);
        final ActorID senderID =
            new ActorID().withActorId(sender.getId()).withInstanceId(senderInstanceId);
        final String remoteId = StageRef.this.remoteId;
        response = getSender().send(new MessageRequest().withActorID(actorID)
            .withMessageData(RawData.wrap(data))
            .withHeaders(envelop.getHeaders())
            .withSenderActorID(senderID)
            .withResources(resources), remoteId);
        while (response instanceof MessageContinue) {
          resources.clear();
          final List<String> missingResources = ((MessageContinue) response).getResourcePaths();
          if ((missingResources == null) || missingResources.isEmpty()) {
            logger.err("invalid response from remote stage: missing resources");
            return false;
          }

          for (final String missingResource : missingResources) {
            final File resourceFile = resourceFiles.get(missingResource);
            if (resourceFile != null) {
              resources.put(missingResource, RawData.wrap(resourceFile));
            }
          }

          if (resources.isEmpty()) {
            logger.err("invalid response from remote stage: unknown resources: %s",
                missingResources);
            return false;
          }
          response = getSender().send(new MessageRequest().withActorID(actorID)
              .withMessageData(RawData.wrap(data))
              .withHeaders(envelop.getHeaders())
              .withSenderActorID(senderID)
              .withResources(resources), remoteId);
        }

      } catch (final Exception e) {
        logger.wrn(e, "failed to deserialize message");
        return false;
      }

      final Throwable error = ((MessageResponse) response).getError();
      if (error != null) {
        if (error instanceof Exception) {
          throw (Exception) error;
        }

        throw new Exception(error);
      }
      return true;
    }

    private void sendRejection(@NotNull final MessageRequest request) {
      final Headers headers = request.getHeaders();
      if ((headers != null) && (headers.getReceiptId() != null)) {
        try {
          getSender().send(new MessageRequest().withActorID(request.getSenderActorID())
              .withSenderActorID(request.getSenderActorID())
              .withMessageData(RawData.wrap(serializer.serialize(new Rejection(null, headers))))
              .withHeaders(headers.threadOnly()), remoteId);

        } catch (final Exception e) {
          logger.err(e, "failed to send rejection message");
        }
      }
    }
  }
}
