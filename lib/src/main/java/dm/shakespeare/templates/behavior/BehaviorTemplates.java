package dm.shakespeare.templates.behavior;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.actor.BehaviorBuilder;
import dm.shakespeare.util.ConstantConditions;

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
