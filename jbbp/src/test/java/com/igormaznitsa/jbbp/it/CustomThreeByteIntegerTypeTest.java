/*
 * Copyright 2017 Igor Maznitsa.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.igormaznitsa.jbbp.it;

import com.igormaznitsa.jbbp.JBBPCustomFieldTypeProcessor;
import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPFieldTypeParameterContainer;
import com.igormaznitsa.jbbp.exceptions.JBBPParsingException;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example of three byte integer custom type processor to parse unsigned integer values represented by three bytes in data stream.
 */
public class CustomThreeByteIntegerTypeTest extends AbstractParserIntegrationTest {

  @Test
  public void testCustomFieldAsAnonymousSingleField() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("int24;", new Int24CustomTypeProcessor());
    assertEquals(5, parser.parse(new byte[] {0, 0, 5}).findFieldForType(JBBPFieldInt.class).getAsInt());
  }

  @Test
  public void testReadThreeByteInteger_AnonymousArray() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("int24 [_];", new Int24CustomTypeProcessor());
    assertArrayEquals(new int[] {0x010203, 0x040506, 0x070809}, parser.parse(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09}).findFieldForType(JBBPFieldArrayInt.class).getArray());
  }

  @Test
  public void testReadThreeByte_NamedCustomFieldAsArrayLength() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("int24 value; byte [value];", new Int24CustomTypeProcessor());
    assertEquals(5, parser.parse(new byte[] {0, 0, 5, 1, 2, 3, 4, 5}).findFieldForType(JBBPFieldArrayByte.class).size());
  }

  @Test
  public void testReadThreeByteInteger_NamedCustomFieldInExpression() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("int24 value1; int24 value2; byte [value1+value2];", new Int24CustomTypeProcessor());
    final JBBPFieldStruct struct = parser.parse(new byte[] {0, 0, 2, 0, 0, 3, 1, 2, 3, 4, 5});
    assertEquals(5, struct.findFieldForType(JBBPFieldArrayByte.class).size());
    assertEquals(2, struct.findFieldForNameAndType("value1", JBBPFieldInt.class).getAsInt());
    assertEquals(3, struct.findFieldForNameAndType("value2", JBBPFieldInt.class).getAsInt());
  }

  @Test
  public void testReadThreeByteInteger_OneValue() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("int24 value;", new Int24CustomTypeProcessor());
    final JBBPParser inverseparser = JBBPParser.prepare("<int24 value;", new Int24CustomTypeProcessor());
    assertEquals(0x010203, parser.parse(new byte[] {0x01, 0x02, 0x03}).findFieldForType(JBBPFieldInt.class).getAsInt());
    assertEquals(0x8040C0, parser.parse(new JBBPBitInputStream(new ByteArrayInputStream(new byte[] {0x01, 0x02, 0x03}), JBBPBitOrder.MSB0)).findFieldForType(JBBPFieldInt.class).getAsInt());
    assertEquals(0x030201, inverseparser.parse(new byte[] {0x01, 0x02, 0x03}).findFieldForType(JBBPFieldInt.class).getAsInt());
    assertEquals(0xC04080, inverseparser.parse(new JBBPBitInputStream(new ByteArrayInputStream(new byte[] {0x01, 0x02, 0x03}), JBBPBitOrder.MSB0)).findFieldForType(JBBPFieldInt.class).getAsInt());
  }

  @Test
  public void testReadThreeByteInteger_ErrorForEOF() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("int24 value;", new Int24CustomTypeProcessor());
    assertThrows(JBBPParsingException.class, new Executable() {
      @Override
      public void execute() throws Throwable {
        parser.parse(new byte[] {0x01, 0x02});
      }
    });
  }

  @Test
  public void testReadThreeByteInteger_WholeArray() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("int24 [_] array;", new Int24CustomTypeProcessor());
    assertArrayEquals(new int[] {0x010203, 0x040506, 0x070809}, parser.parse(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09}).findFieldForType(JBBPFieldArrayInt.class).getArray());
  }

  @Test
  public void testReadThreeByteInteger_ArrayFirstThreeElements() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("int24 [3] array;", new Int24CustomTypeProcessor());
    assertArrayEquals(new int[] {0x010203, 0x040506, 0x070809}, parser.parse(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C}).findFieldForType(JBBPFieldArrayInt.class).getArray());
  }

  /**
   * Buffer to accumulate integer values.
   */
  private static final class IntBuffer {
    private int[] buffer;
    private int freePos;

    public IntBuffer(final int initialCapacity) {
      this.buffer = new int[Math.max(3, initialCapacity)];
    }

    public IntBuffer put(final int value) {
      if (this.freePos == this.buffer.length) {
        final int[] newbuffer = new int[(this.buffer.length * 3) / 2];
        System.arraycopy(this.buffer, 0, newbuffer, 0, this.buffer.length);
        this.buffer = newbuffer;
      }
      this.buffer[this.freePos++] = value;
      return this;
    }

    public int[] toArray() {
      return Arrays.copyOf(this.buffer, this.freePos);
    }
  }

  /**
   * Class implements custom type processor for three byte unsigned integer values.
   */
  private static final class Int24CustomTypeProcessor implements JBBPCustomFieldTypeProcessor {

    private static final String[] TYPES = new String[] {"int24"};

    private static int readThreeBytesAsInt(final JBBPBitInputStream in, final JBBPByteOrder byteOrder, final JBBPBitOrder bitOrder) throws IOException {
      final int b0 = in.readByte();
      final int b1 = in.readByte();
      final int b2 = in.readByte();

      final int value = byteOrder == JBBPByteOrder.BIG_ENDIAN ? (b0 << 16) | (b1 << 8) | b2 : (b2 << 16) | (b1 << 8) | b0;

      return bitOrder == JBBPBitOrder.LSB0 ? value : ((int) JBBPFieldInt.reverseBits(value) >>> 8);
    }

    @Override
    public String[] getCustomFieldTypes() {
      return TYPES;
    }

    @Override
    public boolean isAllowed(final JBBPFieldTypeParameterContainer fieldType, final String fieldName, final int extraData, final boolean isArray) {
      return extraData == 0;
    }

    @Override
    public JBBPAbstractField readCustomFieldType(final JBBPBitInputStream in, final JBBPBitOrder bitOrder, final int parserFlags, final JBBPFieldTypeParameterContainer customTypeFieldInfo, final JBBPNamedFieldInfo fieldName, final int extraData, final boolean readWholeStream, final int arrayLength) throws IOException {
      if (arrayLength < 0) {
        return new JBBPFieldInt(fieldName, readThreeBytesAsInt(in, customTypeFieldInfo.getByteOrder(), bitOrder));
      } else {
        if (readWholeStream) {
          final IntBuffer intBuffer = new IntBuffer(1024);
          while (in.hasAvailableData()) {
            intBuffer.put(readThreeBytesAsInt(in, customTypeFieldInfo.getByteOrder(), bitOrder));
          }
          return new JBBPFieldArrayInt(fieldName, intBuffer.toArray());
        } else {
          final int[] array = new int[arrayLength];
          for (int i = 0; i < arrayLength; i++) {
            array[i] = readThreeBytesAsInt(in, customTypeFieldInfo.getByteOrder(), bitOrder);
          }
          return new JBBPFieldArrayInt(fieldName, array);
        }
      }
    }

  }
}
