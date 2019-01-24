package dm.shakespeare.actor;

import java.io.Serializable;

import dm.shakespeare.config.BuildConfig;

/**
 * Created by davide-maestroni on 01/24/2019.
 */
public abstract class SerializableScript extends Script implements Serializable {

  private static final long serialVersionUID = BuildConfig.VERSION_HASH_CODE;
}
