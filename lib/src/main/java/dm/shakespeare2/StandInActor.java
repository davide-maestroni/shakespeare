package dm.shakespeare2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.actor.Options;

/**
 * Created by davide-maestroni on 01/14/2019.
 */
class StandInActor implements Actor {

  @NotNull
  public Actor addObserver(@NotNull final Actor observer) {
    return this;
  }

  public void dismiss(final boolean mayInterruptIfRunning) {
  }

  @NotNull
  public String getId() {
    return getClass().getName();
  }

  @NotNull
  public Actor removeObserver(@NotNull final Actor observer) {
    return this;
  }

  @NotNull
  public Actor tell(final Object message, @Nullable final Options options,
      @NotNull final Actor sender) {
    return this;
  }

  @NotNull
  public Actor tellAll(@NotNull final Iterable<?> messages, @Nullable final Options options,
      @NotNull final Actor sender) {
    return this;
  }
}
