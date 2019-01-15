package dm.shakespeare2.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare2.actor.Options;

/**
 * Created by davide-maestroni on 01/09/2019.
 */
public class QuotaExceeded extends Bounce {

  public QuotaExceeded(final Object message, @NotNull final Options options) {
    super(message, options);
  }
}
