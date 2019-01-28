package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/25/2019.
 */
class Incident {

  private final Throwable mObstacle;

  Incident(@NotNull final Throwable obstacle) {
    mObstacle = ConstantConditions.notNull("obstacle", obstacle);
  }

  @NotNull
  Throwable getCause() {
    return mObstacle;
  }
}
