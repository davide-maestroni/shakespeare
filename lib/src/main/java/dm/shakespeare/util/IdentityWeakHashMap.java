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
 * @param <K> the key type.
 * @param <V> the value type.
 */
public class IdentityWeakHashMap<K, V> implements Map<K, V> {

  private final HashMap<IdentityWeakReference, V> mMap;
  private final ProbeReference mProbe = new ProbeReference();
  private final ReferenceQueue<Object> mQueue = new ReferenceQueue<Object>();

  private volatile AbstractSet<Entry<K, V>> mEntrySet;
  private volatile AbstractSet<K> mKeySet;

  /**
   * Creates a new empty map with the default initial capacity (16) and the default load factor
   * (0.75).
   */
  public IdentityWeakHashMap() {
    mMap = new HashMap<IdentityWeakReference, V>();
  }

  /**
   * Creates a new empty map with the specified initial capacity and the default load factor (0.75).
   *
   * @param initialCapacity the initial capacity.
   * @throws IllegalArgumentException if the initial capacity is negative.
   */
  public IdentityWeakHashMap(final int initialCapacity) {
    mMap = new HashMap<IdentityWeakReference, V>(initialCapacity);
  }

  /**
   * Creates a new empty map with the specified initial capacity and load factor.
   *
   * @param initialCapacity the initial capacity.
   * @param loadFactor      the load factor.
   * @throws IllegalArgumentException if the initial capacity is negative or the load factor is
   *                                  not positive.
   */
  public IdentityWeakHashMap(final int initialCapacity, final float loadFactor) {
    mMap = new HashMap<IdentityWeakReference, V>(initialCapacity, loadFactor);
  }

  /**
   * Creates a new map with the same mappings as the specified {@code Map}.<br>
   * The map is created with default load factor (0.75) and an initial capacity sufficient to hold
   * the mappings in the specified {@code Map}.
   *
   * @param map the initial mapping.
   */
  public IdentityWeakHashMap(@NotNull final Map<? extends K, ? extends V> map) {
    mMap = new HashMap<IdentityWeakReference, V>(map.size());
    putAll(map);
  }

  @Override
  public int hashCode() {
    return mMap.hashCode();
  }

  /**
   * Removes from this map all the entries whose key has no more strong references.
   *
   * @return this map.
   */
  @NotNull
  public IdentityWeakHashMap<K, V> prune() {
    @SuppressWarnings("UnnecessaryLocalVariable") final HashMap<IdentityWeakReference, V> map =
        mMap;
    final ReferenceQueue<Object> queue = mQueue;
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
    return mMap.size();
  }

  /**
   * {@inheritDoc}
   */
  public boolean isEmpty() {
    return mMap.isEmpty();
  }

  /**
   * {@inheritDoc}
   */
  public boolean containsKey(final Object o) {
    return mMap.containsKey(mProbe.withReferent(o));
  }

  /**
   * {@inheritDoc}
   */
  public boolean containsValue(final Object o) {
    return mMap.containsValue(o);
  }

  /**
   * {@inheritDoc}
   */
  public V get(final Object o) {
    return mMap.get(mProbe.withReferent(o));
  }

  /**
   * {@inheritDoc}
   */
  public V put(final K k, final V v) {
    prune();
    return mMap.put(new IdentityWeakReference(k, mQueue), v);
  }

  /**
   * {@inheritDoc}
   */
  public V remove(final Object o) {
    prune();
    return mMap.remove(mProbe.withReferent(o));
  }

  /**
   * {@inheritDoc}
   */
  public void putAll(@NotNull final Map<? extends K, ? extends V> map) {
    prune();
    @SuppressWarnings("UnnecessaryLocalVariable") final ReferenceQueue<Object> queue = mQueue;
    @SuppressWarnings("UnnecessaryLocalVariable") final HashMap<IdentityWeakReference, V>
        referenceMap = mMap;
    for (final Entry<? extends K, ? extends V> entry : map.entrySet()) {
      referenceMap.put(new IdentityWeakReference(entry.getKey(), queue), entry.getValue());
    }
  }

