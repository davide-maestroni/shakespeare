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

  private final Object[] mArgs;
  private final Thread mCallingThread;
  private final String mFormat;
  private final Locale mLocale;
  private final String mMessage;
  private final Throwable mThrowable;

  /**
   * Creates a new log message with the specified formatted message.
   *
   * @param locale    the message default locale.
   * @param throwable the optional throwable instance.
   * @param message   the pre-formatted message.
   */
  LogMessage(@NotNull final Locale locale, @Nullable final Throwable throwable,
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
  LogMessage(@NotNull final Locale locale, @Nullable final Throwable throwable,
      @NotNull final String format, @Nullable final Object... args) {
    this(locale, throwable, null, format, args);
  }

  private LogMessage(@NotNull final Locale locale, @Nullable final Throwable throwable,
      @Nullable final String message, @Nullable final String format,
      @Nullable final Object... args) {
    mLocale = locale;
    mThrowable = throwable;
    mMessage = message;
    mFormat = format;
    mArgs = args;
    mCallingThread = Thread.currentThread();
  }

  /**
   * Abbreviates the specified message so that its size does not exceed a maximum size.<br>
   * The returned string will terminate with "..." when the original message length is greater than
   * the specified maximum size.<br>
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
   * @param locale         the format locale.
   * @param format         the format string.
   * @param maxMessageSize the maximum size of the text message.
   * @return the formatted log message.
   */
  @Nullable
  public String formatLogMessage(@NotNull final Locale locale, @NotNull final String format,
      final int maxMessageSize) {
    return String.format(locale, format, mCallingThread,
        abbreviate(formatTextMessage(locale), maxMessageSize), printStackTrace());
  }

  /**
   * Formats this log message by using the specified locale.<br>
   * The arguments passed to the format will be, in the order: the calling thread, the (optionally
   * abbreviated) text message, the stack trace of the logged throwable (if any), the specified
   * additional arguments.
   *
   * @param locale         the format locale.
   * @param format         the format string.
   * @param maxMessageSize the maximum size of the text message.
   * @param additionalArgs the list of additional arguments.
   * @return the formatted log message.
   */
  @Nullable
  public String formatLogMessage(@NotNull final Locale locale, @NotNull final String format,
      final int maxMessageSize, @Nullable final Object... additionalArgs) {
    if ((additionalArgs == null) || (additionalArgs.length == 0)) {
      return formatLogMessage(locale, format, maxMessageSize);
    }
    final int length = additionalArgs.length;
    final Object[] args = new Object[3 + length];
    args[0] = mCallingThread;
    args[1] = abbreviate(formatTextMessage(locale), maxMessageSize);
    args[2] = printStackTrace();
    System.arraycopy(additionalArgs, 0, args, 3, length);
    return String.format(locale, format, args);
  }

  /**
   * Formats this log message by using the logger locale.<br>
   * The arguments passed to the format will be, in the order: the calling thread, the (optionally
   * abbreviated) text message, the stack trace of the logged throwable (if any).
   *
   * @param format         the format string.
   * @param maxMessageSize the maximum size of the text message.
   * @return the formatted log message.
   */
  @Nullable
  public String formatLogMessage(@NotNull final String format, final int maxMessageSize) {
    return formatLogMessage(mLocale, format, maxMessageSize);
  }

  /**
   * Formats this log message by using the logger locale.<br>
   * The arguments passed to the format will be, in the order: the calling thread, the (optionally
   * abbreviated) text message, the stack trace of the logged throwable (if any), the specified
   * additional arguments.
   *
   * @param format         the format string.
   * @param maxMessageSize the maximum size of the text message.
   * @param additionalArgs the list of additional arguments.
   * @return the formatted log message.
   */
  @Nullable
  public String formatLogMessage(@NotNull final String format, final int maxMessageSize,
      @Nullable final Object... additionalArgs) {
    return formatLogMessage(mLocale, format, maxMessageSize, additionalArgs);
  }

  /**
   * Formats the text message initialized in the constructor by using the logger locale.
   *
   * @return the formatted message or {@code null}.
   */
  @Nullable
  public String formatTextMessage() {
    return formatTextMessage(mLocale);
  }

  /**
   * Formats the text message initialized in the constructor by using the specified locale.
   *
   * @param locale the format locale.
   * @return the formatted message or {@code null}.
   */
  @Nullable
  public String formatTextMessage(@NotNull final Locale locale) {
    final String format = mFormat;
    return (format != null) ? String.format(locale, format, mArgs) : mMessage;
  }

  /**
   * Returns a copy of the text format arguments.
   *
   * @return the arguments or {@code null}.
   */
  @Nullable
  public Object[] getArgs() {
    final Object[] args = mArgs;
    return (args != null) ? args.clone() : null;
  }

  /**
   * Returns the logger calling thread.
   *
   * @return the thread instance.
   */
  @NotNull
  public Thread getCallingThread() {
    return mCallingThread;
  }

  /**
   * Returns the text format.
   *
   * @return the format or {@code null}.
   */
  @Nullable
  public String getFormat() {
    return mFormat;
  }

  /**
   * Returns the logger locale.
   *
   * @return the locale instance.
   */
  @NotNull
  public Locale getLocale() {
    return mLocale;
  }

  /**
   * Returns the pre-formatted text message.
   *
   * @return the message or {@code null}.
   */
  @Nullable
  public String getMessage() {
    return mMessage;
  }

  /**
   * Returns the optional throwable.
   *
   * @return the throwable or {@code null}.
   */
  @Nullable
  public Throwable getThrowable() {
    return mThrowable;
  }

  /**
   * Prints the logged throwable stack trace into a string.<br>
   * If no throwable was set {@code null} will be returned.
   *
   * @return the stack trace or {@code null}.
   */
  @Nullable
  public String printStackTrace() {
    final Throwable throwable = mThrowable;
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
