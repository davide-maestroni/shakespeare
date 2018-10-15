package dm.shakespeare2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.actor.Actor.Envelop;
import dm.shakespeare2.actor.SignalingMessage;
import dm.shakespeare2.actor.ThreadMessage;
import dm.shakespeare2.message.CompletedMessage;
import dm.shakespeare2.message.DelayedMessage;
import dm.shakespeare2.message.DiscardedMessage;
import dm.shakespeare2.message.FailedMessage;
import dm.shakespeare2.message.ReceivedMessage;
import dm.shakespeare2.message.ThreadAbortedMessage;
import dm.shakespeare2.message.ThreadClosedMessage;
import dm.shakespeare2.message.ThreadOpenedMessage;
import dm.shakespeare2.util.ConstantConditions;

/**
 * Created by davide-maestroni on 10/03/2018.
 */
class ConversationNotifier {

  private static final Notifier EMPTY_NOTIFIER = new Notifier() {

    public void abort() {
    }

    public void complete(final Object message, @NotNull final Envelop envelop) {
    }

    public void delay(final Object message, @NotNull final Envelop envelop) {
    }

    public void discard(final Object message, @NotNull final Envelop envelop,
        @NotNull final Throwable cause) {
    }

    public void fail(final Object message, @NotNull final Envelop envelop,
        @NotNull final Throwable cause) {
    }

    public void receive(final Object message, @NotNull final Envelop envelop) {
    }
  };
  private static final Set<ThreadEnvelop> EMPTY_RECIPIENTS = Collections.emptySet();

  private final DefaultNotifier mDefaultNotifier = new DefaultNotifier();
  private final Actor mSender;

  private Notifier mNotifier = EMPTY_NOTIFIER;
  private Set<ThreadEnvelop> mRecipientsAbort = EMPTY_RECIPIENTS;
  private Set<ThreadEnvelop> mRecipientsComplete = EMPTY_RECIPIENTS;
  private Set<ThreadEnvelop> mRecipientsDelay = EMPTY_RECIPIENTS;
  private Set<ThreadEnvelop> mRecipientsDiscard = EMPTY_RECIPIENTS;
  private Set<ThreadEnvelop> mRecipientsFail = EMPTY_RECIPIENTS;
  private Set<ThreadEnvelop> mRecipientsReceive = EMPTY_RECIPIENTS;

  ConversationNotifier(@NotNull final Actor sender) {
    mSender = ConstantConditions.notNull("sender", sender);
  }

  void abort() {
    mNotifier.abort();
  }

  @SuppressWarnings("unused")
  void close(@NotNull final ThreadClosedMessage message, @NotNull final Envelop envelop) {
    final Actor sender = envelop.getSender();
    Set<ThreadEnvelop> recipients = mRecipientsReceive;
    if (recipients != EMPTY_RECIPIENTS) {
      recipients.remove(new ThreadEnvelop(envelop.getThreadId(), sender));
      if (recipients.isEmpty()) {
        mRecipientsReceive = EMPTY_RECIPIENTS;
      }
    }

    recipients = mRecipientsComplete;
    if (recipients != EMPTY_RECIPIENTS) {
      recipients.remove(new ThreadEnvelop(envelop.getThreadId(), sender));
      if (recipients.isEmpty()) {
        mRecipientsComplete = EMPTY_RECIPIENTS;
      }
    }

    recipients = mRecipientsFail;
    if (recipients != EMPTY_RECIPIENTS) {
      recipients.remove(new ThreadEnvelop(envelop.getThreadId(), sender));
      if (recipients.isEmpty()) {
        mRecipientsFail = EMPTY_RECIPIENTS;
      }
    }

    recipients = mRecipientsDiscard;
    if (recipients != EMPTY_RECIPIENTS) {
      recipients.remove(new ThreadEnvelop(envelop.getThreadId(), sender));
      if (recipients.isEmpty()) {
        mRecipientsDiscard = EMPTY_RECIPIENTS;
      }
    }

    recipients = mRecipientsDelay;
    if (recipients != EMPTY_RECIPIENTS) {
      recipients.remove(new ThreadEnvelop(envelop.getThreadId(), sender));
      if (recipients.isEmpty()) {
        mRecipientsDelay = EMPTY_RECIPIENTS;
      }
    }

    recipients = mRecipientsAbort;
    if (recipients != EMPTY_RECIPIENTS) {
      recipients.remove(new ThreadEnvelop(envelop.getThreadId(), sender));
      if (recipients.isEmpty()) {
        mRecipientsAbort = EMPTY_RECIPIENTS;
      }
    }

    if ((mRecipientsReceive == EMPTY_RECIPIENTS) && (mRecipientsComplete == EMPTY_RECIPIENTS) && (
        mRecipientsFail == EMPTY_RECIPIENTS) && (mRecipientsDiscard == EMPTY_RECIPIENTS) && (
        mRecipientsDelay == EMPTY_RECIPIENTS) && (mRecipientsAbort == EMPTY_RECIPIENTS)) {
      mNotifier = EMPTY_NOTIFIER;
    }
  }

