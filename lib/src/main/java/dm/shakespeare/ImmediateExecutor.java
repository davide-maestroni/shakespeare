package dm.shakespeare;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.log.LogPrinters;
import dm.shakespeare.log.Logger;

/**
 * Created by davide-maestroni on 06/19/2018.
 */
class ImmediateExecutor extends AbstractExecutorService {

  private static final ImmediateExecutor sInstance = new ImmediateExecutor();
  private static final Logger sLogger =
      Logger.newLogger(LogPrinters.javaLoggingPrinter(ImmediateExecutor.class.getName()));

  /**
   * Avoid explicit instantiation.
   */
  private ImmediateExecutor() {
  }

  @NotNull
  static ImmediateExecutor defaultInstance() {
    return sInstance;
  }

  public void execute(@NotNull final Runnable runnable) {
    try {
      runnable.run();

    } catch (final Throwable t) {
      if (Thread.interrupted()) {
        throw new RuntimeException(t);
      }

      sLogger.wrn(t, "Suppressed exception");
    }
  }

  public void shutdown() {
  }

  @NotNull
  public List<Runnable> shutdownNow() {
    return Collections.emptyList();
  }

  public boolean isShutdown() {
    return false;
  }

  public boolean isTerminated() {
    return false;
  }

  public boolean awaitTermination(final long timeout, @NotNull final TimeUnit unit) throws
      InterruptedException {
    Thread.sleep(unit.toMillis(timeout));
    return false;
  }
}
