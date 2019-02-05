package dm.shakespeare.plot.memory;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

import dm.shakespeare.plot.Story.Memory;

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
