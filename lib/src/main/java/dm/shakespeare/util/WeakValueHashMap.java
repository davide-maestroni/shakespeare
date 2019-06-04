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
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Created by davide-maestroni on 05/30/2019.
 */
public class WeakValueHashMap<K, V> implements Map<K, V> {

  private final WeakHashMap<KeyWrapper<K>, WeakReference<V>> keys;
  private final WeakHashMap<V, HashSet<KeyWrapper<K>>> values;

  private AbstractSet<Entry<K, V>> entrySet;
  private AbstractSet<K> keySet;
  private AbstractSet<V> valuesCollection;

  /**
   * Creates a new empty map with the default initial capacity (16) and the default load factor
   * (0.75).
   */
  public WeakValueHashMap() {
    keys = new WeakHashMap<KeyWrapper<K>, WeakReference<V>>();
    values = new WeakHashMap<V, HashSet<KeyWrapper<K>>>();
  }

  /**
   * Creates a new empty map with the specified initial capacity and the default load factor (0.75).
   *
   * @param initialCapacity the initial capacity.
   * @throws IllegalArgumentException if the initial capacity is negative.
   */
  public WeakValueHashMap(final int initialCapacity) {
    keys = new WeakHashMap<KeyWrapper<K>, WeakReference<V>>(initialCapacity);
    values = new WeakHashMap<V, HashSet<KeyWrapper<K>>>(initialCapacity);
  }

  /**
   * Creates a new empty map with the specified initial capacity and load factor.
   *
   * @param initialCapacity the initial capacity.
   * @param loadFactor      the load factor.
   * @throws IllegalArgumentException if the initial capacity is negative or the load factor is
   *                                  not positive.
   */
  public WeakValueHashMap(final int initialCapacity, final float loadFactor) {
    keys = new WeakHashMap<KeyWrapper<K>, WeakReference<V>>(initialCapacity, loadFactor);
    values = new WeakHashMap<V, HashSet<KeyWrapper<K>>>(initialCapacity, loadFactor);
  }

  /**
   * Creates a new map with the same mappings as the specified {@code Map}.<br>
   * The map is created with default load factor (0.75) and an initial capacity sufficient to hold
   * the mappings in the specified {@code Map}.
   *
   * @param map the initial mapping.
   */
  public WeakValueHashMap(@NotNull final Map<? extends K, ? extends V> map) {
    this(map.size());
    putAll(map);
  }

