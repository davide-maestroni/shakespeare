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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Utility class providing several methods to handle iterable instances.
 */
public class Iterables {

  /**
   * Avoid explicit instantiation.
   */
  private Iterables() {
    ConstantConditions.avoid();
  }

  /**
   * Adds all the iterable elements to the specified collection.
   *
   * @param iterable   the source iterable.
   * @param collection the target collection.
   * @param <T>        the iterable elements type.
   * @param <C>        the collection elements type.
   * @return the filled collection.
   */
  @NotNull
  public static <T, C extends Collection<T>> C addAll(@NotNull final Iterable<? extends T> iterable,
      @NotNull final C collection) {
    for (final T element : iterable) {
      collection.add(element);
    }
    return ConstantConditions.notNull("collection", collection);
  }

  /**
   * Converts the specified iterable into a {@code List}.<br>
   * If the specified instance already implements a {@code List} interface the method will just
   * return it, otherwise a new collection will be created and filled with the iterable elements
   * (see {@link #toList(Iterable)}).
   *
   * @param iterable the iterable to convert.
   * @param <T>      the iterable elements type.
   * @return the collection containing the iterable elements.
   */
  @NotNull
  public static <T> List<T> asList(@NotNull final Iterable<T> iterable) {
    if (iterable instanceof List) {
      return (List<T>) iterable;
    }
    return toList(iterable);
  }

  /**
   * Converts the specified iterable into a {@code Set}.<br>
   * If the specified instance already implements a {@code Set} interface the method will just
   * return it, otherwise a new collection will be created and filled with the iterable elements
   * (see {@link #toSet(Iterable)}).
   *
   * @param iterable the iterable to convert.
   * @param <T>      the iterable elements type.
   * @return the collection containing the iterable elements.
   */
  @NotNull
  public static <T> Set<T> asSet(@NotNull final Iterable<T> iterable) {
    if (iterable instanceof Set) {
      return (Set<T>) iterable;
    }
    return toSet(iterable);
  }

  /**
   * Concatenates the specified iterables elements into a new iterable.<br>
   * The returned iterators might support different operations based on the underlying iterable
   * instances. For example, one iterable might support elements removal and another not.
   *
   * @param iterables the iterables.
   * @param <T>       the iterables elements type.
   * @return the concatenating iterable.
   */
  @NotNull
  public static <T> Iterable<T> concat(
      @NotNull final Iterable<? extends Iterable<? extends T>> iterables) {
    return new ConcatIterable<T>(iterables);
  }

