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
import org.junit.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Logger} unit tests.
 */
public class LoggerTest {

  private static final String[] ARGS = new String[]{"test1", "test2", "test3", "test4", "test5"};
  private static final String FORMAT0 = "0: %s";
  private static final String FORMAT1 = "0: %s - 1: %s";
  private static final String FORMAT2 = "0: %s - 1: %s - 2: %s";
  private static final String FORMAT3 = "0: %s - 1: %s - 2: %s - 3: %s";
  private static final String FORMAT4 = "0: %s - 1: %s - 2: %s - 3: %s - 4: %s";

  @Test
  public void debug() {
    final NullPointerException ex = new NullPointerException();
    final TestLogPrinter testPrinter = new TestLogPrinter(Level.DEBUG);
    final Logger logger = Logger.newLogger(testPrinter);

    assertThat(testPrinter.canLogDbg()).isTrue();
    assertThat(testPrinter.canLogWrn()).isTrue();
    assertThat(testPrinter.canLogErr()).isTrue();

    // - DBG
    logger.dbg(ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0]);

    logger.dbg(FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT0, ARGS[0]));

    logger.dbg(FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT1, ARGS[0], ARGS[1]));

    logger.dbg(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT2, ARGS[0], ARGS[1], ARGS[2]));

    logger.dbg(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]));

    logger.dbg(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]));

    logger.dbg(ex);
    assertThat(testPrinter.getMessage()).contains(NullPointerException.class.getSimpleName());

    logger.dbg(ex, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0])
        .contains(NullPointerException.class.getSimpleName());

    logger.dbg(ex, FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT0, ARGS[0]))
        .contains(NullPointerException.class.getSimpleName());

    logger.dbg(ex, FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT1, ARGS[0], ARGS[1]))
        .contains(NullPointerException.class.getSimpleName());

    logger.dbg(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT2, ARGS[0], ARGS[1], ARGS[2]))
        .contains(NullPointerException.class.getSimpleName());

    logger.dbg(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]))
        .contains(NullPointerException.class.getSimpleName());

    logger.dbg(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]))
        .contains(NullPointerException.class.getSimpleName());

    // - WRN
    logger.wrn(ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0]);

    logger.wrn(FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT0, ARGS[0]));

    logger.wrn(FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT1, ARGS[0], ARGS[1]));

    logger.wrn(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT2, ARGS[0], ARGS[1], ARGS[2]));

    logger.wrn(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]));

    logger.wrn(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]));

    logger.wrn(ex);
    assertThat(testPrinter.getMessage()).contains(NullPointerException.class.getSimpleName());

    logger.wrn(ex, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0])
        .contains(NullPointerException.class.getSimpleName());

    logger.wrn(ex, FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT0, ARGS[0]))
        .contains(NullPointerException.class.getSimpleName());

    logger.wrn(ex, FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT1, ARGS[0], ARGS[1]))
        .contains(NullPointerException.class.getSimpleName());

    logger.wrn(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT2, ARGS[0], ARGS[1], ARGS[2]))
        .contains(NullPointerException.class.getSimpleName());

    logger.wrn(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]))
        .contains(NullPointerException.class.getSimpleName());

    logger.wrn(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]))
        .contains(NullPointerException.class.getSimpleName());

    // - ERR
    logger.err(ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0]);

    logger.err(FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT0, ARGS[0]));

    logger.err(FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT1, ARGS[0], ARGS[1]));

    logger.err(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT2, ARGS[0], ARGS[1], ARGS[2]));

    logger.err(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]));

    logger.err(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]));

    logger.err(ex);
    assertThat(testPrinter.getMessage()).contains(NullPointerException.class.getSimpleName());

    logger.err(ex, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0])
        .contains(NullPointerException.class.getSimpleName());

    logger.err(ex, FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT0, ARGS[0]))
        .contains(NullPointerException.class.getSimpleName());

    logger.err(ex, FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT1, ARGS[0], ARGS[1]))
        .contains(NullPointerException.class.getSimpleName());

    logger.err(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT2, ARGS[0], ARGS[1], ARGS[2]))
        .contains(NullPointerException.class.getSimpleName());

    logger.err(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]))
        .contains(NullPointerException.class.getSimpleName());

    logger.err(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]))
        .contains(NullPointerException.class.getSimpleName());
  }

  @Test
  public void error() {
    final NullPointerException ex = new NullPointerException();
    final TestLogPrinter testPrinter = new TestLogPrinter(Level.ERROR);
    final Logger logger = Logger.newLogger(testPrinter);

    assertThat(testPrinter.canLogDbg()).isFalse();
    assertThat(testPrinter.canLogWrn()).isFalse();
    assertThat(testPrinter.canLogErr()).isTrue();

    // - DBG
    logger.dbg(ARGS[0]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(ex);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(ex, ARGS[0]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(ex, FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(ex, FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).isNull();

    // - WRN
    logger.wrn(ARGS[0]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.wrn(FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.wrn(FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.wrn(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.wrn(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.wrn(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.wrn(ex);
    assertThat(testPrinter.getMessage()).isNull();

    logger.wrn(ex, ARGS[0]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.wrn(ex, FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.wrn(ex, FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.wrn(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.wrn(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.wrn(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).isNull();

    // - ERR
    logger.err(ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0]);

    logger.err(FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT0, ARGS[0]));

    logger.err(FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT1, ARGS[0], ARGS[1]));

    logger.err(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT2, ARGS[0], ARGS[1], ARGS[2]));

    logger.err(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]));

    logger.err(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]));

    logger.err(ex);
    assertThat(testPrinter.getMessage()).contains(NullPointerException.class.getSimpleName());

    logger.err(ex, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0])
        .contains(NullPointerException.class.getSimpleName());

    logger.err(ex, FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT0, ARGS[0]))
        .contains(NullPointerException.class.getSimpleName());

    logger.err(ex, FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT1, ARGS[0], ARGS[1]))
        .contains(NullPointerException.class.getSimpleName());

    logger.err(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT2, ARGS[0], ARGS[1], ARGS[2]))
        .contains(NullPointerException.class.getSimpleName());

    logger.err(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]))
        .contains(NullPointerException.class.getSimpleName());

    logger.err(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]))
        .contains(NullPointerException.class.getSimpleName());
  }

  @Test
  public void locale() {
    final TestLogPrinter testPrinter = new TestLogPrinter(Level.WARNING);
    final Logger logger = Logger.newLogger(testPrinter, Locale.CANADA_FRENCH);
    assertThat(logger.getLocale()).isEqualTo(Locale.CANADA_FRENCH);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void newLoggerLocaleNPE() {
    Logger.newLogger(LogPrinters.javaLoggingPrinter(""), null);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void newLoggerNPE() {
    Logger.newLogger(null);
  }

  @Test
  public void printer() {
    final TestLogPrinter testPrinter = new TestLogPrinter(Level.WARNING);
    final Logger logger = Logger.newLogger(testPrinter);
    assertThat(logger.getPrinter()).isSameAs(testPrinter);
    assertThat(logger.getLocale()).isEqualTo(Locale.ENGLISH);
  }

  @Test
  public void silence() {
    final NullPointerException ex = new NullPointerException();
    final TestLogPrinter testPrinter = new TestLogPrinter(Level.SILENCE);
    final Logger logger = Logger.newLogger(testPrinter);

    assertThat(testPrinter.canLogDbg()).isFalse();
    assertThat(testPrinter.canLogWrn()).isFalse();
    assertThat(testPrinter.canLogErr()).isFalse();

    // - DBG
    logger.dbg(ARGS[0]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(ex);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(ex, ARGS[0]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(ex, FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(ex, FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).isNull();

    // - WRN
    logger.wrn(ARGS[0]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.wrn(FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.wrn(FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.wrn(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.wrn(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.wrn(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.wrn(ex);
    assertThat(testPrinter.getMessage()).isNull();

    logger.wrn(ex, ARGS[0]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.wrn(ex, FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.wrn(ex, FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.wrn(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.wrn(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.wrn(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).isNull();

    // - ERR
    logger.err(ARGS[0]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.err(FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.err(FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.err(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.err(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.err(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.err(ex);
    assertThat(testPrinter.getMessage()).isNull();

    logger.err(ex, ARGS[0]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.err(ex, FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.err(ex, FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.err(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.err(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.err(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).isNull();
  }

  @Test
  public void warning() {
    final NullPointerException ex = new NullPointerException();
    final TestLogPrinter testPrinter = new TestLogPrinter(Level.WARNING);
    final Logger logger = Logger.newLogger(testPrinter);

    assertThat(testPrinter.canLogDbg()).isFalse();
    assertThat(testPrinter.canLogWrn()).isTrue();
    assertThat(testPrinter.canLogErr()).isTrue();

    // - DBG
    logger.dbg(ARGS[0]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(ex);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(ex, ARGS[0]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(ex, FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(ex, FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).isNull();

    logger.dbg(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).isNull();

    // - WRN
    logger.wrn(ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0]);

    logger.wrn(FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT0, ARGS[0]));

    logger.wrn(FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT1, ARGS[0], ARGS[1]));

    logger.wrn(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT2, ARGS[0], ARGS[1], ARGS[2]));

    logger.wrn(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]));

    logger.wrn(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]));

    logger.wrn(ex);
    assertThat(testPrinter.getMessage()).contains(NullPointerException.class.getSimpleName());

    logger.wrn(ex, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0])
        .contains(NullPointerException.class.getSimpleName());

    logger.wrn(ex, FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT0, ARGS[0]))
        .contains(NullPointerException.class.getSimpleName());

    logger.wrn(ex, FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT1, ARGS[0], ARGS[1]))
        .contains(NullPointerException.class.getSimpleName());

    logger.wrn(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT2, ARGS[0], ARGS[1], ARGS[2]))
        .contains(NullPointerException.class.getSimpleName());

    logger.wrn(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]))
        .contains(NullPointerException.class.getSimpleName());

    logger.wrn(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]))
        .contains(NullPointerException.class.getSimpleName());

    // - ERR
    logger.err(ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0]);

    logger.err(FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT0, ARGS[0]));

    logger.err(FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT1, ARGS[0], ARGS[1]));

    logger.err(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT2, ARGS[0], ARGS[1], ARGS[2]));

    logger.err(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]));

    logger.err(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]));

    logger.err(ex);
    assertThat(testPrinter.getMessage()).contains(NullPointerException.class.getSimpleName());

    logger.err(ex, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0])
        .contains(NullPointerException.class.getSimpleName());

    logger.err(ex, FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT0, ARGS[0]))
        .contains(NullPointerException.class.getSimpleName());

    logger.err(ex, FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT1, ARGS[0], ARGS[1]))
        .contains(NullPointerException.class.getSimpleName());

    logger.err(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).contains(String.format(FORMAT2, ARGS[0], ARGS[1], ARGS[2]))
        .contains(NullPointerException.class.getSimpleName());

    logger.err(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]))
        .contains(NullPointerException.class.getSimpleName());

    logger.err(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).contains(
        String.format(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]))
        .contains(NullPointerException.class.getSimpleName());
  }

  private enum Level {
    DEBUG, WARNING, ERROR, SILENCE
  }

  private static class TestLogPrinter implements LogPrinter {

    private final Level mLevel;

    private String mMessage;

    private TestLogPrinter(@NotNull final Level level) {
      mLevel = level;
    }

    public boolean canLogDbg() {
      return mLevel.compareTo(Level.DEBUG) <= 0;
    }

    public boolean canLogErr() {
      return mLevel.compareTo(Level.ERROR) <= 0;
    }

    public boolean canLogWrn() {
      return mLevel.compareTo(Level.WARNING) <= 0;
    }

    public void dbg(@NotNull final LogMessage message) {
      mMessage = message.formatLogMessage("%2$s - %3$s", 1000);
    }

    public void err(@NotNull final LogMessage message) {
      mMessage = message.formatLogMessage("%2$s - %3$s", 1000);
    }

    public void wrn(@NotNull final LogMessage message) {
      mMessage = message.formatLogMessage("%2$s - %3$s", 1000);
    }

    String getMessage() {
      final String message = mMessage;
      mMessage = null;
      return message;
    }
  }
}
