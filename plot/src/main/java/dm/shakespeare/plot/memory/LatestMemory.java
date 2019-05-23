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

import dm.shakespeare.util.CQueue;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 04/05/2019.
 */
public class LatestMemory implements Memory {

  private final WeakHashMap<MemoryIterator, Void> iterators =
      new WeakHashMap<MemoryIterator, Void>();
  private final int maxValues;
  private final CQueue<Object> values = new CQueue<Object>();

  public LatestMemory(final int maxValues) {
    this.maxValues = ConstantConditions.positive(maxValues);
  }

  @NotNull
  public Iterator<Object> iterator() {
    final MemoryIterator iterator = new MemoryIterator();
    iterators.put(iterator, null);
    return iterator;
  }

  public void put(final Object value) {
    final CQueue<Object> values = this.values;
    values.add(value);
    if (values.size() > maxValues) {
      values.removeFirst();
      for (final MemoryIterator iterator : iterators.keySet()) {
        iterator.decrementIndex();
      }
    }
  }

  private class MemoryIterator implements Iterator<Object> {

    private int index = 0;

    public boolean hasNext() {
      return ((index = Math.max(index, 0)) < values.size());
    }

    public Object next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return values.get(index++);
    }

    public void remove() {
      throw new UnsupportedOperationException("remove");
    }

    void decrementIndex() {
      --index;
    }
  }
}
