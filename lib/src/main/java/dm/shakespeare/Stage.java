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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.ActorSet;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.actor.Role;
import dm.shakespeare.concurrent.ExecutorServices;
import dm.shakespeare.function.Tester;
import dm.shakespeare.log.Logger;
import dm.shakespeare.message.DeadLetter;
import dm.shakespeare.util.ConstantConditions;

/**
 * Base implementation of a stage object.<br>
 * A stage acts as a roster of actors. Through a stage instance it is possible to create new actors
 * and then query for them trough their IDs.<br>
 * A stage allows for registration of observers of addition and dismissal of actor instances.
 */
public class Stage {

  // TODO: 28/02/2019 swagger converter
  // TODO: 2019-07-22 exceptions w/o message

  private static final Stage BACK_STAGE = new Stage() {

    @NotNull
    @Override
    public Actor createActor(@NotNull final Role role) {
      return newActor(randomId(), role);
    }

    @NotNull
    @Override
    public Actor createActor(@NotNull final String id, @NotNull final Role role) {
      return newActor(id, role);
    }
  };
  private static final StandInActor STAND_IN = new StandInActor();

  private final Actor actor;
  private final HashMap<String, Actor> actors = new HashMap<String, Actor>();
  private final Object mutex = new Object();
  private final CopyOnWriteArraySet<Actor> observers = new CopyOnWriteArraySet<Actor>();

