package dm.shakespeare2.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare2.actor.ActorMonitorMessage;

/**
 * Created by davide-maestroni on 10/01/2018.
 */
public class ActorStartedMessage implements ActorMonitorMessage {

  private static final ActorStartedMessage sInstance = new ActorStartedMessage();

  @NotNull
  public static ActorStartedMessage defaultInstance() {
    return sInstance;
  }
}
