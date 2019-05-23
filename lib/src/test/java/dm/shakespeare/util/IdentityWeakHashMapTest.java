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

import org.assertj.core.data.MapEntry;
import org.junit.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * {@link IdentityWeakHashMap} unit tests.
 */
public class IdentityWeakHashMapTest {

  @Test
  public void testClear() {
    final IdentityWeakHashMap<Object, String> map =
        new IdentityWeakHashMap<Object, String>(4, 0.75f);
    final HashMap<Object, String> entries = new HashMap<Object, String>();
    final Object key0 = new Object();
    final Object key1 = new Object();
    final Object key2 = new Object();
    final Object key3 = new Object();
    entries.put(key0, "test0");
    entries.put(key1, "test1");
    entries.put(key2, "test2");
    entries.put(key3, "test3");
    map.putAll(entries);
    assertThat(map).hasSize(4);
    assertThat(map).contains(MapEntry.entry(key0, "test0"), MapEntry.entry(key1, "test1"),
        MapEntry.entry(key2, "test2"), MapEntry.entry(key3, "test3"));
    map.clear();
    assertThat(map).isEmpty();
  }

  @Test(expected = IllegalStateException.class)
  public void testEntryIteratorDoubleRemove() {
    final IdentityWeakHashMap<Object, String> map =
        new IdentityWeakHashMap<Object, String>(4, 0.75f);
    final Object key0 = new Object();
    map.put(key0, "test0");
    final Iterator<Entry<Object, String>> entryIterator = map.entrySet().iterator();
    final Entry<Object, String> nextEntry = entryIterator.next();
    assertThat(map.get(nextEntry.getKey())).isEqualTo(nextEntry.getValue());
    entryIterator.remove();
    entryIterator.remove();
  }

  @Test(expected = NoSuchElementException.class)
  public void testEntryIteratorRemove() {
    final IdentityWeakHashMap<Object, String> map =
        new IdentityWeakHashMap<Object, String>(4, 0.75f);
    final Object key0 = new Object();
    map.put(key0, "test0");
    final Iterator<Entry<Object, String>> entryIterator = map.entrySet().iterator();
    final Entry<Object, String> nextEntry = entryIterator.next();
    assertThat(map.get(nextEntry.getKey())).isEqualTo(nextEntry.getValue());
    entryIterator.remove();
    assertThat(map).doesNotContainKey(nextEntry.getKey());
    assertThat(map).doesNotContainValue(nextEntry.getValue());
    assertThat(map).isEmpty();
    while (entryIterator.hasNext()) {
      entryIterator.next();
    }
    entryIterator.next();
  }

  @Test
  public void testEquals() {
    final HashMap<Object, String> entries = new HashMap<Object, String>();
    final Object key0 = new Object();
    final Object key1 = new Object();
    final Object key2 = new Object();
    final Object key3 = new Object();
    entries.put(key0, "test0");
    entries.put(key1, "test1");
    entries.put(key2, "test2");
    entries.put(key3, "test3");
    final IdentityWeakHashMap<Object, String> map =
        new IdentityWeakHashMap<Object, String>(entries);
    assertThat(map).hasSize(4);
    assertThat(map).contains(MapEntry.entry(key0, "test0"), MapEntry.entry(key1, "test1"),
        MapEntry.entry(key2, "test2"), MapEntry.entry(key3, "test3"));
    assertThat(map).isEqualTo(entries);
    assertThat(map.keySet()).isEqualTo(entries.keySet());
    assertThat(map.values()).containsOnly(entries.values().toArray(new String[entries.size()]));
    assertThat(map.entrySet()).isEqualTo(entries.entrySet());
  }

  @Test
  public void testIdentity() {
    final MyInteger key0 = new MyInteger(3);
    final MyInteger key1 = new MyInteger(3);
    assertThat(key0).isEqualTo(key1);
    final IdentityWeakHashMap<MyInteger, String> map = new IdentityWeakHashMap<MyInteger, String>();
    map.put(key0, "test0");
    map.put(key1, "test1");
    assertThat(map).contains(MapEntry.entry(key0, "test0"), MapEntry.entry(key1, "test1"));
    map.remove(key0);
    assertThat(map).contains(MapEntry.entry(key1, "test1"));
  }

  @Test(expected = IllegalStateException.class)
  public void testKeyIteratorDoubleRemove() {
    final IdentityWeakHashMap<Object, String> map =
        new IdentityWeakHashMap<Object, String>(4, 0.75f);
    final Object key0 = new Object();
    final Object key1 = new Object();
    final Object key3 = new Object();
    map.put(key0, "test0");
    map.put(key1, "test1");
    map.put(key3, "test3");
    final Iterator<Object> keyIterator = map.keySet().iterator();
    final Object nextKey = keyIterator.next();
    assertThat(map).containsKey(nextKey);
    keyIterator.remove();
    keyIterator.remove();
  }

  @Test(expected = NoSuchElementException.class)
  public void testKeyIteratorRemove() {
    final IdentityWeakHashMap<Object, String> map =
        new IdentityWeakHashMap<Object, String>(4, 0.75f);
    final Object key0 = new Object();
    final Object key1 = new Object();
    final Object key3 = new Object();
    map.put(key0, "test0");
    map.put(key1, "test1");
    map.put(key3, "test3");
    final Iterator<Object> keyIterator = map.keySet().iterator();
    final Object nextKey = keyIterator.next();
    assertThat(map).containsKey(nextKey);
    keyIterator.remove();
    assertThat(map).doesNotContainKey(nextKey);
    assertThat(map).hasSize(2);
    while (keyIterator.hasNext()) {
      keyIterator.next();
    }
    keyIterator.next();
  }

