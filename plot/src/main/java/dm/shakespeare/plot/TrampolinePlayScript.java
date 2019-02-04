package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

import dm.shakespeare.actor.Script;
import dm.shakespeare.log.Logger;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/25/2019.
 */
abstract class TrampolinePlayScript extends Script {

  private final Setting mSetting;

  TrampolinePlayScript(@NotNull final Setting setting) {
    mSetting = ConstantConditions.notNull("setting", setting);
  }

  @NotNull
  @Override
  public ExecutorService getExecutor(@NotNull final String id) {
    return mSetting.getTrampolineExecutor();
  }

  @NotNull
  @Override
  public Logger getLogger(@NotNull final String id) throws Exception {
    final Logger logger = mSetting.getLogger();
    return (logger != null) ? logger : super.getLogger(id);
  }
}
