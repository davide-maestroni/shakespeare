package dm.shakespeare2.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare2.actor.Actor.Envelop;
import dm.shakespeare2.actor.ThreadMessage;
import dm.shakespeare2.util.ConstantConditions;

/**
 * Created by davide-maestroni on 10/03/2018.
 */
abstract class AbstractThreadMessage implements ThreadMessage {

  private final Envelop mEnvelop;
  private final Object mMessage;

  AbstractThreadMessage(final Object message, @NotNull final Envelop envelop) {
    mMessage = message;
    mEnvelop = ConstantConditions.notNull("envelop", envelop);
  }

  @NotNull
  public final Envelop getEnvelop() {
    return mEnvelop;
  }

  public final Object getMessage() {
    return mMessage;
  }
}
