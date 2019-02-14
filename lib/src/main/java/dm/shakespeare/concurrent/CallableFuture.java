package dm.shakespeare.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 09/24/2018.
 */
class CallableFuture<V> extends AbstractFuture<V> {

  private final Callable<V> mCallable;

  CallableFuture(@NotNull final ExecutorService executor, @NotNull final Callable<V> callable,
      final long timestamp) {
    super(executor, timestamp);
    mCallable = ConstantConditions.notNull("callable", callable);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + mCallable.hashCode();
    return result;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if ((o == null) || getClass() != o.getClass()) {
      return false;
    }

    if (!super.equals(o)) {
      return false;
    }
    final CallableFuture<?> that = (CallableFuture<?>) o;
    return mCallable.equals(that.mCallable);
  }

  @NotNull
  Future<V> submitTo(@NotNull final ExecutorService executor) {
    return executor.submit(mCallable);
  }
}
