package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.message.Bounce;
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
  static Conflict ofBounce(@NotNull final Bounce message) {
    return new Conflict(PlotStateException.getOrNew(message));
  }

  @NotNull
  static Conflict ofCancel() {
    return new Conflict(new PlotCancelledException());
  }

  @NotNull
  Throwable getCause() {
    return mIncident;
  }
}
