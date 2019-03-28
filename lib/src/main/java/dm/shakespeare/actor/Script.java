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
 * Base abstract implementation of a script object.<br>
 * A script is used to instruct an actor on how to behave. In fact, by implementing a script
 * object it is possible to configure the {@link ExecutorService} on which the actor business logic
 * will be executed, the inbox message quota, the {@link Logger} instance and the initial actor
 * {@link Behavior}.<br>
 * The script methods are only called once at the actor instantiation, and thread safety is not
 * guaranteed. So, it's advisable to employ a new script instance for each new actor.
 */
public abstract class Script {

  private static final AtomicLong sCount = new AtomicLong();
  private static final Object sMutex = new Object();

  private static ScheduledExecutorService sDefaultExecutorService;

  /**
   * Creates a new message handler wrapping the specified observer instance.
   *
   * @param observer the observer to wrap.
   * @param <T>      the message runtime type.
   * @return the wrapping handler.
   */
  @NotNull
  public static <T> Handler<T> accept(@NotNull final Observer<T> observer) {
    return new AcceptHandler<T>(observer);
  }

  /**
   * Creates a new message handler wrapping the specified mapper instance.
   *
   * @param mapper the mapper to wrap.
   * @param <T>    the message runtime type.
   * @return the wrapping handler.
   */
  @NotNull
  public static <T> Handler<T> apply(@NotNull final Mapper<T, ?> mapper) {
    return new ApplyHandler<T>(mapper);
  }

  /**
   * Returns the default executor service instance used by actors.<br>
   * The instance is lazily created and should be never shutdown but when the application exits.
   *
   * @return the default executor service instance.
   */
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

  /**
   * Creates a new behavior builder.
   *
   * @return the behavior builder instance.
   */
  @NotNull
  public static BehaviorBuilder newBehavior() {
    return new DefaultBehaviorBuilder();
  }

  /**
   * Returns the initial actor behavior.
   *
   * @param id the actor ID.
   * @return the behavior instance.
   * @throws Exception when an unexpected error occurs.
   */
  @NotNull
  public abstract Behavior getBehavior(@NotNull String id) throws Exception;

  /**
   * Returns the executor service backing the actor execution.<br>
   * By default the {@link #defaultExecutorService()} instance will be returned.
   *
   * @param id the actor ID.
   * @return the executor service instance.
   * @throws Exception when an unexpected error occurs.
   */
  @NotNull
  public ExecutorService getExecutorService(@NotNull final String id) throws Exception {
    return defaultExecutorService();
  }

  /**
   * Returns the logger instance to be employed by the actor.<br>
   * By default a logger backed by the Java logging framework will be returned.
   *
   * @param id the actor ID.
   * @return the logger instance.
   * @throws Exception when an unexpected error occurs.
   */
  @NotNull
  public Logger getLogger(@NotNull final String id) throws Exception {
    return Logger.newLogger(LogPrinters.javaLoggingPrinter(getClass().getName() + "." + id));
  }

  /**
   * Returns the actor inbox quota.<br>
   * By default {@link Integer#MAX_VALUE} will be returned.
   *
   * @param id the actor ID.
   * @return the maximum number of unprocessed message in the inbox.
   * @throws Exception when an unexpected error occurs.
   */
  public int getQuota(@NotNull final String id) throws Exception {
    return Integer.MAX_VALUE;
  }
}
