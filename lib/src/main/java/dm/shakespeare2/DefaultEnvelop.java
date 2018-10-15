package dm.shakespeare2;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.actor.Actor.Envelop;
import dm.shakespeare2.util.ConstantConditions;

/**
 * Created by davide-maestroni on 06/15/2018.
 */
abstract class DefaultEnvelop implements Envelop, Runnable {

  private final Actor mSender;
  private final long mSentAt;
  private final String mThreadId;

  private long mReceivedAt = -1;

  DefaultEnvelop(@NotNull final Actor sender) {
    mSender = ConstantConditions.notNull("sender", sender);
    mThreadId = null;
    mSentAt = System.currentTimeMillis();
  }

  DefaultEnvelop(@NotNull final Actor sender, @NotNull final Envelop envelop) {
    mSender = ConstantConditions.notNull("sender", sender);
    mThreadId = envelop.getThreadId();
    mSentAt = envelop.getSentAt();
  }

  DefaultEnvelop(@NotNull final Actor sender, @NotNull final Envelop envelop,
      @NotNull final String threadId) {
    mSender = ConstantConditions.notNull("sender", sender);
    mThreadId = ConstantConditions.notNull("threadId", threadId);
    mSentAt = envelop.getSentAt();
  }

  DefaultEnvelop(@NotNull final Actor sender, @NotNull final String threadId) {
    mSender = ConstantConditions.notNull("sender", sender);
    mThreadId = ConstantConditions.notNull("threadId", threadId);
    mSentAt = System.currentTimeMillis();
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

  @NotNull
  public String getThreadId() {
    return mThreadId;
  }

  public void run() {
    mReceivedAt = System.currentTimeMillis();
    open();
  }

  abstract void open();
}
