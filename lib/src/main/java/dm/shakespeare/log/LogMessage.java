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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

/**
 * Class storing the information about a log message.
 */
public class LogMessage {

  private final Object[] args;
  private final Thread callingThread;
  private final String format;
  private final Locale locale;
  private final String message;
  private final Throwable throwable;

  /**
   * Creates a new log message with the specified formatted message.
   *
   * @param locale    the message default locale.
   * @param throwable the optional throwable instance.
   * @param message   the pre-formatted message.
   */
  LogMessage(@Nullable final Locale locale, @Nullable final Throwable throwable,
      @Nullable final String message) {
    this(locale, throwable, message, null, (Object[]) null);
  }

  /**
   * Creates a new log message with the specified format and arguments.
   *
   * @param locale    the message default locale.
   * @param throwable the optional throwable instance.
   * @param format    the message format.
   * @param args      the format arguments.
   */
  LogMessage(@Nullable final Locale locale, @Nullable final Throwable throwable,
      @NotNull final String format, @Nullable final Object... args) {
    this(locale, throwable, null, format, args);
  }

  private LogMessage(@Nullable final Locale locale, @Nullable final Throwable throwable,
      @Nullable final String message, @Nullable final String format,
      @Nullable final Object... args) {
    this.locale = locale;
    this.throwable = throwable;
    this.message = message;
    this.format = format;
    this.args = args;
    callingThread = Thread.currentThread();
  }

  /**
   * Abbreviates the specified message so that its size does not exceed a maximum size.<br>
   * The returned string will terminate with "..." when the original message length is greater than
   * the specified maximum size. If the specified size is less than 3, the string "..." will be
   * always returned.<br>
   * If the input message is {@code null}, the output will be {@code null}.
   *
   * @param message the message to abbreviate.
   * @param maxSize the output maximum size.
   * @return the abbreviated message.
   */
  @Nullable
  public static String abbreviate(@Nullable final String message, final int maxSize) {
    if (message != null) {
      return (message.length() <= maxSize) ? message
          : message.substring(0, Math.max(maxSize - 3, 0)) + "...";
    }
    return null;
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
   * Formats this log message by using the specified locale.<br>
   * The arguments passed to the format will be, in the order: the calling thread, the (optionally
   * abbreviated) text message, the stack trace of the logged throwable (if any).
   *
   * @param locale      the format locale.
   * @param format      the format string.
   * @param maxTextSize the maximum size of the text message.
   * @return the formatted log message.
   */
  @Nullable
  public String formatLogMessage(@Nullable final Locale locale, @NotNull final String format,
      final int maxTextSize) {
    return String.format(locale, format, callingThread,
        abbreviate(formatTextMessage(locale), maxTextSize), printStackTrace());
  }

  /**
   * Formats this log message by using the specified locale.<br>
   * The arguments passed to the format will be, in the order: the calling thread, the (optionally
   * abbreviated) text message, the stack trace of the logged throwable (if any), the specified
   * additional arguments.
   *
   * @param locale         the format locale.
   * @param format         the format string.
   * @param maxTextSize    the maximum size of the text message.
   * @param additionalArgs the list of additional arguments.
   * @return the formatted log message.
   */
  @Nullable
  public String formatLogMessage(@Nullable final Locale locale, @NotNull final String format,
      final int maxTextSize, @Nullable final Object... additionalArgs) {
    if ((additionalArgs == null) || (additionalArgs.length == 0)) {
      return formatLogMessage(locale, format, maxTextSize);
    }
    final int length = additionalArgs.length;
    final Object[] args = new Object[3 + length];
    args[0] = callingThread;
    args[1] = abbreviate(formatTextMessage(locale), maxTextSize);
    args[2] = printStackTrace();
    System.arraycopy(additionalArgs, 0, args, 3, length);
    return String.format(locale, format, args);
  }

  /**
   * Formats this log message by using the logger locale.<br>
   * The arguments passed to the format will be, in the order: the calling thread, the (optionally
   * abbreviated) text message, the stack trace of the logged throwable (if any).
   *
   * @param format      the format string.
   * @param maxTextSize the maximum size of the text message.
   * @return the formatted log message.
   */
  @Nullable
  public String formatLogMessage(@NotNull final String format, final int maxTextSize) {
    return formatLogMessage(locale, format, maxTextSize);
  }

  /**
   * Formats this log message by using the logger locale.<br>
   * The arguments passed to the format will be, in the order: the calling thread, the (optionally
   * abbreviated) text message, the stack trace of the logged throwable (if any), the specified
   * additional arguments.
   *
   * @param format         the format string.
   * @param maxTextSize    the maximum size of the text message.
   * @param additionalArgs the list of additional arguments.
   * @return the formatted log message.
   */
  @Nullable
  public String formatLogMessage(@NotNull final String format, final int maxTextSize,
      @Nullable final Object... additionalArgs) {
    return formatLogMessage(locale, format, maxTextSize, additionalArgs);
  }

  /**
   * Formats the text message initialized in the constructor by using the logger locale.
   *
   * @return the formatted message or {@code null}.
   */
  @Nullable
  public String formatTextMessage() {
    return formatTextMessage(locale);
  }

  /**
   * Formats the text message initialized in the constructor by using the specified locale.
   *
   * @param locale the format locale.
   * @return the formatted message or {@code null}.
   */
  @Nullable
  public String formatTextMessage(@Nullable final Locale locale) {
    final String format = this.format;
    return (format != null) ? String.format(locale, format, args) : message;
  }

  /**
   * Returns a copy of the text format arguments.
   *
   * @return the arguments or {@code null}.
   */
  @Nullable
  public Object[] getArgs() {
    final Object[] args = this.args;
    return (args != null) ? args.clone() : null;
  }

  /**
   * Returns the logger calling thread.
   *
   * @return the thread instance.
   */
  @NotNull
  public Thread getCallingThread() {
    return callingThread;
  }

  /**
   * Returns the text format.
   *
   * @return the format or {@code null}.
   */
  @Nullable
  public String getFormat() {
    return format;
  }

  /**
   * Returns the logger locale.
   *
   * @return the locale instance or {@code null}.
   */
  @Nullable
  public Locale getLocale() {
    return locale;
  }

  /**
   * Returns the pre-formatted text message.
   *
   * @return the message or {@code null}.
   */
  @Nullable
  public String getMessage() {
    return message;
  }

  /**
   * Returns the optional throwable.
   *
   * @return the throwable or {@code null}.
   */
  @Nullable
  public Throwable getThrowable() {
    return throwable;
  }

  /**
   * Prints the logged throwable stack trace into a string.<br>
   * If no throwable was set {@code null} will be returned.
   *
   * @return the stack trace or {@code null}.
   */
  @Nullable
  public String printStackTrace() {
    final Throwable throwable = this.throwable;
    return (throwable != null) ? printStackTrace(throwable) : null;
  }

  /**
   * Returns a string representation of this object as the formatted text message.
   *
   * @return the formatted message.
   */
  @Override
  public String toString() {
    final String message = formatTextMessage();
    return (message != null) ? message : "null";
  }
}
