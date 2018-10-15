package dm.shakespeare2.message;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import dm.shakespeare2.actor.SignalingMessage;
import dm.shakespeare2.actor.ThreadMessage;
import dm.shakespeare2.util.ConstantConditions;

/**
 * Created by davide-maestroni on 10/01/2018.
 */
public class ThreadOpenedMessage implements SignalingMessage {

  private final Collection<? extends Class<? extends ThreadMessage>> mMessageFilters;

  public ThreadOpenedMessage(
      @NotNull final Collection<? extends Class<? extends ThreadMessage>> messageFilters) {
    mMessageFilters = ConstantConditions.notNullElements("messageFilters", messageFilters);
  }

  @NotNull
  public final Collection<? extends Class<? extends ThreadMessage>> getMessageFilters() {
    return mMessageFilters;
  }
}
