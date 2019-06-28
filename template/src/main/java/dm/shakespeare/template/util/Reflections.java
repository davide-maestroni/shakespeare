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

package dm.shakespeare.template.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashMap;

import dm.shakespeare.util.ConstantConditions;

/**
 * Reflection utility class.
 */
public class Reflections {

  private static final HashMap<Class<?>, Class<?>> BOXING_CLASSES =
      new HashMap<Class<?>, Class<?>>(9) {{
        put(boolean.class, Boolean.class);
        put(byte.class, Byte.class);
        put(char.class, Character.class);
        put(double.class, Double.class);
        put(float.class, Float.class);
        put(int.class, Integer.class);
        put(long.class, Long.class);
        put(short.class, Short.class);
        put(void.class, Void.class);
      }};

  /**
   * Avoid explicit instantiation.
   */
  private Reflections() {
    ConstantConditions.avoid();
  }

  /**
   * Finds the constructor of the specified class best matching the passed arguments.<br>
   * Clashing of signature is automatically avoided, since constructors are not identified by their
   * name. Hence the best match will always be unique in the class.
   *
   * @param type   the target class.
   * @param args   the constructor arguments.
   * @param <TYPE> the target type.
   * @return the best matching constructor.
   * @throws IllegalArgumentException if no constructor taking the specified objects as parameters
   *                                  was found.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static <TYPE> Constructor<TYPE> bestMatchingConstructor(@NotNull final Class<TYPE> type,
      @NotNull final Object... args) {
    Constructor<?> constructor = bestMatchingConstructor(type.getConstructors(), args);
    if (constructor == null) {
      constructor = bestMatchingConstructor(type.getDeclaredConstructors(), args);
      if (constructor == null) {
        throw new IllegalArgumentException(
            "no suitable constructor found for type: " + type.getName());
      }
    }
    return (Constructor<TYPE>) constructor;
  }

  /**
   * Returns the class boxing the specified primitive type.<br>
   * If the class does not represent a primitive type, the same object is returned.
   *
   * @param type the primitive type.
   * @return the boxing class.
   */
  @NotNull
  public static Class<?> boxingClass(@NotNull final Class<?> type) {
    if (!type.isPrimitive()) {
      return type;
    }
    return BOXING_CLASSES.get(type);
  }

  /**
   * Makes the specified constructor accessible.
   *
   * @param constructor the constructor instance.
   * @return the constructor.
   */
  @NotNull
  public static Constructor<?> makeAccessible(@NotNull final Constructor<?> constructor) {
    if (!constructor.isAccessible()) {
      AccessController.doPrivileged(new SetAccessibleConstructorAction(constructor));
    }
    return constructor;
  }

  /**
   * Makes the specified method accessible.
   *
   * @param method the method instance.
   * @return the method.
   */
  @NotNull
  public static Method makeAccessible(@NotNull final Method method) {
    if (!method.isAccessible()) {
      AccessController.doPrivileged(new SetAccessibleMethodAction(method));
    }
    return method;
  }

  /**
   * Creates a new instance of the specified class by invoking its constructor best matching the
   * specified arguments.
   *
   * @param type   the target class.
   * @param args   the constructor arguments.
   * @param <TYPE> the target type.
   * @return the new instance.
   * @throws IllegalArgumentException if no matching constructor was found or an error occurred
   *                                  during the instantiation.
   */
  @NotNull
  public static <TYPE> TYPE newInstance(@NotNull final Class<TYPE> type,
      @NotNull final Object... args) {
    try {
      return bestMatchingConstructor(type, args).newInstance(args);

    } catch (final Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Nullable
  private static Constructor<?> bestMatchingConstructor(
      @NotNull final Constructor<?>[] constructors, @NotNull final Object[] args) {
    Constructor<?> bestMatch = null;
    boolean isClash = false;
    int maxConfidence = -1;
    for (final Constructor<?> constructor : constructors) {
      final Class<?>[] params = constructor.getParameterTypes();
      final int confidence = computeConfidence(params, args);
      if (confidence < 0) {
        continue;
      }

      if ((bestMatch == null) || (confidence > maxConfidence)) {
        isClash = false;
        bestMatch = constructor;
        maxConfidence = confidence;

      } else if (confidence == maxConfidence) {
        isClash = true;
      }
    }

    if (isClash) {
      throw new IllegalArgumentException(
          "more than one constructor found for arguments: " + Arrays.toString(args));
    }
    return bestMatch;
  }

  private static int computeConfidence(@NotNull final Class<?>[] params,
      @NotNull final Object[] args) {
    final int length = params.length;
    final int argsLength = args.length;
    if (length != argsLength) {
      return -1;
    }
    int confidence = 0;
    for (int i = 0; i < argsLength; ++i) {
      final Object arg = args[i];
      final Class<?> param = params[i];
      if (arg != null) {
        final Class<?> boxingClass = boxingClass(param);
        if (!boxingClass.isInstance(arg)) {
          confidence = -1;
          break;
        }

        if (arg.getClass().equals(boxingClass)) {
          ++confidence;
        }

      } else if (param.isPrimitive()) {
        confidence = -1;
        break;
      }
    }
    return confidence;
  }

  private static class SetAccessibleConstructorAction implements PrivilegedAction<Void> {

    private final Constructor<?> constructor;

    private SetAccessibleConstructorAction(@NotNull final Constructor<?> constructor) {
      this.constructor = constructor;
    }

    public Void run() {
      constructor.setAccessible(true);
      return null;
    }
  }

  private static class SetAccessibleMethodAction implements PrivilegedAction<Void> {

    private final Method method;

    private SetAccessibleMethodAction(@NotNull final Method method) {
      this.method = method;
    }

    public Void run() {
      method.setAccessible(true);
      return null;
    }
  }
}
