package dm.shakespeare.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.actor.Options;
import dm.shakespeare.config.BuildConfig;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 10/01/2018.
 */
public class Failure extends Bounce {

  private static final long serialVersionUID = BuildConfig.VERSION_HASH_CODE;

  private final Throwable mCause;

  public Failure(final Object message, @NotNull final Options options,
      @NotNull final Throwable cause) {
    super(message, options);
    mCause = ConstantConditions.notNull("cause", cause);
  }

  @NotNull
  public final Throwable getCause() {
    return mCause;
  }
}
