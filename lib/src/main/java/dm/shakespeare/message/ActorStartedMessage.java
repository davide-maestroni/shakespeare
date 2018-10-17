package dm.shakespeare.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.actor.ActorMonitorMessage;

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
