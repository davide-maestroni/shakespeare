package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dm.shakespeare.function.Observer;
import dm.shakespeare.plot.function.UnaryFunction;

/**
 * Created by davide-maestroni on 01/22/2019.
 */
public abstract class Line<T> {

  @NotNull
  public static <T1 extends Throwable, R> Line<R> correct(@NotNull Class<T1> firstError,
      @NotNull UnaryFunction<? super T1, ? extends Line<R>> errorHandler) {
    return null;
  }

  @NotNull
  public static <T1, R> Line<R> when(@NotNull Line<T1> firstLine,
      @NotNull UnaryFunction<? super T1, ? extends Line<R>> messageHandler) {
    return null;
  }

  abstract void read(@NotNull LineObserver<? super T> lineObserver);

  abstract void read(@Nullable Observer<? super T> valueObserver,
      @Nullable Observer<? super Throwable> errorObserver);

  public interface LineObserver<T> {

    void onError(@NotNull Throwable error) throws Exception;

    void onMessage(T message) throws Exception;
  }
}
