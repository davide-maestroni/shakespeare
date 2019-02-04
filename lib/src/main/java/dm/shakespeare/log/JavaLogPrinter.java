package dm.shakespeare.log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 02/04/2019.
 */
class JavaLogPrinter implements LogPrinter {

  private final Logger mLogger;

  JavaLogPrinter(@NotNull final Logger logger) {
    mLogger = ConstantConditions.notNull("logger", logger);
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
