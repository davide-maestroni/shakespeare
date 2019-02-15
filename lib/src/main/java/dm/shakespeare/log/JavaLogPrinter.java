package dm.shakespeare.log;

import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 02/04/2019.
 */
class JavaLogPrinter implements LogPrinter {

  private static final String FORMAT = "[%s] %s";

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

  public void dbg(@NotNull final LogMessage message) {
    mLogger.log(Level.FINE, message.formatMessage(FORMAT), message.getThrowable());
  }

  public void err(@NotNull final LogMessage message) {
    mLogger.log(Level.SEVERE, message.formatMessage(FORMAT), message.getThrowable());
  }

  public void wrn(@NotNull final LogMessage message) {
    mLogger.log(Level.WARNING, message.formatMessage(FORMAT), message.getThrowable());
  }
}
