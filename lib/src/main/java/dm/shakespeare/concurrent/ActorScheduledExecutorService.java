package dm.shakespeare.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by davide-maestroni on 01/13/2019.
 */
public class ActorScheduledExecutorService extends ThrottledScheduledExecutorService {

  ActorScheduledExecutorService(@NotNull final ScheduledExecutorService executor) {
    super(executor, 1);
  }

  @Override
  public void executeNext(@NotNull final Runnable command) {
    super.executeNext(command);
  }
}
