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

package com.igormaznitsa.jbbp.utils;

import static com.igormaznitsa.jbbp.utils.JBBPUtils.findMaxStaticArraySize;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.igormaznitsa.jbbp.JBBPCustomFieldTypeProcessor;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPFieldTypeParameterContainer;
import com.igormaznitsa.jbbp.io.JBBPArraySizeLimiter;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.model.JBBPAbstractField;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class JBBPUtilsTest {

  @Test
  public void testFindMaxStaticArraySize() {
    assertEquals(0L, findMaxStaticArraySize("byte [_] a;", null));
    assertEquals(1L, findMaxStaticArraySize("byte [1] a;", null));
    assertEquals(0L, findMaxStaticArraySize("byte a; byte [a] b;", null));
    assertEquals(112L, findMaxStaticArraySize("byte a; byte [a] b; int [112];", null));
    assertEquals(1120L,
        findMaxStaticArraySize("byte a; byte [a] b; some [10] { int [112]; }", null));
    assertEquals(10230L,
        findMaxStaticArraySize("byte a; byte [a] b; some [10] { int [112]; byte [1023] d;}",
            null));
    assertEquals(10230L, findMaxStaticArraySize(
        "byte a; byte [a] b; some [10] { int [112]; j [_] {byte [1023] d;}}", null));
    assertEquals(65535L, findMaxStaticArraySize("a [1]{ b[1]{ c[_]{byte[65535] a;}}}", null));
    assertEquals(65535L,
        findMaxStaticArraySize("a [1]{ b[1]{ c[_]{int [128] l; var[65535] a;}}}", null));
    assertEquals(65535L,
        findMaxStaticArraySize("a [1]{ b[1]{ c[_]{int [128] l; var[65535] a;}}}", null));
    assertEquals(65535L,
        findMaxStaticArraySize("a [1]{ b[1]{ c[_]{int [128] l; stringj[65535] a;}}}", null));
    assertEquals(65535L,
        findMaxStaticArraySize("a [1]{ b[1]{ c[_]{int [128] l; floatj[65535] a;}}}", null));
    assertEquals(273948L,
        findMaxStaticArraySize(
            "a [1]{ b[1]{ c[_]{int [128] l; str [222] { ubyte [1234] p; } doublej[65535] a;}}}",
            null));
    assertEquals(273948L,
        findMaxStaticArraySize(
            "a [1]{ b[1]{ c[_]{int [128] l; str [222] { bit:3 [1234] p; } doublej[65535] a;}}}",
            null));
    assertEquals(65535L,
        findMaxStaticArraySize(
            "a [1]{ b[1]{ c[_]{int [128] l; str [222] { val:3 p; } doublej[65535] a;}}}", null));
    assertEquals(288217182213504000L,
        findMaxStaticArraySize(
            "a [65535] { b [65535] { c [65535] { byte [1024]d; }}}", null));
    assertEquals(65535, findMaxStaticArraySize(
        "a [1]{ b[1]{ lala [65534] { long s; } c[1]{int [128] l; cus[65535] a;}}}",
        new JBBPCustomFieldTypeProcessor() {
          @Override
          public String[] getCustomFieldTypes() {
            return new String[] {"cus"};
          }

          @Override
          public boolean isAllowed(JBBPFieldTypeParameterContainer fieldType, String fieldName,
                                   int extraData, boolean isArray) {
            return "cus".equals(fieldType.getTypeName());
          }

          @Override
          public JBBPAbstractField readCustomFieldType(JBBPBitInputStream in, JBBPBitOrder bitOrder,
                                                       int parserFlags,
                                                       JBBPFieldTypeParameterContainer customTypeFieldInfo,
                                                       JBBPNamedFieldInfo fieldName, int extraData,
                                                       boolean readWholeStream, int arrayLength,
                                                       JBBPArraySizeLimiter arraySizeLimiter) {
            throw new UnsupportedOperationException();
          }
        }));
    assertThrows(ArithmeticException.class,
        () -> findMaxStaticArraySize(
            "a [_]{ b[65535]{ c[65535]{byte[65535] { byte [10000000] d;}}}}", null));
  }

  @Test
  public void testUtf8EncdeDecode() {
    assertEquals("78634двлфодйукйДЛОД wdf",
        JBBPUtils.utf8ToStr(JBBPUtils.strToUtf8("78634двлфодйукйДЛОД wdf")));
  }

  @Test
  public void testRemoveTrailingZeros() throws Exception {
    assertNull(JBBPUtils.removeTrailingZeros(null));
    assertEquals("", JBBPUtils.removeTrailingZeros(""));
    assertEquals("abcdf", JBBPUtils.removeTrailingZeros("abcdf"));
    assertEquals("000000abcdf", JBBPUtils.removeTrailingZeros("000000abcdf"));
    assertEquals("000000a00000bcdf", JBBPUtils.removeTrailingZeros("000000a00000bcdf0000000"));
    assertEquals("0", JBBPUtils.removeTrailingZeros("0000000"));
    assertEquals("0", JBBPUtils.removeTrailingZeros("0"));
    assertEquals("0     ", JBBPUtils.removeTrailingZeros("0     "));
    assertEquals(" 000000     ", JBBPUtils.removeTrailingZeros(" 000000     "));
    assertEquals("10.2", JBBPUtils.removeTrailingZeros("10.20000"));
    assertEquals("1", JBBPUtils.removeTrailingZeros("100"));
  }


  @Test
  public void testRemoveLeadingZeros() throws Exception {
    assertNull(JBBPUtils.removeLeadingZeros(null));
    assertEquals("", JBBPUtils.removeLeadingZeros(""));
    assertEquals("abcdf", JBBPUtils.removeLeadingZeros("abcdf"));
    assertEquals("abcdf", JBBPUtils.removeLeadingZeros("000000abcdf"));
    assertEquals("a00000bcdf0000000", JBBPUtils.removeLeadingZeros("000000a00000bcdf0000000"));
    assertEquals("0", JBBPUtils.removeLeadingZeros("0000000"));
    assertEquals("0", JBBPUtils.removeLeadingZeros("0"));
    assertEquals("     ", JBBPUtils.removeLeadingZeros("0     "));
    assertEquals(" 000000     ", JBBPUtils.removeLeadingZeros(" 000000     "));
  }

  @Test
  public void testULong2Str_UnsignedHex() throws Exception {
    assertEquals("12345678ABCD", JBBPUtils.ulong2str(0x12345678ABCDL, 16, null));
    assertEquals("7FFFFFFFFFFFFFFF", JBBPUtils.ulong2str(0x7FFFFFFFFFFFFFFFL, 16, null));
    assertEquals("8000000000000000", JBBPUtils.ulong2str(0x8000000000000000L, 16, null));
    assertEquals("8FFFFFFFFFFFFFFF", JBBPUtils.ulong2str(0x8FFFFFFFFFFFFFFFL, 16, null));
    assertEquals("8100000000000000", JBBPUtils.ulong2str(0x8100000000000000L, 16, null));
    assertEquals("F23418824AB12342", JBBPUtils.ulong2str(0xF23418824AB12342L, 16, null));
  }

  @Test
  public void testULong2Str_UnsignedDec() throws Exception {
    assertEquals("20015998348237", JBBPUtils.ulong2str(0x12345678ABCDL, 10, null));
    assertEquals("9223372036854775807", JBBPUtils.ulong2str(9223372036854775807L, 10, null));
    assertEquals("9223372036853827875", JBBPUtils.ulong2str(0x7FFFFFFFFFF18923L, 10, null));
    assertEquals("9223372036854775808", JBBPUtils.ulong2str(0x8000000000000000L, 10, null));
    assertEquals("10376293541461622783", JBBPUtils.ulong2str(0x8FFFFFFFFFFFFFFFL, 10, null));
    assertEquals("9295429630892703744", JBBPUtils.ulong2str(0x8100000000000000L, 10, null));
    assertEquals("17452601403845452610", JBBPUtils.ulong2str(0xF23418824AB12342L, 10, null));
  }


  @Test
  public void testPackIntToByteArray() {
    for (int i = 0; i < 0x80; i++) {
      final byte[] array = JBBPUtils.packInt(i);
      assertEquals(1, array.length);
      assertEquals(i, array[0] & 0xFF);
    }

    final Random rnd = new Random(1234);

    for (int i = 0; i < 1000; i++) {
      final int generated = rnd.nextInt(0x7FFFFFFF);
      final byte[] array = JBBPUtils.packInt(generated);

      if ((generated & 0xFFFFFF80) == 0) {
        assertEquals(1, array.length);
        assertEquals(i, array[0] & 0xFF);
      } else if ((generated & 0xFFFF0000) == 0) {
        assertEquals(3, array.length);
        assertEquals(0x80, array[0] & 0xFF);
        assertEquals(generated >>> 8, array[1] & 0xFF);
        assertEquals(generated & 0xFF, array[2] & 0xFF);
      } else {
        assertEquals(5, array.length);
        assertEquals(0x81, array[0] & 0xFF);
        assertEquals((generated >>> 24) & 0xFF, array[1] & 0xFF);
        assertEquals((generated >>> 16) & 0xFF, array[2] & 0xFF);
        assertEquals((generated >>> 8) & 0xFF, array[3] & 0xFF);
        assertEquals(generated & 0xFF, array[4] & 0xFF);
      }
    }

  }

  @Test
  public void testCloseQuetly() {
    try {
      JBBPUtils.closeQuietly(null);
      JBBPUtils.closeQuietly(new ByteArrayInputStream(new byte[10]));

      final InputStream closed = new ByteArrayInputStream(new byte[10]);
      closed.close();
      JBBPUtils.closeQuietly(closed);
    } catch (Exception ex) {
      fail("Must not throw any exception");
    }
  }

  @Test
  public void testIsNumber() {
    assertFalse(JBBPUtils.isNumber(null));
    assertFalse(JBBPUtils.isNumber(""));
    assertFalse(JBBPUtils.isNumber("12837921739821739203928103802198383742984732a"));
    assertFalse(JBBPUtils.isNumber("a12837921739821739203928103802198383742984732"));
    assertTrue(JBBPUtils.isNumber("12837921739821739203928103802198383742984732"));
    assertTrue(JBBPUtils.isNumber("-12837921739821739203928103802198383742984732"));
  }

  @Test
  public void testUnpackInt_NPEForArrayIsNull() {
    assertThrows(NullPointerException.class, () -> JBBPUtils.unpackInt(null, new JBBPIntCounter()));
  }

  @Test
  public void testUnpackInt_IAEForWrongPrefix() {
    assertThrows(IllegalArgumentException.class,
        () -> JBBPUtils.unpackInt(new byte[] {(byte) 0xAA, 0, 0, 0, 0, 0}, new JBBPIntCounter()));
  }

  @Test
  public void testPackUnpackIntFromByteArray() {
    final byte[] array = new byte[5];

    final JBBPIntCounter pos = new JBBPIntCounter();

    int counter1 = 0;
    int counter2 = 0;
    int counter3 = 0;

    final int[] etalons =
        new int[] {0, -1, -89, 234, 123124, 1223112, 34323, Integer.MIN_VALUE, Integer.MAX_VALUE};

    for (final int generated : etalons) {
      pos.set(0);
      final int len = JBBPUtils.packInt(array, pos, generated);

      if ((generated & 0xFFFFFF80) == 0) {
        assertEquals(1, len);
        counter1++;
      } else if ((generated & 0xFFFF0000) == 0) {
        assertEquals(3, len);
        counter2++;
      } else {
        assertEquals(5, len);
        counter3++;
      }

      assertEquals(pos.get(), len);

      pos.set(0);
      assertEquals(generated, JBBPUtils.unpackInt(array, pos));
      assertEquals(pos.get(), len);
    }

    assertTrue(counter1 > 0);
    assertTrue(counter2 > 0);
    assertTrue(counter3 > 0);
  }

  @Test
  public void testArray2Hex() {
    assertNull(JBBPUtils.array2hex(null));
    assertEquals("[0x01, 0x02, 0x03, 0xFF]",
        JBBPUtils.array2hex(new byte[] {1, 2, 3, (byte) 0xFF}));
  }

  @Test
  public void testArray2Oct() {
    assertNull(JBBPUtils.array2hex(null));
    assertEquals("[0o001, 0o002, 0o003, 0o377]",
        JBBPUtils.array2oct(new byte[] {1, 2, 3, (byte) 0xFF}));
  }

  @Test
  public void testArray2Bin() {
    assertNull(JBBPUtils.array2bin(null));
    assertEquals("[0b00000001, 0b00000010, 0b00000011, 0b11111111]",
        JBBPUtils.array2bin(new byte[] {1, 2, 3, (byte) 0xFF}));
  }

  @Test
  public void testReverseBitsInByte() {
    for (int i = 0; i < 256; i++) {
      int etalon = 0;
      int a = i;
      for (int y = 0; y < 8; y++) {
        etalon = (etalon << 1) | (a & 0x1);
        a >>= 1;
      }

      assertEquals((byte) etalon, JBBPUtils.reverseBitsInByte((byte) i), "Value is " + i);
    }
  }

  @Test
  public void testReverseBitsInByte_DefinedNumber() {
    assertEquals((byte) 1, JBBPUtils.reverseBitsInByte(JBBPBitNumber.BITS_1, (byte) 0xFF));
    assertEquals((byte) 0, JBBPUtils.reverseBitsInByte(JBBPBitNumber.BITS_1, (byte) 0x00));
    assertEquals((byte) 6, JBBPUtils.reverseBitsInByte(JBBPBitNumber.BITS_3, (byte) 0x63));
    assertEquals((byte) 0x31, JBBPUtils.reverseBitsInByte(JBBPBitNumber.BITS_6, (byte) 0x63));
  }

  @Test
  public void testBin2Str() {
    assertNull(JBBPUtils.bin2str(null, true));
    assertNull(JBBPUtils.bin2str(null, false));

    assertEquals("01010101 10101010", JBBPUtils.bin2str(new byte[] {0x55, (byte) 0xAA}, true));
    assertEquals("0101010110101010", JBBPUtils.bin2str(new byte[] {0x55, (byte) 0xAA}, false));
    assertEquals("00001001", JBBPUtils.bin2str(new byte[] {0x9}, false));
    assertEquals("1010101001010101",
        JBBPUtils.bin2str(new byte[] {0x55, (byte) 0xAA}, JBBPBitOrder.MSB0, false));
    assertEquals("0101010110101010", JBBPUtils.bin2str(new byte[] {0x55, (byte) 0xAA}));
  }

  @Test
  public void testStr2Bin_Default() {
    assertEquals(0, JBBPUtils.str2bin(null).length);

    assertArrayEquals(new byte[] {(byte) 0x80}, JBBPUtils.str2bin("10000000"));
    assertArrayEquals(new byte[] {(byte) 0x01}, JBBPUtils.str2bin("1"));
    assertArrayEquals(new byte[] {(byte) 0x80, 0x01}, JBBPUtils.str2bin("10000000X00x0Zz1"));
    assertArrayEquals(new byte[] {(byte) 0x80, 0x01, 0x07},
        JBBPUtils.str2bin("10000000000000010111"));
    assertArrayEquals(new byte[] {(byte) 0x80, 0x01, 0x07},
        JBBPUtils.str2bin("10000000_00000001_0111"));

    try {
      JBBPUtils.str2bin("10001021");
      fail("Must throw IAE");
    } catch (IllegalArgumentException ex) {

    }
  }

  @Test
  public void testStr2Bin_LSB0() {
    assertEquals(0, JBBPUtils.str2bin(null, JBBPBitOrder.LSB0).length);

    assertArrayEquals(new byte[] {(byte) 0x80}, JBBPUtils.str2bin("10000000", JBBPBitOrder.LSB0));
    assertArrayEquals(new byte[] {(byte) 0x01}, JBBPUtils.str2bin("1", JBBPBitOrder.LSB0));
    assertArrayEquals(new byte[] {(byte) 0x01}, JBBPUtils.str2bin("00000001", JBBPBitOrder.LSB0));
    assertArrayEquals(new byte[] {(byte) 0x80, (byte) 0x01},
        JBBPUtils.str2bin("10000000X00x0Zz1", JBBPBitOrder.LSB0));
    assertArrayEquals(new byte[] {(byte) 0x80, (byte) 0x01, (byte) 0x07},
        JBBPUtils.str2bin("10000000000000010111", JBBPBitOrder.LSB0));
    assertArrayEquals(new byte[] {(byte) 0x80, (byte) 0x01, (byte) 0x07},
        JBBPUtils.str2bin("10000000_00000001_0111", JBBPBitOrder.LSB0));

    try {
      JBBPUtils.str2bin("10001021", JBBPBitOrder.MSB0);
      fail("Must throw IAE");
    } catch (IllegalArgumentException ex) {

    }
  }

  @Test
  public void testStr2Bin_LSB0_1bitShift() {
    final byte[] array =
        JBBPUtils.str2bin("0 11111111 01010101 00011000 00000001", JBBPBitOrder.LSB0);

    assertArrayEquals(new byte[] {(byte) 0x7F, (byte) 0xAA, (byte) 0x8C, (byte) 0x0, (byte) 0x01},
        array);

  }

  @Test
  public void testStr2Bin_MSB() {
    assertEquals(0, JBBPUtils.str2bin(null, JBBPBitOrder.MSB0).length);

    assertArrayEquals(new byte[] {(byte) 0x01}, JBBPUtils.str2bin("1", JBBPBitOrder.MSB0));
    assertArrayEquals(new byte[] {(byte) 0x80}, JBBPUtils.str2bin("00000001", JBBPBitOrder.MSB0));
    assertArrayEquals(new byte[] {(byte) 0x01}, JBBPUtils.str2bin("10000000", JBBPBitOrder.MSB0));
    assertArrayEquals(new byte[] {(byte) 0xA9}, JBBPUtils.str2bin("10010101", JBBPBitOrder.MSB0));
    assertArrayEquals(new byte[] {(byte) 0x01, (byte) 0x80},
        JBBPUtils.str2bin("10000000X00x0Zz1", JBBPBitOrder.MSB0));
    assertArrayEquals(new byte[] {(byte) 0x01, (byte) 0x80, (byte) 0x0E},
        JBBPUtils.str2bin("1000000000000001 0111", JBBPBitOrder.MSB0));
    assertArrayEquals(new byte[] {(byte) 0x01, (byte) 0x80, (byte) 0x0E},
        JBBPUtils.str2bin("10000000_00000001_0111", JBBPBitOrder.MSB0));
    assertArrayEquals(new byte[] {(byte) 0x03, (byte) 0x00, (byte) 0x01, (byte) 0x01},
        JBBPUtils.str2bin("1_10000000_00000001_00000001", JBBPBitOrder.MSB0));

    try {
      JBBPUtils.str2bin("10001021", JBBPBitOrder.MSB0);
      fail("Must throw IAE");
    } catch (IllegalArgumentException ex) {

    }
  }

  @Test
  public void testSplitString() {
    assertArrayEquals(new String[] {""}, JBBPUtils.splitString("", '.'));
    assertArrayEquals(new String[] {"aaa"}, JBBPUtils.splitString("aaa", '.'));
    assertArrayEquals(new String[] {"aaa", "bbb"}, JBBPUtils.splitString("aaa.bbb", '.'));
    assertArrayEquals(new String[] {"aaa", "bbb", ""}, JBBPUtils.splitString("aaa.bbb.", '.'));
    assertArrayEquals(new String[] {"", ""}, JBBPUtils.splitString(".", '.'));
  }

  @Test
  public void testAssertNotNull() {
    JBBPUtils.assertNotNull(new Object(), "test");

    final String message = "Test message";
    try {
      JBBPUtils.assertNotNull(null, message);
      fail("Must throw NPE");
    } catch (NullPointerException ex) {
      assertSame(message, ex.getMessage());
    }

    try {
      JBBPUtils.assertNotNull(null, null);
      fail("Must throw NPE");
    } catch (NullPointerException ex) {
      assertNotNull(message, ex.getMessage());
    }
  }

  @Test
  public void testReverseArray() {
    assertNull(JBBPUtils.reverseArray(null));
    final byte[] empty = new byte[0];
    assertSame(empty, JBBPUtils.reverseArray(empty));

    assertArrayEquals(new byte[] {1}, JBBPUtils.reverseArray(new byte[] {1}));
    assertArrayEquals(new byte[] {2, 1}, JBBPUtils.reverseArray(new byte[] {1, 2}));
    assertArrayEquals(new byte[] {5, 4, 3, 2, 1},
        JBBPUtils.reverseArray(new byte[] {1, 2, 3, 4, 5}));
    assertArrayEquals(new byte[] {6, 5, 4, 3, 2, 1},
        JBBPUtils.reverseArray(new byte[] {1, 2, 3, 4, 5, 6}));
  }

  @Test
  public void testSplitInteger() {
    byte[] buff = null;
    assertArrayEquals(new byte[] {1, 2, 3, 4}, JBBPUtils.splitInteger(0x01020304, false, buff));

    buff = new byte[2];
    assertArrayEquals(new byte[] {1, 2, 3, 4}, JBBPUtils.splitInteger(0x01020304, false, buff));

    buff = new byte[4];
    assertArrayEquals(new byte[] {1, 2, 3, 4}, JBBPUtils.splitInteger(0x01020304, false, buff));

    buff = new byte[8];
    assertArrayEquals(new byte[] {1, 2, 3, 4, 0, 0, 0, 0},
        JBBPUtils.splitInteger(0x01020304, false, buff));

    buff = null;
    assertArrayEquals(new byte[] {4, 3, 2, 1}, JBBPUtils.splitInteger(0x01020304, true, buff));

    buff = new byte[2];
    assertArrayEquals(new byte[] {4, 3, 2, 1}, JBBPUtils.splitInteger(0x01020304, true, buff));

    buff = new byte[4];
    assertArrayEquals(new byte[] {4, 3, 2, 1}, JBBPUtils.splitInteger(0x01020304, true, buff));

    buff = new byte[8];
    assertArrayEquals(new byte[] {4, 3, 2, 1, 0, 0, 0, 0},
        JBBPUtils.splitInteger(0x01020304, true, buff));

  }

  @Test
  public void testSplitLong() {
    byte[] buff = null;
    assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6, 7, 8},
        JBBPUtils.splitLong(0x0102030405060708L, false, buff));

    buff = new byte[2];
    assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6, 7, 8},
        JBBPUtils.splitLong(0x0102030405060708L, false, buff));

    buff = new byte[8];
    assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6, 7, 8},
        JBBPUtils.splitLong(0x0102030405060708L, false, buff));

    buff = new byte[10];
    assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 0, 0},
        JBBPUtils.splitLong(0x0102030405060708L, false, buff));

    buff = null;
    assertArrayEquals(new byte[] {8, 7, 6, 5, 4, 3, 2, 1},
        JBBPUtils.splitLong(0x0102030405060708L, true, buff));

    buff = new byte[2];
    assertArrayEquals(new byte[] {8, 7, 6, 5, 4, 3, 2, 1},
        JBBPUtils.splitLong(0x0102030405060708L, true, buff));

    buff = new byte[8];
    assertArrayEquals(new byte[] {8, 7, 6, 5, 4, 3, 2, 1},
        JBBPUtils.splitLong(0x0102030405060708L, true, buff));

    buff = new byte[10];
    assertArrayEquals(new byte[] {8, 7, 6, 5, 4, 3, 2, 1, 0, 0},
        JBBPUtils.splitLong(0x0102030405060708L, true, buff));
  }

  @Test
  public void testConcat() {
    assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, JBBPUtils
        .concat(new byte[] {1, 2, 3, 4}, new byte[] {5}, new byte[] {6, 7, 8, 9}, new byte[0],
            new byte[] {10}));
  }

  @Test
  public void testReverseByteOrder_ErrorForZeroByteNumber() {
    assertThrows(IllegalArgumentException.class, () -> JBBPUtils.reverseByteOrder(1234, 0));
  }

  @Test
  public void testReverseByteOrder_ErrorForTooBigByteNumber() {
    assertThrows(IllegalArgumentException.class, () -> JBBPUtils.reverseByteOrder(1234, 9));
  }

  @Test
  public void testReverseByteOrder() {
    assertEquals(0x0000000000000012L, JBBPUtils.reverseByteOrder(0x0000000000000012L, 1));
    assertEquals(0x0000000000003412L, JBBPUtils.reverseByteOrder(0x0000000000001234L, 2));
    assertEquals(0x0000000000563412L, JBBPUtils.reverseByteOrder(0x0000000000123456L, 3));
    assertEquals(0x0000000078563412L, JBBPUtils.reverseByteOrder(0x0000000012345678L, 4));
    assertEquals(0x0000009A78563412L, JBBPUtils.reverseByteOrder(0x000000123456789AL, 5));
    assertEquals(0x0000BC9A78563412L, JBBPUtils.reverseByteOrder(0x0000123456789ABCL, 6));
    assertEquals(0x00DEBC9A78563412L, JBBPUtils.reverseByteOrder(0x00123456789ABCDEL, 7));
    assertEquals(0xF1DEBC9A78563412L, JBBPUtils.reverseByteOrder(0x123456789ABCDEF1L, 8));
  }

  @Test
  public void testEnsureMinTextLength() {
    assertEquals("..........text1", JBBPUtils.ensureMinTextLength("text1", 15, '.', 0));
    assertEquals("text1..........", JBBPUtils.ensureMinTextLength("text1", 15, '.', 1));
    assertEquals("..text1...", JBBPUtils.ensureMinTextLength("text1", 10, '.', -1));
  }

  @Test
  public void testArrayStartsWith_NPE() {
    try {
      JBBPUtils.arrayStartsWith(null, new byte[11]);
      fail("Must be NPE");
    } catch (NullPointerException ex) {

    }
    try {
      JBBPUtils.arrayStartsWith(new byte[11], null);
      fail("Must be NPE");
    } catch (NullPointerException ex) {

    }
    try {
      JBBPUtils.arrayStartsWith(null, null);
      fail("Must be NPE");
    } catch (NullPointerException ex) {

    }
  }

  @Test
  public void testArrayStartsWith_EmptyStart() {
    assertTrue(JBBPUtils.arrayStartsWith(new byte[0], new byte[0]));
    assertTrue(JBBPUtils.arrayStartsWith(new byte[111], new byte[0]));
  }

  @Test
  public void testArrayStartsWith_TooLongSubstring() {
    assertFalse(JBBPUtils.arrayStartsWith(new byte[] {1, 2}, new byte[] {1, 2, 3}));
  }

  @Test
  public void testArrayStartsWith_TheSameLength() {
    assertTrue(JBBPUtils.arrayStartsWith(new byte[] {1, 2}, new byte[] {1, 2}));
  }

  @Test
  public void testArrayStartsWith_Found() {
    assertTrue(JBBPUtils.arrayStartsWith(new byte[] {1, 2, 3, 4}, new byte[] {1, 2}));
  }

  @Test
  public void testArrayStartsWith_NotFound() {
    assertFalse(JBBPUtils.arrayStartsWith(new byte[] {1, 2, 3, 4}, new byte[] {1, 2, 4}));
  }

  @Test
  public void testArrayEndsWith_NPE() {
    try {
      JBBPUtils.arrayEndsWith(null, new byte[11]);
      fail("Must be NPE");
    } catch (NullPointerException ex) {

    }
    try {
      JBBPUtils.arrayEndsWith(new byte[11], null);
      fail("Must be NPE");
    } catch (NullPointerException ex) {

    }
    try {
      JBBPUtils.arrayEndsWith(null, null);
      fail("Must be NPE");
    } catch (NullPointerException ex) {

    }
  }

  @Test
  public void testArrayEndsWith_EmptyEnd() {
    assertTrue(JBBPUtils.arrayEndsWith(new byte[0], new byte[0]));
    assertTrue(JBBPUtils.arrayEndsWith(new byte[111], new byte[0]));
  }

  @Test
  public void testArrayEndsWith_TooLongSubstring() {
    assertFalse(JBBPUtils.arrayEndsWith(new byte[] {1, 2}, new byte[] {1, 2, 3}));
  }

  @Test
  public void testArrayEndsWith_TheSameLength() {
    assertTrue(JBBPUtils.arrayEndsWith(new byte[] {1, 2}, new byte[] {1, 2}));
  }

  @Test
  public void testArrayEndsWith_Found() {
    assertTrue(JBBPUtils.arrayEndsWith(new byte[] {1, 2, 3, 4}, new byte[] {2, 3, 4}));
  }

  @Test
  public void testArrayEndsWith_NotFound() {
    assertFalse(JBBPUtils.arrayEndsWith(new byte[] {1, 2, 3, 4}, new byte[] {2, 4}));
  }

  @Test
  public void testGenerateMask() {
    assertEquals(0, JBBPUtils.makeMask(0));
    assertEquals(1, JBBPUtils.makeMask(1));
    assertEquals(3, JBBPUtils.makeMask(2));
    assertEquals(0x7F, JBBPUtils.makeMask(100));
    assertEquals(0xFFFF, JBBPUtils.makeMask(65535));
  }

  @Test
  public void testTraceData_Defaults() throws Exception {
    final byte[] testData = new byte[211];
    for (int i = 0; i < 111; i++) {
      testData[i] = (byte) i;
    }
    final InputStream stream = new ByteArrayInputStream(testData);
    final ByteArrayOutputStream outArray = new ByteArrayOutputStream();
    final PrintStream out = new PrintStream(outArray, true, StandardCharsets.UTF_8);
    JBBPUtils.traceData(stream, out);
    out.close();
    final String asString =
        outArray.toString(StandardCharsets.UTF_8).replace("\r\n", "\n")
            .replace("\r", "\n");
    assertEquals(
        "00000000 00 01 02 03 | 04 05 06 07 | 08 09 0A 0B | 0C 0D 0E 0F | 10 11 12 13 | 14 15 16 17 | 18 19 1A 1B | 1C 1D 1E 1F ................................\n"
            +
            "00000020 20 21 22 23 | 24 25 26 27 | 28 29 2A 2B | 2C 2D 2E 2F | 30 31 32 33 | 34 35 36 37 | 38 39 3A 3B | 3C 3D 3E 3F  !\"#$%&'()*+,-./0123456789:;<=>?\n"
            +
            "00000040 40 41 42 43 | 44 45 46 47 | 48 49 4A 4B | 4C 4D 4E 4F | 50 51 52 53 | 54 55 56 57 | 58 59 5A 5B | 5C 5D 5E 5F @ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_\n"
            +
            "00000060 60 61 62 63 | 64 65 66 67 | 68 69 6A 6B | 6C 6D 6E 00 | 00 00 00 00 | 00 00 00 00 | 00 00 00 00 | 00 00 00 00 `abcdefghijklmn.................\n"
            +
            "00000080 00 00 00 00 | 00 00 00 00 | 00 00 00 00 | 00 00 00 00 | 00 00 00 00 | 00 00 00 00 | 00 00 00 00 | 00 00 00 00 ................................\n"
            +
            "000000A0 00 00 00 00 | 00 00 00 00 | 00 00 00 00 | 00 00 00 00 | 00 00 00 00 | 00 00 00 00 | 00 00 00 00 | 00 00 00 00 ................................\n"
            +
            "000000C0 00 00 00 00 | 00 00 00 00 | 00 00 00 00 | 00 00 00 00 | 00 00 00 -- | -- -- -- -- | -- -- -- -- | -- -- -- -- ................................\n",
        asString);
  }

  @Test
  public void testTraceData_CustomWithChars() throws Exception {
    final byte[] testData = new byte[211];
    for (int i = 0; i < 111; i++) {
      testData[i] = (byte) i;
    }
    final InputStream stream = new ByteArrayInputStream(testData);
    final ByteArrayOutputStream outArray = new ByteArrayOutputStream();
    final PrintStream out = new PrintStream(outArray, true, StandardCharsets.UTF_8);
    JBBPUtils.traceData(stream, 8, 4, "#", "_", "$", "Z", '&', true, out);
    out.close();
    final String asString =
        new String(outArray.toByteArray(), StandardCharsets.UTF_8).replace("\r\n", "\n")
            .replace("\r", "\n");
    assertEquals(
        "00000000#00_01_02_03_04_05_06_07$08_09_0A_0B_0C_0D_0E_0F$10_11_12_13_14_15_16_17$18_19_1A_1B_1C_1D_1E_1FZ&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&\n"
            +
            "00000020#20_21_22_23_24_25_26_27$28_29_2A_2B_2C_2D_2E_2F$30_31_32_33_34_35_36_37$38_39_3A_3B_3C_3D_3E_3FZ !\"#$%&'()*+,-./0123456789:;<=>?\n"
            +
            "00000040#40_41_42_43_44_45_46_47$48_49_4A_4B_4C_4D_4E_4F$50_51_52_53_54_55_56_57$58_59_5A_5B_5C_5D_5E_5FZ@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_\n"
            +
            "00000060#60_61_62_63_64_65_66_67$68_69_6A_6B_6C_6D_6E_00$00_00_00_00_00_00_00_00$00_00_00_00_00_00_00_00Z`abcdefghijklmn&&&&&&&&&&&&&&&&&\n"
            +
            "00000080#00_00_00_00_00_00_00_00$00_00_00_00_00_00_00_00$00_00_00_00_00_00_00_00$00_00_00_00_00_00_00_00Z&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&\n"
            +
            "000000A0#00_00_00_00_00_00_00_00$00_00_00_00_00_00_00_00$00_00_00_00_00_00_00_00$00_00_00_00_00_00_00_00Z&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&\n"
            +
            "000000C0#00_00_00_00_00_00_00_00$00_00_00_00_00_00_00_00$00_00_00_--_--_--_--_--$--_--_--_--_--_--_--_--Z&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&\n",
        asString);
  }

  @Test
  public void testTraceData_CustomNoChars() throws Exception {
    final byte[] testData = new byte[211];
    for (int i = 0; i < 111; i++) {
      testData[i] = (byte) i;
    }
    final InputStream stream = new ByteArrayInputStream(testData);
    final ByteArrayOutputStream outArray = new ByteArrayOutputStream();
    final PrintStream out = new PrintStream(outArray, true, StandardCharsets.UTF_8);
    JBBPUtils.traceData(stream, 8, 4, "#", "_", "$", "Z", '&', false, out);
    out.close();
    final String asString =
        new String(outArray.toByteArray(), StandardCharsets.UTF_8).replace("\r\n", "\n")
            .replace("\r", "\n");
    assertEquals(
        "00000000#00_01_02_03_04_05_06_07$08_09_0A_0B_0C_0D_0E_0F$10_11_12_13_14_15_16_17$18_19_1A_1B_1C_1D_1E_1F\n"
            +
            "00000020#20_21_22_23_24_25_26_27$28_29_2A_2B_2C_2D_2E_2F$30_31_32_33_34_35_36_37$38_39_3A_3B_3C_3D_3E_3F\n"
            +
            "00000040#40_41_42_43_44_45_46_47$48_49_4A_4B_4C_4D_4E_4F$50_51_52_53_54_55_56_57$58_59_5A_5B_5C_5D_5E_5F\n"
            +
            "00000060#60_61_62_63_64_65_66_67$68_69_6A_6B_6C_6D_6E_00$00_00_00_00_00_00_00_00$00_00_00_00_00_00_00_00\n"
            +
            "00000080#00_00_00_00_00_00_00_00$00_00_00_00_00_00_00_00$00_00_00_00_00_00_00_00$00_00_00_00_00_00_00_00\n"
            +
            "000000A0#00_00_00_00_00_00_00_00$00_00_00_00_00_00_00_00$00_00_00_00_00_00_00_00$00_00_00_00_00_00_00_00\n"
            +
            "000000C0#00_00_00_00_00_00_00_00$00_00_00_00_00_00_00_00$00_00_00_--_--_--_--_--$--_--_--_--_--_--_--_--\n",
        asString);
  }
}
