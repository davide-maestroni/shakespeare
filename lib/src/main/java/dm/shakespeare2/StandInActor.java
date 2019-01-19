package dm.shakespeare2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.actor.Options;
import dm.shakespeare2.message.Delivery;

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
    if ((options != null) && (options.getReceiptId() != null)) {
      sender.tell(new Delivery(message, options), options.threadOnly(), this);
    }
    return this;
  }

  @NotNull
  public Actor tellAll(@NotNull final Iterable<?> messages, @Nullable final Options options,
      @NotNull final Actor sender) {
    for (final Object message : messages) {
      tell(message, options, sender);
    }
    return this;
  }
}
