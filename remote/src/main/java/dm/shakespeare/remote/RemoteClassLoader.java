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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import dm.shakespeare.remote.util.Classes;
import dm.shakespeare.remote.util.SerializableData;

/**
 * Created by davide-maestroni on 04/09/2019.
 */
class RemoteClassLoader extends ClassLoader {

  private final File mContainer;
  private final HashMap<String, String> mPaths = new HashMap<String, String>();
  private final ProtectionDomain mProtectionDomain;
  private final HashMap<String, File> mResources = new HashMap<String, File>();

  RemoteClassLoader(@NotNull final ClassLoader classLoader, @NotNull final File container,
      @Nullable final ProtectionDomain protectionDomain) {
    super(classLoader);
    if (!container.isDirectory()) {
      throw new IllegalArgumentException("container is not a directory");
    }
    mContainer = container;
    mProtectionDomain = protectionDomain;
    loadResources(container);
  }

  RemoteClassLoader(@NotNull final File container,
      @Nullable final ProtectionDomain protectionDomain) {
    if (!container.isDirectory()) {
      throw new IllegalArgumentException("container is not a directory");
    }
    mContainer = container;
    mProtectionDomain = protectionDomain;
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
    final File file = mResources.get(mPaths.get(toPath(name)));
    if (file != null) {
      FileInputStream inputStream = null;
      try {
        inputStream = new FileInputStream(file);
        FileChannel channel = inputStream.getChannel();
        final ByteBuffer buffer = channel.map(MapMode.READ_ONLY, 0, channel.size());
        final Class<?> definedClass = super.defineClass(name, buffer, mProtectionDomain);
        if (resolve) {
          super.resolveClass(definedClass);
        }
        return definedClass;

      } catch (final IOException e) {
        if (inputStream != null) {
          try {
            inputStream.close();

          } catch (IOException ignored) {
          }
        }
      }
    }
    try {
      return super.loadClass(name, resolve);

    } catch (final ClassNotFoundException e) {
      throw new RemoteClassNotFoundException(name, e);
    }
  }

  @Override
  protected URL findResource(final String path) {
    final URL resource = super.findResource(path);
    if (resource == null) {
      final String name = mPaths.get(normalize(path));
      if (name != null) {
        final File file = mResources.get(name);
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
    final String name = mPaths.get(normalize(path));
    if (name != null) {
      final File file = mResources.get(name);
      if (file != null) {
        try {
          urls.add(file.toURI().toURL());

        } catch (final MalformedURLException e) {
          throw new IllegalArgumentException(e);
        }
      }
    }
    return Collections.enumeration(urls);
  }

  @NotNull
  File register(@NotNull final String path, @NotNull final SerializableData data) throws
      IOException {
    final String name = Integer.toHexString(path.hashCode());
    final FileOutputStream outputStream =
        new FileOutputStream(new File(mContainer, name + ".path"));
    try {
      outputStream.write(path.getBytes("UTF-8"));
      final File file = new File(mContainer, name);
      data.copyTo(file);
      file.deleteOnExit();
      mPaths.put(path, name);
      mResources.put(name, file);
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
    final HashSet<String> paths = new HashSet<String>();
    for (final Entry<String, SerializableData> entry : resources.entrySet()) {
      final File file = register(entry.getKey(), entry.getValue());
      FileInputStream inputStream = null;
      try {
        inputStream = new FileInputStream(file);
        FileChannel channel = inputStream.getChannel();
        final ByteBuffer buffer = channel.map(MapMode.READ_ONLY, 0, channel.size());
        final Set<String> dependencies = Classes.getDependencies(buffer);
        for (final String name : dependencies) {
          paths.add(toPath(name));
        }

      } catch (final IOException e) {
        if (inputStream != null) {
          try {
            inputStream.close();

          } catch (IOException ignored) {
          }
        }

        throw e;
      }
    }
    paths.removeAll(mPaths.keySet());
    return paths;
  }

  private void loadResources(@NotNull final File container) {
    final File[] files = container.listFiles();
    if (files != null) {
      final HashMap<String, String> paths = mPaths;
      final HashMap<String, File> resources = mResources;
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

        } else {
          resources.put(name, file);
        }
      }
    }
  }
}
