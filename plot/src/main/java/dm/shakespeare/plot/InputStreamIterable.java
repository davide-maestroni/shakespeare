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

package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;

import dm.shakespeare.plot.function.NullaryFunction;
import dm.shakespeare.plot.memory.Memory;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 02/19/2019.
 */
class InputStreamIterable implements Iterable<ByteBuffer> {

  private final NullaryFunction<? extends ByteBuffer> bufferCreator;
  private final InputStream inputStream;
  private final Memory memory;

  private NullaryFunction<ByteBuffer> reader;

  InputStreamIterable(@NotNull final InputStream inputStream,
      @NotNull final NullaryFunction<? extends ByteBuffer> bufferCreator,
      @NotNull final Memory memory) {
    this.inputStream = ConstantConditions.notNull("inputStream", inputStream);
    this.bufferCreator = ConstantConditions.notNull("bufferCreator", bufferCreator);
    this.memory = ConstantConditions.notNull("memory", memory);
    if (inputStream instanceof FileInputStream) {
      reader = new NullaryFunction<ByteBuffer>() {

        public ByteBuffer call() throws Exception {
          final ByteBuffer byteBuffer = InputStreamIterable.this.bufferCreator.call();
          final int read = ((FileInputStream) InputStreamIterable.this.inputStream).getChannel()
              .read(byteBuffer);
          if (read > 0) {
            return byteBuffer;
          }
          reader = new NullaryFunction<ByteBuffer>() {

            public ByteBuffer call() {
              return null;
            }
          };
          return null;
        }
      };

    } else {
      reader = new NullaryFunction<ByteBuffer>() {

        public ByteBuffer call() throws Exception {
          final InputStream inputStream = InputStreamIterable.this.inputStream;
          final ByteBuffer byteBuffer = InputStreamIterable.this.bufferCreator.call();
          int read = 0;
          if (byteBuffer.hasArray()) {
            read = inputStream.read(byteBuffer.array());
            if (read > 0) {
              byteBuffer.position(read);
            }

          } else {
            while (byteBuffer.hasRemaining()) {
              byteBuffer.put((byte) inputStream.read());
              ++read;
            }
          }
          if (read > 0) {
            return byteBuffer;
          }
          reader = new NullaryFunction<ByteBuffer>() {

            public ByteBuffer call() {
              return null;
            }
          };
          return null;
        }
      };
    }
  }

  @NotNull
  public Iterator<ByteBuffer> iterator() {
    return new InputStreamIterator();
  }

  private class InputStreamIterator implements Iterator<ByteBuffer> {

    private final Iterator<Object> iterator;

    private InputStreamIterator() {
      iterator = memory.iterator();
    }

    public boolean hasNext() {
      if (!iterator.hasNext()) {
        try {
          final ByteBuffer byteBuffer = reader.call();
          if (byteBuffer != null) {
            memory.put(byteBuffer);
          }

        } catch (final Exception e) {
          throw new IllegalStateException(e);
        }
      }
      return iterator.hasNext();
    }

    public ByteBuffer next() {
      return (ByteBuffer) iterator.next();
    }

    public void remove() {
      throw new UnsupportedOperationException("remove");
    }
  }
}
