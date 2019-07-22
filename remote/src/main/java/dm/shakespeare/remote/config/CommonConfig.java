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
import dm.shakespeare.remote.transport.connection.Connector;

/**
 * Created by davide-maestroni on 06/07/2019.
 */
public class CommonConfig extends StageConfig {

  public static final String KEY_CONNECTOR_CLASS = "sks.connector.class";
  public static final String KEY_EXECUTOR_CLASS = "sks.executor.service.class";
  public static final String KEY_LOGGER_CLASS = "sks.logger.class";
  public static final String KEY_LOGGER_NAME = "sks.logger.name";
  public static final String KEY_SENDERS_CACHE_MAX_SIZE = "sks.senders.cache.max.size";
  public static final String KEY_SENDERS_CACHE_TIMEOUT = "sks.senders.cache.timeout.millis";
  public static final String KEY_SERIALIZER_BLACKLIST = "sks.serializer.black.list";
  public static final String KEY_SERIALIZER_CLASS = "sks.serializer.class";
  public static final String KEY_SERIALIZER_WHITELIST = "sks.serializer.white.list";

  public CommonConfig() {
  }

  public CommonConfig(@NotNull final Map<? extends String, ?> map) {
    super(map);
  }

  @NotNull
  public CommonConfig withConnector(final String connectorClass) {
    return withOption(KEY_CONNECTOR_CLASS, connectorClass);
  }

  @NotNull
  public CommonConfig withConnector(final Class<? extends Connector> connectorClass) {
    return withOption(KEY_CONNECTOR_CLASS, connectorClass);
  }

  @NotNull
  public CommonConfig withConnector(final Connector connector) {
    return withOption(KEY_CONNECTOR_CLASS, connector);
  }

  @NotNull
  public CommonConfig withExecutorService(final String executorClass) {
    return withOption(KEY_EXECUTOR_CLASS, executorClass);
  }

  @NotNull
  public CommonConfig withExecutorService(final Class<? extends ExecutorService> executorClass) {
    return withOption(KEY_EXECUTOR_CLASS, executorClass);
  }

  @NotNull
  public CommonConfig withExecutorService(final ExecutorService executor) {
    return withOption(KEY_EXECUTOR_CLASS, executor);
  }

  @NotNull
  public CommonConfig withLogger(final String loggerClass) {
    return withOption(KEY_LOGGER_CLASS, loggerClass);
  }

  @NotNull
  public CommonConfig withLogger(final Class<? extends Logger> loggerClass) {
    return withOption(KEY_LOGGER_CLASS, loggerClass);
  }

  @NotNull
  public CommonConfig withLogger(final Logger logger) {
    return withOption(KEY_LOGGER_CLASS, logger);
  }

  @NotNull
  public CommonConfig withLoggerName(final String loggerName) {
    return withOption(KEY_LOGGER_NAME, loggerName);
  }

  @NotNull
  @Override
  public CommonConfig withOption(@NotNull final String key, final Object value) {
    super.withOption(key, value);
    return this;
  }

  @NotNull
  @Override
  public CommonConfig withOptions(@NotNull final Map<? extends String, ?> options) {
    super.withOptions(options);
    return this;
  }

  @NotNull
  public CommonConfig withSendersCacheMaxSize(final String maxSize) {
    return withOption(KEY_SENDERS_CACHE_MAX_SIZE, maxSize);
  }

  @NotNull
  public CommonConfig withSendersCacheMaxSize(final Number maxSize) {
    return withOption(KEY_SENDERS_CACHE_MAX_SIZE, maxSize);
  }

  @NotNull
  public CommonConfig withSendersCacheMaxSize(final int maxSize) {
    return withOption(KEY_SENDERS_CACHE_MAX_SIZE, maxSize);
  }

  @NotNull
  public CommonConfig withSendersCacheTimeout(final String timeoutMillis) {
    return withOption(KEY_SENDERS_CACHE_TIMEOUT, timeoutMillis);
  }

  @NotNull
  public CommonConfig withSendersCacheTimeout(final Number timeoutMillis) {
    return withOption(KEY_SENDERS_CACHE_TIMEOUT, timeoutMillis);
  }

  @NotNull
  public CommonConfig withSendersCacheTimeout(final long timeoutMillis) {
    return withOption(KEY_SENDERS_CACHE_TIMEOUT, timeoutMillis);
  }

  @NotNull
  public CommonConfig withSerializer(final String serializerClass) {
    return withOption(KEY_SERIALIZER_CLASS, serializerClass);
  }

  @NotNull
  public CommonConfig withSerializer(final Class<? extends Serializer> serializerClass) {
    return withOption(KEY_SERIALIZER_CLASS, serializerClass);
  }

  @NotNull
  public CommonConfig withSerializer(final Serializer serializer) {
    return withOption(KEY_SERIALIZER_CLASS, serializer);
  }

  @NotNull
  public CommonConfig withSerializerBlacklist(final String blacklist) {
    return withOption(KEY_SERIALIZER_BLACKLIST, blacklist);
  }

  @NotNull
  public CommonConfig withSerializerBlacklist(final Iterable<? extends String> blacklist) {
    return withOption(KEY_SERIALIZER_BLACKLIST, blacklist);
  }

  @NotNull
  public CommonConfig withSerializerWhitelist(final String whitelist) {
    return withOption(KEY_SERIALIZER_WHITELIST, whitelist);
  }

  @NotNull
  public CommonConfig withSerializerWhitelist(final Iterable<? extends String> whitelist) {
    return withOption(KEY_SERIALIZER_WHITELIST, whitelist);
  }
}
