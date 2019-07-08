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

import java.util.Map;
import java.util.concurrent.ExecutorService;

import dm.shakespeare.log.Logger;
import dm.shakespeare.remote.io.Serializer;
import dm.shakespeare.remote.transport.Connector;

/**
 * Created by davide-maestroni on 06/07/2019.
 */
public class LocalConfig extends CommonConfig {

  public static final String KEY_ACTORS_FULL_SYNC_TIME = "sks.actors.sync.full.millis";
  public static final String KEY_ACTORS_PART_SYNC_TIME = "sks.actors.sync.part.millis";
  public static final String KEY_REMOTE_ID = "sks.remote.id";

  public LocalConfig() {
  }

  public LocalConfig(@NotNull final Map<? extends String, ?> map) {
    super(map);
  }

  @NotNull
  public LocalConfig withActorsFullSyncTime(final String syncFullMillis) {
    withOption(KEY_ACTORS_FULL_SYNC_TIME, syncFullMillis);
    return this;
  }

  @NotNull
  public LocalConfig withActorsFullSyncTime(final Number syncFullMillis) {
    withOption(KEY_ACTORS_FULL_SYNC_TIME, syncFullMillis);
    return this;
  }

  @NotNull
  public LocalConfig withActorsFullSyncTime(final long syncFullMillis) {
    withOption(KEY_ACTORS_FULL_SYNC_TIME, syncFullMillis);
    return this;
  }

  @NotNull
  public LocalConfig withActorsPartSyncTime(final String syncPartMillis) {
    withOption(KEY_ACTORS_PART_SYNC_TIME, syncPartMillis);
    return this;
  }

  @NotNull
  public LocalConfig withActorsPartSyncTime(final Number syncPartMillis) {
    withOption(KEY_ACTORS_PART_SYNC_TIME, syncPartMillis);
    return this;
  }

  @NotNull
  public LocalConfig withActorsPartSyncTime(final long syncPartMillis) {
    withOption(KEY_ACTORS_PART_SYNC_TIME, syncPartMillis);
    return this;
  }

  @NotNull
  @Override
  public LocalConfig withConnector(final String connectorClass) {
    super.withConnector(connectorClass);
    return this;
  }

  @NotNull
  @Override
  public LocalConfig withConnector(final Class<? extends Connector> connectorClass) {
    super.withConnector(connectorClass);
    return this;
  }

  @NotNull
  @Override
  public LocalConfig withConnector(final Connector connector) {
    super.withConnector(connector);
    return this;
  }

  @NotNull
  @Override
  public LocalConfig withExecutor(final String executorClass) {
    super.withExecutor(executorClass);
    return this;
  }

  @NotNull
  @Override
  public LocalConfig withExecutor(final Class<? extends ExecutorService> executorClass) {
    super.withExecutor(executorClass);
    return this;
  }

  @NotNull
  @Override
  public LocalConfig withExecutor(final ExecutorService executor) {
    super.withExecutor(executor);
    return this;
  }

  @NotNull
  @Override
  public LocalConfig withLogger(final String loggerClass) {
    super.withLogger(loggerClass);
    return this;
  }

  @NotNull
  @Override
  public LocalConfig withLogger(final Class<? extends Logger> loggerClass) {
    super.withLogger(loggerClass);
    return this;
  }

  @NotNull
  @Override
  public LocalConfig withLogger(final Logger logger) {
    super.withLogger(logger);
    return this;
  }

  @NotNull
  @Override
  public LocalConfig withLoggerName(final String loggerName) {
    super.withLoggerName(loggerName);
    return this;
  }

  @NotNull
  @Override
  public LocalConfig withOption(@NotNull final String key, final Object value) {
    super.withOption(key, value);
    return this;
  }

  @NotNull
  @Override
  public LocalConfig withOptions(@NotNull final Map<? extends String, ?> options) {
    super.withOptions(options);
    return this;
  }

  @NotNull
  public LocalConfig withSendersCacheMaxSize(final String maxSize) {
    super.withSendersCacheMaxSize(maxSize);
    return this;
  }

  @NotNull
  public LocalConfig withSendersCacheMaxSize(final Number maxSize) {
    super.withSendersCacheMaxSize(maxSize);
    return this;
  }

  @NotNull
  public LocalConfig withSendersCacheMaxSize(final int maxSize) {
    super.withSendersCacheMaxSize(maxSize);
    return this;
  }

  @NotNull
  public LocalConfig withSendersCacheTimeout(final String timeoutMillis) {
    super.withSendersCacheTimeout(timeoutMillis);
    return this;
  }

  @NotNull
  public LocalConfig withSendersCacheTimeout(final Number timeoutMillis) {
    super.withSendersCacheTimeout(timeoutMillis);
    return this;
  }

  @NotNull
  public LocalConfig withSendersCacheTimeout(final long timeoutMillis) {
    super.withSendersCacheTimeout(timeoutMillis);
    return this;
  }

  @NotNull
  @Override
  public LocalConfig withSerializer(final String serializerClass) {
    super.withSerializer(serializerClass);
    return this;
  }

  @NotNull
  @Override
  public LocalConfig withSerializer(final Class<? extends Serializer> serializerClass) {
    super.withSerializer(serializerClass);
    return this;
  }

  @NotNull
  @Override
  public LocalConfig withSerializer(final Serializer serializer) {
    super.withSerializer(serializer);
    return this;
  }

  @NotNull
  @Override
  public LocalConfig withSerializerBlacklist(final String blacklist) {
    super.withSerializerBlacklist(blacklist);
    return this;
  }

  @NotNull
  @Override
  public LocalConfig withSerializerBlacklist(final Iterable<? extends String> blacklist) {
    super.withSerializerBlacklist(blacklist);
    return this;
  }

  @NotNull
  @Override
  public LocalConfig withSerializerWhitelist(final String whitelist) {
    super.withSerializerWhitelist(whitelist);
    return this;
  }

  @NotNull
  @Override
  public LocalConfig withSerializerWhitelist(final Iterable<? extends String> whitelist) {
    super.withSerializerWhitelist(whitelist);
    return this;
  }

  @NotNull
  public LocalConfig withRemoteId(final String remoteId) {
    withOption(KEY_REMOTE_ID, remoteId);
    return this;
  }
}
