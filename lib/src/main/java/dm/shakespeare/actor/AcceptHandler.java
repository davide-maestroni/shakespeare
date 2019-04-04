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

import dm.shakespeare.actor.Behavior.Agent;
import dm.shakespeare.actor.BehaviorBuilder.Handler;
import dm.shakespeare.function.Observer;
import dm.shakespeare.util.ConstantConditions;

/**
 * {@code Handler} implementation wrapping an {@link Observer} function.
 *
 * @param <T> the observed type.
 */
class AcceptHandler<T> implements Handler<T> {

  private final Observer<T> mObserver;

  /**
   * Creates a new handler wrapping the specified observer instance.
   *
   * @param observer the observer to wrap.
   */
  AcceptHandler(@NotNull final Observer<T> observer) {
    mObserver = ConstantConditions.notNull("observer", observer);
  }

  /**
   * {@inheritDoc}
   */
  public void handle(final T message, @NotNull final Envelop envelop,
      @NotNull final Agent agent) throws Exception {
    mObserver.accept(message);
  }
}
