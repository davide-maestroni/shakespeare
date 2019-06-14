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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import dm.shakespeare.remote.util.Classes;
import dm.shakespeare.remote.util.SerializableData;

/**
 * Created by davide-maestroni on 04/09/2019.
 */
class RemoteClassLoader extends ClassLoader {

  private final File container;
  private final Object mutex = new Object();
  private final HashMap<String, String> paths = new HashMap<String, String>();
  private final ProtectionDomain protectionDomain;
  private final HashMap<String, File> resources = new HashMap<String, File>();

  RemoteClassLoader(@NotNull final ClassLoader classLoader, @NotNull final File container,
      @Nullable final ProtectionDomain protectionDomain) {
    super(classLoader);
    if (!container.isDirectory()) {
      throw new IllegalArgumentException("container is not a directory");
    }
    this.container = container;
    this.protectionDomain = protectionDomain;
    loadResources(container);
  }

  RemoteClassLoader(@NotNull final File container,
      @Nullable final ProtectionDomain protectionDomain) {
    if (!container.isDirectory()) {
      throw new IllegalArgumentException("container is not a directory");
    }
    this.container = container;
    this.protectionDomain = protectionDomain;
    loadResources(container);
  }

  private static String normalize(final String path) {
    return ((path != null) && !path.startsWith("/")) ? "/" + path : null;
  }

  @NotNull
  private static String toPath(@NotNull final String name) {
    return "/" + name.replace(".", "/") + ".class";
  }

  @Override
  protected Class<?> loadClass(final String name, final boolean resolve) throws
      ClassNotFoundException {
    final File file;
    final String path = toPath(name);
    synchronized (mutex) {
      file = resources.get(paths.get(path));
    }
    if (file != null) {
      FileInputStream inputStream = null;
      try {
        inputStream = new FileInputStream(file);
        FileChannel channel = inputStream.getChannel();
        final ByteBuffer buffer = channel.map(MapMode.READ_ONLY, 0, channel.size());
        final Class<?> definedClass = super.defineClass(name, buffer, protectionDomain);
        if (resolve) {
          super.resolveClass(definedClass);
        }
        return definedClass;

      } catch (final IOException e) {
        if (inputStream != null) {
          try {
            inputStream.close();

          } catch (final IOException ignored) {
          }
        }
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
    if (resource == null) {
      final String name = paths.get(normalize(path));
      if (name != null) {
        final File file = resources.get(name);
        if (file != null) {
          try {
            return file.toURI().toURL();

          } catch (final MalformedURLException e) {
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
    final String name = paths.get(normalize(path));
    if (name != null) {
      synchronized (mutex) {
        final File file = resources.get(name);
        if (file != null) {
          try {
            urls.add(file.toURI().toURL());

          } catch (final MalformedURLException e) {
            throw new IllegalArgumentException(e);
          }
        }
      }
    }
    return Collections.enumeration(urls);
  }

  @NotNull
  File register(@NotNull final String path, @NotNull final SerializableData data) throws
      IOException {
    final String name = Integer.toHexString(path.hashCode());
    final FileOutputStream outputStream = new FileOutputStream(new File(container, name + ".path"));
    try {
      outputStream.write(path.getBytes("UTF-8"));
      final File file = new File(container, name + ".raw");
      data.copyTo(file);
      synchronized (mutex) {
        paths.put(path, name);
        resources.put(name, file);
      }
      return file;

    } finally {
      try {
        outputStream.close();

      } catch (final IOException e) {
        // TODO: 18/04/2019 ??
      }
    }
  }

  @NotNull
  Set<String> register(@NotNull final Map<String, SerializableData> resources) throws IOException {
    final HashSet<String> missingPaths = new HashSet<String>();
    for (final Entry<String, SerializableData> entry : resources.entrySet()) {
      final File file = register(entry.getKey(), entry.getValue());
      FileInputStream inputStream = null;
      try {
        inputStream = new FileInputStream(file);
        FileChannel channel = inputStream.getChannel();
        final ByteBuffer buffer = channel.map(MapMode.READ_ONLY, 0, channel.size());
        final Set<String> dependencies = Classes.getDependencies(buffer);
        final HashMap<String, String> paths = this.paths;
        for (final String name : dependencies) {
          final String path = toPath(name);
          final boolean hasPath;
          synchronized (mutex) {
            hasPath = paths.containsKey(path);
          }

          if (!hasPath && !(name.startsWith("java.") || name.startsWith("javax.")
              || name.startsWith("sun.") || name.startsWith("com.sun.") || name.startsWith(
              "com.oracle."))) {
            try {
              Class.forName(name, false, getParent());

            } catch (final ClassNotFoundException ignored) {
              // add only if not already in the path
              missingPaths.add(path);
            }
          }
        }

      } catch (final IOException e) {
        if (inputStream != null) {
          try {
            inputStream.close();

          } catch (final IOException ignored) {
          }
        }

        throw e;
      }
    }
    return missingPaths;
  }

  private void loadResources(@NotNull final File container) {
    final File[] files = container.listFiles();
    if (files != null) {
      final HashMap<String, String> paths = this.paths;
      final HashMap<String, File> resources = this.resources;
      for (final File file : files) {
        final String name = file.getName();
        if (name.endsWith(".path")) {
          BufferedReader reader = null;
          try {
            reader = new BufferedReader(new FileReader(file));
            paths.put(reader.readLine(), name.substring(0, name.length() - ".path".length()));

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
          resources.put(name.substring(0, name.length() - ".raw".length()), file);
        }
      }
      final Collection<String> names = paths.values();
      final Iterator<String> iterator = names.iterator();
      while (iterator.hasNext()) {
        final String name = iterator.next();
        if (!resources.containsKey(name)) {
          iterator.remove();
        }
      }
      resources.keySet().retainAll(names);
    }
  }
}
