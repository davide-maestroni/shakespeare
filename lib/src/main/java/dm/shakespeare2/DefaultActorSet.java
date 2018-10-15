package dm.shakespeare2;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.actor.Actor.ActorSet;
import dm.shakespeare2.actor.Actor.Conversation;
import dm.shakespeare2.actor.Actor.Envelop;
import dm.shakespeare2.actor.ThreadMessage;

/**
 * Created by davide-maestroni on 08/06/2018.
 */
class DefaultActorSet extends AbstractSet<Actor> implements ActorSet {

  private final Set<Actor> mActors;

  DefaultActorSet(@NotNull final Set<? extends Actor> actors) {
    mActors = Collections.unmodifiableSet(actors);
  }

  @NotNull
  public ActorSet forward(final Object message, @NotNull final Envelop envelop,
      @NotNull final Actor sender) {
    for (final Actor actor : mActors) {
      actor.forward(message, envelop, sender);
    }

    return this;
  }

  public void kill() {
    for (final Actor actor : mActors) {
      actor.kill();
    }
  }

  @NotNull
  public ActorSet tell(final Object message, @NotNull final Actor sender) {
    for (final Actor actor : mActors) {
      actor.tell(message, sender);
    }

    return this;
  }

  @NotNull
  public ActorSet tellAll(@NotNull final Iterable<?> messages, @NotNull final Actor sender) {
    for (final Actor actor : mActors) {
      actor.tellAll(messages, sender);
    }

    return this;
  }

  @NotNull
  public <T> Conversation<T> thread(@NotNull final String threadId, @NotNull final Actor sender) {
    return thread(threadId, sender, Collections.<Class<? extends ThreadMessage>>emptySet());
  }

  @NotNull
  public <T> Conversation<T> thread(@NotNull final String threadId, @NotNull final Actor sender,
      @NotNull final Class<? extends ThreadMessage>... messageFilters) {
    return thread(threadId, sender, Arrays.asList(messageFilters));
  }

  @NotNull
  public <T> Conversation<T> thread(@NotNull final String threadId, @NotNull final Actor sender,
      @NotNull final Collection<? extends Class<? extends ThreadMessage>> messageFilters) {
    return new ConversationSet<T>(this, threadId, sender, messageFilters);
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

  private static class ConversationSet<T> implements Conversation<T> {

    private final ActorSet mActors;
    private final ArrayList<Conversation<T>> mConversations;
    private final String mThreadId;

    private ConversationSet(@NotNull final ActorSet actors, @NotNull final String threadId,
        @NotNull final Actor sender,
        @NotNull final Collection<? extends Class<? extends ThreadMessage>> includes) {
      final ArrayList<Conversation<T>> conversations = new ArrayList<Conversation<T>>();
      try {
        for (final Actor actor : actors) {
          conversations.add(actor.<T>thread(threadId, sender, includes));
        }

      } catch (final RuntimeException e) {
        for (final Conversation<T> conversation : conversations) {
          conversation.close();
        }

        throw e;
      }

      mActors = actors;
      mThreadId = threadId;
      mConversations = conversations;
    }

    public void abort() {
      for (final Conversation<T> conversation : mConversations) {
        conversation.abort();
      }
    }

    public void close() {
      for (final Conversation<T> conversation : mConversations) {
        conversation.close();
      }
    }

    @NotNull
    public Conversation forward(final Object message, @NotNull final Envelop envelop) {
      for (final Conversation<T> conversation : mConversations) {
        conversation.forward(message, envelop);
      }

      return this;
    }

    @NotNull
    public ActorSet getRecipients() {
      return mActors;
    }

    @NotNull
    public String getThreadId() {
      return mThreadId;
    }

    @NotNull
    public Conversation<T> tell(final T message) {
      for (final Conversation<T> conversation : mConversations) {
        conversation.tell(message);
      }

      return this;
    }

    @NotNull
    public Conversation<T> tellAll(@NotNull final Iterable<? extends T> messages) {
      for (final Conversation<T> conversation : mConversations) {
        conversation.tellAll(messages);
      }

      return this;
    }
  }
}
