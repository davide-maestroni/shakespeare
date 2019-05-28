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

import java.util.logging.Handler;
import java.util.logging.Level;

import dm.shakespeare.util.ConstantConditions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link LogPrinters} unit tests.
 */
public class LogPrintersTest {

  private static final String[] ARGS = new String[]{"test1", "test2", "test3", "test4", "test5"};
  private static final String FORMAT0 = "0: %s";
  private static final String FORMAT1 = "0: %s - 1: %s";
  private static final String FORMAT2 = "0: %s - 1: %s - 2: %s";
  private static final String FORMAT3 = "0: %s - 1: %s - 2: %s - 3: %s";
  private static final String FORMAT4 = "0: %s - 1: %s - 2: %s - 3: %s - 4: %s";

  @Test
  public void javaLogDbg() {
    final NullPointerException ex = new NullPointerException();
    final Logger logger = new Logger(LogPrinters.javaLoggingPrinter("test"));
    final java.util.logging.Logger loggerParent =
        java.util.logging.Logger.getLogger("test").getParent();
    for (final Handler handler : loggerParent.getHandlers()) {
      handler.setLevel(Level.ALL);
    }
    loggerParent.setLevel(Level.ALL);
    logger.dbg(ARGS[0]);
    logger.dbg(FORMAT0, ARGS[0]);
    logger.dbg(FORMAT1, ARGS[0], ARGS[1]);
    logger.dbg(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    logger.dbg(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    logger.dbg(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    logger.dbg(ex);
    logger.dbg(ex, ARGS[0]);
    logger.dbg(ex, FORMAT0, ARGS[0]);
    logger.dbg(ex, FORMAT1, ARGS[0], ARGS[1]);
    logger.dbg(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    logger.dbg(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    logger.dbg(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
  }

  @Test
  public void javaLogErr() {
    final NullPointerException ex = new NullPointerException();
    final Logger logger = new Logger(LogPrinters.javaLoggingPrinter("test", 100, 200));
    logger.err(ARGS[0]);
    logger.err(FORMAT0, ARGS[0]);
    logger.err(FORMAT1, ARGS[0], ARGS[1]);
    logger.err(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    logger.err(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    logger.err(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    logger.err(ex);
    logger.err(ex, ARGS[0]);
    logger.err(ex, FORMAT0, ARGS[0]);
    logger.err(ex, FORMAT1, ARGS[0], ARGS[1]);
    logger.err(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    logger.err(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    logger.err(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
  }

  @Test
  public void javaLogWrn() {
    final NullPointerException ex = new NullPointerException();
    final Logger logger = new Logger(LogPrinters.javaLoggingPrinter("test"));
    logger.wrn(ARGS[0]);
    logger.wrn(FORMAT0, ARGS[0]);
    logger.wrn(FORMAT1, ARGS[0], ARGS[1]);
    logger.wrn(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    logger.wrn(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    logger.wrn(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    logger.wrn(ex);
    logger.wrn(ex, ARGS[0]);
    logger.wrn(ex, FORMAT0, ARGS[0]);
    logger.wrn(ex, FORMAT1, ARGS[0], ARGS[1]);
    logger.wrn(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    logger.wrn(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    logger.wrn(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
  }

  @Test
  public void mergeCannotLogDbg() {
    final NullPointerException ex = new NullPointerException();
    final Logger logger = new Logger(LogPrinters.mergePrinters(new NoLogPrinter()));
    logger.dbg(ARGS[0]);
    logger.dbg(FORMAT0, ARGS[0]);
    logger.dbg(FORMAT1, ARGS[0], ARGS[1]);
    logger.dbg(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    logger.dbg(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    logger.dbg(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    logger.dbg(ex);
    logger.dbg(ex, ARGS[0]);
    logger.dbg(ex, FORMAT0, ARGS[0]);
    logger.dbg(ex, FORMAT1, ARGS[0], ARGS[1]);
    logger.dbg(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    logger.dbg(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    logger.dbg(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
  }

  @Test
  public void mergeCannotLogErr() {
    final NullPointerException ex = new NullPointerException();
    final Logger logger = new Logger(LogPrinters.mergePrinters(new NoLogPrinter()));
    logger.err(ARGS[0]);
    logger.err(FORMAT0, ARGS[0]);
    logger.err(FORMAT1, ARGS[0], ARGS[1]);
    logger.err(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    logger.err(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    logger.err(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    logger.err(ex);
    logger.err(ex, ARGS[0]);
    logger.err(ex, FORMAT0, ARGS[0]);
    logger.err(ex, FORMAT1, ARGS[0], ARGS[1]);
    logger.err(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    logger.err(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    logger.err(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
  }

  @Test
  public void mergeCannotLogWrn() {
    final NullPointerException ex = new NullPointerException();
    final Logger logger = new Logger(LogPrinters.mergePrinters(new NoLogPrinter()));
    logger.wrn(ARGS[0]);
    logger.wrn(FORMAT0, ARGS[0]);
    logger.wrn(FORMAT1, ARGS[0], ARGS[1]);
    logger.wrn(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    logger.wrn(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    logger.wrn(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    logger.wrn(ex);
    logger.wrn(ex, ARGS[0]);
    logger.wrn(ex, FORMAT0, ARGS[0]);
    logger.wrn(ex, FORMAT1, ARGS[0], ARGS[1]);
    logger.wrn(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    logger.wrn(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    logger.wrn(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
  }

  @Test
  public void mergeLogDbg() {
    final NullPointerException ex = new NullPointerException();
    final TestLogPrinter testPrinter = new TestLogPrinter();
    final Logger logger =
        new Logger(LogPrinters.mergePrinters(LogPrinters.javaLoggingPrinter("test"), testPrinter));
    logger.dbg(ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0]);
    logger.dbg(FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0]);
    logger.dbg(FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0], ARGS[1]);
    logger.dbg(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0], ARGS[1], ARGS[2]);
    logger.dbg(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    logger.dbg(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    logger.dbg(ex);
    assertThat(testPrinter.getMessage()).contains(NullPointerException.class.getSimpleName());
    logger.dbg(ex, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0])
        .contains(NullPointerException.class.getSimpleName());
    logger.dbg(ex, FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0])
        .contains(NullPointerException.class.getSimpleName());
    logger.dbg(ex, FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0], ARGS[1])
        .contains(NullPointerException.class.getSimpleName());
    logger.dbg(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0], ARGS[1], ARGS[2])
        .contains(NullPointerException.class.getSimpleName());
    logger.dbg(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0], ARGS[1], ARGS[2], ARGS[3])
        .contains(NullPointerException.class.getSimpleName());
    logger.dbg(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4])
        .contains(NullPointerException.class.getSimpleName());
  }

  @Test
  public void mergeLogErr() {
    final NullPointerException ex = new NullPointerException();
    final TestLogPrinter testPrinter = new TestLogPrinter();
    final Logger logger =
        new Logger(LogPrinters.mergePrinters(LogPrinters.javaLoggingPrinter("test"), testPrinter));
    logger.err(ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0]);
    logger.err(FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0]);
    logger.err(FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0], ARGS[1]);
    logger.err(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0], ARGS[1], ARGS[2]);
    logger.err(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    logger.err(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    logger.err(ex);
    assertThat(testPrinter.getMessage()).contains(NullPointerException.class.getSimpleName());
    logger.err(ex, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0])
        .contains(NullPointerException.class.getSimpleName());
    logger.err(ex, FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0])
        .contains(NullPointerException.class.getSimpleName());
    logger.err(ex, FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0], ARGS[1])
        .contains(NullPointerException.class.getSimpleName());
    logger.err(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0], ARGS[1], ARGS[2])
        .contains(NullPointerException.class.getSimpleName());
    logger.err(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0], ARGS[1], ARGS[2], ARGS[3])
        .contains(NullPointerException.class.getSimpleName());
    logger.err(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4])
        .contains(NullPointerException.class.getSimpleName());
  }

  @Test
  public void mergeLogWrn() {
    final NullPointerException ex = new NullPointerException();
    final TestLogPrinter testPrinter = new TestLogPrinter();
    final Logger logger =
        new Logger(LogPrinters.mergePrinters(LogPrinters.javaLoggingPrinter("test"), testPrinter));
    logger.wrn(ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0]);
    logger.wrn(FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0]);
    logger.wrn(FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0], ARGS[1]);
    logger.wrn(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0], ARGS[1], ARGS[2]);
    logger.wrn(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    logger.wrn(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    logger.wrn(ex);
    assertThat(testPrinter.getMessage()).contains(NullPointerException.class.getSimpleName());
    logger.wrn(ex, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0])
        .contains(NullPointerException.class.getSimpleName());
    logger.wrn(ex, FORMAT0, ARGS[0]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0])
        .contains(NullPointerException.class.getSimpleName());
    logger.wrn(ex, FORMAT1, ARGS[0], ARGS[1]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0], ARGS[1])
        .contains(NullPointerException.class.getSimpleName());
    logger.wrn(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0], ARGS[1], ARGS[2])
        .contains(NullPointerException.class.getSimpleName());
    logger.wrn(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0], ARGS[1], ARGS[2], ARGS[3])
        .contains(NullPointerException.class.getSimpleName());
    logger.wrn(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
    assertThat(testPrinter.getMessage()).contains(ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4])
        .contains(NullPointerException.class.getSimpleName());
  }

  private static class NoLogPrinter implements LogPrinter {

    public boolean canLogDbg() {
      return false;
    }

    public boolean canLogErr() {
      return false;
    }

    public boolean canLogWrn() {
      return false;
    }

    public void dbg(@NotNull final LogMessage message) {
      ConstantConditions.unsupported();
    }

    public void err(@NotNull final LogMessage message) {
      ConstantConditions.unsupported();
    }

    public void wrn(@NotNull final LogMessage message) {
      ConstantConditions.unsupported();
    }
  }

  private static class TestLogPrinter implements LogPrinter {

    private String message;

    public boolean canLogDbg() {
      return true;
    }

    public boolean canLogErr() {
      return true;
    }

    public boolean canLogWrn() {
      return true;
    }

    public void dbg(@NotNull final LogMessage message) {
      this.message = message.formatLogMessage("%2$s - %3$s", 1000);
    }

    public void err(@NotNull final LogMessage message) {
      this.message = message.formatLogMessage("%2$s - %3$s", 1000);
    }

    public void wrn(@NotNull final LogMessage message) {
      this.message = message.formatLogMessage("%2$s - %3$s", 1000);
    }

    String getMessage() {
      final String message = this.message;
      this.message = null;
      return message;
    }
  }
}
