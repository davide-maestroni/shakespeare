/*
 * Copyright 2018 Davide Maestroni
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

import dm.shakespeare.util.ConstantConditions;

/**
 * Utility class used for logging messages.
 * <p>
 * Created by davide-maestroni on 10/03/2014.
 */
@SuppressWarnings("WeakerAccess")
public class Logger {

  private static final Locale DEFAULT_LOCALE = Locale.getDefault();

  private final Locale mLocale;
  private final LogPrinter mPrinter;

  /**
   * Constructor.
   *
   * @param printer the logs printer.
   * @param locale  the locale instance.
   */
  private Logger(@NotNull final LogPrinter printer, @NotNull final Locale locale) {
    mPrinter = ConstantConditions.notNull("printer", printer);
    mLocale = ConstantConditions.notNull("locale", locale);
  }

  @NotNull
  public static Logger newLogger(@NotNull final LogPrinter printer) {
    return new Logger(printer, DEFAULT_LOCALE);
  }

  @NotNull
  public static Logger newLogger(@NotNull final LogPrinter printer, @NotNull final Locale locale) {
    return new Logger(printer, locale);
  }

  /**
   * Prints the stack trace of the specified throwable into a string.
   *
   * @param throwable the throwable instance.
   * @return the printed stack trace.
   */
  @NotNull
  public static String printStackTrace(@NotNull final Throwable throwable) {
    final StringWriter writer = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(writer);
    throwable.printStackTrace(printWriter);
    printWriter.close();
    return writer.toString();
  }

  /**
   * Logs a debug message.
   *
   * @param message the message.
   */
  public void dbg(@Nullable final String message) {
    final LogPrinter printer = mPrinter;
    if (printer.canLogDbg()) {
      printer.dbg(message, null);
    }
  }

