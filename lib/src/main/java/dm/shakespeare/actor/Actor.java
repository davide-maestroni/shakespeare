package dm.shakespeare.actor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.Collection;
import java.util.Set;

/**
 * Created by davide-maestroni on 09/10/2018.
 */
public interface Actor {

  @NotNull
  Actor forward(Object message, @NotNull Envelop envelop, @NotNull Actor sender);

  @NotNull
  String getId();

  void remove();

  @NotNull
  Actor tell(Object message, @NotNull Actor sender);

  @NotNull
  Actor tellAll(@NotNull Iterable<?> messages, @NotNull Actor sender);

  @NotNull
  <T> Conversation<T> thread(@NotNull String threadId,
      @NotNull Collection<? extends Class<? extends ThreadMessage>> messageFilters,
      @NotNull Actor sender);

  interface ActorSet extends Set<Actor> {

    @NotNull
    ActorSet forward(Object message, @NotNull Envelop envelop, @NotNull Actor sender);

    void remove();

    @NotNull
    ActorSet tell(Object message, @NotNull Actor sender);

    @NotNull
    ActorSet tellAll(@NotNull Iterable<?> messages, @NotNull Actor sender);

    @NotNull
    <T> Conversation<T> thread(@NotNull String threadId,
        @NotNull Collection<? extends Class<? extends ThreadMessage>> messageFilters,
        @NotNull Actor sender);
  }

  interface Conversation<T> extends Closeable {

    void abort();

    void close();

    @NotNull
    Conversation forward(Object message, @NotNull Envelop envelop);

    @NotNull
    ActorSet getRecipients();

    @NotNull
    String getThreadId();

    @NotNull
    Conversation<T> tell(T message);

    @NotNull
    Conversation<T> tellAll(@NotNull Iterable<? extends T> messages);
  }

  interface Envelop {

    long getReceivedAt();

    @NotNull
    Actor getSender();

    long getSentAt();

    @Nullable
    String getThreadId();
  }
}
