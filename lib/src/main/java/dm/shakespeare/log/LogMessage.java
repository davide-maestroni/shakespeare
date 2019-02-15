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

  private static final int DEFAULT_MAX_SIZE = 1024;

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
    mArgs = args;
    mCallingThread = Thread.currentThread();
  }

  @Nullable
  public static String abbreviate(@Nullable final String message, final int maxSize) {
    return (message != null) ?
        message.substring(0, Math.min(message.length(), Math.max(maxSize - 3, 0))) + "..." : null;
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
  public String formatMessage() {
    return formatMessage(mLocale);
  }

  @Nullable
  public String formatMessage(@NotNull final Locale locale) {
    final String format = mFormat;
    return (format != null) ? String.format(locale, format, mArgs) : mMessage;
  }

  @Nullable
  public String formatMessage(@NotNull final String format) {
    return formatMessage(mLocale, format);
  }

  @Nullable
  public String formatMessage(@NotNull final Locale locale, @NotNull final String format) {
    return String.format(locale, format, mCallingThread,
        abbreviate(formatMessage(locale), DEFAULT_MAX_SIZE), printStackTrace());
  }

  @Nullable
  public Object[] getArgs() {
    return mArgs;
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
    return formatMessage();
  }
}
