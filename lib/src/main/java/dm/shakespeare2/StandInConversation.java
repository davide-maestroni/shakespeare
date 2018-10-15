package dm.shakespeare2;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.actor.Actor.ActorSet;
import dm.shakespeare2.actor.Actor.Conversation;
import dm.shakespeare2.actor.Actor.Envelop;

/**
 * Created by davide-maestroni on 06/10/2018.
 */
class StandInConversation<T> implements Conversation<T> {

  private final SingleActorSet mRecipients;
  private final String mThreadId;

  private boolean mClosed;

  StandInConversation(@NotNull final Actor receiver) {
    mRecipients = new SingleActorSet(receiver);
    mThreadId = UUID.randomUUID().toString();
  }

  public void abort() {
    mClosed = true;
  }

  public void close() {
    mClosed = true;
  }

  @NotNull
  public Conversation forward(final Object message, @NotNull final Envelop envelop) {
    if (mClosed) {
      throw new ThreadClosedException();
    }

    return this;
  }

  @NotNull
  public ActorSet getRecipients() {
    return mRecipients;
  }

  @NotNull
  public String getThreadId() {
    return mThreadId;
  }

  @NotNull
  public Conversation<T> tell(final T message) {
    if (mClosed) {
      throw new ThreadClosedException();
    }

    return this;
  }

  @NotNull
  public Conversation<T> tellAll(@NotNull final Iterable<? extends T> messages) {
    if (mClosed) {
      throw new ThreadClosedException();
    }

    return this;
  }
}
