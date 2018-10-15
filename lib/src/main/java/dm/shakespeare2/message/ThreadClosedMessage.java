package dm.shakespeare2.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare2.actor.SignalingMessage;

/**
 * Created by davide-maestroni on 10/01/2018.
 */
public class ThreadClosedMessage implements SignalingMessage {

  private static final ThreadClosedMessage sInstance = new ThreadClosedMessage();

  @NotNull
  public static ThreadClosedMessage defaultInstance() {
    return sInstance;
  }
}
