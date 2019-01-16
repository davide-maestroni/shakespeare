package dm.shakespeare2.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare2.actor.Options;

/**
 * Created by davide-maestroni on 10/01/2018.
 */
public class Failure extends Bounce {

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
