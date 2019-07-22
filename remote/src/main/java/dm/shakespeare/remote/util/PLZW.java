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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.WeakValueHashMap;

/**
 * Created by davide-maestroni on 06/18/2019.
 */
public class PLZW {

  private static final String DEFAULT_ALPHABET = buildAsciiAlphabet();
  private static final char[] DEFAULT_ENCODER_LUT = DEFAULT_ALPHABET.toCharArray();
  private static final HashMap<Character, Byte> DEFAULT_DECODER_LUT =
      new HashMap<Character, Byte>() {{
        for (int i = 0; i < DEFAULT_ENCODER_LUT.length; ++i) {
          put(DEFAULT_ENCODER_LUT[i], (byte) i);
        }
      }};
  private static final Decoder DEFAULT_DECODER = new Decoder(DEFAULT_DECODER_LUT);
  private static final Encoder DEFAULT_ENCODER = new Encoder(DEFAULT_ENCODER_LUT);

  private static final Map<CharSequence, Map<Character, Byte>> decoderLuts =
      Collections.synchronizedMap(new WeakValueHashMap<CharSequence, Map<Character, Byte>>());
  private static final Map<CharSequence, Decoder> decoders =
      Collections.synchronizedMap(new WeakValueHashMap<CharSequence, Decoder>());
  private static final Map<CharSequence, char[]> encoderLuts =
      Collections.synchronizedMap(new WeakValueHashMap<CharSequence, char[]>());
  private static final Map<CharSequence, Encoder> encoders =
      Collections.synchronizedMap(new WeakValueHashMap<CharSequence, Encoder>());

  @NotNull
  public static Decoder getDecoder() {
    return DEFAULT_DECODER;
  }

  @NotNull
  public static Decoder getDecoder(@NotNull final CharSequence alphabet) {
    Decoder decoder = decoders.get(alphabet);
    if (decoder == null) {
      decoder = new Decoder(buildDecoderLut(alphabet));
      decoders.put(alphabet, decoder);
    }
    return decoder;
  }

  @NotNull
  public static Encoder getEncoder() {
    return DEFAULT_ENCODER;
  }

  @NotNull
  public static Encoder getEncoder(@NotNull final CharSequence alphabet) {
    Encoder encoder = encoders.get(alphabet);
    if (encoder == null) {
      encoder = new Encoder(buildEncoderLut(alphabet));
      encoders.put(alphabet, encoder);
    }
    return encoder;
  }

  @NotNull
  public static InputStream newDecoder(@NotNull final Reader reader) {
    return new DecoderInputStream(reader, DEFAULT_DECODER_LUT);
  }

  @NotNull
  public static InputStream newDecoder(@NotNull final Reader reader,
      @NotNull final CharSequence alphabet) {
    return new DecoderInputStream(reader, buildDecoderLut(alphabet));
  }

  @NotNull
  public static Writer newDecoder(@NotNull final OutputStream stream) {
    return new DecoderWriter(stream, DEFAULT_DECODER_LUT);
  }

  @NotNull
  public static Writer newDecoder(@NotNull final OutputStream stream,
      @NotNull final CharSequence alphabet) {
    return new DecoderWriter(stream, buildDecoderLut(alphabet));
  }

  @NotNull
  public static OutputStream newEncoder(@NotNull final Writer writer) {
    return new EncoderOutputStream(writer, DEFAULT_ENCODER_LUT);
  }

  @NotNull
  public static OutputStream newEncoder(@NotNull final Writer writer,
      @NotNull final CharSequence alphabet) {
    return new EncoderOutputStream(writer, buildEncoderLut(alphabet));
  }

  @NotNull
  public static Reader newEncoder(@NotNull final InputStream stream) {
    return new EncoderReader(stream, DEFAULT_ENCODER_LUT);
  }

  @NotNull
  public static Reader newEncoder(@NotNull final InputStream stream,
      @NotNull final CharSequence alphabet) {
    return new EncoderReader(stream, buildEncoderLut(alphabet));
  }

  @NotNull
  private static String buildAsciiAlphabet() {
    char[] chars = new char[127 - 32];
    for (int i = 0; i < chars.length; i++) {
      chars[i] = (char) (32 + i);
    }
    return new String(chars);
  }

  @NotNull
  private static Map<Byte, Symbol> buildDecoderDictionary() {
    final HashMap<Byte, Symbol> dictionary = new HashMap<Byte, Symbol>();
    for (int i = 0; i < 16; ++i) {
      dictionary.put((byte) i, new Symbol(i, 4));
    }
    return dictionary;
  }