  void complete(final Object message, @NotNull final Envelop envelop) {
    mNotifier.complete(message, envelop);
  }

  void delay(final Object message, @NotNull final Envelop envelop) {
    mNotifier.delay(message, envelop);
  }

  void discard(final Object message, @NotNull final Envelop envelop,
      @NotNull final Throwable cause) {
    mNotifier.discard(message, envelop, cause);
  }

  void fail(final Object message, @NotNull final Envelop envelop, @NotNull final Throwable cause) {
    mNotifier.fail(message, envelop, cause);
  }

  void open(@NotNull final ThreadOpenedMessage message, @NotNull final Envelop envelop) {
    final ThreadEnvelop threadEnvelop =
        new ThreadEnvelop(envelop.getThreadId(), envelop.getSender(), envelop.getSentAt(),
            envelop.getReceivedAt());
    for (final Class<? extends ThreadMessage> filter : message.getMessageFilters()) {
      if (filter.isAssignableFrom(ReceivedMessage.class)) {
        if (mRecipientsReceive == EMPTY_RECIPIENTS) {
          mRecipientsReceive = new HashSet<ThreadEnvelop>();
        }

        mRecipientsReceive.add(threadEnvelop);
      }

      if (filter.isAssignableFrom(CompletedMessage.class)) {
        if (mRecipientsComplete == EMPTY_RECIPIENTS) {
          mRecipientsComplete = new HashSet<ThreadEnvelop>();
        }

        mRecipientsComplete.add(threadEnvelop);
      }

      if (filter.isAssignableFrom(FailedMessage.class)) {
        if (mRecipientsFail == EMPTY_RECIPIENTS) {
          mRecipientsFail = new HashSet<ThreadEnvelop>();
        }

        mRecipientsFail.add(threadEnvelop);
      }

      if (filter.isAssignableFrom(DelayedMessage.class)) {
        if (mRecipientsDelay == EMPTY_RECIPIENTS) {
          mRecipientsDelay = new HashSet<ThreadEnvelop>();
        }

        mRecipientsDelay.add(threadEnvelop);
      }

      if (mRecipientsAbort == EMPTY_RECIPIENTS) {
        mRecipientsAbort = new HashSet<ThreadEnvelop>();
      }

      mRecipientsAbort.add(threadEnvelop);
    }

    mNotifier = mDefaultNotifier;
  }

  void receive(final Object message, @NotNull final Envelop envelop) {
    mNotifier.receive(message, envelop);
  }

  private interface Notifier {

    void abort();

    void complete(Object message, @NotNull Envelop envelop);

    void delay(Object message, @NotNull Envelop envelop);

    void discard(Object message, @NotNull Envelop envelop, @NotNull Throwable cause);

    void fail(Object message, @NotNull Envelop envelop, @NotNull Throwable cause);

    void receive(Object message, @NotNull Envelop envelop);
  }

