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
import org.jetbrains.annotations.Nullable;

import dm.shakespeare.Stage;

/**
 * Interface defining an actor object.<br>
 * An actor represents the basic concurrent computation unit. The business logic defined in its
 * {@link Behavior} is executed sequentially in such a way that no other synchronization mechanism
 * is needed. That is true for any field variable, while static fields might need to be
 * synchronized if they are not immutable and are shared between different instances.<p>
 * The only way to communicate with an actor is through messages, where a message can be any Java
 * object. In order to preserve concurrency safety, the message objects should always be immutable
 * or effectively so.<p>
 * An actor is uniquely identified within a {@link Stage} by its ID. Although, it might belong to
 * no stage at all.<br>
 * An actor ID may be any string and be formatted in any way useful to the client. For example, the
 * IDs might be hierarchically formatted as filesystem folders or files, and then queried through
 * regex patterns.
 */
public interface Actor {

  /**
   * Adds an observer that will be notified with a {@link dm.shakespeare.message.DeadLetter
   * DeadLetter} message when this actor is dismissed.<br>
   * Adding an observer to an already dismissed actor will always generate a notification message.
   *
   * @param observer the observer actor.
   * @return whether the observer was successfully added.
   */
  boolean addObserver(@NotNull Actor observer);

  /**
   * Dismiss this actor so that its behavior will be stopped and the actor removed from its stage.
   * <br>
   * All the messages still pending in the inbox will be bounced.
   *
   * @return whether the actor was successfully dismissed.
   */
  boolean dismiss();

  /**
   * Lazily dismiss this actor so that its behavior will be stopped and the actor removed from its
   * stage.<br>
   * Any pending message, at the moment this method is called, will be processed before initiating
   * the actor teardown.
   *
   * @return whether the actor was successfully dismissed.
   */
  boolean dismissLazy();

  /**
   * Immediately dismiss this actor so that its behavior will be stopped and the actor removed from
   * its stage.<br>
   * The thread currently processing the actor messages might be interrupted as a result of the call
   * to this method. Be aware that, based on the actor executor service, interrupting the running
   * thread might cause the behavior to never receive a stop notification.<br>
   * All the messages still pending in the inbox will be bounced.
   *
   * @return whether the actor was successfully dismissed.
   */
  boolean dismissNow();

  /**
   * Returns the actor ID.<br>
   * An actor has an ID even if not registered on any stage.
   *
   * @return the actor ID.
   */
  @NotNull
  String getId();

  /**
   * Removes an observer which should be notified of the actor dismissal.
   *
   * @param observer the observer actor.
   * @return whether the observer was successfully removed.
   */
  boolean removeObserver(@NotNull Actor observer);

  /**
   * Tells to this actor the specified message.<br>
   * The headers may include:
   * <ul>
   * <li>a time offset, used to modify the send time</li>
   * <li>a thread ID, useful to identify messages belonging to the same thread</li>
   * <li>a receipt ID, indicating that the sender wants to be notified of the message delivery</li>
   * </ul>
   * The actor will automatically employ the optional receipt ID to send back notifications
   * (unless {@link Envelop#preventReceipt()} is called). All the replies to the sender should
   * contain in the headers the same thread ID as the originating message.<br>
   * If the number of unprocessed messages still in the inbox exceeds the configured quota, the
   * message will be bounced (see {@link dm.shakespeare.message.Bounce Bounce}).<br>
   * In case no reply is expected or no actor is interested in receiving it, it is possible to
   * use a {@link dm.shakespeare.Stage#STAND_IN Stage.STAND_IN} as sender.
   *
   * @param message the message instance (may be {@code null}).
   * @param headers the message headers.
   * @param sender  the sender actor.
   * @see Role
   */
  void tell(Object message, @Nullable Headers headers, @NotNull Actor sender);

  /**
   * Tells to this actor the specified batch of messages.<br>
   * This method will delivered a single object containing all the messages to this actor. Then,
   * each of them will be processed separately with the same headers.<br>
   * The headers may include:
   * <ul>
   * <li>a time offset, used to modify the send time</li>
   * <li>a thread ID, useful to identify messages belonging to the same thread</li>
   * <li>a receipt ID, indicating that the sender wants to be notified of the message delivery</li>
   * </ul>
   * The actor will automatically employ the optional receipt ID to send back notifications
   * (unless {@link Envelop#preventReceipt()} is called). All the replies to the sender should
   * contain in the headers the same thread ID as the originating message.<br>
   * If the number of unprocessed messages still in the inbox exceeds the configured quota, the
   * message will be bounced (see {@link dm.shakespeare.message.Bounce Bounce}).<br>
   * The batch object will count as one message in the inbox quota.<br>
   * In case no reply is expected or no actor is interested in receiving it, it is possible to
   * use a {@link dm.shakespeare.Stage#STAND_IN Stage.STAND_IN} as sender.
   *
   * @param messages the messages (may contains {@code null} objects).
   * @param headers  the messages headers.
   * @param sender   the sender actor.
   * @see Role
   */
  void tellAll(@NotNull Iterable<?> messages, @Nullable Headers headers, @NotNull Actor sender);
}
