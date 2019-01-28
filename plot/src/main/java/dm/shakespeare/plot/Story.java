package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dm.shakespeare.function.Observer;
import dm.shakespeare.plot.function.NullaryFunction;
import dm.shakespeare.plot.function.UnaryFunction;

/**
 * Created by davide-maestroni on 01/22/2019.
 */
public abstract class Story<T> extends Event<Iterable<T>> {

  static final Object BREAK = new Object();
  static final Object NEXT = new Object();

  // TODO: 27/01/2019 stack(?) stories

  @NotNull
  public static <T> Story<T> ofEvent(@NotNull final Event<T> event) {
    return null;
  }

  @NotNull
  public static <T> Story<T> ofIncidents(@NotNull final Iterable<? extends Throwable> obstacles) {
    return null;
  }

  @NotNull
  public static <T> Story<T> ofResolutions(@NotNull final Iterable<T> results) {
    return null;
  }

  @NotNull
  public static <T> Story<T> stack(@NotNull final Iterable<? extends Event<? extends T>> events) {
    return null;
  }

  @NotNull
  public static <T> Story<T> stackEventually(
      @NotNull final Iterable<? extends Event<? extends T>> events) {
    return null;
  }

  @NotNull
  public static <T> Story<T> stackGreedily(
      @NotNull final Iterable<? extends Event<? extends T>> events) {
    return null;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public static <T> Story<T> unfold(@NotNull final Event<? extends Iterable<T>> event) {
    if (event instanceof Story) {
      return (Story<T>) event;
    }
    return null;
  }

  @NotNull
  public static <T1, R> Story<R> when(@NotNull final Story<? extends T1> firstStory,
      @NotNull final NullaryFunction<? extends Story<? extends Boolean>> loopHandler,
      @NotNull final UnaryFunction<? super T1, ? extends Story<? extends R>> resolutionHandler,
      @NotNull final NullaryFunction<? extends Story<? extends R>> endHandler) {
    return null;
  }

  @NotNull
  public static <T1, R> Story<R> when(@NotNull final Story<? extends T1> firstStory,
      @NotNull final StoryHandler<? super T1, R> storyHandler) {
    return null;
  }

  public void observeAll(@Nullable final Observer<? super T> resolutionObserver,
      @Nullable final Observer<? super Throwable> incidentObserver) {
    observeAll(new DefaultEventObserver<T>(resolutionObserver, incidentObserver));
  }

  @NotNull
  public <R> Story<R> resolve(
      @NotNull final NullaryFunction<? extends Story<? extends Boolean>> loopHandler,
      @NotNull final UnaryFunction<? super T, ? extends Story<? extends R>> resolutionHandler,
      @NotNull final UnaryFunction<? super Throwable, ? extends Story<? extends R>> incidentHandler,
      @NotNull final NullaryFunction<? extends Story<? extends R>> endHandler) {
    return null;
  }

  @NotNull
  public <R> Story<R> resolve(@NotNull final StoryResolver<? super T, R> storyResolver) {
    return null;
  }

  @NotNull
  public <R> Story<R> then(
      @NotNull final NullaryFunction<? extends Story<? extends Boolean>> loopHandler,
      @NotNull final UnaryFunction<? super T, ? extends Story<? extends R>> messageHandler,
      @NotNull final NullaryFunction<? extends Story<? extends R>> endHandler) {
    return when(this, loopHandler, messageHandler, endHandler);
  }

  @NotNull
  public <R> Story<R> then(@NotNull final StoryHandler<? super T, R> storyHandler) {
    return when(this, storyHandler);
  }

  abstract void observeAll(@NotNull final EventObserver<? super T> eventObserver);

  public interface StoryHandler<T, R> {

    Story<? extends Boolean> nextEvent() throws Exception;

    Story<R> onEnd() throws Exception;

    Story<R> onResolution(T result) throws Exception;
  }

  public interface StoryResolver<T, R> extends StoryHandler<T, R> {

    Story<R> onIncident(@NotNull Throwable obstacle) throws Exception;
  }
}
