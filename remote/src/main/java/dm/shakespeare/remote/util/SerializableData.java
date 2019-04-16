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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 04/13/2019.
 */
public abstract class SerializableData implements Serializable {

  private static final int DEFAULT_CHUNK_SIZE = 8192;

  @NotNull
  public static SerializableData wrap(@NotNull final byte[] bytes) {
    return new ByteArrayData(bytes);
  }

  @NotNull
  public static SerializableData wrap(@NotNull final ByteBuffer buffer) {
    return new ByteBufferData(buffer);
  }

  @NotNull
  public static SerializableData wrap(@NotNull final File file) {
    return new FileData(file);
  }

  @NotNull
  public static SerializableData wrap(@NotNull final InputStream inputStream) {
    return new InputStreamData(inputStream);
  }

  @NotNull
  public static SerializableData wrapNoCache(@NotNull final InputStream inputStream) {
    return new UncachedInputStreamData(inputStream);
  }

  public abstract long size() throws IOException;

  @NotNull
  public abstract byte[] toByteArray() throws IOException;

  @NotNull
  public abstract InputStream toInputStream() throws IOException;

  abstract void serialize(@NotNull ObjectOutputStream out) throws IOException;

  private static class ByteArrayData extends SerializableData {

    private final byte[] mData;

    private ByteArrayData(@NotNull final byte[] data) {
      mData = ConstantConditions.notNull("data", data);
    }

    public long size() {
      return mData.length;
    }

    Object writeReplace() throws ObjectStreamException {
      return new SerializableWrapper(this);
    }

    @NotNull
    public byte[] toByteArray() {
      return mData;
    }

    @NotNull
    public InputStream toInputStream() {
      return new ByteArrayInputStream(mData);
    }

    void serialize(@NotNull final ObjectOutputStream out) throws IOException {
      final byte[] data = mData;
      out.writeInt(data.length);
      out.write(data);
      out.writeInt(0);
    }
  }

  private static class ByteBufferData extends SerializableData {

    private final ByteBuffer mBuffer;

    private ByteBufferData(@NotNull final ByteBuffer buffer) {
      mBuffer = ConstantConditions.notNull("buffer", buffer);
    }

    Object writeReplace() throws ObjectStreamException {
      return new SerializableWrapper(this);
    }

    public long size() {
      return mBuffer.remaining();
    }

    @NotNull
    public byte[] toByteArray() {
      final ByteBuffer buffer = mBuffer;
      final byte[] data = new byte[buffer.remaining()];
      buffer.mark();
      buffer.get(data);
      buffer.reset();
      return data;
    }

    @NotNull
    public InputStream toInputStream() {
      return new ByteBufferInputStream(mBuffer);
    }

