package dm.shakespeare2.actor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

import dm.shakespeare.config.BuildConfig;

/**
 * Created by davide-maestroni on 01/08/2019.
 */
public class Options implements Serializable {

  public static final Options EMPTY = new Options();

  private static final long serialVersionUID = BuildConfig.VERSION_HASH_CODE;

  private final boolean mBounceEnabled;
  private final boolean mFailureEnabled;
  private final boolean mReceiptEnabled;
  private final long mSentAt;
  private final boolean mSuccessEnabled;
  private final String mThread;

  public Options() {
    mThread = null;
    mBounceEnabled = false;
    mFailureEnabled = false;
    mReceiptEnabled = false;
    mSuccessEnabled = false;
    mSentAt = 0;
  }

  private Options(@Nullable final String threadId, final boolean bounceEnabled,
      final boolean failureEnabled, final boolean receiptEnabled, final boolean successEnabled,
      final long sentAt) {
    mThread = threadId;
    mBounceEnabled = bounceEnabled;
    mFailureEnabled = failureEnabled;
    mReceiptEnabled = receiptEnabled;
    mSuccessEnabled = successEnabled;
    mSentAt = sentAt;
  }

  @NotNull
  public static Options sentAt(final long timestamp) {
    return new Options(null, false, false, false, false, timestamp);
  }

  @NotNull
  public static Options thread(@Nullable final String id) {
    return new Options(id, false, false, false, false, 0);
  }

  public boolean getBounce() {
    return mBounceEnabled;
  }

  public boolean getFailure() {
    return mFailureEnabled;
  }

  public boolean getReceipt() {
    return mReceiptEnabled;
  }

  public long getSentAt() {
    return mSentAt;
  }

  public boolean getSuccess() {
    return mSuccessEnabled;
  }

  public String getThread() {
    return mThread;
  }

  @NotNull
  public Options withBounce(final boolean enabled) {
    return new Options(mThread, enabled, mFailureEnabled, mReceiptEnabled, mSuccessEnabled,
        mSentAt);
  }

  @NotNull
  public Options withFailure(final boolean enabled) {
    return new Options(mThread, mBounceEnabled, enabled, mReceiptEnabled, mSuccessEnabled, mSentAt);
  }

  @NotNull
  public Options withReceipt(final boolean enabled) {
    return new Options(mThread, mBounceEnabled, mFailureEnabled, enabled, mSuccessEnabled, mSentAt);
  }

  @NotNull
  public Options withSentAt(final long timestamp) {
    return new Options(mThread, mBounceEnabled, mFailureEnabled, mReceiptEnabled, mSuccessEnabled,
        timestamp);
  }

  @NotNull
  public Options withSuccess(final boolean enabled) {
    return new Options(mThread, mBounceEnabled, mFailureEnabled, mReceiptEnabled, enabled, mSentAt);
  }

  @NotNull
  public Options withThread(@Nullable final String id) {
    return new Options(id, mBounceEnabled, mFailureEnabled, mReceiptEnabled, mSuccessEnabled,
        mSentAt);
  }
}
