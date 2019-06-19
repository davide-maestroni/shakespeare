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
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 06/18/2019.
 */
public class PLZW {

  private static final String DEFAULT_ALPHABET =
      "!#$%&()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_abcdefghijklmnopqrstuvwxyz{|}~";
  private static final char[] DEFAULT_ENCODER_LUT = DEFAULT_ALPHABET.toCharArray();
  private static final HashMap<Character, Byte> DEFAULT_DECODER_LUT =
      new HashMap<Character, Byte>() {{
        for (int i = 0; i < DEFAULT_ENCODER_LUT.length; ++i) {
          put(DEFAULT_ENCODER_LUT[i], (byte) i);
        }
      }};
  private static final Decoder DEFAULT_DECODER = new Decoder(DEFAULT_DECODER_LUT);
  private static final Encoder DEFAULT_ENCODER = new Encoder(DEFAULT_ENCODER_LUT);

  @NotNull
  public static Decoder getDecoder() {
    return DEFAULT_DECODER;
  }

  @NotNull
  public static Decoder getDecoder(@NotNull final String alphabet) {
    final char[] chars = alphabet.toCharArray();
    if (chars.length < 16) {
      throw new IllegalArgumentException();
    }
    final HashMap<Character, Byte> lut = new HashMap<Character, Byte>();
    for (int i = 0; i < chars.length; ++i) {
      lut.put(chars[i], (byte) i);
    }
    return new Decoder(lut);
  }

  @NotNull
  public static Encoder getEncoder() {
    return DEFAULT_ENCODER;
  }

  @NotNull
  public static Encoder getEncoder(@NotNull final String alphabet) {
    final char[] chars = alphabet.toCharArray();
    if (chars.length < 16) {
      throw new IllegalArgumentException();
    }
    return new Encoder(chars);
  }

  @NotNull
  public static OutputStream getEncoder(@NotNull final Writer writer) {
    return new PLZWOutputStream(writer, DEFAULT_ENCODER_LUT);
  }

  @NotNull
  public static OutputStream getEncoder(@NotNull final Writer writer,
      @NotNull final String alphabet) {
    final char[] chars = alphabet.toCharArray();
    if (chars.length < 16) {
      throw new IllegalArgumentException();
    }
    return new PLZWOutputStream(writer, chars);
  }

