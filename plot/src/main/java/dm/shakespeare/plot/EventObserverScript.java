package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.actor.BehaviorBuilder.Handler;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Script;
import dm.shakespeare.message.Bounce;
import dm.shakespeare.plot.Event.EventObserver;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/25/2019.
 */
class EventObserverScript<T> extends Script {

  private final EventObserver<? super T> mEventObserver;

  EventObserverScript(@NotNull final EventObserver<? super T> eventObserver) {
    mEventObserver = ConstantConditions.notNull("eventObserver", eventObserver);
  }

  @NotNull
  public Behavior getBehavior(@NotNull final String id) {
    return newBehavior().onMessage(Bounce.class, new Handler<Bounce>() {

      public void handle(final Bounce message, @NotNull final Envelop envelop,
          @NotNull final Context context) throws Exception {
        mEventObserver.onIncident(PlotStateException.getOrNew(message));
        context.dismissSelf();
      }
    }).onMessage(Incident.class, new Handler<Incident>() {

      public void handle(final Incident message, @NotNull final Envelop envelop,
          @NotNull final Context context) throws Exception {
        mEventObserver.onIncident(message.getCause());
        context.dismissSelf();
      }
    }).onNoMatch(new Handler<Object>() {

      @SuppressWarnings("unchecked")
      public void handle(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) throws Exception {
        mEventObserver.onResolution((T) message);
        context.dismissSelf();
      }
    }).build();
  }
}
