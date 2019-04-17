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

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import dm.shakespeare.remote.util.ClassLoaderObjectInputStream;
import dm.shakespeare.remote.util.SerializableData;

/**
 * Created by davide-maestroni on 04/16/2019.
 */
class JavaSerializer implements Serializer {

  @NotNull
  public Object deserialize(@NotNull final SerializableData data,
      @NotNull final ClassLoader classLoader) throws Exception {
    final ClassLoaderObjectInputStream objectInputStream =
        new ClassLoaderObjectInputStream(classLoader, data.toInputStream());
    final Object object;
    try {
      object = objectInputStream.readObject();

    } finally {
      objectInputStream.close();
    }
    return object;
  }

  @NotNull
  public byte[] serialize(@NotNull final Object o) throws Exception {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
    try {
      objectOutputStream.writeObject(o);

    } finally {
      objectOutputStream.close();
    }
    return outputStream.toByteArray();
  }
}
