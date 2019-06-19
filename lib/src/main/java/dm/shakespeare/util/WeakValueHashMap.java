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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Map implementation with <i>weak values</i>. An entry in a WeakValueHashMap will be automatically
 * removed when its value is no longer in ordinary use (that is, when no more strong reference to
 * the value object are present and it is ready to be garbage-collected).<br>
 * Both null values and the null key are supported.
 *
 * @param <K> the keys runtime type.
 * @param <V> the values runtime type.
 */
public class WeakValueHashMap<K, V> implements Map<K, V> {

  private final HashMap<K, ValueWeakReference<K, V>> map;
  private final ReferenceQueue<Object> queue = new ReferenceQueue<Object>();

  private volatile AbstractSet<Entry<K, V>> entrySet;
  private volatile AbstractCollection<V> valuesCollection;

  /**
   * Creates a new empty map with the default initial capacity (16) and the default load factor
   * (0.75).
   */
  public WeakValueHashMap() {
    map = new HashMap<K, ValueWeakReference<K, V>>();
  }

  /**
   * Creates a new empty map with the specified initial capacity and the default load factor (0.75).
   *
   * @param initialCapacity the initial capacity.
   * @throws IllegalArgumentException if the initial capacity is negative.
   */
  public WeakValueHashMap(final int initialCapacity) {
    map = new HashMap<K, ValueWeakReference<K, V>>(initialCapacity);
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
    map = new HashMap<K, ValueWeakReference<K, V>>(initialCapacity, loadFactor);
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

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return map.hashCode();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof WeakValueHashMap)) {
      return (o instanceof Map) && o.equals(this);
    }
    final WeakValueHashMap<?, ?> that = (WeakValueHashMap<?, ?>) o;
    return map.equals(that.map);
  }

  /**
   * Removes from this map all the entries whose key has no more strong references.
   *
   * @return this map.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public WeakValueHashMap<K, V> prune() {
    final HashMap<K, ValueWeakReference<K, V>> map = this.map;
    final ReferenceQueue<Object> queue = this.queue;
    ValueWeakReference<K, V> reference = (ValueWeakReference<K, V>) queue.poll();
    while (reference != null) {
      final K key = reference.getKey();
      final ValueWeakReference<K, V> oldReference = map.get(key);
      if (oldReference == reference) {
        map.remove(key);
      }
      reference = (ValueWeakReference<K, V>) queue.poll();
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  public int size() {
    return map.size();
  }

  /**
   * {@inheritDoc}
   */
  public boolean isEmpty() {
    return map.isEmpty();
  }

  /**
   * {@inheritDoc}
   */
  public boolean containsKey(final Object key) {
    return map.containsKey(key);
  }

  /**
   * {@inheritDoc}
   */
  public boolean containsValue(final Object value) {
    return map.containsValue(new ValueWeakReference<K, V>(value));
  }

  /**
   * {@inheritDoc}
   */
  public V get(final Object key) {
    final ValueWeakReference<K, V> reference = map.get(key);
    return (reference != null) ? reference.getValue() : null;
  }

  /**
   * {@inheritDoc}
   */
  @Nullable
  public V put(final K key, final V value) {
    prune();
    final ValueWeakReference<K, V> oldReference =
        map.put(key, new ValueWeakReference<K, V>(key, value, queue));
    return (oldReference != null) ? oldReference.getValue() : null;
  }

  /**
   * {@inheritDoc}
   */
  public V remove(final Object key) {
    prune();
    final ValueWeakReference<K, V> oldReference = map.remove(key);
    return (oldReference != null) ? oldReference.getValue() : null;
  }

  /**
   * {@inheritDoc}
   */
  public void putAll(@NotNull final Map<? extends K, ? extends V> map) {
    prune();
    final ReferenceQueue<Object> queue = this.queue;
    final HashMap<K, ValueWeakReference<K, V>> referenceMap = this.map;
    for (final Entry<? extends K, ? extends V> entry : map.entrySet()) {
      final K key = entry.getKey();
      referenceMap.put(key, new ValueWeakReference<K, V>(key, entry.getValue(), queue));
    }
  }

  /**
   * {@inheritDoc}
   */
  public void clear() {
    map.clear();
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  public Set<K> keySet() {
    return map.keySet();
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  public Collection<V> values() {
    if (valuesCollection == null) {
      valuesCollection = new AbstractCollection<V>() {

        @NotNull
        @Override
        public Iterator<V> iterator() {
          return new ValueIterator();
        }

        @Override
        public int size() {
          return map.size();
        }
      };
    }
    return valuesCollection;
  }

  /**
   * {@inheritDoc}
   */
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
          return map.size();
        }
      };
    }
    return entrySet;
  }

  private static class ValueWeakReference<K, V> extends WeakReference<Object> {

    private final int hashCode;
    private final boolean isNull;
    private final K key;

    private ValueWeakReference(final K key, final Object referent,
        final ReferenceQueue<? super Object> queue) {
      super(referent, queue);
      this.key = key;
      if (referent == null) {
        isNull = true;
        hashCode = 0;

      } else {
        isNull = false;
        hashCode = referent.hashCode();
      }
    }

    private ValueWeakReference(final Object referent) {
      super(referent);
      this.key = null;
      if (referent == null) {
        isNull = true;
        hashCode = 0;

      } else {
        isNull = false;
        hashCode = referent.hashCode();
      }
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }

      if (!(obj instanceof ValueWeakReference)) {
        return false;
      }
      final ValueWeakReference<?, ?> that = (ValueWeakReference<?, ?>) obj;
      if (isNull()) {
        return that.isNull();
      }

      if (hashCode() != that.hashCode()) {
        return false;
      }
      final Object referent = get();
      return (referent != null) && (referent.equals(that.get()));
    }

    K getKey() {
      return key;
    }

    @SuppressWarnings("unchecked")
    V getValue() {
      return (V) get();
    }

    boolean isNull() {
      return isNull;
    }
  }

  private class EntryIterator implements Iterator<Entry<K, V>> {

    private final Iterator<Entry<K, ValueWeakReference<K, V>>> iterator = map.entrySet().iterator();

    public boolean hasNext() {
      return iterator.hasNext();
    }

    public Entry<K, V> next() {
      return new WeakEntry(iterator.next());
    }

    public void remove() {
      iterator.remove();
    }
  }

  private class ValueIterator implements Iterator<V> {

    private final Iterator<ValueWeakReference<K, V>> iterator = map.values().iterator();

    public boolean hasNext() {
      return iterator.hasNext();
    }

    public V next() {
      final ValueWeakReference<K, V> reference = iterator.next();
      return (reference != null) ? reference.getValue() : null;
    }

    public void remove() {
      iterator.remove();
    }
  }

  private class WeakEntry implements Entry<K, V> {

    private final Entry<K, ValueWeakReference<K, V>> entry;

    private WeakEntry(final Entry<K, ValueWeakReference<K, V>> entry) {
      this.entry = entry;
    }

    public K getKey() {
      return entry.getKey();
    }

    public V getValue() {
      final ValueWeakReference<K, V> reference = entry.getValue();
      return (reference != null) ? reference.getValue() : null;
    }

    public V setValue(final V value) {
      return put(getKey(), value);
    }
  }
}
