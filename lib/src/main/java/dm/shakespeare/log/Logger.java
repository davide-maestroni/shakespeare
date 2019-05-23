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
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

import dm.shakespeare.util.ConstantConditions;

/**
 * Utility class used for logging messages.<br>
 * The levels of logging has been reduced to 3 in order to simplify the implementation and avoid
 * misinterpretations. More in details, the {@code ERROR} level is meant to trace all those events
 * which represent code malfunctioning or unexpected behaviors; the {@code WARNING} level is used
 * to signal those events which are not completely unexpected but might represent an error in the
 * usage of the exposed APIs; the {@code DEBUG} level collects all any other type of event.
 */
@SuppressWarnings("WeakerAccess")
public class Logger {

  private final Locale locale;
  private final LogPrinter printer;

  private Logger(@NotNull final LogPrinter printer, @NotNull final Locale locale) {
    this.printer = ConstantConditions.notNull("printer", printer);
    this.locale = ConstantConditions.notNull("locale", locale);
  }

  /**
   * Returns a new {@code Logger} instance initialized with {@link Locale#ENGLISH} locale.
   *
   * @param printer the logs printer.
   * @return the new logger.
   */
  @NotNull
  public static Logger newLogger(@NotNull final LogPrinter printer) {
    return newLogger(printer, Locale.ENGLISH);
  }

  /**
   * Returns a new {@code Logger} instance initialized with the specified locale.
   *
   * @param printer the logs printer.
   * @param locale  the logs locale.
   * @return the new logger.
   */
  @NotNull
  public static Logger newLogger(@NotNull final LogPrinter printer, @NotNull final Locale locale) {
    return new Logger(printer, locale);
  }

  /**
   * Logs a debug message.
   *
   * @param message the message.
   */
  public void dbg(@Nullable final String message) {
    final LogPrinter printer = this.printer;
    if (printer.canLogDbg()) {
      printer.dbg(new LogMessage(locale, null, message));
    }
  }

  /**
   * Logs a debug message.
   *
   * @param format the message format.
   * @param arg1   the first format argument.
   */
  public void dbg(@NotNull final String format, @Nullable final Object arg1) {
    final LogPrinter printer = this.printer;
    if (printer.canLogDbg()) {
      printer.dbg(new LogMessage(locale, null, format, arg1));
    }
  }

  /**
   * Logs a debug message.
   *
   * @param format the message format.
   * @param arg1   the first format argument.
   * @param arg2   the second format argument.
   */
  public void dbg(@NotNull final String format, @Nullable final Object arg1,
      @Nullable final Object arg2) {
    final LogPrinter printer = this.printer;
    if (printer.canLogDbg()) {
      printer.dbg(new LogMessage(locale, null, format, arg1, arg2));
    }
  }

  /**
   * Logs a debug message.
   *
   * @param format the message format.
   * @param arg1   the first format argument.
   * @param arg2   the second format argument.
   * @param arg3   the third format argument.
   */
  public void dbg(@NotNull final String format, @Nullable final Object arg1,
      @Nullable final Object arg2, @Nullable final Object arg3) {
    final LogPrinter printer = this.printer;
    if (printer.canLogDbg()) {
      printer.dbg(new LogMessage(locale, null, format, arg1, arg2, arg3));
    }
  }

  /**
   * Logs a debug message.
   *
   * @param format the message format.
   * @param arg1   the first format argument.
   * @param arg2   the second format argument.
   * @param arg3   the third format argument.
   * @param arg4   the fourth format argument.
   */
  public void dbg(@NotNull final String format, @Nullable final Object arg1,
      @Nullable final Object arg2, @Nullable final Object arg3, @Nullable final Object arg4) {
    final LogPrinter printer = this.printer;
    if (printer.canLogDbg()) {
      printer.dbg(new LogMessage(locale, null, format, arg1, arg2, arg3, arg4));
    }
  }

