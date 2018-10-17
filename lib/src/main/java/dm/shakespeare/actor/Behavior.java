package dm.shakespeare.actor;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import dm.shakespeare.actor.Actor.Envelop;
import dm.shakespeare.log.Logger;

/**
 * Created by davide-maestroni on 09/10/2018.
 */
public interface Behavior {

  void message(Object message, @NotNull Envelop envelop, @NotNull Context context) throws Exception;

  void start(@NotNull Context context) throws Exception;

  void stop(@NotNull Context context) throws Exception;

  interface Context {

    @NotNull
    ExecutorService getExecutor();

    @NotNull
    Logger getLogger();

    @NotNull
    ScheduledExecutorService getScheduledExecutor();

    @NotNull
    Actor getSelf();

    @NotNull
    Stage getStage();

    void resetBehavior();

    void restart();

    void setBehavior(@NotNull Behavior behavior);

    void stopSelf();
  }
}
