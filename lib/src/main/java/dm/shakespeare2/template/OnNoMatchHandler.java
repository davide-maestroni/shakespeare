package dm.shakespeare2.template;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

import dm.shakespeare2.actor.Behavior.Context;
import dm.shakespeare2.actor.BehaviorBuilder;
import dm.shakespeare2.actor.Envelop;
import dm.shakespeare2.template.annotation.OnNoMatch;

/**
 * Created by davide-maestroni on 09/07/2018.
 */
class OnNoMatchHandler implements AnnotationHandler<OnNoMatch> {

  @SuppressWarnings("unchecked")
  public void handle(@NotNull final BehaviorBuilder builder, @NotNull final Object object,
      @NotNull final Method method, @NotNull final OnNoMatch annotation) {
    final String name = method.getName();
    final Class<?>[] parameterTypes = method.getParameterTypes();
    final int length = parameterTypes.length;
    if (length > 1) {
      for (int i = 1; i < length; ++i) {
        final Class<?> parameterType = parameterTypes[i];
        if ((parameterType != Envelop.class) && (parameterType != Context.class)) {
          throw new IllegalArgumentException("invalid method parameters: " + name);
        }
      }
    }
    builder.onNoMatch(new MethodHandler(object, method));
  }
}
