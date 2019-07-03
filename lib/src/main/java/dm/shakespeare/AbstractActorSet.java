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

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.ActorSet;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.util.ConstantConditions;

/**
 * Abstract implementation of an {@code ActorSet}.
 */
public abstract class AbstractActorSet extends AbstractSet<Actor> implements ActorSet {

  /**
   * {@inheritDoc}
   */
  public boolean addObserver(@NotNull final Actor observer) {
    if (!isEmpty()) {
      boolean added = true;
      for (final Actor actor : this) {
        if (!actor.addObserver(observer)) {
          added = false;
        }
      }
      return added;
    }
    ConstantConditions.notNull("observer", observer);
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public boolean dismiss() {
    if (!isEmpty()) {
      boolean dimissed = true;
      for (final Actor actor : this) {
        if (!actor.dismiss()) {
          dimissed = false;
        }
      }
      return dimissed;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public boolean dismissLazy() {
    if (!isEmpty()) {
      boolean dimissed = true;
      for (final Actor actor : this) {
        if (!actor.dismissLazy()) {
          dimissed = false;
        }
      }
      return dimissed;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public boolean dismissNow() {
    if (!isEmpty()) {
      boolean dimissed = true;
      for (final Actor actor : this) {
        if (!actor.dismissNow()) {
          dimissed = false;
        }
      }
      return dimissed;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public boolean removeObserver(@NotNull final Actor observer) {
    if (!isEmpty()) {
      boolean removed = true;
      for (final Actor actor : this) {
        if (!actor.removeObserver(observer)) {
          removed = false;
        }
      }
      return removed;
    }
    ConstantConditions.notNull("observer", observer);
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public void tell(final Object message, @NotNull final Headers headers,
      @NotNull final Actor sender) {
    if (!isEmpty()) {
      for (final Actor actor : this) {
        actor.tell(message, headers, sender);
      }

    } else {
      ConstantConditions.notNull("headers", headers);
      ConstantConditions.notNull("sender", sender);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void tellAll(@NotNull final Iterable<?> messages, @NotNull final Headers headers,
      @NotNull final Actor sender) {
    if (!isEmpty()) {
      for (final Actor actor : this) {
        actor.tellAll(messages, headers, sender);
      }

    } else {
      ConstantConditions.notNull("headers", headers);
      ConstantConditions.notNull("sender", sender);
    }
  }
}
