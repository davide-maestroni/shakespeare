package dm.shakespeare2.message;

import org.jetbrains.annotations.NotNull;

/**
 * Created by davide-maestroni on 10/05/2018.
 */
public class ActorCreatedMessage extends AbstractStageMonitorMessage {

  public ActorCreatedMessage(@NotNull final String stageName, @NotNull final String actorId) {
    super(stageName, actorId);
  }
}