  private static class ThreadEnvelop implements Envelop {

    private final long mReceivedAt;
    private final Actor mSender;
    private final long mSentAt;
    private final String mThreadId;

    private ThreadEnvelop(final String threadId, @NotNull final Actor sender) {
      this(threadId, sender, System.currentTimeMillis(), 0);
    }

    private ThreadEnvelop(final String threadId, @NotNull final Actor sender, final long sentAt,
        final long receivedAt) {
      mThreadId = ConstantConditions.notNull("threadId", threadId);
      mSender = ConstantConditions.notNull("sender", sender);
      mSentAt = sentAt;
      mReceivedAt = receivedAt;
    }

    public long getReceivedAt() {
      return mReceivedAt;
    }

    @NotNull
    public Actor getSender() {
      return mSender;
    }

    public long getSentAt() {
      return mSentAt;
    }

    @Nullable
    public String getThreadId() {
      return mThreadId;
    }

    @Override
    public int hashCode() {
      int result = mSender.hashCode();
      result = 31 * result + mThreadId.hashCode();
      return result;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      final ThreadEnvelop that = (ThreadEnvelop) o;
      return mSender.equals(that.mSender) && mThreadId.equals(that.mThreadId);
    }
  }

  private class DefaultNotifier implements Notifier {

    public void abort() {
      @SuppressWarnings("UnnecessaryLocalVariable") final Actor sender = mSender;
      for (final ThreadEnvelop threadEnvelop : mRecipientsAbort) {
        final Actor actor = threadEnvelop.getSender();
        actor.forward(ThreadAbortedMessage.defaultInstance(),
            new ThreadEnvelop(threadEnvelop.getThreadId(), actor), sender);
      }
    }

    public void complete(final Object message, @NotNull final Envelop envelop) {
      final Actor actor = envelop.getSender();
      final ThreadEnvelop threadEnvelop = new ThreadEnvelop(envelop.getThreadId(), actor);
      if (!(message instanceof SignalingMessage) && mRecipientsComplete.contains(threadEnvelop)) {
        actor.forward(new CompletedMessage(message, envelop), threadEnvelop, mSender);
      }
    }

    public void delay(final Object message, @NotNull final Envelop envelop) {
      final Actor actor = envelop.getSender();
      final ThreadEnvelop threadEnvelop = new ThreadEnvelop(envelop.getThreadId(), actor);
      if (!(message instanceof SignalingMessage) && mRecipientsDelay.contains(threadEnvelop)) {
        actor.forward(new DelayedMessage(message, envelop), threadEnvelop, mSender);
      }
    }

    public void discard(final Object message, @NotNull final Envelop envelop,
        @NotNull final Throwable cause) {
      final Actor actor = envelop.getSender();
      final ThreadEnvelop threadEnvelop = new ThreadEnvelop(envelop.getThreadId(), actor);
      if (!(message instanceof SignalingMessage) && mRecipientsDiscard.contains(threadEnvelop)) {
        actor.forward(new DiscardedMessage(message, envelop, cause), threadEnvelop, mSender);
      }
    }

    public void fail(final Object message, @NotNull final Envelop envelop,
        @NotNull final Throwable cause) {
      final Actor actor = envelop.getSender();
      final ThreadEnvelop threadEnvelop = new ThreadEnvelop(envelop.getThreadId(), actor);
      if (!(message instanceof SignalingMessage) && mRecipientsFail.contains(threadEnvelop)) {
        actor.forward(new FailedMessage(message, envelop, cause), threadEnvelop, mSender);
      }
    }

    public void receive(final Object message, @NotNull final Envelop envelop) {
      final Actor actor = envelop.getSender();
      final ThreadEnvelop threadEnvelop = new ThreadEnvelop(envelop.getThreadId(), actor);
      if (!(message instanceof SignalingMessage) && mRecipientsReceive.contains(threadEnvelop)) {
        actor.forward(new ReceivedMessage(message, envelop), threadEnvelop, mSender);
      }
    }
  }
}
