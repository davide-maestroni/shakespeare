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

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

import dm.shakespeare.config.BuildConfig;

/**
 * Class implementing a light-weight queue, storing elements into a dynamically increasing
 * circular buffer.<br>
 * Note that, even if the class implements the {@code Queue} interface, {@code null} elements are
 * supported, so the values returned by {@link #peek()} and {@link #poll()} methods might not be
 * used to detect whether the queue is empty or not.
 *
 * @param <E> the element type.
 */
public class CQueue<E> extends AbstractCollection<E> implements Queue<E>, Serializable {

  private static final int DEFAULT_SIZE = 1 << 3;
  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;
  private Object[] data;
  private int first;
  private int last;
  private int mask;
  private int size;

  /**
   * Creates a new empty queue with a pre-defined initial capacity.
   */
  public CQueue() {
    data = new Object[DEFAULT_SIZE];
    mask = DEFAULT_SIZE - 1;
  }

  /**
   * Creates a new empty queue with the specified minimum capacity.
   *
   * @param minCapacity the minimum capacity.
   * @throws IllegalArgumentException if the specified capacity is less than 1.
   */
  public CQueue(final int minCapacity) {
    final int msb = Integer.highestOneBit(ConstantConditions.positive("minCapacity", minCapacity));
    final int initialCapacity = (minCapacity == msb) ? msb : msb << 1;
    data = new Object[initialCapacity];
    mask = initialCapacity - 1;
  }

  /**
   * Adds the specified element as the first element of the queue.<br>
   * The element can be null.
   *
   * @param element the element to add.
   */
  public void addFirst(@Nullable final E element) {
    int mask = this.mask;
    int newFirst = (first = (first - 1) & mask);
    data[newFirst] = element;
    if (newFirst == last) {
      doubleCapacity();
    }
    ++size;
  }

  /**
   * Adds the specified element to end of the queue.<br>
   * The element can be null.
   *
   * @param element the element to add.
   */
  public void addLast(@Nullable final E element) {
    final int last = this.last;
    data[last] = element;
    if (first == (this.last = (last + 1) & mask)) {
      doubleCapacity();
    }
    ++size;
  }

  /**
   * Removes all the elements from this queue and puts them into the specified array, starting from
   * {@code destPos} position.<br>
   * If the array is bigger than the required length, the remaining elements will stay untouched,
   * and the number of transferred data will be returned.<br>
   * On the contrary, if the array is not big enough to contain all the data, only the fitting
   * number of elements will be transferred, and a negative number, whose absolute value represents
   * the number of data still remaining in the queue, will be returned.<br>
   * If the queue is empty, {@code 0} will be returned.
   *
   * @param dst     the destination array.
   * @param destPos the destination position in the array.
   * @param <T>     the array component type.
   * @return the number of transferred elements or the negated number of elements still remaining
   * in the queue.
   */
  @SuppressWarnings("unchecked")
  public <T> int drainTo(@NotNull final T[] dst, final int destPos) {
    final Object[] data = this.data;
    final int mask = this.mask;
    final int last = this.last;
    final int length = dst.length;
    int i = first;
    int n = destPos;
    while (i != last) {
      if (n == length) {
        first = i;
        return -(size -= (n - destPos));
      }
      dst[n++] = (T) data[i];
      data[i] = null;
      i = (i + 1) & mask;
    }
    first = 0;
    this.last = 0;
    size = 0;
    return (n - destPos);
  }

  /**
   * Removes at maximum {@code maxElements} number of elements from this queue and puts them into
   * the specified array, starting from {@code destPos} position.<br>
   * If the array is bigger than the required length, the remaining elements will stay untouched,
   * and the number of transferred data will be returned.<br>
   * On the contrary, if the array is not big enough to contain all the data, only the fitting
   * number of elements will be transferred, and a negative number, whose absolute value represents
   * the number of data still remaining in the queue, will be returned.<br>
   * If the queue is empty, {@code 0} will be returned.
   *
   * @param dst         the destination array.
   * @param destPos     the destination position in the array.
   * @param maxElements the maximum number of elements to remove.
   * @param <T>         the array component type.
   * @return the number of transferred elements or the negated number of elements still remaining
   * in the queue.
   */
  @SuppressWarnings("unchecked")
  public <T> int drainTo(@NotNull final T[] dst, final int destPos, final int maxElements) {
    final Object[] data = this.data;
    final int mask = this.mask;
    final int last = this.last;
    final int length = dst.length;
    int i = first;
    int n = destPos;
    int c = 0;
    while (i != last) {
      if ((n == length) || (++c > maxElements)) {
        first = i;
        return -(size -= (n - destPos));
      }
      dst[n++] = (T) data[i];
      data[i] = null;
      i = (i + 1) & mask;
    }
    first = 0;
    this.last = 0;
    size = 0;
    return (n - destPos);
  }

