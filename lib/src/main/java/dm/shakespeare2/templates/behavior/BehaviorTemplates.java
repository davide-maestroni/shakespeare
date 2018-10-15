package dm.shakespeare2.templates.behavior;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare2.actor.BehaviorBuilder;
import dm.shakespeare2.util.ConstantConditions;

/**
 * Created by davide-maestroni on 09/27/2018.
 */
public class BehaviorTemplates {

  private BehaviorTemplates() {
    ConstantConditions.avoid();
  }

  @NotNull
  public static BehaviorBuilder newBehavior(@NotNull final Object object) {
    return BehaviorObject.newBehavior(object);
  }
}
