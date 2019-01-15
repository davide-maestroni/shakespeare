package dm.shakespeare2.template;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare2.actor.Behavior;
import dm.shakespeare2.actor.Envelop;

/**
 * Created by davide-maestroni on 01/15/2019.
 */
public class PoisonableBehavior implements Behavior {

  public static final PoisonPill POISON_PILL = new PoisonPill();

  private final ContextWrapper mContext;

  private Behavior mBehavior;

  PoisonableBehavior(@NotNull final Behavior behavior) {
    mBehavior = ConstantConditions.notNull("behavior", behavior);
    mContext = new ContextWrapper() {

      @Override
      public void setBehavior(@NotNull final Behavior behavior) {
        mBehavior = ConstantConditions.notNull("behavior", behavior);
      }
    };
  }

  public void onMessage(final Object message, @NotNull final Envelop envelop,
      @NotNull final Context context) throws Exception {
    if (message instanceof PoisonPill) {
      context.dismissSelf();
      return;
    }

    mBehavior.onMessage(message, envelop, mContext.withContext(context));
  }

  public void onStart(@NotNull final Context context) throws Exception {
    mBehavior.onStart(mContext.withContext(context));
  }

  public void onStop(@NotNull final Context context) throws Exception {
    mBehavior.onStop(mContext.withContext(context));
  }

  public static class PoisonPill {

    private PoisonPill() {
    }
  }
}
