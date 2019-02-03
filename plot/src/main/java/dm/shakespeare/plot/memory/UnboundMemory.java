package dm.shakespeare.plot.memory;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import dm.shakespeare.plot.Story.Memory;

/**
 * Created by davide-maestroni on 02/02/2019.
 */
public class UnboundMemory implements Memory {

  private final ArrayList<Object> mData = new ArrayList<Object>();

  public Object get(final int index) {
    return mData.get(index);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public Iterable<Object> getAll() {
    return (ArrayList<Object>) mData.clone();
  }

  public boolean has(final int index) {
    return (index >= 0) && (index < mData.size());
  }

  public int next(final int index) {
    return index + 1;
  }

  public void put(final Object value) {
    mData.add(value);
  }
}
