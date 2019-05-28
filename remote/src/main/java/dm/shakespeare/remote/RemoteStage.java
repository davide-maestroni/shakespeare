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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import dm.shakespeare.Stage;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.ActorSet;
import dm.shakespeare.actor.Role;
import dm.shakespeare.function.Tester;
import dm.shakespeare.log.LogPrinters;
import dm.shakespeare.log.Logger;
import dm.shakespeare.remote.config.StageConfig;
import dm.shakespeare.remote.transport.ActorUUID;
import dm.shakespeare.remote.transport.Connector;
import dm.shakespeare.remote.transport.Connector.Receiver;
import dm.shakespeare.remote.transport.Connector.Sender;
import dm.shakespeare.remote.transport.FindRequest;
import dm.shakespeare.remote.transport.FindRequest.FilterType;
import dm.shakespeare.remote.transport.FindResponse;
import dm.shakespeare.remote.transport.RemoteRequest;
import dm.shakespeare.remote.transport.RemoteResponse;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 05/27/2019.
 */
public class RemoteStage extends Stage {

  private final long actorsMaxSyncTime;
  private final long actorsMinSyncTime;
  private final Connector connector;
  private final AtomicLong lastSyncTime = new AtomicLong();
  private final Logger logger;
  private final Object mutex = new Object();
  private final Syncer presync;
  private final String remoteId;
  private final Serializer serializer;

  private ScheduledExecutorService executorService;
  private Sender sender;

  public RemoteStage(@NotNull final Properties properties) {
    this(StageConfig.from(properties));
  }

  public RemoteStage(@NotNull final StageConfig config) {
    this.remoteId = config.getOption(String.class, "sks.remote.id");
    // connector
    final Connector connector =
        (this.connector = config.getOption(Connector.class, "sks.connector.class"));
    if (connector == null) {
      throw new IllegalArgumentException("missing connector configuration");
    }
    // serializer
    final Serializer serializer = config.getOption(Serializer.class, "sks.serializer.class");
    this.serializer = (serializer != null) ? serializer : new JavaSerializer();
    final String whitelist = config.getOption(String.class, "sks.serializer.whitelist");
    if (isNotEmpty(whitelist)) {
      final List<String> classNames = Arrays.asList(whitelist.split("\\s,\\s"));
      this.serializer.whitelist(classNames);
    }
    final String blacklist = config.getOption(String.class, "sks.serializer.blacklist");
    if (isNotEmpty(blacklist)) {
      final List<String> classNames = Arrays.asList(blacklist.split("\\s,\\s"));
      this.serializer.blacklist(classNames);
    }
    // logger
    final Logger logger = config.getOption(Logger.class, "sks.logger.class");
    if (logger != null) {
      this.logger = logger;

    } else {
      final String loggerName = config.getOption(String.class, "sks.logger.name");
      this.logger = new Logger(LogPrinters.javaLoggingPrinter(
          isNotEmpty(loggerName) ? loggerName : getClass().getName()));
    }
    // tasks
    final String actorsMinSyncTime = config.getOption(String.class, "sks.actors.sync.min");
    if (actorsMinSyncTime != null) {
      this.actorsMinSyncTime = Long.parseLong(actorsMinSyncTime);
    } else {
      this.actorsMinSyncTime = -1;
    }
    final String actorsMaxSyncTime = config.getOption(String.class, "sks.actors.sync.max");
    if (actorsMaxSyncTime != null) {
      this.actorsMaxSyncTime = Long.parseLong(actorsMaxSyncTime);
    } else {
      this.actorsMaxSyncTime = -1;
    }
    final long minSyncTime = this.actorsMinSyncTime;
    if (minSyncTime < 0) {
      presync = new Syncer() {

        public boolean sync() {
          return false;
        }
      };

    } else if (minSyncTime > 0) {
      presync = new Syncer() {

        public boolean sync() throws Exception {
          final long currentTimeMillis = System.currentTimeMillis();
          final long syncTime = lastSyncTime.get();
          if (syncTime <= (currentTimeMillis - minSyncTime)) {
            syncActors();
            lastSyncTime.set(currentTimeMillis);
            return true;
          }
          return false;
        }
      };

    } else {
      presync = new Syncer() {

        public boolean sync() throws Exception {
          final long currentTimeMillis = System.currentTimeMillis();
          syncActors();
          lastSyncTime.set(currentTimeMillis);
          return true;
        }
      };
    }
  }

  private static boolean isNotEmpty(@Nullable final String string) {
    return ((string != null) && (string.trim().length() > 0));
  }

  public void connect() throws Exception {
    final Sender sender = connector.connect(new Receiver() {

      @NotNull
      public RemoteResponse receive(@NotNull final RemoteRequest request) throws Exception {
        return null;
      }
    });
    synchronized (mutex) {
      if (this.sender != null) {
        throw new IllegalStateException("stage is already connected");
      }
      this.sender = sender;
      final ScheduledExecutorService executorService =
          (this.executorService = Executors.newSingleThreadScheduledExecutor());
      final long maxSyncTime = this.actorsMaxSyncTime;
      if (maxSyncTime > 0) {
        executorService.schedule(new Runnable() {

          public void run() {
            final long currentTimeMillis = System.currentTimeMillis();
            final long syncTime = lastSyncTime.get();
            final long nextSyncTime;
            if (syncTime <= (currentTimeMillis - maxSyncTime)) {
              try {
                syncActors();

              } catch (final Exception e) {
                logger.err(e, "failed to sync remote actors");
              }
              nextSyncTime = currentTimeMillis + maxSyncTime;

            } else {
              nextSyncTime = syncTime + maxSyncTime;
            }
            executorService.schedule(this, nextSyncTime, TimeUnit.MILLISECONDS);
          }
        }, maxSyncTime, TimeUnit.MILLISECONDS);
      }
    }
    // TODO: 2019-05-27 get remote stage capabilities
    // sender.send();
  }

  public void disconnect() {
    final Sender sender;
    synchronized (mutex) {
      sender = this.sender;
      this.sender = null;
      final ScheduledExecutorService executorService = this.executorService;
      if (executorService != null) {
        executorService.shutdown();
        this.executorService = null;
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

  @NotNull
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
      syncActors();

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
    return super.buildActor(id, role);
  }

  @NotNull
  private Sender getSender() {
    synchronized (mutex) {
      final Sender sender = this.sender;
      if (sender == null) {
        throw new IllegalStateException("stage is already connected");
      }
      return sender;
    }
  }

  private void syncActors(@NotNull final FindResponse response, final boolean retainAll) {
    final HashMap<String, ActorUUID> uuids = new HashMap<String, ActorUUID>();
    for (final ActorUUID actorUUID : response.getActorUUIDs()) {
      uuids.put(actorUUID.getActorId(), actorUUID);
    }

    for (final Actor actor : getAll()) {
      final ActorUUID actorUUID = uuids.get(actor.getId());
      if (actorUUID != null) {
        actor.tell(new RefreshUid(actorUUID.getActorUid()), null, Stage.STAND_IN);

      } else if (retainAll) {
        actor.dismiss(false);
      }
    }
  }

  private void syncActors() throws Exception {
    final FindResponse response =
        (FindResponse) getSender().send(new FindRequest().withFilterType(FilterType.ALL), remoteId);
    syncActors(response, true);
  }

  private interface Syncer {

    boolean sync() throws Exception;
  }

  private static class RefreshUid {

    private final String actorUid;

    private RefreshUid(final String actorUid) {
      this.actorUid = actorUid;
    }

    public String getActorUid() {
      return actorUid;
    }
  }
}
