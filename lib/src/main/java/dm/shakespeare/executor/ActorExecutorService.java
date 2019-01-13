package dm.shakespeare.executor;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

/**
 * Created by davide-maestroni on 01/13/2019.
 */
public class ActorExecutorService extends ThrottledExecutorService {

  ActorExecutorService(@NotNull final ExecutorService executor) {
    super(executor, 1);
  }

  @Override
  public void executeNext(@NotNull final Runnable command) {
    super.executeNext(command);
  }
}
