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

import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.actor.BehaviorBuilder.Handler;
import dm.shakespeare.function.Mapper;
import dm.shakespeare.util.ConstantConditions;

/**
 * {@code Handler} implementation wrapping a {@link Mapper} function.
 *
 * @param <T> the observed type.
 */
class ApplyHandler<T> implements Handler<T> {

  private final Mapper<T, ?> mMapper;

  /**
   * Creates a new handler wrapping the specified mapper instance.
   *
   * @param mapper the mapper to wrap.
   */
  ApplyHandler(@NotNull final Mapper<T, ?> mapper) {
    mMapper = ConstantConditions.notNull("mapper", mapper);
  }

  /**
   * {@inheritDoc}
   */
  public void handle(final T message, @NotNull final Envelop envelop,
      @NotNull final Context context) throws Exception {
    envelop.getSender()
        .tell(mMapper.apply(message), envelop.getOptions().threadOnly(), context.getSelf());
  }
}
