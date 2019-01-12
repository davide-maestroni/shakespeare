package dm.shakespeare2.actor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Created by davide-maestroni on 01/08/2019.
 */
public interface ActorSet extends Set<Actor> {

  void dismiss(boolean mayInterruptIfRunning);

  @NotNull
  ActorSet tell(Object message, @Nullable Options options, @NotNull Actor sender);

  @NotNull
  ActorSet tellAll(@NotNull Iterable<?> messages, @Nullable Options options, @NotNull Actor sender);
}
