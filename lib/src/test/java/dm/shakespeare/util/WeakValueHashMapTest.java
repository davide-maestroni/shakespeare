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
 * {@link WeakValueHashMap} unit tests.
 */
public class WeakValueHashMapTest {

  @Test
  public void testClear() {
    final WeakValueHashMap<String, Object> map = new WeakValueHashMap<String, Object>(4, 0.75f);
    final HashMap<String, Object> entries = new HashMap<String, Object>();
    final Object val0 = new Object();
    final Object val1 = new Object();
    final Object val2 = new Object();
    final Object val3 = new Object();
    entries.put("test0", val0);
    entries.put("test1", val1);
    entries.put("test2", val2);
    entries.put("test3", val3);
    map.putAll(entries);
    assertThat(map).hasSize(4);
    assertThat(map).contains(MapEntry.entry("test0", val0), MapEntry.entry("test1", val1),
        MapEntry.entry("test2", val2), MapEntry.entry("test3", val3));
    map.clear();
    assertThat(map).isEmpty();
  }

  @Test
  public void testEntry() {
    final WeakValueHashMap<String, Object> map = new WeakValueHashMap<String, Object>(4, 0.75f);
    final Object val0 = new Object();
    final Object val1 = new Object();
    final Object val3 = new Object();
    map.put("test0", val0);
    map.put("test1", val1);
    map.put("test3", val3);
    for (final Entry<String, Object> entry : map.entrySet()) {
      assertThat(map.entrySet().contains(entry)).isTrue();
    }
  }

  @Test(expected = IllegalStateException.class)
  public void testEntryIteratorDoubleRemove() {
    final WeakValueHashMap<String, Object> map = new WeakValueHashMap<String, Object>(4, 0.75f);
    final Object val0 = new Object();
    map.put("test0", val0);
    final Iterator<Entry<String, Object>> entryIterator = map.entrySet().iterator();
    final Entry<String, Object> nextEntry = entryIterator.next();
    assertThat(map.get(nextEntry.getKey())).isEqualTo(nextEntry.getValue());
    entryIterator.remove();
    entryIterator.remove();
  }

  @Test(expected = NoSuchElementException.class)
  public void testEntryIteratorRemove() {
    final WeakValueHashMap<String, Object> map = new WeakValueHashMap<String, Object>(4, 0.75f);
    final Object val0 = new Object();
    map.put("test0", val0);
    final Iterator<Entry<String, Object>> entryIterator = map.entrySet().iterator();
    final Entry<String, Object> nextEntry = entryIterator.next();
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
    final HashMap<String, Object> entries = new HashMap<String, Object>();
    final Object val0 = new Object();
    final Object val1 = new Object();
    final Object val2 = new Object();
    final Object val3 = new Object();
    entries.put("test0", val0);
    entries.put("test1", val1);
    entries.put("test2", val2);
    entries.put("test3", val3);
    final WeakValueHashMap<String, Object> map = new WeakValueHashMap<String, Object>(entries);
    assertThat(map).hasSize(4);
    assertThat(map).contains(MapEntry.entry("test0", val0), MapEntry.entry("test1", val1),
        MapEntry.entry("test2", val2), MapEntry.entry("test3", val3));
    assertThat(map).isEqualTo(entries);
    assertThat(map.keySet()).isEqualTo(entries.keySet());
    assertThat(map.values()).containsOnly(entries.values().toArray());
    assertThat(map.entrySet()).isEqualTo(entries.entrySet());
  }

  @Test(expected = IllegalStateException.class)
  public void testKeyIteratorDoubleRemove() {
    final WeakValueHashMap<String, Object> map = new WeakValueHashMap<String, Object>(4, 0.75f);
    final Object val0 = new Object();
    final Object val1 = new Object();
    final Object val3 = new Object();
    map.put("test0", val0);
    map.put("test1", val1);
    map.put("test3", val3);
    final Iterator<String> keyIterator = map.keySet().iterator();
    final String nextKey = keyIterator.next();
    assertThat(map).containsKey(nextKey);
    keyIterator.remove();
    keyIterator.remove();
  }

