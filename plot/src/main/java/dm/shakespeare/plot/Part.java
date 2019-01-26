package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dm.shakespeare.function.Observer;
import dm.shakespeare.plot.function.NullaryFunction;
import dm.shakespeare.plot.function.UnaryFunction;

/**
 * Created by davide-maestroni on 01/22/2019.
 */
public abstract class Part<T> extends Line<Iterable<T>> {

  static final Object BREAK = new Object();
  static final Object NEXT = new Object();

  @NotNull
  public static <T1, R> Part<R> from(@NotNull Line<T1> firstLine,
      @NotNull final UnaryFunction<? super T1, ? extends Part<R>> messageHandler) {
    return null;
  }

  @NotNull
  public static <T, R> Part<R> scroll(
      @NotNull final NullaryFunction<? extends Part<? extends Boolean>> loopTester,
      @NotNull final UnaryFunction<? super T, ? extends Part<? extends R>> messageHandler) {
    return null;
  }

  @NotNull
  public static <T, R> Part<R> scroll(
      @NotNull final NullaryFunction<? extends Part<? extends Boolean>> loopTester,
      @NotNull final UnaryFunction<? super T, ? extends Part<? extends R>> messageHandler,
      @NotNull final UnaryFunction<? super Throwable, ? extends Part<? extends R>> errorHandler) {
    return null;
  }

  @NotNull
  public static <T, R> Part<R> scroll(@NotNull final PartHandler<? super T, R> partHandler) {
    return null;
  }

  @NotNull
  public static <T, R> Part<R> scroll(@NotNull final PartCorrector<? super T, R> partHandler) {
    return null;
  }

  @NotNull
  public static <T> Part<T> unfold(@NotNull final Line<? extends Iterable<T>> line) {
    return null;
  }

  public void readAll(@Nullable final Observer<? super T> valueObserver,
      @Nullable final Observer<? super Throwable> errorObserver) {
    readAll(new DefaultLineObserver<T>(valueObserver, errorObserver));
  }

  abstract void readAll(@NotNull final LineObserver<? super T> lineObserver);

  public interface PartCorrector<T, R> extends PartHandler<T, R> {

    Part<R> onError(@NotNull Throwable error) throws Exception;
  }

  public interface PartHandler<T, R> {

    Part<? extends Boolean> nextMessage() throws Exception;

    Part<R> onEnd() throws Exception;

    Part<R> onMessage(T message) throws Exception;
  }
}
