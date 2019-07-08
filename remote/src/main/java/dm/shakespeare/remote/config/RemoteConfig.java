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

package dm.shakespeare.remote.config;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import dm.shakespeare.log.Logger;
import dm.shakespeare.remote.io.DataStore;
import dm.shakespeare.remote.io.Serializer;
import dm.shakespeare.remote.transport.Connector;

/**
 * Created by davide-maestroni on 06/07/2019.
 */
public class RemoteConfig extends CommonConfig {

  public static final String KEY_CLASSLOADER_CLASS = "sks.classloader.class";
  public static final String KEY_CLASSLOADER_DATA_STORE_CLASS = "sks.classloader.data.store.class";
  public static final String KEY_CLASSLOADER_DIR = "sks.classloader.dir";
  public static final String KEY_PROTECTION_DOMAIN_CLASS = "sks.protection.domain.class";
  public static final String KEY_REMOTE_CREATE_ENABLE = "sks.remote.create.enable";
  public static final String KEY_REMOTE_DISMISS_ENABLE = "sks.remote.dismiss.enable";
  public static final String KEY_REMOTE_MESSAGES_ENABLE = "sks.remote.messages.enable";
  public static final String KEY_REMOTE_RESOURCES_ENABLE = "sks.remote.resources.enable";
  public static final String KEY_REMOTE_ROLES_ENABLE = "sks.remote.roles.enable";

  public RemoteConfig() {
  }

  public RemoteConfig(@NotNull final Map<? extends String, ?> map) {
    super(map);
  }

  @NotNull
  public RemoteConfig withClassLoader(final String classLoaderClass) {
    return withOption(KEY_CLASSLOADER_CLASS, classLoaderClass);
  }

  @NotNull
  public RemoteConfig withClassLoader(final Class<? extends ClassLoader> classLoaderClass) {
    return withOption(KEY_CLASSLOADER_CLASS, classLoaderClass);
  }

  @NotNull
  public RemoteConfig withClassLoader(final ClassLoader classLoader) {
    return withOption(KEY_CLASSLOADER_CLASS, classLoader);
  }

  @NotNull
  public RemoteConfig withClassLoaderDataStore(final String dataStoreClass) {
    return withOption(KEY_CLASSLOADER_DATA_STORE_CLASS, dataStoreClass);
  }

  @NotNull
  public RemoteConfig withClassLoaderDataStore(final Class<? extends DataStore> dataStoreClass) {
    return withOption(KEY_CLASSLOADER_DATA_STORE_CLASS, dataStoreClass);
  }

  @NotNull
  public RemoteConfig withClassLoaderDataStore(final DataStore dataStore) {
    return withOption(KEY_CLASSLOADER_DATA_STORE_CLASS, dataStore);
  }

  @NotNull
  public RemoteConfig withClassLoaderDir(final String dirPath) {
    return withOption(KEY_CLASSLOADER_DIR, dirPath);
  }

  @NotNull
  public RemoteConfig withClassLoaderDir(final File dir) {
    return withOption(KEY_CLASSLOADER_DIR, dir);
  }

  @NotNull
  @Override
  public RemoteConfig withConnector(final String connectorClass) {
    super.withConnector(connectorClass);
    return this;
  }

  @NotNull
  @Override
  public RemoteConfig withConnector(final Class<? extends Connector> connectorClass) {
    super.withConnector(connectorClass);
    return this;
  }

  @NotNull
  @Override
  public RemoteConfig withConnector(final Connector connector) {
    super.withConnector(connector);
    return this;
  }

  @NotNull
  @Override
  public RemoteConfig withExecutor(final String executorClass) {
    super.withExecutor(executorClass);
    return this;
  }

  @NotNull
  @Override
  public RemoteConfig withExecutor(final Class<? extends ExecutorService> executorClass) {
    super.withExecutor(executorClass);
    return this;
  }

  @NotNull
  @Override
  public RemoteConfig withExecutor(final ExecutorService executor) {
    super.withExecutor(executor);
    return this;
  }

  @NotNull
  @Override
  public RemoteConfig withLogger(final String loggerClass) {
    super.withLogger(loggerClass);
    return this;
  }

  @NotNull
  @Override
  public RemoteConfig withLogger(final Class<? extends Logger> loggerClass) {
    super.withLogger(loggerClass);
    return this;
  }

  @NotNull
  @Override
  public RemoteConfig withLogger(final Logger logger) {
    super.withLogger(logger);
    return this;
  }

  @NotNull
  @Override
  public RemoteConfig withLoggerName(final String loggerName) {
    super.withLoggerName(loggerName);
    return this;
  }

  @NotNull
  @Override
  public RemoteConfig withOption(@NotNull final String key, final Object value) {
    super.withOption(key, value);
    return this;
  }

  @NotNull
  @Override
  public RemoteConfig withOptions(@NotNull final Map<? extends String, ?> options) {
    super.withOptions(options);
    return this;
  }

  @NotNull
  public RemoteConfig withSendersCacheMaxSize(final String maxSize) {
    super.withSendersCacheMaxSize(maxSize);
    return this;
  }

  @NotNull
  public RemoteConfig withSendersCacheMaxSize(final Number maxSize) {
    super.withSendersCacheMaxSize(maxSize);
    return this;
  }

  @NotNull
  public RemoteConfig withSendersCacheMaxSize(final int maxSize) {
    super.withSendersCacheMaxSize(maxSize);
    return this;
  }

