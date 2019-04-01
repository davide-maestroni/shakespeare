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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;
import java.util.regex.Pattern;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.ActorSet;
import dm.shakespeare.actor.Script;
import dm.shakespeare.actor.Stage;
import dm.shakespeare.function.Tester;
import dm.shakespeare.util.ConstantConditions;

/**
 * Base abstract implementation of a {@code Stage}.<br>
 * The implementing classes just need to provide new actors instantiation. The ID validation and
 * actor registration are automatically performed by this class.
 */
public abstract class AbstractStage implements Stage {

  private final HashMap<String, Actor> mActors = new HashMap<String, Actor>();
  private final Object mMutex = new Object();

  /**
   * {@inheritDoc}
   */
  @NotNull
  public ActorSet findAll(@NotNull final Pattern idPattern) {
    return findAll(new PatternTester(idPattern));
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  public ActorSet findAll(@NotNull final Tester<? super Actor> tester) {
    ConstantConditions.notNull("tester", tester);
    final HashSet<Actor> actors;
    synchronized (mMutex) {
      actors = new HashSet<Actor>(mActors.values());
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
    return new LocalActorSet(actors);
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  public Actor findAny(@NotNull final Pattern idPattern) {
    return findAny(new PatternTester(idPattern));
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  public Actor findAny(@NotNull final Tester<? super Actor> tester) {
    ConstantConditions.notNull("tester", tester);
    final ArrayList<Actor> actors;
    synchronized (mMutex) {
      actors = new ArrayList<Actor>(mActors.values());
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

    throw new IllegalArgumentException("cannot find an actor satisfying the tester: " + tester);
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  public Actor get(@NotNull final String id) {
    ConstantConditions.notNull("id", id);
    final Actor actor;
    synchronized (mMutex) {
      actor = mActors.get(id);
    }

    if (actor == null) {
      throw new IllegalArgumentException("cannot find an actor with ID: " + id);
    }
    return actor;
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  public ActorSet getAll() {
    final HashSet<Actor> actors;
    synchronized (mMutex) {
      actors = new HashSet<Actor>(mActors.values());
    }
    actors.remove(null);
    return new LocalActorSet(actors);
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  public Actor newActor(@NotNull final Script script) {
    String id;
    synchronized (mMutex) {
      final HashMap<String, Actor> actors = mActors;
      do {
        id = UUID.randomUUID().toString();
      } while (actors.containsKey(id));
      // reserve ID
      actors.put(id, null);
    }
    return registerActor(id, script);
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  public Actor newActor(@NotNull final String id, @NotNull final Script script) {
    synchronized (mMutex) {
      final HashMap<String, Actor> actors = mActors;
      if (actors.containsKey(id)) {
        throw new IllegalStateException("an actor with the same ID already exists: " + id);
      }
      // reserve ID
      actors.put(id, null);
    }
    return registerActor(id, script);
  }

  /**
   * Creates a new actor with the specified ID.
   *
   * @param id     the actor ID.
   * @param script the actor script.
   * @return the new actor instance.
   * @throws Exception when an unexpected error occurs.
   */
  @NotNull
  protected abstract Actor createActor(@NotNull String id, @NotNull Script script) throws Exception;

  void addActor(@NotNull final String id, @NotNull final Actor actor) {
    synchronized (mMutex) {
      mActors.put(id, actor);
    }
  }

  void removeActor(@NotNull final String id) {
    synchronized (mMutex) {
      mActors.remove(id);
    }
  }

  @NotNull
  private Actor registerActor(@NotNull final String id, @NotNull final Script script) {
    try {
      final Actor actor = createActor(id, script);
      addActor(id, actor);
      return actor;

    } catch (final RuntimeException e) {
      removeActor(id);
      throw e;

    } catch (final Exception e) {
      removeActor(id);
      throw new RuntimeException(e);
    }
  }

  private static class PatternTester implements Tester<Actor> {

    private final Pattern mPattern;

    private PatternTester(@NotNull final Pattern idPattern) {
      mPattern = ConstantConditions.notNull("idPattern", idPattern);
    }

    public boolean test(final Actor actor) {
      return mPattern.matcher(actor.getId()).matches();
    }
  }
}
