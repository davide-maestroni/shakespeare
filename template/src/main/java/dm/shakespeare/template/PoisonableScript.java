package dm.shakespeare.template;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Script;
import dm.shakespeare.template.config.BuildConfig;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/16/2019.
 */
public class PoisonableScript extends SerializableScriptWrapper {

  private static final PoisonPill POISON_PILL = new PoisonPill();

  private static final long serialVersionUID = BuildConfig.VERSION_HASH_CODE;

  public PoisonableScript(@NotNull final Script script) {
    super(script);
  }

  @NotNull
  public static PoisonPill poisonPill() {
    return POISON_PILL;
  }

  @NotNull
  @Override
  public Behavior getBehavior(@NotNull final String id) throws Exception {
    return new PoisonableBehavior(super.getBehavior(id));
  }

  public static class PoisonPill implements Serializable {

    private static final long serialVersionUID = BuildConfig.VERSION_HASH_CODE;

    private PoisonPill() {
    }
  }

  private static class PoisonableBehavior implements Behavior {

    private final ContextWrapper mContext;

    private Behavior mBehavior;

    private PoisonableBehavior(@NotNull final Behavior behavior) {
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
  }
}
