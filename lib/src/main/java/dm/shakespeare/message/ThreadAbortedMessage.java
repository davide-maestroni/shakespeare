package dm.shakespeare.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.actor.SignalingMessage;

/**
 * Created by davide-maestroni on 10/01/2018.
 */
public class ThreadAbortedMessage implements SignalingMessage {

  private static final ThreadAbortedMessage sInstance = new ThreadAbortedMessage();

  @NotNull
  public static ThreadAbortedMessage defaultInstance() {
    return sInstance;
  }
}
