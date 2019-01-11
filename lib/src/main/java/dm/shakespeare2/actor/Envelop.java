package dm.shakespeare2.actor;

import org.jetbrains.annotations.NotNull;

/**
 * Created by davide-maestroni on 01/08/2019.
 */
public interface Envelop {

  @NotNull
  Options getOptions();

  long getReceivedAt();

  @NotNull
  Actor getSender();

  long getSentAt();
}