  /**
   * Logs a debug message.
   *
   * @param format the message format.
   * @param args   the format arguments.
   */
  public void dbg(@NotNull final String format, @Nullable final Object... args) {
    final LogPrinter printer = this.printer;
    if (printer.canLogDbg()) {
      printer.dbg(new LogMessage(locale, null, format, args));
    }
  }

  /**
   * Logs a debug exception.
   *
   * @param throwable the related throwable.
   */
  public void dbg(@Nullable final Throwable throwable) {
    final LogPrinter printer = this.printer;
    if (printer.canLogDbg()) {
      printer.dbg(new LogMessage(locale, throwable, null));
    }
  }

  /**
   * Logs a debug message.
   *
   * @param throwable the related throwable.
   * @param message   the message.
   */
  public void dbg(@Nullable final Throwable throwable, @Nullable final String message) {
    final LogPrinter printer = this.printer;
    if (printer.canLogDbg()) {
      printer.dbg(new LogMessage(locale, throwable, message));
    }
  }

  /**
   * Logs a debug message.
   *
   * @param throwable the related throwable.
   * @param format    the message format.
   * @param arg1      the first format argument.
   */
  public void dbg(@NotNull final Throwable throwable, @NotNull final String format,
      @Nullable final Object arg1) {
    final LogPrinter printer = this.printer;
    if (printer.canLogDbg()) {
      printer.dbg(new LogMessage(locale, throwable, format, arg1));
    }
  }

  /**
   * Logs a debug message.
   *
   * @param throwable the related throwable.
   * @param format    the message format.
   * @param arg1      the first format argument.
   * @param arg2      the second format argument.
   */
  public void dbg(@NotNull final Throwable throwable, @NotNull final String format,
      @Nullable final Object arg1, @Nullable final Object arg2) {
    final LogPrinter printer = this.printer;
    if (printer.canLogDbg()) {
      printer.dbg(new LogMessage(locale, throwable, format, arg1, arg2));
    }
  }

  /**
   * Logs a debug message.
   *
   * @param throwable the related throwable.
   * @param format    the message format.
   * @param arg1      the first format argument.
   * @param arg2      the second format argument.
   * @param arg3      the third format argument.
   */
  public void dbg(@NotNull final Throwable throwable, @NotNull final String format,
      @Nullable final Object arg1, @Nullable final Object arg2, @Nullable final Object arg3) {
    final LogPrinter printer = this.printer;
    if (printer.canLogDbg()) {
      printer.dbg(new LogMessage(locale, throwable, format, arg1, arg2, arg3));
    }
  }

  /**
   * Logs a debug message.
   *
   * @param throwable the related throwable.
   * @param format    the message format.
   * @param arg1      the first format argument.
   * @param arg2      the second format argument.
   * @param arg3      the third format argument.
   * @param arg4      the fourth format argument.
   */
  public void dbg(@NotNull final Throwable throwable, @NotNull final String format,
      @Nullable final Object arg1, @Nullable final Object arg2, @Nullable final Object arg3,
      @Nullable final Object arg4) {
    final LogPrinter printer = this.printer;
    if (printer.canLogDbg()) {
      printer.dbg(new LogMessage(locale, throwable, format, arg1, arg2, arg3, arg4));
    }
  }

  /**
   * Logs a debug message.
   *
   * @param throwable the related throwable.
   * @param format    the message format.
   * @param args      the format arguments.
   */
  public void dbg(@NotNull final Throwable throwable, @NotNull final String format,
      @Nullable final Object... args) {
    final LogPrinter printer = this.printer;
    if (printer.canLogDbg()) {
      printer.dbg(new LogMessage(locale, throwable, format, args));
    }
  }

  /**
   * Logs an error message.
   *
   * @param message the message.
   */
  public void err(@Nullable final String message) {
    final LogPrinter printer = this.printer;
    if (printer.canLogErr()) {
      printer.err(new LogMessage(locale, null, message));
    }
  }

