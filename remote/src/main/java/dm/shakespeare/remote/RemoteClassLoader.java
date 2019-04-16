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

import java.io.File;
import java.io.FileInputStream;
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

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 04/09/2019.
 */
class RemoteClassLoader extends ClassLoader {

  private final HashMap<String, File> mClassMap = new HashMap<String, File>();
  private final ProtectionDomain mProtectionDomain;

  RemoteClassLoader(@NotNull final ClassLoader classLoader,
      @Nullable final ProtectionDomain protectionDomain) {
    super(classLoader);
    mProtectionDomain = protectionDomain;
  }

  RemoteClassLoader(@Nullable final ProtectionDomain protectionDomain) {
    mProtectionDomain = protectionDomain;
  }

  @Override
  protected Class<?> loadClass(final String name, final boolean resolve) throws
      ClassNotFoundException {
    final String path = name.replace(".", "/") + ".class";
    final File file = mClassMap.get(path);
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
    final File file = mClassMap.get(path);
    if (file != null) {
      try {
        return file.toURI().toURL();

      } catch (final MalformedURLException e) {
        throw new IllegalArgumentException(e);
      }
    }
    return super.findResource(path);
  }

  @Override
  protected Enumeration<URL> findResources(final String path) throws IOException {
    final ArrayList<URL> urls = new ArrayList<URL>();
    if (path.isEmpty()) {
      for (final File file : mClassMap.values()) {
        try {
          urls.add(file.toURI().toURL());

        } catch (final MalformedURLException e) {
          throw new IOException(e);
        }
      }

    } else {
      for (final Entry<String, File> entry : mClassMap.entrySet()) {
        if (entry.getKey().startsWith(path)) {
          try {
            urls.add(entry.getValue().toURI().toURL());

          } catch (final MalformedURLException e) {
            throw new IOException(e);
          }
        }
      }
    }
    urls.addAll(Collections.list(super.findResources(path)));
    return Collections.enumeration(urls);
  }

  void register(@NotNull final String path, @NotNull final File file) {
    if (!file.isFile()) {
      throw new IllegalArgumentException();
    }
    mClassMap.put(ConstantConditions.notNull("path", path), file);
  }
}
