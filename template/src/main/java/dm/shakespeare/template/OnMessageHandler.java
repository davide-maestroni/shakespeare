package dm.shakespeare.template;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.actor.BehaviorBuilder;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.function.Tester;
import dm.shakespeare.template.annotation.OnMessage;
import dm.shakespeare.template.annotation.VoidTester;

/**
 * Created by davide-maestroni on 09/07/2018.
 */
class OnMessageHandler implements AnnotationHandler<OnMessage> {

  @SuppressWarnings("unchecked")
  public void handle(@NotNull final BehaviorBuilder builder, @NotNull final Object object,
      @NotNull final Method method, @NotNull final OnMessage annotation) throws Exception {
    Tester<?> tester = null;
    final Class<?>[] messageClasses = annotation.messageClasses();
    final Class<? extends Tester<?>> testerClass = annotation.testerClass();
    final String name = annotation.testerName();
    if (messageClasses.length > 0) {
      if ((testerClass != VoidTester.class) || !name.isEmpty()) {
        throw new IllegalArgumentException(
            "only one of messageClasses, testerClass and testerName parameters must be specified");
      }

      if (messageClasses.length == 1) {
        tester = new ClassTester(messageClasses[0]);

      } else {
        tester = new ClassesTester(messageClasses);
      }

    } else if (testerClass != VoidTester.class) {
      if (!name.isEmpty()) {
        throw new IllegalArgumentException(
            "only one of messageClasses, testerClass and testerName parameters must be specified");
      }
      tester = testerClass.newInstance();

    } else {
      if (name.isEmpty()) {
        throw new IllegalArgumentException(
            "at least one of messageClasses, testerClass and testerName parameters must be "
                + "specified");
      }

      for (final Method testerMethod : object.getClass().getMethods()) {
        final Class<?> returnType = testerMethod.getReturnType();
        final Class<?>[] parameterTypes = testerMethod.getParameterTypes();
        if (name.equals(testerMethod.getName()) && ((returnType == Boolean.class) || (returnType
            == boolean.class)) && (parameterTypes.length == 1)) {
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
    builder.onMessage((Tester<? super Object>) tester, new MethodHandler(object, method));
  }

  private static class ClassTester implements Tester<Object> {

    private final Class<?> mClass;

    private ClassTester(@NotNull final Class<?> messageClass) {
      mClass = messageClass;
    }

    public boolean test(final Object message) {
      return mClass.isInstance(message);
    }
  }

  private static class ClassesTester implements Tester<Object> {

    private final Class<?>[] mClasses;

    private ClassesTester(@NotNull final Class<?>[] messageClasses) {
      mClasses = messageClasses;
    }

    public boolean test(final Object message) {
      for (final Class<?> messageClass : mClasses) {
        if (messageClass.isInstance(message)) {
          return true;
        }
      }
      return false;
    }
  }

  private static class MessageTester implements Tester<Object> {

    private final Method mMethod;
    private final Object mObject;

    private MessageTester(@NotNull final Object object, @NotNull final Method method) {
      mObject = object;
      mMethod = Methods.makeAccessible(method);
    }

    public boolean test(final Object message) throws Exception {
      return (Boolean) mMethod.invoke(mObject, message);
    }
  }
}