  /**
   * Logs an error message.
   *
   * @param format the message format.
   * @param arg1   the first format argument.
   */
  public void err(@NotNull final String format, @Nullable final Object arg1) {
    final LogPrinter printer = this.printer;
    if (printer.canLogErr()) {
      printer.err(new LogMessage(locale, null, format, arg1));
    }
  }

  /**
   * Logs an error message.
   *
   * @param format the message format.
   * @param arg1   the first format argument.
   * @param arg2   the second format argument.
   */
  public void err(@NotNull final String format, @Nullable final Object arg1,
      @Nullable final Object arg2) {
    final LogPrinter printer = this.printer;
    if (printer.canLogErr()) {
      printer.err(new LogMessage(locale, null, format, arg1, arg2));
    }
  }

  /**
   * Logs an error message.
   *
   * @param format the message format.
   * @param arg1   the first format argument.
   * @param arg2   the second format argument.
   * @param arg3   the third format argument.
   */
  public void err(@NotNull final String format, @Nullable final Object arg1,
      @Nullable final Object arg2, @Nullable final Object arg3) {
    final LogPrinter printer = this.printer;
    if (printer.canLogErr()) {
      printer.err(new LogMessage(locale, null, format, arg1, arg2, arg3));
    }
  }

  /**
   * Logs an error message.
   *
   * @param format the message format.
   * @param arg1   the first format argument.
   * @param arg2   the second format argument.
   * @param arg3   the third format argument.
   * @param arg4   the fourth format argument.
   */
  public void err(@NotNull final String format, @Nullable final Object arg1,
      @Nullable final Object arg2, @Nullable final Object arg3, @Nullable final Object arg4) {
    final LogPrinter printer = this.printer;
    if (printer.canLogErr()) {
      printer.err(new LogMessage(locale, null, format, arg1, arg2, arg3, arg4));
    }
  }

  /**
   * Logs an error message.
   *
   * @param format the message format.
   * @param args   the format arguments.
   */
  public void err(@NotNull final String format, @Nullable final Object... args) {
    final LogPrinter printer = this.printer;
    if (printer.canLogErr()) {
      printer.err(new LogMessage(locale, null, format, args));
    }
  }

  /**
   * Logs an error exception.
   *
   * @param throwable the related throwable.
   */
  public void err(@NotNull final Throwable throwable) {
    final LogPrinter printer = this.printer;
    if (printer.canLogErr()) {
      printer.err(new LogMessage(locale, throwable, null));
    }
  }

  /**
   * Logs an error message.
   *
   * @param throwable the related throwable.
   * @param message   the message.
   */
  public void err(@NotNull final Throwable throwable, @Nullable final String message) {
    final LogPrinter printer = this.printer;
    if (printer.canLogErr()) {
      printer.err(new LogMessage(locale, throwable, message));
    }
  }

  /**
   * Logs an error message.
   *
   * @param throwable the related throwable.
   * @param format    the message format.
   * @param arg1      the first format argument.
   */
  public void err(@NotNull final Throwable throwable, @NotNull final String format,
      @Nullable final Object arg1) {
    final LogPrinter printer = this.printer;
    if (printer.canLogErr()) {
      printer.err(new LogMessage(locale, throwable, format, arg1));
    }
  }

  /**
   * Logs an error message.
   *
   * @param throwable the related throwable.
   * @param format    the message format.
   * @param arg1      the first format argument.
   * @param arg2      the second format argument.
   */
  public void err(@NotNull final Throwable throwable, @NotNull final String format,
      @Nullable final Object arg1, @Nullable final Object arg2) {
    final LogPrinter printer = this.printer;
    if (printer.canLogErr()) {
      printer.err(new LogMessage(locale, throwable, format, arg1, arg2));
    }
  }

