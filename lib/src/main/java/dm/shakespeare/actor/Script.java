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

package dm.shakespeare.actor;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import dm.shakespeare.actor.BehaviorBuilder.Handler;
import dm.shakespeare.concurrent.ExecutorServices;
import dm.shakespeare.function.Mapper;
import dm.shakespeare.function.Observer;
import dm.shakespeare.log.LogPrinters;
import dm.shakespeare.log.Logger;

/**
 * Created by davide-maestroni on 01/08/2019.
 */
public abstract class Script {

  private static final AtomicLong sCount = new AtomicLong();
  private static final Object sMutex = new Object();

  private static ScheduledExecutorService sDefaultExecutorService;

  @NotNull
  public static <T> Handler<T> accept(@NotNull final Observer<T> observer) {
    return new AcceptHandler<T>(observer);
  }

  @NotNull
  public static <T> Handler<T> apply(@NotNull final Mapper<T, ?> mapper) {
    return new ApplyHandler<T>(mapper);
  }

  @NotNull
  public static ExecutorService defaultExecutorService() {
    synchronized (sMutex) {
      if ((sDefaultExecutorService == null) || sDefaultExecutorService.isShutdown()) {
        sDefaultExecutorService =
            ExecutorServices.newDynamicScheduledThreadPool(new ThreadFactory() {

              public Thread newThread(@NotNull final Runnable runnable) {
                return new Thread(runnable, "shakespeare-thread-" + sCount.getAndIncrement());
              }
            });
      }
    }
    return sDefaultExecutorService;
  }

  @NotNull
  public static BehaviorBuilder newBehavior() {
    return new DefaultBehaviorBuilder();
  }

  @NotNull
  public abstract Behavior getBehavior(@NotNull String id) throws Exception;

  @NotNull
  public ExecutorService getExecutorService(@NotNull final String id) throws Exception {
    return defaultExecutorService();
  }

  @NotNull
  public Logger getLogger(@NotNull final String id) throws Exception {
    return Logger.newLogger(LogPrinters.javaLoggingPrinter(getClass().getName() + "." + id));
  }

  public int getQuota(@NotNull final String id) throws Exception {
    return Integer.MAX_VALUE;
  }
}
