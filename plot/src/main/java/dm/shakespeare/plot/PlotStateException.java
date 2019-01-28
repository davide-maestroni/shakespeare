package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.message.Bounce;
import dm.shakespeare.message.Failure;

/**
 * Created by davide-maestroni on 01/25/2019.
 */
public class PlotStateException extends Exception {

  @NotNull
  public static Throwable getOrNew(@NotNull final Bounce message) {
    return (message instanceof Failure) ? ((Failure) message).getCause() : new PlotStateException();
  }
}
