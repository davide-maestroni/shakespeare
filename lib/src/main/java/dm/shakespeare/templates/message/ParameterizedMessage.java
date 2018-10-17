package dm.shakespeare.templates.message;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.Iterables;

/**
 * Created by davide-maestroni on 07/16/2018.
 */
public class ParameterizedMessage {

  private final List<?> mParams;
  private final String mType;

  public ParameterizedMessage(@NotNull final String type, @NotNull final Iterable<?> params) {
    mType = ConstantConditions.notNull("type", type);
    mParams = Collections.unmodifiableList(Iterables.toList(params));
  }

  public ParameterizedMessage(@NotNull final String type, @NotNull final Object... params) {
    mType = ConstantConditions.notNull("type", type);
    mParams = Arrays.asList(params);
  }

  @NotNull
  public List<?> getParams() {
    return mParams;
  }

  @NotNull
  public String getType() {
    return mType;
  }
}
