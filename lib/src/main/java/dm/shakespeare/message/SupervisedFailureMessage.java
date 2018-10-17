package dm.shakespeare.message;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dm.shakespeare.actor.SupervisorMessage;
import dm.shakespeare.util.ConstantConditions;

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
