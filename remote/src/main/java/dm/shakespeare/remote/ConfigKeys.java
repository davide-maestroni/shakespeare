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

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 06/06/2019.
 */
public class ConfigKeys {

  private ConfigKeys() {
    ConstantConditions.avoid();
  }

  public static class Common {

    public static final String KEY_CONNECTOR_CLASS = "sks.connector.class";
    public static final String KEY_EXECUTOR_CLASS = "sks.executor.service.class";
    public static final String KEY_LOGGER_CLASS = "sks.logger.class";
    public static final String KEY_LOGGER_NAME = "sks.logger.name";
    public static final String KEY_SERIALIZER_BLACKLIST = "sks.serializer.black.list";
    public static final String KEY_SERIALIZER_CLASS = "sks.serializer.class";
    public static final String KEY_SERIALIZER_WHITELIST = "sks.serializer.white.list";

    private Common() {
      ConstantConditions.avoid();
    }
  }

  public static class Local extends Common {

    public static final String KEY_ACTORS_FULL_SYNC_TIME = "sks.actors.sync.full.millis";
    public static final String KEY_ACTORS_PART_SYNC_TIME = "sks.actors.sync.part.millis";
    public static final String KEY_REMOTE_ID = "sks.remote.id";
    public static final String KEY_SENDERS_CACHE_MAX_SIZE = "sks.senders.cache.max.size";
    public static final String KEY_SENDERS_CACHE_TIMEOUT = "sks.senders.cache.timeout.millis";

    private Local() {
      ConstantConditions.avoid();
    }
  }

  public static class Remote extends Common {

    public static final String KEY_CLASSLOADER_CLASS = "sks.classloader.class";
    public static final String KEY_CLASSLOADER_DIR = "sks.classloader.dir";
    public static final String KEY_CLASSLOADER_PROTECTION_DOMAIN_CLASS =
        "sks.classloader.protection.domain.class";
    public static final String KEY_REMOTE_CREATE_ENABLE = "sks.remote.create.enable";
    public static final String KEY_REMOTE_DISMISS_ENABLE = "sks.remote.dismiss.enable";
    public static final String KEY_REMOTE_MESSAGES_ENABLE = "sks.remote.messages.enable";
    public static final String KEY_REMOTE_RESOURCES_ENABLE = "sks.remote.resources.enable";
    public static final String KEY_REMOTE_ROLES_ENABLE = "sks.remote.roles.enable";

    private Remote() {
      ConstantConditions.avoid();
    }
  }
}
