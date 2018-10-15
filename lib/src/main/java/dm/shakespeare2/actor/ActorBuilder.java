package dm.shakespeare2.actor;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

import dm.shakespeare2.function.Mapper;
import dm.shakespeare2.function.Provider;
import dm.shakespeare2.log.Logger;

/**
 * Created by davide-maestroni on 09/10/2018.
 */
public interface ActorBuilder {

  @NotNull
  ActorBuilder behavior(@NotNull Provider<? extends Behavior> provider);

  @NotNull
  Actor build();

  @NotNull
  ActorBuilder executor(@NotNull Mapper<? super String, ? extends ExecutorService> mapper);

  @NotNull
  ActorBuilder id(@NotNull String id);

  @NotNull
  ActorBuilder logger(@NotNull Mapper<? super String, ? extends Logger> mapper);

  @NotNull
  ActorBuilder mayInterruptIfRunning(boolean interruptIfRunning);

  @NotNull
  ActorBuilder preventDefault(boolean prevent);

  @NotNull
  ActorBuilder quota(int quota);
}
