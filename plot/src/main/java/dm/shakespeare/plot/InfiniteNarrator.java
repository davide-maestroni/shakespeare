package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.plot.function.NullaryFunction;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 02/18/2019.
 */
class InfiniteNarrator<T> extends Narrator<T> {

  private final NullaryFunction<T> mNarrationCreator;

  InfiniteNarrator(@NotNull final NullaryFunction<T> narrationCreator) {
    mNarrationCreator = ConstantConditions.notNull("narrationCreator", narrationCreator);
  }

  protected boolean narrate() throws Exception {
    tell(mNarrationCreator.call());
    return true;
  }
}
