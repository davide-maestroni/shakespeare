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

import java.util.regex.Pattern;

import dm.shakespeare.function.Tester;

/**
 * Interface defining a stage object.<br>
 * A stage acts as a roster of actors. Through a stage instance it is possible to create new actors
 * and then query for them trough their IDs.<br>
 * A stage allows for registering of actors observing addition and dismissal of actor instances
 * within itself.
 */
public interface Stage {

  /**
   * Adds an observer that will be notified with a {@link dm.shakespeare.message.Create Create}
   * message when a new actor instance is added to this stage, and a
   * {@link dm.shakespeare.message.Dismiss Dismiss} message when an actor is removed.
   *
   * @param observer the observer actor.
   */
  void addObserver(@NotNull Actor observer);

  /**
   * Finds all the actors in this stage whose ID matches the specified pattern.
   *
   * @param idPattern the ID pattern.
   * @return the set of matching actors.
   */
  @NotNull
  ActorSet findAll(@NotNull Pattern idPattern);

  /**
   * Finds all the actors in this stage verifying the conditions implemented by the specified
   * tester.
   *
   * @param tester the tester instance.
   * @return the set of matching actors.
   */
  @NotNull
  ActorSet findAll(@NotNull Tester<? super Actor> tester);

  /**
   * Finds any actor in this stage whose ID matches the specified pattern.
   *
   * @param idPattern the ID pattern.
   * @return the matching actor.
   * @throws IllegalArgumentException if no matching actor is found within this stage.
   */
  @NotNull
  Actor findAny(@NotNull Pattern idPattern);

  /**
   * Finds any actor in this stage verifying the conditions implemented by the specified tester.
   *
   * @param tester the tester instance.
   * @return the matching actor.
   * @throws IllegalArgumentException if no matching actor is found within this stage.
   */
  @NotNull
  Actor findAny(@NotNull Tester<? super Actor> tester);

  /**
   * Returns the actor in this stage with the specified ID.
   *
   * @param id the actor ID.
   * @return the matching actor.
   * @throws IllegalArgumentException if no matching actor is found within this stage.
   */
  @NotNull
  Actor get(@NotNull String id);

  /**
   * Returns all the actor in this stage.
   *
   * @return the set of actors.
   */
  @NotNull
  ActorSet getAll();

  /**
   * Creates a new actor playing the specified role.
   *
   * @param role the actor role.
   * @return the new actor instance.
   */
  @NotNull
  Actor newActor(@NotNull Role role);

  /**
   * Creates a new actor, with the specified ID, playing the specified role.
   *
   * @param id   the actor ID.
   * @param role the actor role.
   * @return the new actor instance.
   * @throws IllegalStateException if an actor with the same ID already exists in this stage.
   */
  @NotNull
  Actor newActor(@NotNull String id, @NotNull Role role);

  /**
   * Removes an observer which should be notified of the actor creation and dismissal.
   *
   * @param observer the observer actor.
   */
  void removeObserver(@NotNull Actor observer);
}
