package dm.shakespeare2;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.actor.ThreadMessage;

/**
 * Created by davide-maestroni on 06/10/2018.
 */
class StandInActor implements Actor {

  private static final StandInActor sInstance = new StandInActor();

  private final String mId;

  private StandInActor() {
    mId = getClass().getName();
  }

  @NotNull
  static StandInActor defaultInstance() {
    return sInstance;
  }

  @NotNull
  public Actor forward(final Object message, @NotNull final Envelop envelop,
      @NotNull final Actor sender) {
    return this;
  }

  @NotNull
  public String getId() {
    return mId;
  }

  public boolean isStopped() {
    return false;
  }

  public void kill() {
  }

  @NotNull
  public Actor tell(final Object message, @NotNull final Actor sender) {
    return this;
  }

  @NotNull
  public Actor tellAll(@NotNull final Iterable<?> messages, @NotNull final Actor sender) {
    return this;
  }

  @NotNull
  public <T> Conversation<T> thread(@NotNull final String threadId, @NotNull final Actor sender) {
    return new StandInConversation<T>(sender);
  }

  @NotNull
  public <T> Conversation<T> thread(@NotNull final String threadId, @NotNull final Actor sender,
      @NotNull final Class<? extends ThreadMessage>... messageFilters) {
    return thread(threadId, sender);
  }

  @NotNull
  public <T> Conversation<T> thread(@NotNull final String threadId, @NotNull final Actor sender,
      @NotNull final Collection<? extends Class<? extends ThreadMessage>> messageFilters) {
    return thread(threadId, sender);
  }
}
