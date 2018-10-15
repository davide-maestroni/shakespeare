package dm.shakespeare2.actor;

import org.jetbrains.annotations.NotNull;

/**
 * Created by davide-maestroni on 10/01/2018.
 */
public interface StageMonitorMessage extends SignalingMessage {

  @NotNull
  String getActorId();

  @NotNull
  String getStageName();
}
