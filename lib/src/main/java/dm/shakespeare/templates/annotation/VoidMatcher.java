package dm.shakespeare.templates.annotation;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.actor.Actor.Envelop;
import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.actor.BehaviorBuilder.Matcher;
import dm.shakespeare.util.ConstantConditions;

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
