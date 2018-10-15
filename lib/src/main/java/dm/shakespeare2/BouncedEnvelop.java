package dm.shakespeare2;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.actor.Actor.Envelop;

/**
 * Created by davide-maestroni on 10/12/2018.
 */
class BouncedEnvelop extends DefaultEnvelop {

  BouncedEnvelop(@NotNull final Actor sender) {
    super(sender);
  }

  BouncedEnvelop(@NotNull final Actor sender, @NotNull final Envelop envelop) {
    super(sender, envelop);
  }

  BouncedEnvelop(@NotNull final Actor sender, @NotNull final Envelop envelop,
      @NotNull final String threadId) {
    super(sender, envelop, threadId);
  }

  BouncedEnvelop(@NotNull final Actor sender, @NotNull final String threadId) {
    super(sender, threadId);
  }

  void open() {
  }
}
