package dm.shakespeare2.actor;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare2.actor.Actor.Envelop;

/**
 * Created by davide-maestroni on 10/01/2018.
 */
public interface ThreadMessage extends SignalingMessage {

  @NotNull
  Envelop getEnvelop();

  Object getMessage();
}