  @Test
  public void testPut() {
    final IdentityWeakHashMap<Object, String> map = new IdentityWeakHashMap<Object, String>(13);
    assertThat(map).isEmpty();
    final Object key0 = new Object();
    map.put(key0, "test0");
    assertThat(map).hasSize(1);
    assertThat(map).contains(MapEntry.entry(key0, "test0"));
    assertThat(map).containsKey(key0);
    assertThat(map).containsValue("test0");
    assertThat(map).doesNotContainKey("test0");
    assertThat(map).doesNotContainValue("test1");
  }

  @Test
  public void testPutAll() {
    final IdentityWeakHashMap<Object, String> map = new IdentityWeakHashMap<Object, String>(13);
    assertThat(map).isEmpty();
    final Object key0 = new Object();
    map.put(key0, "test0");
    final HashMap<Object, String> entries = new HashMap<Object, String>();
    final Object key1 = new Object();
    final Object key2 = new Object();
    final Object key3 = new Object();
    entries.put(key1, "test1");
    entries.put(key2, "test2");
    entries.put(key3, "test3");
    map.putAll(entries);
    assertThat(map).hasSize(4);
    assertThat(map).contains(MapEntry.entry(key0, "test0"), MapEntry.entry(key1, "test1"),
        MapEntry.entry(key2, "test2"), MapEntry.entry(key3, "test3"));
  }

  @Test
  public void testRemove() {
    final IdentityWeakHashMap<Object, String> map =
        new IdentityWeakHashMap<Object, String>(4, 0.75f);
    final Object key0 = new Object();
    final Object key1 = new Object();
    final Object key2 = new Object();
    final Object key3 = new Object();
    map.put(key0, "test0");
    map.put(key1, "test1");
    map.put(key2, "test2");
    map.put(key3, "test3");
    assertThat(map).hasSize(4);
    assertThat(map).contains(MapEntry.entry(key0, "test0"), MapEntry.entry(key1, "test1"),
        MapEntry.entry(key2, "test2"), MapEntry.entry(key3, "test3"));
    assertThat(map.get(key2)).isEqualTo("test2");
    map.remove(key2);
    assertThat(map).hasSize(3);
    assertThat(map).contains(MapEntry.entry(key0, "test0"), MapEntry.entry(key1, "test1"),
        MapEntry.entry(key3, "test3"));
  }

  @Test
  public void testSetEntry() {
    final IdentityWeakHashMap<Object, String> map = new IdentityWeakHashMap<Object, String>(13);
    final Object key0 = new Object();
    map.put(key0, "test0");
    final HashMap<Object, String> entries = new HashMap<Object, String>();
    final Object key1 = new Object();
    final Object key2 = new Object();
    final Object key3 = new Object();
    entries.put(key1, "test1");
    entries.put(key2, "test2");
    entries.put(key3, "test3");
    map.putAll(entries);
    final Entry<Object, String> entry = map.entrySet().iterator().next();
    entry.setValue("test");
    assertThat(entry.getValue()).isEqualTo("test");
    assertThat(map.get(entry.getKey())).isEqualTo("test");
  }

  @Test(expected = IllegalStateException.class)
  public void testValueIteratorDoubleRemove() {
    final IdentityWeakHashMap<Object, String> map =
        new IdentityWeakHashMap<Object, String>(4, 0.75f);
    final Object key0 = new Object();
    final Object key1 = new Object();
    map.put(key0, "test0");
    map.put(key1, "test1");
    final Iterator<String> valueIterator = map.values().iterator();
    final String nextValue = valueIterator.next();
    assertThat(map).containsValue(nextValue);
    valueIterator.remove();
    valueIterator.remove();
  }

  @Test(expected = NoSuchElementException.class)
  public void testValueIteratorRemove() {
    final IdentityWeakHashMap<Object, String> map =
        new IdentityWeakHashMap<Object, String>(4, 0.75f);
    final Object key0 = new Object();
    final Object key1 = new Object();
    map.put(key0, "test0");
    map.put(key1, "test1");
    final Iterator<String> valueIterator = map.values().iterator();
    final String nextValue = valueIterator.next();
    assertThat(map).containsValue(nextValue);
    valueIterator.remove();
    assertThat(map).doesNotContainValue(nextValue);
    assertThat(map).hasSize(1);
    while (valueIterator.hasNext()) {
      valueIterator.next();
    }
    valueIterator.next();
  }

  @Test
  @SuppressWarnings("UnusedAssignment")
  public void testWeakReference() throws InterruptedException {
    final IdentityWeakHashMap<Object, String> map = new IdentityWeakHashMap<Object, String>(4);
    final Object key0 = new Object();
    final Object key1 = new Object();
    Object key2 = new Object();
    final Object key3 = new Object();
    map.put(key0, "test0");
    map.put(key1, "test1");
    map.put(key2, "test2");
    map.put(key3, "test3");
    key2 = null;
    // This is not guaranteed to work, so let's try a few times...
    for (int i = 0; i < 5; i++) {
      System.gc();
      Thread.sleep(100);
      if (!map.prune().containsValue("test2")) {
        return;
      }
    }
    fail();
  }

  private static class MyInteger {

    private final int value;

    private MyInteger(final int i) {
      value = i;
    }

    @Override
    public int hashCode() {
      return value;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }

      if (!(o instanceof MyInteger)) {
        return false;
      }

      final MyInteger myInteger = (MyInteger) o;
      return value == myInteger.value;
    }
  }
}
