package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Script;
import dm.shakespeare.message.Bounce;
import dm.shakespeare.message.Receipt;
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
    return new AbstractBehavior() {

      @SuppressWarnings("unchecked")
      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) throws Exception {
        if (message instanceof Incident) {
          mEventObserver.onIncident(((Incident) message).getCause());
          context.dismissSelf();

        } else if (message instanceof Bounce) {
          mEventObserver.onIncident(PlotStateException.getOrNew((Bounce) message));
          context.dismissSelf();

        } else if (!(message instanceof Receipt)) {
          mEventObserver.onResolution((T) message);
          context.dismissSelf();
        }
      }
    };
  }
}
