package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.actor.BehaviorBuilder.Handler;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Script;
import dm.shakespeare.message.Bounce;
import dm.shakespeare.plot.Line.LineObserver;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/25/2019.
 */
class ReadLineScript<T> extends Script {

  private final LineObserver<? super T> mLineObserver;

  ReadLineScript(@NotNull final LineObserver<? super T> lineObserver) {
    mLineObserver = ConstantConditions.notNull("lineObserver", lineObserver);
  }

  @NotNull
  public Behavior getBehavior(@NotNull final String id) {
    return newBehavior().onMessage(Bounce.class, new Handler<Bounce>() {

      public void handle(final Bounce message, @NotNull final Envelop envelop,
          @NotNull final Context context) throws Exception {
        mLineObserver.onError(PlotStateException.getError(message));
        context.dismissSelf();
      }
    }).onMessage(LineFailure.class, new Handler<LineFailure>() {

      public void handle(final LineFailure message, @NotNull final Envelop envelop,
          @NotNull final Context context) throws Exception {
        mLineObserver.onError(message.getCause());
        context.dismissSelf();
      }
    }).onNoMatch(new Handler<Object>() {

      @SuppressWarnings("unchecked")
      public void handle(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) throws Exception {
        mLineObserver.onMessage((T) message);
        context.dismissSelf();
      }
    }).build();
  }
}
