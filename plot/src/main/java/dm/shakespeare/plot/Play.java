package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import dm.shakespeare.actor.Script;
import dm.shakespeare.executor.ExecutorServices;
import dm.shakespeare.log.Logger;
import dm.shakespeare.plot.function.NullaryFunction;
import dm.shakespeare.plot.function.UnaryFunction;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/25/2019.
 */
public class Play {

  private static final EventFunction<?> EVENT_FUNCTION = new EventFunction<Object>();
  private static final LoopFunction LOOP_FUNCTION = new LoopFunction();
  private static final StoryFunction<?> STORY_FUNCTION = new StoryFunction<Object>();

  private final PlayContext mPlayContext;

  public Play() {
    mPlayContext = new PlayContext(asActorExecutor(Script.defaultExecutor()), null);
  }

  public Play(@NotNull final ExecutorService executor) {
    mPlayContext = new PlayContext(asActorExecutor(executor), null);
  }

  public Play(@NotNull final ExecutorService executor, @NotNull final Logger logger) {
    mPlayContext =
        new PlayContext(asActorExecutor(executor), ConstantConditions.notNull("logger", logger));
  }

  public Play(@NotNull final Logger logger) {
    mPlayContext = new PlayContext(asActorExecutor(Script.defaultExecutor()),
        ConstantConditions.notNull("logger", logger));
  }

  @NotNull
  private static ExecutorService asActorExecutor(@NotNull final ExecutorService executor) {
    return (executor instanceof ScheduledExecutorService) ? ExecutorServices.asActorExecutor(
        (ScheduledExecutorService) executor) : ExecutorServices.asActorExecutor(executor);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <T> Event<T> performEvent(@NotNull final Event<T> event) {
    PlayContext.set(mPlayContext);
    try {
      return event.then((EventFunction<T>) EVENT_FUNCTION);

    } finally {
      PlayContext.unset();
    }
  }

  @NotNull
  public <T> Event<T> performEvent(@NotNull final NullaryFunction<? extends Event<T>> function) {
    PlayContext.set(mPlayContext);
    try {
      return Event.ofNull().then(new UnaryFunction<Object, Event<T>>() {

        public Event<T> call(final Object first) throws Exception {
          return function.call();
        }
      });

    } catch (final Throwable t) {
      return Event.ofIncident(t);

    } finally {
      PlayContext.unset();
    }
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <T> Story<T> performStory(@NotNull final Story<T> story) {
    PlayContext.set(mPlayContext);
    try {
      return story.then(LOOP_FUNCTION,
          (UnaryFunction<? super T, ? extends Story<T>>) STORY_FUNCTION);

    } finally {
      PlayContext.unset();
    }
  }

  @NotNull
  public <T> Story<T> performStory(@NotNull final NullaryFunction<? extends Story<T>> function) {
    PlayContext.set(mPlayContext);
    try {
      return Story.ofEvent(Event.<T>ofNull())
          .then(LOOP_FUNCTION, new UnaryFunction<Object, Story<T>>() {

            public Story<T> call(final Object first) throws Exception {
              return function.call();
            }
          });

    } catch (final Throwable t) {
      return Story.ofIncidents(Collections.singleton(t));

    } finally {
      PlayContext.unset();
    }
  }

  private static class EventFunction<T> implements UnaryFunction<T, Event<T>> {

    public Event<T> call(final T first) {
      return Event.ofResolution(first);
    }
  }

  private static class LoopFunction implements NullaryFunction<Event<Boolean>> {

    public Event<Boolean> call() {
      return Event.ofTrue();
    }
  }

  private static class StoryFunction<T> implements UnaryFunction<T, Story<T>> {

    public Story<T> call(final T first) {
      return Story.ofResolutions(Collections.singleton(first));
    }
  }
}
