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
import java.nio.ByteBuffer;
import java.nio.InvalidMarkException;

/**
 * Created by davide-maestroni on 04/13/2019.
 */
public class ByteBufferInputStream extends InputStream {

  private final ByteBuffer buffer;
  private final int position;

  private boolean isClosed;

  public ByteBufferInputStream(@NotNull final ByteBuffer buffer) {
    position = buffer.position();
    this.buffer = buffer;
  }

  public int read() throws IOException {
    if (isClosed) {
      throw new IOException();
    }

    if (!buffer.hasRemaining()) {
      return -1;
    }
    return buffer.get() & 0xFF;
  }

  @Override
  public int read(@NotNull final byte[] bytes, final int offset, final int length) throws
      IOException {
    if (isClosed) {
      throw new IOException();
    }

    if (length == 0) {
      return 0;
    }
    final ByteBuffer buffer = this.buffer;
    final int count = Math.min(buffer.remaining(), length);
    if (count == 0) {
      return -1;
    }
    buffer.get(bytes, offset, count);
    return count;
  }

  @Override
  public long skip(final long n) throws IOException {
    if (isClosed) {
      throw new IOException();
    }

    if (n <= 0) {
      return 0;
    }
    final ByteBuffer buffer = this.buffer;
    final int skip = Math.min(buffer.remaining(), (int) n);
    buffer.position(buffer.position() + skip);
    return skip;
  }

  @Override
  public int available() throws IOException {
    if (isClosed) {
      throw new IOException();
    }
    return buffer.remaining();
  }

  @Override
  public void close() {
    if (!isClosed) {
      isClosed = true;
      buffer.position(position);
    }
  }

  @Override
  public void mark(final int readlimit) {
    if (!isClosed) {
      buffer.mark();
    }
  }

  @Override
  public void reset() throws IOException {
    if (!isClosed) {
      try {
        buffer.reset();

      } catch (final InvalidMarkException e) {
        throw new IOException(e);
      }
    }
  }

  @Override
  public boolean markSupported() {
    return true;
  }
}
