package dm.shakespeare2.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare2.actor.ActorMonitorMessage;

/**
 * Created by davide-maestroni on 10/01/2018.
 */
public class ActorResumedMessage implements ActorMonitorMessage {

  private static final ActorResumedMessage sInstance = new ActorResumedMessage();

  @NotNull
  public static ActorResumedMessage defaultInstance() {
    return sInstance;
  }
}
