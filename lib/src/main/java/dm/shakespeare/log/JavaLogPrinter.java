/*
 * Copyright 2019 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dm.shakespeare.log;

import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

import dm.shakespeare.util.ConstantConditions;

/**
 * {@code LogPrinter} implementation leveraging the built-in Java logging framework.
 */
class JavaLogPrinter implements LogPrinter {

  private static final int DEFAULT_MAX_LINE_SIZE = 2048;
  private static final int DEFAULT_MAX_MESSAGE_SIZE = 1024;
  private static final String FORMAT = "[%s] %s";

  private final Logger logger;
  private final int maxMessageSize;
  private final int maxTextSize;

  /**
   * Creates a new printer with a default configuration.
   *
   * @param logger the wrapped Java logger instance.
   */
  JavaLogPrinter(@NotNull final Logger logger) {
    this(logger, DEFAULT_MAX_MESSAGE_SIZE, DEFAULT_MAX_LINE_SIZE);
  }

  /**
   * Creates a new printer with the specified configuration.
   *
   * @param logger         the wrapped Java logger instance.
   * @param maxTextSize    the maximum size of a text message.
   * @param maxMessageSize the maximum size of a log message.
   */
  JavaLogPrinter(@NotNull final Logger logger, final int maxTextSize, final int maxMessageSize) {
    this.logger = ConstantConditions.notNull("logger", logger);
    this.maxTextSize = maxTextSize;
    this.maxMessageSize = maxMessageSize;
  }

  public boolean canLogDbg() {
    return logger.isLoggable(Level.FINE);
  }

  public boolean canLogErr() {
    return logger.isLoggable(Level.SEVERE);
  }

  public boolean canLogWrn() {
    return logger.isLoggable(Level.WARNING);
  }

  public void dbg(@NotNull final LogMessage message) {
    logger.log(Level.FINE, printMessage(message), message.getThrowable());
  }

  public void err(@NotNull final LogMessage message) {
    logger.log(Level.SEVERE, printMessage(message), message.getThrowable());
  }

  public void wrn(@NotNull final LogMessage message) {
    logger.log(Level.WARNING, printMessage(message), message.getThrowable());
  }

  private String printMessage(@NotNull final LogMessage message) {
    return LogMessage.abbreviate(message.formatLogMessage(FORMAT, maxTextSize), maxMessageSize);
  }
}
