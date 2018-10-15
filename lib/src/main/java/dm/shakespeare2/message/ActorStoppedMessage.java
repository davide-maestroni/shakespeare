package dm.shakespeare2.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare2.actor.ActorMonitorMessage;

/**
 * Created by davide-maestroni on 10/01/2018.
 */
public class ActorStoppedMessage implements ActorMonitorMessage {

  private static final ActorStoppedMessage sInstance = new ActorStoppedMessage();

  @NotNull
  public static ActorStoppedMessage defaultInstance() {
    return sInstance;
  }
}
