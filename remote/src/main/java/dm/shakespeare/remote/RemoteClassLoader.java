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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.security.ProtectionDomain;
import java.util.HashMap;

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
}
