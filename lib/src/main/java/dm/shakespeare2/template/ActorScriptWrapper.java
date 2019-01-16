package dm.shakespeare2.template;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

import dm.shakespeare.log.Logger;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare2.actor.ActorScript;
import dm.shakespeare2.actor.Behavior;

/**
 * Created by davide-maestroni on 01/16/2019.
 */
public class ActorScriptWrapper extends ActorScript {

  private final ActorScript mScript;

  public ActorScriptWrapper(@NotNull final ActorScript script) {
    mScript = ConstantConditions.notNull("script", script);
  }

  @NotNull
  public Behavior getBehavior(@NotNull final String id) throws Exception {
    return mScript.getBehavior(id);
  }

  @NotNull
  @Override
  public ExecutorService getExecutor(@NotNull final String id) throws Exception {
    return mScript.getExecutor(id);
  }

  @NotNull
  @Override
  public Logger getLogger(@NotNull final String id) throws Exception {
    return mScript.getLogger(id);
  }

  @Override
  public int getQuota(@NotNull final String id) throws Exception {
    return mScript.getQuota(id);
  }
}
