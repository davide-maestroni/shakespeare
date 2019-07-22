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

package dm.shakespeare.remote.io;

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

import dm.shakespeare.remote.config.BuildConfig;
import dm.shakespeare.remote.util.ByteBufferInputStream;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 04/13/2019.
 */
public abstract class RawData implements Serializable {

  private static final int DEFAULT_CHUNK_SIZE = 8192;
  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  @NotNull
  public static RawData wrap(@NotNull final byte[] bytes) {
    return new ByteArrayData(bytes);
  }

  @NotNull
  public static RawData wrap(@NotNull final ByteBuffer buffer) {
    return new ByteBufferData(buffer);
  }

  @NotNull
  public static RawData wrap(@NotNull final File file) {
    return new FileData(file);
  }

  @NotNull
  public static RawData wrap(@NotNull final InputStream inputStream) {
    return new InputStreamData(inputStream);
  }

  @NotNull
  public static RawData wrapOnce(@NotNull final InputStream inputStream) {
    return new UncachedInputStreamData(inputStream);
  }

  public void copyTo(@NotNull final File file) throws IOException {
    final FileOutputStream outputStream = new FileOutputStream(file);
    try {
      copyTo(outputStream);

    } finally {
      outputStream.close();
    }
  }

  public abstract void copyTo(@NotNull OutputStream out) throws IOException;

  public abstract void copyTo(@NotNull ByteBuffer buffer) throws IOException;

  public abstract long size() throws IOException;

  @NotNull
  public abstract byte[] toByteArray() throws IOException;

  @NotNull
  public abstract InputStream toInputStream() throws IOException;

  abstract void serialize(@NotNull ObjectOutputStream out) throws IOException;

  private static class ByteArrayData extends RawData {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final byte[] data;

    private ByteArrayData(@NotNull final byte[] data) {
      this.data = ConstantConditions.notNull("data", data);
    }

    public void copyTo(@NotNull final OutputStream out) throws IOException {
      out.write(data);
    }

    public void copyTo(@NotNull final ByteBuffer buffer) {
      buffer.put(data);
    }

    public long size() {
      return data.length;
    }

    @NotNull
    public byte[] toByteArray() {
      return data;
    }

    @NotNull
    public InputStream toInputStream() {
      return new ByteArrayInputStream(data);
    }

    void serialize(@NotNull final ObjectOutputStream out) throws IOException {
      final byte[] data = this.data;
      if (data.length <= DEFAULT_CHUNK_SIZE) {
        out.writeInt(data.length);
        out.write(data);
        out.writeInt(0);

      } else {
        int offset = 0;
        int length;
        while ((length = Math.min(data.length - offset, DEFAULT_CHUNK_SIZE)) > 0) {
          out.writeInt(length);
          out.write(data, offset, length);
          offset += length;
        }
        out.writeInt(0);
      }
    }

    private Object writeReplace() throws ObjectStreamException {
      return new RawDataWrapper(this);
    }
  }

  private static class ByteBufferData extends RawData {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final ByteBuffer buffer;

    private ByteBufferData(@NotNull final ByteBuffer buffer) {
      this.buffer = ConstantConditions.notNull("buffer", buffer);
    }

    @Override
    public void copyTo(@NotNull final File file) throws IOException {
      final FileOutputStream outputStream = new FileOutputStream(file);
      try {
        outputStream.getChannel().write(buffer);

      } finally {
        outputStream.close();
      }
    }

    public void copyTo(@NotNull final OutputStream out) throws IOException {
      if (out instanceof FileOutputStream) {
        ((FileOutputStream) out).getChannel().write(buffer);

      } else {
        out.write(toByteArray());
      }
    }

    public void copyTo(@NotNull final ByteBuffer buffer) {
      buffer.put(this.buffer);
    }

    public long size() {
      return buffer.remaining();
    }

    @NotNull
    public byte[] toByteArray() {
      final ByteBuffer buffer = this.buffer;
      final byte[] data = new byte[buffer.remaining()];
      buffer.slice().get(data);
      return data;
    }

    @NotNull
    public InputStream toInputStream() {
      return new ByteBufferInputStream(buffer);
    }

    void serialize(@NotNull final ObjectOutputStream out) throws IOException {
      RawData.wrapOnce(new ByteBufferInputStream(buffer)).serialize(out);
    }

    private Object writeReplace() throws ObjectStreamException {
      return new RawDataWrapper(this);
    }
  }

  private static class FileData extends RawData {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final File file;

    private byte[] data;

    private FileData(@NotNull final File file) {
      if (!file.isFile()) {
        throw new IllegalArgumentException();
      }
      this.file = file;
    }