  @Override
  public int hashCode() {
    return keys.hashCode();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof WeakValueHashMap)) {
      return (o instanceof Map) && o.equals(this);
    }
    final WeakValueHashMap<?, ?> that = (WeakValueHashMap<?, ?>) o;
    return keys.equals(that.keys);
  }

  public int size() {
    return keys.size();
  }

  public boolean isEmpty() {
    return keys.isEmpty();
  }

  public boolean containsKey(final Object key) {
    return keys.containsKey(wrapKey(key));
  }

  @SuppressWarnings("SuspiciousMethodCalls")
  public boolean containsValue(final Object value) {
    return values.containsKey(value);
  }

  public V get(final Object key) {
    return keys.get(key).get();
  }

  @Nullable
  public V put(final K key, final V value) {
    if (value == null) {
      return remove(key);
    }
    final KeyWrapper<K> wrapKey = wrapKey(key);
    final WeakHashMap<V, HashSet<KeyWrapper<K>>> values = this.values;
    HashSet<KeyWrapper<K>> keyWrappers = values.get(value);
    if (keyWrappers == null) {
      keyWrappers = new HashSet<KeyWrapper<K>>();
      values.put(value, keyWrappers);
    }
    final WeakReference<V> oldRef = keys.put(wrapKey, new WeakReference<V>(value));
    if (oldRef != null) {
      final V oldValue = oldRef.get();
      final HashSet<KeyWrapper<K>> oldKeyWrapper = values.get(oldValue);
      if (oldKeyWrapper != null) {
        final Iterator<KeyWrapper<K>> iterator = oldKeyWrapper.iterator();
        while (iterator.hasNext()) {
          final KeyWrapper<K> next = iterator.next();
          if (wrapKey.equals(next)) {
            keyWrappers.add(next);
            iterator.remove();
            if (oldKeyWrapper.isEmpty()) {
              values.remove(oldValue);
            }
            return oldValue;
          }
        }
        keyWrappers.add(wrapKey);
      }
      return oldValue;

    } else {
      keyWrappers.add(wrapKey);
    }
    return null;
  }

  public V remove(final Object key) {
    final KeyWrapper<K> wrapKey = wrapKey(key);
    final WeakReference<V> oldRef = keys.remove(wrapKey);
    if (oldRef != null) {
      final V oldValue = oldRef.get();
      final WeakHashMap<V, HashSet<KeyWrapper<K>>> values = this.values;
      final HashSet<KeyWrapper<K>> keyWrappers = values.get(oldValue);
      if (keyWrappers != null) {
        keyWrappers.remove(wrapKey);
        if (keyWrappers.isEmpty()) {
          values.remove(oldValue);
        }
      }
      return oldValue;
    }
    return null;
  }

  public void putAll(@NotNull final Map<? extends K, ? extends V> map) {
    for (final Entry<? extends K, ? extends V> entry : map.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  public void clear() {
    keys.clear();
    values.clear();
  }

  @NotNull
  public Set<K> keySet() {
    if (keySet == null) {
      keySet = new AbstractSet<K>() {

        @NotNull
        @Override
        public Iterator<K> iterator() {
          return new KeyIterator();
        }

        @Override
        public int size() {
          return keys.size();
        }
      };
    }
    return keySet;
  }

  @NotNull
  public Collection<V> values() {
    if (valuesCollection == null) {
      valuesCollection = new AbstractSet<V>() {

        @NotNull
        @Override
        public Iterator<V> iterator() {
          return new ValueIterator();
        }

        @Override
        public int size() {
          return keys.size();
        }
      };
    }
    return valuesCollection;
  }

  @NotNull
  public Set<Entry<K, V>> entrySet() {
    if (entrySet == null) {
      entrySet = new AbstractSet<Entry<K, V>>() {

        @NotNull
        @Override
        public Iterator<Entry<K, V>> iterator() {
          return new EntryIterator();
        }

        @Override
        public int size() {
          return keys.size();
        }
      };
    }
    return entrySet;
  }

  @NotNull
  private KeyWrapper<K> wrapKey(final Object key) {
    return new KeyWrapper<K>(key);
  }

  private static class KeyWrapper<K> {

    private final Object key;

    private KeyWrapper(final Object key) {
      this.key = key;
    }

    @SuppressWarnings("unchecked")
    public K getKey() {
      return (K) key;
    }

    @Override
    public int hashCode() {
      return key != null ? key.hashCode() : 0;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }

      if ((o == null) || getClass() != o.getClass()) {
        return false;
      }
      final KeyWrapper<?> that = (KeyWrapper<?>) o;
      return (key != null) ? key.equals(that.key) : that.key == null;
    }
  }

  private abstract class AbstractIterator<T> implements Iterator<T> {

    protected final Iterator<Entry<KeyWrapper<K>, WeakReference<V>>> iterator =
        keys.entrySet().iterator();

    private Entry<KeyWrapper<K>, WeakReference<V>> next;

    public boolean hasNext() {
      return iterator.hasNext();
    }

    public T next() {
      return getValue(this.next = iterator.next());
    }

    public void remove() {
      iterator.remove();
      final Entry<KeyWrapper<K>, WeakReference<V>> entry = this.next;
      final V value = entry.getValue().get();
      final WeakHashMap<V, HashSet<KeyWrapper<K>>> values = WeakValueHashMap.this.values;
      final HashSet<KeyWrapper<K>> keyWrappers = values.get(value);
      if (keyWrappers != null) {
        final KeyWrapper<K> wrapKey = entry.getKey();
        keyWrappers.remove(wrapKey);
        if (keyWrappers.isEmpty()) {
          values.remove(value);
        }
      }
    }

    protected abstract T getValue(final Entry<KeyWrapper<K>, WeakReference<V>> entry);
  }

  private class EntryIterator extends AbstractIterator<Entry<K, V>> {

    protected Entry<K, V> getValue(final Entry<KeyWrapper<K>, WeakReference<V>> entry) {
      return new WeakEntry(entry);
    }
  }

  private class KeyIterator extends AbstractIterator<K> {

    protected K getValue(final Entry<KeyWrapper<K>, WeakReference<V>> entry) {
      return entry.getKey().getKey();
    }
  }

  private class ValueIterator extends AbstractIterator<V> {

    protected V getValue(final Entry<KeyWrapper<K>, WeakReference<V>> entry) {
      return entry.getValue().get();
    }
  }

  private class WeakEntry implements Entry<K, V> {

    private final Entry<KeyWrapper<K>, WeakReference<V>> entry;

    private WeakEntry(final Entry<KeyWrapper<K>, WeakReference<V>> entry) {
      this.entry = entry;
    }

    public K getKey() {
      return entry.getKey().getKey();
    }

    public V getValue() {
      return entry.getValue().get();
    }

    public V setValue(final V value) {
      return put(getKey(), value);
    }
  }
}
