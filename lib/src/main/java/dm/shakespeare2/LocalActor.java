package dm.shakespeare2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.RejectedExecutionException;

import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.Iterables;
import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.actor.Options;

/**
 * Created by davide-maestroni on 08/03/2018.
 */
class LocalActor implements Actor {

  private final LocalContext mContext;
  private final String mId;

  LocalActor(@NotNull final String id, @NotNull final LocalContext context) {
    mId = ConstantConditions.notNull("id", id);
    mContext = ConstantConditions.notNull("context", context);
  }

  @NotNull
  public Actor addObserver(@NotNull final Actor observer) {
    mContext.getActorExecutor().executeNext(new Runnable() {

      public void run() {
        mContext.addObserver(observer);
      }
    });
    return this;
  }

  public void dismiss(final boolean mayInterruptIfRunning) {
    mContext.dismiss(mayInterruptIfRunning);
  }

  @NotNull
  public String getId() {
    return mId;
  }

  @NotNull
  public Actor removeObserver(@NotNull final Actor observer) {
    mContext.getActorExecutor().executeNext(new Runnable() {

      public void run() {
        mContext.removeObserver(observer);
      }
    });
    return this;
  }

  @NotNull
  public Actor tell(final Object message, @Nullable final Options options,
      @NotNull final Actor sender) {
    final LocalContext context = mContext;
    if (context.exceedsQuota(1)) {
      context.quotaExceeded(message, new BounceEnvelop(sender, options));

    } else {
      try {
        context.getActorExecutor().execute(new DefaultEnvelop(sender, options) {

          void open() {
            mContext.message(message, this);
          }
        });

      } catch (final RejectedExecutionException e) {
        context.quotaExceeded(message, new BounceEnvelop(sender, options));
      }
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
      try {
        context.getActorExecutor().execute(new DefaultEnvelop(sender, options) {

          void open() {
            mContext.messages(messages, this);
          }
        });

      } catch (final RejectedExecutionException e) {
        context.quotaExceeded(messages, new BounceEnvelop(sender, options));
      }
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
