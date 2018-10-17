package dm.shakespeare.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.actor.Actor.Envelop;

/**
 * Created by davide-maestroni on 10/01/2018.
 */
public class ReceivedMessage extends AbstractThreadMessage implements DeliveredMessage {

  public ReceivedMessage(final Object message, @NotNull final Envelop envelop) {
    super(message, envelop);
  }
}
