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

package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.plot.function.NullaryFunction;
import dm.shakespeare.plot.function.UnaryFunction;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 04/05/2019.
 */
public class Handlers {

  // TODO: 05/04/2019 skip(?)

  private static final UnaryFunction<?, ?> IDENTITY = new UnaryFunction<Object, Object>() {

    public Object call(final Object first) {
      return first;
    }
  };

  private Handlers() {
    ConstantConditions.avoid();
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public static <T> UnaryFunction<T, T> identity() {
    return (UnaryFunction<T, T>) IDENTITY;
  }

  @NotNull
  public static NullaryFunction<Event<Boolean>> take(final int maxEffects) {
    return new TakeFunction(maxEffects);
  }

  private static class TakeFunction implements NullaryFunction<Event<Boolean>> {

    private final int maxEffects;

    private int count = 0;

    private TakeFunction(final int maxEffects) {
      this.maxEffects = ConstantConditions.notNegative(maxEffects);
    }

    public Event<Boolean> call() {
      return Event.ofEffect(++count <= maxEffects);
    }
  }
}