  /**
   * Removes all the elements from this queue and adds them to the specified collection.
   *
   * @param collection the collection to fill.
   */
  @SuppressWarnings("unchecked")
  public void drainTo(@NotNull final Collection<? super E> collection) {
    final Object[] data = this.data;
    final int mask = this.mask;
    final int last = this.last;
    int i = first;
    while (i != last) {
      collection.add((E) data[i]);
      data[i] = null;
      i = (i + 1) & mask;
    }
    first = 0;
    this.last = 0;
    size = 0;
  }

  /**
   * Removes at maximum {@code maxElements} number of elements from this queue and adds them to
   * the specified collection.
   *
   * @param collection  the collection to fill.
   * @param maxElements the maximum number of elements to remove.
   */
  @SuppressWarnings("unchecked")
  public void drainTo(@NotNull final Collection<? super E> collection, final int maxElements) {
    final Object[] data = this.data;
    final int mask = this.mask;
    final int last = this.last;
    int i = first;
    int c = 0;
    while (i != last) {
      if (c >= maxElements) {
        first = i;
        size -= c;
        return;
      }
      ++c;
      collection.add((E) data[i]);
      data[i] = null;
      i = (i + 1) & mask;
    }
    first = 0;
    this.last = 0;
    size = 0;
  }

  /**
   * Returns the element at the specified position in this queue.
   *
   * @param index index of the element to return.
   * @return the element at the specified position in this queue.
   * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= size()).
   */
  @SuppressWarnings("unchecked")
  public E get(final int index) {
    if ((index < 0) || (index >= size())) {
      throw new IndexOutOfBoundsException(Integer.toString(index));
    }
    return (E) data[(first + index) & mask];
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  public Iterator<E> iterator() {
    return new CQueueIterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return size;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  public Object[] toArray() {
    return copyElements(new Object[size()]);
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public <T> T[] toArray(@NotNull T[] array) {
    int size = size();
    if (array.length < size) {
      array = (T[]) Array.newInstance(array.getClass().getComponentType(), size);
      copyElements(array);

    } else {
      copyElements(array);
      if (array.length > size) {
        array[size] = null;
      }
    }
    return array;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean add(@Nullable final E element) {
    addLast(element);
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear() {
    final int mask = this.mask;
    final int last = this.last;
    final Object[] data = this.data;
    int index = first;
    while (index != last) {
      data[index] = null;
      index = (index + 1) & mask;
    }
    first = 0;
    this.last = 0;
    size = 0;
  }

  /**
   * {@inheritDoc}
   */
  public boolean offer(final E e) {
    addLast(e);
    return true;
  }

  /**
   * {@inheritDoc}
   */
  public E remove() {
    return removeFirst();
  }

  /**
   * {@inheritDoc}
   */
  public E poll() {
    if (isEmpty()) {
      return null;
    }
    return unsafeRemoveFirst();
  }

  /**
   * {@inheritDoc}
   */
  public E element() {
    return peekFirst();
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  public E peek() {
    if (isEmpty()) {
      return null;
    }
    return (E) data[first];
  }

  /**
   * Peeks the first element in the queue.
   *
   * @return the first element.
   * @throws NoSuchElementException if the queue is empty.
   */
  @SuppressWarnings("unchecked")
  public E peekFirst() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }
    return (E) data[first];
  }

  /**
   * Peeks the last element in the queue.
   *
   * @return the last element.
   * @throws NoSuchElementException if the queue is empty.
   */
  @SuppressWarnings("unchecked")
  public E peekLast() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }
    final int mask = this.mask;
    return (E) data[(last - 1) & mask];
  }

  /**
   * Removes the element at the specified position in this queue. Shifts any subsequent elements
   * to the left (subtracts one from their indices).
   *
   * @param index the index of the element to be removed.
   * @return the element that was removed from the queue.
   * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= size()).
   */
  public E remove(final int index) {
    final E element = get(index);
    removeElement((first + index) & mask);
    return element;
  }

  /**
   * Removes the first element in the queue.
   *
   * @return the first element.
   * @throws NoSuchElementException if the queue is empty.
   */
  public E removeFirst() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }
    return unsafeRemoveFirst();
  }

