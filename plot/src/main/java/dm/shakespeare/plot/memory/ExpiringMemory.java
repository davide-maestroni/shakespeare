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

  private final long mExpireTime;
  private final WeakHashMap<MemoryIterator, Void> mIterators =
      new WeakHashMap<MemoryIterator, Void>();
  private final CQueue<TimedReference> mReferences = new CQueue<TimedReference>();
  private final TimeUnit mUnit;

  public ExpiringMemory(final long expireTime, @NotNull final TimeUnit unit) {
    mExpireTime = ConstantConditions.positive(expireTime);
    mUnit = ConstantConditions.notNull("unit", unit);
  }

  @NotNull
  public Iterator<Object> iterator() {
    final MemoryIterator iterator = new MemoryIterator();
    mIterators.put(iterator, null);
    return iterator;
  }

  public void put(final Object value) {
    mReferences.add(new TimedReference(value));
    prune();
  }

  private void prune() {
    final long timeOffset = System.currentTimeMillis() - mUnit.toMillis(mExpireTime);
    final CQueue<TimedReference> references = mReferences;
    int removed = 0;
    while (!references.isEmpty() && (references.peekFirst().getTime() < timeOffset)) {
      references.removeFirst();
      ++removed;
    }

    if (removed > 0) {
      for (final MemoryIterator iterator : mIterators.keySet()) {
        iterator.decrementIndex(removed);
      }
    }
  }

  private static class TimedReference {

    private final Object mObject;
    private final long timestamp = System.currentTimeMillis();

    private TimedReference(final Object object) {
      mObject = object;
    }

    Object getObject() {
      return mObject;
    }

    long getTime() {
      return timestamp;
    }
  }

  private class MemoryIterator implements Iterator<Object> {

    private int mIndex = 0;

    public boolean hasNext() {
      prune();
      return ((mIndex = Math.max(mIndex, 0)) < mReferences.size());
    }

    public Object next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return mReferences.get(mIndex++);
    }

    public void remove() {
      throw new UnsupportedOperationException("remove");
    }

    void decrementIndex(final int delta) {
      mIndex -= delta;
    }
  }
}
