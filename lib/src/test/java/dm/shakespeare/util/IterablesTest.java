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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by davide-maestroni on 03/18/2019.
 */
public class IterablesTest {

  private static <E> Iterable<E> toIterable(@NotNull final Collection<E> collection) {
    return Iterables.concat(Collections.singleton(collection));
  }

  @Test
  public void addAll() {
    final ArrayList<Integer> list = new ArrayList<Integer>();
    list.add(1);
    assertThat(Iterables.addAll(Arrays.asList(2, 3), list)).isSameAs(list);
    assertThat(list).containsExactly(1, 2, 3);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void addAllIterableNPE() {
    Iterables.addAll(null, new ArrayList<Integer>());
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void addAllNPE() {
    Iterables.addAll(Arrays.asList(2, 3), null);
  }

  @Test
  public void asList() {
    final HashSet<Integer> set = new HashSet<Integer>(Arrays.asList(2, 3));
    assertThat(Iterables.asList(set)).isInstanceOf(List.class).containsExactlyInAnyOrder(2, 3);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void asListNPE() {
    Iterables.asList(null);
  }

  @Test
  public void asListSame() {
    final List<Integer> list = Arrays.asList(2, 3);
    assertThat(Iterables.asList(list)).isSameAs(list);
  }

  @Test
  public void asSet() {
    final List<Integer> list = Arrays.asList(2, 3);
    assertThat(Iterables.asSet(list)).isInstanceOf(Set.class).containsExactlyInAnyOrder(2, 3);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void asSetNPE() {
    Iterables.asSet(null);
  }

  @Test
  public void asSetSame() {
    final HashSet<Integer> set = new HashSet<Integer>(Arrays.asList(2, 3));
    assertThat(Iterables.asSet(set)).isSameAs(set);
  }

  @Test
  public void concat() {
    final Set<Integer> set = Collections.singleton(1);
    final List<Integer> list = Arrays.asList(2, 3);
    assertThat(Iterables.concat(Arrays.asList(set, list))).containsExactly(1, 2, 3);
  }

  @Test
  public void concatEmpty() {
    assertThat(Iterables.concat(Collections.<Iterable<?>>emptyList())).isEmpty();
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void concatNPE() {
    Iterables.concat(null);
  }

  @Test(expected = NoSuchElementException.class)
  public void concatNoElements() {
    Iterables.concat(Collections.<Iterable<?>>emptyList()).iterator().next();
  }

  @Test(expected = NullPointerException.class)
  public void concatNull() {
    Iterables.concat(Arrays.asList(Arrays.asList(2, 3), null));
  }

  @Test(expected = IllegalStateException.class)
  public void concatRemoveError() {
    final Set<Integer> set = Collections.singleton(1);
    final List<Integer> list = Arrays.asList(2, 3);
    Iterables.concat(Arrays.asList(set, list)).iterator().remove();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void concatRemoveUnsupported() {
    final Set<Integer> set = Collections.singleton(1);
    final List<Integer> list = Arrays.asList(2, 3);
    final Iterator<Integer> iterator = Iterables.concat(Arrays.asList(set, list)).iterator();
    iterator.next();
    iterator.remove();
  }

  @Test
  public void contains() {
    final List<Integer> list = Arrays.asList(2, 3);
    assertThat(Iterables.contains(list, 3)).isTrue();
  }

  @Test
  public void containsAll() {
    final List<Integer> list = Arrays.asList(1, 2, 3);
    assertThat(Iterables.containsAll(list, Arrays.asList(2, 3))).isTrue();
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void containsAllCollectionNPE() {
    Iterables.containsAll(Collections.emptyList(), null);
  }

  @Test
  public void containsAllEmpty() {
    final List<Integer> list = Arrays.asList(1, 2, 3);
    assertThat(Iterables.containsAll(list, Collections.emptyList())).isTrue();
  }

  @Test
  public void containsAllEmptyIterable() {
    final List<Integer> list = Arrays.asList(1, 2, 3);
    assertThat(Iterables.containsAll(toIterable(list), Collections.emptyList())).isTrue();
  }

  @Test
  public void containsAllIterable() {
    final List<Integer> list = Arrays.asList(1, 2, 3);
    assertThat(Iterables.containsAll(toIterable(list), Arrays.asList(2, 3))).isTrue();
  }

  @Test
  public void containsAllMulti() {
    final List<Integer> list = Arrays.asList(1, 2, 3);
    assertThat(Iterables.containsAll(list, Arrays.asList(2, 2))).isTrue();
  }

  @Test
  public void containsAllMultiIterable() {
    final List<Integer> list = Arrays.asList(1, 2, 3);
    assertThat(Iterables.containsAll(toIterable(list), Arrays.asList(2, 2))).isTrue();
  }

  @Test
  public void containsAllMultiNot() {
    final List<Integer> list = Arrays.asList(1, 2, 3);
    assertThat(Iterables.containsAll(list, Arrays.asList(4, 4))).isFalse();
  }

  @Test
  public void containsAllMultiNotIterable() {
    final List<Integer> list = Arrays.asList(1, 2, 3);
    assertThat(Iterables.containsAll(toIterable(list), Arrays.asList(4, 4))).isFalse();
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void containsAllNPE() {
    Iterables.containsAll(null, Collections.emptyList());
  }

  @Test
  public void containsAllNot() {
    final List<Integer> list = Arrays.asList(1, 2, 3);
    assertThat(Iterables.containsAll(list, Arrays.asList(3, 4))).isFalse();
  }

  @Test
  public void containsAllNotIterable() {
    final List<Integer> list = Arrays.asList(1, 2, 3);
    assertThat(Iterables.containsAll(toIterable(list), Arrays.asList(3, 4))).isFalse();
  }

  @Test
  public void containsAllNull() {
    final List<Integer> list = Arrays.asList(1, 2, 3, null);
    assertThat(Iterables.containsAll(list, Arrays.asList(null, 3))).isTrue();
  }

  @Test
  public void containsAllNullIterable() {
    final List<Integer> list = Arrays.asList(1, 2, 3, null);
    assertThat(Iterables.containsAll(toIterable(list), Arrays.asList(null, 3))).isTrue();
  }

  @Test
  public void containsAllNullNot() {
    final List<Integer> list = Arrays.asList(1, 2, 3);
    assertThat(Iterables.containsAll(list, Arrays.asList(null, 3))).isFalse();
  }

  @Test
  public void containsAllNullNotIterable() {
    final List<Integer> list = Arrays.asList(1, 2, 3);
    assertThat(Iterables.containsAll(toIterable(list), Arrays.asList(null, 3))).isFalse();
  }

  @Test
  public void containsAllSame() {
    final List<Integer> list = Arrays.asList(2, 2);
    assertThat(Iterables.containsAll(list, Collections.singleton(2))).isTrue();
  }

  @Test
  public void containsAllSameIterable() {
    final List<Integer> list = Arrays.asList(2, 2);
    assertThat(Iterables.containsAll(toIterable(list), Collections.singleton(2))).isTrue();
  }

  @Test
  public void containsAllSameNot() {
    final List<Integer> list = Arrays.asList(2, 2);
    assertThat(Iterables.containsAll(list, Collections.singleton(3))).isFalse();
  }

  @Test
  public void containsAllSameNotIterable() {
    final List<Integer> list = Arrays.asList(2, 2);
    assertThat(Iterables.containsAll(toIterable(list), Collections.singleton(3))).isFalse();
  }

  @Test
  public void containsIterable() {
    final List<Integer> list = Arrays.asList(2, 3);
    assertThat(Iterables.contains(toIterable(list), 3)).isTrue();
  }

  @Test
  public void containsIterableNot() {
    final List<Integer> list = Arrays.asList(2, 3);
    assertThat(Iterables.contains(toIterable(list), 1)).isFalse();
  }

  @Test
  public void containsIterableNull() {
    final List<Integer> list = Arrays.asList(2, null, 3);
    assertThat(Iterables.contains(toIterable(list), null)).isTrue();
  }

  @Test
  public void containsIterableNullNot() {
    final List<Integer> list = Arrays.asList(2, 3);
    assertThat(Iterables.contains(toIterable(list), null)).isFalse();
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void containsNPE() {
    Iterables.contains(null, 2);
  }

  @Test
  public void containsNot() {
    final List<Integer> list = Arrays.asList(2, 3);
    assertThat(Iterables.contains(list, 1)).isFalse();
  }

  @Test
  public void containsNull() {
    final List<Integer> list = Arrays.asList(2, null, 3);
    assertThat(Iterables.contains(list, null)).isTrue();
  }

  @Test
  public void containsNullNot() {
    final List<Integer> list = Arrays.asList(2, 3);
    assertThat(Iterables.contains(list, null)).isFalse();
  }

  @Test
  public void first() {
    final List<Integer> list = Arrays.asList(2, 3);
    assertThat(Iterables.first(list)).isEqualTo(2);
  }

  @Test(expected = NoSuchElementException.class)
  public void firstEmpty() {
    Iterables.first(Collections.<Integer>emptyList());
  }

  @Test(expected = NoSuchElementException.class)
  public void firstEmptyIterable() {
    Iterables.first(toIterable(Collections.<Integer>emptyList()));
  }

  @Test
  public void firstIterable() {
    final List<Integer> list = Arrays.asList(2, 3);
    assertThat(Iterables.first(toIterable(list))).isEqualTo(2);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void firstNPE() {
    Iterables.first(null);
  }

  @Test
  public void get() {
    final List<Integer> list = Arrays.asList(2, 3);
    assertThat(Iterables.get(list, 1)).isEqualTo(3);
  }

  @Test
  public void getIterable() {
    final List<Integer> list = Arrays.asList(2, 3);
    assertThat(Iterables.get(toIterable(list), 1)).isEqualTo(3);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void getNPE() {
    Iterables.get(null, 2);
  }

  @Test(expected = NoSuchElementException.class)
  public void getNegative() {
    final List<Integer> list = Arrays.asList(2, 3);
    Iterables.get(list, -1);
  }

  @Test(expected = NoSuchElementException.class)
  public void getNegativeIterable() {
    final List<Integer> list = Arrays.asList(2, 3);
    Iterables.get(toIterable(list), -1);
  }

  @Test(expected = NoSuchElementException.class)
  public void getOOB() {
    final List<Integer> list = Arrays.asList(2, 3);
    Iterables.get(list, 3);
  }

  @Test(expected = NoSuchElementException.class)
  public void getOOBIterable() {
    final List<Integer> list = Arrays.asList(2, 3);
    Iterables.get(toIterable(list), 3);
  }

  @Test
  public void isEmpty() {
    final List<Integer> list = Arrays.asList(2, 3);
    assertThat(Iterables.isEmpty(list)).isFalse();
  }

  @Test
  public void isEmptyEmpty() {
    assertThat(Iterables.isEmpty(Collections.emptyList())).isTrue();
  }

  @Test
  public void isEmptyEmptyIterable() {
    assertThat(Iterables.isEmpty(toIterable(Collections.emptyList()))).isTrue();
  }

  @Test
  public void isEmptyIterable() {
    final List<Integer> list = Arrays.asList(2, 3);
    assertThat(Iterables.isEmpty(toIterable(list))).isFalse();
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void isEmptyNPE() {
    Iterables.isEmpty(null);
  }

  @Test
  public void remove() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 2));
    assertThat(Iterables.remove(list, 2)).isTrue();
    assertThat(list).containsExactly(1, 2);
  }

  @Test
  public void removeAll() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    assertThat(Iterables.removeAll(list, Arrays.asList(1, 3))).isTrue();
    assertThat(list).containsExactly(2);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void removeAllCollectionNPE() {
    Iterables.removeAll(Collections.emptyList(), null);
  }

  @Test
  public void removeAllEmpty() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 2));
    assertThat(Iterables.removeAll(list, Collections.emptyList())).isFalse();
    assertThat(list).containsExactly(1, 2, 2);
  }

  @Test
  public void removeAllEmptyIterable() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 2));
    assertThat(Iterables.removeAll(toIterable(list), Collections.emptyList())).isFalse();
    assertThat(list).containsExactly(1, 2, 2);
  }

  @Test
  public void removeAllIterable() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    assertThat(Iterables.removeAll(toIterable(list), Arrays.asList(1, 3))).isTrue();
    assertThat(list).containsExactly(2);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void removeAllNPE() {
    Iterables.removeAll(null, Collections.emptyList());
  }

  @Test
  public void removeAllNot() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    assertThat(Iterables.removeAll(list, Arrays.asList(0, 4))).isFalse();
    assertThat(list).containsExactly(1, 2, 3);
  }

  @Test
  public void removeAllNotIterable() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    assertThat(Iterables.removeAll(toIterable(list), Arrays.asList(0, 4))).isFalse();
    assertThat(list).containsExactly(1, 2, 3);
  }

  @Test
  public void removeAllNull() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, null, null));
    assertThat(Iterables.removeAll(list, Collections.singleton(null))).isTrue();
    assertThat(list).containsExactly(1);
  }

  @Test
  public void removeAllNullIterable() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, null, null));
    assertThat(Iterables.removeAll(toIterable(list), Collections.singleton(null))).isTrue();
    assertThat(list).containsExactly(1);
  }

  @Test
  public void removeAllSame() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 1));
    assertThat(Iterables.removeAll(list, Arrays.asList(1, 1))).isTrue();
    assertThat(list).containsExactly(2);
  }

  @Test
  public void removeAllSameIterable() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 1));
    assertThat(Iterables.removeAll(toIterable(list), Arrays.asList(1, 1))).isTrue();
    assertThat(list).containsExactly(2);
  }

  @Test
  public void removeEmpty() {
    final List<Integer> list = Collections.emptyList();
    assertThat(Iterables.remove(list, 2)).isFalse();
  }

  @Test
  public void removeEmptyIterable() {
    final List<Integer> list = Collections.emptyList();
    assertThat(Iterables.remove(toIterable(list), 2)).isFalse();
  }

  @Test
  public void removeIterable() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 2));
    assertThat(Iterables.remove(toIterable(list), 2)).isTrue();
    assertThat(list).containsExactly(1, 2);
  }

  @Test
  public void removeNot() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    assertThat(Iterables.remove(list, 4)).isFalse();
    assertThat(list).containsExactly(1, 2, 3);
  }

  @Test
  public void removeNotIterable() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    assertThat(Iterables.remove(toIterable(list), 4)).isFalse();
    assertThat(list).containsExactly(1, 2, 3);
  }

  @Test
  public void removeNull() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, null, null));
    assertThat(Iterables.remove(list, null)).isTrue();
    assertThat(list).containsExactly(1, null);
  }

  @Test
  public void removeNullIterable() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, null, null));
    assertThat(Iterables.remove(toIterable(list), null)).isTrue();
    assertThat(list).containsExactly(1, null);
  }

  @Test
  public void removeNullNot() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    assertThat(Iterables.remove(list, null)).isFalse();
    assertThat(list).containsExactly(1, 2, 3);
  }

  @Test
  public void removeNullNotIterable() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    assertThat(Iterables.remove(toIterable(list), null)).isFalse();
    assertThat(list).containsExactly(1, 2, 3);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void removeUnsupported() {
    final List<Integer> list = Arrays.asList(1, 2, 3);
    Iterables.remove(list, 2);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void removeUnsupportedIterable() {
    final List<Integer> list = Arrays.asList(1, 2, 3);
    Iterables.remove(toIterable(list), 2);
  }

  @Test
  public void retainAll() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    assertThat(Iterables.retainAll(list, Arrays.asList(3, 1))).isTrue();
    assertThat(list).containsExactly(1, 3);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void retainAllCollectionNPE() {
    Iterables.retainAll(Collections.emptyList(), null);
  }

  @Test
  public void retainAllEmpty() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    assertThat(Iterables.retainAll(list, Collections.emptyList())).isTrue();
    assertThat(list).isEmpty();
  }

  @Test
  public void retainAllEmptyIterable() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    assertThat(Iterables.retainAll(toIterable(list), Collections.emptyList())).isTrue();
    assertThat(list).isEmpty();
  }

  @Test
  public void retainAllIterable() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    assertThat(Iterables.retainAll(toIterable(list), Arrays.asList(3, 1))).isTrue();
    assertThat(list).containsExactly(1, 3);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void retainAllNPE() {
    Iterables.retainAll(null, Collections.emptyList());
  }

  @Test
  public void retainAllNot() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    assertThat(Iterables.retainAll(list, Arrays.asList(4, 5, null))).isTrue();
    assertThat(list).isEmpty();
  }

  @Test
  public void retainAllNotIterable() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    assertThat(Iterables.retainAll(toIterable(list), Arrays.asList(4, 5, null))).isTrue();
    assertThat(list).isEmpty();
  }

  @Test
  public void retainAllNull() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, null, 3, null));
    assertThat(Iterables.retainAll(list, Arrays.asList(null, 1))).isTrue();
    assertThat(list).containsExactly(1, null, null);
  }

  @Test
  public void retainAllNullIterable() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, null, 3, null));
    assertThat(Iterables.retainAll(toIterable(list), Arrays.asList(null, 1))).isTrue();
    assertThat(list).containsExactly(1, null, null);
  }

  @Test
  public void retainAllSame() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    assertThat(Iterables.retainAll(list, Arrays.asList(3, 3))).isTrue();
    assertThat(list).containsExactly(3);
  }

  @Test
  public void retainAllSameIterable() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    assertThat(Iterables.retainAll(toIterable(list), Arrays.asList(3, 3))).isTrue();
    assertThat(list).containsExactly(3);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void retainAllUnsupported() {
    final List<Integer> list = Arrays.asList(1, 2, 3);
    Iterables.retainAll(list, Arrays.asList(3, 3));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void retainAllUnsupportedIterable() {
    final List<Integer> list = Arrays.asList(1, 2, 3);
    Iterables.retainAll(toIterable(list), Arrays.asList(3, 3));
  }

  @Test
  public void size() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    assertThat(Iterables.size(list)).isEqualTo(3);
  }

  @Test
  public void sizeEmpty() {
    final List<Integer> list = Collections.emptyList();
    assertThat(Iterables.size(list)).isZero();
  }

  @Test
  public void sizeEmptyIterable() {
    final List<Integer> list = Collections.emptyList();
    assertThat(Iterables.size(toIterable(list))).isZero();
  }

  @Test
  public void sizeIterable() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    assertThat(Iterables.size(toIterable(list))).isEqualTo(3);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void sizeNPE() {
    Iterables.size(null);
  }

  @Test
  public void sizeNull() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, null, 3));
    assertThat(Iterables.size(list)).isEqualTo(3);
  }

  @Test
  public void sizeNullIterable() {
    final List<Integer> list = new ArrayList<Integer>(Arrays.asList(1, null, 3));
    assertThat(Iterables.size(toIterable(list))).isEqualTo(3);
  }

  @Test
  public void toArray() {
    final List<Integer> list = Arrays.asList(1, 2, 3);
    assertThat(Iterables.toArray(list, new Integer[3])).isInstanceOf(Integer[].class)
        .containsExactly(1, 2, 3);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void toArrayArrayNPE() {
    Iterables.toArray(Collections.emptyList(), null);
  }

  @Test
  public void toArrayBigger() {
    final List<Integer> list = Arrays.asList(1, 2, 3);
    assertThat(Iterables.toArray(list, new Integer[4])).containsExactly(1, 2, 3, null);
  }

  @Test
  public void toArrayBiggerIterable() {
    final List<Integer> list = Arrays.asList(1, 2, 3);
    assertThat(Iterables.toArray(toIterable(list), new Integer[4])).containsExactly(1, 2, 3, null);
  }

  @Test
  public void toArrayClass() {
    final List<Integer> list = Arrays.asList(1, 2, 3);
    assertThat(Iterables.toArray(list, new Object[0])).isInstanceOf(Object[].class)
        .containsExactly(1, 2, 3);
  }

  @Test
  public void toArrayClassIterable() {
    final List<Integer> list = Arrays.asList(1, 2, 3);
    assertThat(Iterables.toArray(toIterable(list), new Object[0])).isInstanceOf(Object[].class)
        .containsExactly(1, 2, 3);
  }

  @Test
  public void toArrayIterable() {
    final List<Integer> list = Arrays.asList(1, 2, 3);
    assertThat(Iterables.toArray(toIterable(list), new Integer[3])).isInstanceOf(Integer[].class)
        .containsExactly(1, 2, 3);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void toArrayNPE() {
    Iterables.toArray(null, new Object[0]);
  }

  @Test
  public void toArraySmaller() {
    final List<Integer> list = Arrays.asList(1, 2, 3);
    assertThat(Iterables.toArray(list, new Integer[0])).containsExactly(1, 2, 3);
  }

  @Test
  public void toArraySmallerIterable() {
    final List<Integer> list = Arrays.asList(1, 2, 3);
    assertThat(Iterables.toArray(toIterable(list), new Integer[0])).containsExactly(1, 2, 3);
  }

  @Test
  public void toList() {
    final HashSet<Integer> set = new HashSet<Integer>(Arrays.asList(2, 3));
    assertThat(Iterables.toList(set)).isInstanceOf(List.class).containsExactlyInAnyOrder(2, 3);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void toListNPE() {
    Iterables.toList(null);
  }

  @Test
  public void toListSame() {
    final List<Integer> list = Arrays.asList(2, 3);
    assertThat(Iterables.toList(list)).isNotSameAs(list).isEqualTo(list);
  }

  @Test
  public void toObjectArray() {
    final List<Integer> list = Arrays.asList(1, 2, 3);
    assertThat(Iterables.toArray(list)).containsExactly(1, 2, 3);
  }

  @Test
  public void toObjectArrayIterable() {
    final List<Integer> list = Arrays.asList(1, 2, 3);
    assertThat(Iterables.toArray(toIterable(list))).containsExactly(1, 2, 3);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void toObjectArrayNPE() {
    Iterables.toArray(null);
  }

  @Test
  public void toSet() {
    final List<Integer> list = Arrays.asList(2, 3);
    assertThat(Iterables.toSet(list)).isInstanceOf(Set.class).containsExactlyInAnyOrder(2, 3);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void toSetNPE() {
    Iterables.toSet(null);
  }

  @Test
  public void toSetSame() {
    final HashSet<Integer> set = new HashSet<Integer>(Arrays.asList(2, 3));
    assertThat(Iterables.toSet(set)).isNotSameAs(set).isEqualTo(set);
  }

  @Test
  public void toStringEmpty() {
    assertThat(Iterables.toString(Collections.emptyList())).isEqualTo("[]");
  }

  @Test
  public void toStringNull() {
    assertThat(Iterables.toString(null)).isEqualTo("null");
  }

  @Test
  public void toStringTest() {
    final List<Integer> list = Arrays.asList(2, 3);
    assertThat(Iterables.toString(list)).isEqualTo("[2, 3]");
  }
}
