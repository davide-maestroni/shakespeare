package dm.shakespeare;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Actor.ActorSet;
import dm.shakespeare.actor.Actor.Conversation;
import dm.shakespeare.actor.Actor.Envelop;
import dm.shakespeare.actor.ThreadMessage;
import dm.shakespeare.message.ThreadClosedMessage;
import dm.shakespeare.message.ThreadOpenedMessage;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.Iterables;

/**
 * Created by davide-maestroni on 06/15/2018.
 */
class DefaultConversation<T> implements Conversation<T> {

  private final DefaultContext mContext;
  private final SingleActorSet mRecipients;
  private final Actor mSender;
  private final String mThreadId;

  private volatile boolean mAborted;
  private boolean mClosed;

  DefaultConversation(@NotNull final String threadId, @NotNull final Actor sender,
      @NotNull final DefaultContext context) {
    mThreadId = ConstantConditions.notNull("threadId", threadId);
    mSender = ConstantConditions.notNull("sender", sender);
    mContext = context;
    mRecipients = new SingleActorSet(context.getSelf());
  }

  public void abort() {
    if (mAborted) {
      return;
    }

    mAborted = true;
    close();
  }

  public void close() {
    if (mClosed) {
      return;
    }

    mClosed = true;
    mContext.getExecutor().execute(new DefaultEnvelop(mSender, mThreadId) {

      void open() {
        mContext.message(ThreadClosedMessage.defaultInstance(), this);
      }
    });
  }

  @NotNull
  public Conversation forward(final Object message, @NotNull final Envelop envelop) {
    if (mClosed) {
      throw new ThreadClosedException();
    }

    final DefaultContext context = mContext;
    if (context.exceedsQuota(1)) {
      context.quotaExceeded(message, new BouncedEnvelop(mSender, envelop, mThreadId));

    } else {
      context.getExecutor().execute(new DefaultEnvelop(mSender, envelop, mThreadId) {

        void open() {
          final DefaultContext context = mContext;
          if (mAborted) {
            context.bounce(message, this);
            return;
          }

          context.message(message, this);
        }
      });
    }

    return this;
  }

  @NotNull
  public ActorSet getRecipients() {
    return mRecipients;
  }

  @NotNull
  public String getThreadId() {
    return mThreadId;
  }

  @NotNull
  public Conversation<T> tell(final T message) {
    if (mClosed) {
      throw new ThreadClosedException();
    }

    final DefaultContext context = mContext;
    if (context.exceedsQuota(1)) {
      context.quotaExceeded(message, new BouncedEnvelop(mSender, mThreadId));

    } else {
      context.getExecutor().execute(new DefaultEnvelop(mSender, mThreadId) {

        void open() {
          final DefaultContext context = mContext;
          if (mAborted) {
            context.bounce(message, this);
            return;
          }

          context.message(message, this);
        }
      });
    }

    return this;
  }

  @NotNull
  public Conversation<T> tellAll(@NotNull final Iterable<? extends T> messages) {
    if (mClosed) {
      throw new ThreadClosedException();
    }

    final DefaultContext context = mContext;
    if (context.exceedsQuota(Iterables.size(messages))) {
      context.quotaExceeded(messages, new BouncedEnvelop(mSender, mThreadId));

    } else {
      context.getExecutor().execute(new DefaultEnvelop(mSender, mThreadId) {

        void open() {
          final DefaultContext context = mContext;
          if (mAborted || context.isStopped()) {
            context.bounce(messages, this);
            return;
          }

          for (final T message : messages) {
            context.message(message, this);
          }
        }
      });
    }

    return this;
  }

  @NotNull
  Conversation<T> open(
      @NotNull final Collection<? extends Class<? extends ThreadMessage>> messageClasses) {
    final ThreadOpenedMessage message = new ThreadOpenedMessage(messageClasses);
    final DefaultContext context = mContext;
    if (context.exceedsQuota(1)) {
      context.quotaExceeded(message, new BouncedEnvelop(mSender, mThreadId));

    } else {
      context.getExecutor().execute(new DefaultEnvelop(mSender, mThreadId) {

        void open() {
          final DefaultContext context = mContext;
          if (mAborted) {
            context.bounce(message, this);
            return;
          }

          context.message(message, this);
        }
      });
    }

    return this;
  }
}
