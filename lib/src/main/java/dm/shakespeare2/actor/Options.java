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
  private final String mThread;
  private final long mTimeOffset;

  public Options() {
    mThread = null;
    mBounceEnabled = false;
    mFailureEnabled = false;
    mTimeOffset = 0;
  }

  private Options(@Nullable final String threadId, final boolean bounceEnabled,
      final boolean failureEnabled, final long timeOffset) {
    mThread = threadId;
    mBounceEnabled = bounceEnabled;
    mFailureEnabled = failureEnabled;
    mTimeOffset = timeOffset;
  }

  @NotNull
  public static Options thread(@Nullable final String id) {
    return new Options(id, false, false, 0);
  }

  @NotNull
  public static Options timeOffset(final long offsetMillis) {
    return new Options(null, false, false, offsetMillis);
  }

  public boolean getBounce() {
    return mBounceEnabled;
  }

  public boolean getFailure() {
    return mFailureEnabled;
  }

  public String getThread() {
    return mThread;
  }

  public long getTimeOffset() {
    return mTimeOffset;
  }

  @NotNull
  public Options withBounce(final boolean enabled) {
    return new Options(mThread, enabled, mFailureEnabled, mTimeOffset);
  }

  @NotNull
  public Options withFailure(final boolean enabled) {
    return new Options(mThread, mBounceEnabled, enabled, mTimeOffset);
  }

  @NotNull
  public Options withThread(@Nullable final String id) {
    return new Options(id, mBounceEnabled, mFailureEnabled, mTimeOffset);
  }

  @NotNull
  public Options withTimeOffset(final long offsetMillis) {
    return new Options(mThread, mBounceEnabled, mFailureEnabled, offsetMillis);
  }
}
