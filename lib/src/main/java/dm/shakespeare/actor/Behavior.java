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

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import dm.shakespeare.log.Logger;

/**
 * Interface defining an actor behavior.<br>
 * The behavior represents the actor business logic, that is, how the actor reacts to received
 * messages.<br>
 * The behavior instance is notified of three type of events:<ul>
 * <li>{@code onStart}: called when the actor starts or after a restart cycle</li>
 * <li>{@code onMessage}: called each time a new message is received</li>
 * <li>{@code onStop}: called when the actor is dismissed or at the beginning of a restart
 * cycle</li></ul><br>
 * It is always possible to know whether the behavior is stopped as a result of actor dismissal or
 * a simple restart by calling {@link Agent#isDismissed()}.<p>
 * The actor data, executor services and logger should always be accessed through the
 * {@code Agent} instance. The provided executor services can be safely used to perform
 * asynchronous operations, since they employ the same synchronization mechanism as the actor
 * executor service (see {@link Actor}), but do not support any blocking method (like
 * {@link ExecutorService#invokeAll(Collection)} or {@link ExecutorService#invokeAny(Collection)}).
 * <br>
 * Any scheduled task will be automatically cancelled when the actor is dismissed.<br>
 * Through the agent object it is also possible to dynamically change the behavior instance
 * and to dismiss and restart the actor. Any command initiated by calling an agent method will
 * take place after the current behavior method execution ends, and it takes precedence over any
 * pending message still in the inbox.<p>
 * The behavior implementation should take into account that a restart cycle may be applied, hence
 * it should perform the proper clean-up and re-initialization operations in the
 * {@link #onStop(Agent)} and {@link #onStart(Agent)} methods respectively.<br>
 * In any case, if an exception escapes those two methods, the actor will be automatically
 * dismissed.<p>
 * Each received message will come with an envelop containing the sender data and the message
 * headers. The headers may include:
 * <ul>
 * <li>a time offset, used to modify the send time</li>
 * <li>a thread ID, useful to identify messages belonging to the same thread</li>
 * <li>a receipt ID, indicating that the sender wants to be notified of the message delivery</li>
 * </ul>
 * The actor will automatically employ the optional receipt ID to send back notifications
 * (unless {@link Envelop#preventReceipt()} is called). All the replies to the sender should
 * contain in the headers the same thread ID as the originating message.
 */
public interface Behavior {

  /**
   * Notifies the behavior that a new message has been received.
   *
   * @param message the message object.
   * @param envelop the message envelop.
   * @param agent   the behavior agent.
   * @throws Exception when an unexpected error occurs.
   */
  void onMessage(Object message, @NotNull Envelop envelop, @NotNull Agent agent) throws Exception;

  /**
   * Notifies the behavior that the actor has been started.
   *
   * @param agent the behavior agent.
   * @throws Exception when an unexpected error occurs.
   */
  void onStart(@NotNull Agent agent) throws Exception;

  /**
   * Notifies the behavior that the actor has been stopped.
   *
   * @param agent the behavior agent.
   * @throws Exception when an unexpected error occurs.
   */
  void onStop(@NotNull Agent agent) throws Exception;

  /**
   * Interface defining the {@link Behavior} agent.<br>
   * Any command initiated by calling an agent method will take place after the current behavior
   * method execution ends, and it takes precedence over any pending message still in the inbox.
   */
  interface Agent {

    /**
     * Dismiss the actor and remove it from its stage.
     */
    void dismissSelf();

    /**
     * Returns the agent executor service.<br>
     * The returned instance can be safely used to perform asynchronous operations, since it
     * employs the same synchronization mechanism as the actor executor service (see {@link Actor}),
     * but do not support any blocking method (like {@link ExecutorService#invokeAll(Collection)} or
     * {@link ExecutorService#invokeAny(Collection)}).
     *
     * @return the executor service instance.
     */
    @NotNull
    ExecutorService getExecutorService();

    /**
     * Returns the agent logger.
     *
     * @return the logger instance.
     */
    @NotNull
    Logger getLogger();

    /**
     * Returns the agent scheduled executor service.<br>
     * The returned instance can be safely used to perform asynchronous operations, since it
     * employs the same synchronization mechanism as the actor executor service (see {@link Actor}),
     * but do not support any blocking method (like {@link ExecutorService#invokeAll(Collection)} or
     * {@link ExecutorService#invokeAny(Collection)}).
     *
     * @return the scheduled executor service instance.
     */
    @NotNull
    ScheduledExecutorService getScheduledExecutorService();

    /**
     * Returns the agent actor.
     *
     * @return the actor instance.
     */
    @NotNull
    Actor getSelf();

    /**
     * Verifies whether the actor has been dismissed.
     *
     * @return {@code true} if {@link Agent#dismissSelf()} or {@link Actor#dismiss(boolean)} has
     * been called.
     */
    boolean isDismissed();

    /**
     * Initiate a restart cycle.<br>
     * The current behavior {@link #onStop(Agent)} and {@link #onStart(Agent)} methods will be
     * called in sequence.
     */
    void restartSelf();

    /**
     * Modifies the current behavior.<br>
     * The previous behavior {@link #onStop(Agent)} method will not be automatically called as
     * a result of this method invocation. It is responsibility of the caller to perform the
     * proper clean-up operations when needed.
     *
     * @param behavior the new behavior instance.
     */
    void setBehavior(@NotNull Behavior behavior);
  }
}
