package dm.shakespeare.plot.memory;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dm.shakespeare.plot.Story.Memory;
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
      return mData.get(mIndex++);
    }

    public void remove() {
      throw new UnsupportedOperationException("remove");
    }
  }
}
