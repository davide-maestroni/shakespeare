package dm.shakespeare2.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare2.actor.SignalingMessage;

/**
 * Created by davide-maestroni on 10/03/2018.
 */
public class RemoveActorMonitorMessage implements SignalingMessage {

  private static final RemoveActorMonitorMessage sInstance = new RemoveActorMonitorMessage();

  @NotNull
  public static RemoveActorMonitorMessage defaultInstance() {
    return sInstance;
  }
}
