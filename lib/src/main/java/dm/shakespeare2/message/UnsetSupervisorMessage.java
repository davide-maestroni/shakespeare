package dm.shakespeare2.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare2.actor.SignalingMessage;

/**
 * Created by davide-maestroni on 10/01/2018.
 */
public class UnsetSupervisorMessage implements SignalingMessage {

  private static final UnsetSupervisorMessage sInstance = new UnsetSupervisorMessage();

  @NotNull
  public static UnsetSupervisorMessage defaultInstance() {
    return sInstance;
  }
}