  private static int decodeNext(final byte code, @NotNull final DecoderContext context,
      @NotNull final Map<Byte, Symbol> dictionary, @NotNull final byte[] bytes) {
    final Symbol last = context.last;
    Symbol symbol = dictionary.get(code);
    if (symbol == null) {
      int word = last.word + ((last.word & 0x0f) << last.length);
      symbol = new Symbol(word, last.length + 4);
      if (dictionary.size() < context.maxLength) {
        dictionary.put((byte) dictionary.size(), symbol);
      }

    } else if ((last != null) && (dictionary.size() < context.maxLength)) {
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

  private static byte encodeNext(final byte[] bytes, @NotNull final EncoderContext context,
      @NotNull final Map<Symbol, Byte> dictionary) {
    int word = 0;
    int shift = 0;
    Symbol symbol;
    boolean hasSymbol;
    byte code = -1;
    do {
      word += ((bytes[context.off] & context.mask) >> context.down) << shift;
      symbol = new Symbol(word, shift + 4);
      if (hasSymbol = dictionary.containsKey(symbol)) {
        code = dictionary.get(symbol);
        context.mask = (context.mask ^ 0xff);
        if (context.mask == 0xf0) {
          context.down = 4;
          ++context.off;

        } else {
          context.down = 0;
        }
        shift += 4;
      }

    } while (hasSymbol && (context.off < bytes.length) && (shift < Integer.SIZE));

    if (!hasSymbol && (dictionary.size() < context.maxLength)) {
      dictionary.put(symbol, (byte) dictionary.size());
    }
    return code;
  }

  public static class Decoder {

    private final Map<Character, Byte> lut;

    private Decoder(@NotNull final Map<Character, Byte> lut) {
      this.lut = lut;
    }

    public byte[] decode(@NotNull final String string) {
      final Map<Character, Byte> lut = this.lut;
      final DecoderContext context = new DecoderContext(lut.size());
      final HashMap<Byte, Symbol> dictionary = new HashMap<Byte, Symbol>();
      for (int i = 0; i < 16; ++i) {
        dictionary.put((byte) i, new Symbol(i, 4));
      }
      final byte[] buffer = new byte[(lut.size() - 15) / 2];
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      for (final char c : string.toCharArray()) {
        final int b = decodeNext(lut.get(c), context, dictionary, buffer);
        outputStream.write(buffer, 0, b);
      }
      //      final int maxLength = lut.size();
      //      byte maskShift = 4;
      //      byte next = 0;
      //      Symbol last = null;
      //      final char[] chars = string.toCharArray();
      //      for (final char aChar : chars) {
      //        Symbol symbol = dictionary.get(lut.get(aChar));
      //        if (symbol == null) {
      //          int word = last.word + ((last.word & 0x0f) << last.length);
      //          symbol = new Symbol(word, last.length + 4);
      //          if (dictionary.size() < maxLength) {
      //            dictionary.put((byte) dictionary.size(), symbol);
      //          }
      //
      //        } else if ((last != null) && (dictionary.size() < maxLength)) {
      //          int word = last.word + ((symbol.word & 0x0f) << last.length);
      //          dictionary.put((byte) dictionary.size(), new Symbol(word, last.length + 4));
      //        }
      //        int word = symbol.word;
      //        int shift = 0;
      //        while (shift < symbol.length) {
      //          next += (word & 0x0f) << maskShift;
      //          word >>= 4;
      //          shift += 4;
      //          if (maskShift == 0) {
      //            maskShift = 4;
      //            outputStream.write(next);
      //            next = 0;
      //
      //          } else {
      //            maskShift = 0;
      //          }
      //        }
      //        last = symbol;
      //      }
      return outputStream.toByteArray();
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
      final HashMap<Symbol, Byte> dictionary = new HashMap<Symbol, Byte>();
      for (int i = 0; i < 16; ++i) {
        dictionary.put(new Symbol(i, 4), (byte) i);
      }
      final EncoderContext context = new EncoderContext(lut.length);
      while (context.off < bytes.length) {
        final byte code = encodeNext(bytes, context, dictionary);
        writer.write(lut[code]);
      }

      System.out.println(bytes.length + " -> " + writer.toString().length() + " - " + (
          (float) writer.toString().length() / bytes.length));

      return writer.toString();

      //      int mask = 0xf0;
      //      int down = 4;
      //      for (int i = 0; i < bytes.length; ) {
      //        int word = 0;
      //        int shift = 0;
      //        Symbol symbol;
      //        boolean hasSymbol;
      //        byte code = -1;
      //        do {
      //          word += ((bytes[i] & mask) >> down) << shift;
      //          symbol = new Symbol(word, shift + 4);
      //          if (hasSymbol = dictionary.containsKey(symbol)) {
      //            code = dictionary.get(symbol);
      //            mask = (mask ^ 0xff);
      //            if (mask == 0xf0) {
      //              down = 4;
      //              ++i;
      //
      //            } else {
      //              down = 0;
      //            }
      //            shift += 4;
      //          }
      //
      //        } while (hasSymbol && (i < bytes.length) && (shift < Integer.SIZE));
      //
      //        if (!hasSymbol && (dictionary.size() < maxLength)) {
      //          dictionary.put(symbol, (byte) dictionary.size());
      //        }
      //        outputStream.write(lut[code]);
      //      }
      //
      //      return outputStream.toString();
    }
  }

  private static class DecoderContext {

    private final int maxLength;

    private Symbol last;
    private byte maskShift = 4;
    private byte next;

    private DecoderContext(final int maxLength) {
      this.maxLength = maxLength;
    }
  }

  private static class EncoderContext {

    private final int maxLength;

    private int down = 4;
    private int mask = 0xf0;
    private int off;

    private EncoderContext(final int maxLength) {
      this.maxLength = maxLength;
    }
  }

  private static class PLZWOutputStream extends OutputStream {

    private final EncoderContext context;
    private final HashMap<Symbol, Byte> dictionary;
    private final char[] lut;
    private final int minSize;
    private final Writer writer;

    private ByteArrayOutputStream buffer;

    private PLZWOutputStream(@NotNull final Writer writer, @NotNull final char[] lut) {
      this.writer = ConstantConditions.notNull("writer", writer);
      this.lut = lut;
      context = new EncoderContext(lut.length);
      buffer = new ByteArrayOutputStream();
      minSize = (lut.length - 15) / 2;
      final HashMap<Symbol, Byte> dictionary = new HashMap<Symbol, Byte>();
      for (int i = 0; i < 16; ++i) {
        dictionary.put(new Symbol(i, 4), (byte) i);
      }
      this.dictionary = dictionary;
    }

    @Override
    public void write(final int b) throws IOException {
      buffer.write(b);
      encode();
    }

    @Override
    public void write(@NotNull final byte[] b, final int off, final int len) throws IOException {
      buffer.write(b, off, len);
      encode();
    }

    @Override
    public void flush() throws IOException {
      writer.flush();
    }

    @Override
    public void close() throws IOException {
      final EncoderContext context = this.context;
      final HashMap<Symbol, Byte> dictionary = this.dictionary;
      final Writer writer = this.writer;
      final char[] lut = this.lut;
      final byte[] bytes = buffer.toByteArray();
      while (context.off < bytes.length) {
        final byte code = encodeNext(bytes, context, dictionary);
        writer.write(lut[code]);
      }
      writer.close();
    }

    private void encode() throws IOException {
      final int minSize = this.minSize;
      final ByteArrayOutputStream buffer = this.buffer;
      if (buffer.size() >= minSize) {
        final EncoderContext context = this.context;
        final HashMap<Symbol, Byte> dictionary = this.dictionary;
        final Writer writer = this.writer;
        final char[] lut = this.lut;
        final byte[] bytes = buffer.toByteArray();
        while (bytes.length - context.off >= minSize) {
          final byte code = encodeNext(bytes, context, dictionary);
          writer.write(lut[code]);
        }
        this.buffer = new ByteArrayOutputStream();
        this.buffer.write(bytes, context.off, bytes.length - context.off);
        context.off = 0;
      }
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
