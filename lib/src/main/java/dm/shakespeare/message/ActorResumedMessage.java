package dm.shakespeare.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.actor.ActorMonitorMessage;

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
