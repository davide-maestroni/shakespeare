package dm.shakespeare.templates.behavior;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Collections;

import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.function.Observer;
import dm.shakespeare.templates.util.Methods;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 09/12/2018.
 */
class MethodObserver implements Observer<Context> {

  private static final Object[] EMPTY_ARGS = new Object[0];

  private final Method mMethod;
  private final Object mObject;

  MethodObserver(@NotNull final Object object, @NotNull final Method method) {
    mObject = ConstantConditions.notNull("object", object);
    mMethod = ConstantConditions.notNull("method", method);
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

    Methods.makeAccessible(method).invoke(mObject, args);
  }
}
