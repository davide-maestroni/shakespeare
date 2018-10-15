package dm.shakespeare2.templates.behavior;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;

import dm.shakespeare2.actor.Actor.Envelop;
import dm.shakespeare2.actor.Behavior.Context;
import dm.shakespeare2.actor.BehaviorBuilder.Handler;
import dm.shakespeare2.templates.util.Methods;
import dm.shakespeare2.util.ConstantConditions;

/**
 * Created by davide-maestroni on 09/12/2018.
 */
class MethodHandler implements Handler<Object> {

  private final Method mMethod;
  private final Object mObject;

  MethodHandler(@NotNull final Object object, @NotNull final Method method) {
    mObject = ConstantConditions.notNull("object", object);
    mMethod = ConstantConditions.notNull("method", method);
  }

  static void handleReturnValue(@NotNull final Method method, final Object value,
      @NotNull final Envelop envelop, @NotNull final Context context) {
    final Class<?> returnType = method.getReturnType();
    if ((returnType != void.class) && (returnType != Void.class)) {
      // TODO: 31/08/2018 specific message?
      envelop.getSender().tell(value, context.getSelf());
    }
  }

  public void handle(final Object message, @NotNull final Envelop envelop,
      @NotNull final Context context) throws Exception {
    final Method method = mMethod;
    final Class<?>[] parameterTypes = method.getParameterTypes();
    final int length = parameterTypes.length;
    final Object[] args;
    if (length > 1) {
      final ArrayList<Object> parameters = new ArrayList<Object>(length);
      parameters.add(message);
      for (int i = 1; i < length; ++i) {
        if (parameterTypes[i] == Envelop.class) {
          parameters.add(envelop);

        } else if (parameterTypes[i] == Context.class) {
          parameters.add(context);
        }
      }

      args = parameters.toArray();

    } else {
      args = new Object[]{message};
    }

    final Object result = Methods.makeAccessible(method).invoke(mObject, args);
    handleReturnValue(method, result, envelop, context);
  }
}
