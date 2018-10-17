package dm.shakespeare.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.actor.Actor.Envelop;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 10/01/2018.
 */
public class DiscardedMessage extends AbstractThreadMessage
    implements ThreadFailureMessage, ProcessedMessage {

  private final Throwable mCause;

  public DiscardedMessage(final Object message, @NotNull final Envelop envelop,
      @NotNull final Throwable cause) {
    super(message, envelop);
    mCause = ConstantConditions.notNull("cause", cause);
  }

  @NotNull
  public final Throwable getCause() {
    return mCause;
  }
}
