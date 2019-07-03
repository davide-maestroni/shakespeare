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

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.util.ConstantConditions;

/**
 * Class implementing an actor set.
 */
class StandardActorSet extends AbstractActorSet {

  private final Set<Actor> actors;

  /**
   * Creates a new actor set.
   *
   * @param actors the set of actors.
   */
  StandardActorSet(@NotNull final Set<? extends Actor> actors) {
    this.actors = Collections.unmodifiableSet(ConstantConditions.notNullElements("actors", actors));
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
