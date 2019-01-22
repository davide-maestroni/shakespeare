package dm.shakespeare.template;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Script;
import dm.shakespeare.config.BuildConfig;
import dm.shakespeare.log.Logger;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/16/2019.
 */
public class ScriptWrapper extends Script {

  private static final long serialVersionUID = BuildConfig.VERSION_HASH_CODE;

  private final Script mScript;

  public ScriptWrapper(@NotNull final Script script) {
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
