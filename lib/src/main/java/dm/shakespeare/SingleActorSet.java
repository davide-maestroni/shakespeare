package dm.shakespeare;

import org.jetbrains.annotations.NotNull;

import java.io.NotSerializableException;
import java.io.ObjectStreamException;
import java.util.Collection;
import java.util.HashSet;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Actor.ActorSet;
import dm.shakespeare.actor.Actor.Conversation;
import dm.shakespeare.actor.Actor.Envelop;
import dm.shakespeare.actor.ThreadMessage;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 06/19/2018.
 */
class SingleActorSet extends HashSet<Actor> implements ActorSet {

  // Unused
  private static final long serialVersionUID = -1;

  private final Actor mActor;

  SingleActorSet(@NotNull final Actor actor) {
    mActor = ConstantConditions.notNull("actor", actor);
  }

  @NotNull
  public ActorSet forward(final Object message, @NotNull final Envelop envelop,
      @NotNull final Actor sender) {
    mActor.forward(message, envelop, sender);
    return this;
  }

  public void kill() {
    mActor.kill();
  }

  @NotNull
  public ActorSet tell(final Object message, @NotNull final Actor sender) {
    mActor.tell(message, sender);
    return this;
  }

  @NotNull
  public ActorSet tellAll(@NotNull final Iterable<?> messages, @NotNull final Actor sender) {
    mActor.tellAll(messages, sender);
    return this;
  }

  @NotNull
  public <T> Conversation<T> thread(@NotNull final String threadId, @NotNull final Actor sender) {
    return mActor.thread(threadId, sender);
  }

  @NotNull
  public <T> Conversation<T> thread(@NotNull final String threadId, @NotNull final Actor sender,
      @NotNull final Class<? extends ThreadMessage>... messageFilters) {
    return mActor.thread(threadId, sender, messageFilters);
  }

  @NotNull
  public <T> Conversation<T> thread(@NotNull final String threadId, @NotNull final Actor sender,
      @NotNull final Collection<? extends Class<? extends ThreadMessage>> messageFilters) {
    return mActor.thread(threadId, sender, messageFilters);
  }

  @NotNull
  private Object writeReplace() throws ObjectStreamException {
    throw new NotSerializableException();
  }
}
