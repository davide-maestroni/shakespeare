package dm.shakespeare.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.actor.Actor.Envelop;

/**
 * Created by davide-maestroni on 10/01/2018.
 */
public class QuotaExceededMessage extends BouncedMessage {

  public QuotaExceededMessage(final Object message, @NotNull final Envelop envelop) {
    super(message, envelop);
  }
}
