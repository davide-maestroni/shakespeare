package dm.shakespeare.log;

import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 02/04/2019.
 */
class JavaLogPrinter implements LogPrinter {

  private static final int DEFAULT_MAX_LINE_SIZE = 2048;
  private static final int DEFAULT_MAX_MESSAGE_SIZE = 1024;
  private static final String FORMAT = "[%s] %s";

  private final Logger mLogger;
  private final int mMaxLineSize;
  private final int mMaxMessageSize;

  JavaLogPrinter(@NotNull final Logger logger) {
    this(logger, DEFAULT_MAX_MESSAGE_SIZE, DEFAULT_MAX_LINE_SIZE);
  }

  JavaLogPrinter(@NotNull final Logger logger, final int maxMessageSize, final int maxLineSize) {
    mLogger = ConstantConditions.notNull("logger", logger);
    mMaxMessageSize = maxMessageSize;
    mMaxLineSize = maxLineSize;
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
    mLogger.log(Level.FINE, printMessage(message), message.getThrowable());
  }

  public void err(@NotNull final LogMessage message) {
    mLogger.log(Level.SEVERE, printMessage(message), message.getThrowable());
  }

  public void wrn(@NotNull final LogMessage message) {
    mLogger.log(Level.WARNING, printMessage(message), message.getThrowable());
  }

  private String printMessage(@NotNull final LogMessage message) {
    return LogMessage.abbreviate(message.formatLogMessage(FORMAT, mMaxMessageSize), mMaxLineSize);
  }
}
