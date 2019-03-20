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

import org.junit.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link LogMessage} unit tests.
 */
public class LogMessageTest {

  @Test
  public void abbreviate() {
    assertThat(LogMessage.abbreviate("this is a test", 5)).isEqualTo("th...");
  }

  @Test
  public void abbreviateMin() {
    assertThat(LogMessage.abbreviate("this is a test", 2)).isEqualTo("...");
  }

  @Test
  public void abbreviateNull() {
    assertThat(LogMessage.abbreviate(null, 5)).isNull();
  }

  @Test
  public void formatLogMessage() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a test");
    assertThat(message.formatLogMessage("%2$s", 5)).isEqualTo("th...");
  }

  @Test
  public void formatLogMessageArgs() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a test");
    assertThat(message.formatLogMessage("%2$s - %4$s", 5, "test")).isEqualTo("th... - test");
  }

  @Test
  public void formatLogMessageLocale() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a test");
    assertThat(message.formatLogMessage(Locale.CANADA_FRENCH, "%2$s", 5)).isEqualTo("th...");
  }

  @Test
  public void formatLogMessageLocaleArgs() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a test");
    assertThat(message.formatLogMessage(Locale.CANADA_FRENCH, "%2$s - %4$s", 5, "test")).isEqualTo(
        "th... - test");
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void formatLogMessageNPE() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a test");
    message.formatLogMessage(null, 5);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void formatLogMessageNPEArgs() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a test");
    message.formatLogMessage(null, 5, "test");
  }

  @Test
  public void formatLogMessageNoArgs() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a test");
    assertThat(message.formatLogMessage("%2$s", 5, new Object[0])).isEqualTo("th...");
  }

  @Test
  public void formatLogMessageNullArgs() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a test");
    assertThat(message.formatLogMessage("%2$s", 5, (Object[]) null)).isEqualTo("th...");
  }

  @Test
  public void formatLogMessageStack() {
    final LogMessage message =
        new LogMessage(Locale.ENGLISH, new NumberFormatException(), "this is a test");
    assertThat(message.formatLogMessage("%3$s", 5)).contains(
        NumberFormatException.class.getSimpleName());
  }

  @Test
  public void formatLogMessageStackArgs() {
    final LogMessage message =
        new LogMessage(Locale.ENGLISH, new NumberFormatException(), "this is a test");
    assertThat(message.formatLogMessage("%3$s - %4$s", 5, "test")).contains(
        NumberFormatException.class.getSimpleName(), "test");
  }

  @Test
  public void formatLogMessageStackLocale() {
    final LogMessage message =
        new LogMessage(Locale.ENGLISH, new NumberFormatException(), "this is a test");
    assertThat(message.formatLogMessage(Locale.CHINA, "%3$s", 5)).contains(
        NumberFormatException.class.getSimpleName());
  }

  @Test
  public void formatLogMessageStackLocaleArgs() {
    final LogMessage message =
        new LogMessage(Locale.ENGLISH, new NumberFormatException(), "this is a test");
    assertThat(message.formatLogMessage(Locale.CHINA, "%3$s - %4$s", 5, "test")).contains(
        NumberFormatException.class.getSimpleName(), "test");
  }

  @Test
  public void formatLogMessageThread() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a test");
    assertThat(message.formatLogMessage("%1$s", 5)).contains(Thread.currentThread().getName());
  }

  @Test
  public void formatLogMessageThreadArgs() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a test");
    assertThat(message.formatLogMessage("%1$s - %4$s", 5, "test")).contains(
        Thread.currentThread().getName(), "test");
  }

  @Test
  public void formatLogMessageThreadLocale() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a test");
    assertThat(message.formatLogMessage(null, "%1$s", 5)).contains(
        Thread.currentThread().getName());
  }

  @Test
  public void formatLogMessageThreadLocaleArgs() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a test");
    assertThat(message.formatLogMessage(null, "%1$s - %4$s", 5, "test")).contains(
        Thread.currentThread().getName(), "test");
  }

  @Test
  public void formatTextMessage() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a %s", "test");
    assertThat(message.formatTextMessage()).isEqualTo("this is a test");
  }

  @Test
  public void formatTextMessageLocale() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a %s", "test");
    assertThat(message.formatTextMessage(Locale.CANADA_FRENCH)).isEqualTo("this is a test");
  }

  @Test
  public void formatTextMessageNoFormat() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a test");
    assertThat(message.formatTextMessage()).isEqualTo("this is a test");
  }

  @Test
  public void formatTextMessageNoFormatLocale() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a test");
    assertThat(message.formatTextMessage(null)).isEqualTo("this is a test");
  }

  @Test
  public void getArgs() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a %s", "test");
    assertThat(message.getArgs()).containsExactly("test");
  }

  @Test
  public void getArgsNull() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a test");
    assertThat(message.getArgs()).isNull();
  }

  @Test
  public void getCallingThread() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a test");
    assertThat(message.getCallingThread()).isEqualTo(Thread.currentThread());
  }

  @Test
  public void getFormat() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a %s", "test");
    assertThat(message.getFormat()).isEqualTo("this is a %s");
  }

  @Test
  public void getFormatNull() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a test");
    assertThat(message.getFormat()).isNull();
  }

  @Test
  public void getLocale() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a test");
    assertThat(message.getLocale()).isEqualTo(Locale.ENGLISH);
  }

  @Test
  public void getLocaleNull() {
    final LogMessage message = new LogMessage(null, null, "this is a test");
    assertThat(message.getLocale()).isNull();
  }

  @Test
  public void getMessage() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a test");
    assertThat(message.getMessage()).isEqualTo("this is a test");
  }

  @Test
  public void getMessageNull() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a %s", "test");
    assertThat(message.getMessage()).isNull();
  }

  @Test
  public void getThrowable() {
    final NumberFormatException exception = new NumberFormatException();
    final LogMessage message = new LogMessage(Locale.ENGLISH, exception, "this is a test");
    assertThat(message.getThrowable()).isSameAs(exception);
  }

  @Test
  public void getThrowableNull() {
    final LogMessage message = new LogMessage(null, null, "this is a test");
    assertThat(message.getLocale()).isNull();
  }

  @Test
  public void printStackTrace() {
    assertThat(LogMessage.printStackTrace(new NumberFormatException("test"))).contains(
        NumberFormatException.class.getSimpleName(), "test");
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void printStackTraceNPE() {
    LogMessage.printStackTrace(null);
  }

  @Test
  public void printStackTraceNull() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a test");
    assertThat(message.printStackTrace()).isNull();
  }

  @Test
  public void printStackTraceThrowable() {
    final NumberFormatException exception = new NumberFormatException();
    final LogMessage message = new LogMessage(Locale.ENGLISH, exception, "this is a test");
    assertThat(message.printStackTrace()).contains(NumberFormatException.class.getSimpleName());
  }

  @Test
  public void toStringFormat() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a %s", "test");
    assertThat(message.toString()).isEqualTo("this is a test");
  }

  @Test
  public void toStringNull() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, null);
    assertThat(message.toString()).isEqualTo("null");
  }

  @Test
  public void toStringTest() {
    final LogMessage message = new LogMessage(Locale.ENGLISH, null, "this is a test");
    assertThat(message.toString()).isEqualTo("this is a test");
  }
}
