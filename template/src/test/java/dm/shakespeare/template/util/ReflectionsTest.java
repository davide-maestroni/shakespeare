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

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Reflections} unit tests.
 */
public class ReflectionsTest {

  @Test
  public void testAccessibleConstructor() throws NoSuchMethodException, IllegalAccessException,
      InvocationTargetException {
    final Method method =
        Reflections.makeAccessible(TestClass.class.getDeclaredMethod("run", List.class));
    method.invoke(new TestClass(), Collections.emptyList());
  }

  @Test
  public void testAccessibleMethod() throws NoSuchMethodException, IllegalAccessException,
      InvocationTargetException, InstantiationException {
    final Constructor<?> constructor =
        Reflections.makeAccessible(TestClass.class.getDeclaredConstructor(List.class));
    assertThat(constructor.newInstance(Collections.emptyList())).isExactlyInstanceOf(
        TestClass.class);
  }

  @Test
  public void testBoxingClass() {
    assertThat(Void.class.equals(Reflections.boxingClass(void.class))).isTrue();
    assertThat(Integer.class.equals(Reflections.boxingClass(int.class))).isTrue();
    assertThat(Byte.class.equals(Reflections.boxingClass(byte.class))).isTrue();
    assertThat(Boolean.class.equals(Reflections.boxingClass(boolean.class))).isTrue();
    assertThat(Character.class.equals(Reflections.boxingClass(char.class))).isTrue();
    assertThat(Short.class.equals(Reflections.boxingClass(short.class))).isTrue();
    assertThat(Long.class.equals(Reflections.boxingClass(long.class))).isTrue();
    assertThat(Float.class.equals(Reflections.boxingClass(float.class))).isTrue();
    assertThat(Double.class.equals(Reflections.boxingClass(double.class))).isTrue();
    assertThat(Reflections.class.equals(Reflections.boxingClass(Reflections.class))).isTrue();
  }

  @Test
  public void testFindConstructor() {
    assertThat(Reflections.bestMatchingConstructor(TestClass.class)).isNotNull();
    assertThat(Reflections.bestMatchingConstructor(TestClass.class, "test")).isNotNull();
    assertThat(
        Reflections.bestMatchingConstructor(TestClass.class, new ArrayList<String>())).isNotNull();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindConstructorError() {
    Reflections.bestMatchingConstructor(TestClass.class, 4);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindConstructorNullParamError() {
    Reflections.bestMatchingConstructor(TestClass.class, (Object) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindConstructorParamNumberError() {
    Reflections.bestMatchingConstructor(TestClass.class, "test", 4);
  }

  @Test
  public void testNewInstance() {
    assertThat(Reflections.newInstance(String.class)).isExactlyInstanceOf(String.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNewInstanceNoParams() {
    Reflections.newInstance(Integer.class);
  }

  @SuppressWarnings("unused")
  public static class TestClass {

    public TestClass() {
    }

    public TestClass(final String ignored) {
    }

    public TestClass(final int ignored) {
    }

    public TestClass(final Integer ignored) {
    }

    private TestClass(final LinkedList<String> ignored) {
    }

    private TestClass(final ArrayList<String> ignored) {
    }

    private TestClass(final List<String> ignored) {
    }

    public void run(final int ignore) {
    }

    public void run(final Integer ignored) {
    }

    public void run(final String ignored) {
    }

    protected void run() {
    }

    void run(final Exception ignored) {
    }

    private void run(final LinkedList<String> ignored) {
    }

    private void run(final ArrayList<String> ignored) {
    }

    private void run(final List<String> ignored) {
    }
  }
}