  @NotNull
  private static Map<Character, Byte> buildDecoderLut(@NotNull final CharSequence alphabet) {
    Map<Character, Byte> lut = decoderLuts.get(alphabet);
    if (lut == null) {
      final int length = alphabet.length();
      if (length < 16) {
        throw new IllegalArgumentException("alphabet too large: " + length);
      }
      lut = new HashMap<Character, Byte>();
      for (int i = 0; i < length; ++i) {
        lut.put(alphabet.charAt(i), (byte) i);
      }
      decoderLuts.put(alphabet, lut);
    }
    return lut;
  }

  @NotNull
  private static Map<Symbol, Byte> buildEncoderDictionary() {
    final HashMap<Symbol, Byte> dictionary = new HashMap<Symbol, Byte>();
    for (int i = 0; i < 16; ++i) {
      dictionary.put(new Symbol(i, 4), (byte) i);
    }
    return dictionary;
  }

  @NotNull
  private static char[] buildEncoderLut(@NotNull final CharSequence alphabet) {
    char[] lut = encoderLuts.get(alphabet);
    if (lut == null) {
      final int length = alphabet.length();
      if (length < 16) {
        throw new IllegalArgumentException("alphabet too large: " + length);
      }
      lut = new char[length];
      for (int i = 0; i < length; ++i) {
        lut[i] = alphabet.charAt(i);
      }
      encoderLuts.put(alphabet, lut);
    }
    return lut;
  }

  private static int decodeNext(final byte code, @NotNull final DecoderContext context,
      @NotNull final Map<Byte, Symbol> dictionary, @NotNull final byte[] bytes) {
    final Symbol last = context.last;
    Symbol symbol = dictionary.get(code);
    if (symbol == null) {
      int word = last.word + ((last.word & 0x0f) << last.length);
      symbol = new Symbol(word, last.length + 4);
      if (dictionary.size() < context.maxSymbols) {
        dictionary.put((byte) dictionary.size(), symbol);
      }

    } else if ((last != null) && (dictionary.size() < context.maxSymbols)) {
      int word = last.word + ((symbol.word & 0x0f) << last.length);
      dictionary.put((byte) dictionary.size(), new Symbol(word, last.length + 4));
    }
    int written = 0;
    int word = symbol.word;
    int shift = 0;
    while (shift < symbol.length) {
      context.next += (word & 0x0f) << context.maskShift;
      word >>= 4;
      shift += 4;
      if (context.maskShift == 0) {
        context.maskShift = 4;
        bytes[written] = context.next;
        context.next = 0;
        ++written;

      } else {
        context.maskShift = 0;
      }
    }
    context.last = symbol;
    return written;
  }

  private static int encodeNext(final byte[] bytes, @NotNull final EncoderContext context,
      @NotNull final Map<Symbol, Byte> dictionary) {
    int word = 0;
    int shift = 0;
    Symbol symbol;
    boolean hasSymbol;
    int code = -1;
    do {
      word += ((bytes[context.off] & context.mask) >> context.maskShift) << shift;
      symbol = new Symbol(word, shift + 4);
      if (hasSymbol = dictionary.containsKey(symbol)) {
        code = dictionary.get(symbol);
        context.mask = (context.mask ^ 0xff);
        if (context.mask == 0xf0) {
          context.maskShift = 4;
          ++context.off;

        } else {
          context.maskShift = 0;
        }
        shift += 4;
      }

    } while (hasSymbol && (context.off < bytes.length) && (shift < Integer.SIZE));

    if (!hasSymbol && (dictionary.size() < context.maxSymbols)) {
      dictionary.put(symbol, (byte) dictionary.size());
    }
    return code;
  }

  private static boolean outOfBound(final int off, final int len, final int bytes) {
    return (off < 0) || (len < 0) || (len > bytes - off) || ((off + len) < 0);
  }

  public static class Decoder {

    private final Map<Character, Byte> lut;

    private Decoder(@NotNull final Map<Character, Byte> lut) {
      this.lut = lut;
    }

    public byte[] decode(@NotNull final String string) {
      final Map<Character, Byte> lut = this.lut;
      final DecoderContext context = new DecoderContext(lut.size());
      final Map<Byte, Symbol> dictionary = buildDecoderDictionary();
      final byte[] buffer = new byte[(lut.size() - 15) / 2];
      final ByteArrayOutputStream stream = new ByteArrayOutputStream();
      for (final char c : string.toCharArray()) {
        final int count = decodeNext(lut.get(c), context, dictionary, buffer);
        stream.write(buffer, 0, count);
        if (count < buffer.length) {
          buffer[0] = buffer[count];
        }
      }
      return stream.toByteArray();
    }
  }

  public static class Encoder {

    private final char[] lut;

    private Encoder(@NotNull final char[] lut) {
      this.lut = lut;
    }