  /**
   * Removes the last element in the queue.
   *
   * @return the last element.
   * @throws NoSuchElementException if the queue is empty.
   */
  public E removeLast() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }
    return unsafeRemoveLast();
  }

  /**
   * Replaces the element at the specified position in this queue with the specified element.
   *
   * @param index   the index of the element to replace
   * @param element element to be stored at the specified position.
   * @return the element that was removed from the queue.
   * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= size()).
   */
  @SuppressWarnings("unchecked")
  public E set(final int index, @Nullable final E element) {
    if ((index < 0) || (index >= size())) {
      throw new IndexOutOfBoundsException(Integer.toString(index));
    }
    final Object[] data = this.data;
    final int pos = (first + index) & mask;
    final E old = (E) data[pos];
    data[pos] = element;
    return old;
  }

  @NotNull
  @SuppressWarnings("SuspiciousSystemArraycopy")
  private <T> T[] copyElements(@NotNull final T[] dst) {
    final Object[] data = this.data;
    final int first = this.first;
    final int last = this.last;
    if (first <= last) {
      System.arraycopy(data, first, dst, 0, size);

    } else {
      final int length = data.length - first;
      System.arraycopy(data, first, dst, 0, length);
      System.arraycopy(data, 0, dst, length, last);
    }
    return dst;
  }

  private void doubleCapacity() {
    final Object[] data = this.data;
    final int size = data.length;
    final int newSize = size << 1;
    if (newSize < size) {
      throw new OutOfMemoryError();
    }
    final int first = this.first;
    final int remainder = size - first;
    final Object[] newData = new Object[newSize];
    System.arraycopy(data, first, newData, 0, remainder);
    System.arraycopy(data, 0, newData, remainder, first);
    this.data = newData;
    this.first = 0;
    last = size;
    mask = newSize - 1;
  }

  private boolean removeElement(final int index) {
    final int first = this.first;
    final int last = this.last;
    final Object[] data = this.data;
    final int mask = this.mask;
    final int front = (index - first) & mask;
    final int back = (last - index) & mask;
    final boolean isForward;
    if (front <= back) {
      if (first <= index) {
        System.arraycopy(data, first, data, first + 1, front);

      } else {
        System.arraycopy(data, 0, data, 1, index);
        data[0] = data[mask];
        System.arraycopy(data, first, data, first + 1, mask - first);
      }
      this.data[first] = null;
      this.first = (first + 1) & mask;
      isForward = true;

    } else {
      if (index < last) {
        System.arraycopy(data, index + 1, data, index, back);

      } else {
        System.arraycopy(data, index + 1, data, index, mask - index);
        data[mask] = data[0];
        System.arraycopy(data, 1, data, 0, last);
      }
      this.last = (last - 1) & mask;
      isForward = false;
    }
    --size;
    return isForward;
  }

  @SuppressWarnings("unchecked")
  private E unsafeRemoveFirst() {
    final Object[] data = this.data;
    final int first = this.first;
    this.first = (first + 1) & mask;
    final Object output = data[first];
    data[first] = null;
    --size;
    return (E) output;
  }

  @SuppressWarnings("unchecked")
  private E unsafeRemoveLast() {
    final Object[] data = this.data;
    final int mask = this.mask;
    final int newLast = (last - 1) & mask;
    last = newLast;
    final Object output = data[newLast];
    data[newLast] = null;
    --size;
    return (E) output;
  }

  private class CQueueIterator implements Iterator<E> {

    private boolean isRemoved;
    private int originalFirst;
    private int originalLast;
    private int pointer;

    private CQueueIterator() {
      pointer = (originalFirst = first);
      originalLast = last;
    }

    public boolean hasNext() {
      return (pointer != originalLast);
    }

    @SuppressWarnings("unchecked")
    public E next() {
      final int pointer = this.pointer;
      final int originalLast = this.originalLast;
      if (pointer == originalLast) {
        throw new NoSuchElementException();
      }
      if ((first != originalFirst) || (last != originalLast)) {
        throw new ConcurrentModificationException();
      }
      isRemoved = false;
      this.pointer = (pointer + 1) & mask;
      return (E) data[pointer];
    }

    public void remove() {
      if (isRemoved) {
        throw new IllegalStateException("element already removed");
      }
      final int pointer = this.pointer;
      final int originalFirst = this.originalFirst;
      if (pointer == originalFirst) {
        throw new IllegalStateException();
      }
      if ((first != originalFirst) || (last != originalLast)) {
        throw new ConcurrentModificationException();
      }
      final int mask = CQueue.this.mask;
      final int index = (pointer - 1) & mask;
      if (removeElement(index)) {
        this.originalFirst = first;

      } else {
        originalLast = last;
        this.pointer = (this.pointer - 1) & mask;
      }
      isRemoved = true;
    }
  }
}
