/*
 * Copyright 2019 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dm.shakespeare.template.behavior;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.lang.reflect.Method;

import dm.shakespeare.actor.Behavior.Agent;
import dm.shakespeare.actor.BehaviorBuilder;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.function.Tester;
import dm.shakespeare.template.behavior.annotation.OnMessage;
import dm.shakespeare.template.behavior.annotation.VoidTester;
import dm.shakespeare.template.config.BuildConfig;
import dm.shakespeare.template.util.Reflections;
import dm.shakespeare.util.ConstantConditions;

/**
 * {@code AnnotationHandler} handling {@link OnMessage} annotations.
 */
class OnMessageHandler implements AnnotationHandler<OnMessage> {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

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
        if ((parameterType != Envelop.class) && (parameterType != Agent.class)) {
          throw new IllegalArgumentException("invalid method parameters: " + name);
        }
      }
    }
    builder.onMessage((Tester<? super Object>) tester,
        new dm.shakespeare.template.behavior.MethodHandler(object, method));
  }

  private static class ClassTester implements Tester<Object>, Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final Class<?> messageClass;

    private ClassTester() {
      messageClass = ClassTester.class;
    }

    private ClassTester(@NotNull final Class<?> messageClass) {
      this.messageClass = messageClass;
    }

    public Class<?> getMessageClass() {
      return messageClass;
    }

    public boolean test(final Object message) {
      return messageClass.isInstance(message);
    }
  }

  private static class ClassesTester implements Tester<Object>, Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final Class<?>[] messageClasses;

    private ClassesTester() {
      messageClasses = new Class<?>[0];
    }

    private ClassesTester(@NotNull final Class<?>[] messageClasses) {
      this.messageClasses = messageClasses;
    }

    public Class<?>[] getMessageClasses() {
      return messageClasses;
    }

    public boolean test(final Object message) {
      for (final Class<?> messageClass : messageClasses) {
        if (messageClass.isInstance(message)) {
          return true;
        }
      }
      return false;
    }
  }

  private static class MessageTester implements Tester<Object>, Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final Method method;
    private final Object object;

    private MessageTester() {
      object = null;
      method = null;
    }

    private MessageTester(@NotNull final Object object, @NotNull final Method method) {
      this.object = object;
      this.method =  ConstantConditions.notNull("method", method);
    }

    public Method getMethod() {
      return method;
    }

    public Object getObject() {
      return object;
    }

    public boolean test(final Object message) throws Exception {
      return (Boolean) method.invoke(object, message);
    }
  }
}
