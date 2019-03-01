package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.message.Bounce;
import dm.shakespeare.message.Failure;

/**
 * Created by davide-maestroni on 01/25/2019.
 */
public class PlotFailureException extends IllegalStateException {

  public PlotFailureException() {
  }

  public PlotFailureException(final Throwable cause) {
    super(cause);
  }

  @NotNull
  public static Throwable getOrNew(@NotNull final Bounce message) {
    return (message instanceof Failure) ? ((Failure) message).getCause()
        : new PlotFailureException();
  }
}
