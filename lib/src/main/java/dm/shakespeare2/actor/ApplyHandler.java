package dm.shakespeare2.actor;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.function.Mapper;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare2.actor.Behavior.Context;
import dm.shakespeare2.actor.BehaviorBuilder.Handler;

/**
 * Created by davide-maestroni on 01/19/2019.
 */
class ApplyHandler<T> implements Handler<T> {

  private final Mapper<T, ?> mApply;

  ApplyHandler(@NotNull final Mapper<T, ?> mapper) {
    mApply = ConstantConditions.notNull("mapper", mapper);
  }

  public void handle(final T message, @NotNull final Envelop envelop,
      @NotNull final Context context) throws Exception {
    envelop.getSender()
        .tell(mApply.apply(message), envelop.getOptions().threadOnly(), context.getSelf());
  }
}
