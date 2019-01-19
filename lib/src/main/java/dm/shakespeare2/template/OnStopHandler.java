package dm.shakespeare2.template;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

import dm.shakespeare2.actor.Behavior.Context;
import dm.shakespeare2.actor.BehaviorBuilder;
import dm.shakespeare2.template.annotation.OnStop;

/**
 * Created by davide-maestroni on 09/07/2018.
 */
class OnStopHandler implements AnnotationHandler<OnStop> {

  @SuppressWarnings("unchecked")
  public void handle(@NotNull final BehaviorBuilder builder, @NotNull final Object object,
      @NotNull final Method method, @NotNull final OnStop annotation) {
    final String name = method.getName();
    final Class<?>[] parameterTypes = method.getParameterTypes();
    if (parameterTypes.length > 0) {
      for (final Class<?> parameterType : parameterTypes) {
        if (parameterType != Context.class) {
          throw new IllegalArgumentException("invalid method parameters: " + name);
        }
      }
    }
    builder.onStop(new MethodObserver(object, method));
  }
}
