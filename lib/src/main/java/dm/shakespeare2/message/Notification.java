package dm.shakespeare2.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare2.actor.Envelop;

/**
 * Created by davide-maestroni on 01/09/2019.
 */
public class Notification {

  private final Envelop mEnvelop;
  private final Object mMessage;

  public Notification(final Object message, @NotNull final Envelop envelop) {
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
