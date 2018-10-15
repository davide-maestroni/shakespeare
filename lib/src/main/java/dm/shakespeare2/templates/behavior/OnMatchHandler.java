package dm.shakespeare2.templates.behavior;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

import dm.shakespeare2.actor.Actor.Envelop;
import dm.shakespeare2.actor.Behavior.Context;
import dm.shakespeare2.actor.BehaviorBuilder;
import dm.shakespeare2.actor.BehaviorBuilder.Matcher;
import dm.shakespeare2.templates.annotation.OnMatch;
import dm.shakespeare2.templates.annotation.VoidMatcher;
import dm.shakespeare2.templates.util.Methods;

/**
 * Created by davide-maestroni on 09/07/2018.
 */
class OnMatchHandler implements AnnotationHandler<OnMatch> {

  @SuppressWarnings("unchecked")
  public void handle(@NotNull final BehaviorBuilder builder, @NotNull final Object object,
      @NotNull final Method method, @NotNull final OnMatch annotation) {
    Matcher<?> matcher = null;
    final Class<? extends Matcher<?>> matcherClass = annotation.matcherClass();
    final String name = annotation.matcherName();
    if (matcherClass != VoidMatcher.class) {
      if (!name.isEmpty()) {
        throw new IllegalArgumentException(
            "only one of matcherClass and matcherName parameters must be specified");
      }

      try {
        matcher = matcherClass.newInstance();

      } catch (final RuntimeException e) {
        throw e;

      } catch (final Exception e) {
        throw new RuntimeException(e);
      }

    } else {
      if (name.isEmpty()) {
        throw new IllegalArgumentException(
            "at least one of matcherClass and matcherName parameters must be specified");
      }

      for (final Method matcherMethod : object.getClass().getMethods()) {
        final Class<?> returnType = matcherMethod.getReturnType();
        final Class<?>[] parameterTypes = matcherMethod.getParameterTypes();
        if (name.equals(matcherMethod.getName()) && ((returnType == Boolean.class) || (returnType
            == boolean.class)) && (parameterTypes.length == 3)
            && parameterTypes[1].isAssignableFrom(Envelop.class)
            && parameterTypes[2].isAssignableFrom(Context.class)) {
          matcher = new MessageMatcher(object, matcherMethod);
          break;
        }
      }

      if (matcher == null) {
        throw new IllegalArgumentException("cannot find matcher method: " + name);
      }
    }

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

    builder.onMatch((Matcher<? super Object>) matcher, new MethodHandler(object, method));
  }

  private static class MessageMatcher implements Matcher<Object> {

    private final Method mMethod;
    private final Object mObject;

    private MessageMatcher(@NotNull final Object object, @NotNull final Method method) {
      mObject = object;
      mMethod = method;
    }

    public boolean match(final Object message, @NotNull final Envelop envelop,
        @NotNull final Context context) throws Exception {
      return (Boolean) Methods.makeAccessible(mMethod).invoke(mObject, message, envelop, context);
    }
  }
}
