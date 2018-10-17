package dm.shakespeare.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.actor.Actor.Envelop;
import dm.shakespeare.actor.SignalingMessage;
import dm.shakespeare.util.ConstantConditions;

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
