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

package dm.shakespeare.remote.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Proxy;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 04/09/2019.
 */
public class ClassLoaderObjectInputStream extends ObjectInputStream {

  private final ClassLoader classLoader;

  public ClassLoaderObjectInputStream(@NotNull final ClassLoader classLoader,
      @NotNull final InputStream inputStream) throws IOException {
    super(inputStream);
    this.classLoader = ConstantConditions.notNull("classLoader", classLoader);
  }

  @Override
  protected Class<?> resolveClass(@NotNull final ObjectStreamClass desc) throws IOException,
      ClassNotFoundException {
    try {
      return Class.forName(desc.getName(), false, classLoader);

    } catch (final ClassNotFoundException ignored) {
      return super.resolveClass(desc);
    }
  }

  @Override
  protected Class<?> resolveProxyClass(@NotNull final String[] interfaces) throws IOException,
      ClassNotFoundException {
    final ClassLoader classLoader = this.classLoader;
    final int length = interfaces.length;
    final Class<?>[] interfaceClasses = new Class[length];
    for (int i = 0; i < length; ++i) {
      interfaceClasses[i] = Class.forName(interfaces[i], false, classLoader);
    }
    try {
      return Proxy.getProxyClass(classLoader, interfaceClasses);

    } catch (final IllegalArgumentException ignored) {
      return super.resolveProxyClass(interfaces);
    }
  }
}
