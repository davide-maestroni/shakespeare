package dm.shakespeare2.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare2.actor.SignalingMessage;

/**
 * Created by davide-maestroni on 10/01/2018.
 */
public class SetSupervisorMessage implements SignalingMessage {

  private static final SetSupervisorMessage sInstance = new SetSupervisorMessage();

  @NotNull
  public static SetSupervisorMessage defaultInstance() {
    return sInstance;
  }
}
