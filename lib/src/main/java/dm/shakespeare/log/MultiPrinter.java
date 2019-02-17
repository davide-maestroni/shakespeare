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
