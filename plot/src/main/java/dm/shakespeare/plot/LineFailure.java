package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/25/2019.
 */
class LineFailure {

  private final Throwable mError;

  LineFailure(@NotNull final Throwable error) {
    mError = ConstantConditions.notNull("error", error);
  }

  @NotNull
  Throwable getCause() {
    return mError;
  }
}