    @Override
    public void copyTo(@NotNull final File file) throws IOException {
      if (!this.file.equals(file)) {
        FileInputStream inputStream = null;
        final FileOutputStream outputStream = new FileOutputStream(file);
        try {
          inputStream = new FileInputStream(this.file);
          inputStream.getChannel().transferTo(0, this.file.length(), outputStream.getChannel());

        } finally {
          if (inputStream != null) {
            try {
              inputStream.close();

            } catch (final IOException e) {
              // TODO: 18/04/2019 ???
            }
          }

          try {
            outputStream.close();

          } catch (final IOException e) {
            // TODO: 18/04/2019 ???
          }
        }
      }
    }

    public void copyTo(@NotNull final OutputStream out) throws IOException {
      if (out instanceof FileOutputStream) {
        final File file = this.file;
        final FileInputStream inputStream = new FileInputStream(file);
        try {
          inputStream.getChannel()
              .transferTo(0, file.length(), ((FileOutputStream) out).getChannel());

        } finally {
          try {
            inputStream.close();
          } catch (final IOException e) {
            // TODO: 18/04/2019 ???
          }
        }

      } else {
        final byte[] data = this.data;
        if (data != null) {
          out.write(data);

        } else {
          final byte[] chunk = new byte[DEFAULT_CHUNK_SIZE];
          final FileInputStream inputStream = new FileInputStream(file);
          try {
            int length;
            while ((length = inputStream.read(chunk)) > 0) {
              out.write(chunk, 0, length);
            }

          } finally {
            try {
              inputStream.close();

            } catch (final IOException e) {
              // TODO: 18/04/2019 ???
            }
          }
        }
      }
    }

    public void copyTo(@NotNull final ByteBuffer buffer) throws IOException {
      final byte[] data = this.data;
      if (data != null) {
        buffer.put(data);

      } else {
        final File file = this.file;
        final FileInputStream inputStream = new FileInputStream(file);
        try {
          buffer.put(inputStream.getChannel().map(MapMode.READ_ONLY, 0, file.length()));

        } finally {
          try {
            inputStream.close();

          } catch (final IOException e) {
            // TODO: 18/04/2019 ???
          }
        }
      }
    }

    public long size() {
      return file.length();
    }

    @NotNull
    public byte[] toByteArray() throws IOException {
      if (data == null) {
        final File file = this.file;
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
          this.data = data;

        } finally {
          try {
            inputStream.close();

          } catch (final IOException e) {
            // TODO: 18/04/2019 ???
          }
        }
      }
      return data;
    }

    @NotNull
    public InputStream toInputStream() throws IOException {
      if (data == null) {
        return new FileInputStream(file);
      }
      return new ByteArrayInputStream(data);
    }

    void serialize(@NotNull final ObjectOutputStream out) throws IOException {
      final byte[] data = this.data;
      if (data != null) {
        RawData.wrap(data).serialize(out);

      } else {
        RawData.wrapOnce(new FileInputStream(file)).serialize(out);
      }
    }

