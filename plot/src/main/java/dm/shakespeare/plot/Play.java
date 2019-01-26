package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

import dm.shakespeare.LocalStage;
import dm.shakespeare.log.Logger;
import dm.shakespeare.plot.function.NullaryFunction;
import dm.shakespeare.plot.function.UnaryFunction;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/25/2019.
 */
public class Play {

  // TODO: 26/01/2019 memory leak

  private static final LineFunction<?> LINE_FUNCTION = new LineFunction<Object>();

  private final PlayContext mPlayContext;

  public Play() {
    mPlayContext = new PlayContext(new LocalStage(), null, null);
  }

  public Play(@NotNull final ExecutorService executor) {
    mPlayContext =
        new PlayContext(new LocalStage(), ConstantConditions.notNull("executor", executor), null);
  }

  public Play(@NotNull final ExecutorService executor, @NotNull final Logger logger) {
    mPlayContext =
        new PlayContext(new LocalStage(), ConstantConditions.notNull("executor", executor),
            ConstantConditions.notNull("logger", logger));
  }

  public Play(@NotNull final Logger logger) {
    mPlayContext =
        new PlayContext(new LocalStage(), null, ConstantConditions.notNull("logger", logger));
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <T> Line<T> runLine(@NotNull final Line<T> line) {
    PlayContext.set(mPlayContext);
    try {
      return line.translate((LineFunction<T>) LINE_FUNCTION);

    } finally {
      PlayContext.unset();
    }
  }

  @NotNull
  public <T> Line<T> runLine(@NotNull final NullaryFunction<? extends Line<T>> function) {
    PlayContext.set(mPlayContext);
    try {
      return function.call();

    } catch (final Throwable t) {
      if (t instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      return Line.ofError(t);

    } finally {
      PlayContext.unset();
    }
  }

  private static class LineFunction<T> implements UnaryFunction<T, Line<T>> {

    public Line<T> call(final T first) {
      return Line.ofValue(first);
    }
  }
}
