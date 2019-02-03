package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Options;
import dm.shakespeare.actor.Script;
import dm.shakespeare.message.Bounce;
import dm.shakespeare.message.Receipt;
import dm.shakespeare.plot.Story.StoryObserver;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/25/2019.
 */
class StoryObserverScript<T> extends Script {

  private final StoryObserver<? super T> mStoryObserver;

  StoryObserverScript(@NotNull final StoryObserver<? super T> storyObserver) {
    mStoryObserver = ConstantConditions.notNull("storyObserver", storyObserver);
  }

  @NotNull
  public Behavior getBehavior(@NotNull final String id) {
    final Options options = new Options().withReceiptId(id).withThread(id);
    return new AbstractBehavior() {

      private Actor mSender;

      @SuppressWarnings("unchecked")
      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) throws Exception {
        mSender = envelop.getSender();
        final Actor self = context.getSelf();
        if (message instanceof Incident) {
          mStoryObserver.onIncident(((Incident) message).getCause());
          envelop.getSender().tell(Story.NEXT, options, self);

        } else if (message instanceof Bounce) {
          mStoryObserver.onIncident(PlotStateException.getOrNew((Bounce) message));
          context.dismissSelf();

        } else if (message == Story.END) {
          mStoryObserver.onEnd();
          context.dismissSelf();

        } else if (!(message instanceof Receipt)) {
          mStoryObserver.onResolution((T) message);
          envelop.getSender().tell(Story.NEXT, options, self);
        }
      }

      @Override
      public void onStop(@NotNull final Context context) {
        final Actor sender = mSender;
        if (sender != null) {
          sender.tell(Story.BREAK, options, context.getSelf());
        }
      }
    };
  }
}
