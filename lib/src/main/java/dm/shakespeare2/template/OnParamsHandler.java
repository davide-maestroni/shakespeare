package dm.shakespeare2.template;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import dm.shakespeare.function.Tester;
import dm.shakespeare.templates.message.ParameterizedMessage;
import dm.shakespeare2.actor.Behavior.Context;
import dm.shakespeare2.actor.BehaviorBuilder;
import dm.shakespeare2.actor.BehaviorBuilder.Handler;
import dm.shakespeare2.actor.Envelop;
import dm.shakespeare2.template.annotation.OnParams;

/**
 * Created by davide-maestroni on 09/07/2018.
 */
class OnParamsHandler implements AnnotationHandler<OnParams> {

  public void handle(@NotNull final BehaviorBuilder builder, @NotNull final Object object,
      @NotNull final Method method, @NotNull final OnParams annotation) {
    builder.onMessage(new MessageTester(annotation), new MessageHandler(object, method));
  }

  private static class MessageHandler implements Handler<ParameterizedMessage> {

    private final Method mMethod;
    private final Object mObject;

    private MessageHandler(@NotNull final Object object, @NotNull final Method method) {
      mObject = object;
      mMethod = method;
    }

    public void handle(final ParameterizedMessage message, @NotNull final Envelop envelop,
        @NotNull final Context context) throws Exception {
      final Method method = mMethod;
      final Class<?>[] parameterTypes = method.getParameterTypes();
      final int length = parameterTypes.length;
      List<?> args = message.getParams();
      final int size = args.size();
      if (length > size) {
        final ArrayList<Object> parameters = new ArrayList<Object>(args);
        for (int i = size; i < length; ++i) {
          final Class<?> parameterType = parameterTypes[i];
          if (parameterType == Envelop.class) {
            parameters.add(envelop);

          } else if (parameterType == Context.class) {
            parameters.add(context);

          } else {
            throw new IllegalArgumentException("invalid method parameter: " + parameterType);
          }
        }

        args = parameters;
      }
      final Object result = Methods.makeAccessible(method).invoke(mObject, args.toArray());
      MethodHandler.handleReturnValue(method, result, envelop, context);
    }
  }

  private static class MessageTester implements Tester<Object> {

    private final String mName;

    private MessageTester(@NotNull final OnParams annotation) {
      mName = annotation.value();
    }

    public boolean test(final Object message) {
      return (message instanceof ParameterizedMessage) && mName.equals(
          ((ParameterizedMessage) message).getType());
    }
  }
}
