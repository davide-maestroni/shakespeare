package dm.shakespeare.executor;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

/**
 * Created by davide-maestroni on 01/15/2019.
 */
public interface QueuedExecutorService extends ExecutorService {

  void executeNext(@NotNull Runnable command);
}