  @NotNull
  public RemoteConfig withSendersCacheTimeout(final String timeoutMillis) {
    super.withSendersCacheTimeout(timeoutMillis);
    return this;
  }

  @NotNull
  public RemoteConfig withSendersCacheTimeout(final Number timeoutMillis) {
    super.withSendersCacheTimeout(timeoutMillis);
    return this;
  }

  @NotNull
  public RemoteConfig withSendersCacheTimeout(final long timeoutMillis) {
    super.withSendersCacheTimeout(timeoutMillis);
    return this;
  }

  @NotNull
  @Override
  public RemoteConfig withSerializer(final String serializerClass) {
    super.withSerializer(serializerClass);
    return this;
  }

  @NotNull
  @Override
  public RemoteConfig withSerializer(final Class<? extends Serializer> serializerClass) {
    super.withSerializer(serializerClass);
    return this;
  }

  @NotNull
  @Override
  public RemoteConfig withSerializer(final Serializer serializer) {
    super.withSerializer(serializer);
    return this;
  }

  @NotNull
  @Override
  public RemoteConfig withSerializerBlacklist(final String blacklist) {
    super.withSerializerBlacklist(blacklist);
    return this;
  }

  @NotNull
  @Override
  public RemoteConfig withSerializerBlacklist(final Iterable<? extends String> blacklist) {
    super.withSerializerBlacklist(blacklist);
    return this;
  }

  @NotNull
  @Override
  public RemoteConfig withSerializerWhitelist(final String whitelist) {
    super.withSerializerWhitelist(whitelist);
    return this;
  }

  @NotNull
  @Override
  public RemoteConfig withSerializerWhitelist(final Iterable<? extends String> whitelist) {
    super.withSerializerWhitelist(whitelist);
    return this;
  }

  @NotNull
  public RemoteConfig withProtectionDomain(
      final Class<? extends ProtectionDomain> protectionDomainClass) {
    return withOption(KEY_PROTECTION_DOMAIN_CLASS, protectionDomainClass);
  }

  @NotNull
  public RemoteConfig withProtectionDomain(final ProtectionDomain protectionDomain) {
    return withOption(KEY_PROTECTION_DOMAIN_CLASS, protectionDomain);
  }

  @NotNull
  public RemoteConfig withProtectionDomain(final String protectionDomainClass) {
    return withOption(KEY_PROTECTION_DOMAIN_CLASS, protectionDomainClass);
  }

  @NotNull
  public RemoteConfig withRemoteCreateEnable(final String createEnable) {
    return withOption(KEY_REMOTE_CREATE_ENABLE, createEnable);
  }

  @NotNull
  public RemoteConfig withRemoteCreateEnable(final Boolean createEnable) {
    return withOption(KEY_REMOTE_CREATE_ENABLE, createEnable);
  }

  @NotNull
  public RemoteConfig withRemoteCreateEnable(final boolean createEnable) {
    return withOption(KEY_REMOTE_CREATE_ENABLE, createEnable);
  }

  @NotNull
  public RemoteConfig withRemoteDismissEnable(final String dismissEnable) {
    return withOption(KEY_REMOTE_DISMISS_ENABLE, dismissEnable);
  }

  @NotNull
  public RemoteConfig withRemoteDismissEnable(final Boolean dismissEnable) {
    return withOption(KEY_REMOTE_DISMISS_ENABLE, dismissEnable);
  }

  @NotNull
  public RemoteConfig withRemoteDismissEnable(final boolean dismissEnable) {
    return withOption(KEY_REMOTE_DISMISS_ENABLE, dismissEnable);
  }

  @NotNull
  public RemoteConfig withRemoteMessagesEnable(final String messagesEnable) {
    return withOption(KEY_REMOTE_MESSAGES_ENABLE, messagesEnable);
  }

  @NotNull
  public RemoteConfig withRemoteMessagesEnable(final Boolean messagesEnable) {
    return withOption(KEY_REMOTE_MESSAGES_ENABLE, messagesEnable);
  }

  @NotNull
  public RemoteConfig withRemoteMessagesEnable(final boolean messagesEnable) {
    return withOption(KEY_REMOTE_MESSAGES_ENABLE, messagesEnable);
  }

  @NotNull
  public RemoteConfig withRemoteResourcesEnable(final String resourcesEnable) {
    return withOption(KEY_REMOTE_RESOURCES_ENABLE, resourcesEnable);
  }

  @NotNull
  public RemoteConfig withRemoteResourcesEnable(final Boolean resourcesEnable) {
    return withOption(KEY_REMOTE_RESOURCES_ENABLE, resourcesEnable);
  }

  @NotNull
  public RemoteConfig withRemoteResourcesEnable(final boolean resourcesEnable) {
    return withOption(KEY_REMOTE_RESOURCES_ENABLE, resourcesEnable);
  }

  @NotNull
  public RemoteConfig withRemoteRolesEnable(final String rolesEnable) {
    return withOption(KEY_REMOTE_ROLES_ENABLE, rolesEnable);
  }

  @NotNull
  public RemoteConfig withRemoteRolesEnable(final Boolean rolesEnable) {
    return withOption(KEY_REMOTE_ROLES_ENABLE, rolesEnable);
  }

  @NotNull
  public RemoteConfig withRemoteRolesEnable(final boolean rolesEnable) {
    return withOption(KEY_REMOTE_ROLES_ENABLE, rolesEnable);
  }
}
