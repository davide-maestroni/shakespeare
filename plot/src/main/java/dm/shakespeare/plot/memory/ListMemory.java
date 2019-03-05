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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 02/02/2019.
 */
public class ListMemory implements Memory {

  private final List<Object> mData;

  public ListMemory() {
    this(new ArrayList<Object>());
  }

  public ListMemory(@NotNull final List<Object> list) {
    mData = ConstantConditions.notNull("list", list);
  }

  @NotNull
  public Iterator<Object> iterator() {
    return new MemoryIterator();
  }

  public void put(final Object value) {
    mData.add(value);
  }

  private class MemoryIterator implements Iterator<Object> {

    private int mIndex;

    public boolean hasNext() {
      return mIndex < mData.size();
    }

    public Object next() {
      try {
        final Object next = mData.get(mIndex);
        ++mIndex;
        return next;

      } catch (final IndexOutOfBoundsException e) {
        throw new NoSuchElementException();
      }
    }

    public void remove() {
      throw new UnsupportedOperationException("remove");
    }
  }
}
