package dm.shakespeare2.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare2.actor.StageMonitorMessage;
import dm.shakespeare2.util.ConstantConditions;

/**
 * Created by davide-maestroni on 10/05/2018.
 */
abstract class AbstractStageMonitorMessage implements StageMonitorMessage {

  private final String mActorId;
  private final String mStageName;

  AbstractStageMonitorMessage(@NotNull final String stageName, @NotNull final String actorId) {
    mStageName = ConstantConditions.notNull("stageName", stageName);
    mActorId = ConstantConditions.notNull("actorId", actorId);
  }

  @NotNull
  public final String getActorId() {
    return mActorId;
  }

  @NotNull
  public final String getStageName() {
    return mStageName;
  }
}
