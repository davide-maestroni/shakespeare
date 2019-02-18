package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.plot.function.UnaryFunction;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 02/18/2019.
 */
class FunctionNarrator<T> extends Narrator<T> {

  private final UnaryFunction<? super Narrator<T>, ? extends Boolean> mNarrationCreator;

  FunctionNarrator(
      @NotNull final UnaryFunction<? super Narrator<T>, ? extends Boolean> narrationCreator) {
    mNarrationCreator = ConstantConditions.notNull("narrationCreator", narrationCreator);
  }

  protected boolean narrate() throws Exception {
    final Boolean isContinue = mNarrationCreator.call(this);
    return (isContinue != null) && isContinue;
  }
}
