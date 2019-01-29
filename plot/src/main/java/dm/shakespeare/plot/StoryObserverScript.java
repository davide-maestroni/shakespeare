package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Options;
import dm.shakespeare.actor.Script;
import dm.shakespeare.message.Bounce;
import dm.shakespeare.plot.Event.EventObserver;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/25/2019.
 */
class StoryObserverScript<T> extends Script {

  private final EventObserver<? super T> mEventObserver;

  StoryObserverScript(@NotNull final EventObserver<? super T> eventObserver) {
    mEventObserver = ConstantConditions.notNull("eventObserver", eventObserver);
  }

  @NotNull
  public Behavior getBehavior(@NotNull final String id) {
    final Options options = new Options().withReceiptId(id);
    return new AbstractBehavior() {

      private long mCount = 0;

      @SuppressWarnings("unchecked")
      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) throws Exception {
        if (Long.toString(mCount).equals(envelop.getOptions().getThread())) {
          if (message instanceof Incident) {
            mEventObserver.onIncident(((Incident) message).getCause());
            envelop.getSender()
                .tell(Story.NEXT, options.withThread(Long.toString(++mCount)), context.getSelf());

          } else if (message instanceof Bounce) {
            mEventObserver.onIncident(PlotStateException.getOrNew((Bounce) message));
            envelop.getSender()
                .tell(Story.NEXT, options.withThread(Long.toString(++mCount)), context.getSelf());

          } else if (message == Story.END) {
            context.dismissSelf();

          } else {
            mEventObserver.onResolution((T) message);
            envelop.getSender()
                .tell(Story.NEXT, options.withThread(Long.toString(++mCount)), context.getSelf());
          }
        }
      }
    };
  }
}
