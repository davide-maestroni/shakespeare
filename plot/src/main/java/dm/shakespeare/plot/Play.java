package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

import dm.shakespeare.log.Logger;
import dm.shakespeare.plot.function.NullaryFunction;
import dm.shakespeare.plot.function.UnaryFunction;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/25/2019.
 */
public class Play {

  private static final EventFunction<?> EVENT_FUNCTION = new EventFunction<Object>();

  private final PlayContext mPlayContext;

  public Play() {
    mPlayContext = new PlayContext(null, null);
  }

  public Play(@NotNull final ExecutorService executor) {
    mPlayContext = new PlayContext(ConstantConditions.notNull("executor", executor), null);
  }

  public Play(@NotNull final ExecutorService executor, @NotNull final Logger logger) {
    mPlayContext = new PlayContext(ConstantConditions.notNull("executor", executor),
        ConstantConditions.notNull("logger", logger));
  }

  public Play(@NotNull final Logger logger) {
    mPlayContext = new PlayContext(null, ConstantConditions.notNull("logger", logger));
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <T> Event<T> runEvent(@NotNull final Event<T> event) {
    PlayContext.set(mPlayContext);
    try {
      return event.then((EventFunction<T>) EVENT_FUNCTION);

    } finally {
      PlayContext.unset();
    }
  }

  @NotNull
  public <T> Event<T> runEvent(@NotNull final NullaryFunction<? extends Event<T>> function) {
    PlayContext.set(mPlayContext);
    try {
      return function.call();

    } catch (final Throwable t) {
      if (t instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      return Event.ofIncident(t);

    } finally {
      PlayContext.unset();
    }
  }

  private static class EventFunction<T> implements UnaryFunction<T, Event<T>> {

    public Event<T> call(final T first) {
      return Event.ofResolution(first);
    }
  }
}
