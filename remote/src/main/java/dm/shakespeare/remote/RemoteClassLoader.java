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

package dm.shakespeare.remote;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import dm.shakespeare.remote.io.DataStore;
import dm.shakespeare.remote.io.DataStore.DataEntry;
import dm.shakespeare.remote.io.RawData;
import dm.shakespeare.remote.util.Classes;

/**
 * Created by davide-maestroni on 04/09/2019.
 */
class RemoteClassLoader extends ClassLoader {

  private final DataStore dataStore;
  private final Object mutex = new Object();
  private final HashMap<String, String> paths = new HashMap<String, String>();
  private final ProtectionDomain protectionDomain;

  RemoteClassLoader(@NotNull final ClassLoader classLoader,
      @Nullable final ProtectionDomain protectionDomain, @NotNull final DataStore dataStore) {
    super(classLoader);
    this.protectionDomain = protectionDomain;
    this.dataStore = dataStore;
    loadResources(dataStore);
  }

  RemoteClassLoader(@Nullable final ProtectionDomain protectionDomain,
      @NotNull final DataStore dataStore) {
    this.protectionDomain = protectionDomain;
    this.dataStore = dataStore;
    loadResources(dataStore);
  }

  private static String normalize(final String path) {
    return ((path != null) && !path.startsWith("/")) ? "/" + path : null;
  }

  @Override
  protected Class<?> loadClass(final String name, final boolean resolve) throws
      ClassNotFoundException {
    RawData data = null;
    final String path = Classes.toPath(name);
    synchronized (mutex) {
      try {
        data = dataStore.get(paths.get(path));

      } catch (final IOException e) {
        // TODO: 2019-06-27 ???
      }
    }
    if (data != null) {
      try {
        final InputStream inputStream = data.toInputStream();
        if (inputStream instanceof FileInputStream) {
          try {
            FileChannel channel = ((FileInputStream) inputStream).getChannel();
            final ByteBuffer buffer = channel.map(MapMode.READ_ONLY, 0, channel.size());
            final Class<?> definedClass = super.defineClass(name, buffer, protectionDomain);
            if (resolve) {
              super.resolveClass(definedClass);
            }
            return definedClass;

          } catch (final IOException e) {
            try {
              inputStream.close();

            } catch (final IOException ignored) {
            }
          }

        } else {
          final ByteBuffer buffer = ByteBuffer.wrap(data.toByteArray());
          final Class<?> definedClass = super.defineClass(name, buffer, protectionDomain);
          if (resolve) {
            super.resolveClass(definedClass);
          }
          return definedClass;
        }

      } catch (final IOException e) {
        // TODO: 2019-06-27 ???
      }
    }
    try {
      return super.loadClass(name, resolve);

    } catch (final ClassNotFoundException e) {
      throw new RemoteClassNotFoundException(path, e);
    }
  }

  @Override
  protected URL findResource(final String path) {
    final URL resource = super.findResource(path);
    synchronized (mutex) {
      if (resource == null) {
        final String name = paths.get(normalize(path));
        if (name != null) {
          try {
            return dataStore.getURL(name);

          } catch (final IOException e) {
            throw new IllegalArgumentException(e);
          }
        }
      }
    }
    return resource;
  }

  @Override
  protected Enumeration<URL> findResources(final String path) throws IOException {
    if (path == null) {
      return super.findResources(null);
    }
    final ArrayList<URL> urls = new ArrayList<URL>(Collections.list(super.findResources(path)));
    synchronized (mutex) {
      final String name = paths.get(normalize(path));
      if (name != null) {
        urls.add(dataStore.getURL(name));
      }
    }
    return Collections.enumeration(urls);
  }

  @NotNull
  Set<String> register(@NotNull final Map<String, RawData> resources) throws IOException {
    final HashSet<String> missingPaths = new HashSet<String>();
    for (final Entry<String, RawData> entry : resources.entrySet()) {
      final RawData data = register(entry.getKey(), entry.getValue());
      final InputStream inputStream = data.toInputStream();
      if (inputStream instanceof FileInputStream) {
        try {
          FileChannel channel = ((FileInputStream) inputStream).getChannel();
          final ByteBuffer buffer = channel.map(MapMode.READ_ONLY, 0, channel.size());
          fillMissingPaths(buffer, missingPaths);

        } catch (final IOException e) {
          try {
            inputStream.close();

          } catch (final IOException ignored) {
          }
          throw e;
        }

      } else {
        final ByteBuffer buffer = ByteBuffer.wrap(data.toByteArray());
        fillMissingPaths(buffer, missingPaths);
      }
    }
    return missingPaths;
  }

  private void fillMissingPaths(final ByteBuffer buffer, final HashSet<String> missingPaths) {
    final Set<String> dependencies = Classes.getDependencies(buffer);
    final HashMap<String, String> paths = this.paths;
    for (final String name : dependencies) {
      final String path = Classes.toPath(name);
      final boolean hasPath;
      synchronized (mutex) {
        hasPath = paths.containsKey(path);
      }

      if (!hasPath && !(name.startsWith("java.") || name.startsWith("javax.") || name.startsWith(
          "sun.") || name.startsWith("com.sun.") || name.startsWith("com.oracle."))) {
        try {
          Class.forName(name, false, getParent());

        } catch (final ClassNotFoundException ignored) {
          // add only if not already in the path
          missingPaths.add(path);
        }
      }
    }
  }

  private void loadResources(@NotNull final DataStore dataStore) {
    final HashMap<String, String> paths = this.paths;
    final HashSet<String> resources = new HashSet<String>();
    for (final DataEntry entry : dataStore) {
      final String name = entry.getKey();
      if (name.endsWith(".path")) {
        BufferedReader reader = null;
        try {
          reader = new BufferedReader(new InputStreamReader(entry.getData().toInputStream()));
          paths.put(reader.readLine(),
              name.substring(0, name.length() - ".path".length()) + ".raw");

        } catch (final IOException e) {
          // TODO: 18/04/2019 ???

        } finally {
          if (reader != null) {
            try {
              reader.close();

            } catch (final IOException e) {
              // TODO: 18/04/2019 ??
            }
          }
        }

      } else if (name.endsWith(".raw")) {
        resources.add(name);
      }
    }
    paths.values().retainAll(resources);
  }

  @NotNull
  private RawData register(@NotNull final String path, @NotNull final RawData data) throws
      IOException {
    final String name = Integer.toHexString(path.hashCode());
    synchronized (mutex) {
      dataStore.save(name + ".path", RawData.wrap(path.getBytes("UTF-8")));
      final RawData rawData = dataStore.save(name + ".raw", data);
      paths.put(path, name + ".raw");
      return rawData;
    }
  }
}
