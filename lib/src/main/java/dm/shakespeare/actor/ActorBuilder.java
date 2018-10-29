package dm.shakespeare.actor;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

import dm.shakespeare.function.Mapper;
import dm.shakespeare.function.Provider;
import dm.shakespeare.function.Tester;
import dm.shakespeare.log.Logger;

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
  ActorBuilder mayInterruptIfRunning(@NotNull Tester<? super String> tester);

  @NotNull
  ActorBuilder preventDefault(@NotNull Tester<? super String> tester);

  @NotNull
  ActorBuilder quota(@NotNull Mapper<? super String, ? extends Integer> mapper);
}
