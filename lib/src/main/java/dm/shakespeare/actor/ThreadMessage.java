package dm.shakespeare.actor;

import org.jetbrains.annotations.NotNull;

/**
 * Created by davide-maestroni on 10/01/2018.
 */
public interface ThreadMessage extends SignalingMessage {

  Object getMessage();

  @NotNull
  String getThreadId();
}