    @NotNull
    public String encode(@NotNull final byte[] bytes) {
      final char[] lut = this.lut;
      final StringWriter writer = new StringWriter();
      final EncoderContext context = new EncoderContext(lut.length);
      final Map<Symbol, Byte> dictionary = buildEncoderDictionary();
      while (context.off < bytes.length) {
        final int code = encodeNext(bytes, context, dictionary);
        writer.write(lut[code]);
      }
      return writer.toString();
    }
  }

  private static class DecoderContext {

    private final int maxSymbols;

    private Symbol last;
    private byte maskShift = 4;
    private byte next;

    private DecoderContext(final int maxSymbols) {
      this.maxSymbols = maxSymbols;
    }
  }

  private static class DecoderInputStream extends InputStream {

    private final byte[] buffer;
    private final DecoderContext context;
    private final Map<Byte, Symbol> dictionary;
    private final Map<Character, Byte> lut;
    private final Reader reader;

    private int count;

    private DecoderInputStream(@NotNull final Reader reader,
        @NotNull final Map<Character, Byte> lut) {
      this.reader = ConstantConditions.notNull("reader", reader);
      this.lut = lut;
      context = new DecoderContext(lut.size());
      dictionary = buildDecoderDictionary();
      buffer = new byte[(lut.size() - 15) / 2];
    }

    public int read() throws IOException {
      final Map<Character, Byte> lut = this.lut;
      final DecoderContext context = this.context;
      final Map<Byte, Symbol> dictionary = this.dictionary;
      final byte[] buffer = this.buffer;
      final Reader reader = this.reader;
      int c;
      while ((count == 0) && ((c = reader.read()) != -1)) {
        count += decodeNext(lut.get((char) c), context, dictionary, buffer);
      }
      if (count == 0) {
        return -1;
      }
      final int out = buffer[0];
      System.arraycopy(buffer, 1, buffer, 0, buffer.length - 1);
      --count;
      return out & 0xff;
    }

    @Override
    public int read(@NotNull final byte[] b, final int off, final int len) throws IOException {
      if (outOfBound(off, len, b.length)) {
        throw new IndexOutOfBoundsException();

      } else if (len == 0) {
        return 0;
      }
      final Map<Character, Byte> lut = this.lut;
      final DecoderContext context = this.context;
      final Map<Byte, Symbol> dictionary = this.dictionary;
      final byte[] buffer = this.buffer;
      final Reader reader = this.reader;
      int written = 0;
      int c;
      while (written < len) {
        while ((count == 0) && ((c = reader.read()) != -1)) {
          count += decodeNext(lut.get((char) c), context, dictionary, buffer);
        }
        if (count == 0) {
          return (written > 0) ? written : -1;
        }
        final int toWrite = Math.min(len - written, count);
        System.arraycopy(buffer, 0, b, off + written, toWrite);
        written += toWrite;
        System.arraycopy(buffer, toWrite, buffer, 0, buffer.length - toWrite);
        count -= toWrite;
      }
      return written;
    }

    public void close() throws IOException {
      reader.close();
    }
  }

  private static class DecoderWriter extends Writer {

    private final byte[] buffer;
    private final DecoderContext context;
    private final Map<Byte, Symbol> dictionary;
    private final Map<Character, Byte> lut;
    private final OutputStream stream;

    private DecoderWriter(@NotNull final OutputStream stream,
        @NotNull final Map<Character, Byte> lut) {
      this.stream = ConstantConditions.notNull("stream", stream);
      this.lut = lut;
      context = new DecoderContext(lut.size());
      dictionary = buildDecoderDictionary();
      buffer = new byte[(lut.size() - 15) / 2];
    }

    public void write(@NotNull final char[] cbuf, final int off, final int len) throws IOException {
      if (outOfBound(off, len, cbuf.length)) {
        throw new IndexOutOfBoundsException();

      } else if (len == 0) {
        return;
      }
      final Map<Character, Byte> lut = this.lut;
      final DecoderContext context = this.context;
      final Map<Byte, Symbol> dictionary = this.dictionary;
      final byte[] buffer = this.buffer;
      final OutputStream stream = this.stream;
      for (int i = off; i < len; ++i) {
        final int count = decodeNext(lut.get(cbuf[i]), context, dictionary, buffer);
        stream.write(buffer, 0, count);
        if (count < buffer.length) {
          buffer[0] = buffer[count];
        }
      }
    }

    public void flush() throws IOException {
      stream.flush();
    }

    public void close() throws IOException {
      stream.close();
    }
  }

  private static class EncoderContext {

    private final int maxSymbols;

    private int mask = 0xf0;
    private int maskShift = 4;
    private int off;

    private EncoderContext(final int maxSymbols) {
      this.maxSymbols = maxSymbols;
    }
  }

  private static class EncoderOutputStream extends OutputStream {

