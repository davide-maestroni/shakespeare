package dm.shakespeare2.actor;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.function.Observer;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare2.actor.Behavior.Context;
import dm.shakespeare2.actor.BehaviorBuilder.Handler;

/**
 * Created by davide-maestroni on 01/19/2019.
 */
class AcceptHandler<T> implements Handler<T> {

  private final Observer<T> mObserver;

  AcceptHandler(@NotNull final Observer<T> observer) {
    mObserver = ConstantConditions.notNull("observer", observer);
  }

  public void handle(final T message, @NotNull final Envelop envelop,
      @NotNull final Context context) throws Exception {
    mObserver.accept(message);
  }
}
