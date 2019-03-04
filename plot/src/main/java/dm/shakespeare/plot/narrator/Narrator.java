package dm.shakespeare.plot.narrator;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide-maestroni on 03/02/2019.
 */
public interface Narrator<T> extends Closeable {

  void close();

  boolean report(@NotNull Throwable incident, long timeout, @NotNull TimeUnit unit) throws
      InterruptedException;

  boolean tell(T effect, long timeout, @NotNull TimeUnit unit) throws InterruptedException;
}
