package dm.shakespeare;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.ActorSet;
import dm.shakespeare.actor.Options;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 08/06/2018.
 */
class LocalActorSet extends AbstractSet<Actor> implements ActorSet {

  private final Set<Actor> mActors;

  LocalActorSet(@NotNull final Set<? extends Actor> actors) {
    mActors = Collections.unmodifiableSet(ConstantConditions.notNullElements("actors", actors));
  }

  public void dismiss(final boolean mayInterruptIfRunning) {
    for (final Actor actor : mActors) {
      actor.dismiss(mayInterruptIfRunning);
    }
  }

  @NotNull
  public ActorSet tell(final Object message, @Nullable final Options options,
      @NotNull final Actor sender) {
    for (final Actor actor : mActors) {
      actor.tell(message, options, sender);
    }
    return this;
  }

  @NotNull
  public ActorSet tellAll(@NotNull final Iterable<?> messages, @Nullable final Options options,
      @NotNull final Actor sender) {
    for (final Actor actor : mActors) {
      actor.tellAll(messages, options, sender);
    }
    return this;
  }

  @NotNull
  public Iterator<Actor> iterator() {
    return mActors.iterator();
  }

  public int size() {
    return mActors.size();
  }

  @Override
  public boolean isEmpty() {
    return mActors.isEmpty();
  }

  @Override
  public boolean contains(final Object o) {
    return mActors.contains(o);
  }
}
