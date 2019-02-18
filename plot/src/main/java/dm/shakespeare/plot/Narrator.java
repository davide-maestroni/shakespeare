package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import dm.shakespeare.BackStage;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 02/17/2019.
 */
public abstract class Narrator<T> {

  static final Object AVAILABLE = new Object();
  static final Object NULL = new Object();

  private final BlockingQueue<Object> mQueue;

  private volatile Actor mActor;
  private volatile Throwable mException;

  public Narrator() {
    this(new LinkedBlockingQueue<Object>());
  }

  public Narrator(@NotNull final BlockingQueue<Object> queue) {
    mQueue = ConstantConditions.notNull("queue", queue);
  }

  public void report(@NotNull final Throwable incident) throws Exception {
    resolve(new Conflict(incident));
  }

  public void tell(final T effect) throws Exception {
    resolve((effect != null) ? effect : NULL);
  }

  protected abstract boolean narrate() throws Exception;

  final void cancel(@NotNull final Throwable cause) {
    mException = cause;
    mQueue.clear();
  }

  final void setActor(@NotNull final Actor actor) {
    mActor = actor;
  }

  @NotNull
  final List<Object> stop() {
    final ArrayList<Object> effects = new ArrayList<Object>();
    mQueue.drainTo(effects);
    cancel(new NarrationStoppedException());
    return effects;
  }

  @Nullable
  final Object takeEffect() {
    return mQueue.poll();
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
    mQueue.add(resolution);
    final Actor actor = mActor;
    if (actor != null) {
      actor.tell(AVAILABLE, null, BackStage.standIn());
    }
  }
}
