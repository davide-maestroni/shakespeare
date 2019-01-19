package dm.shakespeare2.template;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Collections;

import dm.shakespeare.function.Observer;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare2.actor.Behavior.Context;

/**
 * Created by davide-maestroni on 09/12/2018.
 */
class MethodObserver implements Observer<Context> {

  private static final Object[] EMPTY_ARGS = new Object[0];

  private final Method mMethod;
  private final Object mObject;

  MethodObserver(@NotNull final Object object, @NotNull final Method method) {
    mObject = ConstantConditions.notNull("object", object);
    mMethod = Methods.makeAccessible(method);
  }

  public void accept(final Context context) throws Exception {
    final Method method = mMethod;
    final Class<?>[] parameterTypes = method.getParameterTypes();
    final int length = parameterTypes.length;
    final Object[] args;
    if (length > 0) {
      if (length == 1) {
        args = new Object[]{context};

      } else {
        args = Collections.nCopies(length, context).toArray();
      }

    } else {
      args = EMPTY_ARGS;
    }
    method.invoke(mObject, args);
  }
}
