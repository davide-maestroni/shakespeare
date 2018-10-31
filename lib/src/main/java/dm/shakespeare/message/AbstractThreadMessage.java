package dm.shakespeare.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.actor.Actor.Envelop;
import dm.shakespeare.actor.ThreadMessage;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 10/03/2018.
 */
abstract class AbstractThreadMessage implements ThreadMessage {

  private final Envelop mEnvelop;
  private final Object mMessage;
  private final String mThreadId;

  AbstractThreadMessage(final Object message, @NotNull final Envelop envelop) {
    mMessage = message;
    mEnvelop = ConstantConditions.notNull("envelop", envelop);
    mThreadId = ConstantConditions.notNull("threadId", envelop.getThreadId());
  }

  @NotNull
  public final Envelop getEnvelop() {
    return mEnvelop;
  }

  public final Object getMessage() {
    return mMessage;
  }

  @NotNull
  public final String getThreadId() {
    return mThreadId;
  }
}
