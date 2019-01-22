package dm.shakespeare.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.actor.Options;
import dm.shakespeare.config.BuildConfig;

/**
 * Created by davide-maestroni on 01/16/2019.
 */
public class Delivery extends Receipt {

  private static final long serialVersionUID = BuildConfig.VERSION_HASH_CODE;

  public Delivery(final Object message, @NotNull final Options options) {
    super(message, options);
  }
}
