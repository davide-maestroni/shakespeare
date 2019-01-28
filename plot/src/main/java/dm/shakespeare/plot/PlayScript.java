package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

import dm.shakespeare.actor.Script;
import dm.shakespeare.log.Logger;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/25/2019.
 */
abstract class PlayScript extends Script {

  private final PlayContext mPlayContext;

  PlayScript(@NotNull final PlayContext playContext) {
    mPlayContext = ConstantConditions.notNull("playContext", playContext);
  }

  @NotNull
  @Override
  public ExecutorService getExecutor(@NotNull final String id) throws Exception {
    final ExecutorService executor = mPlayContext.getExecutor();
    return (executor != null) ? executor : super.getExecutor(id);
  }

  @NotNull
  @Override
  public Logger getLogger(@NotNull final String id) throws Exception {
    final Logger logger = mPlayContext.getLogger();
    return (logger != null) ? logger : super.getLogger(id);
  }
}
