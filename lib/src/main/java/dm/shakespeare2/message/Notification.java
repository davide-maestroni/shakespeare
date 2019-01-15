package dm.shakespeare2.message;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

import dm.shakespeare.config.BuildConfig;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare2.actor.Options;

/**
 * Created by davide-maestroni on 01/09/2019.
 */
public class Notification implements Serializable {

  private static final long serialVersionUID = BuildConfig.VERSION_HASH_CODE;

  private final Object mMessage;
  private final Options mOptions;

  public Notification(final Object message, @NotNull final Options options) {
    mMessage = message;
    mOptions = ConstantConditions.notNull("options", options);
  }

  public final Object getMessage() {
    return mMessage;
  }

  @NotNull
  public final Options getOptions() {
    return mOptions;
  }
}
