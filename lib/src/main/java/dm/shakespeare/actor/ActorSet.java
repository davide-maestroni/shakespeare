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
   * @return whether the observer was successfully added to all the actors.
   * @see Actor#addObserver(Actor)
   */
  boolean addObserver(@NotNull Actor observer);

  /**
   * Dismiss all the actors in this set.
   *
   * @return whether all the actors were successfully dismissed.
   * @see Actor#dismiss()
   */
  boolean dismiss();

  /**
   * Lazily dismiss all the actors in this set.
   *
   * @return whether all the actors were successfully dismissed.
   * @see Actor#dismissLazy()
   */
  boolean dismissLazy();

  /**
   * Immediately dismiss all the actors in this set.
   *
   * @return whether all the actors were successfully dismissed.
   * @see Actor#dismissNow()
   */
  boolean dismissNow();

  /**
   * Removes an observer which should be notified of the dismissal of an actor in this set.
   *
   * @param observer the observer actor.
   * @return whether the observer was successfully removed from all the actors.
   * @see Actor#removeObserver(Actor)
   */
  boolean removeObserver(@NotNull Actor observer);

  /**
   * Tells the specified message to all the actors in this set.
   *
   * @param message the message instance (may be {@code null}).
   * @param headers the message headers.
   * @param sender  the sender actor.
   * @see Actor#tell(Object, Headers, Actor)
   */
  void tell(Object message, @NotNull Headers headers, @NotNull Actor sender);

  /**
   * Tells the specified batch of messages to all the actors in this set.
   *
   * @param messages the messages (may contains {@code null} objects).
   * @param headers  the message headers.
   * @param sender   the sender actor.
   * @see Actor#tellAll(Iterable, Headers, Actor)
   */
  void tellAll(@NotNull Iterable<?> messages, @NotNull Headers headers, @NotNull Actor sender);
}
