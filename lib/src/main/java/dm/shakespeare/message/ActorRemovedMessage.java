package dm.shakespeare.message;

import org.jetbrains.annotations.NotNull;

/**
 * Created by davide-maestroni on 10/05/2018.
 */
public class ActorRemovedMessage extends AbstractStageMonitorMessage {

  public ActorRemovedMessage(@NotNull final String stageName, @NotNull final String actorId) {
    super(stageName, actorId);
  }
}
