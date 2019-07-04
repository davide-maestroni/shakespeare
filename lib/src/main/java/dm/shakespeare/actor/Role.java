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
import dm.shakespeare.util.ConstantConditions;

/**
 * Base abstract implementation of a role object.<br>
 * A role is used to instruct an actor on how to behave. By implementing a {@code Role} object it
 * is possible to configure:<ul>
 * <li>the initial actor {@link Behavior}</li>
 * <li>the {@link ExecutorService} on which the actor business logic will be executed</li>
 * <li>the inbox message quota</li>
 * <li>the {@link Logger} instance</li>
 * </ul>
 * The role methods are only called once at the actor instantiation, and thread safety is not
 * guaranteed. So, it's advisable to employ a new role instance for each new actor.
 */
public abstract class Role {

  private static final AtomicLong count = new AtomicLong();
  private static final Object mutex = new Object();

  private static ScheduledExecutorService defaultExecutorService;

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
    synchronized (mutex) {
      if ((defaultExecutorService == null) || defaultExecutorService.isShutdown()) {
        defaultExecutorService =
            ExecutorServices.newDynamicScheduledThreadPool(new ThreadFactory() {

              public Thread newThread(@NotNull final Runnable runnable) {
                return new Thread(runnable, "shakespeare-thread-" + count.getAndIncrement());
              }
            });
      }
    }
    return defaultExecutorService;
  }

  /**
   * Creates a default logger for the specified object.
   *
   * @param object the object.
   * @return the logger instance.
   */
  @NotNull
  public static Logger defaultLogger(@NotNull final Object object) {
    return new Logger(LogPrinters.javaLoggingPrinter(object.getClass().getName()));
  }

  /**
   * Creates a new role from the specified mapper.<br>
   * The mapper will be called passing the actor ID as input parameter and must return a behavior
   * instance.
   *
   * @param mapper the mapper function.
   * @return the role instance.
   */
  @NotNull
  public static Role from(@NotNull final Mapper<? super String, ? extends Behavior> mapper) {
    ConstantConditions.notNull("mapper", mapper);
    return new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) throws Exception {
        return mapper.apply(id);
      }
    };
  }

  /**
   * Creates a new behavior builder.
   *
   * @return the behavior builder instance.
   */
  @NotNull
  public static BehaviorBuilder newBehavior() {
    return new StandardBehaviorBuilder();
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
   * By default the {@link #defaultExecutorService()} instance will be returned.<br>
   * Notice that it is responsibility of the overriding class to properly shutdown the returned
   * executor when needed, for example by calling {@link ExecutorService#shutdown()} method on
   * the stop event in the returned behavior:
   * <pre class="brush:java">
   *   private static class MyBehavior extends AbstractBehavior {
   *
   *     public void onMessage(Object message, &#64;NotNull Envelop envelop, &#64;NotNull Agent agent)
   *         throws Exception {
   *       // do your stuff here
   *     }
   *
   *     public void onStop(&#64;NotNull Agent agent) throws Exception {
   *       if (agent.isDismissed()) {
   *         agent.getExecutorService().shutdown();
   *       }
   *     }
   *   }
   * </pre>
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
    return defaultLogger(this);
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
