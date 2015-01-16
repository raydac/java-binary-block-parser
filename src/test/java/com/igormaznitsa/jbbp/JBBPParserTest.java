/* 
 * Copyright 2014 Igor Maznitsa (http://www.igormaznitsa.com).
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

import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.exceptions.JBBPCompilationException;
import com.igormaznitsa.jbbp.exceptions.JBBPParsingException;
import com.igormaznitsa.jbbp.exceptions.JBBPTooManyFieldsFoundException;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.model.*;
import com.igormaznitsa.jbbp.utils.JBBPIntCounter;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import static org.junit.Assert.*;
import org.junit.Test;

public class JBBPParserTest {

  @Test
  public void testErrorDuringReadingOfNamedField() throws Exception {
    try {
      JBBPParser.prepare("short helloworld;").parse(new byte[]{1});
      fail("Must throw JBBPParsingException");
    }
    catch (JBBPParsingException ex) {
      assertTrue(ex.getMessage().contains("helloworld"));
    }
  }

  @Test
  public void testErrorDuringReadingOfNonNamedField() throws Exception {
    try {
      JBBPParser.prepare("short;").parse(new byte[]{1});
      fail("Must throw EOFException");
    }
    catch (EOFException ex) {
      assertNull(ex.getMessage());
    }
  }

  @Test(expected = JBBPCompilationException.class)
  public void testFieldNameCaseInsensetive_ExceptionForDuplicationOfFieldNames() throws Exception {
    JBBPParser.prepare("bool Field1; byte field1;");
  }

  @Test
  public void testFieldNameCaseInsensetive() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("bool FiElD1;").parse(new byte[]{1});
    assertTrue(parsed.nameExists("fiELD1"));
    assertTrue(parsed.pathExists("fiELD1"));
    assertNotNull(parsed.findFieldForName("fiELD1"));
    assertNotNull(parsed.findFieldForNameAndType("fiELD1", JBBPFieldBoolean.class));
    assertNotNull(parsed.findFieldForPathAndType("fiELD1", JBBPFieldBoolean.class));
  }

  @Test(expected = EOFException.class)
  public void testParse_Bool_ErrorForEOF() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("bool;");
    parser.parse(new byte[0]);
  }

  @Test
  public void testParse_SingleDefaultNonamedBool_Default() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("bool;");
    JBBPFieldStruct result = parser.parse(new byte[]{(byte) 1});
    assertTrue(result.findFieldForType(JBBPFieldBoolean.class).getAsBool());
    result = parser.parse(new byte[]{(byte) 0});
    assertFalse(result.findFieldForType(JBBPFieldBoolean.class).getAsBool());
  }

  @Test
  public void testParse_SingleDefaultNonamedBool_BigEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare(">bool;");
    JBBPFieldStruct result = parser.parse(new byte[]{(byte) 1});
    assertTrue(result.findFieldForType(JBBPFieldBoolean.class).getAsBool());
    result = parser.parse(new byte[]{(byte) 0});
    assertFalse(result.findFieldForType(JBBPFieldBoolean.class).getAsBool());
  }

  @Test
  public void testParse_SingleDefaultNonamedBool_LittleEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("<bool;");
    JBBPFieldStruct result = parser.parse(new byte[]{(byte) 1});
    assertTrue(result.findFieldForType(JBBPFieldBoolean.class).getAsBool());
    result = parser.parse(new byte[]{(byte) 0});
    assertFalse(result.findFieldForType(JBBPFieldBoolean.class).getAsBool());
  }

  @Test(expected = EOFException.class)
  public void testParse_Byte_ErrorForEOF() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("byte;");
    parser.parse(new byte[0]);
  }

  @Test
  public void testParse_SingleDefaultNonamedByte_Default() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("byte;");
    final JBBPFieldStruct result = parser.parse(new byte[]{(byte) -42});
    assertEquals(-42, result.findFieldForType(JBBPFieldByte.class).getAsInt());
  }

  @Test
  public void testParse_SingleDefaultNonamedByte_BigEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare(">byte;");
    final JBBPFieldStruct result = parser.parse(new byte[]{(byte) -42});
    assertEquals(-42, result.findFieldForType(JBBPFieldByte.class).getAsInt());
  }

  @Test
  public void testParse_SingleDefaultNonamedByte_LittleEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("<byte;");
    final JBBPFieldStruct result = parser.parse(new byte[]{(byte) -42});
    assertEquals(-42, result.findFieldForType(JBBPFieldByte.class).getAsInt());
  }

  @Test(expected = EOFException.class)
  public void testParse_UByte_ErrorForEOF() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("ubyte;");
    parser.parse(new byte[0]);
  }

  @Test
  public void testParse_SingleDefaultNonamedUByte_Default() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("ubyte;");
    final JBBPFieldStruct result = parser.parse(new byte[]{(byte) -42});
    assertEquals(-42 & 0xFF, result.findFieldForType(JBBPFieldUByte.class).getAsInt());
  }

  @Test
  public void testParse_SingleDefaultNonamedUByte_BigEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare(">ubyte;");
    final JBBPFieldStruct result = parser.parse(new byte[]{(byte) -42});
    assertEquals(-42 & 0xFF, result.findFieldForType(JBBPFieldUByte.class).getAsInt());
  }

  @Test
  public void testParse_SingleDefaultNonamedUByte_LittleEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("<ubyte;");
    final JBBPFieldStruct result = parser.parse(new byte[]{(byte) -42});
    assertEquals(-42 & 0xFF, result.findFieldForType(JBBPFieldUByte.class).getAsInt());
  }

  @Test(expected = EOFException.class)
  public void testParse_Short_ErrorForEOF() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("short;");
    parser.parse(new byte[0]);
  }

  @Test
  public void testParse_SingleDefaultNonamedShort_Default() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("short;");
    final JBBPFieldStruct result = parser.parse(new byte[]{1, 2});
    assertEquals(0x0102, result.findFieldForType(JBBPFieldShort.class).getAsInt());
  }

  @Test
  public void testParse_SingleDefaultNonamedShort_BigEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare(">short;");
    final JBBPFieldStruct result = parser.parse(new byte[]{1, 2});
    assertEquals(0x0102, result.findFieldForType(JBBPFieldShort.class).getAsInt());
  }

  @Test
  public void testParse_SingleDefaultNonamedShort_LittleEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("<short;");
    final JBBPFieldStruct result = parser.parse(new byte[]{1, 2});
    assertEquals(0x0201, result.findFieldForType(JBBPFieldShort.class).getAsInt());
  }

  @Test(expected = EOFException.class)
  public void testParse_UShort_ErrorForEOF() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("ushort;");
    parser.parse(new byte[0]);
  }

  @Test
  public void testParse_SingleDefaultNonamedUShort_Default() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("ushort;");
    final JBBPFieldStruct result = parser.parse(new byte[]{-1, -2});
    assertEquals(((-1 << 8) | (-2 & 0xFF)) & 0xFFFF, result.findFieldForType(JBBPFieldUShort.class).getAsInt());
  }

  @Test
  public void testParse_SingleDefaultNonamedUShort_BigEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare(">ushort;");
    final JBBPFieldStruct result = parser.parse(new byte[]{-1, -2});
    assertEquals(((-1 << 8) | (-2 & 0xFF)) & 0xFFFF, result.findFieldForType(JBBPFieldUShort.class).getAsInt());
  }

  @Test
  public void testParse_SingleDefaultNonamedUShort_LittleEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("<ushort;");
    final JBBPFieldStruct result = parser.parse(new byte[]{-1, -2});
    assertEquals(((-2 << 8) | (-1 & 0xFF)) & 0xFFFF, result.findFieldForType(JBBPFieldUShort.class).getAsInt());
  }

  @Test(expected = EOFException.class)
  public void testParse_Int_ErrorForEOF() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("int;");
    parser.parse(new byte[0]);
  }

  @Test
  public void testParse_SingleDefaultNonamedInt_Default() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("int;");
    final JBBPFieldStruct result = parser.parse(new byte[]{1, 2, 3, 4});
    assertEquals(0x01020304, result.findFieldForType(JBBPFieldInt.class).getAsInt());
  }

  @Test
  public void testParse_SingleDefaultNonamedInt_BigEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare(">int;");
    final JBBPFieldStruct result = parser.parse(new byte[]{1, 2, 3, 4});
    assertEquals(0x01020304, result.findFieldForType(JBBPFieldInt.class).getAsInt());
  }

  @Test
  public void testParse_SingleDefaultNonamedInt_LittleEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("<int;");
    final JBBPFieldStruct result = parser.parse(new byte[]{1, 2, 3, 4});
    assertEquals(0x04030201, result.findFieldForType(JBBPFieldInt.class).getAsInt());
  }

  @Test(expected = EOFException.class)
  public void testParse_Long_ErrorForEOF() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("long;");
    parser.parse(new byte[0]);
  }

  @Test
  public void testParse_SingleDefaultNonamedLong_Default() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("long;");
    final JBBPFieldStruct result = parser.parse(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
    assertEquals(0x0102030405060708L, result.findFieldForType(JBBPFieldLong.class).getAsLong());
  }

  @Test
  public void testParse_SingleDefaultNonamedLong_BigEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare(">long;");
    final JBBPFieldStruct result = parser.parse(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
    assertEquals(0x0102030405060708L, result.findFieldForType(JBBPFieldLong.class).getAsLong());
  }

  @Test
  public void testParse_SingleDefaultNonamedLong_LittleEndian() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("<long;");
    final JBBPFieldStruct result = parser.parse(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
    assertEquals(0x0807060504030201L, result.findFieldForType(JBBPFieldLong.class).getAsLong());
  }

  @Test
  public void testParse_SingleNonamedVar() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("short k; var; int;");

    final JBBPIntCounter counter = new JBBPIntCounter();

    final JBBPFieldStruct struct = parser.parse(new byte[]{9, 8, 33, 1, 2, 3, 4}, new JBBPVarFieldProcessor() {

      public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(final JBBPBitInputStream inStream, final int arraySize, final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        fail("Must not be called");
        return null;
      }

      public JBBPAbstractField readVarField(final JBBPBitInputStream inStream, final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
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

    assertEquals(33, struct.findFieldForType(JBBPFieldByte.class).getAsInt());
    assertEquals(1, counter.get());
  }

  @Test
  public void testParse_NamedVarWithCustomOrder() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("short k; <var:-12345 Some; int;");

    final JBBPIntCounter counter = new JBBPIntCounter();

    final JBBPFieldStruct struct = parser.parse(new byte[]{9, 8, 33, 1, 2, 3, 4}, new JBBPVarFieldProcessor() {

      public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(final JBBPBitInputStream inStream, final int arraySize, final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        fail("Must not be called");
        return null;
      }

      public JBBPAbstractField readVarField(final JBBPBitInputStream inStream, final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
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

  @Test(expected = NullPointerException.class)
  public void testParse_SingleNonamedVar_ErrorForNullResult() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("short k; var; int;");

    final JBBPFieldStruct struct = parser.parse(new byte[]{9, 8, 33, 1, 2, 3, 4}, new JBBPVarFieldProcessor() {

      public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(final JBBPBitInputStream inStream, final int arraySize, final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        fail("Must not be called");
        return null;
      }

      public JBBPAbstractField readVarField(final JBBPBitInputStream inStream, final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        return null;
      }
    }, null);
  }

  @Test(expected = JBBPParsingException.class)
  public void testParse_SingleNonamedVar_ErrorForArrayResult() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("short k; var; int;");

    final JBBPFieldStruct struct = parser.parse(new byte[]{9, 8, 33, 1, 2, 3, 4}, new JBBPVarFieldProcessor() {

      public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(final JBBPBitInputStream inStream, final int arraySize, final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        fail("Must not be called");
        return null;
      }

      public JBBPAbstractField readVarField(final JBBPBitInputStream inStream, final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        return new JBBPFieldArrayByte(fieldName, new byte[]{1, 2, 3});
      }
    }, null);
  }

  @Test(expected = JBBPParsingException.class)
  public void testParse_SingleNonamedVar_ErrorForDifferentName() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("short k; var name; int;");

    final JBBPFieldStruct struct = parser.parse(new byte[]{9, 8, 33, 1, 2, 3, 4}, new JBBPVarFieldProcessor() {

      public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(final JBBPBitInputStream inStream, final int arraySize, final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        fail("Must not be called");
        return null;
      }

      public JBBPAbstractField readVarField(final JBBPBitInputStream inStream, final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        assertNotNull(fieldName);
        return new JBBPFieldByte(new JBBPNamedFieldInfo("jskdjhsd", "dlkjsf", 0), (byte) 1);
      }
    }, null);
  }

  @Test
  public void testParse_SingleNonamedVarArray() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("short k; var[18]; int;");

    final JBBPIntCounter counter = new JBBPIntCounter();

    final JBBPFieldStruct struct = parser.parse(new byte[]{9, 8, 33, 1, 2, 3, 4}, new JBBPVarFieldProcessor() {

      public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(final JBBPBitInputStream inStream, final int arraySize, final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        assertNotNull(inStream);
        final int value = inStream.readByte();
        assertEquals(33, value);
        assertEquals(0x0908, numericFieldMap.findFieldForType(JBBPFieldShort.class).getAsInt());

        assertNull(fieldName);
        assertEquals(0, extraValue);
        assertEquals(18, arraySize);
        assertEquals(JBBPByteOrder.BIG_ENDIAN, byteOrder);

        counter.incrementAndGet();

        return new JBBPFieldArrayByte(fieldName, new byte[]{(byte) value});
      }

      public JBBPAbstractField readVarField(final JBBPBitInputStream inStream, final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        fail("Must not be called");
        return null;
      }
    }, null);

    assertArrayEquals(new byte[]{33}, struct.findFieldForType(JBBPFieldArrayByte.class).getArray());
    assertEquals(1, counter.get());
  }

  @Test
  public void testParse_NamedVarArrayWithCustomOrder() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("short k; <var:-12345 [2334] Some; int;");

    final JBBPIntCounter counter = new JBBPIntCounter();

    final JBBPFieldStruct struct = parser.parse(new byte[]{9, 8, 33, 1, 2, 3, 4}, new JBBPVarFieldProcessor() {

      public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(final JBBPBitInputStream inStream, final int arraySize, final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        assertNotNull(inStream);
        final int value = inStream.readByte();
        assertEquals(33, value);
        assertEquals(0x0908, numericFieldMap.findFieldForType(JBBPFieldShort.class).getAsInt());

        assertEquals("some", fieldName.getFieldName());
        assertEquals(-12345, extraValue);
        assertEquals(2334, arraySize);
        assertEquals(JBBPByteOrder.LITTLE_ENDIAN, byteOrder);

        counter.incrementAndGet();

        return new JBBPFieldArrayByte(fieldName, new byte[]{(byte) value});
      }

      public JBBPAbstractField readVarField(final JBBPBitInputStream inStream, final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        fail("Must not be called");
        return null;
      }
    }, null);

    assertArrayEquals(new byte[]{33}, struct.findFieldForNameAndType("some", JBBPFieldArrayByte.class).getArray());
    assertEquals(1, counter.get());
  }

  @Test
  public void testParse_NamedVarArrayTillEndOfStream() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("short k; var [_] Some;");

    final JBBPIntCounter counter = new JBBPIntCounter();

    final JBBPFieldStruct struct = parser.parse(new byte[]{9, 8, 33, 1, 2, 3, 4}, new JBBPVarFieldProcessor() {

      public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(final JBBPBitInputStream inStream, final int arraySize, final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        assertNotNull(inStream);

        assertEquals(0x0908, numericFieldMap.findFieldForType(JBBPFieldShort.class).getAsInt());

        assertEquals("some", fieldName.getFieldName());
        assertEquals(0, extraValue);
        assertTrue(arraySize < 0);
        assertEquals(JBBPByteOrder.BIG_ENDIAN, byteOrder);

        counter.incrementAndGet();

        return new JBBPFieldArrayByte(fieldName, inStream.readByteArray(-1));
      }

      public JBBPAbstractField readVarField(final JBBPBitInputStream inStream, final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        fail("Must not be called");
        return null;
      }
    }, null);

    assertArrayEquals(new byte[]{33, 1, 2, 3, 4}, struct.findFieldForNameAndType("some", JBBPFieldArrayByte.class).getArray());
    assertEquals(1, counter.get());
  }

  @Test
  public void testParse_NamedVarArrayForZeroLength() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("short k; var [k] Some;");

    final JBBPFieldStruct struct = parser.parse(new byte[]{0, 0}, new JBBPVarFieldProcessor() {

      public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(final JBBPBitInputStream inStream, final int arraySize, final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        assertEquals(0, arraySize);
        return new JBBPFieldArrayByte(fieldName, new byte[0]);
      }

      public JBBPAbstractField readVarField(final JBBPBitInputStream inStream, final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        fail("Must not be called");
        return null;
      }
    }, null);

    assertEquals(0, struct.findFieldForNameAndType("some", JBBPFieldArrayByte.class).size());
  }

  @Test(expected = NullPointerException.class)
  public void testParse_SingleNonamedVarArray_ErrorForNullResult() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("short k; var [k]; int;");

    final JBBPFieldStruct struct = parser.parse(new byte[]{9, 8, 33, 1, 2, 3, 4}, new JBBPVarFieldProcessor() {

      public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(final JBBPBitInputStream inStream, final int arraySize, final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        assertEquals(0x0908, arraySize);
        return null;
      }

      public JBBPAbstractField readVarField(final JBBPBitInputStream inStream, final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        fail("Must not be called");
        return null;
      }
    }, null);
  }

  @Test(expected = JBBPParsingException.class)
  public void testParse_SingleNonamedVarArray_ErrorForDifferentName() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("short k; var [234] name; int;");

    final JBBPFieldStruct struct = parser.parse(new byte[]{9, 8, 33, 1, 2, 3, 4}, new JBBPVarFieldProcessor() {

      public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(final JBBPBitInputStream inStream, final int arraySize, final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        assertNotNull(fieldName);
        return new JBBPFieldArrayByte(new JBBPNamedFieldInfo("jskdjhsd", "dlkjsf", 0), new byte[]{1});
      }

      public JBBPAbstractField readVarField(final JBBPBitInputStream inStream, final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        fail("Must not be called");
        return null;
      }
    }, null);
  }

  @Test(expected = EOFException.class)
  public void testParse_BitFields_EOF() throws Exception {
    JBBPParser.prepare("bit:4;").parse(new byte[0]);
  }

  @Test(expected = EOFException.class)
  public void testParse_BitFieldArray_EOF() throws Exception {
    JBBPParser.prepare("bit:4 [1];").parse(new byte[0]);
  }

  @Test
  public void testParse_BitFieldArrayWholeStream_Empty() throws Exception {
    assertEquals(0, JBBPParser.prepare("bit:4 [_];").parse(new byte[0]).findFieldForType(JBBPFieldArrayBit.class).size());
  }

  @Test
  public void testParse_SeveralPrimitiveFields() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("bit:4; bit:4; bool; byte; ubyte; short; ushort; int; long;");
    final JBBPFieldStruct result = parser.parse(new byte[]{0x12, 1, 87, (byte) 0xF3, 1, 2, (byte) 0xFE, 4, 6, 7, 8, 9, (byte) 0xFF, 1, 2, 3, 5, 6, 7, 8, 9});

    assertEquals(2, result.findFirstFieldForType(JBBPFieldBit.class).getAsInt());
    assertEquals(1, result.findLastFieldForType(JBBPFieldBit.class).getAsInt());

    try {
      result.findFieldForType(JBBPFieldBit.class);
      fail("Must throw JBBPTooManyFieldsFoundException");
    }
    catch (JBBPTooManyFieldsFoundException ex) {
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
    JBBPParser.prepare("byte; align;").parse(new byte[]{1});
  }

  @Test(expected = EOFException.class)
  public void testParse_Align_ErrorForEOF() throws Exception {
    JBBPParser.prepare("byte; align:34;").parse(new byte[]{1});
  }

  @Test
  public void testParse_Align_Default() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("bit:4; align; bool; byte; ubyte; short; ushort; int; long;");
    final JBBPFieldStruct result = parser.parse(new byte[]{0x12, 1, 87, (byte) 0xF3, 1, 2, (byte) 0xFE, 4, 6, 7, 8, 9, (byte) 0xFF, 1, 2, 3, 5, 6, 7, 8, 9});
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

  @Test(expected = EOFException.class)
  public void testParse_Skip_Default_ErrorForEOF() throws Exception {
    JBBPParser.prepare("byte; skip;").parse(new byte[]{1});
  }

  @Test(expected = EOFException.class)
  public void testParse_Skip_ErrorForEOF() throws Exception {
    JBBPParser.prepare("byte; skip:34;").parse(new byte[]{1});
  }

  @Test
  public void testParse_Skip_WithoutArgument() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("byte; skip; short;");
    final JBBPFieldStruct result = parser.parse(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
    assertEquals(1, result.findFirstFieldForType(JBBPFieldByte.class).getAsInt());
    assertEquals(0x0304, result.findFieldForType(JBBPFieldShort.class).getAsInt());
  }

  @Test
  public void testParse_Skip_ShortDistance() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("byte; skip:3; short;");
    final JBBPFieldStruct result = parser.parse(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
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

  @Test(expected = EOFException.class)
  public void testParse_Skip_TooLongDistance() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("byte; skip:33; short;");
    parser.parse(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
  }

  @Test
  public void testParse_Align_Int_WithEffect() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("byte; byte; align:4; int;");
    final JBBPFieldStruct result = parser.parse(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
    assertEquals(1, result.findFirstFieldForType(JBBPFieldByte.class).getAsInt());
    assertEquals(2, result.findLastFieldForType(JBBPFieldByte.class).getAsInt());
    assertEquals(0x05060708, result.findLastFieldForType(JBBPFieldInt.class).getAsInt());
  }

  @Test
  public void testParse_Align_Int_WithoutEffect() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("byte; byte; byte; byte; align:4; int;");
    final JBBPFieldStruct result = parser.parse(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
    assertEquals(1, result.findFirstFieldForType(JBBPFieldByte.class).getAsInt());
    assertEquals(4, result.findLastFieldForType(JBBPFieldByte.class).getAsInt());
    assertEquals(0x05060708, result.findLastFieldForType(JBBPFieldInt.class).getAsInt());
  }

  @Test(expected = EOFException.class)
  public void testParse_FixedBitArray_EOFException() throws Exception {
    JBBPParser.prepare("byte; bit:4[1];").parse(new byte[]{1});
  }

  @Test
  public void testParse_NonFixedBitArray_ParsedAsEmptyArray() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte; bit:4[_] array;").parse(new byte[]{1});
    assertEquals(0, parsed.findFieldForNameAndType("array", JBBPFieldArrayBit.class).size());
  }

  @Test
  public void testParse_ProcessingOfExtraFieldValuesInSkippedStructureFields() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte len; struct1 [len] { int a; var:23231223 [1024] helloarray; int b; bit:3; bit:7 [10233]; var:-1332 hello; skip:34221223; bit:7; bit:1; align:3445; bit:2; int skippedInt; long lng; insidestruct {bit:1; bit:2; bit:3;} } int end; ").parse(new byte[]{0, 1, 2, 3, 4}, new JBBPVarFieldProcessor() {

      public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(JBBPBitInputStream inStream, int arraySize, JBBPNamedFieldInfo fieldName, int extraValue, JBBPByteOrder byteOrder, JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        fail("Must not be called");
        return null;
      }

      public JBBPAbstractField readVarField(JBBPBitInputStream inStream, JBBPNamedFieldInfo fieldName, int extraValue, JBBPByteOrder byteOrder, JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        fail("Must not be called");
        return null;
      }
    }, null);
    assertEquals(0x01020304, parsed.findFieldForNameAndType("end", JBBPFieldInt.class).getAsInt());
  }

  @Test
  public void testParse_ProcessingOfExtraFieldValuesInSkippedStructureFields1() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte len; struct1 [len] {var:-1332 hello; align:3445; } int end; ").parse(new byte[]{0, 1, 2, 3, 4}, new JBBPVarFieldProcessor() {

      public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(JBBPBitInputStream inStream, int arraySize, JBBPNamedFieldInfo fieldName, int extraValue, JBBPByteOrder byteOrder, JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        fail("Must not be called");
        return null;
      }

      public JBBPAbstractField readVarField(JBBPBitInputStream inStream, JBBPNamedFieldInfo fieldName, int extraValue, JBBPByteOrder byteOrder, JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        fail("Must not be called");
        return null;
      }
    }, null);
    assertEquals(0x01020304, parsed.findFieldForNameAndType("end", JBBPFieldInt.class).getAsInt());
  }

  @Test
  public void testParse_FixedBitArray() throws Exception {
    final JBBPFieldArrayBit bits = JBBPParser.prepare("bit:4 [8];").parse(new byte[]{0x21, 0x43, 0x65, (byte) 0x87}).findFieldForType(JBBPFieldArrayBit.class);
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
    final JBBPFieldArrayBit bits = JBBPParser.prepare("bit:4 [_];").parse(new byte[]{0x21, 0x43, 0x65, (byte) 0x87}).findFieldForType(JBBPFieldArrayBit.class);
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

  @Test(expected = EOFException.class)
  public void testParse_FixedByteArray_EOFException() throws Exception {
    JBBPParser.prepare("byte; byte[1];").parse(new byte[]{1});
  }

  @Test
  public void testParse_NonFixedByteArray_ParsedAsEmptyArray() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte; byte[_] array;").parse(new byte[]{1});
    assertEquals(0, parsed.findFieldForNameAndType("array", JBBPFieldArrayByte.class).size());
  }

  @Test
  public void testParse_FixedByteArray_Default() throws Exception {
    final JBBPFieldArrayByte bytes = JBBPParser.prepare("byte[4];").parse(new byte[]{0x21, 0x43, 0x65, (byte) 0x87}).findFieldForType(JBBPFieldArrayByte.class);
    assertEquals(4, bytes.size());
    assertEquals(0x21, bytes.getAsInt(0) & 0xFF);
    assertEquals(0x43, bytes.getAsInt(1) & 0xFF);
    assertEquals(0x65, bytes.getAsInt(2) & 0xFF);
    assertEquals(0x87, bytes.getAsInt(3) & 0xFF);
  }

  @Test
  public void testParse_FixedByteArray_BigEndian() throws Exception {
    final JBBPFieldArrayByte bytes = JBBPParser.prepare(">byte[4];").parse(new byte[]{0x21, 0x43, 0x65, (byte) 0x87}).findFieldForType(JBBPFieldArrayByte.class);
    assertEquals(4, bytes.size());
    assertEquals(0x21, bytes.getAsInt(0) & 0xFF);
    assertEquals(0x43, bytes.getAsInt(1) & 0xFF);
    assertEquals(0x65, bytes.getAsInt(2) & 0xFF);
    assertEquals(0x87, bytes.getAsInt(3) & 0xFF);
  }

  @Test
  public void testParse_FixedByteArray_LittleEndian() throws Exception {
    final JBBPFieldArrayByte bytes = JBBPParser.prepare("<byte[4];").parse(new byte[]{0x21, 0x43, 0x65, (byte) 0x87}).findFieldForType(JBBPFieldArrayByte.class);
    assertEquals(4, bytes.size());
    assertEquals(0x87, bytes.getAsInt(0) & 0xFF);
    assertEquals(0x65, bytes.getAsInt(1) & 0xFF);
    assertEquals(0x43, bytes.getAsInt(2) & 0xFF);
    assertEquals(0x21, bytes.getAsInt(3) & 0xFF);
  }

  @Test
  public void testParse_NonFixedByteArray_Default() throws Exception {
    final JBBPFieldArrayByte bytes = JBBPParser.prepare("byte[_];").parse(new byte[]{0x21, 0x43, 0x65, (byte) 0x87}).findFieldForType(JBBPFieldArrayByte.class);
    assertEquals(4, bytes.size());
    assertEquals(0x21, bytes.getAsInt(0) & 0xFF);
    assertEquals(0x43, bytes.getAsInt(1) & 0xFF);
    assertEquals(0x65, bytes.getAsInt(2) & 0xFF);
    assertEquals(0x87, bytes.getAsInt(3) & 0xFF);
  }

  @Test
  public void testParse_NonFixedByteArray_BigEndian() throws Exception {
    final JBBPFieldArrayByte bytes = JBBPParser.prepare(">byte[_];").parse(new byte[]{0x21, 0x43, 0x65, (byte) 0x87}).findFieldForType(JBBPFieldArrayByte.class);
    assertEquals(4, bytes.size());
    assertEquals(0x21, bytes.getAsInt(0) & 0xFF);
    assertEquals(0x43, bytes.getAsInt(1) & 0xFF);
    assertEquals(0x65, bytes.getAsInt(2) & 0xFF);
    assertEquals(0x87, bytes.getAsInt(3) & 0xFF);
  }

  @Test
  public void testParse_NonFixedByteArray_LittleEndian() throws Exception {
    final JBBPFieldArrayByte bytes = JBBPParser.prepare("<byte[_];").parse(new byte[]{0x21, 0x43, 0x65, (byte) 0x87}).findFieldForType(JBBPFieldArrayByte.class);
    assertEquals(4, bytes.size());
    assertEquals(0x87, bytes.getAsInt(0) & 0xFF);
    assertEquals(0x65, bytes.getAsInt(1) & 0xFF);
    assertEquals(0x43, bytes.getAsInt(2) & 0xFF);
    assertEquals(0x21, bytes.getAsInt(3) & 0xFF);
  }

  @Test(expected = EOFException.class)
  public void testParse_FixedUByteArray_EOFException() throws Exception {
    JBBPParser.prepare("byte; ubyte[1];").parse(new byte[]{1});
  }

  @Test
  public void testParse_NonFixedUByteArray_ParsedAsEmptyArray() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte; ubyte[_] array;").parse(new byte[]{1});
    assertEquals(0, parsed.findFieldForNameAndType("array", JBBPFieldArrayUByte.class).size());
  }

  @Test
  public void testParse_FixedUByteArray_Default() throws Exception {
    final JBBPFieldArrayUByte bytes = JBBPParser.prepare("ubyte[4];").parse(new byte[]{0x21, 0x43, 0x65, (byte) 0x87}).findFieldForType(JBBPFieldArrayUByte.class);
    assertEquals(4, bytes.size());
    assertEquals(0x21, bytes.getAsInt(0));
    assertEquals(0x43, bytes.getAsInt(1));
    assertEquals(0x65, bytes.getAsInt(2));
    assertEquals(0x87, bytes.getAsInt(3));
  }

  @Test
  public void testParse_FixedUByteArray_BigEndian() throws Exception {
    final JBBPFieldArrayUByte bytes = JBBPParser.prepare(">ubyte[4];").parse(new byte[]{0x21, 0x43, 0x65, (byte) 0x87}).findFieldForType(JBBPFieldArrayUByte.class);
    assertEquals(4, bytes.size());
    assertEquals(0x21, bytes.getAsInt(0));
    assertEquals(0x43, bytes.getAsInt(1));
    assertEquals(0x65, bytes.getAsInt(2));
    assertEquals(0x87, bytes.getAsInt(3));
  }

  @Test
  public void testParse_FixedUByteArray_LittleEndian() throws Exception {
    final JBBPFieldArrayUByte bytes = JBBPParser.prepare("<ubyte[4];").parse(new byte[]{0x21, 0x43, 0x65, (byte) 0x87}).findFieldForType(JBBPFieldArrayUByte.class);
    assertEquals(4, bytes.size());
    assertEquals(0x87, bytes.getAsInt(0));
    assertEquals(0x65, bytes.getAsInt(1));
    assertEquals(0x43, bytes.getAsInt(2));
    assertEquals(0x21, bytes.getAsInt(3));
  }

  @Test
  public void testParse_NonFixedUByteArray_Default() throws Exception {
    final JBBPFieldArrayUByte bytes = JBBPParser.prepare("ubyte[_];").parse(new byte[]{0x21, 0x43, 0x65, (byte) 0x87}).findFieldForType(JBBPFieldArrayUByte.class);
    assertEquals(4, bytes.size());
    assertEquals(0x21, bytes.getAsInt(0));
    assertEquals(0x43, bytes.getAsInt(1));
    assertEquals(0x65, bytes.getAsInt(2));
    assertEquals(0x87, bytes.getAsInt(3));
  }

  @Test
  public void testParse_NonFixedUByteArray_BigEndian() throws Exception {
    final JBBPFieldArrayUByte bytes = JBBPParser.prepare(">ubyte[_];").parse(new byte[]{0x21, 0x43, 0x65, (byte) 0x87}).findFieldForType(JBBPFieldArrayUByte.class);
    assertEquals(4, bytes.size());
    assertEquals(0x21, bytes.getAsInt(0));
    assertEquals(0x43, bytes.getAsInt(1));
    assertEquals(0x65, bytes.getAsInt(2));
    assertEquals(0x87, bytes.getAsInt(3));
  }

  @Test
  public void testParse_NonFixedUByteArray_LittleEndian() throws Exception {
    final JBBPFieldArrayUByte bytes = JBBPParser.prepare("<ubyte[_];").parse(new byte[]{0x21, 0x43, 0x65, (byte) 0x87}).findFieldForType(JBBPFieldArrayUByte.class);
    assertEquals(4, bytes.size());
    assertEquals(0x87, bytes.getAsInt(0));
    assertEquals(0x65, bytes.getAsInt(1));
    assertEquals(0x43, bytes.getAsInt(2));
    assertEquals(0x21, bytes.getAsInt(3));
  }

  @Test(expected = EOFException.class)
  public void testParse_FixedBooleanArray_EOFException() throws Exception {
    JBBPParser.prepare("byte; bool[1];").parse(new byte[]{1});
  }

  @Test
  public void testParse_NonFixedBoolArray_ParsedAsEmptyArray() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte; bool[_] array;").parse(new byte[]{1});
    assertEquals(0, parsed.findFieldForNameAndType("array", JBBPFieldArrayBoolean.class).size());
  }

  @Test
  public void testParse_FixedBooleanArray_Default() throws Exception {
    final JBBPFieldArrayBoolean bools = JBBPParser.prepare("bool[4];").parse(new byte[]{0, 0x43, 0x65, 0}).findFieldForType(JBBPFieldArrayBoolean.class);
    assertEquals(4, bools.size());
    assertFalse(bools.getAsBool(0));
    assertTrue(bools.getAsBool(1));
    assertTrue(bools.getAsBool(2));
    assertFalse(bools.getAsBool(3));
  }

  @Test
  public void testParse_FixedBooleanArray_BigEndian() throws Exception {
    final JBBPFieldArrayBoolean bools = JBBPParser.prepare(">bool[4];").parse(new byte[]{0, 0x43, 0x65, 0}).findFieldForType(JBBPFieldArrayBoolean.class);
    assertEquals(4, bools.size());
    assertFalse(bools.getAsBool(0));
    assertTrue(bools.getAsBool(1));
    assertTrue(bools.getAsBool(2));
    assertFalse(bools.getAsBool(3));
  }

  @Test
  public void testParse_FixedBooleanArray_LittleEndian() throws Exception {
    final JBBPFieldArrayBoolean bools = JBBPParser.prepare("<bool[4];").parse(new byte[]{0, 0x43, 0x65, 0}).findFieldForType(JBBPFieldArrayBoolean.class);
    assertEquals(4, bools.size());
    assertFalse(bools.getAsBool(0));
    assertTrue(bools.getAsBool(1));
    assertTrue(bools.getAsBool(2));
    assertFalse(bools.getAsBool(3));
  }

  @Test
  public void testParse_NonFixedBooleanArray_Default() throws Exception {
    final JBBPFieldArrayBoolean bools = JBBPParser.prepare("bool[_];").parse(new byte[]{0, 0x43, 0x65, 0}).findFieldForType(JBBPFieldArrayBoolean.class);
    assertEquals(4, bools.size());
    assertFalse(bools.getAsBool(0));
    assertTrue(bools.getAsBool(1));
    assertTrue(bools.getAsBool(2));
    assertFalse(bools.getAsBool(3));
  }

  @Test
  public void testParse_NonFixedBooleanArray_BigEndian() throws Exception {
    final JBBPFieldArrayBoolean bools = JBBPParser.prepare(">bool[_];").parse(new byte[]{0, 0x43, 0x65, 0}).findFieldForType(JBBPFieldArrayBoolean.class);
    assertEquals(4, bools.size());
    assertFalse(bools.getAsBool(0));
    assertTrue(bools.getAsBool(1));
    assertTrue(bools.getAsBool(2));
    assertFalse(bools.getAsBool(3));
  }

  @Test
  public void testParse_NonFixedBooleanArray_LittleEndian() throws Exception {
    final JBBPFieldArrayBoolean bools = JBBPParser.prepare("<bool[_];").parse(new byte[]{0, 0x43, 0x65, 0}).findFieldForType(JBBPFieldArrayBoolean.class);
    assertEquals(4, bools.size());
    assertFalse(bools.getAsBool(0));
    assertTrue(bools.getAsBool(1));
    assertTrue(bools.getAsBool(2));
    assertFalse(bools.getAsBool(3));
  }

  @Test(expected = EOFException.class)
  public void testParse_FixedShortArray_EOFException() throws Exception {
    JBBPParser.prepare("byte; short[1];").parse(new byte[]{1});
  }

  @Test
  public void testParse_NonFixedShortArray_ParsedAsEmptyArray() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte; short[_] array;").parse(new byte[]{1});
    assertEquals(0, parsed.findFieldForNameAndType("array", JBBPFieldArrayShort.class).size());
  }

  @Test
  public void testParse_FixedShortArray_Default() throws Exception {
    final JBBPFieldArrayShort shorts = JBBPParser.prepare("short[2];").parse(new byte[]{(byte) 0xF7, 0x43, 0x65, 0}).findFieldForType(JBBPFieldArrayShort.class);
    assertEquals(2, shorts.size());
    assertEquals((short) 0xF743, shorts.getAsInt(0));
    assertEquals((short) 0x6500, shorts.getAsInt(1));
  }

  @Test
  public void testParse_FixedShortArray_BigEndian() throws Exception {
    final JBBPFieldArrayShort shorts = JBBPParser.prepare(">short[2];").parse(new byte[]{(byte) 0xF7, 0x43, 0x65, 0}).findFieldForType(JBBPFieldArrayShort.class);
    assertEquals(2, shorts.size());
    assertEquals((short) 0xF743, shorts.getAsInt(0));
    assertEquals((short) 0x6500, shorts.getAsInt(1));
  }

  @Test
  public void testParse_FixedShortArray_LittleEndian() throws Exception {
    final JBBPFieldArrayShort shorts = JBBPParser.prepare("<short[2];").parse(new byte[]{(byte) 0xF7, 0x43, 0x65, 0}).findFieldForType(JBBPFieldArrayShort.class);
    assertEquals(2, shorts.size());
    assertEquals((short) 0x43F7, shorts.getAsInt(0));
    assertEquals((short) 0x0065, shorts.getAsInt(1));
  }

  @Test
  public void testParse_NonFixedShortArray_Default() throws Exception {
    final JBBPFieldArrayShort shorts = JBBPParser.prepare("short[_];").parse(new byte[]{(byte) 0xF7, 0x43, 0x65, 0}).findFieldForType(JBBPFieldArrayShort.class);
    assertEquals(2, shorts.size());
    assertEquals((short) 0xF743, shorts.getAsInt(0));
    assertEquals((short) 0x6500, shorts.getAsInt(1));
  }

  @Test
  public void testParse_NonFixedShortArray_BigEndian() throws Exception {
    final JBBPFieldArrayShort shorts = JBBPParser.prepare(">short[_];").parse(new byte[]{(byte) 0xF7, 0x43, 0x65, 0}).findFieldForType(JBBPFieldArrayShort.class);
    assertEquals(2, shorts.size());
    assertEquals((short) 0xF743, shorts.getAsInt(0));
    assertEquals((short) 0x6500, shorts.getAsInt(1));
  }

  @Test
  public void testParse_NonFixedShortArray_LittleEndian() throws Exception {
    final JBBPFieldArrayShort shorts = JBBPParser.prepare("<short[_];").parse(new byte[]{(byte) 0xF7, 0x43, 0x65, 0}).findFieldForType(JBBPFieldArrayShort.class);
    assertEquals(2, shorts.size());
    assertEquals((short) 0x43F7, shorts.getAsInt(0));
    assertEquals((short) 0x0065, shorts.getAsInt(1));
  }

  @Test(expected = EOFException.class)
  public void testParse_FixedUShortArray_EOFException() throws Exception {
    JBBPParser.prepare("byte; ushort[1];").parse(new byte[]{1});
  }

  @Test
  public void testParse_NonFixedUShortArray_ParsedAsEmptyArray() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte; ushort[_] array;").parse(new byte[]{1});
    assertEquals(0, parsed.findFieldForNameAndType("array", JBBPFieldArrayUShort.class).size());
  }

  @Test
  public void testParse_FixedUShortArray_Default() throws Exception {
    final JBBPFieldArrayUShort shorts = JBBPParser.prepare("ushort[2];").parse(new byte[]{(byte) 0xF7, 0x43, 0x65, 0}).findFieldForType(JBBPFieldArrayUShort.class);
    assertEquals(2, shorts.size());
    assertEquals(0xF743, shorts.getAsInt(0));
    assertEquals(0x6500, shorts.getAsInt(1));
  }

  @Test
  public void testParse_FixedUShortArray_BigEndian() throws Exception {
    final JBBPFieldArrayUShort shorts = JBBPParser.prepare(">ushort[2];").parse(new byte[]{(byte) 0xF7, 0x43, 0x65, 0}).findFieldForType(JBBPFieldArrayUShort.class);
    assertEquals(2, shorts.size());
    assertEquals(0xF743, shorts.getAsInt(0));
    assertEquals(0x6500, shorts.getAsInt(1));
  }

  @Test
  public void testParse_FixedUShortArray_LittleEndian() throws Exception {
    final JBBPFieldArrayUShort shorts = JBBPParser.prepare("<ushort[2];").parse(new byte[]{(byte) 0xF7, 0x43, 0x65, 0}).findFieldForType(JBBPFieldArrayUShort.class);
    assertEquals(2, shorts.size());
    assertEquals(0x43F7, shorts.getAsInt(0));
    assertEquals(0x0065, shorts.getAsInt(1));
  }

  @Test
  public void testParse_NonFixedUShortArray_Default() throws Exception {
    final JBBPFieldArrayUShort shorts = JBBPParser.prepare("ushort[_];").parse(new byte[]{(byte) 0xF7, 0x43, 0x65, 0}).findFieldForType(JBBPFieldArrayUShort.class);
    assertEquals(2, shorts.size());
    assertEquals(0xF743, shorts.getAsInt(0));
    assertEquals(0x6500, shorts.getAsInt(1));
  }

  @Test
  public void testParse_NonFixedUShortArray_BigEndian() throws Exception {
    final JBBPFieldArrayUShort shorts = JBBPParser.prepare(">ushort[_];").parse(new byte[]{(byte) 0xF7, 0x43, 0x65, 0}).findFieldForType(JBBPFieldArrayUShort.class);
    assertEquals(2, shorts.size());
    assertEquals(0xF743, shorts.getAsInt(0));
    assertEquals(0x6500, shorts.getAsInt(1));
  }

  @Test
  public void testParse_NonFixedUShortArray_LittleEndian() throws Exception {
    final JBBPFieldArrayUShort shorts = JBBPParser.prepare("<ushort[_];").parse(new byte[]{(byte) 0xF7, 0x43, 0x65, 0}).findFieldForType(JBBPFieldArrayUShort.class);
    assertEquals(2, shorts.size());
    assertEquals(0x43F7, shorts.getAsInt(0));
    assertEquals(0x0065, shorts.getAsInt(1));
  }

  @Test(expected = EOFException.class)
  public void testParse_FixedIntArray_EOFException() throws Exception {
    JBBPParser.prepare("byte; int[1];").parse(new byte[]{1});
  }

  @Test
  public void testParse_NonFixedIntArray_ParsedAsEmptyArray() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte; int[_] array;").parse(new byte[]{1});
    assertEquals(0, parsed.findFieldForNameAndType("array", JBBPFieldArrayInt.class).size());
  }

  @Test
  public void testParse_FixedIntArray_Default() throws Exception {
    final JBBPFieldArrayInt ints = JBBPParser.prepare("int[2];").parse(new byte[]{(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0}).findFieldForType(JBBPFieldArrayInt.class);
    assertEquals(2, ints.size());
    assertEquals(0xF7436510, ints.getAsInt(0));
    assertEquals(0x352367A0, ints.getAsInt(1));
  }

  @Test
  public void testParse_FixedIntArray_BigEndian() throws Exception {
    final JBBPFieldArrayInt ints = JBBPParser.prepare(">int[2];").parse(new byte[]{(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0}).findFieldForType(JBBPFieldArrayInt.class);
    assertEquals(2, ints.size());
    assertEquals(0xF7436510, ints.getAsInt(0));
    assertEquals(0x352367A0, ints.getAsInt(1));
  }

  @Test
  public void testParse_FixedIntArray_LittleEndian() throws Exception {
    final JBBPFieldArrayInt ints = JBBPParser.prepare("<int[2];").parse(new byte[]{(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0}).findFieldForType(JBBPFieldArrayInt.class);
    assertEquals(2, ints.size());
    assertEquals(0x106543F7, ints.getAsInt(0));
    assertEquals(0xA0672335, ints.getAsInt(1));
  }

  @Test
  public void testParse_NonFixedIntArray_Default() throws Exception {
    final JBBPFieldArrayInt ints = JBBPParser.prepare("int[_];").parse(new byte[]{(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0}).findFieldForType(JBBPFieldArrayInt.class);
    assertEquals(2, ints.size());
    assertEquals(0xF7436510, ints.getAsInt(0));
    assertEquals(0x352367A0, ints.getAsInt(1));
  }

  @Test
  public void testParse_NonFixedIntArray_BigEndian() throws Exception {
    final JBBPFieldArrayInt ints = JBBPParser.prepare(">int[_];").parse(new byte[]{(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0}).findFieldForType(JBBPFieldArrayInt.class);
    assertEquals(2, ints.size());
    assertEquals(0xF7436510, ints.getAsInt(0));
    assertEquals(0x352367A0, ints.getAsInt(1));
  }

  @Test
  public void testParse_NonFixedIntArray_LittleEndian() throws Exception {
    final JBBPFieldArrayInt ints = JBBPParser.prepare("<int[_];").parse(new byte[]{(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0}).findFieldForType(JBBPFieldArrayInt.class);
    assertEquals(2, ints.size());
    assertEquals(0x106543F7, ints.getAsInt(0));
    assertEquals(0xA0672335, ints.getAsInt(1));
  }

  @Test(expected = EOFException.class)
  public void testParse_FixedLongArray_EOFException() throws Exception {
    JBBPParser.prepare("byte; long[1];").parse(new byte[]{1});
  }

  @Test
  public void testParse_NonFixedLongArray_ParsedAsEmptyArray() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte; long[_] array;").parse(new byte[]{1});
    assertEquals(0, parsed.findFieldForNameAndType("array", JBBPFieldArrayLong.class).size());
  }

  @Test
  public void testParse_FixedLongArray_Default() throws Exception {
    final JBBPFieldArrayLong longs = JBBPParser.prepare("long[2];").parse(new byte[]{(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0, 0x32, 0x33, 0x61, (byte) 0xCA, (byte) 0xBE, 0x22, 0x12, 0x30}).findFieldForType(JBBPFieldArrayLong.class);
    assertEquals(2, longs.size());
    assertEquals(0xF7436510352367A0L, longs.getAsLong(0));
    assertEquals(0x323361CABE221230L, longs.getAsLong(1));
  }

  @Test
  public void testParse_FixedLongArray_BigEndian() throws Exception {
    final JBBPFieldArrayLong longs = JBBPParser.prepare(">long[2];").parse(new byte[]{(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0, 0x32, 0x33, 0x61, (byte) 0xCA, (byte) 0xBE, 0x22, 0x12, 0x30}).findFieldForType(JBBPFieldArrayLong.class);
    assertEquals(2, longs.size());
    assertEquals(0xF7436510352367A0L, longs.getAsLong(0));
    assertEquals(0x323361CABE221230L, longs.getAsLong(1));
  }

  @Test
  public void testParse_FixedLongArray_LittleEndian() throws Exception {
    final JBBPFieldArrayLong longs = JBBPParser.prepare("<long[2];").parse(new byte[]{(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0, 0x32, 0x33, 0x61, (byte) 0xCA, (byte) 0xBE, 0x22, 0x12, 0x30}).findFieldForType(JBBPFieldArrayLong.class);
    assertEquals(2, longs.size());
    assertEquals(0xA0672335106543F7L, longs.getAsLong(0));
    assertEquals(0x301222BECA613332L, longs.getAsLong(1));
  }

  @Test
  public void testParse_NonFixedLongArray_Default() throws Exception {
    final JBBPFieldArrayLong longs = JBBPParser.prepare("long[_];").parse(new byte[]{(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0, 0x32, 0x33, 0x61, (byte) 0xCA, (byte) 0xBE, 0x22, 0x12, 0x30}).findFieldForType(JBBPFieldArrayLong.class);
    assertEquals(2, longs.size());
    assertEquals(0xF7436510352367A0L, longs.getAsLong(0));
    assertEquals(0x323361CABE221230L, longs.getAsLong(1));
  }

  @Test
  public void testParse_NonFixedLongArray_BigEndian() throws Exception {
    final JBBPFieldArrayLong longs = JBBPParser.prepare(">long[_];").parse(new byte[]{(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0, 0x32, 0x33, 0x61, (byte) 0xCA, (byte) 0xBE, 0x22, 0x12, 0x30}).findFieldForType(JBBPFieldArrayLong.class);
    assertEquals(2, longs.size());
    assertEquals(0xF7436510352367A0L, longs.getAsLong(0));
    assertEquals(0x323361CABE221230L, longs.getAsLong(1));
  }

  @Test
  public void testParse_NonFixedLongArray_LittleEndian() throws Exception {
    final JBBPFieldArrayLong longs = JBBPParser.prepare("<long[_];").parse(new byte[]{(byte) 0xF7, 0x43, 0x65, 0x10, 0x35, 0x23, 0x67, (byte) 0xA0, 0x32, 0x33, 0x61, (byte) 0xCA, (byte) 0xBE, 0x22, 0x12, 0x30}).findFieldForType(JBBPFieldArrayLong.class);
    assertEquals(2, longs.size());
    assertEquals(0xA0672335106543F7L, longs.getAsLong(0));
    assertEquals(0x301222BECA613332L, longs.getAsLong(1));
  }

  @Test
  public void testParse_VarBitArray() throws Exception {
    final JBBPFieldArrayBit array = JBBPParser.prepare("byte size; bit:4[size];").parse(new byte[]{8, 0x12, 0x34, 0x56, 0x78}).findFieldForType(JBBPFieldArrayBit.class);
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
    final JBBPFieldArrayBit array = JBBPParser.prepare("str {byte size;} bit:4[str.size];").parse(new byte[]{8, 0x12, 0x34, 0x56, 0x78}).findFieldForType(JBBPFieldArrayBit.class);
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
    final JBBPFieldArrayBit array = JBBPParser.prepare("bit:4[_];").parse(new byte[]{0x12, 0x34, 0x56, 0x78}).findFieldForType(JBBPFieldArrayBit.class);
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
    final JBBPFieldArrayBit array = JBBPParser.prepare("ubyte len; bit:4[2*len*6/2-4];").parse(new byte[]{2, 0x12, 0x34, 0x56, 0x78}).findFieldForType(JBBPFieldArrayBit.class);

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
    final JBBPFieldArrayByte array = JBBPParser.prepare("byte size; byte[size];").parse(new byte[]{4, 1, 2, 3, 4}).findFieldForType(JBBPFieldArrayByte.class);
    assertEquals(4, array.size());
    assertEquals(1, array.getAsInt(0));
    assertEquals(2, array.getAsInt(1));
    assertEquals(3, array.getAsInt(2));
    assertEquals(4, array.getAsInt(3));
  }

  @Test
  public void testParse_VarByteArrayWhenSizeInStruct() throws Exception {
    final JBBPFieldArrayByte array = JBBPParser.prepare("str {byte size;} byte[str.size];").parse(new byte[]{4, 1, 2, 3, 4}).findFieldForType(JBBPFieldArrayByte.class);
    assertEquals(4, array.size());
    assertEquals(1, array.getAsInt(0));
    assertEquals(2, array.getAsInt(1));
    assertEquals(3, array.getAsInt(2));
    assertEquals(4, array.getAsInt(3));
  }

  @Test
  public void testParse_WholeByteStream() throws Exception {
    final JBBPFieldArrayByte array = JBBPParser.prepare("byte[_];").parse(new byte[]{1, 2, 3, 4}).findFieldForType(JBBPFieldArrayByte.class);
    assertEquals(1, array.getAsInt(0));
    assertEquals(2, array.getAsInt(1));
    assertEquals(3, array.getAsInt(2));
    assertEquals(4, array.getAsInt(3));
  }

  @Test
  public void testParse_WholeByteStream_Empty() throws Exception {
    assertEquals(0, JBBPParser.prepare("byte[_];").parse(new byte[0]).findFieldForType(JBBPFieldArrayByte.class).size());
  }

  @Test
  public void testParse_Byte_ArrayForCalculatedLength() throws Exception {
    final JBBPFieldArrayByte array = JBBPParser.prepare("ubyte len; byte[len*6/2-2];").parse(new byte[]{2, 1, 2, 3, 4}).findFieldForType(JBBPFieldArrayByte.class);

    assertEquals(1, array.getAsInt(0));
    assertEquals(2, array.getAsInt(1));
    assertEquals(3, array.getAsInt(2));
    assertEquals(4, array.getAsInt(3));
  }

  @Test
  public void testParse_Boolean_ArrayForCalculatedLength() throws Exception {
    final JBBPFieldArrayBoolean array = JBBPParser.prepare("ubyte len; bool[len*6/2-2];").parse(new byte[]{2, 1, 2, 0, 4}).findFieldForType(JBBPFieldArrayBoolean.class);

    assertTrue(array.getAsBool(0));
    assertTrue(array.getAsBool(1));
    assertFalse(array.getAsBool(2));
    assertTrue(array.getAsBool(3));
  }

  @Test(expected = JBBPParsingException.class)
  public void testParse_NegativeCalculatedArrayLength() throws Exception {
    JBBPParser.prepare("ubyte len; byte[len-4];").parse(new byte[]{2, 1, 2, 3, 4});
  }

  @Test(expected = JBBPCompilationException.class)
  public void testParse_NegativeArrayLength() throws Exception {
    JBBPParser.prepare("ubyte len; byte[-2];").parse(new byte[]{2, 1, 2, 3, 4});
  }

  @Test(expected = EOFException.class)
  public void testParse_ErrorForLessDataThanExpected() throws Exception {
    JBBPParser.prepare("ubyte len; byte[5];").parse(new byte[]{2, 1, 2, 3, 4});
  }

  @Test
  public void testParse_WholeStructStream() throws Exception {
    final JBBPFieldArrayStruct array = JBBPParser.prepare("struct [_] {byte;}").parse(new byte[]{1, 2, 3, 4}).findFieldForType(JBBPFieldArrayStruct.class);
    assertEquals(4, array.size());
    assertEquals(1, array.getElementAt(0).findFieldForType(JBBPFieldByte.class).getAsInt());
    assertEquals(2, array.getElementAt(1).findFieldForType(JBBPFieldByte.class).getAsInt());
    assertEquals(3, array.getElementAt(2).findFieldForType(JBBPFieldByte.class).getAsInt());
    assertEquals(4, array.getElementAt(3).findFieldForType(JBBPFieldByte.class).getAsInt());
  }

  @Test
  public void testParse_BitArray_IgnoredForZeroLength() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte len; bit:4 [len]; ushort;").parse(new byte[]{0x0, 0x01, (byte) 0x02});
    assertEquals(0, parsed.findFieldForType(JBBPFieldArrayBit.class).size());
    assertEquals(0x0102, parsed.findFieldForType(JBBPFieldUShort.class).getAsInt());
  }

  @Test
  public void testParse_BoolArray_IgnoredForZeroLength() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte len; bool [len]; ushort;").parse(new byte[]{0x0, 0x01, (byte) 0x02});
    assertEquals(0, parsed.findFieldForType(JBBPFieldArrayBoolean.class).size());
    assertEquals(0x0102, parsed.findFieldForType(JBBPFieldUShort.class).getAsInt());
  }

  @Test
  public void testParse_ByteArray_IgnoredForZeroLength() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte len; byte [len]; ushort;").parse(new byte[]{0x0, 0x01, (byte) 0x02});
    assertEquals(0, parsed.findFieldForType(JBBPFieldArrayByte.class).size());
    assertEquals(0x0102, parsed.findFieldForType(JBBPFieldUShort.class).getAsInt());
  }

  @Test
  public void testParse_UByteArray_IgnoredForZeroLength() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte len; ubyte [len]; ushort;").parse(new byte[]{0x0, 0x01, (byte) 0x02});
    assertEquals(0, parsed.findFieldForType(JBBPFieldArrayUByte.class).size());
    assertEquals(0x0102, parsed.findFieldForType(JBBPFieldUShort.class).getAsInt());
  }

  @Test
  public void testParse_ShortArray_IgnoredForZeroLength() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte len; short [len]; ushort;").parse(new byte[]{0x0, 0x01, (byte) 0x02});
    assertEquals(0, parsed.findFieldForType(JBBPFieldArrayShort.class).size());
    assertEquals(0x0102, parsed.findFieldForType(JBBPFieldUShort.class).getAsInt());
  }

  @Test
  public void testParse_UShortArray_IgnoredForZeroLength() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte len; ushort [len]; ushort;").parse(new byte[]{0x0, 0x01, (byte) 0x02});
    assertEquals(0, parsed.findFieldForType(JBBPFieldArrayUShort.class).size());
    assertEquals(0x0102, parsed.findFieldForType(JBBPFieldUShort.class).getAsInt());
  }

  @Test
  public void testParse_IntArray_IgnoredForZeroLength() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte len; int [len]; ushort;").parse(new byte[]{0x0, 0x01, (byte) 0x02});
    assertEquals(0, parsed.findFieldForType(JBBPFieldArrayInt.class).size());
    assertEquals(0x0102, parsed.findFieldForType(JBBPFieldUShort.class).getAsInt());
  }

  @Test
  public void testParse_LongArray_IgnoredForZeroLength() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte len; long [len]; ushort;").parse(new byte[]{0x0, 0x01, (byte) 0x02});
    assertEquals(0, parsed.findFieldForType(JBBPFieldArrayLong.class).size());
    assertEquals(0x0102, parsed.findFieldForType(JBBPFieldUShort.class).getAsInt());
  }

  @Test
  public void testParse_StructArray_IgnoredForZeroLength() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte len; sss [len] { byte a; byte b; byte c;}  ushort;").parse(new byte[]{0x0, 0x01, (byte) 0x02});
    assertEquals(0, parsed.findFieldForType(JBBPFieldArrayStruct.class).size());
    assertEquals(0x0102, parsed.findFieldForType(JBBPFieldUShort.class).getAsInt());
  }

  @Test
  public void testParse_StructArray_FixedSize() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("sss [1] { byte a; byte b; byte c;}").parse(new byte[]{0x0, 0x01, (byte) 0x02});
    final JBBPFieldArrayStruct struct = parsed.findFieldForPathAndType("sss", JBBPFieldArrayStruct.class);
    assertEquals(1, struct.size());
    final JBBPFieldStruct readStruct = struct.getElementAt(0);
    assertEquals(0, readStruct.findFieldForNameAndType("a", JBBPFieldByte.class).getAsInt());
    assertEquals(1, readStruct.findFieldForNameAndType("b", JBBPFieldByte.class).getAsInt());
    assertEquals(2, readStruct.findFieldForNameAndType("c", JBBPFieldByte.class).getAsInt());
  }

  @Test
  public void testParse_StructArray_WholeStream() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("sss [_] { byte a; byte b; byte c;}").parse(new byte[]{0x0, 0x01, (byte) 0x02});
    final JBBPFieldArrayStruct struct = parsed.findFieldForPathAndType("sss", JBBPFieldArrayStruct.class);
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
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte len; sss [len]{ sss2[10]{ sss3{long;} sss4[45]{ushort; bool [11]; short; bit:4;} byte;}} byte end;").parse(new byte[]{0x00, 0x1F});
    assertEquals(0, parsed.findFieldForPathAndType("len", JBBPFieldByte.class).getAsInt());
    assertEquals(0, parsed.findFieldForPathAndType("sss", JBBPFieldArrayStruct.class).size());
    assertEquals(0x1F, parsed.findFieldForPathAndType("end", JBBPFieldByte.class).getAsInt());
  }

  @Test
  public void testParseWithStreamPositionMacros() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("int start; byte [$$] array; int end;").parse(new byte[]{1, 2, 3, 4, 0x1A, 0x1B, 0x1C, 0x1D, 4, 3, 2, 1});
    assertEquals(0x01020304, parsed.findFieldForPathAndType("start", JBBPFieldInt.class).getAsInt());
    assertArrayEquals(new byte[]{0x1A, 0x1B, 0x1C, 0x1D}, parsed.findFieldForPathAndType("array", JBBPFieldArrayByte.class).getArray());
    assertEquals(0x04030201, parsed.findFieldForPathAndType("end", JBBPFieldInt.class).getAsInt());
  }

  @Test
  public void testParseWithStreamPositionMacrosInExpressions() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("int start; byte [$$-2] array; byte [$$-4] array2; int end;").parse(new byte[]{1, 2, 3, 4, 0x1A, 0x1B, 0x1C, 0x1D, 4, 3, 2, 1});
    assertEquals(0x01020304, parsed.findFieldForPathAndType("start", JBBPFieldInt.class).getAsInt());
    assertArrayEquals(new byte[]{0x1A, 0x1B}, parsed.findFieldForPathAndType("array", JBBPFieldArrayByte.class).getArray());
    assertArrayEquals(new byte[]{0x1C, 0x1D}, parsed.findFieldForPathAndType("array2", JBBPFieldArrayByte.class).getArray());
    assertEquals(0x04030201, parsed.findFieldForPathAndType("end", JBBPFieldInt.class).getAsInt());
  }

  @Test
  public void testParseManyFieldsWithTheSameName() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte l; a { byte a; b { byte a; c { byte a;}}} byte [a.b.c.a] aa;").parse(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
    assertEquals(1, parsed.findFieldForPathAndType("l", JBBPFieldByte.class).getAsInt());
    assertEquals(2, parsed.findFieldForPathAndType("a.a", JBBPFieldByte.class).getAsInt());
    assertEquals(3, parsed.findFieldForPathAndType("a.b.a", JBBPFieldByte.class).getAsInt());
    assertEquals(4, parsed.findFieldForPathAndType("a.b.c.a", JBBPFieldByte.class).getAsInt());
    assertArrayEquals(new byte[]{5, 6, 7, 8}, parsed.findFieldForPathAndType("aa", JBBPFieldArrayByte.class).getArray());
  }

  @Test
  public void testParseScopeOfVisibilityOfFieldIfTheSameInStructBefore() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte a; b { byte a;} byte [a] aa;").parse(new byte[]{1, 2, 3});
    assertEquals(1, parsed.findFieldForPathAndType("a", JBBPFieldByte.class).getAsInt());
    assertEquals(2, parsed.findFieldForPathAndType("b.a", JBBPFieldByte.class).getAsInt());
    assertArrayEquals(new byte[]{3}, parsed.findFieldForPathAndType("aa", JBBPFieldArrayByte.class).getArray());
  }

  @Test
  public void testParseScopeOfVisibilityOfFieldInsideStructure() throws Exception {
    final JBBPFieldStruct parsed = JBBPParser.prepare("byte a; b { byte a; byte [a] d; } byte [a] aa;").parse(new byte[]{1, 2, 3, 4, 5, 6});
    assertEquals(1, parsed.findFieldForPathAndType("a", JBBPFieldByte.class).getAsInt());
    assertEquals(2, parsed.findFieldForPathAndType("b.a", JBBPFieldByte.class).getAsInt());
    assertArrayEquals(new byte[]{3, 4}, parsed.findFieldForPathAndType("b.d", JBBPFieldArrayByte.class).getArray());
    assertArrayEquals(new byte[]{5}, parsed.findFieldForPathAndType("aa", JBBPFieldArrayByte.class).getArray());
  }

  @Test
  public void testParseFixedSizeStructureArray() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("int val; inner [2] { byte a; byte b;}");

    final JBBPFieldStruct parsed = parser.parse(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
    assertEquals(0x01020304, parsed.findFieldForPathAndType("val", JBBPFieldInt.class).getAsInt());

    final JBBPFieldArrayStruct structArray = parsed.findFieldForNameAndType("inner", JBBPFieldArrayStruct.class);

    assertEquals(2, structArray.size());
  }

  @Test
  public void testParseWithResetCounter() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("struct[_]{reset$$; byte a; align:3; byte b;}");
    final JBBPFieldStruct parsed = parser.parse(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16});

    final JBBPFieldArrayStruct structArray = parsed.findFieldForNameAndType("struct", JBBPFieldArrayStruct.class);
    final byte[] etalon = new byte[]{1, 4, 5, 8, 9, 12, 13, 16};
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
    final JBBPFieldStruct parsed = parser.parse(new byte[]{0x3, 0x1F});
    assertEquals(3, parsed.findFieldForNameAndType("a", JBBPFieldBit.class).getAsInt());
    assertEquals(0x1F, parsed.findFieldForNameAndType("b", JBBPFieldByte.class).getAsInt());
    assertEquals(1, parser.getFinalStreamByteCounter());
  }

  @Test
  public void testParseArrayWithZeroLengthForResetCounter() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("byte; byte [$$] a; reset$$; byte[$$] b; byte [2] c;");
    final JBBPFieldStruct parsed = parser.parse(new byte[]{1, 2, 3, 4});
    final JBBPFieldArrayByte a = parsed.findFieldForNameAndType("a", JBBPFieldArrayByte.class);
    final JBBPFieldArrayByte b = parsed.findFieldForNameAndType("b", JBBPFieldArrayByte.class);
    final JBBPFieldArrayByte c = parsed.findFieldForNameAndType("c", JBBPFieldArrayByte.class);
    assertArrayEquals(new byte[]{2}, a.getArray());
    assertArrayEquals(new byte[0], b.getArray());
    assertArrayEquals(new byte[]{3, 4}, c.getArray());
  }

  @Test
  public void testGetFinalStreamByteCounter_Single_NoError() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("byte [_];");
    assertEquals(0L, parser.getFinalStreamByteCounter());
    parser.parse(new byte[2345]);
    assertEquals(2345L, parser.getFinalStreamByteCounter());
  }

  @Test
  public void testGetFinalStreamByteCounter_SequentlyFromTheSameStream_WithEOFAtTheEnd() throws Exception {
    final JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}));
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
    }
    catch (EOFException ex) {
      assertEquals(16, parser.getFinalStreamByteCounter());
    }
  }

  @Test(expected = JBBPParsingException.class)
  public void testParse_ErrorForNotAllReadFields() throws Exception {
    final JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4}));
    final JBBPParser parser = JBBPParser.prepare("int a; int b;");
    parser.parse(stream);
  }
  

  @Test
  public void testParse_NoErrorForIgnoreRemainingFieldsFlag() throws Exception {
    final JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4}));
    final JBBPParser parser = JBBPParser.prepare("int a; int b;",JBBPParser.FLAG_IGNORE_REMAINING_FIELDS_IF_EOF);
    final JBBPFieldStruct result = parser.parse(stream);
    assertEquals(1, result.getArray().length);
    assertEquals("a", result.getArray()[0].getFieldName());
    assertEquals(0x01020304, ((JBBPFieldInt)result.findFieldForName("a")).getAsInt());
  }
  
}
