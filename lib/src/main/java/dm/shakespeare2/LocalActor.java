package dm.shakespeare2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.Iterables;
import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.actor.Options;

/**
 * Created by davide-maestroni on 08/03/2018.
 */
class LocalActor implements Actor {

  private final String mId;

  private final LocalContext mContext;

  LocalActor(@NotNull final String id, @NotNull final LocalContext context) {
    mId = ConstantConditions.notNull("id", id);
    mContext = ConstantConditions.notNull("context", context);
  }

  public void dismiss(final boolean mayInterruptIfRunning) {
    mContext.dismiss(mayInterruptIfRunning);
  }

  @NotNull
  public String getId() {
    return mId;
  }

  @NotNull
  public Actor tell(final Object message, @Nullable final Options options,
      @NotNull final Actor sender) {
    final LocalContext context = mContext;
    if (context.exceedsQuota(1)) {
      context.quotaExceeded(message, new BounceEnvelop(sender, options));

    } else {
      context.getExecutor().execute(new DefaultEnvelop(sender, options) {

        void open() {
          mContext.message(message, this);
        }
      });
    }

    return this;
  }

  @NotNull
  public Actor tellAll(@NotNull final Iterable<?> messages, @Nullable final Options options,
      @NotNull final Actor sender) {
    final LocalContext context = mContext;
    if (context.exceedsQuota(Iterables.size(messages))) {
      context.quotaExceeded(messages, new BounceEnvelop(sender, options));

    } else {
      context.getExecutor().execute(new DefaultEnvelop(sender, options) {

        void open() {
          mContext.messages(messages, this);
        }
      });
    }

    return this;
  }

  private static class BounceEnvelop extends DefaultEnvelop {

    BounceEnvelop(@NotNull final Actor sender, @Nullable final Options options) {
      super(sender, options);
    }

    void open() {
    }
  }
}
