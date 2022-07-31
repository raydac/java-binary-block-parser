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

package com.igormaznitsa.jbbp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.exceptions.JBBPCompilationException;
import com.igormaznitsa.jbbp.exceptions.JBBPParsingException;
import com.igormaznitsa.jbbp.exceptions.JBBPTooManyFieldsFoundException;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.model.JBBPAbstractArrayField;
import com.igormaznitsa.jbbp.model.JBBPAbstractField;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayBit;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayBoolean;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayByte;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayDouble;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayFloat;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayInt;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayLong;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayShort;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayStruct;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayUByte;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayUInt;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayUShort;
import com.igormaznitsa.jbbp.model.JBBPFieldBit;
import com.igormaznitsa.jbbp.model.JBBPFieldBoolean;
import com.igormaznitsa.jbbp.model.JBBPFieldByte;
import com.igormaznitsa.jbbp.model.JBBPFieldDouble;
import com.igormaznitsa.jbbp.model.JBBPFieldFloat;
import com.igormaznitsa.jbbp.model.JBBPFieldInt;
import com.igormaznitsa.jbbp.model.JBBPFieldLong;
import com.igormaznitsa.jbbp.model.JBBPFieldShort;
import com.igormaznitsa.jbbp.model.JBBPFieldString;
import com.igormaznitsa.jbbp.model.JBBPFieldStruct;
import com.igormaznitsa.jbbp.model.JBBPFieldUByte;
import com.igormaznitsa.jbbp.model.JBBPFieldUInt;
import com.igormaznitsa.jbbp.model.JBBPFieldUShort;
import com.igormaznitsa.jbbp.utils.JBBPIntCounter;
import com.igormaznitsa.jbbp.utils.TargetSources;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class JBBPParserTest {

  @Test
  public void testErrorDuringReadingOfNamedField() throws Exception {
    try {
      JBBPParser.prepare("short helloworld;").parse(new byte[] {1});
      fail("Must throw JBBPParsingException");
    } catch (JBBPParsingException ex) {
      assertTrue(ex.getMessage().contains("helloworld"));
    }
  }

  @Test
  public void testErrorDuringReadingOfNonNamedField() throws Exception {
    try {
      JBBPParser.prepare("short;").parse(new byte[] {1});
      fail("Must throw EOFException");
    } catch (EOFException ex) {
      assertNull(ex.getMessage());
    }
  }

  @Test
  public void testFieldNameCaseInsensetive_ExceptionForDuplicationOfFieldNames() throws Exception {
    assertThrows(JBBPCompilationException.class,
        () -> JBBPParser.prepare("bool Field1; byte field1;"));
  }

  @Test
  public void testFieldNameCaseInsensetive() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("bool FiElD1;").parse(new byte[] {1});
    assertTrue(parsed.nameExists("fiELD1"));
    assertTrue(parsed.pathExists("fiELD1"));
    assertNotNull(parsed.findFieldForName("fiELD1"));
    assertNotNull(parsed.findFieldForNameAndType("fiELD1", JBBPFieldBoolean.class));
    assertNotNull(parsed.findFieldForPathAndType("fiELD1", JBBPFieldBoolean.class));
  }

  @Test
  public void testParse_Bool_ErrorForEOF() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("bool;");
    assertThrows(EOFException.class, () -> parser.parse(new byte[0]));
  }

  @Test
  public void testParse_SingleDefaultNonamedBool_Default() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("bool;");
    JBBPFieldStruct result = parser.parse(new byte[] {(byte) 1});
    assertTrue(result.findFieldForType(JBBPFieldBoolean.class).getAsBool());
    result = parser.parse(new byte[] {(byte) 0});
    assertFalse(result.findFieldForType(JBBPFieldBoolean.class).getAsBool());
  }

  @Test
  public void testParse_SingleDefaultNonamedBool_BigEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare(">bool;");
    JBBPFieldStruct result = parser.parse(new byte[] {(byte) 1});
    assertTrue(result.findFieldForType(JBBPFieldBoolean.class).getAsBool());
    result = parser.parse(new byte[] {(byte) 0});
    assertFalse(result.findFieldForType(JBBPFieldBoolean.class).getAsBool());
  }

  @Test
  public void testParse_SingleDefaultNonamedBool_LittleEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("<bool;");
    JBBPFieldStruct result = parser.parse(new byte[] {(byte) 1});
    assertTrue(result.findFieldForType(JBBPFieldBoolean.class).getAsBool());
    result = parser.parse(new byte[] {(byte) 0});
    assertFalse(result.findFieldForType(JBBPFieldBoolean.class).getAsBool());
  }

  @Test
  public void testParse_Byte_ErrorForEOF() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("byte;");
    assertThrows(EOFException.class, () -> parser.parse(new byte[0]));
  }

  @Test
  public void testParse_SingleDefaultNonamedByte_Default() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("byte;");
    final JBBPFieldStruct result = parser.parse(new byte[] {(byte) -42});
    assertEquals(-42, result.findFieldForType(JBBPFieldByte.class).getAsInt());
  }

  @Test
  public void testParse_SingleDefaultNonamedByte_BigEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare(">byte;");
    final JBBPFieldStruct result = parser.parse(new byte[] {(byte) -42});
    assertEquals(-42, result.findFieldForType(JBBPFieldByte.class).getAsInt());
  }

  @Test
  public void testParse_SingleDefaultNonamedByte_LittleEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("<byte;");
    final JBBPFieldStruct result = parser.parse(new byte[] {(byte) -42});
    assertEquals(-42, result.findFieldForType(JBBPFieldByte.class).getAsInt());
  }

  @Test
  public void testCompile_Value() {
    JBBPParser.prepare("val:34 value;");
    JBBPParser.prepare("val:(1+$$) value;");
    JBBPParser.prepare("val:(1<<4) value;");
  }

  @Test
  public void testCompile_Value_CompilationErrors() {
    assertThrows(JBBPCompilationException.class, () -> JBBPParser.prepare("val;"));
    assertThrows(JBBPCompilationException.class, () -> JBBPParser.prepare("val:1;"));
    assertThrows(JBBPCompilationException.class, () -> JBBPParser.prepare("val:(3+5);"));
    assertThrows(JBBPCompilationException.class, () -> JBBPParser.prepare("val a;"));
    assertThrows(JBBPCompilationException.class, () -> JBBPParser.prepare("val:3 [_];"));
    assertThrows(JBBPCompilationException.class, () -> JBBPParser.prepare("val:3 [_] field;"));
  }

  @Test
  public void testParse_Value_Constant() throws Exception {
    final JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(new byte[0]));
    assertEquals(34,
        JBBPParser.prepare("val:34 value;").parse(stream).findFieldForType(JBBPFieldInt.class)
            .getAsInt());
    assertEquals(0L, stream.getCounter());
  }

  @Test
  public void testParse_Value_NegativeConstant() throws Exception {
    final JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(new byte[0]));
    assertEquals(-34,
        JBBPParser.prepare("val:-34 value;").parse(stream).findFieldForType(JBBPFieldInt.class)
            .getAsInt());
    assertEquals(0L, stream.getCounter());
  }

  @Test
  public void testParse_Value_Expression() throws Exception {
    final JBBPBitInputStream stream =
        new JBBPBitInputStream(new ByteArrayInputStream(new byte[] {1, 2}));
    assertEquals(3, JBBPParser.prepare("ubyte a; ubyte b; val:(a+b) value;").parse(stream)
        .findFieldForType(JBBPFieldInt.class).getAsInt());
    assertEquals(2L, stream.getCounter());
  }

  @Test
  public void testParse_Value_UseInExpression_NegativeResult() throws Exception {
    final JBBPBitInputStream stream =
        new JBBPBitInputStream(new ByteArrayInputStream(new byte[] {1, 2}));
    assertEquals(-2,
        JBBPParser.prepare("ubyte a; ubyte b; val:(a-b) value; val:(value*2) secondvalue;")
            .parse(stream).findFieldForNameAndType("secondvalue", JBBPFieldInt.class).getAsInt());
    assertEquals(2L, stream.getCounter());
  }

  @Test
  public void testParse_Value_UseInExpression() throws Exception {
    final JBBPBitInputStream stream =
        new JBBPBitInputStream(new ByteArrayInputStream(new byte[] {1, 2}));
    assertEquals(6,
        JBBPParser.prepare("ubyte a; ubyte b; val:(a+b) value; val:(value*2) secondvalue;")
            .parse(stream).findFieldForNameAndType("secondvalue", JBBPFieldInt.class).getAsInt());
    assertEquals(2L, stream.getCounter());
  }

  @Test
  public void testParse_String_ErrorForEOF() {
    final JBBPParser parser = JBBPParser.prepare("stringj;");
    assertThrows(EOFException.class, () -> parser.parse(new byte[0]));
  }

  @Test
  public void testParse_String_Default() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("stringj;");
    final JBBPFieldStruct result = parser.parse(new byte[] {3, 65, 66, 67});
    assertEquals("ABC", result.findFieldForType(JBBPFieldString.class).getAsString());
  }

  @Test
  public void testParse_String_BigEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare(">stringj;");
    final JBBPFieldStruct result = parser.parse(new byte[] {3, 65, 66, 67});
    assertEquals("ABC", result.findFieldForType(JBBPFieldString.class).getAsString());
  }

  @Test
  public void testParse_String_LittleEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("<stringj;");
    final JBBPFieldStruct result = parser.parse(new byte[] {3, 65, 66, 67});
    assertEquals("ABC", result.findFieldForType(JBBPFieldString.class).getAsString());
  }

  @Test
  public void testParse_UByte_ErrorForEOF() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("ubyte;");
    assertThrows(EOFException.class, () -> parser.parse(new byte[0]));
  }

  @Test
  public void testParse_SingleDefaultNonamedUByte_Default() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("ubyte;");
    final JBBPFieldStruct result = parser.parse(new byte[] {(byte) -42});
    assertEquals(-42 & 0xFF, result.findFieldForType(JBBPFieldUByte.class).getAsInt());
  }

  @Test
  public void testParse_SingleDefaultNonamedUByte_BigEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare(">ubyte;");
    final JBBPFieldStruct result = parser.parse(new byte[] {(byte) -42});
    assertEquals(-42 & 0xFF, result.findFieldForType(JBBPFieldUByte.class).getAsInt());
  }

  @Test
  public void testParse_SingleDefaultNonamedUByte_LittleEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("<ubyte;");
    final JBBPFieldStruct result = parser.parse(new byte[] {(byte) -42});
    assertEquals(-42 & 0xFF, result.findFieldForType(JBBPFieldUByte.class).getAsInt());
  }

  @Test
  public void testParse_Short_ErrorForEOF() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("short;");
    assertThrows(EOFException.class, () -> parser.parse(new byte[0]));
  }

  @Test
  public void testParse_SingleDefaultNonamedShort_Default() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("short;");
    final JBBPFieldStruct result = parser.parse(new byte[] {1, 2});
    assertEquals(0x0102, result.findFieldForType(JBBPFieldShort.class).getAsInt());
  }

  @Test
  public void testParse_SingleDefaultNonamedShort_BigEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare(">short;");
    final JBBPFieldStruct result = parser.parse(new byte[] {1, 2});
    assertEquals(0x0102, result.findFieldForType(JBBPFieldShort.class).getAsInt());
  }

  @Test
  public void testParse_SingleDefaultNonamedShort_LittleEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("<short;");
    final JBBPFieldStruct result = parser.parse(new byte[] {1, 2});
    assertEquals(0x0201, result.findFieldForType(JBBPFieldShort.class).getAsInt());
  }

  @Test
  public void testParse_UShort_ErrorForEOF() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("ushort;");
    assertThrows(EOFException.class, () -> parser.parse(new byte[0]));
  }

  @Test
  public void testParse_SingleDefaultNonamedUShort_Default() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("ushort;");
    final JBBPFieldStruct result = parser.parse(new byte[] {-1, -2});
    assertEquals(((-1 << 8) | (-2 & 0xFF)) & 0xFFFF,
        result.findFieldForType(JBBPFieldUShort.class).getAsInt());
  }

  @Test
  public void testParse_SingleDefaultNonamedUShort_BigEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare(">ushort;");
    final JBBPFieldStruct result = parser.parse(new byte[] {-1, -2});
    assertEquals(((-1 << 8) | (-2 & 0xFF)) & 0xFFFF,
        result.findFieldForType(JBBPFieldUShort.class).getAsInt());
  }

  @Test
  public void testParse_SingleDefaultNonamedUShort_LittleEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("<ushort;");
    final JBBPFieldStruct result = parser.parse(new byte[] {-1, -2});
    assertEquals(((-2 << 8) | (-1 & 0xFF)) & 0xFFFF,
        result.findFieldForType(JBBPFieldUShort.class).getAsInt());
  }

  @Test
  public void testParse_Int_ErrorForEOF() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("int;");
    assertThrows(EOFException.class, () -> parser.parse(new byte[0]));
  }

  @Test
  public void testParse_SingleDefaultNonamedInt_Default() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("int;");
    final JBBPFieldStruct result = parser.parse(new byte[] {1, 2, 3, 4});
    assertEquals(0x01020304, result.findFieldForType(JBBPFieldInt.class).getAsInt());
  }

  @Test
  public void testParse_SingleDefaultNonamedInt_BigEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare(">int;");
    final JBBPFieldStruct result = parser.parse(new byte[] {1, 2, 3, 4});
    assertEquals(0x01020304, result.findFieldForType(JBBPFieldInt.class).getAsInt());
  }

  @Test
  public void testParse_SingleDefaultNonamedInt_LittleEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("<int;");
    final JBBPFieldStruct result = parser.parse(new byte[] {1, 2, 3, 4});
    assertEquals(0x04030201, result.findFieldForType(JBBPFieldInt.class).getAsInt());
  }

  @Test
  public void testParse_Float_ErrorForEOF() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("floatj;");
    assertThrows(EOFException.class, () -> parser.parse(new byte[0]));
  }

  @Test
  public void testParse_SingleDefaultNonamedFloat_Default() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("floatj;");
    final JBBPFieldStruct result = parser.parse(new byte[] {1, 2, 3, 4});
    assertEquals(2.3879393E-38f, result.findFieldForType(JBBPFieldFloat.class).getAsFloat(),
        TestUtils.FLOAT_DELTA);
  }

  @Test
  public void testParse_SingleDefaultNonamedFloat_BigEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare(">floatj;");
    final JBBPFieldStruct result = parser.parse(new byte[] {1, 2, 3, 4});
    assertEquals(2.3879393E-38f, result.findFieldForType(JBBPFieldFloat.class).getAsFloat(),
        TestUtils.FLOAT_DELTA);
  }

  @Test
  public void testParse_SingleDefaultNonamedFloat_LittleEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("<floatj;");
    final JBBPFieldStruct result = parser.parse(new byte[] {1, 2, 3, 4});
    assertEquals(1.5399896E-36f, result.findFieldForType(JBBPFieldFloat.class).getAsFloat(),
        TestUtils.FLOAT_DELTA);
  }

  @Test
  public void testParse_Long_ErrorForEOF() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("long;");
    assertThrows(EOFException.class, () -> parser.parse(new byte[0]));
  }

  @Test
  public void testParse_SingleDefaultNonamedLong_Default() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("long;");
    final JBBPFieldStruct result = parser.parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
    assertEquals(0x0102030405060708L, result.findFieldForType(JBBPFieldLong.class).getAsLong());
  }

  @Test
  public void testParse_SingleDefaultNonamedLong_BigEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare(">long;");
    final JBBPFieldStruct result = parser.parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
    assertEquals(0x0102030405060708L, result.findFieldForType(JBBPFieldLong.class).getAsLong());
  }

  @Test
  public void testParse_SingleDefaultNonamedLong_LittleEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("<long;");
    final JBBPFieldStruct result = parser.parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
    assertEquals(0x0807060504030201L, result.findFieldForType(JBBPFieldLong.class).getAsLong());
  }

  @Test
  public void testParse_Double_ErrorForEOF() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("doublej;");
    assertThrows(EOFException.class, () -> parser.parse(new byte[0]));
  }

  @Test
  public void testParse_SingleDefaultNonamedDouble_Default() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("doublej;");
    final JBBPFieldStruct result = parser.parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
    assertEquals(8.20788039913184E-304d,
        result.findFieldForType(JBBPFieldDouble.class).getAsDouble(), TestUtils.FLOAT_DELTA);
  }

  @Test
  public void testParse_SingleDefaultNonamedDouble_BigEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare(">doublej;");
    final JBBPFieldStruct result = parser.parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
    assertEquals(8.20788039913184E-304d,
        result.findFieldForType(JBBPFieldDouble.class).getAsDouble(), TestUtils.FLOAT_DELTA);
  }

  @Test
  public void testParse_SingleDefaultNonamedDouble_LittleEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("<doublej;");
    final JBBPFieldStruct result = parser.parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
    assertEquals(5.447603722011605E-270d,
        result.findFieldForType(JBBPFieldDouble.class).getAsDouble(), TestUtils.FLOAT_DELTA);
  }

  @Test
  public void testParse_SingleNonamedVar() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("short k; var; int;");

    final JBBPIntCounter counter = new JBBPIntCounter();

    final JBBPFieldStruct struct =
        parser.parse(new byte[] {9, 8, 33, 1, 2, 3, 4}, new JBBPVarFieldProcessor() {

          @Override
          public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(
              final JBBPBitInputStream inStream, final int arraySize,
              final JBBPNamedFieldInfo fieldName, final int extraValue,
              final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap)
              throws IOException {
            fail("Must not be called");
            return null;
          }

          @Override
          public JBBPAbstractField readVarField(final JBBPBitInputStream inStream,
                                                final JBBPNamedFieldInfo fieldName,
                                                final int extraValue, final JBBPByteOrder byteOrder,
                                                final JBBPNamedNumericFieldMap numericFieldMap)
              throws IOException {
            assertNotNull(inStream);
            final int value = inStream.readByte();
            assertEquals(33, value);
            assertEquals(0x0908, numericFieldMap.findFieldForType(JBBPFieldShort.class).getAsInt());

            assertNull(fieldName);
            assertEquals(0, extraValue);
            assertEquals(JBBPByteOrder.BIG_ENDIAN, byteOrder);

            counter.incrementAndGet();

            return new JBBPFieldByte(fieldName, (byte) value);
          }
        }, null);

    assertNotNull(struct);
    assertEquals(33, struct.findFieldForType(JBBPFieldByte.class).getAsInt());
    assertEquals(1, counter.get());
  }

  @Test
  public void testGetFlags() throws Exception {
    assertEquals(123, JBBPParser.prepare("byte;", 123).getFlags());
  }

  @Test
  public void testParse_Bit_ExtraNumericFieldAsExpression() throws Exception {
    final JBBPParser parser =
        JBBPParser.prepare("ubyte a; bit:(a*4-12) b; bit:(a) c; bit:(a*2) d;");
    final JBBPFieldStruct parsed = parser.parse(new byte[] {4, 0x12, 0x34});
    assertEquals(4, parsed.findFieldForNameAndType("a", JBBPFieldUByte.class).getAsInt());
    assertEquals(2, parsed.findFieldForNameAndType("b", JBBPFieldBit.class).getAsInt());
    assertEquals(1, parsed.findFieldForNameAndType("c", JBBPFieldBit.class).getAsInt());
    assertEquals(0x34, parsed.findFieldForNameAndType("d", JBBPFieldBit.class).getAsInt());
  }

  @Test
  public void testParse_BitArray_ExtraNumericFieldAsExpression() throws Exception {
    final JBBPParser parser =
        JBBPParser.prepare("ubyte s; ubyte a; bit:(a*4-12) [s] b; bit:(a*2) d;");
    final JBBPFieldStruct parsed = parser.parse(new byte[] {2, 4, 0x12, 0x34});
    assertEquals(2, parsed.findFieldForNameAndType("s", JBBPFieldUByte.class).getAsInt());
    assertEquals(4, parsed.findFieldForNameAndType("a", JBBPFieldUByte.class).getAsInt());
    assertArrayEquals(new byte[] {2, 1},
        parsed.findFieldForNameAndType("b", JBBPFieldArrayBit.class).getArray());
    assertEquals(0x34, parsed.findFieldForNameAndType("d", JBBPFieldBit.class).getAsInt());
  }

  @Test
  public void testParse_Skip_ExtraNumericFieldAsExpression() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("ubyte a; skip:(a*2); ubyte b;");
    final JBBPFieldStruct parsed = parser.parse(new byte[] {2, 0x12, 0x34, 0x11, 0x22, 0x56});
    assertEquals(2, parsed.findFieldForNameAndType("a", JBBPFieldUByte.class).getAsInt());
    assertEquals(0x56, parsed.findFieldForNameAndType("b", JBBPFieldUByte.class).getAsInt());
  }

  @Test
  public void testParse_Align_ExtraNumericFieldAsExpression() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("ubyte a; align:(a+1); ubyte b;");
    final JBBPFieldStruct parsed = parser.parse(new byte[] {2, 0x12, 0x34, 0x11, 0x22, 0x56});
    assertEquals(2, parsed.findFieldForNameAndType("a", JBBPFieldUByte.class).getAsInt());
    assertEquals(0x11, parsed.findFieldForNameAndType("b", JBBPFieldUByte.class).getAsInt());
  }

  @Test
  public void testParse_Var_ExtraNumericFieldAsExpression() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("ubyte a; var:(a/21) vvv; ubyte b;");
    final JBBPFieldStruct parsed = parser
        .parse(new byte[] {(byte) 123, 0x12, 0x34, 0x11, 0x22, 0x56}, new JBBPVarFieldProcessor() {

          @Override
          public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(
              JBBPBitInputStream inStream, int arraySize, JBBPNamedFieldInfo fieldName,
              int extraValue, JBBPByteOrder byteOrder, JBBPNamedNumericFieldMap numericFieldMap)
              throws IOException {
            throw new UnsupportedOperationException(
                "Not supported yet."); //To change body of generated methods, choose Tools | Templates.
          }

          @Override
          public JBBPAbstractField readVarField(final JBBPBitInputStream inStream,
                                                final JBBPNamedFieldInfo fieldName,
                                                final int extraValue, final JBBPByteOrder byteOrder,
                                                JBBPNamedNumericFieldMap numericFieldMap)
              throws IOException {
            inStream.skip(3);
            assertEquals(123 / 21, extraValue);
            return new JBBPFieldInt(fieldName, 666);
          }
        }, null);
    assertEquals(123, parsed.findFieldForNameAndType("a", JBBPFieldUByte.class).getAsInt());
    assertEquals(666, parsed.findFieldForNameAndType("vvv", JBBPFieldInt.class).getAsInt());
    assertEquals(0x22, parsed.findFieldForNameAndType("b", JBBPFieldUByte.class).getAsInt());
  }

  @Test
  public void testParse_VarArray_ExtraNumericFieldAsExpression() throws Exception {
    final JBBPParser parser =
        JBBPParser.prepare("ubyte s; ubyte a; var:(a/21) [s*2] vvv; ubyte b;");
    final JBBPFieldStruct parsed = parser
        .parse(new byte[] {4, (byte) 123, 0x12, 0x34, 0x11, 0x22, 0x56},
            new JBBPVarFieldProcessor() {

              @Override
              public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(
                  JBBPBitInputStream inStream, int arraySize, JBBPNamedFieldInfo fieldName,
                  int extraValue, JBBPByteOrder byteOrder, JBBPNamedNumericFieldMap numericFieldMap)
                  throws IOException {
                inStream.skip(3);
                assertEquals(123 / 21, extraValue);
                assertEquals(8, arraySize);
                return new JBBPFieldArrayByte(fieldName, new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
              }

              @Override
              public JBBPAbstractField readVarField(final JBBPBitInputStream inStream,
                                                    final JBBPNamedFieldInfo fieldName,
                                                    final int extraValue,
                                                    final JBBPByteOrder byteOrder,
                                                    JBBPNamedNumericFieldMap numericFieldMap)
                  throws IOException {
                throw new UnsupportedOperationException(
                    "Not supported yet."); //To change body of generated methods, choose Tools | Templates.
              }
            }, null);
    assertEquals(123, parsed.findFieldForNameAndType("a", JBBPFieldUByte.class).getAsInt());
    assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6, 7, 8},
        parsed.findFieldForNameAndType("vvv", JBBPFieldArrayByte.class).getArray());
    assertEquals(0x22, parsed.findFieldForNameAndType("b", JBBPFieldUByte.class).getAsInt());
  }

  @Test
  public void testParse_NamedVarWithCustomOrder() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("short k; <var:-12345 Some; int;");
    final JBBPIntCounter counter = new JBBPIntCounter();

    final JBBPFieldStruct struct =
        parser.parse(new byte[] {9, 8, 33, 1, 2, 3, 4}, new JBBPVarFieldProcessor() {

          @Override
          public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(
              final JBBPBitInputStream inStream, final int arraySize,
              final JBBPNamedFieldInfo fieldName, final int extraValue,
              final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap)
              throws IOException {
            fail("Must not be called");
            return null;
          }

          @Override
          public JBBPAbstractField readVarField(final JBBPBitInputStream inStream,
                                                final JBBPNamedFieldInfo fieldName,
                                                final int extraValue, final JBBPByteOrder byteOrder,
                                                final JBBPNamedNumericFieldMap numericFieldMap)
              throws IOException {
            assertNotNull(inStream);
            final int value = inStream.readByte();
            assertEquals(33, value);
            assertEquals(0x0908, numericFieldMap.findFieldForType(JBBPFieldShort.class).getAsInt());

            assertEquals("some", fieldName.getFieldName());
            assertEquals(-12345, extraValue);
            assertEquals(JBBPByteOrder.LITTLE_ENDIAN, byteOrder);

            counter.incrementAndGet();

            return new JBBPFieldByte(fieldName, (byte) value);
          }
        }, null);

    assertEquals(33, struct.findFieldForNameAndType("some", JBBPFieldByte.class).getAsInt());
    assertEquals(1, counter.get());
  }

  @Test
  public void testParse_StringFieldInExpression_NoErrorDuringCompilation() throws Exception {
    JBBPParser.prepare("byte a; stringj b; byte[a+b];");
    JBBPParser.prepare("stringj b; byte[b];");
  }

  @Test
  public void testParse_StringFieldInArihmeticExpression_ArihmeticException() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("byte a; stringj b; byte[a+b];");
    assertThrows(ArithmeticException.class,
        () -> parser.parse(new byte[] {1, 3, 65, 66, 67, 0, 1, 2, 3}));
  }

  @Test
  public void testParse_StringFieldAsSingleVariableInExpression_ArihmeticException()
      throws Exception {
    final JBBPParser parser = JBBPParser.prepare("stringj b; byte[b];");
    assertThrows(ArithmeticException.class,
        () -> parser.parse(new byte[] {3, 65, 66, 67, 0, 1, 2, 3}));
  }

  @Test
  public void testParse_SingleNonamedVar_ErrorForNullResult() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("short k; var; int;");
    assertThrows(NullPointerException.class, () -> {
      final JBBPFieldStruct struct =
          parser.parse(new byte[] {9, 8, 33, 1, 2, 3, 4}, new JBBPVarFieldProcessor() {

            @Override
            public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(
                final JBBPBitInputStream inStream, final int arraySize,
                final JBBPNamedFieldInfo fieldName, final int extraValue,
                final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap)
                throws IOException {
              fail("Must not be called");
              return null;
            }

            @Override
            public JBBPAbstractField readVarField(final JBBPBitInputStream inStream,
                                                  final JBBPNamedFieldInfo fieldName,
                                                  final int extraValue,
                                                  final JBBPByteOrder byteOrder,
                                                  final JBBPNamedNumericFieldMap numericFieldMap)
                throws IOException {
              return null;
            }
          }, null);
    });
  }

  @Test
  public void testParse_SingleNonamedVar_ErrorForArrayResult() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("short k; var; int;");

    assertThrows(JBBPParsingException.class, () -> {
      final JBBPFieldStruct struct =
          parser.parse(new byte[] {9, 8, 33, 1, 2, 3, 4}, new JBBPVarFieldProcessor() {

            @Override
            public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(
                final JBBPBitInputStream inStream, final int arraySize,
                final JBBPNamedFieldInfo fieldName, final int extraValue,
                final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap)
                throws IOException {
              fail("Must not be called");
              return null;
            }

            @Override
            public JBBPAbstractField readVarField(final JBBPBitInputStream inStream,
                                                  final JBBPNamedFieldInfo fieldName,
                                                  final int extraValue,
                                                  final JBBPByteOrder byteOrder,
                                                  final JBBPNamedNumericFieldMap numericFieldMap)
                throws IOException {
              return new JBBPFieldArrayByte(fieldName, new byte[] {1, 2, 3});
            }
          }, null);
    });
  }

  @Test
  public void testParse_SingleNonamedVar_ErrorForDifferentName() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("short k; var name; int;");

    assertThrows(JBBPParsingException.class, () -> {
      final JBBPFieldStruct struct =
          parser.parse(new byte[] {9, 8, 33, 1, 2, 3, 4}, new JBBPVarFieldProcessor() {

            @Override
            public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(
                final JBBPBitInputStream inStream, final int arraySize,
                final JBBPNamedFieldInfo fieldName, final int extraValue,
                final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap)
                throws IOException {
              fail("Must not be called");
              return null;
            }

            @Override
            public JBBPAbstractField readVarField(final JBBPBitInputStream inStream,
                                                  final JBBPNamedFieldInfo fieldName,
                                                  final int extraValue,
                                                  final JBBPByteOrder byteOrder,
                                                  final JBBPNamedNumericFieldMap numericFieldMap)
                throws IOException {
              assertNotNull(fieldName);
              return new JBBPFieldByte(new JBBPNamedFieldInfo("jskdjhsd", "dlkjsf", 0), (byte) 1);
            }
          }, null);
    });
  }

  @Test
  public void testParse_SingleNonamedVarArray() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("short k; var[18]; int;");

    final JBBPIntCounter counter = new JBBPIntCounter();

    final JBBPFieldStruct struct =
        parser.parse(new byte[] {9, 8, 33, 1, 2, 3, 4}, new JBBPVarFieldProcessor() {

          @Override
          public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(
              final JBBPBitInputStream inStream, final int arraySize,
              final JBBPNamedFieldInfo fieldName, final int extraValue,
              final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap)
              throws IOException {
            assertNotNull(inStream);
            final int value = inStream.readByte();
            assertEquals(33, value);
            assertEquals(0x0908, numericFieldMap.findFieldForType(JBBPFieldShort.class).getAsInt());

            assertNull(fieldName);
            assertEquals(0, extraValue);
            assertEquals(18, arraySize);
            assertEquals(JBBPByteOrder.BIG_ENDIAN, byteOrder);

            counter.incrementAndGet();

            return new JBBPFieldArrayByte(fieldName, new byte[] {(byte) value});
          }

          @Override
          public JBBPAbstractField readVarField(final JBBPBitInputStream inStream,
                                                final JBBPNamedFieldInfo fieldName,
                                                final int extraValue, final JBBPByteOrder byteOrder,
                                                final JBBPNamedNumericFieldMap numericFieldMap)
              throws IOException {
            fail("Must not be called");
            return null;
          }
        }, null);

    assertNotNull(struct);
    assertArrayEquals(new byte[] {33},
        struct.findFieldForType(JBBPFieldArrayByte.class).getArray());
    assertEquals(1, counter.get());
  }

  @Test
  public void testParse_NamedVarArrayWithCustomOrder() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("short k; <var:-12345 [2334] Some; int;");

    final JBBPIntCounter counter = new JBBPIntCounter();

    final JBBPFieldStruct struct =
        parser.parse(new byte[] {9, 8, 33, 1, 2, 3, 4}, new JBBPVarFieldProcessor() {

          @Override
          public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(
              final JBBPBitInputStream inStream, final int arraySize,
              final JBBPNamedFieldInfo fieldName, final int extraValue,
              final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap)
              throws IOException {
            assertNotNull(inStream);
            final int value = inStream.readByte();
            assertEquals(33, value);
            assertEquals(0x0908, numericFieldMap.findFieldForType(JBBPFieldShort.class).getAsInt());

            assertEquals("some", fieldName.getFieldName());
            assertEquals(-12345, extraValue);
            assertEquals(2334, arraySize);
            assertEquals(JBBPByteOrder.LITTLE_ENDIAN, byteOrder);

            counter.incrementAndGet();

            return new JBBPFieldArrayByte(fieldName, new byte[] {(byte) value});
          }

          @Override
          public JBBPAbstractField readVarField(final JBBPBitInputStream inStream,
                                                final JBBPNamedFieldInfo fieldName,
                                                final int extraValue, final JBBPByteOrder byteOrder,
                                                final JBBPNamedNumericFieldMap numericFieldMap)
              throws IOException {
            fail("Must not be called");
            return null;
          }
        }, null);

    assertNotNull(struct);
    assertArrayEquals(new byte[] {33},
        struct.findFieldForNameAndType("some", JBBPFieldArrayByte.class).getArray());
    assertEquals(1, counter.get());
  }

  @Test
  public void testParse_NamedVarArrayTillEndOfStream() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("short k; var [_] Some;");

    final JBBPIntCounter counter = new JBBPIntCounter();

    final JBBPFieldStruct struct =
        parser.parse(new byte[] {9, 8, 33, 1, 2, 3, 4}, new JBBPVarFieldProcessor() {

          @Override
          public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(
              final JBBPBitInputStream inStream, final int arraySize,
              final JBBPNamedFieldInfo fieldName, final int extraValue,
              final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap)
              throws IOException {
            assertNotNull(inStream);

            assertEquals(0x0908, numericFieldMap.findFieldForType(JBBPFieldShort.class).getAsInt());

            assertEquals("some", fieldName.getFieldName());
            assertEquals(0, extraValue);
            assertTrue(arraySize < 0);
            assertEquals(JBBPByteOrder.BIG_ENDIAN, byteOrder);

            counter.incrementAndGet();

            return new JBBPFieldArrayByte(fieldName, inStream.readByteArray(-1));
          }

          @Override
          public JBBPAbstractField readVarField(final JBBPBitInputStream inStream,
                                                final JBBPNamedFieldInfo fieldName,
                                                final int extraValue, final JBBPByteOrder byteOrder,
                                                final JBBPNamedNumericFieldMap numericFieldMap)
              throws IOException {
            fail("Must not be called");
            return null;
          }
        }, null);

    assertNotNull(struct);
    assertArrayEquals(new byte[] {33, 1, 2, 3, 4},
        struct.findFieldForNameAndType("some", JBBPFieldArrayByte.class).getArray());
    assertEquals(1, counter.get());
  }

  @Test
  public void testParse_NamedVarArrayForZeroLength() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("short k; var [k] Some;");

    final JBBPFieldStruct struct = parser.parse(new byte[] {0, 0}, new JBBPVarFieldProcessor() {

      @Override
      public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(
          final JBBPBitInputStream inStream, final int arraySize,
          final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder,
          final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        assertEquals(0, arraySize);
        return new JBBPFieldArrayByte(fieldName, new byte[0]);
      }

      @Override
      public JBBPAbstractField readVarField(final JBBPBitInputStream inStream,
                                            final JBBPNamedFieldInfo fieldName,
                                            final int extraValue, final JBBPByteOrder byteOrder,
                                            final JBBPNamedNumericFieldMap numericFieldMap)
          throws IOException {
        fail("Must not be called");
        return null;
      }
    }, null);

    assertNotNull(struct);
    assertEquals(0, struct.findFieldForNameAndType("some", JBBPFieldArrayByte.class).size());
  }

  @Test
  public void testParse_SingleNonamedVarArray_ErrorForNullResult() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("short k; var [k]; int;");

    assertThrows(NullPointerException.class, () -> {
      final JBBPFieldStruct struct =
          parser.parse(new byte[] {9, 8, 33, 1, 2, 3, 4}, new JBBPVarFieldProcessor() {

            @Override
            public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(
                final JBBPBitInputStream inStream, final int arraySize,
                final JBBPNamedFieldInfo fieldName, final int extraValue,
                final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap)
                throws IOException {
              assertEquals(0x0908, arraySize);
              return null;
            }

            @Override
            public JBBPAbstractField readVarField(final JBBPBitInputStream inStream,
                                                  final JBBPNamedFieldInfo fieldName,
                                                  final int extraValue,
                                                  final JBBPByteOrder byteOrder,
                                                  final JBBPNamedNumericFieldMap numericFieldMap)
                throws IOException {
              fail("Must not be called");
              return null;
            }
          }, null);
    });
  }

  @Test
  public void testParse_SingleNonamedVarArray_ErrorForDifferentName() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("short k; var [234] name; int;");

    assertThrows(JBBPParsingException.class, () -> {
      final JBBPFieldStruct struct =
          parser.parse(new byte[] {9, 8, 33, 1, 2, 3, 4}, new JBBPVarFieldProcessor() {

            @Override
            public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(
                final JBBPBitInputStream inStream, final int arraySize,
                final JBBPNamedFieldInfo fieldName, final int extraValue,
                final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap)
                throws IOException {
              assertNotNull(fieldName);
              return new JBBPFieldArrayByte(new JBBPNamedFieldInfo("jskdjhsd", "dlkjsf", 0),
                  new byte[] {1});
            }

            @Override
            public JBBPAbstractField readVarField(final JBBPBitInputStream inStream,
                                                  final JBBPNamedFieldInfo fieldName,
                                                  final int extraValue,
                                                  final JBBPByteOrder byteOrder,
                                                  final JBBPNamedNumericFieldMap numericFieldMap)
                throws IOException {
              fail("Must not be called");
              return null;
            }
          }, null);
    });
  }

  @Test
  public void testParse_BitFields_EOF() throws Exception {
    assertThrows(EOFException.class, () -> JBBPParser.prepare("bit:4;").parse(new byte[0]));
  }

  @Test
  public void testParse_BitFields_SizeProvidedThroughExpression() throws Exception {
    assertEquals(4,
        JBBPParser.prepare("ubyte a; ubyte b; bit:(a+b) c;").parse(new byte[] {1, 2, (byte) 0xB4})
            .findFieldForType(JBBPFieldBit.class).getAsInt());
    assertEquals(20,
        JBBPParser.prepare("ubyte a; ubyte b; bit:(a+b) c;").parse(new byte[] {3, 2, (byte) 0xB4})
            .findFieldForType(JBBPFieldBit.class).getAsInt());
  }

  @Test
  public void testParse_BitFields_ErrorForWrongValueOfBitFieldLength() throws Exception {
    assertThrows(IllegalArgumentException.class,
        () -> JBBPParser.prepare("ubyte a; ubyte b; bit:(a+b) c;")
            .parse(new byte[] {11, 2, (byte) 0xB4}));
    assertThrows(IllegalArgumentException.class,
        () -> JBBPParser.prepare("ubyte a; ubyte b; bit:(a-b) c;")
            .parse(new byte[] {2, 2, (byte) 0xB4}));
  }

  @Test
  public void testParse_BitFieldArray_EOF() throws Exception {
    assertThrows(EOFException.class, () -> JBBPParser.prepare("bit:4 [1];").parse(new byte[0]));
  }

  @Test
  public void testParse_BitFieldArrayWholeStream_Empty() throws Exception {
    assertEquals(0, JBBPParser.prepare("bit:4 [_];").parse(new byte[0])
        .findFieldForType(JBBPFieldArrayBit.class).size());
  }

  @Test
  public void testParse_SeveralPrimitiveFields() throws Exception {
    final JBBPParser parser =
        JBBPParser.prepare("bit:4; bit:4; bool; byte; ubyte; short; ushort; int; long;");
    final JBBPFieldStruct result = parser.parse(
        new byte[] {0x12, 1, 87, (byte) 0xF3, 1, 2, (byte) 0xFE, 4, 6, 7, 8, 9, (byte) 0xFF, 1, 2,
            3, 5, 6, 7, 8, 9});

    assertEquals(2, result.findFirstFieldForType(JBBPFieldBit.class).getAsInt());
    assertEquals(1, result.findLastFieldForType(JBBPFieldBit.class).getAsInt());

    try {
      result.findFieldForType(JBBPFieldBit.class);
      fail("Must throw JBBPTooManyFieldsFoundException");
    } catch (JBBPTooManyFieldsFoundException ex) {
      assertEquals(2, ex.getNumberOfFoundInstances());
    }

    assertTrue(result.findFieldForType(JBBPFieldBoolean.class).getAsBool());
    assertEquals(87, result.findFieldForType(JBBPFieldByte.class).getAsInt());
    assertEquals(0xF3, result.findFieldForType(JBBPFieldUByte.class).getAsInt());
    assertEquals(0x0102, result.findFieldForType(JBBPFieldShort.class).getAsInt());
    assertEquals(0xFE04, result.findFieldForType(JBBPFieldUShort.class).getAsInt());
    assertEquals(0x06070809, result.findFieldForType(JBBPFieldInt.class).getAsInt());
    assertEquals(0xFF01020305060708L, result.findFieldForType(JBBPFieldLong.class).getAsLong());
  }

  @Test
  public void testParse_Align_Default_EmptyStream_NoErrors() throws Exception {
    assertNotNull(JBBPParser.prepare("byte; align;").parse(new byte[] {1}));
  }

  @Test
  public void testParse_Align_ErrorForEOF() throws Exception {
    assertThrows(EOFException.class,
        () -> JBBPParser.prepare("byte; align:34;").parse(new byte[] {1}));
  }

  @Test
  public void testParse_Align_Default() throws Exception {
    final JBBPParser parser =
        JBBPParser.prepare("bit:4; align; bool; byte; ubyte; short; ushort; int; long;");
    final JBBPFieldStruct result = parser.parse(
        new byte[] {0x12, 1, 87, (byte) 0xF3, 1, 2, (byte) 0xFE, 4, 6, 7, 8, 9, (byte) 0xFF, 1, 2,
            3, 5, 6, 7, 8, 9});
    assertEquals(2, result.findFieldForType(JBBPFieldBit.class).getAsInt());
    assertTrue(result.findFieldForType(JBBPFieldBoolean.class).getAsBool());
    assertEquals(87, result.findFieldForType(JBBPFieldByte.class).getAsInt());
    assertEquals(0xF3, result.findFieldForType(JBBPFieldUByte.class).getAsInt());
    assertEquals(0x0102, result.findFieldForType(JBBPFieldShort.class).getAsInt());
    assertEquals(0xFE04, result.findFieldForType(JBBPFieldUShort.class).getAsInt());
    assertEquals(0x06070809, result.findFieldForType(JBBPFieldInt.class).getAsInt());
    assertEquals(0xFF01020305060708L, result.findFieldForType(JBBPFieldLong.class).getAsLong());
  }

  @Test
  public void testParse_Align_LongDistance() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("ubyte; align:32000; short;");

    final byte[] data = new byte[32002];
    data[0] = (byte) 0xFE;
    data[32000] = 0x12;
    data[32001] = 0x34;

    final JBBPFieldStruct root = parser.parse(data);

    assertEquals(0xFE, root.findFieldForType(JBBPFieldUByte.class).getAsInt());
    assertEquals(0x1234, root.findFieldForType(JBBPFieldShort.class).getAsInt());
  }

  @Test
  public void testParse_Skip_Default_ErrorForEOF() throws Exception {
    assertThrows(EOFException.class, () -> JBBPParser.prepare("byte; skip;").parse(new byte[] {1}));
  }

  @Test
  public void testParse_Skip_ErrorForEOF() throws Exception {
    assertThrows(EOFException.class,
        () -> JBBPParser.prepare("byte; skip:34;").parse(new byte[] {1}));
  }

  @Test
  public void testParse_Skip_WithoutArgument() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("byte; skip; short;");
    final JBBPFieldStruct result = parser.parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
    assertEquals(1, result.findFirstFieldForType(JBBPFieldByte.class).getAsInt());
    assertEquals(0x0304, result.findFieldForType(JBBPFieldShort.class).getAsInt());
  }

  @Test
  public void testParse_Skip_ShortDistance() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("byte; skip:3; short;");
    final JBBPFieldStruct result = parser.parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
    assertEquals(1, result.findFirstFieldForType(JBBPFieldByte.class).getAsInt());
    assertEquals(0x0506, result.findFieldForType(JBBPFieldShort.class).getAsInt());
  }

  @Test
  public void testParse_Skip_LongDistance() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("byte; skip:32000; short;");
    final byte[] data = new byte[32003];
    data[0] = 1;
    data[32001] = 0x0A;
    data[32002] = 0x0B;
    final JBBPFieldStruct result = parser.parse(data);
    assertEquals(1, result.findFirstFieldForType(JBBPFieldByte.class).getAsInt());
    assertEquals(0x0A0B, result.findFieldForType(JBBPFieldShort.class).getAsInt());
  }

  @Test
  public void testParse_Skip_TooLongDistance() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("byte; skip:33; short;");
    assertThrows(EOFException.class, () -> parser.parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}));
  }

  @Test
  public void testParse_Align_Int_WithEffect() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("byte; byte; align:4; int;");
    final JBBPFieldStruct result = parser.parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
    assertEquals(1, result.findFirstFieldForType(JBBPFieldByte.class).getAsInt());
    assertEquals(2, result.findLastFieldForType(JBBPFieldByte.class).getAsInt());
    assertEquals(0x05060708, result.findLastFieldForType(JBBPFieldInt.class).getAsInt());
  }

  @Test
  public void testParse_Align_Int_WithoutEffect() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("byte; byte; byte; byte; align:4; int;");
    final JBBPFieldStruct result = parser.parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
    assertEquals(1, result.findFirstFieldForType(JBBPFieldByte.class).getAsInt());
    assertEquals(4, result.findLastFieldForType(JBBPFieldByte.class).getAsInt());
    assertEquals(0x05060708, result.findLastFieldForType(JBBPFieldInt.class).getAsInt());
  }

  @Test
  public void testParse_FixedBitArray_EOFException() throws Exception {
    assertThrows(EOFException.class,
        () -> JBBPParser.prepare("byte; bit:4[1];").parse(new byte[] {1}));
  }

  @Test
  public void testParse_NonFixedBitArray_ParsedAsEmptyArray() throws Exception {
    final JBBPFieldStruct parsed =
        JBBPParser.prepare("byte; bit:4[_] array;").parse(new byte[] {1});
    assertEquals(0, parsed.findFieldForNameAndType("array", JBBPFieldArrayBit.class).size());
  }

  @Test
  public void testParse_ProcessingOfExtraFieldValuesInSkippedStructureFields() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare(
        "byte len; struct1 [len] { int a; var:23231223 [1024] helloarray; int b; bit:3; bit:7 [10233]; var:-1332 hello; skip:34221223; bit:7; bit:1; align:3445; bit:2; int skippedInt; long lng; insidestruct {bit:1; bit:2; bit:3;} } int end; ")
        .parse(new byte[] {0, 1, 2, 3, 4}, new JBBPVarFieldProcessor() {

          @Override
          public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(
              JBBPBitInputStream inStream, int arraySize, JBBPNamedFieldInfo fieldName,
              int extraValue, JBBPByteOrder byteOrder, JBBPNamedNumericFieldMap numericFieldMap)
              throws IOException {
            fail("Must not be called");
            return null;
          }

          @Override
          public JBBPAbstractField readVarField(JBBPBitInputStream inStream,
                                                JBBPNamedFieldInfo fieldName, int extraValue,
                                                JBBPByteOrder byteOrder,
                                                JBBPNamedNumericFieldMap numericFieldMap)
              throws IOException {
            fail("Must not be called");
            return null;
          }
        }, null);
    assertEquals(0x01020304, parsed.findFieldForNameAndType("end", JBBPFieldInt.class).getAsInt());
  }

  @Test
  public void testParse_ProcessingOfExtraFieldValuesInSkippedStructureFields1() throws Exception {
    final JBBPFieldStruct parsed =
        JBBPParser.prepare("byte len; struct1 [len] {var:-1332 hello; align:3445; } int end; ")
            .parse(new byte[] {0, 1, 2, 3, 4}, new JBBPVarFieldProcessor() {

              @Override
              public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(
                  JBBPBitInputStream inStream, int arraySize, JBBPNamedFieldInfo fieldName,
                  int extraValue, JBBPByteOrder byteOrder, JBBPNamedNumericFieldMap numericFieldMap)
                  throws IOException {
                fail("Must not be called");
                return null;
              }

              @Override
              public JBBPAbstractField readVarField(JBBPBitInputStream inStream,
                                                    JBBPNamedFieldInfo fieldName, int extraValue,
                                                    JBBPByteOrder byteOrder,
                                                    JBBPNamedNumericFieldMap numericFieldMap)
                  throws IOException {
                fail("Must not be called");
                return null;
              }
            }, null);
    assertEquals(0x01020304, parsed.findFieldForNameAndType("end", JBBPFieldInt.class).getAsInt());
  }

  @Test
  public void testParse_FixedBitArray() throws Exception {
    final JBBPFieldArrayBit bits =
        JBBPParser.prepare("bit:4 [8];").parse(new byte[] {0x21, 0x43, 0x65, (byte) 0x87})
            .findFieldForType(JBBPFieldArrayBit.class);
    assertEquals(8, bits.size());
    assertEquals(1, bits.getAsInt(0));
    assertEquals(2, bits.getAsInt(1));
    assertEquals(3, bits.getAsInt(2));
    assertEquals(4, bits.getAsInt(3));
    assertEquals(5, bits.getAsInt(4));
    assertEquals(6, bits.getAsInt(5));
    assertEquals(7, bits.getAsInt(6));
    assertEquals(8, bits.getAsInt(7));
  }

  @Test
  public void testParse_NonFixedBitArray() throws Exception {
    final JBBPFieldArrayBit bits =
        JBBPParser.prepare("bit:4 [_];").parse(new byte[] {0x21, 0x43, 0x65, (byte) 0x87})
            .findFieldForType(JBBPFieldArrayBit.class);
    assertEquals(8, bits.size());
    assertEquals(1, bits.getAsInt(0));
    assertEquals(2, bits.getAsInt(1));
    assertEquals(3, bits.getAsInt(2));
    assertEquals(4, bits.getAsInt(3));
    assertEquals(5, bits.getAsInt(4));
    assertEquals(6, bits.getAsInt(5));
    assertEquals(7, bits.getAsInt(6));
    assertEquals(8, bits.getAsInt(7));
  }

  @Test
  public void testParse_FixedByteArray_EOFException() throws Exception {
    assertThrows(EOFException.class,
        () -> JBBPParser.prepare("byte; byte[1];").parse(new byte[] {1}));
  }

  @Test
  public void testParse_NonFixedByteArray_ParsedAsEmptyArray() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte; byte[_] array;").parse(new byte[] {1});
    assertEquals(0, parsed.findFieldForNameAndType("array", JBBPFieldArrayByte.class).size());
  }

  @Test
  public void testParse_FixedByteArray_Default() throws Exception {
    final JBBPFieldArrayByte bytes =
        JBBPParser.prepare("byte[4];").parse(new byte[] {0x21, 0x43, 0x65, (byte) 0x87})
            .findFieldForType(JBBPFieldArrayByte.class);
    assertEquals(4, bytes.size());
    assertEquals(0x21, bytes.getAsInt(0) & 0xFF);
    assertEquals(0x43, bytes.getAsInt(1) & 0xFF);
    assertEquals(0x65, bytes.getAsInt(2) & 0xFF);
    assertEquals(0x87, bytes.getAsInt(3) & 0xFF);
  }

  @Test
  public void testParse_FixedByteArray_BigEndian() throws Exception {
    final JBBPFieldArrayByte bytes =
        JBBPParser.prepare(">byte[4];").parse(new byte[] {0x21, 0x43, 0x65, (byte) 0x87})
            .findFieldForType(JBBPFieldArrayByte.class);
    assertEquals(4, bytes.size());
    assertEquals(0x21, bytes.getAsInt(0) & 0xFF);
    assertEquals(0x43, bytes.getAsInt(1) & 0xFF);
    assertEquals(0x65, bytes.getAsInt(2) & 0xFF);
    assertEquals(0x87, bytes.getAsInt(3) & 0xFF);
  }

  @Test
  public void testParse_FixedByteArray_LittleEndian() throws Exception {
    final JBBPFieldArrayByte bytes =
        JBBPParser.prepare("<byte[4];").parse(new byte[] {0x21, 0x43, 0x65, (byte) 0x87})
            .findFieldForType(JBBPFieldArrayByte.class);
    assertEquals(4, bytes.size());
    assertEquals(0x87, bytes.getAsInt(0) & 0xFF);
    assertEquals(0x65, bytes.getAsInt(1) & 0xFF);
    assertEquals(0x43, bytes.getAsInt(2) & 0xFF);
    assertEquals(0x21, bytes.getAsInt(3) & 0xFF);
  }

  @Test
  public void testParse_NonFixedByteArray_Default() throws Exception {
    final JBBPFieldArrayByte bytes =
        JBBPParser.prepare("byte[_];").parse(new byte[] {0x21, 0x43, 0x65, (byte) 0x87})
            .findFieldForType(JBBPFieldArrayByte.class);
    assertEquals(4, bytes.size());
    assertEquals(0x21, bytes.getAsInt(0) & 0xFF);
    assertEquals(0x43, bytes.getAsInt(1) & 0xFF);
    assertEquals(0x65, bytes.getAsInt(2) & 0xFF);
    assertEquals(0x87, bytes.getAsInt(3) & 0xFF);
  }

  @Test
  public void testParse_NonFixedByteArray_BigEndian() throws Exception {
    final JBBPFieldArrayByte bytes =
        JBBPParser.prepare(">byte[_];").parse(new byte[] {0x21, 0x43, 0x65, (byte) 0x87})
            .findFieldForType(JBBPFieldArrayByte.class);
    assertEquals(4, bytes.size());
    assertEquals(0x21, bytes.getAsInt(0) & 0xFF);
    assertEquals(0x43, bytes.getAsInt(1) & 0xFF);
    assertEquals(0x65, bytes.getAsInt(2) & 0xFF);
    assertEquals(0x87, bytes.getAsInt(3) & 0xFF);
  }

  @Test
  public void testParse_NonFixedByteArray_LittleEndian() throws Exception {
    final JBBPFieldArrayByte bytes =
        JBBPParser.prepare("<byte[_];").parse(new byte[] {0x21, 0x43, 0x65, (byte) 0x87})
            .findFieldForType(JBBPFieldArrayByte.class);
    assertEquals(4, bytes.size());
    assertEquals(0x87, bytes.getAsInt(0) & 0xFF);
    assertEquals(0x65, bytes.getAsInt(1) & 0xFF);
    assertEquals(0x43, bytes.getAsInt(2) & 0xFF);
    assertEquals(0x21, bytes.getAsInt(3) & 0xFF);
  }

  @Test
  public void testParse_FixedUByteArray_EOFException() throws Exception {
    assertThrows(EOFException.class,
        () -> JBBPParser.prepare("byte; ubyte[1];").parse(new byte[] {1}));
  }

  @Test
  public void testParse_NonFixedUByteArray_ParsedAsEmptyArray() throws Exception {
    final JBBPFieldStruct parsed =
        JBBPParser.prepare("byte; ubyte[_] array;").parse(new byte[] {1});
    assertEquals(0, parsed.findFieldForNameAndType("array", JBBPFieldArrayUByte.class).size());
  }

  @Test
  public void testParse_FixedUByteArray_Default() throws Exception {
    final JBBPFieldArrayUByte bytes =
        JBBPParser.prepare("ubyte[4];").parse(new byte[] {0x21, 0x43, 0x65, (byte) 0x87})
            .findFieldForType(JBBPFieldArrayUByte.class);
    assertEquals(4, bytes.size());
    assertEquals(0x21, bytes.getAsInt(0));
    assertEquals(0x43, bytes.getAsInt(1));
    assertEquals(0x65, bytes.getAsInt(2));
    assertEquals(0x87, bytes.getAsInt(3));
  }

  @Test
  public void testParse_FixedUByteArray_BigEndian() throws Exception {
    final JBBPFieldArrayUByte bytes =
        JBBPParser.prepare(">ubyte[4];").parse(new byte[] {0x21, 0x43, 0x65, (byte) 0x87})
            .findFieldForType(JBBPFieldArrayUByte.class);
    assertEquals(4, bytes.size());
    assertEquals(0x21, bytes.getAsInt(0));
    assertEquals(0x43, bytes.getAsInt(1));
    assertEquals(0x65, bytes.getAsInt(2));
    assertEquals(0x87, bytes.getAsInt(3));
  }

  @Test
  public void testParse_FixedUByteArray_LittleEndian() throws Exception {
    final JBBPFieldArrayUByte bytes =
        JBBPParser.prepare("<ubyte[4];").parse(new byte[] {0x21, 0x43, 0x65, (byte) 0x87})
            .findFieldForType(JBBPFieldArrayUByte.class);
    assertEquals(4, bytes.size());
    assertEquals(0x87, bytes.getAsInt(0));
    assertEquals(0x65, bytes.getAsInt(1));
    assertEquals(0x43, bytes.getAsInt(2));
    assertEquals(0x21, bytes.getAsInt(3));
  }

  @Test
  public void testParse_NonFixedUByteArray_Default() throws Exception {
    final JBBPFieldArrayUByte bytes =
        JBBPParser.prepare("ubyte[_];").parse(new byte[] {0x21, 0x43, 0x65, (byte) 0x87})
            .findFieldForType(JBBPFieldArrayUByte.class);
    assertEquals(4, bytes.size());
    assertEquals(0x21, bytes.getAsInt(0));
    assertEquals(0x43, bytes.getAsInt(1));
    assertEquals(0x65, bytes.getAsInt(2));
    assertEquals(0x87, bytes.getAsInt(3));
  }

  @Test
  public void testParse_NonFixedUByteArray_BigEndian() throws Exception {
    final JBBPFieldArrayUByte bytes =
        JBBPParser.prepare(">ubyte[_];").parse(new byte[] {0x21, 0x43, 0x65, (byte) 0x87})
            .findFieldForType(JBBPFieldArrayUByte.class);
    assertEquals(4, bytes.size());
    assertEquals(0x21, bytes.getAsInt(0));
    assertEquals(0x43, bytes.getAsInt(1));
    assertEquals(0x65, bytes.getAsInt(2));
    assertEquals(0x87, bytes.getAsInt(3));
  }

  @Test
  public void testParse_NonFixedUByteArray_LittleEndian() throws Exception {
    final JBBPFieldArrayUByte bytes =
        JBBPParser.prepare("<ubyte[_];").parse(new byte[] {0x21, 0x43, 0x65, (byte) 0x87})
            .findFieldForType(JBBPFieldArrayUByte.class);
    assertEquals(4, bytes.size());
    assertEquals(0x87, bytes.getAsInt(0));
    assertEquals(0x65, bytes.getAsInt(1));
    assertEquals(0x43, bytes.getAsInt(2));
    assertEquals(0x21, bytes.getAsInt(3));
  }

  @Test
  public void testParse_FixedBooleanArray_EOFException() throws Exception {
    assertThrows(EOFException.class,
        () -> JBBPParser.prepare("byte; bool[1];").parse(new byte[] {1}));
  }

  @Test
  public void testParse_NonFixedBoolArray_ParsedAsEmptyArray() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte; bool[_] array;").parse(new byte[] {1});
    assertEquals(0, parsed.findFieldForNameAndType("array", JBBPFieldArrayBoolean.class).size());
  }

  @Test
  public void testParse_FixedBooleanArray_Default() throws Exception {
    final JBBPFieldArrayBoolean bools =
        JBBPParser.prepare("bool[4];").parse(new byte[] {0, 0x43, 0x65, 0})
            .findFieldForType(JBBPFieldArrayBoolean.class);
    assertEquals(4, bools.size());
    assertFalse(bools.getAsBool(0));
    assertTrue(bools.getAsBool(1));
    assertTrue(bools.getAsBool(2));
    assertFalse(bools.getAsBool(3));
  }

  @Test
  public void testParse_FixedBooleanArray_BigEndian() throws Exception {
    final JBBPFieldArrayBoolean bools =
        JBBPParser.prepare(">bool[4];").parse(new byte[] {0, 0x43, 0x65, 0})
            .findFieldForType(JBBPFieldArrayBoolean.class);
    assertEquals(4, bools.size());
    assertFalse(bools.getAsBool(0));
    assertTrue(bools.getAsBool(1));
    assertTrue(bools.getAsBool(2));
    assertFalse(bools.getAsBool(3));
  }

  @Test
  public void testParse_FixedBooleanArray_LittleEndian() throws Exception {
    final JBBPFieldArrayBoolean bools =
        JBBPParser.prepare("<bool[4];").parse(new byte[] {0, 0x43, 0x65, 0})
            .findFieldForType(JBBPFieldArrayBoolean.class);
    assertEquals(4, bools.size());
    assertFalse(bools.getAsBool(0));
    assertTrue(bools.getAsBool(1));
    assertTrue(bools.getAsBool(2));
    assertFalse(bools.getAsBool(3));
  }

  @Test
  public void testParse_NonFixedBooleanArray_Default() throws Exception {
    final JBBPFieldArrayBoolean bools =
        JBBPParser.prepare("bool[_];").parse(new byte[] {0, 0x43, 0x65, 0})
            .findFieldForType(JBBPFieldArrayBoolean.class);
    assertEquals(4, bools.size());
    assertFalse(bools.getAsBool(0));
    assertTrue(bools.getAsBool(1));
    assertTrue(bools.getAsBool(2));
    assertFalse(bools.getAsBool(3));
  }

  @Test
  public void testParse_NonFixedBooleanArray_BigEndian() throws Exception {
    final JBBPFieldArrayBoolean bools =
        JBBPParser.prepare(">bool[_];").parse(new byte[] {0, 0x43, 0x65, 0})
            .findFieldForType(JBBPFieldArrayBoolean.class);
    assertEquals(4, bools.size());
    assertFalse(bools.getAsBool(0));
    assertTrue(bools.getAsBool(1));
    assertTrue(bools.getAsBool(2));
    assertFalse(bools.getAsBool(3));
  }

  @Test
  public void testParse_NonFixedBooleanArray_LittleEndian() throws Exception {
    final JBBPFieldArrayBoolean bools =
        JBBPParser.prepare("<bool[_];").parse(new byte[] {0, 0x43, 0x65, 0})
            .findFieldForType(JBBPFieldArrayBoolean.class);
    assertEquals(4, bools.size());
    assertFalse(bools.getAsBool(0));
    assertTrue(bools.getAsBool(1));
    assertTrue(bools.getAsBool(2));
    assertFalse(bools.getAsBool(3));
  }

  @Test
  public void testParse_FixedShortArray_EOFException() throws Exception {
    assertThrows(EOFException.class,
        () -> JBBPParser.prepare("byte; short[1];").parse(new byte[] {1}));
  }

  @Test
  public void testParse_NonFixedShortArray_ParsedAsEmptyArray() throws Exception {
    final JBBPFieldStruct parsed =
        JBBPParser.prepare("byte; short[_] array;").parse(new byte[] {1});
    assertEquals(0, parsed.findFieldForNameAndType("array", JBBPFieldArrayShort.class).size());
  }

  @Test
  public void testParse_FixedShortArray_Default() throws Exception {
    final JBBPFieldArrayShort shorts =
        JBBPParser.prepare("short[2];").parse(new byte[] {(byte) 0xF7, 0x43, 0x65, 0})
            .findFieldForType(JBBPFieldArrayShort.class);
    assertEquals(2, shorts.size());
    assertEquals((short) 0xF743, shorts.getAsInt(0));
    assertEquals((short) 0x6500, shorts.getAsInt(1));
  }

  @Test
  public void testParse_FixedShortArray_BigEndian() throws Exception {
    final JBBPFieldArrayShort shorts =
        JBBPParser.prepare(">short[2];").parse(new byte[] {(byte) 0xF7, 0x43, 0x65, 0})
            .findFieldForType(JBBPFieldArrayShort.class);
    assertEquals(2, shorts.size());
    assertEquals((short) 0xF743, shorts.getAsInt(0));
    assertEquals((short) 0x6500, shorts.getAsInt(1));
  }

  @Test
  public void testParse_FixedShortArray_LittleEndian() throws Exception {
    final JBBPFieldArrayShort shorts =
        JBBPParser.prepare("<short[2];").parse(new byte[] {(byte) 0xF7, 0x43, 0x65, 0})
            .findFieldForType(JBBPFieldArrayShort.class);
    assertEquals(2, shorts.size());
    assertEquals((short) 0x43F7, shorts.getAsInt(0));
    assertEquals((short) 0x0065, shorts.getAsInt(1));
  }

  @Test
  public void testParse_NonFixedShortArray_Default() throws Exception {
    final JBBPFieldArrayShort shorts =
        JBBPParser.prepare("short[_];").parse(new byte[] {(byte) 0xF7, 0x43, 0x65, 0})
            .findFieldForType(JBBPFieldArrayShort.class);
    assertEquals(2, shorts.size());
    assertEquals((short) 0xF743, shorts.getAsInt(0));
    assertEquals((short) 0x6500, shorts.getAsInt(1));
  }

  @Test
  public void testParse_NonFixedShortArray_BigEndian() throws Exception {
    final JBBPFieldArrayShort shorts =
        JBBPParser.prepare(">short[_];").parse(new byte[] {(byte) 0xF7, 0x43, 0x65, 0})
            .findFieldForType(JBBPFieldArrayShort.class);
    assertEquals(2, shorts.size());
    assertEquals((short) 0xF743, shorts.getAsInt(0));
    assertEquals((short) 0x6500, shorts.getAsInt(1));
  }

  @Test
  public void testParse_NonFixedShortArray_LittleEndian() throws Exception {
    final JBBPFieldArrayShort shorts =
        JBBPParser.prepare("<short[_];").parse(new byte[] {(byte) 0xF7, 0x43, 0x65, 0})
            .findFieldForType(JBBPFieldArrayShort.class);
    assertEquals(2, shorts.size());
    assertEquals((short) 0x43F7, shorts.getAsInt(0));
    assertEquals((short) 0x0065, shorts.getAsInt(1));
  }

  @Test
  public void testParse_FixedUShortArray_EOFException() throws Exception {
    assertThrows(EOFException.class,
        () -> JBBPParser.prepare("byte; ushort[1];").parse(new byte[] {1}));
  }

  @Test
  public void testParse_NonFixedUShortArray_ParsedAsEmptyArray() throws Exception {
    final JBBPFieldStruct parsed =
        JBBPParser.prepare("byte; ushort[_] array;").parse(new byte[] {1});
    assertEquals(0, parsed.findFieldForNameAndType("array", JBBPFieldArrayUShort.class).size());
  }

  @Test
  public void testParse_FixedUShortArray_Default() throws Exception {
    final JBBPFieldArrayUShort shorts =
        JBBPParser.prepare("ushort[2];").parse(new byte[] {(byte) 0xF7, 0x43, 0x65, 0})
            .findFieldForType(JBBPFieldArrayUShort.class);
    assertEquals(2, shorts.size());
    assertEquals(0xF743, shorts.getAsInt(0));
    assertEquals(0x6500, shorts.getAsInt(1));
  }

  @Test
  public void testParse_FixedUShortArray_BigEndian() throws Exception {
    final JBBPFieldArrayUShort shorts =
        JBBPParser.prepare(">ushort[2];").parse(new byte[] {(byte) 0xF7, 0x43, 0x65, 0})
            .findFieldForType(JBBPFieldArrayUShort.class);
    assertEquals(2, shorts.size());
    assertEquals(0xF743, shorts.getAsInt(0));
    assertEquals(0x6500, shorts.getAsInt(1));
  }

  @Test
  public void testParse_FixedUShortArray_LittleEndian() throws Exception {
    final JBBPFieldArrayUShort shorts =
        JBBPParser.prepare("<ushort[2];").parse(new byte[] {(byte) 0xF7, 0x43, 0x65, 0})
            .findFieldForType(JBBPFieldArrayUShort.class);
    assertEquals(2, shorts.size());
    assertEquals(0x43F7, shorts.getAsInt(0));
    assertEquals(0x0065, shorts.getAsInt(1));
  }

  @Test
  public void testParse_NonFixedUShortArray_Default() throws Exception {
    final JBBPFieldArrayUShort shorts =
        JBBPParser.prepare("ushort[_];").parse(new byte[] {(byte) 0xF7, 0x43, 0x65, 0})
            .findFieldForType(JBBPFieldArrayUShort.class);
    assertEquals(2, shorts.size());
    assertEquals(0xF743, shorts.getAsInt(0));
    assertEquals(0x6500, shorts.getAsInt(1));
  }

  @Test
  public void testParse_NonFixedUShortArray_BigEndian() throws Exception {
    final JBBPFieldArrayUShort shorts =
        JBBPParser.prepare(">ushort[_];").parse(new byte[] {(byte) 0xF7, 0x43, 0x65, 0})
            .findFieldForType(JBBPFieldArrayUShort.class);
    assertEquals(2, shorts.size());
    assertEquals(0xF743, shorts.getAsInt(0));
    assertEquals(0x6500, shorts.getAsInt(1));
  }

  @Test
  public void testParse_NonFixedUShortArray_LittleEndian() throws Exception {
    final JBBPFieldArrayUShort shorts =
        JBBPParser.prepare("<ushort[_];").parse(new byte[] {(byte) 0xF7, 0x43, 0x65, 0})
            .findFieldForType(JBBPFieldArrayUShort.class);
    assertEquals(2, shorts.size());
    assertEquals(0x43F7, shorts.getAsInt(0));
    assertEquals(0x0065, shorts.getAsInt(1));
  }

  @Test
  public void testParse_FixedIntArray_EOFException() throws Exception {
    assertThrows(EOFException.class,
        () -> JBBPParser.prepare("byte; int[1];").parse(new byte[] {1}));
  }

  @Test
  public void testParse_NonFixedIntArray_ParsedAsEmptyArray() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte; int[_] array;").parse(new byte[] {1});
    assertEquals(0, parsed.findFieldForNameAndType("array", JBBPFieldArrayInt.class).size());
  }

  @Test
  public void testParse_FixedIntArray_Default() throws Exception {
    final JBBPFieldArrayInt ints = JBBPParser.prepare("int[2];")
        .parse(new byte[] {(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0})
        .findFieldForType(JBBPFieldArrayInt.class);
    assertEquals(2, ints.size());
    assertEquals(0xF7436510, ints.getAsInt(0));
    assertEquals(0x352367A0, ints.getAsInt(1));
  }

  @Test
  public void testParse_FixedIntArray_BigEndian() throws Exception {
    final JBBPFieldArrayInt ints = JBBPParser.prepare(">int[2];")
        .parse(new byte[] {(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0})
        .findFieldForType(JBBPFieldArrayInt.class);
    assertEquals(2, ints.size());
    assertEquals(0xF7436510, ints.getAsInt(0));
    assertEquals(0x352367A0, ints.getAsInt(1));
  }

  @Test
  public void testParse_FixedIntArray_LittleEndian() throws Exception {
    final JBBPFieldArrayInt ints = JBBPParser.prepare("<int[2];")
        .parse(new byte[] {(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0})
        .findFieldForType(JBBPFieldArrayInt.class);
    assertEquals(2, ints.size());
    assertEquals(0x106543F7, ints.getAsInt(0));
    assertEquals(0xA0672335, ints.getAsInt(1));
  }

  @Test
  public void testParse_NonFixedIntArray_Default() throws Exception {
    final JBBPFieldArrayInt ints = JBBPParser.prepare("int[_];")
        .parse(new byte[] {(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0})
        .findFieldForType(JBBPFieldArrayInt.class);
    assertEquals(2, ints.size());
    assertEquals(0xF7436510, ints.getAsInt(0));
    assertEquals(0x352367A0, ints.getAsInt(1));
  }

  @Test
  public void testParse_NonFixedIntArray_BigEndian() throws Exception {
    final JBBPFieldArrayInt ints = JBBPParser.prepare(">int[_];")
        .parse(new byte[] {(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0})
        .findFieldForType(JBBPFieldArrayInt.class);
    assertEquals(2, ints.size());
    assertEquals(0xF7436510, ints.getAsInt(0));
    assertEquals(0x352367A0, ints.getAsInt(1));
  }

  @Test
  public void testParse_NonFixedIntArray_LittleEndian() throws Exception {
    final JBBPFieldArrayInt ints = JBBPParser.prepare("<int[_];")
        .parse(new byte[] {(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0})
        .findFieldForType(JBBPFieldArrayInt.class);
    assertEquals(2, ints.size());
    assertEquals(0x106543F7, ints.getAsInt(0));
    assertEquals(0xA0672335, ints.getAsInt(1));
  }

  @Test
  public void testParse_FixedFloatArray_EOFException() throws Exception {
    assertThrows(EOFException.class,
        () -> JBBPParser.prepare("byte; floatj[1];").parse(new byte[] {1}));
  }

  @Test
  public void testParse_NonFixedFloatArray_ParsedAsEmptyArray() throws Exception {
    final JBBPFieldStruct parsed =
        JBBPParser.prepare("byte; floatj[_] array;").parse(new byte[] {1});
    assertEquals(0, parsed.findFieldForNameAndType("array", JBBPFieldArrayFloat.class).size());
  }

  @Test
  public void testParse_FixedFloatArray_Default() throws Exception {
    final JBBPFieldArrayFloat ints = JBBPParser.prepare("floatj[2];")
        .parse(new byte[] {(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0})
        .findFieldForType(JBBPFieldArrayFloat.class);
    assertEquals(2, ints.size());
    assertEquals(-3.963077E33f, ints.getAsFloat(0), TestUtils.FLOAT_DELTA);
    assertEquals(6.0873026E-7f, ints.getAsFloat(1), TestUtils.FLOAT_DELTA);
  }

  @Test
  public void testParse_FixedFloatArray_BigEndian() throws Exception {
    final JBBPFieldArrayFloat ints = JBBPParser.prepare(">floatj[2];")
        .parse(new byte[] {(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0})
        .findFieldForType(JBBPFieldArrayFloat.class);
    assertEquals(2, ints.size());
    assertEquals(-3.963077E33f, ints.getAsFloat(0), TestUtils.FLOAT_DELTA);
    assertEquals(6.0873026E-7f, ints.getAsFloat(1), TestUtils.FLOAT_DELTA);
  }

  @Test
  public void testParse_FixedFloatArray_LittleEndian() throws Exception {
    final JBBPFieldArrayFloat ints = JBBPParser.prepare("<floatj[2];")
        .parse(new byte[] {(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0})
        .findFieldForType(JBBPFieldArrayFloat.class);
    assertEquals(2, ints.size());
    assertEquals(4.5214645E-29f, ints.getAsFloat(0), TestUtils.FLOAT_DELTA);
    assertEquals(-1.957811E-19f, ints.getAsFloat(1), TestUtils.FLOAT_DELTA);
  }

  @Test
  public void testParse_NonFixedFloatArray_Default() throws Exception {
    final JBBPFieldArrayFloat ints = JBBPParser.prepare("floatj[_];")
        .parse(new byte[] {(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0})
        .findFieldForType(JBBPFieldArrayFloat.class);
    assertEquals(2, ints.size());
    assertEquals(-3.963077E33f, ints.getAsFloat(0), TestUtils.FLOAT_DELTA);
    assertEquals(6.0873026E-7f, ints.getAsFloat(1), TestUtils.FLOAT_DELTA);
  }

  @Test
  public void testParse_NonFixedFloatArray_BigEndian() throws Exception {
    final JBBPFieldArrayFloat ints = JBBPParser.prepare(">floatj[_];")
        .parse(new byte[] {(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0})
        .findFieldForType(JBBPFieldArrayFloat.class);
    assertEquals(2, ints.size());
    assertEquals(-3.963077E33f, ints.getAsFloat(0), TestUtils.FLOAT_DELTA);
    assertEquals(6.0873026E-7f, ints.getAsFloat(1), TestUtils.FLOAT_DELTA);
  }

  @Test
  public void testParse_NonFixedFloatArray_LittleEndian() throws Exception {
    final JBBPFieldArrayFloat ints = JBBPParser.prepare("<floatj[_];")
        .parse(new byte[] {(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0})
        .findFieldForType(JBBPFieldArrayFloat.class);
    assertEquals(2, ints.size());
    assertEquals(4.5214645E-29f, ints.getAsFloat(0), TestUtils.FLOAT_DELTA);
    assertEquals(-1.957811E-19f, ints.getAsFloat(1), TestUtils.FLOAT_DELTA);
  }

  @Test
  public void testParse_FixedLongArray_EOFException() throws Exception {
    assertThrows(EOFException.class,
        () -> JBBPParser.prepare("byte; long[1];").parse(new byte[] {1}));
  }

  @Test
  public void testParse_NonFixedLongArray_ParsedAsEmptyArray() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte; long[_] array;").parse(new byte[] {1});
    assertEquals(0, parsed.findFieldForNameAndType("array", JBBPFieldArrayLong.class).size());
  }

  @Test
  public void testParse_FixedLongArray_Default() throws Exception {
    final JBBPFieldArrayLong longs = JBBPParser.prepare("long[2];").parse(
        new byte[] {(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0, 0x32, 0x33, 0x61,
            (byte) 0xCA, (byte) 0xBE, 0x22, 0x12, 0x30}).findFieldForType(JBBPFieldArrayLong.class);
    assertEquals(2, longs.size());
    assertEquals(0xF7436510352367A0L, longs.getAsLong(0));
    assertEquals(0x323361CABE221230L, longs.getAsLong(1));
  }

  @Test
  public void testParse_FixedLongArray_BigEndian() throws Exception {
    final JBBPFieldArrayLong longs = JBBPParser.prepare(">long[2];").parse(
        new byte[] {(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0, 0x32, 0x33, 0x61,
            (byte) 0xCA, (byte) 0xBE, 0x22, 0x12, 0x30}).findFieldForType(JBBPFieldArrayLong.class);
    assertEquals(2, longs.size());
    assertEquals(0xF7436510352367A0L, longs.getAsLong(0));
    assertEquals(0x323361CABE221230L, longs.getAsLong(1));
  }

  @Test
  public void testParse_FixedLongArray_LittleEndian() throws Exception {
    final JBBPFieldArrayLong longs = JBBPParser.prepare("<long[2];").parse(
        new byte[] {(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0, 0x32, 0x33, 0x61,
            (byte) 0xCA, (byte) 0xBE, 0x22, 0x12, 0x30}).findFieldForType(JBBPFieldArrayLong.class);
    assertEquals(2, longs.size());
    assertEquals(0xA0672335106543F7L, longs.getAsLong(0));
    assertEquals(0x301222BECA613332L, longs.getAsLong(1));
  }

  @Test
  public void testParse_NonFixedLongArray_Default() throws Exception {
    final JBBPFieldArrayLong longs = JBBPParser.prepare("long[_];").parse(
        new byte[] {(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0, 0x32, 0x33, 0x61,
            (byte) 0xCA, (byte) 0xBE, 0x22, 0x12, 0x30}).findFieldForType(JBBPFieldArrayLong.class);
    assertEquals(2, longs.size());
    assertEquals(0xF7436510352367A0L, longs.getAsLong(0));
    assertEquals(0x323361CABE221230L, longs.getAsLong(1));
  }

  @Test
  public void testParse_NonFixedLongArray_BigEndian() throws Exception {
    final JBBPFieldArrayLong longs = JBBPParser.prepare(">long[_];").parse(
        new byte[] {(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0, 0x32, 0x33, 0x61,
            (byte) 0xCA, (byte) 0xBE, 0x22, 0x12, 0x30}).findFieldForType(JBBPFieldArrayLong.class);
    assertEquals(2, longs.size());
    assertEquals(0xF7436510352367A0L, longs.getAsLong(0));
    assertEquals(0x323361CABE221230L, longs.getAsLong(1));
  }

  @Test
  public void testParse_NonFixedLongArray_LittleEndian() throws Exception {
    final JBBPFieldArrayLong longs = JBBPParser.prepare("<long[_];").parse(
        new byte[] {(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0, 0x32, 0x33, 0x61,
            (byte) 0xCA, (byte) 0xBE, 0x22, 0x12, 0x30}).findFieldForType(JBBPFieldArrayLong.class);
    assertEquals(2, longs.size());
    assertEquals(0xA0672335106543F7L, longs.getAsLong(0));
    assertEquals(0x301222BECA613332L, longs.getAsLong(1));
  }

  @Test
  public void testParse_FixedDoubleArray_EOFException() throws Exception {
    assertThrows(EOFException.class,
        () -> JBBPParser.prepare("byte; doublej[1];").parse(new byte[] {1}));
  }

  @Test
  public void testParse_NonFixedDoubleArray_ParsedAsEmptyArray() throws Exception {
    final JBBPFieldStruct parsed =
        JBBPParser.prepare("byte; doublej[_] array;").parse(new byte[] {1});
    assertEquals(0, parsed.findFieldForNameAndType("array", JBBPFieldArrayDouble.class).size());
  }

  @Test
  public void testParse_FixedDoubleArray_Default() throws Exception {
    final JBBPFieldArrayDouble longs = JBBPParser.prepare("doublej[2];").parse(
        new byte[] {(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0, 0x32, 0x33, 0x61,
            (byte) 0xCA, (byte) 0xBE, 0x22, 0x12, 0x30})
        .findFieldForType(JBBPFieldArrayDouble.class);
    assertEquals(2, longs.size());
    assertEquals(-3.126878492655484E266d, longs.getAsDouble(0), TestUtils.FLOAT_DELTA);
    assertEquals(7.189183308668011E-67d, longs.getAsDouble(1), TestUtils.FLOAT_DELTA);
  }

  @Test
  public void testParse_FixedDoubleArray_BigEndian() throws Exception {
    final JBBPFieldArrayDouble longs = JBBPParser.prepare(">doublej[2];").parse(
        new byte[] {(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0, 0x32, 0x33, 0x61,
            (byte) 0xCA, (byte) 0xBE, 0x22, 0x12, 0x30})
        .findFieldForType(JBBPFieldArrayDouble.class);
    assertEquals(2, longs.size());
    assertEquals(-3.126878492655484E266d, longs.getAsDouble(0), TestUtils.FLOAT_DELTA);
    assertEquals(7.189183308668011E-67d, longs.getAsDouble(1), TestUtils.FLOAT_DELTA);
  }

  @Test
  public void testParse_FixedDoubleArray_LittleEndian() throws Exception {
    final JBBPFieldArrayDouble longs = JBBPParser.prepare("<doublej[2];").parse(
        new byte[] {(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0, 0x32, 0x33, 0x61,
            (byte) 0xCA, (byte) 0xBE, 0x22, 0x12, 0x30})
        .findFieldForType(JBBPFieldArrayDouble.class);
    assertEquals(2, longs.size());
    assertEquals(-1.3805405664501578E-152d, longs.getAsDouble(0), TestUtils.FLOAT_DELTA);
    assertEquals(3.915579175603706E-77d, longs.getAsDouble(1), TestUtils.FLOAT_DELTA);
  }

  @Test
  public void testParse_NonFixedDoubleArray_Default() throws Exception {
    final JBBPFieldArrayDouble longs = JBBPParser.prepare("doublej[_];").parse(
        new byte[] {(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0, 0x32, 0x33, 0x61,
            (byte) 0xCA, (byte) 0xBE, 0x22, 0x12, 0x30})
        .findFieldForType(JBBPFieldArrayDouble.class);
    assertEquals(2, longs.size());
    assertEquals(-3.126878492655484E266d, longs.getAsDouble(0), TestUtils.FLOAT_DELTA);
    assertEquals(7.189183308668011E-67d, longs.getAsDouble(1), TestUtils.FLOAT_DELTA);
  }

  @Test
  public void testParse_NonFixedDoubleArray_BigEndian() throws Exception {
    final JBBPFieldArrayDouble longs = JBBPParser.prepare(">doublej[_];").parse(
        new byte[] {(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0, 0x32, 0x33, 0x61,
            (byte) 0xCA, (byte) 0xBE, 0x22, 0x12, 0x30})
        .findFieldForType(JBBPFieldArrayDouble.class);
    assertEquals(2, longs.size());
    assertEquals(-3.126878492655484E266d, longs.getAsDouble(0), TestUtils.FLOAT_DELTA);
    assertEquals(7.189183308668011E-67d, longs.getAsDouble(1), TestUtils.FLOAT_DELTA);
  }

  @Test
  public void testParse_NonFixedDoubleArray_LittleEndian() throws Exception {
    final JBBPFieldArrayDouble longs = JBBPParser.prepare("<doublej[_];").parse(
        new byte[] {(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0, 0x32, 0x33, 0x61,
            (byte) 0xCA, (byte) 0xBE, 0x22, 0x12, 0x30})
        .findFieldForType(JBBPFieldArrayDouble.class);
    assertEquals(2, longs.size());
    assertEquals(-1.3805405664501578E-152, longs.getAsDouble(0), TestUtils.FLOAT_DELTA);
    assertEquals(3.915579175603706E-77, longs.getAsDouble(1), TestUtils.FLOAT_DELTA);
  }

  @Test
  public void testParse_VarBitArray() throws Exception {
    final JBBPFieldArrayBit array =
        JBBPParser.prepare("byte size; bit:4[size];").parse(new byte[] {8, 0x12, 0x34, 0x56, 0x78})
            .findFieldForType(JBBPFieldArrayBit.class);
    assertEquals(8, array.size());
    assertEquals(2, array.getAsInt(0));
    assertEquals(1, array.getAsInt(1));
    assertEquals(4, array.getAsInt(2));
    assertEquals(3, array.getAsInt(3));
    assertEquals(6, array.getAsInt(4));
    assertEquals(5, array.getAsInt(5));
    assertEquals(8, array.getAsInt(6));
    assertEquals(7, array.getAsInt(7));
  }

  @Test
  public void testParse_VarBitArrayWhenSizeInStruct() throws Exception {
    final JBBPFieldArrayBit array = JBBPParser.prepare("str {byte size;} bit:4[str.size];")
        .parse(new byte[] {8, 0x12, 0x34, 0x56, 0x78}).findFieldForType(JBBPFieldArrayBit.class);
    assertEquals(8, array.size());
    assertEquals(2, array.getAsInt(0));
    assertEquals(1, array.getAsInt(1));
    assertEquals(4, array.getAsInt(2));
    assertEquals(3, array.getAsInt(3));
    assertEquals(6, array.getAsInt(4));
    assertEquals(5, array.getAsInt(5));
    assertEquals(8, array.getAsInt(6));
    assertEquals(7, array.getAsInt(7));
  }

  @Test
  public void testParse_WholeBitStream() throws Exception {
    final JBBPFieldArrayBit array =
        JBBPParser.prepare("bit:4[_];").parse(new byte[] {0x12, 0x34, 0x56, 0x78})
            .findFieldForType(JBBPFieldArrayBit.class);
    assertEquals(2, array.getAsInt(0));
    assertEquals(1, array.getAsInt(1));
    assertEquals(4, array.getAsInt(2));
    assertEquals(3, array.getAsInt(3));
    assertEquals(6, array.getAsInt(4));
    assertEquals(5, array.getAsInt(5));
    assertEquals(8, array.getAsInt(6));
    assertEquals(7, array.getAsInt(7));
  }

  @Test
  public void testParse_Bit_ArrayForCalculatedLength() throws Exception {
    final JBBPFieldArrayBit array = JBBPParser.prepare("ubyte len; bit:4[2*len*6/2-4];")
        .parse(new byte[] {2, 0x12, 0x34, 0x56, 0x78}).findFieldForType(JBBPFieldArrayBit.class);

    assertEquals(2, array.getAsInt(0));
    assertEquals(1, array.getAsInt(1));
    assertEquals(4, array.getAsInt(2));
    assertEquals(3, array.getAsInt(3));
    assertEquals(6, array.getAsInt(4));
    assertEquals(5, array.getAsInt(5));
    assertEquals(8, array.getAsInt(6));
    assertEquals(7, array.getAsInt(7));
  }

  @Test
  public void testParse_VarByteArray() throws Exception {
    final JBBPFieldArrayByte array =
        JBBPParser.prepare("byte size; byte[size];").parse(new byte[] {4, 1, 2, 3, 4})
            .findFieldForType(JBBPFieldArrayByte.class);
    assertEquals(4, array.size());
    assertEquals(1, array.getAsInt(0));
    assertEquals(2, array.getAsInt(1));
    assertEquals(3, array.getAsInt(2));
    assertEquals(4, array.getAsInt(3));
  }

  @Test
  public void testParse_VarByteArrayWhenSizeInStruct() throws Exception {
    final JBBPFieldArrayByte array =
        JBBPParser.prepare("str {byte size;} byte[str.size];").parse(new byte[] {4, 1, 2, 3, 4})
            .findFieldForType(JBBPFieldArrayByte.class);
    assertEquals(4, array.size());
    assertEquals(1, array.getAsInt(0));
    assertEquals(2, array.getAsInt(1));
    assertEquals(3, array.getAsInt(2));
    assertEquals(4, array.getAsInt(3));
  }

  @Test
  public void testParse_WholeByteStream() throws Exception {
    final JBBPFieldArrayByte array = JBBPParser.prepare("byte[_];").parse(new byte[] {1, 2, 3, 4})
        .findFieldForType(JBBPFieldArrayByte.class);
    assertEquals(1, array.getAsInt(0));
    assertEquals(2, array.getAsInt(1));
    assertEquals(3, array.getAsInt(2));
    assertEquals(4, array.getAsInt(3));
  }

  @Test
  public void testParse_WholeByteStream_Empty() throws Exception {
    assertEquals(0,
        JBBPParser.prepare("byte[_];").parse(new byte[0]).findFieldForType(JBBPFieldArrayByte.class)
            .size());
  }

  @Test
  public void testParse_Byte_ArrayForCalculatedLength() throws Exception {
    final JBBPFieldArrayByte array =
        JBBPParser.prepare("ubyte len; byte[len*6/2-2];").parse(new byte[] {2, 1, 2, 3, 4})
            .findFieldForType(JBBPFieldArrayByte.class);

    assertEquals(1, array.getAsInt(0));
    assertEquals(2, array.getAsInt(1));
    assertEquals(3, array.getAsInt(2));
    assertEquals(4, array.getAsInt(3));
  }

  @Test
  public void testParse_Boolean_ArrayForCalculatedLength() throws Exception {
    final JBBPFieldArrayBoolean array =
        JBBPParser.prepare("ubyte len; bool[len*6/2-2];").parse(new byte[] {2, 1, 2, 0, 4})
            .findFieldForType(JBBPFieldArrayBoolean.class);

    assertTrue(array.getAsBool(0));
    assertTrue(array.getAsBool(1));
    assertFalse(array.getAsBool(2));
    assertTrue(array.getAsBool(3));
  }

  @Test
  public void testParse_NegativeCalculatedArrayLength() throws Exception {
    assertThrows(JBBPParsingException.class,
        () -> JBBPParser.prepare("ubyte len; byte[len-4];").parse(new byte[] {2, 1, 2, 3, 4}));
  }

  @Test
  public void testParse_NegativeArrayLength() throws Exception {
    assertThrows(JBBPCompilationException.class,
        () -> JBBPParser.prepare("ubyte len; byte[-2];").parse(new byte[] {2, 1, 2, 3, 4}));
  }

  @Test
  public void testParse_ErrorForLessDataThanExpected() throws Exception {
    assertThrows(EOFException.class,
        () -> JBBPParser.prepare("ubyte len; byte[5];").parse(new byte[] {2, 1, 2, 3, 4}));
  }

  @Test
  public void testParse_WholeStructStream() throws Exception {
    final JBBPFieldArrayStruct array =
        JBBPParser.prepare("struct [_] {byte;}").parse(new byte[] {1, 2, 3, 4})
            .findFieldForType(JBBPFieldArrayStruct.class);
    assertEquals(4, array.size());
    assertEquals(1, array.getElementAt(0).findFieldForType(JBBPFieldByte.class).getAsInt());
    assertEquals(2, array.getElementAt(1).findFieldForType(JBBPFieldByte.class).getAsInt());
    assertEquals(3, array.getElementAt(2).findFieldForType(JBBPFieldByte.class).getAsInt());
    assertEquals(4, array.getElementAt(3).findFieldForType(JBBPFieldByte.class).getAsInt());
  }

  @Test
  public void testParse_BitArray_IgnoredForZeroLength() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte len; bit:4 [len]; ushort;")
        .parse(new byte[] {0x0, 0x01, (byte) 0x02});
    assertEquals(0, parsed.findFieldForType(JBBPFieldArrayBit.class).size());
    assertEquals(0x0102, parsed.findFieldForType(JBBPFieldUShort.class).getAsInt());
  }

  @Test
  public void testParse_BoolArray_IgnoredForZeroLength() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte len; bool [len]; ushort;")
        .parse(new byte[] {0x0, 0x01, (byte) 0x02});
    assertEquals(0, parsed.findFieldForType(JBBPFieldArrayBoolean.class).size());
    assertEquals(0x0102, parsed.findFieldForType(JBBPFieldUShort.class).getAsInt());
  }

  @Test
  public void testParse_ByteArray_IgnoredForZeroLength() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte len; byte [len]; ushort;")
        .parse(new byte[] {0x0, 0x01, (byte) 0x02});
    assertEquals(0, parsed.findFieldForType(JBBPFieldArrayByte.class).size());
    assertEquals(0x0102, parsed.findFieldForType(JBBPFieldUShort.class).getAsInt());
  }

  @Test
  public void testParse_UByteArray_IgnoredForZeroLength() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte len; ubyte [len]; ushort;")
        .parse(new byte[] {0x0, 0x01, (byte) 0x02});
    assertEquals(0, parsed.findFieldForType(JBBPFieldArrayUByte.class).size());
    assertEquals(0x0102, parsed.findFieldForType(JBBPFieldUShort.class).getAsInt());
  }

  @Test
  public void testParse_ShortArray_IgnoredForZeroLength() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte len; short [len]; ushort;")
        .parse(new byte[] {0x0, 0x01, (byte) 0x02});
    assertEquals(0, parsed.findFieldForType(JBBPFieldArrayShort.class).size());
    assertEquals(0x0102, parsed.findFieldForType(JBBPFieldUShort.class).getAsInt());
  }

  @Test
  public void testParse_UShortArray_IgnoredForZeroLength() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte len; ushort [len]; ushort;")
        .parse(new byte[] {0x0, 0x01, (byte) 0x02});
    assertEquals(0, parsed.findFieldForType(JBBPFieldArrayUShort.class).size());
    assertEquals(0x0102, parsed.findFieldForType(JBBPFieldUShort.class).getAsInt());
  }

  @Test
  public void testParse_IntArray_IgnoredForZeroLength() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte len; int [len]; ushort;")
        .parse(new byte[] {0x0, 0x01, (byte) 0x02});
    assertEquals(0, parsed.findFieldForType(JBBPFieldArrayInt.class).size());
    assertEquals(0x0102, parsed.findFieldForType(JBBPFieldUShort.class).getAsInt());
  }

  @Test
  public void testParse_LongArray_IgnoredForZeroLength() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte len; long [len]; ushort;")
        .parse(new byte[] {0x0, 0x01, (byte) 0x02});
    assertEquals(0, parsed.findFieldForType(JBBPFieldArrayLong.class).size());
    assertEquals(0x0102, parsed.findFieldForType(JBBPFieldUShort.class).getAsInt());
  }

  @Test
  public void testParse_StructArray_IgnoredForZeroLength() throws Exception {
    final JBBPFieldStruct parsed =
        JBBPParser.prepare("byte len; sss [len] { byte a; byte b; byte c;}  ushort;")
            .parse(new byte[] {0x0, 0x01, (byte) 0x02});
    assertEquals(0, parsed.findFieldForType(JBBPFieldArrayStruct.class).size());
    assertEquals(0x0102, parsed.findFieldForType(JBBPFieldUShort.class).getAsInt());
  }

  @Test
  public void testParse_StructArray_FixedSize() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("sss [1] { byte a; byte b; byte c;}")
        .parse(new byte[] {0x0, 0x01, (byte) 0x02});
    final JBBPFieldArrayStruct struct =
        parsed.findFieldForPathAndType("sss", JBBPFieldArrayStruct.class);
    assertEquals(1, struct.size());
    final JBBPFieldStruct readStruct = struct.getElementAt(0);
    assertEquals(0, readStruct.findFieldForNameAndType("a", JBBPFieldByte.class).getAsInt());
    assertEquals(1, readStruct.findFieldForNameAndType("b", JBBPFieldByte.class).getAsInt());
    assertEquals(2, readStruct.findFieldForNameAndType("c", JBBPFieldByte.class).getAsInt());
  }

  @Test
  public void testParse_StructArray_WholeStream() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("sss [_] { byte a; byte b; byte c;}")
        .parse(new byte[] {0x0, 0x01, (byte) 0x02});
    final JBBPFieldArrayStruct struct =
        parsed.findFieldForPathAndType("sss", JBBPFieldArrayStruct.class);
    assertEquals(1, struct.size());
    final JBBPFieldStruct readStruct = struct.getElementAt(0);
    assertEquals(0, readStruct.findFieldForNameAndType("a", JBBPFieldByte.class).getAsInt());
    assertEquals(1, readStruct.findFieldForNameAndType("b", JBBPFieldByte.class).getAsInt());
    assertEquals(2, readStruct.findFieldForNameAndType("c", JBBPFieldByte.class).getAsInt());
  }

  @Test
  public void testParse_EmptyStructArray_WholeStream() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("sss [_] { }").parse(new byte[0]);
    assertEquals(0, parsed.findFieldForPathAndType("sss", JBBPFieldArrayStruct.class).size());
  }

  @Test
  public void testParse_EmptyStructArrayInsideStruct_WholeStream() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("sss { sss2[_]{}}").parse(new byte[0]);
    assertEquals(0, parsed.findFieldForPathAndType("sss.sss2", JBBPFieldArrayStruct.class).size());
  }

  @Test
  public void testParse_SkipStructureForZeroItems() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare(
        "byte len; sss [len]{ sss2[10]{ sss3{long;} sss4[45]{ushort; bool [11]; short; bit:4;} byte;}} byte end;")
        .parse(new byte[] {0x00, 0x1F});
    assertEquals(0, parsed.findFieldForPathAndType("len", JBBPFieldByte.class).getAsInt());
    assertEquals(0, parsed.findFieldForPathAndType("sss", JBBPFieldArrayStruct.class).size());
    assertEquals(0x1F, parsed.findFieldForPathAndType("end", JBBPFieldByte.class).getAsInt());
  }

  @Test
  public void testParseWithStreamPositionMacros() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("int start; byte [$$] array; int end;")
        .parse(new byte[] {1, 2, 3, 4, 0x1A, 0x1B, 0x1C, 0x1D, 4, 3, 2, 1});
    assertEquals(0x01020304,
        parsed.findFieldForPathAndType("start", JBBPFieldInt.class).getAsInt());
    assertArrayEquals(new byte[] {0x1A, 0x1B, 0x1C, 0x1D},
        parsed.findFieldForPathAndType("array", JBBPFieldArrayByte.class).getArray());
    assertEquals(0x04030201, parsed.findFieldForPathAndType("end", JBBPFieldInt.class).getAsInt());
  }

  @Test
  public void testParseWithStreamPositionMacrosInExpressions() throws Exception {
    final JBBPFieldStruct parsed =
        JBBPParser.prepare("int start; byte [$$-2] array; byte [$$-4] array2; int end;")
            .parse(new byte[] {1, 2, 3, 4, 0x1A, 0x1B, 0x1C, 0x1D, 4, 3, 2, 1});
    assertEquals(0x01020304,
        parsed.findFieldForPathAndType("start", JBBPFieldInt.class).getAsInt());
    assertArrayEquals(new byte[] {0x1A, 0x1B},
        parsed.findFieldForPathAndType("array", JBBPFieldArrayByte.class).getArray());
    assertArrayEquals(new byte[] {0x1C, 0x1D},
        parsed.findFieldForPathAndType("array2", JBBPFieldArrayByte.class).getArray());
    assertEquals(0x04030201, parsed.findFieldForPathAndType("end", JBBPFieldInt.class).getAsInt());
  }

  @Test
  public void testParseManyFieldsWithTheSameName() throws Exception {
    final JBBPFieldStruct parsed =
        JBBPParser.prepare("byte l; a { byte a; b { byte a; c { byte a;}}} byte [a.b.c.a] aa;")
            .parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
    assertEquals(1, parsed.findFieldForPathAndType("l", JBBPFieldByte.class).getAsInt());
    assertEquals(2, parsed.findFieldForPathAndType("a.a", JBBPFieldByte.class).getAsInt());
    assertEquals(3, parsed.findFieldForPathAndType("a.b.a", JBBPFieldByte.class).getAsInt());
    assertEquals(4, parsed.findFieldForPathAndType("a.b.c.a", JBBPFieldByte.class).getAsInt());
    assertArrayEquals(new byte[] {5, 6, 7, 8},
        parsed.findFieldForPathAndType("aa", JBBPFieldArrayByte.class).getArray());
  }

  @Test
  public void testParseScopeOfVisibilityOfFieldIfTheSameInStructBefore() throws Exception {
    final JBBPFieldStruct parsed =
        JBBPParser.prepare("byte a; b { byte a;} byte [a] aa;").parse(new byte[] {1, 2, 3});
    assertEquals(1, parsed.findFieldForPathAndType("a", JBBPFieldByte.class).getAsInt());
    assertEquals(2, parsed.findFieldForPathAndType("b.a", JBBPFieldByte.class).getAsInt());
    assertArrayEquals(new byte[] {3},
        parsed.findFieldForPathAndType("aa", JBBPFieldArrayByte.class).getArray());
  }

  @Test
  public void testParseScopeOfVisibilityOfFieldInsideStructure() throws Exception {
    final JBBPFieldStruct parsed =
        JBBPParser.prepare("byte a; b { byte a; byte [a] d; } byte [a] aa;")
            .parse(new byte[] {1, 2, 3, 4, 5, 6});
    assertEquals(1, parsed.findFieldForPathAndType("a", JBBPFieldByte.class).getAsInt());
    assertEquals(2, parsed.findFieldForPathAndType("b.a", JBBPFieldByte.class).getAsInt());
    assertArrayEquals(new byte[] {3, 4},
        parsed.findFieldForPathAndType("b.d", JBBPFieldArrayByte.class).getArray());
    assertArrayEquals(new byte[] {5},
        parsed.findFieldForPathAndType("aa", JBBPFieldArrayByte.class).getArray());
  }

  @Test
  public void testParseFixedSizeStructureArray() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("int val1; inner [2] { byte a; byte b;}");

    final JBBPFieldStruct parsed = parser.parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
    assertEquals(0x01020304, parsed.findFieldForPathAndType("val1", JBBPFieldInt.class).getAsInt());

    final JBBPFieldArrayStruct structArray =
        parsed.findFieldForNameAndType("inner", JBBPFieldArrayStruct.class);

    assertEquals(2, structArray.size());
  }

  @Test
  public void testParseWithResetCounter() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("struct[_]{reset$$; byte a; align:3; byte b;}");
    final JBBPFieldStruct parsed =
        parser.parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16});

    final JBBPFieldArrayStruct structArray =
        parsed.findFieldForNameAndType("struct", JBBPFieldArrayStruct.class);
    final byte[] etalon = new byte[] {1, 4, 5, 8, 9, 12, 13, 16};
    assertEquals(4, structArray.size());

    int i = 0;
    for (final JBBPFieldStruct s : structArray) {
      final JBBPFieldByte a = s.findFieldForNameAndType("a", JBBPFieldByte.class);
      final JBBPFieldByte b = s.findFieldForNameAndType("b", JBBPFieldByte.class);

      assertEquals(etalon[i++] & 0xFF, a.getAsInt());
      assertEquals(etalon[i++] & 0xFF, b.getAsInt());
    }
  }

  @Test
  public void testParseResetCounterWithCachedBits() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("bit:4 a; reset$$; byte b;");
    final JBBPFieldStruct parsed = parser.parse(new byte[] {0x3, 0x1F});
    assertEquals(3, parsed.findFieldForNameAndType("a", JBBPFieldBit.class).getAsInt());
    assertEquals(0x1F, parsed.findFieldForNameAndType("b", JBBPFieldByte.class).getAsInt());
    assertEquals(1, parser.getFinalStreamByteCounter());
  }

  @Test
  public void testParseArrayWithZeroLengthForResetCounter() throws Exception {
    final JBBPParser parser =
        JBBPParser.prepare("byte; byte [$$] a; reset$$; byte[$$] b; byte [2] c;");
    final JBBPFieldStruct parsed = parser.parse(new byte[] {1, 2, 3, 4});
    final JBBPFieldArrayByte a = parsed.findFieldForNameAndType("a", JBBPFieldArrayByte.class);
    final JBBPFieldArrayByte b = parsed.findFieldForNameAndType("b", JBBPFieldArrayByte.class);
    final JBBPFieldArrayByte c = parsed.findFieldForNameAndType("c", JBBPFieldArrayByte.class);
    assertArrayEquals(new byte[] {2}, a.getArray());
    assertArrayEquals(new byte[0], b.getArray());
    assertArrayEquals(new byte[] {3, 4}, c.getArray());
  }

  @Test
  public void testGetFinalStreamByteCounter_Single_NoError() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("byte [_];");
    assertEquals(0L, parser.getFinalStreamByteCounter());
    parser.parse(new byte[2345]);
    assertEquals(2345L, parser.getFinalStreamByteCounter());
  }

  @Test
  public void testGetFinalStreamByteCounter_SequentlyFromTheSameStream_WithEOFAtTheEnd()
      throws Exception {
    final JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(
        new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}));
    final JBBPParser parser = JBBPParser.prepare("byte [5];");
    assertEquals(0L, parser.getFinalStreamByteCounter());
    parser.parse(stream);
    assertEquals(5, parser.getFinalStreamByteCounter());
    parser.parse(stream);
    assertEquals(10, parser.getFinalStreamByteCounter());
    parser.parse(stream);
    assertEquals(15, parser.getFinalStreamByteCounter());
    try {
      parser.parse(stream);
      fail("Must throw EOF");
    } catch (EOFException ex) {
      assertEquals(16, parser.getFinalStreamByteCounter());
    }
  }

  @Test
  public void testParse_ErrorForNotAllReadFields() throws Exception {
    final JBBPBitInputStream stream =
        new JBBPBitInputStream(new ByteArrayInputStream(new byte[] {1, 2, 3, 4}));
    final JBBPParser parser = JBBPParser.prepare("int a; int b;");
    assertThrows(JBBPParsingException.class, () -> parser.parse(stream));
  }

  @Test
  public void testParse_NegativeExpressonResult_OneFieldAsExpression_FlagOff() throws Exception {
    final JBBPBitInputStream stream =
        new JBBPBitInputStream(new ByteArrayInputStream(new byte[] {(byte) 0xEF, 1, 2, 3}));
    final JBBPParser parser = JBBPParser.prepare("byte len; byte [len] arr;");

    assertThrows(JBBPParsingException.class, () -> parser.parse(stream));
  }

  @Test
  public void testParse_NegativeExpressonResult_ExpressionWithNegativeResult_FlagOff()
      throws Exception {
    final JBBPBitInputStream stream =
        new JBBPBitInputStream(new ByteArrayInputStream(new byte[] {2, 1, 2, 3}));
    final JBBPParser parser = JBBPParser.prepare("byte len; byte [len - 8] arr;");

    assertThrows(JBBPParsingException.class, () -> parser.parse(stream));
  }

  @Test
  public void testParse_NegativeExpressonResult_OneFieldAsExpression_FlagOn() throws Exception {
    final JBBPBitInputStream stream =
        new JBBPBitInputStream(new ByteArrayInputStream(new byte[] {(byte) 0xEF, 1, 2, 3}));
    final JBBPParser parser = JBBPParser
        .prepare("byte len; byte [len] arr;", JBBPParser.FLAG_NEGATIVE_EXPRESSION_RESULT_AS_ZERO);
    final JBBPFieldStruct result = parser.parse(stream);
    assertEquals((byte) 0xEF, result.findFieldForType(JBBPFieldByte.class).getAsInt());
    assertEquals(0, result.findFieldForType(JBBPFieldArrayByte.class).getArray().length);
  }

  @Test
  public void testParse_NegativeExpressonResult_ExpressionWithNegativResult_FlagOn()
      throws Exception {
    final JBBPBitInputStream stream =
        new JBBPBitInputStream(new ByteArrayInputStream(new byte[] {4, 1, 2, 3}));
    final JBBPParser parser = JBBPParser
        .prepare("byte len; byte [len-8] arr;", JBBPParser.FLAG_NEGATIVE_EXPRESSION_RESULT_AS_ZERO);
    final JBBPFieldStruct result = parser.parse(stream);
    assertEquals(4, result.findFieldForType(JBBPFieldByte.class).getAsInt());
    assertEquals(0, result.findFieldForType(JBBPFieldArrayByte.class).getArray().length);
  }

  @Test
  public void testParse_NoErrorForIgnoreRemainingFieldsFlag() throws Exception {
    final JBBPBitInputStream stream =
        new JBBPBitInputStream(new ByteArrayInputStream(new byte[] {1, 2, 3, 4}));
    final JBBPParser parser =
        JBBPParser.prepare("int a; int b;", JBBPParser.FLAG_SKIP_REMAINING_FIELDS_IF_EOF);
    final JBBPFieldStruct result = parser.parse(stream);
    assertEquals(1, result.getArray().length);
    assertEquals("a", result.getArray()[0].getFieldName());
    assertEquals(0x01020304, ((JBBPFieldInt) result.findFieldForName("a")).getAsInt());
  }

  @Test
  public void testConvertToSrc_Java_NamedPackage() {
    final JBBPParser parser = JBBPParser.prepare("byte a;");

    final List<ResultSrcItem> src =
        parser.convertToSrc(TargetSources.JAVA, "some.package.SomeClass");

    assertEquals(1, src.size());
    assertEquals("byte a;", src.get(0).getMetadata().getProperty("script"));
    assertTrue(src.get(0).getResult().get("some/package/SomeClass.java").length() > 128);
  }

  @Test
  public void testConvertToSrc_Java_DefaultPackage() {
    final JBBPParser parser = JBBPParser.prepare("byte a;");

    final List<ResultSrcItem> src = parser.convertToSrc(TargetSources.JAVA, "SomeClass");

    assertEquals(1, src.size());
    assertEquals("byte a;", src.get(0).getMetadata().getProperty("script"));
    assertTrue(src.get(0).getResult().get("SomeClass.java").length() > 128);
  }

  @Test
  public void testUintUseInExpression() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("uint length; uint[(length * 2) >> 1] array;");
    final JBBPFieldStruct struct = parser.parse(
        new byte[] {0, 0, 0, 2, (byte) 0xFF, (byte) 0xF0, (byte) 0xE0, (byte) 0x12, 0x01, 0x02,
            0x03, 0x04});
    final JBBPFieldUInt length = struct.findFieldForPathAndType("length", JBBPFieldUInt.class);
    final JBBPFieldArrayUInt array =
        struct.findFieldForPathAndType("array", JBBPFieldArrayUInt.class);

    assertEquals(2, length.getAsInt());
    assertEquals(2, array.size());
    assertThrows(IllegalStateException.class, () -> array.getElementAt(0).getAsInt());
    assertEquals(0xFFF0E012L, array.getElementAt(0).getAsLong());
    assertEquals(0x01020304, array.getElementAt(1).getAsInt());
  }

}
