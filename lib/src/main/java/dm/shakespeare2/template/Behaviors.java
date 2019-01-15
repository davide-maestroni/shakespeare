package dm.shakespeare2.template;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare2.actor.Behavior;

/**
 * Created by davide-maestroni on 01/13/2019.
 */
public class Behaviors {

  private Behaviors() {
    ConstantConditions.avoid();
  }

  @NotNull
  public static PoisonableBehavior poisonable(@NotNull final Behavior behavior) {
    if (behavior instanceof PoisonableBehavior) {
      return (PoisonableBehavior) behavior;
    }
    return new PoisonableBehavior(behavior);
  }

  @NotNull
  public static SupervisedBehavior supervised(@NotNull final Behavior behavior) {
    if (behavior instanceof SupervisedBehavior) {
      return (SupervisedBehavior) behavior;
    }
    return new SupervisedBehavior(behavior);
  }
}
