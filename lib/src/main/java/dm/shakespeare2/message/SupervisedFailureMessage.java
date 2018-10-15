package dm.shakespeare2.message;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dm.shakespeare2.actor.SupervisorMessage;
import dm.shakespeare2.util.ConstantConditions;

/**
 * Created by davide-maestroni on 07/06/2018.
 */
public class SupervisedFailureMessage implements SupervisorMessage {

  private final Throwable mCause;
  private final String mFailureId;

  public SupervisedFailureMessage(@Nullable final String failureId,
      @NotNull final Throwable cause) {
    mFailureId = ConstantConditions.notNull("failureId", failureId);
    mCause = ConstantConditions.notNull("cause", cause);
  }

  @NotNull
  public final Throwable getCause() {
    return mCause;
  }

  @NotNull
  public final String getFailureId() {
    return mFailureId;
  }
}
