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
import java.util.regex.Pattern;

import dm.shakespeare.Stage;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.ActorSet;
import dm.shakespeare.actor.Role;
import dm.shakespeare.function.Tester;
import dm.shakespeare.log.LogPrinters;
import dm.shakespeare.log.Logger;
import dm.shakespeare.remote.config.StageConfig;
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
import dm.shakespeare.remote.transport.RemoteRequest;
import dm.shakespeare.remote.transport.RemoteResponse;
import dm.shakespeare.remote.util.SerializableData;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.WeakValueHashMap;

/**
 * Created by davide-maestroni on 06/04/2019.
 */
public class StageReceiver {

  public static final String KEY_CLASSLOADER_CLASS = "sks.classloader.class";
  public static final String KEY_CLASSLOADER_DIR = "sks.classloader.dir";
  public static final String KEY_CLASSLOADER_PROTECTION_DOMAIN_CLASS =
      "sks.classloader.protection.domain.class";
  public static final String KEY_CONNECTOR_CLASS = "sks.connector.class";
  public static final String KEY_LOGGER_CLASS = "sks.logger.class";
  public static final String KEY_LOGGER_NAME = "sks.logger.name";
  public static final String KEY_REMOTE_CREATE_ENABLE = "sks.remote.create.enable";
  public static final String KEY_REMOTE_DISMISS_ENABLE = "sks.remote.dismiss.enable";
  public static final String KEY_REMOTE_MESSAGES_ENABLE = "sks.remote.messages.enable";
  public static final String KEY_REMOTE_RESOURCES_ENABLE = "sks.remote.resources.enable";
  public static final String KEY_REMOTE_ROLES_ENABLE = "sks.remote.roles.enable";
  public static final String KEY_SERIALIZER_BLACKLIST = "sks.serializer.black.list";
  public static final String KEY_SERIALIZER_CLASS = "sks.serializer.class";
  public static final String KEY_SERIALIZER_WHITELIST = "sks.serializer.white.list";

  private final WeakValueHashMap<String, Actor> actors = new WeakValueHashMap<String, Actor>();
  private final RemoteClassLoader classLoader;
  private final Object connectionMutex = new Object();
  private final Connector connector;
  private final Object idsMutex = new Object();
  private final WeakHashMap<Actor, String> instances = new WeakHashMap<Actor, String>();
  private final Logger logger;
  private final boolean remoteCreateEnabled;
  private final boolean remoteDismissEnabled;
  private final boolean remoteRolesEnabled;
  private final WeakValueHashMap<ActorID, Actor> senders = new WeakValueHashMap<ActorID, Actor>();
  private final Object sendersMutex = new Object();
  private final Serializer serializer;
  private final Stage stage;

  private Sender sender;

  public StageReceiver(@NotNull final StageConfig config, @NotNull final Stage stage) {
    this.stage = ConstantConditions.notNull("stage", stage);
    // connector
    final Connector connector =
        (this.connector = config.getOption(Connector.class, KEY_CONNECTOR_CLASS));
    if (connector == null) {
      throw new IllegalArgumentException("missing connector configuration");
    }
    // serializer
    final Serializer serializer = config.getOption(Serializer.class, KEY_SERIALIZER_CLASS);
    this.serializer = (serializer != null) ? serializer : new JavaSerializer();
    @SuppressWarnings("unchecked") final List<String> whitelist =
        config.getOption(List.class, KEY_SERIALIZER_WHITELIST);
    if (whitelist != null) {
      this.serializer.whitelist(whitelist);
    }
    @SuppressWarnings("unchecked") final List<String> blacklist =
        config.getOption(List.class, KEY_SERIALIZER_BLACKLIST);
    if (blacklist != null) {
      this.serializer.blacklist(blacklist);
    }
    // classloader
    final ClassLoader classLoader = config.getOption(ClassLoader.class, KEY_CLASSLOADER_CLASS);
    final File container = config.getOption(File.class, KEY_CLASSLOADER_DIR);
    final ProtectionDomain protectionDomain =
        config.getOption(ProtectionDomain.class, KEY_CLASSLOADER_PROTECTION_DOMAIN_CLASS);
    if (classLoader != null) {
      this.classLoader = new RemoteClassLoader(classLoader, container, protectionDomain);

    } else {
      this.classLoader = new RemoteClassLoader(container, protectionDomain);
    }
    // logger
    final Logger logger = config.getOption(Logger.class, KEY_LOGGER_CLASS);
    if (logger != null) {
      this.logger = logger;

    } else {
      final String loggerName = config.getOption(String.class, KEY_LOGGER_NAME);
      this.logger = new Logger(LogPrinters.javaLoggingPrinter(
          isNotEmpty(loggerName) ? loggerName : getClass().getName()));
    }
    // options
    final Boolean remoteRolesEnabled = config.getOption(Boolean.class, KEY_REMOTE_ROLES_ENABLE);
    this.remoteRolesEnabled = (remoteRolesEnabled != null) ? remoteRolesEnabled : false;
    final Boolean remoteCreateEnabled = config.getOption(Boolean.class, KEY_REMOTE_CREATE_ENABLE);
    this.remoteCreateEnabled = (remoteCreateEnabled != null) ? remoteCreateEnabled : false;
    final Boolean remoteDismissEnabled = config.getOption(Boolean.class, KEY_REMOTE_DISMISS_ENABLE);
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
  private RemoteResponse handleCreate(@NotNull final CreateActorRequest request) {
    final String actorId = request.getActorId();
    if ((actorId == null)) {
      return new CreateActorResponse().withError(new IllegalArgumentException());
    }
    final RemoteClassLoader classLoader = this.classLoader;
    if (remoteCreateEnabled) {
      final SerializableData roleData = request.getRoleData();
      try {
        Set<String> dependencies = null;
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
        return new CreateActorResponse().withError(e);
      }
    }
    return new CreateActorResponse().withError(new UnsupportedOperationException());
  }

  @NotNull
  private RemoteResponse handleDismiss(@NotNull final DismissActorRequest request) {
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
    return new CreateActorResponse().withError(new UnsupportedOperationException());
  }

  @NotNull
  private RemoteResponse handleFind(@NotNull final FindRequest request) {
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
        final String instanceId;
        synchronized (idsMutex) {
          instanceId = instances.get(actor);
        }
        actorIds.add(new ActorID().withActorId(actor.getId()).withInstanceId(instanceId));
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
        final String instanceId;
        synchronized (idsMutex) {
          instanceId = instances.get(actor);
        }
        return new FindResponse().withActorIDs(Collections.singleton(
            new ActorID().withActorId(actor.getId()).withInstanceId(instanceId)));
      }
      return new FindResponse().withActorIDs(Collections.<ActorID>emptySet());

    } else if (filterType == FilterType.EXACT) {
      final Actor actor = stage.get(pattern);
      if (actor != null) {
        final String instanceId;
        synchronized (idsMutex) {
          instanceId = instances.get(actor);
        }
        return new FindResponse().withActorIDs(Collections.singleton(
            new ActorID().withActorId(actor.getId()).withInstanceId(instanceId)));
      }
      return new FindResponse().withActorIDs(Collections.<ActorID>emptySet());
    }
    return new CreateActorResponse().withError(new IllegalArgumentException());
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
      }
      throw new UnsupportedOperationException();
    }
  }
}
