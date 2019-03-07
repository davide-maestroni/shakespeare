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

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import dm.shakespeare.BackStage;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Script;
import dm.shakespeare.concurrent.ExecutorServices;
import dm.shakespeare.log.Logger;
import dm.shakespeare.util.CQueue;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/25/2019.
 */
class Setting {

  private static final ThreadLocal<CQueue<Setting>> sLocalSetting =
      new ThreadLocal<CQueue<Setting>>() {

        @Override
        protected CQueue<Setting> initialValue() {
          return new CQueue<Setting>();
        }
      };

  private final HashMap<Class<?>, Cache> mCaches = new HashMap<Class<?>, Cache>();
  private final ExecutorService mExecutor;
  private final Logger mLogger;

  private ExecutorService mTrampolineExecutor;

  Setting(@Nullable final ExecutorService executor, @Nullable final Logger logger) {
    mExecutor =
        (executor != null) ? asActorExecutor(executor) : asActorExecutor(Script.defaultExecutor());
    mLogger = logger;
  }

  @NotNull
  static Setting get() {
    try {
      return sLocalSetting.get().get(0);

    } catch (final IndexOutOfBoundsException e) {
      throw new IllegalStateException("code is not running inside a scene");
    }
  }

  static void set(@NotNull final Setting setting) {
    sLocalSetting.get().add(ConstantConditions.notNull("setting", setting));
  }

  static void unset() {
    sLocalSetting.get().removeLast();
  }

  @NotNull
  private static ExecutorService asActorExecutor(@NotNull final ExecutorService executor) {
    return (executor instanceof ScheduledExecutorService) ? ExecutorServices.asActorExecutor(
        (ScheduledExecutorService) executor) : ExecutorServices.asActorExecutor(executor);
  }

  @NotNull
  Cache getCache(@NotNull final Class<?> type) {
    Cache cache = mCaches.get(type);
    if (cache == null) {
      cache = new Cache();
      mCaches.put(type, cache);
    }
    return cache;
  }

  @Nullable
  ExecutorService getExecutor() {
    return mExecutor;
  }

  @NotNull
  ExecutorService getLocalExecutor() {
    if (mTrampolineExecutor == null) {
      mTrampolineExecutor = ExecutorServices.asActorExecutor(ExecutorServices.localExecutor());
    }
    return mTrampolineExecutor;
  }

  @Nullable
  Logger getLogger() {
    return mLogger;
  }

  @NotNull
  Actor newActor(@NotNull final Script script) {
    return BackStage.newActor(script);
  }

  static class Cache {

    private final HashMap<Object, Object> mData = new HashMap<Object, Object>();

    private Cache() {
    }

    @Nullable
    @SuppressWarnings("unchecked")
    <T> T get(@NotNull final Object key) {
      return (T) mData.get(key);
    }

    <T> void put(@NotNull final Object key, final T value) {
      mData.put(key, value);
    }
  }
}
