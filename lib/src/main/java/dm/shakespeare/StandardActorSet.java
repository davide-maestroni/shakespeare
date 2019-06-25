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

import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.ActorSet;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.util.ConstantConditions;

/**
 * Class implementing an actor set.
 */
class StandardActorSet extends AbstractSet<Actor> implements ActorSet {

  private final Set<Actor> actors;

  /**
   * Creates a new actor set.
   *
   * @param actors the set of actors.
   */
  StandardActorSet(@NotNull final Set<? extends Actor> actors) {
    this.actors = Collections.unmodifiableSet(ConstantConditions.notNullElements("actors", actors));
  }

  public boolean addObserver(@NotNull final Actor observer) {
    if (!isEmpty()) {
      boolean added = true;
      for (final Actor actor : actors) {
        if (!actor.addObserver(observer)) {
          added = false;
        }
      }
      return added;
    }
    ConstantConditions.notNull("observer", observer);
    return false;
  }

  public boolean dismiss() {
    if (!isEmpty()) {
      boolean dimissed = true;
      for (final Actor actor : actors) {
        if (!actor.dismiss()) {
          dimissed = false;
        }
      }
      return dimissed;
    }
    return false;
  }

  public boolean dismissLazy() {
    if (!isEmpty()) {
      boolean dimissed = true;
      for (final Actor actor : actors) {
        if (!actor.dismissLazy()) {
          dimissed = false;
        }
      }
      return dimissed;
    }
    return false;
  }

  public boolean dismissNow() {
    if (!isEmpty()) {
      boolean dimissed = true;
      for (final Actor actor : actors) {
        if (!actor.dismissNow()) {
          dimissed = false;
        }
      }
      return dimissed;
    }
    return false;
  }

  public boolean removeObserver(@NotNull final Actor observer) {
    if (!isEmpty()) {
      boolean removed = true;
      for (final Actor actor : actors) {
        if (!actor.removeObserver(observer)) {
          removed = false;
        }
      }
      return removed;
    }
    ConstantConditions.notNull("observer", observer);
    return false;
  }

  public void tell(final Object message, @NotNull final Headers headers,
      @NotNull final Actor sender) {
    if (!isEmpty()) {
      for (final Actor actor : actors) {
        actor.tell(message, headers, sender);
      }

    } else {
      ConstantConditions.notNull("headers", headers);
      ConstantConditions.notNull("sender", sender);
    }
  }

  public void tellAll(@NotNull final Iterable<?> messages, @NotNull final Headers headers,
      @NotNull final Actor sender) {
    if (!isEmpty()) {
      for (final Actor actor : actors) {
        actor.tellAll(messages, headers, sender);
      }

    } else {
      ConstantConditions.notNull("headers", headers);
      ConstantConditions.notNull("sender", sender);
    }
  }

  @NotNull
  public Iterator<Actor> iterator() {
    return actors.iterator();
  }

  public int size() {
    return actors.size();
  }

  @Override
  public boolean isEmpty() {
    return actors.isEmpty();
  }

  @Override
  public boolean contains(final Object o) {
    return actors.contains(o);
  }
}
