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

import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Class implementing a light-weight queue, storing elements into a dynamically increasing
 * circular buffer.<br>
 * Note that, even if the class implements the {@code Queue} interface, {@code null} elements are
 * supported, so the values returned by {@link #peek()} and {@link #poll()} methods might not be
 * used to detect whether the queue is empty or not.
 *
 * @param <E> the element type.
 */
public class CQueue<E> extends AbstractCollection<E> implements Queue<E> {

  private static final int DEFAULT_SIZE = 1 << 3;

  private Object[] mData;
  private int mFirst;
  private int mLast;
  private int mMask;
  private int mSize;

  /**
   * Creates a new empty queue with a pre-defined initial capacity.
   */
  public CQueue() {
    mData = new Object[DEFAULT_SIZE];
    mMask = DEFAULT_SIZE - 1;
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
    mData = new Object[initialCapacity];
    mMask = initialCapacity - 1;
  }

  /**
   * Adds the specified element as the first element of the queue.<br>
   * The element can be null.
   *
   * @param element the element to add.
   */
  public void addFirst(@Nullable final E element) {
    int mask = mMask;
    int newFirst = (mFirst = (mFirst - 1) & mask);
    mData[newFirst] = element;
    if (newFirst == mLast) {
      doubleCapacity();
    }
    ++mSize;
  }

  /**
   * Adds the specified element to end of the queue.<br>
   * The element can be null.
   *
   * @param element the element to add.
   */
  public void addLast(@Nullable final E element) {
    final int last = mLast;
    mData[last] = element;
    if (mFirst == (mLast = (last + 1) & mMask)) {
      doubleCapacity();
    }
    ++mSize;
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
    final Object[] data = mData;
    final int mask = mMask;
    final int last = mLast;
    final int length = dst.length;
    int i = mFirst;
    int n = destPos;
    while (i != last) {
      if (n == length) {
        mFirst = i;
        return -(mSize -= (n - destPos));
      }
      dst[n++] = (T) data[i];
      data[i] = null;
      i = (i + 1) & mask;
    }
    mFirst = 0;
    mLast = 0;
    mSize = 0;
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
    final Object[] data = mData;
    final int mask = mMask;
    final int last = mLast;
    final int length = dst.length;
    int i = mFirst;
    int n = destPos;
    int c = 0;
    while (i != last) {
      if ((n == length) || (++c > maxElements)) {
        mFirst = i;
        return -(mSize -= (n - destPos));
      }
      dst[n++] = (T) data[i];
      data[i] = null;
      i = (i + 1) & mask;
    }
    mFirst = 0;
    mLast = 0;
    mSize = 0;
    return (n - destPos);
  }

  /**
   * Removes all the elements from this queue and adds them to the specified collection.
   *
   * @param collection the collection to fill.
   */
  @SuppressWarnings("unchecked")
  public void drainTo(@NotNull final Collection<? super E> collection) {
    final Object[] data = mData;
    final int mask = mMask;
    final int last = mLast;
    int i = mFirst;
    while (i != last) {
      collection.add((E) data[i]);
      data[i] = null;
      i = (i + 1) & mask;
    }
    mFirst = 0;
    mLast = 0;
    mSize = 0;
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
    final Object[] data = mData;
    final int mask = mMask;
    final int last = mLast;
    int i = mFirst;
    int c = 0;
    while (i != last) {
      if (c >= maxElements) {
        mFirst = i;
        mSize -= c;
        return;
      }
      ++c;
      collection.add((E) data[i]);
      data[i] = null;
      i = (i + 1) & mask;
    }
    mFirst = 0;
    mLast = 0;
    mSize = 0;
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
      throw new IndexOutOfBoundsException();
    }
    return (E) mData[(mFirst + index) & mMask];
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
    return mSize;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEmpty() {
    return mSize == 0;
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
    final int mask = mMask;
    final int last = mLast;
    final Object[] data = mData;
    int index = mFirst;
    while (index != last) {
      data[index] = null;
      index = (index + 1) & mask;
    }
    mFirst = 0;
    mLast = 0;
    mSize = 0;
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
    return (E) mData[mFirst];
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
    return (E) mData[mFirst];
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
    final int mask = mMask;
    return (E) mData[(mLast - 1) & mask];
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
    removeElement((mFirst + index) & mMask);
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
      throw new IndexOutOfBoundsException();
    }
    final Object[] data = mData;
    final int pos = (mFirst + index) & mMask;
    final E old = (E) data[pos];
    data[pos] = element;
    return old;
  }

  @NotNull
  @SuppressWarnings("SuspiciousSystemArraycopy")
  private <T> T[] copyElements(@NotNull final T[] dst) {
    final Object[] data = mData;
    final int first = mFirst;
    final int last = mLast;
    if (first <= last) {
      System.arraycopy(data, first, dst, 0, mSize);

    } else {
      final int length = data.length - first;
      System.arraycopy(data, first, dst, 0, length);
      System.arraycopy(data, 0, dst, length, last);
    }
    return dst;
  }

  private void doubleCapacity() {
    final Object[] data = mData;
    final int size = data.length;
    final int newSize = size << 1;
    if (newSize < size) {
      throw new OutOfMemoryError();
    }
    final int first = mFirst;
    final int remainder = size - first;
    final Object[] newData = new Object[newSize];
    System.arraycopy(data, first, newData, 0, remainder);
    System.arraycopy(data, 0, newData, remainder, first);
    mData = newData;
    mFirst = 0;
    mLast = size;
    mMask = newSize - 1;
  }

  private boolean removeElement(final int index) {
    final int first = mFirst;
    final int last = mLast;
    final Object[] data = mData;
    final int mask = mMask;
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
      mData[first] = null;
      mFirst = (first + 1) & mask;
      isForward = true;

    } else {
      if (index < last) {
        System.arraycopy(data, index + 1, data, index, back);

      } else {
        System.arraycopy(data, index + 1, data, index, mask - index);
        data[mask] = data[0];
        System.arraycopy(data, 1, data, 0, last);
      }
      mLast = (last - 1) & mask;
      isForward = false;
    }
    --mSize;
    return isForward;
  }

  @SuppressWarnings("unchecked")
  private E unsafeRemoveFirst() {
    final Object[] data = mData;
    final int first = mFirst;
    mFirst = (first + 1) & mMask;
    final Object output = data[first];
    data[first] = null;
    --mSize;
    return (E) output;
  }

  @SuppressWarnings("unchecked")
  private E unsafeRemoveLast() {
    final Object[] data = mData;
    final int mask = mMask;
    final int newLast = (mLast - 1) & mask;
    mLast = newLast;
    final Object output = data[newLast];
    data[newLast] = null;
    --mSize;
    return (E) output;
  }

  private class CQueueIterator implements Iterator<E> {

    private boolean mIsRemoved;
    private int mOriginalFirst;
    private int mOriginalLast;
    private int mPointer;

    private CQueueIterator() {
      mPointer = (mOriginalFirst = mFirst);
      mOriginalLast = mLast;
    }

    public boolean hasNext() {
      return (mPointer != mOriginalLast);
    }

    @SuppressWarnings("unchecked")
    public E next() {
      final int pointer = mPointer;
      final int originalLast = mOriginalLast;
      if (pointer == originalLast) {
        throw new NoSuchElementException();
      }
      if ((mFirst != mOriginalFirst) || (mLast != originalLast)) {
        throw new ConcurrentModificationException();
      }
      mIsRemoved = false;
      mPointer = (pointer + 1) & mMask;
      return (E) mData[pointer];
    }

    public void remove() {
      if (mIsRemoved) {
        throw new IllegalStateException("element already removed");
      }
      final int pointer = mPointer;
      final int originalFirst = mOriginalFirst;
      if (pointer == originalFirst) {
        throw new IllegalStateException();
      }
      if ((mFirst != originalFirst) || (mLast != mOriginalLast)) {
        throw new ConcurrentModificationException();
      }
      final int mask = mMask;
      final int index = (pointer - 1) & mask;
      if (removeElement(index)) {
        mOriginalFirst = mFirst;

      } else {
        mOriginalLast = mLast;
        mPointer = (mPointer - 1) & mask;
      }
      mIsRemoved = true;
    }
  }
}
