package dm.shakespeare;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Options;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 06/15/2018.
 */
abstract class DefaultEnvelop implements Envelop, Runnable {

  private final Options mOptions;
  private final Actor mSender;
  private final long mSentAt;
  private boolean mPreventReceipt;
  private long mReceivedAt = -1;

  DefaultEnvelop(@NotNull final Actor sender, @Nullable final Options options) {
    mSender = ConstantConditions.notNull("sender", sender);
    mOptions = (options != null) ? options : Options.EMPTY;
    mSentAt = System.currentTimeMillis() - mOptions.getTimeOffset();
  }

  @NotNull
  public Options getOptions() {
    return mOptions;
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

  public boolean isPreventReceipt() {
    return mPreventReceipt;
  }

  public void preventReceipt() {
    mPreventReceipt = true;
  }

  public void run() {
    mReceivedAt = System.currentTimeMillis();
    open();
  }

  @Override
  public String toString() {
    return "DefaultEnvelop{" + "mOptions=" + mOptions + ", mSender=" + mSender + ", mSentAt="
        + mSentAt + ", mPreventReceipt=" + mPreventReceipt + ", mReceivedAt=" + mReceivedAt + '}';
  }

  abstract void open();
}
