package dm.shakespeare2.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare2.actor.SignalingMessage;
import dm.shakespeare2.util.ConstantConditions;

/**
 * Created by davide-maestroni on 07/06/2018.
 */
public class SupervisedRecoveryMessage implements SignalingMessage {

  private final String mFailureId;
  private final RecoveryType mRecoveryType;

  public SupervisedRecoveryMessage(@NotNull final String failureId,
      @NotNull final RecoveryType recoveryType) {
    mFailureId = ConstantConditions.notNull("failureId", failureId);
    mRecoveryType = ConstantConditions.notNull("recoveryType", recoveryType);
  }

  @NotNull
  public final String getFailureId() {
    return mFailureId;
  }

  @NotNull
  public final RecoveryType getRecoveryType() {
    return mRecoveryType;
  }

  public enum RecoveryType {
    RESUME, RESTART, STOP
  }
}
