package dm.shakespeare.message;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import dm.shakespeare.config.BuildConfig;

/**
 * Created by davide-maestroni on 09/06/2018.
 */
public class GenericMessage extends HashMap<String, Object> {

  private static final long serialVersionUID = BuildConfig.VERSION_HASH_CODE;

  private String mId;

  public String getId() {
    return mId;
  }

  @NotNull
  public GenericMessage withAttribute(final String key, final Object value) {
    put(key, value);
    return this;
  }

  @NotNull
  public GenericMessage withAttributes(@NotNull final Map<? extends String, ?> attributes) {
    putAll(attributes);
    return this;
  }

  @NotNull
  public GenericMessage withId(final String id) {
    mId = id;
    return this;
  }
}