  /**
   * {@inheritDoc}
   */
  public void clear() {
    mMap.clear();
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  public Set<K> keySet() {
    if (mKeySet == null) {
      mKeySet = new AbstractSet<K>() {

        @NotNull
        @Override
        public Iterator<K> iterator() {
          return new KeyIterator();
        }

        @Override
        public int size() {
          return mMap.size();
        }
      };
    }
    return mKeySet;
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  public Collection<V> values() {
    return mMap.values();
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  public Set<Entry<K, V>> entrySet() {
    if (mEntrySet == null) {
      mEntrySet = new AbstractSet<Entry<K, V>>() {

        @NotNull
        @Override
        public Iterator<Entry<K, V>> iterator() {
          return new EntryIterator();
        }

        @Override
        public int size() {
          return mMap.size();
        }
      };
    }
    return mEntrySet;
  }

  private static class IdentityWeakReference extends WeakReference<Object> {

    private final int mHashCode;
    private final boolean mIsNull;

    private IdentityWeakReference(final Object referent,
        final ReferenceQueue<? super Object> queue) {
      super(referent, queue);
      mIsNull = (referent == null);
      mHashCode = System.identityHashCode(referent);
    }

    private IdentityWeakReference(final Object referent) {
      super(referent);
      mIsNull = (referent == null);
      mHashCode = System.identityHashCode(referent);
    }

    boolean isNull() {
      return mIsNull;
    }

    @Override
    public int hashCode() {
      return mHashCode;
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
  }

  private static class ProbeReference extends IdentityWeakReference {

    private int mHashCode;
    private boolean mIsNull;
    private Object mReferent;

    private ProbeReference() {
      super(null);
    }

    @Nullable
    @Override
    public Object get() {
      return mReferent;
    }

    @Override
    boolean isNull() {
      return mIsNull;
    }

    @Override
    public int hashCode() {
      return mHashCode;
    }

    @Override
    public boolean equals(final Object obj) {
      return super.equals(obj);
    }

    @NotNull
    ProbeReference withReferent(final Object referent) {
      mIsNull = (referent == null);
      mReferent = referent;
      mHashCode = System.identityHashCode(referent);
      return this;
    }
  }

  private class EntryIterator implements Iterator<Entry<K, V>> {

    private final Iterator<IdentityWeakReference> mIterator = mMap.keySet().iterator();

    public boolean hasNext() {
      return mIterator.hasNext();
    }

    public Entry<K, V> next() {
      return new WeakEntry(mIterator.next());
    }

    public void remove() {
      mIterator.remove();
    }
  }

  private class KeyIterator implements Iterator<K> {

    private final Iterator<IdentityWeakReference> mIterator = mMap.keySet().iterator();

    public boolean hasNext() {
      return mIterator.hasNext();
    }

    @SuppressWarnings("unchecked")
    public K next() {
      return (K) mIterator.next().get();
    }

    public void remove() {
      mIterator.remove();
    }
  }

  private class WeakEntry implements Entry<K, V> {

    private final IdentityWeakReference mReference;

    private WeakEntry(@NotNull final IdentityWeakReference key) {
      mReference = key;
    }

    @SuppressWarnings("unchecked")
    public K getKey() {
      return (K) mReference.get();
    }

    public V getValue() {
      return mMap.get(mReference);
    }

    public V setValue(final V v) {
      return mMap.put(mReference, v);
    }
  }

  @Override
  @SuppressWarnings("EqualsBetweenInconvertibleTypes")
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof IdentityWeakHashMap)) {
      return (o instanceof Map) && o.equals(this);
    }
    final IdentityWeakHashMap<?, ?> that = (IdentityWeakHashMap<?, ?>) o;
    return mMap.equals(that.mMap);
  }
}