  /**
   * Logs a debug message.
   *
   * @param format the message format.
   * @param arg1   the first format argument.
   */
  public void dbg(@NotNull final String format, @Nullable final Object arg1) {
    final LogPrinter printer = mPrinter;
    if (printer.canLogDbg()) {
      printer.dbg(String.format(mLocale, format, arg1), null);
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
    final LogPrinter printer = mPrinter;
    if (printer.canLogDbg()) {
      printer.dbg(String.format(mLocale, format, arg1, arg2), null);
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
    final LogPrinter printer = mPrinter;
    if (printer.canLogDbg()) {
      printer.dbg(String.format(mLocale, format, arg1, arg2, arg3), null);
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
    final LogPrinter printer = mPrinter;
    if (printer.canLogDbg()) {
      printer.dbg(String.format(mLocale, format, arg1, arg2, arg3, arg4), null);
    }
  }

  /**
   * Logs a debug message.
   *
   * @param format the message format.
   * @param args   the format arguments.
   */
  public void dbg(@NotNull final String format, @Nullable final Object... args) {
    final LogPrinter printer = mPrinter;
    if (printer.canLogDbg()) {
      printer.dbg(String.format(mLocale, format, args), null);
    }
  }

  /**
   * Logs a debug exception.
   *
   * @param throwable the related throwable.
   */
  public void dbg(@Nullable final Throwable throwable) {
    final LogPrinter printer = mPrinter;
    if (printer.canLogDbg()) {
      printer.dbg("", throwable);
    }
  }

  /**
   * Logs a debug message.
   *
   * @param throwable the related throwable.
   * @param message   the message.
   */
  public void dbg(@Nullable final Throwable throwable, @Nullable final String message) {
    final LogPrinter printer = mPrinter;
    if (printer.canLogDbg()) {
      printer.dbg(message, throwable);
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
    final LogPrinter printer = mPrinter;
    if (printer.canLogDbg()) {
      printer.dbg(String.format(mLocale, format, arg1), throwable);
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
    final LogPrinter printer = mPrinter;
    if (printer.canLogDbg()) {
      printer.dbg(String.format(mLocale, format, arg1, arg2), throwable);
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
    final LogPrinter printer = mPrinter;
    if (printer.canLogDbg()) {
      printer.dbg(String.format(mLocale, format, arg1, arg2, arg3), throwable);
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
    final LogPrinter printer = mPrinter;
    if (printer.canLogDbg()) {
      printer.dbg(String.format(mLocale, format, arg1, arg2, arg3, arg4), throwable);
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
    final LogPrinter printer = mPrinter;
    if (printer.canLogDbg()) {
      printer.dbg(String.format(mLocale, format, args), throwable);
    }
  }

  /**
   * Logs an error message.
   *
   * @param message the message.
   */
  public void err(@Nullable final String message) {
    final LogPrinter printer = mPrinter;
    if (printer.canLogErr()) {
      printer.err(message, null);
    }
  }

  /**
   * Logs an error message.
   *
   * @param format the message format.
   * @param arg1   the first format argument.
   */
  public void err(@NotNull final String format, @Nullable final Object arg1) {
    final LogPrinter printer = mPrinter;
    if (printer.canLogErr()) {
      printer.err(String.format(mLocale, format, arg1), null);
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
    final LogPrinter printer = mPrinter;
    if (printer.canLogErr()) {
      printer.err(String.format(mLocale, format, arg1, arg2), null);
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
    final LogPrinter printer = mPrinter;
    if (printer.canLogErr()) {
      printer.err(String.format(mLocale, format, arg1, arg2, arg3), null);
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
    final LogPrinter printer = mPrinter;
    if (printer.canLogErr()) {
      printer.err(String.format(mLocale, format, arg1, arg2, arg3, arg4), null);
    }
  }

  /**
   * Logs an error message.
   *
   * @param format the message format.
   * @param args   the format arguments.
   */
  public void err(@NotNull final String format, @Nullable final Object... args) {
    final LogPrinter printer = mPrinter;
    if (printer.canLogErr()) {
      printer.err(String.format(mLocale, format, args), null);
    }
  }

  /**
   * Logs an error exception.
   *
   * @param throwable the related throwable.
   */
  public void err(@NotNull final Throwable throwable) {
    final LogPrinter printer = mPrinter;
    if (printer.canLogErr()) {
      printer.err("", throwable);
    }
  }

  /**
   * Logs an error message.
   *
   * @param throwable the related throwable.
   * @param message   the message.
   */
  public void err(@NotNull final Throwable throwable, @Nullable final String message) {
    final LogPrinter printer = mPrinter;
    if (printer.canLogErr()) {
      printer.err(message, throwable);
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
    final LogPrinter printer = mPrinter;
    if (printer.canLogErr()) {
      printer.err(String.format(mLocale, format, arg1), throwable);
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
    final LogPrinter printer = mPrinter;
    if (printer.canLogErr()) {
      printer.err(String.format(mLocale, format, arg1, arg2), throwable);
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
    final LogPrinter printer = mPrinter;
    if (printer.canLogErr()) {
      printer.err(String.format(mLocale, format, arg1, arg2, arg3), throwable);
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
    final LogPrinter printer = mPrinter;
    if (printer.canLogErr()) {
      printer.err(String.format(mLocale, format, arg1, arg2, arg3, arg4), throwable);
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
    final LogPrinter printer = mPrinter;
    if (printer.canLogErr()) {
      printer.err(String.format(mLocale, format, args), throwable);
    }
  }

  @NotNull
  public Locale getLocale() {
    return mLocale;
  }

  /**
   * Returns the log instance of this logger.
   *
   * @return the log instance.
   */
  @NotNull
  public LogPrinter getPrinter() {
    return mPrinter;
  }

  /**
   * Logs a warning message.
   *
   * @param message the message.
   */
  public void wrn(@Nullable final String message) {
    final LogPrinter printer = mPrinter;
    if (printer.canLogWrn()) {
      printer.wrn(message, null);
    }
  }

  /**
   * Logs a warning message.
   *
   * @param format the message format.
   * @param arg1   the first format argument.
   */
  public void wrn(@NotNull final String format, @Nullable final Object arg1) {
    final LogPrinter printer = mPrinter;
    if (printer.canLogWrn()) {
      printer.wrn(String.format(mLocale, format, arg1), null);
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
    final LogPrinter printer = mPrinter;
    if (printer.canLogWrn()) {
      printer.wrn(String.format(mLocale, format, arg1, arg2), null);
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
    final LogPrinter printer = mPrinter;
    if (printer.canLogWrn()) {
      printer.wrn(String.format(mLocale, format, arg1, arg2, arg3), null);
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
    final LogPrinter printer = mPrinter;
    if (printer.canLogWrn()) {
      printer.wrn(String.format(mLocale, format, arg1, arg2, arg3, arg4), null);
    }
  }

  /**
   * Logs a warning message.
   *
   * @param format the message format.
   * @param args   the format arguments.
   */
  public void wrn(@NotNull final String format, @Nullable final Object... args) {
    final LogPrinter printer = mPrinter;
    if (printer.canLogWrn()) {
      printer.wrn(String.format(mLocale, format, args), null);
    }
  }

  /**
   * Logs a warning exception.
   *
   * @param throwable the related throwable.
   */
  public void wrn(@NotNull final Throwable throwable) {
    final LogPrinter printer = mPrinter;
    if (printer.canLogWrn()) {
      printer.wrn("", throwable);
    }
  }

  /**
   * Logs a warning message.
   *
   * @param throwable the related throwable.
   * @param message   the message.
   */
  public void wrn(@NotNull final Throwable throwable, @Nullable final String message) {
    final LogPrinter printer = mPrinter;
    if (printer.canLogWrn()) {
      printer.wrn(message, throwable);
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
    final LogPrinter printer = mPrinter;
    if (printer.canLogWrn()) {
      printer.wrn(String.format(mLocale, format, arg1), throwable);
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
    final LogPrinter printer = mPrinter;
    if (printer.canLogWrn()) {
      printer.wrn(String.format(mLocale, format, arg1, arg2), throwable);
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
    final LogPrinter printer = mPrinter;
    if (printer.canLogWrn()) {
      printer.wrn(String.format(mLocale, format, arg1, arg2, arg3), throwable);
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
    final LogPrinter printer = mPrinter;
    if (printer.canLogWrn()) {
      printer.wrn(String.format(mLocale, format, arg1, arg2, arg3, arg4), throwable);
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
    final LogPrinter printer = mPrinter;
    if (printer.canLogWrn()) {
      printer.wrn(String.format(mLocale, format, args), throwable);
    }
  }
}
