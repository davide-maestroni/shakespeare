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

import java.util.Iterator;
import java.util.NoSuchElementException;

import dm.shakespeare.plot.function.NullaryFunction;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 02/19/2019.
 */
class CreatorIterable<T> implements Iterable<T> {

  private final Iterator<T> iterator;

  CreatorIterable(@NotNull final NullaryFunction<? extends T> effectsCreator) {
    iterator = new CreatorIterator<T>(effectsCreator);
  }

  @NotNull
  public Iterator<T> iterator() {
    return iterator;
  }

  private static class CreatorIterator<T> implements Iterator<T> {

    private final NullaryFunction<? extends T> effectsCreator;

    CreatorIterator(@NotNull final NullaryFunction<? extends T> effectsCreator) {
      this.effectsCreator = ConstantConditions.notNull("effectsCreator", effectsCreator);
    }

    public boolean hasNext() {
      return true;
    }

    public T next() {
      try {
        return effectsCreator.call();

      } catch (final Exception e) {
        if (e instanceof RuntimeException) {
          throw (RuntimeException) e;
        }

        throw new NoSuchElementException(e.getMessage());
      }
    }

    public void remove() {
      throw new UnsupportedOperationException("remove");
    }
  }
}
