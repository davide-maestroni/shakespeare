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

import java.util.Collection;
import java.util.HashSet;

import dm.shakespeare.util.ConstantConditions;

/**
 * Implementation of a {@code LogPrinter} broadcasting the log messages to a list of printer
 * instances.
 */
class MultiPrinter implements LogPrinter {

  private final HashSet<LogPrinter> printers;

  /**
   * Creates a new broadcasting printer.
   *
   * @param printers the log printers which will receive the broadcast messages.
   */
  MultiPrinter(@NotNull final Collection<? extends LogPrinter> printers) {
    this.printers =
        new HashSet<LogPrinter>(ConstantConditions.notNullElements("printers", printers));
  }

  public boolean canLogDbg() {
    for (final LogPrinter printer : printers) {
      if (printer.canLogDbg()) {
        return true;
      }
    }
    return false;
  }

  public boolean canLogErr() {
    for (final LogPrinter printer : printers) {
      if (printer.canLogErr()) {
        return true;
      }
    }
    return false;
  }

  public boolean canLogWrn() {
    for (final LogPrinter printer : printers) {
      if (printer.canLogWrn()) {
        return true;
      }
    }
    return false;
  }

  public void dbg(@NotNull final LogMessage message) {
    for (final LogPrinter printer : printers) {
      printer.dbg(message);
    }
  }

  public void err(@NotNull final LogMessage message) {
    for (final LogPrinter printer : printers) {
      printer.err(message);
    }
  }

  public void wrn(@NotNull final LogMessage message) {
    for (final LogPrinter printer : printers) {
      printer.wrn(message);
    }
  }
}
