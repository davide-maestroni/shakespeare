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

  @NotNull
  public static <T1, R> Part<R> from(@NotNull Line<T1> firstLine,
      @NotNull UnaryFunction<? super T1, ? extends Part<R>> messageHandler) {
    return null;
  }

  @NotNull
  public static <T, R> Part<R> scroll(
      @NotNull NullaryFunction<? extends Part<? extends Boolean>> loopTester,
      @NotNull UnaryFunction<? super T, ? extends Part<R>> messageHandler) {
    return null;
  }

  @NotNull
  public static <T, R> Part<R> scroll(
      @NotNull NullaryFunction<? extends Part<? extends Boolean>> loopTester,
      @NotNull UnaryFunction<? super T, ? extends Part<? extends R>> messageHandler,
      @NotNull UnaryFunction<? super Throwable, ? extends Part<? extends R>> errorHandler) {
    return null;
  }

  @NotNull
  public static <T, R> Part<R> scroll(@NotNull PartHandler<? super T, R> partHandler) {
    return null;
  }

  @NotNull
  public static <T, R> Part<R> scroll(@NotNull PartCorrector<? super T, R> partHandler) {
    return null;
  }

  abstract void readAll(@NotNull LineObserver<? super T> lineObserver);

  abstract void readAll(@Nullable Observer<? super T> valueObserver,
      @Nullable Observer<? super Throwable> errorObserver);

  public interface PartCorrector<T, R> extends PartHandler<T, R> {

    Part<R> onError(@NotNull Throwable error) throws Exception;
  }

  public interface PartHandler<T, R> {

    Part<? extends Boolean> nextMessage() throws Exception;

    Part<R> onMessage(T message) throws Exception;
  }
}
