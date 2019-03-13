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

import org.junit.Test;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CQueue} unit tests.
 */
public class CQueueTest {

  @Test
  public void add() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 77; i++) {
      queue.add(i);
    }

    for (int i = 0; i < 77; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }
    assertThat(queue.isEmpty()).isTrue();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    for (int i = 0; i < 3; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }

    for (int i = 7; i < 13; i++) {
      queue.add(i);
    }

    for (int i = 3; i < 13; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }
    assertThat(queue.isEmpty()).isTrue();
  }

  @Test
  public void addCircular() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    for (int i = 0; i < 7; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }
    assertThat(queue.isEmpty()).isTrue();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }
    assertThat(queue.toArray()).containsExactly(0, 1, 2, 3, 4, 5, 6);
    for (int i = 0; i < 7; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }
    assertThat(queue.isEmpty()).isTrue();
    queue.clear();
    for (int i = 0; i < 4; i++) {
      queue.add(i);
    }

    for (int i = 0; i < 4; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }
    assertThat(queue.toArray()).containsExactly(0, 1, 2, 3, 4, 5, 6);
  }

  @Test
  public void addFirst() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 77; i++) {
      queue.addFirst(i);
    }

    for (int i = 76; i >= 0; i--) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }
    assertThat(queue.isEmpty()).isTrue();
    for (int i = 0; i < 7; i++) {
      queue.addFirst(i);
    }

    for (int i = 6; i >= 4; i--) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.element()).isEqualTo(i);
      assertThat(queue.remove()).isEqualTo(i);
    }

    for (int i = 4; i < 13; i++) {
      queue.addFirst(i);
    }

    for (int i = 12; i >= 0; i--) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peek()).isEqualTo(i);
      assertThat(queue.remove()).isEqualTo(i);
    }
    assertThat(queue.isEmpty()).isTrue();
  }

  @Test
  public void addLast() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 77; i++) {
      queue.addLast(i);
    }

    for (int i = 0; i < 77; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peek()).isEqualTo(i);
      assertThat(queue.remove()).isEqualTo(i);
    }
    assertThat(queue.isEmpty()).isTrue();
    for (int i = 0; i < 7; i++) {
      queue.offer(i);
    }

    for (int i = 0; i < 3; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.element()).isEqualTo(i);
      assertThat(queue.remove()).isEqualTo(i);
    }

    for (int i = 7; i < 13; i++) {
      queue.addLast(i);
    }

    for (int i = 3; i < 13; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }
    assertThat(queue.isEmpty()).isTrue();
  }

  @Test
  public void clear() {
    final CQueue<Integer> queue = new CQueue<Integer>(10);
    for (int i = 0; i < 77; i++) {
      queue.add(i);
    }

    for (int i = 0; i < 3; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peek()).isEqualTo(i);
      assertThat(queue.remove()).isEqualTo(i);
    }
    queue.clear();
    assertThat(queue.isEmpty()).isTrue();
  }

  @Test
  public void drain() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    for (int i = 0; i < 3; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peek()).isEqualTo(i);
      assertThat(queue.remove()).isEqualTo(i);
    }
    final ArrayList<Integer> list = new ArrayList<Integer>();
    queue.drainTo(list);
    assertThat(queue.isEmpty()).isTrue();
    for (int i = 3; i < 7; i++) {
      assertThat(list.get(i - 3)).isEqualTo(i);
    }
  }

  @Test
  public void drainLimited() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    for (int i = 0; i < 3; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }
    final ArrayList<Integer> list = new ArrayList<Integer>();
    queue.drainTo(list, 3);
    assertThat(queue.isEmpty()).isFalse();
    assertThat(queue).hasSize(1);
    assertThat(list).hasSize(3);
    for (int i = 3; i < 6; i++) {
      assertThat(list.get(i - 3)).isEqualTo(i);
    }
  }

  @Test
  public void drainToArray() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    for (int i = 0; i < 3; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }
    final Integer[] array = new Integer[5];
    queue.drainTo(array, 2);
    assertThat(array[0]).isNull();
    assertThat(array[1]).isNull();
    assertThat(array[2]).isEqualTo(3);
    assertThat(array[3]).isEqualTo(4);
    assertThat(array[4]).isEqualTo(5);
    assertThat(queue).hasSize(1);
  }

  @Test
  public void drainToArrayLimited() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    for (int i = 0; i < 3; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }
    final Integer[] array = new Integer[5];
    queue.drainTo(array, 0, 3);
    assertThat(array[0]).isEqualTo(3);
    assertThat(array[1]).isEqualTo(4);
    assertThat(array[2]).isEqualTo(5);
    assertThat(array[3]).isNull();
    assertThat(array[4]).isNull();
    assertThat(queue).hasSize(1);
  }

  @Test
  public void drainToQueue() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    for (int i = 0; i < 3; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }
    final CQueue<Integer> other = new CQueue<Integer>();
    queue.drainTo(other);
    assertThat(queue.isEmpty()).isTrue();
    int i = 3;
    for (final Integer integer : other) {
      assertThat(integer).isEqualTo(i++);
    }
  }

  @Test
  public void get() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }
    assertThat(queue.get(2)).isEqualTo(2);
    for (int i = 0; i < 3; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }
    assertThat(queue.get(2)).isEqualTo(5);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  public void getNegative() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }
    queue.get(-2);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  public void getOutOfBound() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }
    queue.get(10);
  }

  @Test
  public void iterator() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    for (int i = -1; i > -7; i--) {
      queue.addFirst(i);
    }
    int count = -6;
    for (final Integer integer : queue) {
      assertThat(integer).isEqualTo(count++);
    }
    queue.clear();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }
    count = 0;
    final Iterator<Integer> iterator = queue.iterator();
    while (iterator.hasNext()) {
      final Integer integer = iterator.next();
      assertThat(integer).isEqualTo(count++);
      iterator.remove();
    }
    assertThat(queue.isEmpty()).isTrue();
  }

  @Test(expected = NoSuchElementException.class)
  public void iteratorError() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }
    int count = 0;
    final Iterator<Integer> iterator = queue.iterator();
    while (iterator.hasNext()) {
      final Integer integer = iterator.next();
      assertThat(integer).isEqualTo(count++);
    }
    iterator.next();
  }

  @Test(expected = ConcurrentModificationException.class)
  public void iteratorErrorAdd() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    for (final Integer ignored : queue) {
      queue.add(8);
    }
  }

  @Test(expected = ConcurrentModificationException.class)
  public void iteratorErrorNext() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }
    final Iterator<Integer> iterator = queue.iterator();
    assertThat(iterator.next()).isZero();
    queue.add(8);
    iterator.next();
  }

  @Test(expected = ConcurrentModificationException.class)
  public void iteratorErrorRemove() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }
    final Iterator<Integer> iterator = queue.iterator();
    assertThat(iterator.next()).isZero();
    queue.add(8);
    iterator.remove();
  }

  @Test(expected = IllegalStateException.class)
  public void iteratorRemoveError() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }
    final Iterator<Integer> iterator = queue.iterator();
    iterator.remove();
    iterator.remove();
  }

  @Test
  public void peekEmpty() {
    assertThat(new CQueue<Integer>().peek()).isNull();
  }

  @Test(expected = NoSuchElementException.class)
  public void peekFirstClearError() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }
    queue.clear();
    queue.peekFirst();
  }

  @Test(expected = NoSuchElementException.class)
  public void peekFirstEmptyError() {
    new CQueue<Integer>().peekFirst();
  }

  @Test(expected = NoSuchElementException.class)
  public void peekFirstError() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    for (int i = 0; i < 7; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }
    queue.peekFirst();
  }

  @Test
  public void peekLast() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }
    assertThat(queue.peekLast()).isEqualTo(6);
  }

  @Test(expected = NoSuchElementException.class)
  public void peekLastClearError() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }
    queue.clear();
    queue.peekLast();
  }

  @Test(expected = NoSuchElementException.class)
  public void peekLastEmptyError() {
    new CQueue<Integer>().peekLast();
  }

  @Test(expected = NoSuchElementException.class)
  public void peekLastError() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    for (int i = 0; i < 7; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }
    queue.peekLast();
  }

  @Test
  public void remove() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 77; i++) {
      queue.add(i);
    }
    assertThat(queue.remove(2)).isEqualTo(2);
    for (int i = 0; i < 3; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo((i >= 2) ? i + 1 : i);
      assertThat(queue.removeFirst()).isEqualTo((i >= 2) ? i + 1 : i);
    }
    assertThat(queue.remove(2)).isEqualTo(6);
  }

  @Test
  public void removeCircular() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    for (int i = 0; i < 7; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }
    assertThat(queue.isEmpty()).isTrue();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }
    assertThat(queue.remove(3)).isEqualTo(3);
    assertThat(queue.toArray()).containsExactly(0, 1, 2, 4, 5, 6);
    queue.clear();
    for (int i = 0; i < 4; i++) {
      queue.add(i);
    }

    for (int i = 0; i < 4; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }

    for (int i = 0; i < 4; i++) {
      queue.add(i);
    }
    assertThat(queue.remove(3)).isEqualTo(3);
    assertThat(queue.toArray()).containsExactly(0, 1, 2);
  }

  @Test(expected = NoSuchElementException.class)
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  public void removeFirstClearError() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }
    queue.clear();
    queue.removeFirst();
  }

  @Test(expected = NoSuchElementException.class)
  public void removeFirstEmptyError() {
    new CQueue<Integer>().removeFirst();
  }

  @Test(expected = NoSuchElementException.class)
  public void removeFirstError() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    for (int i = 0; i < 7; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }
    queue.removeFirst();
  }

  @Test
  public void removeLast() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 77; i++) {
      queue.add(i);
    }

    for (int i = 76; i >= 0; i--) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(0);
      assertThat(queue.removeLast()).isEqualTo(i);
    }
    assertThat(queue.isEmpty()).isTrue();
  }

  @Test(expected = NoSuchElementException.class)
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  public void removeLastClearError() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }
    queue.clear();
    queue.removeLast();
  }

  @Test(expected = NoSuchElementException.class)
  public void removeLastEmptyError() {
    new CQueue<Integer>().removeLast();
  }

  @Test(expected = NoSuchElementException.class)
  public void removeLastError() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    for (int i = 6; i >= 0; i--) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(0);
      assertThat(queue.removeLast()).isEqualTo(i);
    }
    queue.removeLast();
  }

  @Test(expected = IndexOutOfBoundsException.class)
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  public void removeNegative() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }
    queue.remove(-2);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  public void removeOutOfBound() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }
    queue.remove(10);
  }

  @Test
  public void set() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }
    queue.set(2, 71);
    for (int i = 0; i < 3; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo((i == 2) ? 71 : i);
      assertThat(queue.removeFirst()).isEqualTo((i == 2) ? 71 : i);
    }
  }

  @Test(expected = IndexOutOfBoundsException.class)
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  public void setNegative() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }
    queue.set(-2, 71);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  public void setOutOfBound() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }
    queue.set(10, 71);
  }

  @Test
  public void toArray() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    for (int i = 0; i < 3; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }
    final Integer[] array = queue.toArray(new Integer[5]);
    assertThat(array[0]).isEqualTo(3);
    assertThat(array[1]).isEqualTo(4);
    assertThat(array[2]).isEqualTo(5);
    assertThat(array[3]).isEqualTo(6);
    assertThat(array[4]).isNull();
    assertThat(queue).hasSize(4);
  }

  @Test
  public void toArrayEmpty() {
    final CQueue<Integer> queue = new CQueue<Integer>();
    for (int i = 0; i < 7; i++) {
      queue.add(i);
    }

    for (int i = 0; i < 3; i++) {
      assertThat(queue.isEmpty()).isFalse();
      assertThat(queue.peekFirst()).isEqualTo(i);
      assertThat(queue.removeFirst()).isEqualTo(i);
    }
    final Integer[] array = queue.toArray(new Integer[0]);
    assertThat(array[0]).isEqualTo(3);
    assertThat(array[1]).isEqualTo(4);
    assertThat(array[2]).isEqualTo(5);
    assertThat(array[3]).isEqualTo(6);
    assertThat(array).hasSize(4);
    assertThat(queue).hasSize(4);
  }
}
