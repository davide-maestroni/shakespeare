package dm.shakespeare;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.ThreadMessage;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.Iterables;

/**
 * Created by davide-maestroni on 08/03/2018.
 */
class DefaultActor implements Actor {

  private final String mId;

  private DefaultContext mContext;

  DefaultActor(@NotNull final String id) {
    mId = ConstantConditions.notNull("id", id);
  }

  @NotNull
  public Actor forward(final Object message, @NotNull final Envelop envelop,
      @NotNull final Actor sender) {
    final DefaultContext context = mContext;
    if (context.exceedsQuota(1)) {
      context.quotaExceeded(message, new BouncedEnvelop(sender, envelop));

    } else {
      context.getExecutor().execute(new DefaultEnvelop(sender, envelop) {

        void open() {
          mContext.message(message, this);
        }
      });
    }

    return this;
  }

  @NotNull
  public String getId() {
    return mId;
  }

  public void remove() {
    mContext.abort();
  }

  @NotNull
  public Actor tell(final Object message, @NotNull final Actor sender) {
    final DefaultContext context = mContext;
    if (context.exceedsQuota(1)) {
      context.quotaExceeded(message, new BouncedEnvelop(sender));

    } else {
      context.getExecutor().execute(new DefaultEnvelop(sender) {

        void open() {
          mContext.message(message, this);
        }
      });
    }

    return this;
  }

  @NotNull
  public Actor tellAll(@NotNull final Iterable<?> messages, @NotNull final Actor sender) {
    final DefaultContext context = mContext;
    if (context.exceedsQuota(Iterables.size(messages))) {
      context.quotaExceeded(messages, new BouncedEnvelop(sender));

    } else {
      context.getExecutor().execute(new DefaultEnvelop(sender) {

        void open() {
          final DefaultContext context = mContext;
          if (context.isStopped()) {
            context.bounce(messages, this);
            return;
          }

          for (final Object message : messages) {
            context.message(message, this);
          }
        }
      });
    }

    return this;
  }

  @NotNull
  public <T> Conversation<T> thread(@NotNull final String threadId,
      @NotNull final Collection<? extends Class<? extends ThreadMessage>> messageFilters,
      @NotNull final Actor sender) {
    return new DefaultConversation<T>(threadId, sender, mContext).open(messageFilters);
  }

  void setContext(@NotNull final DefaultContext context) {
    mContext = ConstantConditions.notNull("context", context);
  }
}
