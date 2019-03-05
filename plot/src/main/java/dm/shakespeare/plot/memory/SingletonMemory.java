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

/**
 * Created by davide-maestroni on 02/05/2019.
 */
public class SingletonMemory implements Memory {

  private boolean mHasIterator;
  private boolean mHasValue;
  private Object mValue;

  @NotNull
  public Iterator<Object> iterator() {
    if (mHasIterator) {
      throw new IllegalStateException("singleton memory cannot have more than one iterator");
    }
    mHasIterator = true;
    return new MemoryIterator();
  }

  public void put(final Object value) {
    mHasValue = true;
    mValue = value;
  }

  private class MemoryIterator implements Iterator<Object> {

    public boolean hasNext() {
      return mHasValue;
    }

    public Object next() {
      if (!mHasValue) {
        throw new NoSuchElementException();
      }
      mHasValue = false;
      return mValue;
    }

    public void remove() {
      throw new UnsupportedOperationException("remove");
    }
  }
}
