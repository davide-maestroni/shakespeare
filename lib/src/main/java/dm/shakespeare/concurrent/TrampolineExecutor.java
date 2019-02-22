package dm.shakespeare.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 05/28/2018.
 */
class TrampolineExecutor extends AbstractExecutorService {

  private static final TrampolineExecutor sInstance = new TrampolineExecutor();

  /**
   * Avoid explicit instantiation.
   */
  private TrampolineExecutor() {
  }

  @NotNull
  static TrampolineExecutor defaultInstance() {
    return sInstance;
  }

  public void execute(@NotNull final Runnable command) {
    LocalExecutor.run(ConstantConditions.notNull("command", command));
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
