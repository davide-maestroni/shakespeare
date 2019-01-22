package dm.shakespeare.message;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

import dm.shakespeare.actor.Options;
import dm.shakespeare.config.BuildConfig;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/09/2019.
 */
public class Receipt implements Serializable {

  private static final long serialVersionUID = BuildConfig.VERSION_HASH_CODE;

  private final Object mMessage;
  private final Options mOptions;

  public Receipt(final Object message, @NotNull final Options options) {
    mMessage = message;
    mOptions = ConstantConditions.notNull("options", options);
  }

  public static boolean isReceipt(@Nullable final Object message, @NotNull final String receiptId) {
    return (message instanceof Receipt) && receiptId.equals(
        ((Receipt) message).getOptions().getReceiptId());
  }

  public final Object getMessage() {
    return mMessage;
  }

  @NotNull
  public final Options getOptions() {
    return mOptions;
  }
}
