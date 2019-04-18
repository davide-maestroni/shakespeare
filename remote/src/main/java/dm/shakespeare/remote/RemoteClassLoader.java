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
import java.util.Map.Entry;

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

  @Override
  protected Class<?> loadClass(final String name, final boolean resolve) throws
      ClassNotFoundException {
    final String path = "/" + name.replace(".", "/") + ".class";
    final File file = mResources.get(mPaths.get(path));
    if (file != null) {
      FileChannel channel = null;
      try {
        channel = new FileInputStream(file).getChannel();
        final ByteBuffer buffer = channel.map(MapMode.READ_ONLY, 0, channel.size());
        final Class<?> definedClass = super.defineClass(name, buffer, mProtectionDomain);
        if (resolve) {
          super.resolveClass(definedClass);
        }
        return definedClass;

      } catch (final IOException e) {
        if (channel != null) {
          try {
            channel.close();

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
    final String name = mPaths.get(path);
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
    return super.findResource(path);
  }

  @Override
  protected Enumeration<URL> findResources(final String path) throws IOException {
    final ArrayList<URL> urls = new ArrayList<URL>();
    if (path.isEmpty()) {
      for (final String name : mPaths.values()) {
        final File file = mResources.get(name);
        if (file != null) {
          try {
            urls.add(file.toURI().toURL());

          } catch (final MalformedURLException e) {
            throw new IllegalArgumentException(e);
          }
        }
      }

    } else {
      for (final Entry<String, String> entry : mPaths.entrySet()) {
        // TODO: 18/04/2019 match??
        if (entry.getKey().startsWith(path)) {
          final File file = mResources.get(entry.getValue());
          if (file != null) {
            try {
              urls.add(file.toURI().toURL());

            } catch (final MalformedURLException e) {
              throw new IllegalArgumentException(e);
            }
          }
        }
      }
    }
    urls.addAll(Collections.list(super.findResources(path)));
    return Collections.enumeration(urls);
  }

  void register(@NotNull final String path, @NotNull final SerializableData data) throws
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

    } finally {
      try {
        outputStream.close();
      } catch (final IOException e) {
        // TODO: 18/04/2019 ??
      }
    }
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
