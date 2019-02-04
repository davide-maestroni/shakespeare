package dm.shakespeare.log;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 08/06/2018.
 */
public class LogPrinters {

  private LogPrinters() {
    ConstantConditions.avoid();
  }

  @NotNull
  public static LogPrinter javaLoggingPrinter(@NotNull final String name) {
    return new JavaLogPrinter(Logger.getLogger(name));
  }

  @NotNull
  public static LogPrinter mergePrinters(@NotNull final LogPrinter... printers) {
    return mergePrinters(Arrays.asList(printers));
  }

  @NotNull
  private static LogPrinter mergePrinters(
      @NotNull final Collection<? extends LogPrinter> printers) {
    return new MultiPrinter(printers);
  }
}
