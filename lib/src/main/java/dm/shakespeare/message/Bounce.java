package dm.shakespeare.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.actor.Options;
import dm.shakespeare.config.BuildConfig;

/**
 * Created by davide-maestroni on 01/09/2019.
 */
public class Bounce extends Receipt {

  private static final long serialVersionUID = BuildConfig.VERSION_HASH_CODE;

  public Bounce(final Object message, @NotNull final Options options) {
    super(message, options);
  }
}