  /**
   * Verifies that an object is part of the specified iterable elements (see
   * {@link Collection#contains(Object)}).
   *
   * @param iterable the iterable.
   * @param o        the object whose presence is to be tested.
   * @return {@code true} if at least one iterable element is equal to the object.
   */
  public static boolean contains(@NotNull final Iterable<?> iterable, final Object o) {
    if (iterable instanceof Collection) {
      return ((Collection<?>) iterable).contains(o);
    }

    if (o != null) {
      for (final Object object : iterable) {
        if ((object == o) || o.equals(object)) {
          return true;
        }
      }

    } else {
      for (final Object object : iterable) {
        if (object == null) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Verifies that all the elements of a collection are part in the specified iterable ones (see
   * {@link Collection#containsAll(Collection)}).
   *
   * @param iterable   the iterable.
   * @param collection the collection to be checked for containment.
   * @return {@code true} if all the elements in the collection are equal to at least one iterable
   * element.
   */
  public static boolean containsAll(@NotNull final Iterable<?> iterable,
      @NotNull final Collection<?> collection) {
    if (iterable instanceof Collection) {
      return ((Collection<?>) iterable).containsAll(collection);
    }
    for (final Object object : collection) {
      boolean found = false;
      for (final Object next : iterable) {
        if ((object == next) || ((next != null) && next.equals(object))) {
          found = true;
          break;
        }
      }

      if (!found) {
        return false;
      }
    }
    ConstantConditions.notNull("iterable", iterable);
    return true;
  }

  /**
   * Returns the first element of the specified iterable.
   *
   * @param iterable the iterable instance.
   * @param <T>      the iterables elements type.
   * @return the first element.
   * @throws NoSuchElementException if the iterable contains no elements.
   */
  public static <T> T first(@NotNull final Iterable<T> iterable) {
    return get(iterable, 0);
  }

  /**
   * Returns the element of the specified iterable at the position {@code index}.<br>
   * If the passed iterable implements a {@code List} interface the method {@link List#get(int)}
   * will be called.
   *
   * @param iterable the iterable instance.
   * @param index    the index of the element to return
   * @param <T>      the iterables elements type.
   * @return the element at the specified position.
   * @throws NoSuchElementException if the index is out of range.
   */
  public static <T> T get(@NotNull final Iterable<T> iterable, final int index) {
    if (iterable instanceof List) {
      try {
        return ((List<T>) iterable).get(index);

      } catch (final IndexOutOfBoundsException e) {
        throw new NoSuchElementException(e.getMessage());
      }
    }
    if (index < 0) {
      throw new NoSuchElementException();
    }
    final Iterator<T> iterator = iterable.iterator();
    int i = 0;
    while (++i <= index) {
      iterator.next();
    }
    return iterator.next();
  }

  /**
   * Verifies that the specified iterable is empty.
   *
   * @param iterable the iterable instance.
   * @return {@code true} if the iterable contains no elements.
   */
  public static boolean isEmpty(@NotNull final Iterable<?> iterable) {
    return (iterable instanceof Collection) ? ((Collection<?>) iterable).isEmpty()
        : !iterable.iterator().hasNext();
  }

  /**
   * Removes the first occurrence of the specified object from the iterable elements (see
   * {@link Collection#remove(Object)}).
   *
   * @param iterable the iterable instance.
   * @param o        the object to remove.
   * @return {@code true} if an element was removed as a result of this call.
   * @throws UnsupportedOperationException if the remove operation is not supported by the iterable.
   */
  public static boolean remove(@NotNull final Iterable<?> iterable, final Object o) {
    if (iterable instanceof Collection) {
      return ((Collection<?>) iterable).remove(o);
    }

    if (o != null) {
      final Iterator<?> iterator = iterable.iterator();
      while (iterator.hasNext()) {
        final Object object = iterator.next();
        if ((object == o) || o.equals(object)) {
          iterator.remove();
          return true;
        }
      }

    } else {
      final Iterator<?> iterator = iterable.iterator();
      while (iterator.hasNext()) {
        final Object object = iterator.next();
        if (object == null) {
          iterator.remove();
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Removes all of the elements of a collection from the specified iterable ones (see
   * {@link Collection#removeAll(Collection)}).<br>
   * After this call returns, the iterable will contain no elements in common with the specified
   * collection.
   *
   * @param iterable   the iterable instance.
   * @param collection the collection of object to remove.
   * @return {@code true} if at least one element was removed as a result of this call.
   * @throws UnsupportedOperationException if the remove operation is not supported by the iterable.
   */
  @SuppressWarnings("unchecked")
  public static boolean removeAll(@NotNull final Iterable<?> iterable,
      @NotNull final Collection<?> collection) {
    if (iterable instanceof Collection) {
      return ((Collection<Object>) iterable).removeAll(collection);
    }
    boolean found = false;
    for (final Object element : collection) {
      if (element != null) {
        final Iterator<?> iterator = iterable.iterator();
        while (iterator.hasNext()) {
          final Object object = iterator.next();
          if ((object == element) || element.equals(object)) {
            iterator.remove();
            found = true;
          }
        }

      } else {
        final Iterator<?> iterator = iterable.iterator();
        while (iterator.hasNext()) {
          final Object object = iterator.next();
          if (object == null) {
            iterator.remove();
            found = true;
          }
        }
      }
    }
    ConstantConditions.notNull("iterable", iterable);
    return found;
  }

  /**
   * Retains only the elements of a collection by removing all the other objects from the specified
   * iterable ones (see {@link Collection#retainAll(Collection)}).
   *
   * @param iterable   the iterable instance.
   * @param collection the collection of object to retain.
   * @return {@code true} if at least one element was removed as a result of this call.
   * @throws UnsupportedOperationException if the remove operation is not supported by the iterable.
   */
  @SuppressWarnings("SuspiciousMethodCalls")
  public static boolean retainAll(@NotNull final Iterable<?> iterable,
      @NotNull final Collection<?> collection) {
    if (iterable instanceof Collection) {
      return ((Collection<?>) iterable).retainAll(collection);
    }
    boolean isModified = false;
    final Iterator<?> iterator = iterable.iterator();
    while (iterator.hasNext()) {
      if (!collection.contains(iterator.next())) {
        iterator.remove();
        isModified = true;
      }
    }
    ConstantConditions.notNull("collection", collection);
    return isModified;
  }

  /**
   * Returns the number of elements of the specified iterable.
   *
   * @param iterable the iterable instance.
   * @return the number of elements.
   */
  public static int size(@NotNull final Iterable<?> iterable) {
    int size;
    if (iterable instanceof Collection) {
      size = ((Collection<?>) iterable).size();

    } else {
      size = 0;
      for (final Iterator<?> iterator = iterable.iterator(); iterator.hasNext(); iterator.next()) {
        ++size;
      }
    }
    return size;
  }

  /**
   * Returns an array containing all of the elements of the specified iterable (see
   * {@link Collection#toArray()}).
   *
   * @param iterable the iterable instance.
   * @return an array containing all of the iterable elements.
   */
  @NotNull
  public static Object[] toArray(@NotNull final Iterable<?> iterable) {
    if (iterable instanceof Collection) {
      return ((Collection<?>) iterable).toArray();
    }
    return toList(iterable).toArray();
  }

  /**
   * Returns an array containing all of the elements of the specified iterable (see
   * {@link Collection#toArray(Object[])}).<br>
   * If the elements fit in the specified array, it is returned therein. Otherwise, a new array is
   * allocated with the runtime type of the specified array and the size of the iterable elements.
   * <br>
   * If the elements fit in the specified array with room to spare (i.e., the array has more
   * elements than the iterable), the element in the array immediately following the end of the
   * collection is set to null.
   *
   * @param iterable the iterable instance.
   * @param array    the array into which the elements are to be stored.
   * @param <T>      the runtime type of the array to contain the collection.
   * @return an array containing the elements of the iterable.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static <T> T[] toArray(@NotNull final Iterable<? extends T> iterable, @NotNull T[] array) {
    if (iterable instanceof Collection) {
      return ((Collection<? extends T>) iterable).toArray(array);
    }
    return toList(iterable).toArray(array);
  }

  /**
   * Returns a new {@code List} containing all the elements of the specified iterable in the same
   * order.
   *
   * @param iterable the iterable instance.
   * @param <T>      the iterables elements type.
   * @return the collection containing the iterable elements.
   */
  @NotNull
  public static <T> List<T> toList(@NotNull final Iterable<? extends T> iterable) {
    return addAll(iterable, new ArrayList<T>());
  }

  /**
   * Returns a new {@code Set} containing all the elements of the specified iterable.
   *
   * @param iterable the iterable instance.
   * @param <T>      the iterables elements type.
   * @return the collection containing the iterable elements.
   */
  @NotNull
  public static <T> Set<T> toSet(@NotNull final Iterable<? extends T> iterable) {
    return addAll(iterable, new HashSet<T>());
  }

  /**
   * Returns a string representing the data in the specified iterable.
   *
   * @param iterable the iterable instance.
   * @return a string representation of the iterable elements.
   */
  @NotNull
  public static String toString(@Nullable final Iterable<?> iterable) {
    if (iterable == null) {
      return "null";
    }
    final Iterator<?> iterator = iterable.iterator();
    if (iterator.hasNext()) {
      final StringBuilder builder = new StringBuilder();
      builder.append('[');
      while (true) {
        builder.append(iterator.next());
        if (!iterator.hasNext()) {
          return builder.append(']').toString();
        }
        builder.append(',').append(' ');
      }
    }
    return "[]";
  }

  private static class ConcatIterable<T> implements Iterable<T> {

    private final Iterable<? extends Iterable<? extends T>> iterables;

    private ConcatIterable(@NotNull final Iterable<? extends Iterable<? extends T>> iterables) {
      this.iterables = ConstantConditions.notNullElements("iterables", iterables);
    }

    @NotNull
    public Iterator<T> iterator() {
      return new ConcatIterator<T>(iterables.iterator());
    }
  }

  private static class ConcatIterator<T> implements Iterator<T> {

    private final Iterator<? extends Iterable<? extends T>> iterables;
    private Iterator<? extends T> iterator = null;

    private ConcatIterator(@NotNull final Iterator<? extends Iterable<? extends T>> iterator) {
      iterables = iterator;
    }

    public boolean hasNext() {
      final Iterator<? extends Iterable<? extends T>> iterables = this.iterables;
      if (iterator == null) {
        if (iterables.hasNext()) {
          iterator = iterables.next().iterator();

        } else {
          return false;
        }
      }
      while (!iterator.hasNext()) {
        if (iterables.hasNext()) {
          iterator = iterables.next().iterator();

        } else {
          return false;
        }
      }
      return true;
    }

    public T next() {
      if (iterator == null) {
        final Iterator<? extends Iterable<? extends T>> iterables = this.iterables;
        if (iterables.hasNext()) {
          iterator = iterables.next().iterator();

        } else {
          throw new NoSuchElementException();
        }
      }
      return iterator.next();
    }

    public void remove() {
      if (iterator == null) {
        throw new IllegalStateException("next has not been called");
      }
      iterator.remove();
    }
  }
}