    private Object writeReplace() throws ObjectStreamException {
      return new RawDataWrapper(this);
    }
  }

  private static class InputStreamData extends RawData {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final InputStream input;

    private byte[] data;

    private InputStreamData(@NotNull final InputStream input) {
      this.input = ConstantConditions.notNull("input", input);
    }

    public void copyTo(@NotNull final OutputStream out) throws IOException {
      final byte[] data = this.data;
      if (data != null) {
        out.write(data);

      } else {
        final byte[] chunk = new byte[DEFAULT_CHUNK_SIZE];
        @SuppressWarnings("UnnecessaryLocalVariable") final InputStream inputStream = input;
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int length;
        while ((length = inputStream.read(chunk)) > 0) {
          out.write(chunk, 0, length);
          outputStream.write(chunk, 0, length);
        }
        outputStream.close();
        this.data = outputStream.toByteArray();
      }
    }

    public void copyTo(@NotNull final ByteBuffer buffer) throws IOException {
      final byte[] data = this.data;
      if (data != null) {
        buffer.put(data);

      } else {
        final byte[] chunk = new byte[DEFAULT_CHUNK_SIZE];
        @SuppressWarnings("UnnecessaryLocalVariable") final InputStream inputStream = input;
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int length;
        while ((length = inputStream.read(chunk)) > 0) {
          buffer.put(chunk, 0, length);
          outputStream.write(chunk, 0, length);
        }
        outputStream.close();
        this.data = outputStream.toByteArray();
      }
    }

    public long size() {
      final byte[] data = this.data;
      return (data != null) ? data.length : -1;
    }

    @NotNull
    public byte[] toByteArray() throws IOException {
      if (data == null) {
        final byte[] chunk = new byte[DEFAULT_CHUNK_SIZE];
        @SuppressWarnings("UnnecessaryLocalVariable") final InputStream inputStream = input;
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int length;
        while ((length = inputStream.read(chunk)) > 0) {
          outputStream.write(chunk, 0, length);
        }
        outputStream.close();
        data = outputStream.toByteArray();
      }
      return data;
    }

    @NotNull
    public InputStream toInputStream() throws IOException {
      return new ByteArrayInputStream(toByteArray());
    }

    void serialize(@NotNull final ObjectOutputStream out) throws IOException {
      final byte[] data = this.data;
      if (data != null) {
        RawData.wrap(data).serialize(out);

      } else {
        final byte[] chunk = new byte[DEFAULT_CHUNK_SIZE];
        @SuppressWarnings("UnnecessaryLocalVariable") final InputStream inputStream = input;
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int length;
        while ((length = inputStream.read(chunk)) > 0) {
          out.writeInt(length);
          out.write(chunk, 0, length);
          outputStream.write(chunk, 0, length);
        }
        out.writeInt(0);
        out.close();
        outputStream.close();
        this.data = outputStream.toByteArray();
      }
    }

    private Object writeReplace() throws ObjectStreamException {
      return new RawDataWrapper(this);
    }
  }

  private static class RawDataWrapper extends RawData {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private transient RawData data;

    private RawDataWrapper(@NotNull final RawData data) {
      this.data = data;
    }

    @Override
    public void copyTo(@NotNull final File file) throws IOException {
      data.copyTo(file);
    }

    public void copyTo(@NotNull final OutputStream out) throws IOException {
      data.copyTo(out);
    }

    public void copyTo(@NotNull final ByteBuffer buffer) throws IOException {
      data.copyTo(buffer);
    }

    public long size() throws IOException {
      return data.size();
    }

    @NotNull
    public byte[] toByteArray() throws IOException {
      return data.toByteArray();
    }

    @NotNull
    public InputStream toInputStream() throws IOException {
      return data.toInputStream();
    }

    void serialize(@NotNull final ObjectOutputStream out) throws IOException {
      data.serialize(out);
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
      in.defaultReadObject();
      File file = null;
      OutputStream outputStream = new ByteArrayOutputStream();
      try {
        byte[] chunk = new byte[DEFAULT_CHUNK_SIZE];
        long total = 0;
        int length;
        while ((length = in.readInt()) > 0) {
          // sanity check
          if (length > DEFAULT_CHUNK_SIZE) {
            throw new IOException();
          }
          in.readFully(chunk, 0, length);
          total += length;
          if ((outputStream instanceof ByteArrayOutputStream) && (total > DEFAULT_CHUNK_SIZE)) {
            file = File.createTempFile("sks-", ".data");
            file.deleteOnExit();
            final FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(((ByteArrayOutputStream) outputStream).toByteArray());
            outputStream = fileOutputStream;
          }
          outputStream.write(chunk, 0, length);
        }

      } finally {
        outputStream.close();
      }

      if (outputStream instanceof ByteArrayOutputStream) {
        data = new ByteArrayData(((ByteArrayOutputStream) outputStream).toByteArray());

      } else {
        data = new FileData(file);
      }
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();
      serialize(out);
    }
  }

  private static class UncachedInputStreamData extends RawData {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final InputStream input;

    private UncachedInputStreamData(@NotNull final InputStream input) {
      this.input = ConstantConditions.notNull("input", input);
    }

    public void copyTo(@NotNull final OutputStream out) throws IOException {
      final byte[] chunk = new byte[DEFAULT_CHUNK_SIZE];
      @SuppressWarnings("UnnecessaryLocalVariable") final InputStream inputStream = input;
      int length;
      while ((length = inputStream.read(chunk)) > 0) {
        out.write(chunk, 0, length);
      }
    }

    public void copyTo(@NotNull final ByteBuffer buffer) throws IOException {
      final byte[] chunk = new byte[DEFAULT_CHUNK_SIZE];
      @SuppressWarnings("UnnecessaryLocalVariable") final InputStream inputStream = input;
      int length;
      while ((length = inputStream.read(chunk)) > 0) {
        buffer.put(chunk, 0, length);
      }
    }

    public long size() {
      return -1;
    }

    @NotNull
    public byte[] toByteArray() throws IOException {
      final byte[] chunk = new byte[DEFAULT_CHUNK_SIZE];
      @SuppressWarnings("UnnecessaryLocalVariable") final InputStream inputStream = input;
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      int length;
      while ((length = inputStream.read(chunk)) > 0) {
        outputStream.write(chunk, 0, length);
      }
      outputStream.close();
      return outputStream.toByteArray();
    }

    @NotNull
    public InputStream toInputStream() {
      return input;
    }

    void serialize(@NotNull final ObjectOutputStream out) throws IOException {
      final byte[] chunk = new byte[DEFAULT_CHUNK_SIZE];
      @SuppressWarnings("UnnecessaryLocalVariable") final InputStream inputStream = input;
      int length;
      while ((length = inputStream.read(chunk)) > 0) {
        out.writeInt(length);
        out.write(chunk, 0, length);
      }
      out.writeInt(0);
      out.close();
    }

    private Object writeReplace() throws ObjectStreamException {
      return new RawDataWrapper(this);
    }
  }
}
