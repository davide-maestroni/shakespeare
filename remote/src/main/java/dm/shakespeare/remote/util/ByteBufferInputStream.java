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

  private final ByteBuffer mBuffer;
  private final int mPosition;

  private boolean mIsClosed;

  public ByteBufferInputStream(@NotNull final ByteBuffer buffer) {
    mPosition = buffer.position();
    mBuffer = buffer;
  }

  public int read() throws IOException {
    if (mIsClosed) {
      throw new IOException();
    }

    if (!mBuffer.hasRemaining()) {
      return -1;
    }
    return mBuffer.get() & 0xFF;
  }

  @Override
  public int read(@NotNull final byte[] bytes, final int offset, final int length) throws
      IOException {
    if (mIsClosed) {
      throw new IOException();
    }

    if (length == 0) {
      return 0;
    }
    final ByteBuffer buffer = mBuffer;
    final int count = Math.min(buffer.remaining(), length);
    if (count == 0) {
      return -1;
    }
    buffer.get(bytes, offset, count);
    return count;
  }

  @Override
  public long skip(final long n) throws IOException {
    if (mIsClosed) {
      throw new IOException();
    }

    if (n <= 0) {
      return 0;
    }
    final ByteBuffer buffer = mBuffer;
    final int skip = Math.min(buffer.remaining(), (int) n);
    buffer.position(buffer.position() + skip);
    return skip;
  }

  @Override
  public int available() throws IOException {
    if (mIsClosed) {
      throw new IOException();
    }
    return mBuffer.remaining();
  }

  @Override
  public void close() {
    if (!mIsClosed) {
      mIsClosed = true;
      mBuffer.position(mPosition);
    }
  }

  @Override
  public void mark(final int readlimit) {
    if (!mIsClosed) {
      mBuffer.mark();
    }
  }

  @Override
  public void reset() throws IOException {
    if (!mIsClosed) {
      try {
        mBuffer.reset();

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