    void serialize(@NotNull final ObjectOutputStream out) throws IOException {
      final byte[] buffer = new byte[DEFAULT_CHUNK_SIZE];
      final ByteBufferInputStream inputStream = new ByteBufferInputStream(mBuffer);
      try {
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
          out.writeInt(length);
          out.write(buffer, 0, length);
        }
        out.writeInt(0);
        out.close();

      } finally {
        inputStream.close();
      }
    }
  }

  private static class FileData extends SerializableData {

    private final File mFile;

    private byte[] mData;

    private FileData(@NotNull final File file) {
      if (!file.isFile()) {
        throw new IllegalArgumentException();
      }
      mFile = file;
    }

    Object writeReplace() throws ObjectStreamException {
      return new SerializableWrapper(this);
    }

    public long size() {
      return mFile.length();
    }

    @NotNull
    public byte[] toByteArray() throws IOException {
      if (mData == null) {
        final File file = mFile;
        final long length = file.length();
        if (length > Integer.MAX_VALUE) {
          throw new IOException();
        }
        final byte[] data = new byte[(int) length];
        final FileInputStream inputStream = new FileInputStream(file);
        try {
          final MappedByteBuffer buffer =
              inputStream.getChannel().map(MapMode.READ_ONLY, 0, length);
          buffer.get(data);
          mData = data;

        } finally {
          inputStream.close();
        }
      }
      return mData;
    }

    @NotNull
    public InputStream toInputStream() throws IOException {
      if (mData == null) {
        return new FileInputStream(mFile);
      }
      return new ByteArrayInputStream(mData);
    }

    void serialize(@NotNull final ObjectOutputStream out) throws IOException {
      final byte[] data = mData;
      if (data != null) {
        out.write(data.length);
        out.write(data);
        out.writeInt(0);

      } else {
        final byte[] buffer = new byte[DEFAULT_CHUNK_SIZE];
        final FileInputStream inputStream = new FileInputStream(mFile);
        try {
          int length;
          while ((length = inputStream.read(buffer)) > 0) {
            out.writeInt(length);
            out.write(buffer, 0, length);
          }
          out.writeInt(0);
          out.close();

        } finally {
          inputStream.close();
        }
      }
    }
  }

  private static class InputStreamData extends SerializableData {

    private final InputStream mInput;

    private byte[] mData;

    private InputStreamData(@NotNull final InputStream input) {
      mInput = ConstantConditions.notNull("input", input);
    }

    Object writeReplace() throws ObjectStreamException {
      return new SerializableWrapper(this);
    }

    public long size() {
      final byte[] data = mData;
      return (data != null) ? data.length : -1;
    }

    @NotNull
    public byte[] toByteArray() throws IOException {
      if (mData == null) {
        final byte[] buffer = new byte[DEFAULT_CHUNK_SIZE];
        final InputStream inputStream = mInput;
        try {
          final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
          int length;
          while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
          }
          outputStream.close();
          mData = outputStream.toByteArray();

        } finally {
          inputStream.close();
        }
      }
      return mData;
    }

    @NotNull
    public InputStream toInputStream() throws IOException {
      return new ByteArrayInputStream(toByteArray());
    }

    void serialize(@NotNull final ObjectOutputStream out) throws IOException {
      final byte[] data = mData;
      if (data != null) {
        out.write(data.length);
        out.write(data);
        out.writeInt(0);

      } else {
        final byte[] buffer = new byte[DEFAULT_CHUNK_SIZE];
        final InputStream inputStream = mInput;
        try {
          final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
          int length;
          while ((length = inputStream.read(buffer)) > 0) {
            out.writeInt(length);
            out.write(buffer, 0, length);
            outputStream.write(buffer, 0, length);
          }
          out.writeInt(0);
          out.close();
          outputStream.close();
          mData = outputStream.toByteArray();

        } finally {
          inputStream.close();
        }
      }
    }
  }

  private static class SerializableWrapper extends SerializableData {

    private transient SerializableData mData;

    private SerializableWrapper(@NotNull final SerializableData data) {
      mData = data;
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
      in.defaultReadObject();
      File file = null;
      OutputStream outputStream = new ByteArrayOutputStream();
      try {
        byte[] buffer = new byte[DEFAULT_CHUNK_SIZE];
        long total = 0;
        int length;
        while ((length = in.readInt()) > 0) {
          if (buffer.length < length) {
            buffer = new byte[length];
          }
          in.readFully(buffer, 0, length);
          total += length;
          if ((outputStream instanceof ByteArrayOutputStream) && (total > DEFAULT_CHUNK_SIZE)) {
            file = File.createTempFile("sks", ".data");
            file.deleteOnExit();
            final FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(((ByteArrayOutputStream) outputStream).toByteArray());
            outputStream = fileOutputStream;
          }
          outputStream.write(buffer, 0, length);
        }
      } finally {
        outputStream.close();
      }

      if (outputStream instanceof ByteArrayOutputStream) {
        mData = new ByteArrayData(((ByteArrayOutputStream) outputStream).toByteArray());

      } else {
        mData = new FileData(file);
      }
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();
      serialize(out);
    }

    public long size() throws IOException {
      return mData.size();
    }

    @NotNull
    public byte[] toByteArray() throws IOException {
      return mData.toByteArray();
    }

    @NotNull
    public InputStream toInputStream() throws IOException {
      return mData.toInputStream();
    }

    void serialize(@NotNull final ObjectOutputStream out) throws IOException {
      mData.serialize(out);
    }
  }

  private static class UncachedInputStreamData extends SerializableData {

    private final InputStream mInput;

    private UncachedInputStreamData(@NotNull final InputStream input) {
      mInput = ConstantConditions.notNull("input", input);
    }

    Object writeReplace() throws ObjectStreamException {
      return new SerializableWrapper(this);
    }

    public long size() {
      return -1;
    }

    @NotNull
    public byte[] toByteArray() throws IOException {
      final byte[] buffer = new byte[DEFAULT_CHUNK_SIZE];
      final InputStream inputStream = mInput;
      try {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
          outputStream.write(buffer, 0, length);
        }
        outputStream.close();
        return outputStream.toByteArray();

      } finally {
        inputStream.close();
      }
    }

    @NotNull
    public InputStream toInputStream() {
      return mInput;
    }

    void serialize(@NotNull final ObjectOutputStream out) throws IOException {
      final byte[] buffer = new byte[DEFAULT_CHUNK_SIZE];
      final InputStream inputStream = mInput;
      try {
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
          out.writeInt(length);
          out.write(buffer, 0, length);
        }
        out.writeInt(0);
        out.close();

      } finally {
        inputStream.close();
      }
    }
  }
}
