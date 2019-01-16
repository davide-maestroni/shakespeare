package dm.shakespeare2.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare2.actor.Options;

/**
 * Created by davide-maestroni on 01/16/2019.
 */
public class Delivery extends Receipt {

  public Delivery(final Object message, @NotNull final Options options) {
    super(message, options);
  }
}