  @Test(expected = NoSuchElementException.class)
  public void testKeyIteratorRemove() {
    final WeakValueHashMap<String, Object> map = new WeakValueHashMap<String, Object>(4, 0.75f);
    final Object val0 = new Object();
    final Object val1 = new Object();
    final Object val3 = new Object();
    map.put("test0", val0);
    map.put("test1", val1);
    map.put("test3", val3);
    final Iterator<String> keyIterator = map.keySet().iterator();
    final String nextKey = keyIterator.next();
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
  public void testKeySet() {
    final WeakValueHashMap<String, Object> map = new WeakValueHashMap<String, Object>(4, 0.75f);
    final Object val0 = new Object();
    final Object val1 = new Object();
    final Object val3 = new Object();
    map.put("test0", val0);
    map.put("test1", val1);
    map.put("test3", val3);
    for (final String key : map.keySet()) {
      assertThat(map.keySet().contains(key)).isTrue();
    }
  }

  @Test
  public void testPut() {
    final WeakValueHashMap<String, Object> map = new WeakValueHashMap<String, Object>();
    final Object val0 = new Object();
    map.put("test0", val0);
    assertThat(map).hasSize(1);
    assertThat(map).contains(MapEntry.entry("test0", val0));
    assertThat(map).containsKey("test0");
    assertThat(map).containsValue(val0);
    assertThat(map).doesNotContainKey("test1");
    assertThat(map).doesNotContainValue("test1");
  }

  @Test
  public void testPutAll() {
    final WeakValueHashMap<String, Object> map = new WeakValueHashMap<String, Object>(13);
    assertThat(map).isEmpty();
    final Object val0 = new Object();
    map.put("test0", val0);
    final HashMap<String, Object> entries = new HashMap<String, Object>();
    final Object val1 = new Object();
    final Object val2 = new Object();
    final Object val3 = new Object();
    entries.put("test1", val1);
    entries.put("test2", val2);
    entries.put("test3", val3);
    map.putAll(entries);
    assertThat(map).hasSize(4);
    assertThat(map).contains(MapEntry.entry("test0", val0), MapEntry.entry("test1", val1),
        MapEntry.entry("test2", val2), MapEntry.entry("test3", val3));
  }

  @Test
  public void testRemove() {
    final WeakValueHashMap<String, Object> map = new WeakValueHashMap<String, Object>(4, 0.75f);
    final Object val0 = new Object();
    final Object val1 = new Object();
    final Object val2 = new Object();
    final Object val3 = new Object();
    map.put("test0", val0);
    map.put("test1", val1);
    map.put("test2", val2);
    map.put("test3", val3);
    assertThat(map).hasSize(4);
    assertThat(map).contains(MapEntry.entry("test0", val0), MapEntry.entry("test1", val1),
        MapEntry.entry("test2", val2), MapEntry.entry("test3", val3));
    assertThat(map.get("test2")).isEqualTo(val2);
    map.remove("test2");
    assertThat(map).hasSize(3);
    assertThat(map).contains(MapEntry.entry("test0", val0), MapEntry.entry("test1", val1),
        MapEntry.entry("test3", val3));
  }

  @Test
  public void testSetEntry() {
    final WeakValueHashMap<String, Object> map = new WeakValueHashMap<String, Object>(13);
    assertThat(map).isEmpty();
    final Object val0 = new Object();
    map.put("test0", val0);
    final HashMap<String, Object> entries = new HashMap<String, Object>();
    final Object val1 = new Object();
    final Object val2 = new Object();
    final Object val3 = new Object();
    entries.put("test1", val1);
    entries.put("test2", val2);
    entries.put("test3", val3);
    map.putAll(entries);
    final Entry<String, Object> entry = map.entrySet().iterator().next();
    entry.setValue("test");
    assertThat(entry.getValue()).isEqualTo("test");
    assertThat(map.get(entry.getKey())).isEqualTo("test");
  }

  @Test(expected = IllegalStateException.class)
  public void testValueIteratorDoubleRemove() {
    final WeakValueHashMap<String, Object> map = new WeakValueHashMap<String, Object>(4, 0.75f);
    final Object val0 = new Object();
    final Object val1 = new Object();
    map.put("test0", val0);
    map.put("test1", val1);
    final Iterator<Object> valueIterator = map.values().iterator();
    final Object nextValue = valueIterator.next();
    assertThat(map).containsValue(nextValue);
    valueIterator.remove();
    valueIterator.remove();
  }

  @Test(expected = NoSuchElementException.class)
  public void testValueIteratorRemove() {
    final WeakValueHashMap<String, Object> map = new WeakValueHashMap<String, Object>(4, 0.75f);
    final Object val0 = new Object();
    final Object val1 = new Object();
    map.put("test0", val0);
    map.put("test1", val1);
    final Iterator<Object> valueIterator = map.values().iterator();
    final Object nextValue = valueIterator.next();
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
  public void testValues() {
    final WeakValueHashMap<String, Object> map = new WeakValueHashMap<String, Object>(4, 0.75f);
    final Object val0 = new Object();
    final Object val1 = new Object();
    final Object val3 = new Object();
    map.put("test0", val0);
    map.put("test1", val1);
    map.put("test3", val3);
    for (final Object value : map.values()) {
      assertThat(map.values().contains(value)).isTrue();
    }
  }

  @Test
  @SuppressWarnings("UnusedAssignment")
  public void testWeakReference() throws InterruptedException {
    final WeakValueHashMap<String, Object> map = new WeakValueHashMap<String, Object>(4);
    final Object val0 = new Object();
    final Object val1 = new Object();
    Object val2 = new Object();
    final Object val3 = new Object();
    map.put("test0", val0);
    map.put("test1", val1);
    map.put("test2", val2);
    map.put("test3", val3);
    val2 = null;
    // This is not guaranteed to work, so let's try a few times...
    for (int i = 0; i < 5; i++) {
      System.gc();
      Thread.sleep(100);
      if (!map.prune().containsKey("test2")) {
        return;
      }
    }
    fail();
  }
}
