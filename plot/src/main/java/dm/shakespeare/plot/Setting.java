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

package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import dm.shakespeare.actor.Role;
import dm.shakespeare.concurrent.ExecutorServices;
import dm.shakespeare.log.Logger;
import dm.shakespeare.util.CQueue;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.WeakIdentityHashMap;

/**
 * Created by davide-maestroni on 01/25/2019.
 */
class Setting {

  private static final ThreadLocal<CQueue<Setting>> localSettings =
      new ThreadLocal<CQueue<Setting>>() {

        @Override
        protected CQueue<Setting> initialValue() {
          return new CQueue<Setting>();
        }
      };

  private final HashMap<Class<?>, Cache> caches = new HashMap<Class<?>, Cache>();
  private final ExecutorService executor;
  private final Logger logger;

  private ExecutorService localExecutor;

  Setting(@Nullable final ExecutorService executor, @Nullable final Logger logger) {
    this.executor = (executor != null) ? asActorExecutor(executor)
        : asActorExecutor(Role.defaultExecutorService());
    this.logger = logger;
  }

  @NotNull
  static Setting get() {
    try {
      return localSettings.get().get(0);

    } catch (final IndexOutOfBoundsException e) {
      throw new IllegalStateException("code is not running inside a plot");
    }
  }

  static void set(@NotNull final Setting setting) {
    localSettings.get().add(ConstantConditions.notNull("setting", setting));
  }

  static void unset() {
    localSettings.get().removeLast();
  }

  @NotNull
  private static ExecutorService asActorExecutor(@NotNull final ExecutorService executor) {
    return (executor instanceof ScheduledExecutorService) ? ExecutorServices.asActorExecutor(
        (ScheduledExecutorService) executor) : ExecutorServices.asActorExecutor(executor);
  }

  @NotNull
  Cache getCache(@NotNull final Class<?> type) {
    Cache cache = caches.get(type);
    if (cache == null) {
      cache = new Cache();
      caches.put(type, cache);
    }
    return cache;
  }

  @Nullable
  ExecutorService getExecutor() {
    return executor;
  }

  @NotNull
  ExecutorService getLocalExecutor() {
    if (localExecutor == null) {
      localExecutor = ExecutorServices.asActorExecutor(ExecutorServices.localExecutor());
    }
    return localExecutor;
  }

  @Nullable
  Logger getLogger() {
    return logger;
  }

  static class Cache {

    private final WeakIdentityHashMap<Object, WeakReference<Object>> data =
        new WeakIdentityHashMap<Object, WeakReference<Object>>();

    private Cache() {
    }

    @Nullable
    @SuppressWarnings("unchecked")
    <T> T get(final Object key) {
      final WeakReference<Object> reference = data.get(key);
      return (reference != null) ? (T) reference.get() : null;
    }

    <T> void put(@NotNull final Object key, final T value) {
      data.put(key, new WeakReference<Object>(value));
    }
  }
}