  /**
   * Logs an error message.
   *
   * @param throwable the related throwable.
   * @param format    the message format.
   * @param arg1      the first format argument.
   * @param arg2      the second format argument.
   * @param arg3      the third format argument.
   */
  public void err(@NotNull final Throwable throwable, @NotNull final String format,
      @Nullable final Object arg1, @Nullable final Object arg2, @Nullable final Object arg3) {
    final LogPrinter printer = this.printer;
    if (printer.canLogErr()) {
      printer.err(new LogMessage(locale, throwable, format, arg1, arg2, arg3));
    }
  }

  /**
   * Logs an error message.
   *
   * @param throwable the related throwable.
   * @param format    the message format.
   * @param arg1      the first format argument.
   * @param arg2      the second format argument.
   * @param arg3      the third format argument.
   * @param arg4      the fourth format argument.
   */
  public void err(@NotNull final Throwable throwable, @NotNull final String format,
      @Nullable final Object arg1, @Nullable final Object arg2, @Nullable final Object arg3,
      @Nullable final Object arg4) {
    final LogPrinter printer = this.printer;
    if (printer.canLogErr()) {
      printer.err(new LogMessage(locale, throwable, format, arg1, arg2, arg3, arg4));
    }
  }

  /**
   * Logs an error message.
   *
   * @param throwable the related throwable.
   * @param format    the message format.
   * @param args      the format arguments.
   */
  public void err(@NotNull final Throwable throwable, @NotNull final String format,
      @Nullable final Object... args) {
    final LogPrinter printer = this.printer;
    if (printer.canLogErr()) {
      printer.err(new LogMessage(locale, throwable, format, args));
    }
  }

  /**
   * Returns the logger locale.
   *
   * @return the logger locale.
   */
  @NotNull
  public Locale getLocale() {
    return locale;
  }

  /**
   * Returns the printer instance of this logger.
   *
   * @return the printer instance.
   */
  @NotNull
  public LogPrinter getPrinter() {
    return printer;
  }

  /**
   * Logs a warning message.
   *
   * @param message the message.
   */
  public void wrn(@Nullable final String message) {
    final LogPrinter printer = this.printer;
    if (printer.canLogWrn()) {
      printer.wrn(new LogMessage(locale, null, message));
    }
  }

  /**
   * Logs a warning message.
   *
   * @param format the message format.
   * @param arg1   the first format argument.
   */
  public void wrn(@NotNull final String format, @Nullable final Object arg1) {
    final LogPrinter printer = this.printer;
    if (printer.canLogWrn()) {
      printer.wrn(new LogMessage(locale, null, format, arg1));
    }
  }

  /**
   * Logs a warning message.
   *
   * @param format the message format.
   * @param arg1   the first format argument.
   * @param arg2   the second format argument.
   */
  public void wrn(@NotNull final String format, @Nullable final Object arg1,
      @Nullable final Object arg2) {
    final LogPrinter printer = this.printer;
    if (printer.canLogWrn()) {
      printer.wrn(new LogMessage(locale, null, format, arg1, arg2));
    }
  }

  /**
   * Logs a warning message.
   *
   * @param format the message format.
   * @param arg1   the first format argument.
   * @param arg2   the second format argument.
   * @param arg3   the third format argument.
   */
  public void wrn(@NotNull final String format, @Nullable final Object arg1,
      @Nullable final Object arg2, @Nullable final Object arg3) {
    final LogPrinter printer = this.printer;
    if (printer.canLogWrn()) {
      printer.wrn(new LogMessage(locale, null, format, arg1, arg2, arg3));
    }
  }

  /**
   * Logs a warning message.
   *
   * @param format the message format.
   * @param arg1   the first format argument.
   * @param arg2   the second format argument.
   * @param arg3   the third format argument.
   * @param arg4   the fourth format argument.
   */
  public void wrn(@NotNull final String format, @Nullable final Object arg1,
      @Nullable final Object arg2, @Nullable final Object arg3, @Nullable final Object arg4) {
    final LogPrinter printer = this.printer;
    if (printer.canLogWrn()) {
      printer.wrn(new LogMessage(locale, null, format, arg1, arg2, arg3, arg4));
    }
  }

