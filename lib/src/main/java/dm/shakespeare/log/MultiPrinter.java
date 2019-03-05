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
 * Created by davide-maestroni on 02/04/2019.
 */
class MultiPrinter implements LogPrinter {

  private final HashSet<LogPrinter> mPrinters;

  MultiPrinter(@NotNull final Collection<? extends LogPrinter> printers) {
    mPrinters = new HashSet<LogPrinter>(ConstantConditions.notNullElements("printers", printers));
  }

  public boolean canLogDbg() {
    for (final LogPrinter printer : mPrinters) {
      if (printer.canLogDbg()) {
        return true;
      }
    }
    return false;
  }

  public boolean canLogErr() {
    for (final LogPrinter printer : mPrinters) {
      if (printer.canLogErr()) {
        return true;
      }
    }
    return false;
  }

  public boolean canLogWrn() {
    for (final LogPrinter printer : mPrinters) {
      if (printer.canLogWrn()) {
        return true;
      }
    }
    return false;
  }

  public void dbg(@NotNull final LogMessage message) {
    for (final LogPrinter printer : mPrinters) {
      printer.dbg(message);
    }
  }

  public void err(@NotNull final LogMessage message) {
    for (final LogPrinter printer : mPrinters) {
      printer.err(message);
    }
  }

  public void wrn(@NotNull final LogMessage message) {
    for (final LogPrinter printer : mPrinters) {
      printer.wrn(message);
    }
  }
}
