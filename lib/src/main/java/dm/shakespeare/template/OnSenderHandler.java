package dm.shakespeare.template;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.actor.BehaviorBuilder;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.function.Tester;
import dm.shakespeare.template.annotation.OnSender;
import dm.shakespeare.template.annotation.VoidTester;

/**
 * Created by davide-maestroni on 09/07/2018.
 */
class OnSenderHandler implements AnnotationHandler<OnSender> {

  public void handle(@NotNull final BehaviorBuilder builder, @NotNull final Object object,
      @NotNull final Method method, @NotNull final OnSender annotation) throws Exception {
    Tester<? super Envelop> tester = null;
    final Class<? extends Tester<? super Envelop>> testerClass = annotation.testerClass();
    final String name = annotation.testerName();
    if (testerClass != VoidTester.class) {
      if (!name.isEmpty()) {
        throw new IllegalArgumentException(
            "only one of idRegexp, testerClass and testerName parameters must be specified");
      }
      tester = testerClass.newInstance();

    } else {
      if (name.isEmpty()) {
        throw new IllegalArgumentException(
            "at least one of idRegexp, testerClass and testerName parameters must be specified");
      }

      for (final Method testerMethod : object.getClass().getMethods()) {
        final Class<?> returnType = testerMethod.getReturnType();
        final Class<?>[] parameterTypes = testerMethod.getParameterTypes();
        if (name.equals(testerMethod.getName()) && ((returnType == Boolean.class) || (returnType
            == boolean.class)) && (parameterTypes.length == 1)
            && parameterTypes[0].isAssignableFrom(Envelop.class)) {
          tester = new MessageTester(object, testerMethod);
          break;
        }
      }

      if (tester == null) {
        throw new IllegalArgumentException("cannot find tester method: " + name);
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
    builder.onSender(tester, new MethodHandler(object, method));
  }

  private static class MessageTester implements Tester<Envelop> {

    private final Method mMethod;
    private final Object mObject;

    private MessageTester(@NotNull final Object object, @NotNull final Method method) {
      mObject = object;
      mMethod = Methods.makeAccessible(method);
    }

    public boolean test(final Envelop envelop) throws Exception {
      return (Boolean) mMethod.invoke(mObject, envelop);
    }
  }
}
