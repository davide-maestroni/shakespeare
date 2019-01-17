package dm.shakespeare2.template.annotation;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare2.actor.Behavior.Context;
import dm.shakespeare2.actor.BehaviorBuilder.Matcher;
import dm.shakespeare2.actor.Envelop;

/**
 * Created by davide-maestroni on 09/06/2018.
 */
public class VoidMatcher implements Matcher<Object> {

  private VoidMatcher() {
    ConstantConditions.avoid();
  }

  public boolean match(final Object message, @NotNull final Envelop envelop,
      @NotNull final Context context) {
    return false;
  }
}
