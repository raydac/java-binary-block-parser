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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.igormaznitsa.jbbp.JBBPCustomFieldTypeProcessor;
import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPFieldTypeParameterContainer;
import com.igormaznitsa.jbbp.exceptions.JBBPCompilationException;
import com.igormaznitsa.jbbp.io.JBBPArraySizeLimiter;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.model.JBBPAbstractField;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayLong;
import com.igormaznitsa.jbbp.model.JBBPFieldLong;
import com.igormaznitsa.jbbp.model.JBBPFieldStruct;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class PackedBCDCustomFieldTest implements JBBPCustomFieldTypeProcessor {

  private static final String[] types = new String[] {"bcd", "sbcd"};

  public static long readValueFromPackedDecimal(final JBBPBitInputStream in, final int len,
                                                final boolean signed) throws IOException {
    final byte[] data = in.readByteArray(len);

    StringBuilder digitStr = new StringBuilder();
    for (int i = 0; i < len * 2; i++) {
      byte currentByte = data[i / 2];
      byte digit = (i % 2 == 0) ? (byte) ((currentByte & 0xff) >>> 4) : (byte) (currentByte & 0x0f);
      if (digit < 10) {
        digitStr.append(digit);
      }
    }

    if (signed) {
      byte sign = (byte) (data[len - 1] & 0x0f);
      if (sign == 0x0b || sign == 0x0d) {
        digitStr.insert(0, '-');
      }
    }

    return Long.parseLong(digitStr.toString());
  }

  @Override
  public String[] getCustomFieldTypes() {
    return types;
  }

  @Override
  public boolean isAllowed(final JBBPFieldTypeParameterContainer fieldType, final String fieldName,
                           final int extraData, final boolean isArray) {
    if (fieldType.getByteOrder() == JBBPByteOrder.LITTLE_ENDIAN) {
      System.err
          .println("Packed Decimal does not support little endian...using big endian instead");
      return false;
    }

    return extraData > 0 && extraData < 15;
  }

  @Override
  public JBBPAbstractField readCustomFieldType(final JBBPBitInputStream in,
                                               final JBBPBitOrder bitOrder, final int parserFlags,
                                               final JBBPFieldTypeParameterContainer customTypeFieldInfo,
                                               JBBPNamedFieldInfo fieldName, int extraData,
                                               boolean readWholeStream, int arrayLength,
                                               JBBPArraySizeLimiter arraySizeLimiter)
      throws IOException {
    final boolean signed = "sbcd".equals(customTypeFieldInfo.getTypeName());

    if (readWholeStream) {
      throw new UnsupportedOperationException("Whole stream reading unsupported");
    } else {
      if (arrayLength <= 0) {
        return new JBBPFieldLong(fieldName, readValueFromPackedDecimal(in, extraData, signed));
      } else {
        final long[] result = new long[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
          result[i] = readValueFromPackedDecimal(in, extraData, signed);
        }
        return new JBBPFieldArrayLong(fieldName, result);
      }
    }
  }

  @Test
  public void testParse_SingleDefaultNonamedPackedDecimal_Default() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("bcd:4;", this);
    final JBBPFieldStruct result = parser.parse(new byte[] {0x12, 0x34, 0x56, 0x7F});
    assertEquals(1234567L, result.findFieldForType(JBBPFieldLong.class).getAsLong());
  }

  @Test
  public void testParse_SingleDefaultNonamedPackedDecimal_BigEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare(">bcd:4;", this);
    final JBBPFieldStruct result = parser.parse(new byte[] {0x12, 0x34, 0x56, 0x7F});
    assertEquals(1234567L, result.findFieldForType(JBBPFieldLong.class).getAsLong());
  }

  @Test
  public void testParse_SingleDefaultNonamedPackedDecimal_LittleEndian_Exception()
      throws Exception {
    final PackedBCDCustomFieldTest theInstance = this;
    assertThrows(JBBPCompilationException.class, () -> JBBPParser.prepare("<bcd:4;", theInstance));
  }

}
