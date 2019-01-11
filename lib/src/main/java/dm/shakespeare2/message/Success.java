package dm.shakespeare2.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare2.actor.Envelop;

/**
 * Created by davide-maestroni on 01/09/2019.
 */
public class Success extends Notification {

  public Success(final Object message, @NotNull final Envelop envelop) {
    super(message, envelop);
  }
}
