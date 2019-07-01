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

package dm.shakespeare.template.typed.actor;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.actor.Role;
import dm.shakespeare.log.Logger;

/**
 * Base implementation of a script object.<br>
 * A script is used to set up the role and the environment in which a typed actor will operate. By
 * implementing a {@code Script} object it is possible to configure:<ul>
 * <li>the role object to be invoked</li>
 * <li>the {@link ExecutorService} on which the actor business logic will be executed</li>
 * <li>the inbox message quota</li>
 * <li>the maximum time to wait for the invocation result</li>
 * <li>the {@link Logger} instance</li>
 * </ul>
 * The script methods, with the exception of {@link #getResultTimeoutMillis(String, Method)}, are
 * only called once at the actor instantiation, and thread safety is not guaranteed. So, it's
 * advisable to employ a new script instance for each new actor, and to synchronized shared
 * resources when needed.
 */
public abstract class Script {

  private static final long DEFAULT_TIMEOUT = TimeUnit.SECONDS.toMillis(20);

  /**
   * Returns the executor service backing the actor execution.<br>
   * By default the {@link Role#defaultExecutorService()} instance will be returned.
   *
   * @param id the actor ID.
   * @return the executor service instance.
   * @throws Exception when an unexpected error occurs.
   */
  @NotNull
  public ExecutorService getExecutorService(@NotNull final String id) throws Exception {
    return Role.defaultExecutorService();
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
    return Role.defaultLogger(this);
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

  /**
   * Returns the number of milliseconds to wait for invocation result, or {@code null} if no waiting
   * is needed.<br>
   * By default a pre-defined timeout is returned for methods with a non void return type, and
   * {@code null} otherwise.
   *
   * @param id     the actor ID.
   * @param method the method to be invoked.
   * @return the timeout in number of milliseconds or {@code null}.
   * @throws Exception when an unexpected error occurs.
   */
  public Long getResultTimeoutMillis(@NotNull final String id, @NotNull final Method method) throws
      Exception {
    final Class<?> returnType = method.getReturnType();
    return ((returnType != void.class) && (returnType != Void.class)) ? DEFAULT_TIMEOUT : null;
  }

  /**
   * Returns the role object to be invoked by the typed actor.
   *
   * @param id the actor ID.
   * @return the role instance.
   * @throws Exception when an unexpected error occurs.
   */
  @NotNull
  public abstract Object getRole(@NotNull String id) throws Exception;
}
