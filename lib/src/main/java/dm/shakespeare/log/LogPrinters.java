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

import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;

import dm.shakespeare.util.ConstantConditions;

/**
 * Utility class providing constructors for {@link LogPrinter} classes.
 */
public class LogPrinters {

  /**
   * Avoid explicit instantiation.
   */
  private LogPrinters() {
    ConstantConditions.avoid();
  }

  /**
   * Creates a new log printer leveraging the built-in Java logging framework.<br>
   * The printer will be initialized with a default configuration.
   *
   * @param name the logger name.
   * @return the new log printer.
   */
  @NotNull
  public static LogPrinter javaLoggingPrinter(@NotNull final String name) {
    return new JavaLogPrinter(Logger.getLogger(name));
  }

  /**
   * Creates a new log printer leveraging the built-in Java logging framework.
   *
   * @param name           the logger name.
   * @param maxTextSize    the maximum size of a text message.
   * @param maxMessageSize the maximum size of the formatted log.
   * @return the new log printer.
   */
  @NotNull
  public static LogPrinter javaLoggingPrinter(@NotNull final String name, final int maxTextSize,
      final int maxMessageSize) {
    return new JavaLogPrinter(Logger.getLogger(name), maxTextSize, maxMessageSize);
  }

  /**
   * Creates a new log printer broadcasting the same log message to the specified instances.
   *
   * @param printers the log printers to merge.
   * @return the merging printer.
   */
  @NotNull
  public static LogPrinter mergePrinters(@NotNull final LogPrinter... printers) {
    return mergePrinters(Arrays.asList(printers));
  }

  /**
   * Creates a new log printer broadcasting the same log message to the specified instances.
   *
   * @param printers the log printers to merge.
   * @return the merging printer.
   */
  @NotNull
  private static LogPrinter mergePrinters(
      @NotNull final Collection<? extends LogPrinter> printers) {
    return new MultiPrinter(printers);
  }
}