    private final EncoderContext context;
    private final Map<Symbol, Byte> dictionary;
    private final char[] lut;
    private final int maxSize;
    private final int minSize;
    private final Writer writer;

    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    private EncoderOutputStream(@NotNull final Writer writer, @NotNull final char[] lut) {
      this.writer = ConstantConditions.notNull("writer", writer);
      this.lut = lut;
      context = new EncoderContext(lut.length);
      dictionary = buildEncoderDictionary();
      minSize = (lut.length - 15) / 2;
      maxSize = Math.max(minSize, 8192);
    }

    @Override
    public void write(final int b) throws IOException {
      final ByteArrayOutputStream buffer = this.buffer;
      buffer.write(b);
      encodeBuffer();
    }

    @Override
    public void write(@NotNull final byte[] b, final int off, final int len) throws IOException {
      if (outOfBound(off, len, b.length)) {
        throw new IndexOutOfBoundsException();

      } else if (len == 0) {
        return;
      }
      final ByteArrayOutputStream buffer = this.buffer;
      if ((buffer.size() == 0) && (len >= maxSize)) {
        final EncoderContext context = this.context;
        context.off = off;
        encode(b, off + len - minSize);
        context.off = 0;

      } else {
        buffer.write(b, off, len);
        encodeBuffer();
      }
    }

    @Override
    public void flush() throws IOException {
      final EncoderContext context = this.context;
      final byte[] bytes = buffer.toByteArray();
      encode(bytes, bytes.length);
      buffer = new ByteArrayOutputStream();
      buffer.write(bytes, context.off, bytes.length - context.off);
      context.off = 0;
      writer.flush();
    }

    @Override
    public void close() throws IOException {
      final byte[] bytes = buffer.toByteArray();
      encode(bytes, bytes.length);
      writer.close();
    }

    private void encode(@NotNull final byte[] bytes, final int maxOffset) throws IOException {
      final char[] lut = this.lut;
      final EncoderContext context = this.context;
      final Map<Symbol, Byte> dictionary = this.dictionary;
      final Writer writer = this.writer;
      while (context.off < maxOffset) {
        final int code = encodeNext(bytes, context, dictionary);
        writer.write(lut[code]);
      }
    }

    private void encodeBuffer() throws IOException {
      if (buffer.size() > maxSize) {
        final EncoderContext context = this.context;
        final byte[] bytes = buffer.toByteArray();
        encode(bytes, bytes.length - minSize);
        this.buffer = new ByteArrayOutputStream();
        this.buffer.write(bytes, context.off, bytes.length - context.off);
        context.off = 0;
      }
    }
  }

  private static class EncoderReader extends Reader {

    private final EncoderContext context;
    private final Map<Symbol, Byte> dictionary;
    private final char[] lut;
    private final int maxSize;
    private final int minSize;
    private final InputStream stream;

    private byte[] buffer;
    private boolean ended;
    private int length;

    private EncoderReader(@NotNull InputStream stream, @NotNull final char[] lut) {
      this.stream = ConstantConditions.notNull("stream", stream);
      this.lut = lut;
      context = new EncoderContext(lut.length);
      dictionary = buildEncoderDictionary();
      minSize = (lut.length - 15) / 2;
      maxSize = Math.max(minSize, 8192);
      buffer = new byte[maxSize];
    }

    public int read(@NotNull final char[] cbuf, final int off, final int len) throws IOException {
      if (outOfBound(off, len, cbuf.length)) {
        throw new IndexOutOfBoundsException();

      } else if (len == 0) {
        return 0;
      }
      final char[] lut = this.lut;
      final EncoderContext context = this.context;
      final Map<Symbol, Byte> dictionary = this.dictionary;
      final InputStream stream = this.stream;
      final byte[] buffer = this.buffer;
      final int minSize = this.minSize;
      int read = 0;
      while (read < len) {
        if ((ended && (context.off < length)) || (length - context.off > minSize)) {
          final int code = encodeNext(buffer, context, dictionary);
          cbuf[off + read] = lut[code];
          ++read;

        } else {
          System.arraycopy(buffer, context.off, buffer, 0, length - context.off);
          length -= context.off;
          context.off = 0;
          int count = 0;
          while ((length < buffer.length) && (
              (count = stream.read(buffer, length, buffer.length - length)) > 0)) {
            length += count;
          }
          ended = (count == -1);
        }
      }
      return read;
    }

    public void close() throws IOException {
      stream.close();
    }
  }

  private static class Symbol {

    private final int length;
    private final int word;

    private Symbol(final int word, final int length) {
      this.word = word;
      this.length = length;
    }

    @Override
    public int hashCode() {
      int result = length;
      result = 31 * result + word;
      return result;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }

      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      final Symbol symbol = (Symbol) o;
      if (length != symbol.length) {
        return false;
      }
      return this.word == symbol.word;
    }
  }
}
