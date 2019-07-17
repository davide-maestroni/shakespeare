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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 04/19/2019.
 */
public class Classes {

  private static final char ARRAY_START = '[';
  private static final byte CONSTANT_CLASS = 7;
  private static final byte CONSTANT_DOUBLE = 6;
  private static final byte CONSTANT_FIELDREF = 9;
  private static final byte CONSTANT_FLOAT = 4;
  private static final byte CONSTANT_INTEGER = 3;
  private static final byte CONSTANT_INTERFACEMETHODREF = 11;
  private static final byte CONSTANT_INVOKEDYNAMIC = 18;
  private static final byte CONSTANT_LONG = 5;
  private static final byte CONSTANT_METHODHANDLE = 15;
  private static final byte CONSTANT_METHODREF = 10;
  private static final byte CONSTANT_METHODTYPE = 16;
  private static final byte CONSTANT_NAMEANDTYPE = 12;
  private static final byte CONSTANT_STRING = 8;
  private static final byte CONSTANT_UTF8 = 1;
  private static final int JAVA_MAGIC_CODE = 0xcafebabe;
  private static final char PACKAGE_SEPARATOR = '.';
  private static final char PATH_SEPARATOR = '/';
  private static final Charset UTF_8 = Charset.forName("UTF-8");

  private Classes() {
    ConstantConditions.avoid();
  }

  @NotNull
  public static Set<String> getDependencies(@NotNull final ByteBuffer buffer) {
    if (buffer.getInt() != JAVA_MAGIC_CODE) {
      throw new IllegalArgumentException("Not a class file");
    }
    buffer.position(8);
    final int constNum = buffer.getChar();
    final BitSet isClass = new BitSet(constNum);
    final BitSet isNamedType = new BitSet(constNum);
    for (int i = 1; i < constNum; ++i) {
      final byte constType = buffer.get();
      switch (constType) {
        case CONSTANT_UTF8:
          buffer.position(buffer.getChar() + buffer.position());
          break;
        case CONSTANT_INTEGER:
        case CONSTANT_FLOAT:
        case CONSTANT_FIELDREF:
        case CONSTANT_METHODREF:
        case CONSTANT_INTERFACEMETHODREF:
        case CONSTANT_INVOKEDYNAMIC:
          buffer.position(buffer.position() + 4);
          break;
        case CONSTANT_LONG:
        case CONSTANT_DOUBLE:
          buffer.position(buffer.position() + 8);
          ++i;
          break;
        case CONSTANT_STRING:
          buffer.position(buffer.position() + 2);
          break;
        case CONSTANT_NAMEANDTYPE:
          buffer.position(buffer.position() + 2);// skip name, fall through:
        case CONSTANT_METHODTYPE:
          isNamedType.set(buffer.getChar());
          break;
        case CONSTANT_CLASS:
          isClass.set(buffer.getChar());
          break;
        case CONSTANT_METHODHANDLE:
          buffer.position(buffer.position() + 3);
          break;
        default:
          throw new IllegalArgumentException(
              "unknown constant pool item type: " + (constType & 0xff));
      }
    }
    buffer.position(buffer.position() + 6);
    buffer.position(buffer.getChar() * 2 + buffer.position());
    // inspect fields and methods
    for (int i = 0; i < 2; ++i) {
      final int memberNum = buffer.getChar();
      for (int j = 0; j < memberNum; ++j) {
        buffer.position(buffer.position() + 4);
        isNamedType.set(buffer.getChar());
        final int attrNum = buffer.getChar();
        for (int k = 0; k < attrNum; ++k) {
          buffer.position(buffer.position() + 2);
          buffer.position(buffer.getInt() + buffer.position());
        }
      }
    }
    buffer.position(10);
    final HashSet<String> names = new HashSet<String>();
    for (int i = 1; i < constNum; ++i) {
      switch (buffer.get()) {
        case CONSTANT_UTF8:
          final int size = buffer.getChar();
          final int start = buffer.position();
          boolean hasMany = isNamedType.get(i);
          if (isClass.get(i)) {
            if (buffer.get(buffer.position()) == ARRAY_START) {
              hasMany = true;
            } else {
              addName(names, buffer, start, size);
            }
          }

          if (hasMany) {
            addNames(names, buffer, start, size);
          }
          buffer.position(start + size);
          break;
        case CONSTANT_INTEGER:
        case CONSTANT_FLOAT:
        case CONSTANT_FIELDREF:
        case CONSTANT_METHODREF:
        case CONSTANT_INTERFACEMETHODREF:
        case CONSTANT_NAMEANDTYPE:
        case CONSTANT_INVOKEDYNAMIC:
          buffer.position(buffer.position() + 4);
          break;
        case CONSTANT_LONG:
        case CONSTANT_DOUBLE:
          buffer.position(buffer.position() + 8);
          i++;
          break;
        case CONSTANT_STRING:
        case CONSTANT_CLASS:
        case CONSTANT_METHODTYPE:
          buffer.position(buffer.position() + 2);
          break;
        case CONSTANT_METHODHANDLE:
          buffer.position(buffer.position() + 3);
          break;
        default:
          throw new AssertionError();
      }
    }
    return names;
  }

  @NotNull
  public static String toPath(@NotNull final Class<?> aClass) {
    return toPath(aClass.getName());
  }

  @NotNull
  public static String toPath(@NotNull final String className) {
    return "/" + className.replace(".", "/") + ".class";
  }

  private static void addName(@NotNull final HashSet<String> names,
      @NotNull final ByteBuffer buffer, int start, final int size) {
    final int end = start + size;
    final StringBuilder builder = new StringBuilder(size);
    boolean isAscii = true;
    for (; start < end; ++start) {
      final byte b = buffer.get(start);
      if (b < 0) {
        isAscii = false;
        break;
      }
      builder.append((char) (b == PATH_SEPARATOR ? PACKAGE_SEPARATOR : b));
    }

    if (!isAscii) {
      final int oldLimit = buffer.limit();
      final int oldPos = builder.length();
      buffer.limit(end).position(start);
      builder.append(UTF_8.decode(buffer));
      buffer.limit(oldLimit);
      for (int pos = oldPos, len = builder.length(); pos < len; ++pos) {
        if (builder.charAt(pos) == PATH_SEPARATOR) {
          builder.setCharAt(pos, PACKAGE_SEPARATOR);
        }
      }
    }
    names.add(builder.toString());
  }

  private static void addNames(@NotNull final HashSet<String> names, ByteBuffer buffer, int start,
      final int size) {
    final int end = start + size;
    for (int i = start; i < end; ++i) {
      if (buffer.get(i) == 'L') {
        int pos = i + 1;
        while (buffer.get(pos) != ';') {
          ++pos;
        }
        addName(names, buffer, i + 1, pos - i - 1);
        i = pos;
      }
    }
  }
}
