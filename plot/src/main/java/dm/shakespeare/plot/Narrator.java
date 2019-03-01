package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import dm.shakespeare.BackStage;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 02/17/2019.
 */
public final class Narrator<T> implements Closeable {

  static final Object AVAILABLE = new Object();
  static final Object NULL = new Object();
  static final Object STOP = new Object();

  private final AtomicBoolean mIsClosed = new AtomicBoolean();
  private final BlockingQueue<Object> mQueue;

  private volatile Actor mActor;
  private volatile Throwable mException;

  public Narrator() {
    this(new LinkedBlockingQueue<Object>());
  }

  public Narrator(@NotNull final BlockingQueue<Object> queue) {
    mQueue = ConstantConditions.notNull("queue", queue);
  }

  public void close() {
    if (!mIsClosed.getAndSet(true)) {
      if (mException == null) {
        mException = new NarrationStoppedException();
      }
      final Actor actor = mActor;
      if (mQueue.isEmpty() && (actor != null)) {
        actor.tell(AVAILABLE, null, BackStage.standIn());
      }
    }
  }

  public boolean report(@NotNull final Throwable incident, final long timeout,
      @NotNull final TimeUnit unit) throws InterruptedException {
    return enqueue(new Conflict(incident), timeout, unit);
  }

  public boolean tell(final T effect, final long timeout, @NotNull final TimeUnit unit) throws
      InterruptedException {
    return enqueue((effect != null) ? effect : NULL, timeout, unit);
  }

  void cancel(@NotNull final Throwable cause) {
    if (mException == null) {
      mException = cause;
    }
    mQueue.clear();
  }

  void setActor(@NotNull final Actor actor) {
    mActor = actor;
  }

  @Nullable
  Object takeEffect() {
    final Object effect = mQueue.poll();
    if ((effect == null) && mIsClosed.get()) {
      return STOP;
    }
    return effect;
  }

  private boolean enqueue(@NotNull final Object resolution, final long timeout,
      @NotNull final TimeUnit unit) throws InterruptedException {
    final Throwable exception = mException;
    if (exception != null) {
      if (exception instanceof RuntimeException) {
        throw (RuntimeException) exception;

      } else {
        throw new PlotFailureException(exception);
      }
    }
    final BlockingQueue<Object> queue = mQueue;
    final boolean wasEmpty = queue.isEmpty();
    if (queue.offer(resolution, timeout, unit)) {
      final Actor actor = mActor;
      if (wasEmpty && (actor != null)) {
        actor.tell(AVAILABLE, null, BackStage.standIn());
      }
      return true;
    }
    return false;
  }
}