  /**
   * Logs a warning message.
   *
   * @param format the message format.
   * @param args   the format arguments.
   */
  public void wrn(@NotNull final String format, @Nullable final Object... args) {
    final LogPrinter printer = this.printer;
    if (printer.canLogWrn()) {
      printer.wrn(new LogMessage(locale, null, format, args));
    }
  }

  /**
   * Logs a warning exception.
   *
   * @param throwable the related throwable.
   */
  public void wrn(@NotNull final Throwable throwable) {
    final LogPrinter printer = this.printer;
    if (printer.canLogWrn()) {
      printer.wrn(new LogMessage(locale, throwable, null));
    }
  }

  /**
   * Logs a warning message.
   *
   * @param throwable the related throwable.
   * @param message   the message.
   */
  public void wrn(@NotNull final Throwable throwable, @Nullable final String message) {
    final LogPrinter printer = this.printer;
    if (printer.canLogWrn()) {
      printer.wrn(new LogMessage(locale, throwable, message));
    }
  }

  /**
   * Logs a warning message.
   *
   * @param throwable the related throwable.
   * @param format    the message format.
   * @param arg1      the first format argument.
   */
  public void wrn(@NotNull final Throwable throwable, @NotNull final String format,
      @Nullable final Object arg1) {
    final LogPrinter printer = this.printer;
    if (printer.canLogWrn()) {
      printer.wrn(new LogMessage(locale, throwable, format, arg1));
    }
  }

  /**
   * Logs a warning message.
   *
   * @param throwable the related throwable.
   * @param format    the message format.
   * @param arg1      the first format argument.
   * @param arg2      the second format argument.
   */
  public void wrn(@NotNull final Throwable throwable, @NotNull final String format,
      @Nullable final Object arg1, @Nullable final Object arg2) {
    final LogPrinter printer = this.printer;
    if (printer.canLogWrn()) {
      printer.wrn(new LogMessage(locale, throwable, format, arg1, arg2));
    }
  }

  /**
   * Logs a warning message.
   *
   * @param throwable the related throwable.
   * @param format    the message format.
   * @param arg1      the first format argument.
   * @param arg2      the second format argument.
   * @param arg3      the third format argument.
   */
  public void wrn(@NotNull final Throwable throwable, @NotNull final String format,
      @Nullable final Object arg1, @Nullable final Object arg2, @Nullable final Object arg3) {
    final LogPrinter printer = this.printer;
    if (printer.canLogWrn()) {
      printer.wrn(new LogMessage(locale, throwable, format, arg1, arg2, arg3));
    }
  }

  /**
   * Logs a warning message.
   *
   * @param throwable the related throwable.
   * @param format    the message format.
   * @param arg1      the first format argument.
   * @param arg2      the second format argument.
   * @param arg3      the third format argument.
   * @param arg4      the fourth format argument.
   */
  public void wrn(@NotNull final Throwable throwable, @NotNull final String format,
      @Nullable final Object arg1, @Nullable final Object arg2, @Nullable final Object arg3,
      @Nullable final Object arg4) {
    final LogPrinter printer = this.printer;
    if (printer.canLogWrn()) {
      printer.wrn(new LogMessage(locale, throwable, format, arg1, arg2, arg3, arg4));
    }
  }

  /**
   * Logs a warning message.
   *
   * @param throwable the related throwable.
   * @param format    the message format.
   * @param args      the format arguments.
   */
  public void wrn(@NotNull final Throwable throwable, @NotNull final String format,
      @Nullable final Object... args) {
    final LogPrinter printer = this.printer;
    if (printer.canLogWrn()) {
      printer.wrn(new LogMessage(locale, throwable, format, args));
    }
  }
}
