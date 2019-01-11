package dm.shakespeare2.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare2.actor.Envelop;

/**
 * Created by davide-maestroni on 10/01/2018.
 */
public class Failure extends Receipt {

  private final Throwable mCause;

  public Failure(final Object message, @NotNull final Envelop envelop,
      @NotNull final Throwable cause) {
    super(message, envelop);
    mCause = ConstantConditions.notNull("cause", cause);
  }

  @NotNull
  public final Throwable getCause() {
    return mCause;
  }
}
