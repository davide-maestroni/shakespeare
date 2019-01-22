package dm.shakespeare.actor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by davide-maestroni on 01/08/2019.
 */
public interface Actor {

  @NotNull
  Actor addObserver(@NotNull Actor observer);

  void dismiss(boolean mayInterruptIfRunning);

  @NotNull
  String getId();

  @NotNull
  Actor removeObserver(@NotNull Actor observer);

  @NotNull
  Actor tell(Object message, @Nullable Options options, @NotNull Actor sender);

  @NotNull
  Actor tellAll(@NotNull Iterable<?> messages, @Nullable Options options, @NotNull Actor sender);
}
