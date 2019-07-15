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
import java.util.concurrent.TimeUnit;

import dm.shakespeare.util.CQueue;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 04/05/2019.
 */
public class ExpiringMemory implements Memory {

  private final long expireTime;
  private final WeakHashMap<MemoryIterator, Void> iterators =
      new WeakHashMap<MemoryIterator, Void>();
  private final CQueue<TimedReference> references = new CQueue<TimedReference>();
  private final TimeUnit unit;

  public ExpiringMemory(final long expireTime, @NotNull final TimeUnit unit) {
    this.expireTime = ConstantConditions.positive(expireTime);
    this.unit = ConstantConditions.notNull("unit", unit);
  }

  @NotNull
  public Iterator<Object> iterator() {
    final MemoryIterator iterator = new MemoryIterator();
    iterators.put(iterator, null);
    return iterator;
  }

  public void put(final Object value) {
    references.add(new TimedReference(value));
    prune();
  }

  private void prune() {
    final long timeOffset = System.currentTimeMillis() - unit.toMillis(expireTime);
    final CQueue<TimedReference> references = this.references;
    int removed = 0;
    while (!references.isEmpty() && (references.peekFirst().getTime() < timeOffset)) {
      references.removeFirst();
      ++removed;
    }

    if (removed > 0) {
      for (final MemoryIterator iterator : iterators.keySet()) {
        iterator.decrementIndex(removed);
      }
    }
  }

  private static class TimedReference {

    private final Object object;
    private final long timestamp = System.currentTimeMillis();

    private TimedReference(final Object object) {
      this.object = object;
    }

    Object getObject() {
      return object;
    }

    long getTime() {
      return timestamp;
    }
  }

  private class MemoryIterator implements Iterator<Object> {

    private int index = 0;

    public boolean hasNext() {
      prune();
      return ((index = Math.max(index, 0)) < references.size());
    }

    public Object next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return references.get(index++).getObject();
    }

    public void remove() {
      throw new UnsupportedOperationException("remove");
    }

    void decrementIndex(final int delta) {
      index -= delta;
    }
  }
}
