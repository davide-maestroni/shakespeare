package dm.shakespeare2.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare2.actor.SignalingMessage;

/**
 * Created by davide-maestroni on 10/03/2018.
 */
public class AddActorMonitorMessage implements SignalingMessage {

  private static final AddActorMonitorMessage sInstance = new AddActorMonitorMessage();

  @NotNull
  public static AddActorMonitorMessage defaultInstance() {
    return sInstance;
  }
}
