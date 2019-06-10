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

import java.util.Set;

/**
 * {@code Set} of {@code Actor}s replicating the actor interface.
 */
public interface ActorSet extends Set<Actor> {

  /**
   * Adds an observer that will be notified with a {@link dm.shakespeare.message.DeadLetter
   * DeadLetter} message when an actor in this set is dismissed.
   *
   * @param observer the observer actor.
   * @return this actor set.
   * @see Actor#addObserver(Actor)
   */
  @NotNull
  ActorSet addObserver(@NotNull Actor observer);

  /**
   * Dismiss all the actors in this set.
   *
   * @param mayInterruptIfRunning whether the currently running thread (if any) can be interrupted
   *                              to stop the processing of messages. Be aware that, based on the
   *                              actor executor service, interrupting the running thread might
   *                              cause the behavior to never receive a stop notification.
   * @see Actor#dismiss(boolean)
   */
  void dismiss(boolean mayInterruptIfRunning);

  /**
   * Removes an observer which should be notified of the dismissal of an actor in this set.
   *
   * @param observer the observer actor.
   * @return this actor set.
   * @see Actor#removeObserver(Actor)
   */
  @NotNull
  ActorSet removeObserver(@NotNull Actor observer);

  /**
   * Tells the specified message to all the actors in this set.
   *
   * @param message the message instance (may be {@code null}).
   * @param headers the message headers.
   * @param sender  the sender actor.
   * @return this actor set.
   * @see Actor#tell(Object, Headers, Actor)
   */
  @NotNull
  ActorSet tell(Object message, @Nullable Headers headers, @NotNull Actor sender);

  /**
   * Tells the specified batch of messages to all the actors in this set.
   *
   * @param messages the messages (may contains {@code null} objects).
   * @param headers  the message headers.
   * @param sender   the sender actor.
   * @return this actor set.
   * @see Actor#tellAll(Iterable, Headers, Actor)
   */
  @NotNull
  ActorSet tellAll(@NotNull Iterable<?> messages, @Nullable Headers headers, @NotNull Actor sender);
}
