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

package dm.shakespeare;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Role;
import dm.shakespeare.function.Observer;
import dm.shakespeare.log.Logger;
import dm.shakespeare.util.ConstantConditions;

/**
 * Utility class acting as a simple creator of actors.<br>
 * The actor references are not retained by this class. It's up to the caller to avoid garbage
 * collection of the returned instances.
 */
public class BackStage {

  /**
   * Stand-in actor instance.<br>
   * This actor methods will have no effect when invoked.
   */
  public static final Actor STAND_IN = new StandInActor();

  private static final Observer<Actor> EMPTY_REMOVER = new Observer<Actor>() {

    public void accept(final Actor actor) {
    }
  };

  /**
   * Avoid explicit instantiation.
   */
  private BackStage() {
    ConstantConditions.avoid();
  }

  /**
   * Creates a new actor with a random ID.
   *
   * @param role the actor role.
   * @return the new actor instance.
   */
  @NotNull
  public static Actor newActor(@NotNull final Role role) {
    return newActor(UUID.randomUUID().toString(), role);
  }

  /**
   * Creates a new actor with the specified ID.
   *
   * @param id   the actor ID.
   * @param role the actor role.
   * @return the new actor instance.
   */
  @NotNull
  public static Actor newActor(@NotNull final String id, @NotNull final Role role) {
    try {
      return newActor(id, role, EMPTY_REMOVER);

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates a new actor with the specified ID.
   *
   * @param id      the actor ID.
   * @param role    the actor role.
   * @param remover the observer to be called when the actor is dismissed.
   * @return the new actor instance.
   * @throws Exception when an unexpected error occurs.
   */
  @NotNull
  static Actor newActor(@NotNull final String id, @NotNull final Role role,
      @NotNull final Observer<Actor> remover) throws Exception {
    final int quota = role.getQuota(id);
    final Logger logger = role.getLogger(id);
    final ExecutorService executorService = role.getExecutorService(id);
    final Behavior behavior = role.getBehavior(id);
    final LocalContext context = new LocalContext(remover, behavior, executorService, logger);
    final LocalActor actor = new LocalActor(id, quota, context);
    context.setActor(actor);
    return actor;
  }
}
