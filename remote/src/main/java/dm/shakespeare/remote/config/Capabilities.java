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
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by davide-maestroni on 04/09/2019.
 */
public class Capabilities extends HashMap<String, String> {

  public static final String CREATE_REMOTE = "sks.create.remote";
  public static final String DISMISS_REMOTE = "sks.dismiss.remote";
  public static final String LOAD_REMOTE = "sks.load.remote";

  public Capabilities() {
  }

  public Capabilities(final int initialCapacity) {
    super(initialCapacity);
  }

  public Capabilities(final int initialCapacity, final float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  public Capabilities(@Nullable final Map<? extends String, ? extends String> map) {
    super((map != null) ? map : Collections.<String, String>emptyMap());
  }

  public boolean asBoolean(final String key) {
    return checkTrue(key);
  }

  public boolean asBoolean(final String key, final boolean defaultValue) {
    final String value = get(key);
    return (value != null) ? Boolean.TRUE.toString().equalsIgnoreCase(value) : defaultValue;
  }

  public double asDouble(final String key) {
    return Double.parseDouble(get(key));
  }

  public double asDouble(final String key, final double defaultValue) {
    final String value = get(key);
    return (value != null) ? Double.parseDouble(value) : defaultValue;
  }

  public float asFloat(final String key) {
    return Float.parseFloat(get(key));
  }

  public float asFloat(final String key, final float defaultValue) {
    final String value = get(key);
    return (value != null) ? Float.parseFloat(value) : defaultValue;
  }

  public int asInteger(final String key) {
    return Integer.decode(get(key));
  }

  public int asInteger(final String key, final int defaultValue) {
    final String value = get(key);
    return (value != null) ? Integer.decode(value) : defaultValue;
  }

  public long asLong(final String key) {
    return Long.decode(get(key));
  }

  public long asLong(final String key, final long defaultValue) {
    final String value = get(key);
    return (value != null) ? Long.decode(value) : defaultValue;
  }

  public boolean checkFalse(final String key) {
    return Boolean.FALSE.toString().equalsIgnoreCase(get(key));
  }

  public boolean checkTrue(final String key) {
    return Boolean.TRUE.toString().equalsIgnoreCase(get(key));
  }

  @NotNull
  public Capabilities putValue(final String key, final Object value) {
    put(key, (value != null) ? value.toString() : null);
    return this;
  }
}
