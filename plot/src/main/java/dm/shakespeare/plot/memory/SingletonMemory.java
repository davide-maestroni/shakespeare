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

package dm.shakespeare.plot.memory;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.WeakHashMap;

/**
 * Created by davide-maestroni on 02/05/2019.
 */
public class SingletonMemory implements Memory {

  private final WeakHashMap<MemoryIterator, Void> iterators =
      new WeakHashMap<MemoryIterator, Void>();

  private boolean hasValue;
  private Object value;

  @NotNull
  public Iterator<Object> iterator() {
    final MemoryIterator iterator = new MemoryIterator(hasValue);
    iterators.put(iterator, null);
    return iterator;
  }

  public void put(final Object value) {
    hasValue = true;
    this.value = value;
    for (final MemoryIterator iterator : iterators.keySet()) {
      iterator.setNext();
    }
  }

  private class MemoryIterator implements Iterator<Object> {

    private boolean hasNext;

    private MemoryIterator(final boolean hasNext) {
      this.hasNext = hasNext;
    }

    public boolean hasNext() {
      return hasNext;
    }

    public Object next() {
      if (!hasNext) {
        throw new NoSuchElementException();
      }
      hasNext = false;
      return value;
    }

    public void remove() {
      throw new UnsupportedOperationException("remove");
    }

    void setNext() {
      hasNext = true;
    }
  }
}
