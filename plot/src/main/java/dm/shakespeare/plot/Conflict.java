package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/25/2019.
 */
class Conflict {

  private final Throwable mIncident;

  Conflict(@NotNull final Throwable incident) {
    mIncident = ConstantConditions.notNull("incident", incident);
  }

  @NotNull
  Throwable getCause() {
    return mIncident;
  }
}
