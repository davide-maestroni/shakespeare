package dm.shakespeare;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 06/06/2018.
 */
class CancellableExecutorService extends AbstractExecutorService {

  private static final ThreadLocal<Boolean> sInSubmit = new ThreadLocal<Boolean>() {

    @Override
    protected Boolean initialValue() {
      return false;
    }
  };

  private final ExecutorService mExecutor;
  private final Object mMutex = new Object();

  private WeakHashMap<Future<?>, Void> mFutures;

  CancellableExecutorService(@NotNull final ExecutorService executor) {
    mExecutor = ConstantConditions.notNull("executor", executor);
    mFutures = new WeakHashMap<Future<?>, Void>();
  }

  public void execute(@NotNull final Runnable runnable) {
    if (sInSubmit.get()) {
      mExecutor.execute(runnable);

    } else {
      register(mExecutor.submit(runnable));
    }
  }

  public void shutdown() {
    mExecutor.shutdown();
  }

  @NotNull
  public List<Runnable> shutdownNow() {
    return mExecutor.shutdownNow();
  }

  public boolean isShutdown() {
    return mExecutor.isShutdown();
  }

  public boolean isTerminated() {
    return mExecutor.isTerminated();
  }

  public boolean awaitTermination(final long timeout, @NotNull final TimeUnit unit) throws
      InterruptedException {
    return mExecutor.awaitTermination(timeout, unit);
  }

  @NotNull
  @Override
  public Future<?> submit(@NotNull final Runnable runnable) {
    final ThreadLocal<Boolean> inSubmit = sInSubmit;
    inSubmit.set(true);
    try {
      return register(super.submit(runnable));

    } finally {
      inSubmit.set(false);
    }
  }

  @NotNull
  @Override
  public <T> Future<T> submit(@NotNull final Runnable runnable, final T t) {
    final ThreadLocal<Boolean> inSubmit = sInSubmit;
    inSubmit.set(true);
    try {
      return register(super.submit(runnable, t));

    } finally {
      inSubmit.set(false);
    }
  }

  @NotNull
  @Override
  public <T> Future<T> submit(@NotNull final Callable<T> callable) {
    final ThreadLocal<Boolean> inSubmit = sInSubmit;
    inSubmit.set(true);
    try {
      return register(super.submit(callable));

    } finally {
      inSubmit.set(false);
    }
  }

  void cancel() {
    final HashSet<Future<?>> futures;
    synchronized (mMutex) {
      futures = new HashSet<Future<?>>(mFutures.keySet());
      mFutures = new WeakHashMap<Future<?>, Void>();
    }

    for (final Future<?> future : futures) {
      future.cancel(true);
    }
  }

  @NotNull
  private <T> Future<T> register(@NotNull final Future<T> future) {
    synchronized (mMutex) {
      mFutures.put(future, null);
    }

    return future;
  }
}
