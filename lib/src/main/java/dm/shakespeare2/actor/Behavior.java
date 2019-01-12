package dm.shakespeare2.actor;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import dm.shakespeare.log.Logger;

/**
 * Created by davide-maestroni on 09/10/2018.
 */
public interface Behavior {

  void onMessage(Object message, @NotNull Envelop envelop, @NotNull Context context) throws
      Exception;

  void onStart(@NotNull Context context) throws Exception;

  void onStop(@NotNull Context context) throws Exception;

  interface Context {

    void dismissSelf();

    @NotNull
    ExecutorService getExecutor();

    @NotNull
    Logger getLogger();

    @NotNull
    ScheduledExecutorService getScheduledExecutor();

    @NotNull
    Actor getSelf();

    boolean isDismissed();

    void restartSelf();

    void setBehavior(@NotNull Behavior behavior);
  }
}
