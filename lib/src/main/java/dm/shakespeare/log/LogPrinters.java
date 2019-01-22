package dm.shakespeare.log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Level;
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

  private static class JavaLogPrinter implements LogPrinter {

    private final Logger mLogger;

    private JavaLogPrinter(@NotNull final Logger logger) {
      mLogger = logger;
    }

    public boolean canLogDbg() {
      return mLogger.isLoggable(Level.FINE);
    }

    public boolean canLogErr() {
      return mLogger.isLoggable(Level.SEVERE);
    }

    public boolean canLogWrn() {
      return mLogger.isLoggable(Level.WARNING);
    }

    public void dbg(@Nullable final String message, @Nullable final Throwable throwable) {
      mLogger.log(Level.FINE, message, throwable);
    }

    public void err(@Nullable final String message, @Nullable final Throwable throwable) {
      mLogger.log(Level.SEVERE, message, throwable);
    }

    public void wrn(@Nullable final String message, @Nullable final Throwable throwable) {
      mLogger.log(Level.WARNING, message, throwable);
    }
  }

  private static class MultiPrinter implements LogPrinter {

    private final HashSet<LogPrinter> mPrinters;

    private MultiPrinter(@NotNull final Collection<? extends LogPrinter> printers) {
      mPrinters = new HashSet<LogPrinter>(ConstantConditions.notNullElements("printers", printers));
    }

    public boolean canLogDbg() {
      for (final LogPrinter printer : mPrinters) {
        if (printer.canLogDbg()) {
          return true;
        }
      }
      return false;
    }

    public boolean canLogErr() {
      for (final LogPrinter printer : mPrinters) {
        if (printer.canLogErr()) {
          return true;
        }
      }
      return false;
    }

    public boolean canLogWrn() {
      for (final LogPrinter printer : mPrinters) {
        if (printer.canLogWrn()) {
          return true;
        }
      }
      return false;
    }

    public void dbg(@Nullable final String message, @Nullable final Throwable throwable) {
      for (final LogPrinter printer : mPrinters) {
        printer.dbg(message, throwable);
      }
    }

    public void err(@Nullable final String message, @Nullable final Throwable throwable) {
      for (final LogPrinter printer : mPrinters) {
        printer.err(message, throwable);
      }
    }

    public void wrn(@Nullable final String message, @Nullable final Throwable throwable) {
      for (final LogPrinter printer : mPrinters) {
        printer.wrn(message, throwable);
      }
    }
  }
}
