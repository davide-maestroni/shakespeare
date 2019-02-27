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

  private final Iterator<T> mIterator;

  CreatorIterable(@NotNull final NullaryFunction<? extends T> effectsCreator) {
    mIterator = new CreatorIterator<T>(effectsCreator);
  }

  @NotNull
  public Iterator<T> iterator() {
    return mIterator;
  }

  private static class CreatorIterator<T> implements Iterator<T> {

    private final NullaryFunction<? extends T> mEffectsCreator;

    CreatorIterator(@NotNull final NullaryFunction<? extends T> effectsCreator) {
      mEffectsCreator = ConstantConditions.notNull("effectsCreator", effectsCreator);
    }

    public boolean hasNext() {
      return true;
    }

    public T next() {
      try {
        return mEffectsCreator.call();

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
