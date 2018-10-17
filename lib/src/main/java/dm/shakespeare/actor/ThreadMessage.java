package dm.shakespeare.actor;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.actor.Actor.Envelop;

/**
 * Created by davide-maestroni on 10/01/2018.
 */
public interface ThreadMessage extends SignalingMessage {

  @NotNull
  Envelop getEnvelop();

  Object getMessage();
}
