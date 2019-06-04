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

package dm.shakespeare.remote.config;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 05/28/2019.
 */
public class StageConfig extends HashMap<String, Object> {

  private final HashMap<String, Object> map = new HashMap<String, Object>();

  private ConfigEntrySet entrySet;

  @NotNull
  public static StageConfig from(@NotNull final Properties properties) {
    final StageConfig config = new StageConfig();
    for (final Entry<Object, Object> entry : properties.entrySet()) {
      final String key = ConstantConditions.notNull("key", entry.getKey()).toString();
      config.put(key, entry.getValue());
    }
    return config;
  }

  @NotNull
  @Override
  public Set<Entry<String, Object>> entrySet() {
    return (entrySet != null) ? entrySet : (entrySet = new ConfigEntrySet(map.entrySet()));
  }

  public <T> T getOption(@NotNull final Class<? extends T> type, final String key) {
    final Object value = map.get(key);
    return type.isInstance(value) ? type.cast(value) : null;
  }

  @NotNull
  public StageConfig withOption(@NotNull final String key, final Object value) {
    put(key, value);
    return this;
  }

  @NotNull
  public StageConfig withOptions(@NotNull final Map<? extends String, ?> options) {
    ConstantConditions.notNullElements("keys", options.keySet());
    putAll(options);
    return this;
  }

  private static class ConfigEntrySet implements Set<Entry<String, Object>> {

    private final Set<Entry<String, Object>> set;

    private ConfigEntrySet(@NotNull final Set<Entry<String, Object>> set) {
      this.set = set;
    }

    public int size() {
      return set.size();
    }

    public boolean isEmpty() {
      return set.isEmpty();
    }

    public boolean contains(final Object o) {
      return set.contains(o);
    }

    @NotNull
    public Iterator<Entry<String, Object>> iterator() {
      return set.iterator();
    }

    @NotNull
    public Object[] toArray() {
      return set.toArray();
    }

    @NotNull
    @SuppressWarnings("SuspiciousToArrayCall")
    public <T> T[] toArray(@NotNull final T[] a) {
      return set.toArray(a);
    }

    @SuppressWarnings("unchecked")
    public boolean add(final Entry<String, Object> entry) {
      final String key = ConstantConditions.notNull("key", entry.getKey());
      final Object value = entry.getValue();
      if (key.endsWith(".class")) {
        if (value instanceof String) {
          try {
            return set.add(
                new SimpleEntry<String, Object>(key, Class.forName((String) value).newInstance()));

          } catch (final RuntimeException e) {
            throw e;

          } catch (final Exception e) {
            throw new IllegalArgumentException(e);
          }

        } else if (value instanceof Class) {
          try {
            return set.add(new SimpleEntry<String, Object>(key, ((Class) value).newInstance()));

          } catch (final RuntimeException e) {
            throw e;

          } catch (final Exception e) {
            throw new IllegalArgumentException(e);
          }
        }

      } else if (key.endsWith(".list")) {
        if (value instanceof String) {
          final String[] parts = ((String) value).split("\\s*,\\s*");
          return set.add(new SimpleEntry<String, Object>(key, Arrays.asList(parts)));

        } else if (value instanceof Collection) {
          return set.add(new SimpleEntry<String, Object>(key,
              new ArrayList<String>((Collection<? extends String>) value)));

        } else if ((value != null) && value.getClass().isArray()) {
          final ArrayList<String> list = new ArrayList<String>();
          final int length = Array.getLength(value);
          for (int i = 0; i < length; ++i) {
            list.add((String) Array.get(value, i));
          }
          return set.add(new SimpleEntry<String, Object>(key, list));
        }

      } else if (key.endsWith(".size")) {
        if (value instanceof String) {
          return set.add(new SimpleEntry<String, Object>(key, Integer.parseInt((String) value)));

        } else if (value instanceof Number) {
          return set.add(new SimpleEntry<String, Object>(key, ((Number) value).intValue()));
        }

      } else if (key.endsWith(".millis")) {
        if (value instanceof String) {
          return set.add(new SimpleEntry<String, Object>(key, Long.parseLong((String) value)));

        } else if (value instanceof Number) {
          return set.add(new SimpleEntry<String, Object>(key, ((Number) value).longValue()));
        }

      } else if (key.endsWith(".enable")) {
        if (value instanceof String) {
          return set.add(
              new SimpleEntry<String, Object>(key, Boolean.parseBoolean((String) value)));
        }
      }
      return set.add(entry);
    }

    public boolean remove(final Object o) {
      return set.remove(o);
    }

    public boolean containsAll(@NotNull final Collection<?> c) {
      return set.containsAll(c);
    }

    public boolean addAll(@NotNull final Collection<? extends Entry<String, Object>> c) {
      return set.addAll(c);
    }

    public boolean retainAll(@NotNull final Collection<?> c) {
      return set.retainAll(c);
    }

    public boolean removeAll(@NotNull final Collection<?> c) {
      return set.removeAll(c);
    }

    public void clear() {
      set.clear();
    }
  }
}
