package dm.shakespeare.actor;

import org.jetbrains.annotations.NotNull;

/**
 * Created by davide-maestroni on 01/17/2019.
 */
public abstract class AbstractBehavior implements Behavior {

  public void onStart(@NotNull final Context context) throws Exception {
  }

  public void onStop(@NotNull final Context context) throws Exception {
  }
}
