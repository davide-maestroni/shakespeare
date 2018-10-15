package dm.shakespeare2.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare2.actor.ActorMonitorMessage;

/**
 * Created by davide-maestroni on 10/01/2018.
 */
public class ActorSuspendedMessage implements ActorMonitorMessage {

  private static final ActorSuspendedMessage sInstance = new ActorSuspendedMessage();

  @NotNull
  public static ActorSuspendedMessage defaultInstance() {
    return sInstance;
  }
}
