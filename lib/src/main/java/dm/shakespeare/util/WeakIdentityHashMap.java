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
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Map implementation combining the features of {@link java.util.IdentityHashMap} and
 * {@link java.util.WeakHashMap}.<br>
 * Iterating through the map keys might produce one or more null values.
 *
 * @param <K> the keys runtime type.
 * @param <V> the values runtime type.
 */
public class WeakIdentityHashMap<K, V> implements Map<K, V> {

  private final HashMap<IdentityWeakReference, V> map;
  private final ProbeReference probe = new ProbeReference();
  private final ReferenceQueue<Object> queue = new ReferenceQueue<Object>();

  private volatile AbstractSet<Entry<K, V>> entrySet;
  private volatile AbstractSet<K> keySet;

  /**
   * Creates a new empty map with the default initial capacity (16) and the default load factor
   * (0.75).
   */
  public WeakIdentityHashMap() {
    map = new HashMap<IdentityWeakReference, V>();
  }

  /**
   * Creates a new empty map with the specified initial capacity and the default load factor (0.75).
   *
   * @param initialCapacity the initial capacity.
   * @throws IllegalArgumentException if the initial capacity is negative.
   */
  public WeakIdentityHashMap(final int initialCapacity) {
    map = new HashMap<IdentityWeakReference, V>(initialCapacity);
  }

  /**
   * Creates a new empty map with the specified initial capacity and load factor.
   *
   * @param initialCapacity the initial capacity.
   * @param loadFactor      the load factor.
   * @throws IllegalArgumentException if the initial capacity is negative or the load factor is
   *                                  not positive.
   */
  public WeakIdentityHashMap(final int initialCapacity, final float loadFactor) {
    map = new HashMap<IdentityWeakReference, V>(initialCapacity, loadFactor);
  }

  /**
   * Creates a new map with the same mappings as the specified {@code Map}.<br>
   * The map is created with default load factor (0.75) and an initial capacity sufficient to hold
   * the mappings in the specified {@code Map}.
   *
   * @param map the initial mapping.
   */
  public WeakIdentityHashMap(@NotNull final Map<? extends K, ? extends V> map) {
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

    if (!(o instanceof WeakIdentityHashMap)) {
      return (o instanceof Map) && o.equals(this);
    }
    final WeakIdentityHashMap<?, ?> that = (WeakIdentityHashMap<?, ?>) o;
    return map.equals(that.map);
  }

  /**
   * Removes from this map all the entries whose key has no more strong references.
   *
   * @return this map.
   */
  @NotNull
  public WeakIdentityHashMap<K, V> prune() {
    final HashMap<IdentityWeakReference, V> map = this.map;
    final ReferenceQueue<Object> queue = this.queue;
    IdentityWeakReference reference = (IdentityWeakReference) queue.poll();
    while (reference != null) {
      map.remove(reference);
      reference = (IdentityWeakReference) queue.poll();
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
    return map.containsKey(probe.withReferent(key));
  }

  /**
   * {@inheritDoc}
   */
  public boolean containsValue(final Object value) {
    return map.containsValue(value);
  }

  /**
   * {@inheritDoc}
   */
  public V get(final Object key) {
    return map.get(probe.withReferent(key));
  }

  /**
   * {@inheritDoc}
   */
  public V put(final K key, final V value) {
    prune();
    return map.put(new IdentityWeakReference(key, queue), value);
  }

  /**
   * {@inheritDoc}
   */
  public V remove(final Object key) {
    prune();
    return map.remove(probe.withReferent(key));
  }

  /**
   * {@inheritDoc}
   */
  public void putAll(@NotNull final Map<? extends K, ? extends V> map) {
    prune();
    final ReferenceQueue<Object> queue = this.queue;
    final HashMap<IdentityWeakReference, V> referenceMap = this.map;
    for (final Entry<? extends K, ? extends V> entry : map.entrySet()) {
      referenceMap.put(new IdentityWeakReference(entry.getKey(), queue), entry.getValue());
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
    if (keySet == null) {
      keySet = new AbstractSet<K>() {

        @NotNull
        @Override
        public Iterator<K> iterator() {
          return new KeyIterator();
        }

        @Override
        public int size() {
          return map.size();
        }
      };
    }
    return keySet;
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  public Collection<V> values() {
    return map.values();
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

  private static class IdentityWeakReference extends WeakReference<Object> {

    private final int hashCode;
    private final boolean isNull;

    private IdentityWeakReference(final Object referent,
        final ReferenceQueue<? super Object> queue) {
      super(referent, queue);
      isNull = (referent == null);
      hashCode = System.identityHashCode(referent);
    }

    private IdentityWeakReference(final Object referent) {
      super(referent);
      isNull = (referent == null);
      hashCode = System.identityHashCode(referent);
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

      if (!(obj instanceof IdentityWeakReference)) {
        return false;
      }
      final IdentityWeakReference that = (IdentityWeakReference) obj;
      if (isNull()) {
        return that.isNull();
      }

      if (hashCode() != that.hashCode()) {
        return false;
      }
      final Object referent = get();
      return (referent != null) && (referent == that.get());
    }

    boolean isNull() {
      return isNull;
    }
  }

  private static class ProbeReference extends IdentityWeakReference {

    private int hashCode;
    private boolean isNull;
    private Object referent;

    private ProbeReference() {
      super(null);
    }

    @Nullable
    @Override
    public Object get() {
      return referent;
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    public boolean equals(final Object obj) {
      return super.equals(obj);
    }

    @Override
    boolean isNull() {
      return isNull;
    }

    @NotNull
    ProbeReference withReferent(final Object referent) {
      isNull = (referent == null);
      this.referent = referent;
      hashCode = System.identityHashCode(referent);
      return this;
    }
  }

  private class EntryIterator implements Iterator<Entry<K, V>> {

    private final Iterator<IdentityWeakReference> iterator = map.keySet().iterator();

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

  private class KeyIterator implements Iterator<K> {

    private final Iterator<IdentityWeakReference> iterator = map.keySet().iterator();

    public boolean hasNext() {
      return iterator.hasNext();
    }

    @SuppressWarnings("unchecked")
    public K next() {
      return (K) iterator.next().get();
    }

    public void remove() {
      iterator.remove();
    }
  }

  private class WeakEntry implements Entry<K, V> {

    private final IdentityWeakReference reference;

    private WeakEntry(@NotNull final IdentityWeakReference key) {
      reference = key;
    }

    @SuppressWarnings("unchecked")
    public K getKey() {
      return (K) reference.get();
    }

    public V getValue() {
      return map.get(reference);
    }

    public V setValue(final V value) {
      return map.put(reference, value);
    }
  }
}
