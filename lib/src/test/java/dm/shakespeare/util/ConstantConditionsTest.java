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

package dm.shakespeare.util;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * {@link ConstantConditions} unit tests.
 */
public class ConstantConditionsTest {

  @Test
  public void notNegativeInteger() {
    assertThat(ConstantConditions.notNegative(17)).isEqualTo(17);
  }

  @Test
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void notNegativeIntegerFailure() {
    try {
      ConstantConditions.notNegative(-71);
      fail();

    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("number");
    }
  }

  @Test
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void notNegativeIntegerFailureNamed() {
    try {
      ConstantConditions.notNegative("test", -1);
      fail();

    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("test");
    }
  }

  @Test
  public void notNegativeIntegerNamed() {
    assertThat(ConstantConditions.notNegative("test", 0)).isEqualTo(0);
  }

  @Test
  public void notNegativeLong() {
    assertThat(ConstantConditions.notNegative(17L)).isEqualTo(17L);
  }

  @Test
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void notNegativeLongFailure() {
    try {
      ConstantConditions.notNegative(-71L);
      fail();

    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("number");
    }
  }

  @Test
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void notNegativeLongFailureNamed() {
    try {
      ConstantConditions.notNegative("test", -1L);
      fail();

    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("test");
    }
  }

  @Test
  public void notNegativeLongNamed() {
    assertThat(ConstantConditions.notNegative("test", 0L)).isEqualTo(0L);
  }

  @Test
  public void notNullElementsArray() {
    assertThat(ConstantConditions.notNullElements(new Object[0])).isEqualTo(new Object[0]);
  }

  @Test
  public void notNullElementsArrayFailure() {
    try {
      ConstantConditions.notNullElements(new Object[]{1, null});
      fail();

    } catch (final NullPointerException e) {
      assertThat(e.getMessage()).contains("objects");
    }
  }

  @Test
  public void notNullElementsArrayFailureNamed() {
    try {
      ConstantConditions.notNullElements("test", new Object[]{1, null});
      fail();

    } catch (final NullPointerException e) {
      assertThat(e.getMessage()).contains("test");
    }
  }

  @Test
  public void notNullElementsArrayNamed() {
    assertThat(ConstantConditions.notNullElements("test", new Object[0])).isEqualTo(new Object[0]);
  }

  @Test
  public void notNullElementsGenericIterable() {
    final GenericIterable<Object> iterable = new GenericIterable<Object>(Collections.emptyList());
    assertThat(ConstantConditions.notNullElements(iterable)).isSameAs(iterable);
  }

  @Test
  public void notNullElementsGenericIterableFailure() {
    final GenericIterable<Integer> iterable = new GenericIterable<Integer>(Arrays.asList(1, null));
    try {
      ConstantConditions.notNullElements(iterable);
      fail();

    } catch (final NullPointerException e) {
      assertThat(e.getMessage()).contains("objects");
    }
  }

  @Test
  public void notNullElementsIterable() {
    assertThat(ConstantConditions.notNullElements(Collections.emptyList())).isSameAs(
        Collections.emptyList());
  }

  @Test
  public void notNullElementsIterableFailure() {
    try {
      ConstantConditions.notNullElements(Arrays.asList(1, null));
      fail();

    } catch (final NullPointerException e) {
      assertThat(e.getMessage()).contains("objects");
    }
  }

  @Test
  public void notNullElementsIterableFailureNamed() {
    try {
      ConstantConditions.notNullElements("test", Arrays.asList(1, null));
      fail();

    } catch (final NullPointerException e) {
      assertThat(e.getMessage()).contains("test");
    }
  }

  @Test
  public void notNullElementsIterableNamed() {
    assertThat(ConstantConditions.notNullElements("test", Collections.emptyList())).isSameAs(
        Collections.emptyList());
  }

  @Test
  public void nullity() {
    assertThat(ConstantConditions.notNull(this)).isSameAs(this);
  }

  @Test
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void nullityFailure() {
    try {
      ConstantConditions.notNull(null);
      fail();

    } catch (final NullPointerException e) {
      assertThat(e.getMessage()).contains("object");
    }
  }

  @Test
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void nullityFailureNamed() {
    try {
      ConstantConditions.notNull("test", null);
      fail();

    } catch (final NullPointerException e) {
      assertThat(e.getMessage()).contains("test");
    }
  }

  @Test
  @SuppressWarnings("ObviousNullCheck")
  public void nullityNamed() {
    assertThat(ConstantConditions.notNull("test", this)).isSameAs(this);
  }

  @Test
  public void positiveInteger() {
    assertThat(ConstantConditions.positive(17)).isEqualTo(17);
  }

  @Test
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void positiveIntegerFailure() {
    try {
      ConstantConditions.positive(-71);
      fail();

    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("number");
    }
  }

  @Test
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void positiveIntegerFailureNamed() {
    try {
      ConstantConditions.positive("test", -13);
      fail();

    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("test");
    }
  }

  @Test
  public void positiveIntegerNamed() {
    assertThat(ConstantConditions.positive("test", 11)).isEqualTo(11);
  }

  @Test
  public void positiveLong() {
    assertThat(ConstantConditions.positive(17L)).isEqualTo(17L);
  }

  @Test
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void positiveLongFailure() {
    try {
      ConstantConditions.positive(-71L);
      fail();

    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("number");
    }
  }

  @Test
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void positiveLongFailureNamed() {
    try {
      ConstantConditions.positive("test", -13L);
      fail();

    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("test");
    }
  }

  @Test
  public void positiveLongNamed() {
    assertThat(ConstantConditions.positive("test", 11L)).isEqualTo(11L);
  }

  @Test
  public void unsupported() {
    try {
      ConstantConditions.unsupported();
      fail();

    } catch (final UnsupportedOperationException e) {
      assertThat(e.getMessage()).contains("unsupported");
    }
  }

  @Test
  public void unsupportedMessage() {
    try {
      ConstantConditions.unsupported("test");
      fail();

    } catch (final UnsupportedOperationException e) {
      assertThat(e.getMessage()).contains("test");
    }
  }

  @Test
  public void unsupportedMessageNamed() {
    try {
      unsupportedMethod();
      fail();

    } catch (final UnsupportedOperationException e) {
      assertThat(e.getMessage()).contains("test").contains("unsupportedMessageNamed");
    }
  }

  private void unsupportedMethod() {
    ConstantConditions.unsupported("test", "unsupportedMethod");
  }

  private static class GenericIterable<T> implements Iterable<T> {

    private final Iterable<T> iterable;

    private GenericIterable(@NotNull final Iterable<T> iterable) {
      this.iterable = iterable;
    }

    @NotNull
    public Iterator<T> iterator() {
      return iterable.iterator();
    }
  }
}
