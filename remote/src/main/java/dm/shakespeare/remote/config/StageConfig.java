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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 05/28/2019.
 */
public class StageConfig extends AbstractMap<String, Object> {

  private final HashMap<String, Object> map = new HashMap<String, Object>();

  @NotNull
  public static StageConfig from(@NotNull final Properties properties) {
    final StageConfig config = new StageConfig();
    for (final Entry<Object, Object> entry : properties.entrySet()) {
      final String key = ConstantConditions.notNull("key", entry.getKey()).toString();
      config.put(key, entry.getValue());
    }
    return config;
  }

  public <T> T getOption(@NotNull final Class<? extends T> type, final String key) {
    final Object value = map.get(key);
    return type.isInstance(value) ? type.cast(value) : null;
  }

  @Override
  public Object put(final String key, final Object value) {
    ConstantConditions.notNull("key", key);
    if (key.endsWith(".class")) {
      if (value instanceof String) {
        try {
          return map.put(key, Class.forName((String) value).newInstance());

        } catch (final RuntimeException e) {
          throw e;

        } catch (final Exception e) {
          throw new IllegalArgumentException(e);
        }

      } else if (value instanceof Class) {
        try {
          return map.put(key, ((Class) value).newInstance());

        } catch (final RuntimeException e) {
          throw e;

        } catch (final Exception e) {
          throw new IllegalArgumentException(e);
        }
      }

    } else if (key.endsWith(".list")) {
      if (value instanceof String) {
        final String[] parts = ((String) value).split("\\s*,\\s*");
        return map.put(key, Arrays.asList(parts));

      } else if (value instanceof Collection) {
        return map.put(key, new ArrayList<String>((Collection<? extends String>) value));

      } else if ((value != null) && value.getClass().isArray()) {
        final ArrayList<String> list = new ArrayList<String>();
        final int length = Array.getLength(value);
        for (int i = 0; i < length; ++i) {
          list.add((String) Array.get(value, i));
        }
        return map.put(key, list);
      }

    } else if (key.endsWith(".size")) {
      if (value instanceof String) {
        return map.put(key, Integer.parseInt((String) value));

      } else if (value instanceof Number) {
        return map.put(key, ((Number) value).intValue());
      }

    } else if (key.endsWith(".millis")) {
      if (value instanceof String) {
        return map.put(key, Long.parseLong((String) value));

      } else if (value instanceof Number) {
        return map.put(key, ((Number) value).longValue());
      }

    } else if (key.endsWith(".enable")) {
      if (value instanceof String) {
        return map.put(key, Boolean.parseBoolean((String) value));
      }
    }
    return map.put(key, value);
  }

  @NotNull
  @Override
  public Set<Entry<String, Object>> entrySet() {
    return map.entrySet();
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
}