  /**
   * Creates a new stage instance.
   */
  public Stage() {
    actor = newActor(randomId(), new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            if (message instanceof DeadLetter) {
              removeActor(envelop.getSender().getId());
            }
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    });
  }

  /**
   * Returns the back stage instance.<br>
   * This particular stage does not register its actors, so that no strong reference is retained.
   * The immediate consequence it is that, through this stage, it is possible to create multiple
   * actors with the same ID.<br>
   * Calls to get and find methods will never returned any result.
   *
   * @return the stage instance.
   */
  @NotNull
  public static Stage back() {
    return BACK_STAGE;
  }

  /**
   * Returns the stand-in actor instance.<br>
   * This actor methods will have no effect when invoked.
   *
   * @return the actor instance.
   */
  @NotNull
  public static Actor standIn() {
    return STAND_IN;
  }

  @NotNull
  private static Actor newActor(@NotNull final String id, @NotNull final Role role) {
    try {
      final int quota = role.getQuota(id);
      final Logger logger = role.getLogger(id);
      final ExecutorService executorService = role.getExecutorService(id);
      final Behavior behavior = role.getBehavior(id);
      final StandardAgent agent = new StandardAgent(behavior, executorService, logger);
      final StandardActor actor = new StandardActor(id, quota, agent);
      agent.setActor(actor);
      return actor;

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  private static String randomId() {
    return UUID.randomUUID().toString();
  }

  /**
   * Adds an observer that will be notified with a {@link StageSignal#CREATE} message when a new
   * actor instance is added to this stage, and a {@link StageSignal#DISMISS} message when an actor
   * is removed.
   *
   * @param observer the observer actor.
   */
  public void addObserver(@NotNull final Actor observer) {
    observers.add(ConstantConditions.notNull("observer", observer));
  }

  /**
   * Creates a new actor playing the specified role.
   *
   * @param role the actor role.
   * @return the new actor instance.
   */
  @NotNull
  public Actor createActor(@NotNull final Role role) {
    String id;
    synchronized (mutex) {
      final HashMap<String, Actor> actors = this.actors;
      do {
        id = randomId();
      } while (actors.containsKey(id));
      // reserve ID
      actors.put(id, null);
    }
    return registerActor(id, role);
  }

  /**
   * Creates a new actor, with the specified ID, playing the specified role.
   *
   * @param id   the actor ID.
   * @param role the actor role.
   * @return the new actor instance.
   * @throws IllegalStateException if an actor with the same ID already exists in this stage.
   */
  @NotNull
  public Actor createActor(@NotNull final String id, @NotNull final Role role) {
    synchronized (mutex) {
      final HashMap<String, Actor> actors = this.actors;
      if (actors.containsKey(id)) {
        throw new IllegalStateException("an actor with the same ID already exists: " + id);
      }
      // reserve ID
      actors.put(id, null);
    }
    return registerActor(id, role);
  }

  /**
   * Finds all the actors in this stage whose ID matches the specified pattern.
   *
   * @param idPattern the ID pattern.
   * @return the set of matching actors.
   */
  @NotNull
  public ActorSet findAll(@NotNull final Pattern idPattern) {
    return findAll(new PatternTester(idPattern));
  }

  /**
   * Finds all the actors in this stage verifying the conditions implemented by the specified
   * tester.
   *
   * @param tester the tester instance.
   * @return the set of matching actors.
   */
  @NotNull
  public ActorSet findAll(@NotNull final Tester<? super Actor> tester) {
    ConstantConditions.notNull("tester", tester);
    final HashSet<Actor> actors;
    synchronized (mutex) {
      actors = new HashSet<Actor>(this.actors.values());
    }

    try {
      final Iterator<Actor> iterator = actors.iterator();
      while (iterator.hasNext()) {
        final Actor actor = iterator.next();
        if ((actor == null) || !tester.test(actor)) {
          iterator.remove();
        }
      }

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
    return new StandardActorSet(actors);
  }

  /**
   * Finds any actor in this stage whose ID matches the specified pattern.
   *
   * @param idPattern the ID pattern.
   * @return the matching actor.
   * @throws IllegalArgumentException if no matching actor is found within this stage.
   */
  @Nullable
  public Actor findAny(@NotNull final Pattern idPattern) {
    return findAny(new PatternTester(idPattern));
  }

  /**
   * Finds any actor in this stage verifying the conditions implemented by the specified tester.
   *
   * @param tester the tester instance.
   * @return the matching actor.
   * @throws IllegalArgumentException if no matching actor is found within this stage.
   */
  @Nullable
  public Actor findAny(@NotNull final Tester<? super Actor> tester) {
    ConstantConditions.notNull("tester", tester);
    final ArrayList<Actor> actors;
    synchronized (mutex) {
      actors = new ArrayList<Actor>(this.actors.values());
    }

    try {
      for (final Actor actor : actors) {
        if ((actor != null) && tester.test(actor)) {
          return actor;
        }
      }

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  /**
   * Returns the actor in this stage with the specified ID.
   *
   * @param id the actor ID.
   * @return the matching actor.
   * @throws IllegalArgumentException if no matching actor is found within this stage.
   */
  @Nullable
  public Actor get(@NotNull final String id) {
    ConstantConditions.notNull("id", id);
    final Actor actor;
    synchronized (mutex) {
      actor = actors.get(id);
    }
    return actor;
  }

  /**
   * Returns all the actor in this stage.
   *
   * @return the set of actors.
   */
  @NotNull
  public ActorSet getAll() {
    final HashSet<Actor> actors;
    synchronized (mutex) {
      actors = new HashSet<Actor>(this.actors.values());
    }
    actors.remove(null);
    return new StandardActorSet(actors);
  }

  /**
   * Removes an observer which should be notified of the actor creation and dismissal.
   *
   * @param observer the observer actor.
   */
  public void removeObserver(@NotNull final Actor observer) {
    observers.remove(ConstantConditions.notNull("observer", observer));
  }

  /**
   * Creates a new actor with the specified ID.
   *
   * @param id   the actor ID.
   * @param role the actor role.
   * @return the new actor instance.
   * @throws Exception when an unexpected error occurs.
   */
  @NotNull
  protected Actor buildActor(@NotNull final String id, @NotNull final Role role) throws Exception {
    return newActor(id, role);
  }

  private void addActor(@NotNull final Actor actor) {
    final String id = actor.getId();
    synchronized (mutex) {
      actors.put(id, actor);
    }
    if (actor.addObserver(this.actor)) {
      for (final Actor observer : observers) {
        observer.tell(StageSignal.CREATE, Headers.empty(), actor);
      }

    } else {
      synchronized (mutex) {
        actors.remove(id, actor);
      }
      throw new IllegalStateException("cannot add observer to actor");
    }
  }

  @NotNull
  private Actor registerActor(@NotNull final String id, @NotNull final Role role) {
    try {
      final Actor actor = buildActor(id, role);
      addActor(actor);
      return actor;

    } catch (final RuntimeException e) {
      removeActor(id);
      throw e;

    } catch (final Exception e) {
      removeActor(id);
      throw new RuntimeException(e);
    }
  }

  private void removeActor(@NotNull final String id) {
    final Actor actor;
    synchronized (mutex) {
      actor = actors.remove(id);
    }

    if (actor != null) {
      actor.removeObserver(this.actor);
      for (final Actor observer : observers) {
        observer.tell(StageSignal.DISMISS, Headers.empty(), actor);
      }
    }
  }

  /**
   * Stage signalling messages.
   */
  public enum StageSignal {

    /**
     * Message notifying that the sender actor has been added to a stage.
     */
    CREATE,

    /**
     * Message notifying that the sender actor has been removed from a stage.
     */
    DISMISS

  }

  private static class PatternTester implements Tester<Actor> {

    private final Pattern pattern;

    private PatternTester(@NotNull final Pattern idPattern) {
      pattern = ConstantConditions.notNull("idPattern", idPattern);
    }

    public boolean test(final Actor actor) {
      return pattern.matcher(actor.getId()).matches();
    }
  }
}
