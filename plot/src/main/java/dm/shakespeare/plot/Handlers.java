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

import java.io.Serializable;

import dm.shakespeare.plot.config.BuildConfig;
import dm.shakespeare.plot.function.NullaryFunction;
import dm.shakespeare.plot.function.UnaryFunction;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 04/05/2019.
 */
public class Handlers {

  private static final IdentityFunction IDENTITY = new IdentityFunction();

  private Handlers() {
    ConstantConditions.avoid();
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public static <T> UnaryFunction<T, T> identity() {
    return (UnaryFunction<T, T>) IDENTITY;
  }

  @NotNull
  public static <T> UnaryFunction<T, Story<T>> skip(final int numEffects) {
    return new SkipFunction<T>(numEffects);
  }

  @NotNull
  public static NullaryFunction<Event<Boolean>> take(final int maxEffects) {
    return new TakeFunction(maxEffects);
  }

  private static class IdentityFunction implements UnaryFunction<Object, Object>, Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    public Object call(final Object effect) {
      return effect;
    }
  }

  private static class SkipFunction<T> implements UnaryFunction<T, Story<T>>, Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final int numEffects;

    private int count = 0;

    private SkipFunction() {
      numEffects = 0;
    }

    private SkipFunction(final int numEffects) {
      this.numEffects = ConstantConditions.notNegative("numEffects", numEffects);
    }

    public Story<T> call(final T effect) {
      return (++count > numEffects) ? Story.ofSingleEffect(effect) : null;
    }

    public int getCount() {
      return count;
    }

    public int getNumEffects() {
      return numEffects;
    }
  }

  private static class TakeFunction implements NullaryFunction<Event<Boolean>>, Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final int maxEffects;

    private int count = 0;

    private TakeFunction() {
      maxEffects = 0;
    }

    private TakeFunction(final int maxEffects) {
      this.maxEffects = ConstantConditions.notNegative("maxEffects", maxEffects);
    }

    public Event<Boolean> call() {
      return Event.ofEffect(++count <= maxEffects);
    }

    public int getCount() {
      return count;
    }

    public int getMaxEffects() {
      return maxEffects;
    }
  }
}
