package dm.shakespeare.template;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ExecutorService;

import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Script;
import dm.shakespeare.actor.SerializableScript;
import dm.shakespeare.log.Logger;
import dm.shakespeare.template.config.BuildConfig;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/16/2019.
 */
public class SerializableScriptWrapper extends SerializableScript {

  private static final long serialVersionUID = BuildConfig.VERSION_HASH_CODE;

  private transient Script mScript;

  public SerializableScriptWrapper(@NotNull final Script script) {
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

  @SuppressWarnings("unchecked")
  private void readObject(@NotNull final ObjectInputStream in) throws IOException,
      ClassNotFoundException {
    in.defaultReadObject();
    mScript = (Script) in.readObject();
  }

  private void writeObject(@NotNull final ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    out.writeObject(mScript);
  }
}
