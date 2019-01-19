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

  private final String mReceiptId;
  private final String mThread;
  private final long mTimeOffset;

  public Options() {
    mThread = null;
    mReceiptId = null;
    mTimeOffset = 0;
  }

  private Options(@Nullable final String threadId, final String receiptId, final long timeOffset) {
    mThread = threadId;
    mReceiptId = receiptId;
    mTimeOffset = timeOffset;
  }


  @NotNull
  public Options asSentAt(final long timeMillis) {
    return withTimeOffset(System.currentTimeMillis() - timeMillis + mTimeOffset);
  }

  @Nullable
  public String getReceiptId() {
    return mReceiptId;
  }

  @Nullable
  public String getThread() {
    return mThread;
  }

  public long getTimeOffset() {
    return mTimeOffset;
  }

  @NotNull
  public Options threadOnly() {
    return new Options(mThread, null, 0);
  }

  @NotNull
  public Options withReceiptId(@Nullable final String receiptId) {
    return new Options(mThread, receiptId, mTimeOffset);
  }

  @NotNull
  public Options withThread(@Nullable final String threadId) {
    return new Options(threadId, mReceiptId, mTimeOffset);
  }

  @NotNull
  public Options withTimeOffset(final long offsetMillis) {
    return new Options(mThread, mReceiptId, offsetMillis);
  }
}
