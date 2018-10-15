package dm.shakespeare2.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare2.actor.Actor.Envelop;
import dm.shakespeare2.actor.SignalingMessage;
import dm.shakespeare2.util.ConstantConditions;

/**
 * Created by davide-maestroni on 10/01/2018.
 */
public class BouncedMessage implements SignalingMessage {

  private final Envelop mEnvelop;
  private final Object mMessage;

  public BouncedMessage(final Object message, @NotNull final Envelop envelop) {
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
