package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.LocalStage;
import dm.shakespeare.plot.function.NullaryFunction;

/**
 * Created by davide-maestroni on 01/25/2019.
 */
public class Play {

  private final PlayContext mPlayContext;

  public Play() {
    mPlayContext = new PlayContext(new LocalStage(), null, null);
  }

  @NotNull
  public <T> Line<T> run(@NotNull NullaryFunction<? extends Line<T>> function) throws Exception {
    PlayContext.set(mPlayContext);
    try {
      return function.call();

    } finally {
      PlayContext.unset();
    }
  }
}
