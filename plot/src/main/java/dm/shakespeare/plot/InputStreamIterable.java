package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;

import dm.shakespeare.plot.Story.Memory;
import dm.shakespeare.plot.function.NullaryFunction;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 02/19/2019.
 */
class InputStreamIterable implements Iterable<ByteBuffer> {

  private final NullaryFunction<? extends ByteBuffer> mBufferCreator;
  private final InputStream mInputStream;
  private final Memory mMemory;

  private NullaryFunction<ByteBuffer> mReader;

  InputStreamIterable(@NotNull final InputStream inputStream,
      @NotNull final NullaryFunction<? extends ByteBuffer> bufferCreator,
      @NotNull final Memory memory) {
    mInputStream = ConstantConditions.notNull("inputStream", inputStream);
    mBufferCreator = ConstantConditions.notNull("bufferCreator", bufferCreator);
    mMemory = ConstantConditions.notNull("memory", memory);
    if (inputStream instanceof FileInputStream) {
      mReader = new NullaryFunction<ByteBuffer>() {

        public ByteBuffer call() throws Exception {
          final ByteBuffer byteBuffer = mBufferCreator.call();
          final int read = ((FileInputStream) mInputStream).getChannel().read(byteBuffer);
          if (read > 0) {
            return byteBuffer;
          }
          mReader = new NullaryFunction<ByteBuffer>() {

            public ByteBuffer call() {
              return null;
            }
          };
          return null;
        }
      };

    } else {
      mReader = new NullaryFunction<ByteBuffer>() {

        public ByteBuffer call() throws Exception {
          final InputStream inputStream = mInputStream;
          final ByteBuffer byteBuffer = mBufferCreator.call();
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
          mReader = new NullaryFunction<ByteBuffer>() {

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

    private final Iterator<Object> mIterator;

    private InputStreamIterator() {
      mIterator = mMemory.iterator();
    }

    public boolean hasNext() {
      if (!mIterator.hasNext()) {
        try {
          final ByteBuffer byteBuffer = mReader.call();
          if (byteBuffer != null) {
            mMemory.put(byteBuffer);
          }

        } catch (final Exception e) {
          throw new IllegalStateException(e);
        }
      }
      return mIterator.hasNext();
    }

    public ByteBuffer next() {
      return (ByteBuffer) mIterator.next();
    }

    public void remove() {
      throw new UnsupportedOperationException("remove");
    }
  }
}
