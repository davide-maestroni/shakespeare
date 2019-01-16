package dm.shakespeare2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.actor.Envelop;
import dm.shakespeare2.actor.Options;

/**
 * Created by davide-maestroni on 06/15/2018.
 */
abstract class DefaultEnvelop implements Envelop, Runnable {

  private final Options mOptions;
  private final Actor mSender;
  private final long mSentAt;
  private long mReceivedAt = -1;

  DefaultEnvelop(@NotNull final Actor sender) {
    mSender = ConstantConditions.notNull("sender", sender);
    mOptions = Options.EMPTY;
    mSentAt = System.currentTimeMillis();
  }

  DefaultEnvelop(@NotNull final Actor sender, @NotNull final Envelop envelop) {
    mSender = ConstantConditions.notNull("sender", sender);
    mOptions = envelop.getOptions();
    mSentAt = envelop.getSentAt();
  }

  DefaultEnvelop(@NotNull final Actor sender, @NotNull final Envelop envelop,
      @Nullable final Options options) {
    mSender = ConstantConditions.notNull("sender", sender);
    mSentAt = envelop.getSentAt();
    mOptions = (options != null) ? options : Options.EMPTY;
  }

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

  public void run() {
    mReceivedAt = System.currentTimeMillis();
    open();
  }

  abstract void open();
}
