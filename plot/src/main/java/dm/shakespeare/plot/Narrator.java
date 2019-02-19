package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
    if (mException == null) {
      mException = new NarrationStoppedException();
      enqueue(STOP);
    }
  }

  @NotNull
  public Narrator<T> report(@NotNull final Throwable incident) throws Exception {
    resolve(new Conflict(incident));
    return this;
  }

  @NotNull
  public Narrator<T> tell(final T effect) throws Exception {
    resolve((effect != null) ? effect : NULL);
    return this;
  }

  void cancel(@NotNull final Throwable cause) {
    mException = cause;
    mQueue.clear();
  }

  void setActor(@NotNull final Actor actor) {
    mActor = actor;
  }

  @Nullable
  Object takeEffect() {
    return mQueue.poll();
  }

  private void enqueue(@NotNull final Object resolution) {
    final BlockingQueue<Object> queue = mQueue;
    final boolean wasEmpty = queue.isEmpty();
    queue.add(resolution);
    final Actor actor = mActor;
    if (wasEmpty && (actor != null)) {
      actor.tell(AVAILABLE, null, BackStage.standIn());
    }
  }

  private void resolve(@NotNull final Object resolution) throws Exception {
    // TODO: 17/02/2019 detect deadlock
    final Throwable exception = mException;
    if (exception != null) {
      if (exception instanceof Exception) {
        throw (Exception) exception;

      } else {
        throw new IllegalStateException(exception);
      }
    }
    enqueue(resolution);
  }
}
