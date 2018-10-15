package dm.shakespeare2.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare2.actor.Actor.Envelop;

/**
 * Created by davide-maestroni on 10/01/2018.
 */
public class CompletedMessage extends AbstractThreadMessage
    implements ThreadSuccessMessage, ProcessedMessage {

  public CompletedMessage(final Object message, @NotNull final Envelop envelop) {
    super(message, envelop);
  }
}
