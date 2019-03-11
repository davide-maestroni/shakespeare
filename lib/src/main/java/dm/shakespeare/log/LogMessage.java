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
 * Created by davide-maestroni on 02/15/2019.
 */
public class LogMessage {

  private static final Object[] NO_ARGS = new Object[0];

  private final Object[] mArgs;
  private final Thread mCallingThread;
  private final String mFormat;
  private final Locale mLocale;
  private final String mMessage;
  private final Throwable mThrowable;

  LogMessage(@NotNull final Locale locale, @Nullable final Throwable throwable,
      @Nullable final String message) {
    this(locale, throwable, message, null, (Object[]) null);
  }

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
    mArgs = (args != null) ? args : NO_ARGS;
    mCallingThread = Thread.currentThread();
  }

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

  @Nullable
  public String formatLogMessage(@NotNull final Locale locale, @NotNull final String format,
      final int maxMessageSize) {
    return String.format(locale, format, mCallingThread,
        abbreviate(formatMessage(locale), maxMessageSize), printStackTrace());
  }

  @Nullable
  public String formatLogMessage(@NotNull final Locale locale, @NotNull final String format,
      final int maxMessageSize, @Nullable final Object... additionalArgs) {
    if ((additionalArgs == null) || (additionalArgs.length == 0)) {
      return formatLogMessage(locale, format, maxMessageSize);
    }
    final int length = additionalArgs.length;
    final Object[] args = new Object[3 + length];
    args[0] = mCallingThread;
    args[1] = abbreviate(formatMessage(locale), maxMessageSize);
    args[2] = printStackTrace();
    System.arraycopy(additionalArgs, 0, args, 3, length);
    return String.format(locale, format, args);
  }

  @Nullable
  public String formatLogMessage(@NotNull final String format, final int maxMessageSize) {
    return formatLogMessage(mLocale, format, maxMessageSize);
  }

  @Nullable
  public String formatLogMessage(@NotNull final String format, final int maxMessageSize,
      @Nullable final Object... additionalArgs) {
    return formatLogMessage(mLocale, format, maxMessageSize, additionalArgs);
  }

  @Nullable
  public String formatMessage() {
    return formatMessage(mLocale);
  }

  @Nullable
  public String formatMessage(@NotNull final Locale locale) {
    final String format = mFormat;
    return (format != null) ? String.format(locale, format, mArgs) : mMessage;
  }

  @Nullable
  public Object[] getArgs() {
    return mArgs.clone();
  }

  @NotNull
  public Thread getCallingThread() {
    return mCallingThread;
  }

  @Nullable
  public String getFormat() {
    return mFormat;
  }

  @NotNull
  public Locale getLocale() {
    return mLocale;
  }

  @Nullable
  public String getMessage() {
    return mMessage;
  }

  @Nullable
  public Throwable getThrowable() {
    return mThrowable;
  }

  @Nullable
  public String printStackTrace() {
    final Throwable throwable = mThrowable;
    return (throwable != null) ? printStackTrace(throwable) : null;
  }

  @Override
  public String toString() {
    final String message = formatMessage();
    return (message != null) ? message : "null";
  }
}
