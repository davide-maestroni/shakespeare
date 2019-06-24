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

import java.io.Serializable;

import dm.shakespeare.actor.Behavior.Agent;
import dm.shakespeare.actor.BehaviorBuilder.Handler;
import dm.shakespeare.config.BuildConfig;
import dm.shakespeare.function.Observer;
import dm.shakespeare.util.ConstantConditions;

/**
 * {@code Handler} implementation wrapping an {@link Observer} function.
 *
 * @param <T> the observed type.
 */
class AcceptHandler<T> implements Handler<T>, Serializable {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private final Observer<T> observer;

  /**
   * Creates a new empty handler.<br>
   * Usually needed during deserialization.
   */
  AcceptHandler() {
    observer = null;
  }

  /**
   * Creates a new handler wrapping the specified observer instance.<br>
   * The returned instance will be serializable only if the observer instance effectively is.
   *
   * @param observer the observer to wrap.
   */
  AcceptHandler(@NotNull final Observer<T> observer) {
    this.observer = ConstantConditions.notNull("observer", observer);
  }

  /**
   * Returns the wrapped observer.<br>
   * Usually needed during serialization.
   *
   * @return the observer instance.
   */
  @NotNull
  public Observer<T> getObserver() {
    return observer;
  }

  /**
   * {@inheritDoc}
   */
  public void handle(final T message, @NotNull final Envelop envelop,
      @NotNull final Agent agent) throws Exception {
    observer.accept(message);
  }
}
