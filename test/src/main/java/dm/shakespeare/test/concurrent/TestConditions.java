/*
 * Copyright 2018 Davide Maestroni
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

package dm.shakespeare.test.concurrent;

import org.jetbrains.annotations.NotNull;

/**
 * Utility class for verifying constant conditions.
 * <p>
 * Created by davide-maestroni on 03/27/2016.
 */
class TestConditions {

  /**
   * Avoid explicit instantiation.
   */
  private TestConditions() {
  }

  /**
   * Asserts that the specified object is not null.
   *
   * @param object the object.
   * @param <T>    the object type.
   * @return the object.
   * @throws NullPointerException is the object is null.
   */
  @NotNull
  public static <T> T notNull(final T object) {
    return notNull("object", object);
  }

  /**
   * Asserts that the specified object is not null.
   *
   * @param name   the name of the parameter used to build the error message.
   * @param object the object.
   * @param <T>    the object type.
   * @return the object.
   * @throws NullPointerException is the object is null.
   */
  @NotNull
  public static <T> T notNull(final String name, final T object) {
    if (object == null) {
      throw new NullPointerException("the " + name + " must not be null");
    }
    return object;
  }

  /**
   * Asserts that the specified number is positive.
   *
   * @param number the number.
   * @return the number.
   * @throws IllegalArgumentException if the number is 0 or negative.
   */
  public static int positive(final int number) {
    return positive("number", number);
  }

  /**
   * Asserts that the specified number is positive.
   *
   * @param name   the name of the parameter used to build the error message.
   * @param number the number.
   * @return the number.
   * @throws IllegalArgumentException if the number is 0 or negative.
   */
  public static int positive(final String name, final int number) {
    return (int) positive(name, (long) number);
  }

  /**
   * Asserts that the specified number is positive.
   *
   * @param number the number.
   * @return the number.
   * @throws IllegalArgumentException if the number is 0 or negative.
   */
  public static long positive(final long number) {
    return positive("number", number);
  }

  /**
   * Asserts that the specified number is positive.
   *
   * @param name   the name of the parameter used to build the error message.
   * @param number the number.
   * @return the number.
   * @throws IllegalArgumentException if the number is 0 or negative.
   */
  public static long positive(final String name, final long number) {
    if (number <= 0) {
      throw new IllegalArgumentException("the " + name + " must be positive, but is: " + number);
    }
    return number;
  }
}
