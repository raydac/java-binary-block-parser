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
package com.igormaznitsa.jbbp.utils;

import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import java.io.*;
import java.util.Random;
import static org.junit.Assert.*;
import org.junit.Test;

public class JBBPUtilsTest {

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
      }
      else if ((generated & 0xFFFF0000) == 0) {
        assertEquals(3, array.length);
        assertEquals(0x80, array[0] & 0xFF);
        assertEquals(generated >>> 8, array[1] & 0xFF);
        assertEquals(generated & 0xFF, array[2] & 0xFF);
      }
      else {
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
  public void testCloseQuetly() throws Exception {
    JBBPUtils.closeQuietly(null);
    JBBPUtils.closeQuietly(new ByteArrayInputStream(new byte[10]));

    final InputStream closed = new ByteArrayInputStream(new byte[10]);
    closed.close();
    JBBPUtils.closeQuietly(closed);
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

  @Test(expected = NullPointerException.class)
  public void testUnpackInt_NPEForArrayIsNull() {
    JBBPUtils.unpackInt(null, new JBBPIntCounter());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testUnpackInt_IAEForWrongPrefix() {
    JBBPUtils.unpackInt(new byte[]{(byte) 0xAA, 0, 0, 0, 0, 0}, new JBBPIntCounter());
  }

  @Test
  public void testPackUnpackIntFromByteArray() {
    final byte[] array = new byte[5];

    final JBBPIntCounter pos = new JBBPIntCounter();

    int counter1 = 0;
    int counter2 = 0;
    int counter3 = 0;

    final int[] etalons = new int[]{0, -1, -89, 234, 123124, 1223112, 34323, Integer.MIN_VALUE, Integer.MAX_VALUE};

    for (final int generated : etalons) {
      pos.set(0);
      final int len = JBBPUtils.packInt(array, pos, generated);

      if ((generated & 0xFFFFFF80) == 0) {
        assertEquals(1, len);
        counter1++;
      }
      else if ((generated & 0xFFFF0000) == 0) {
        assertEquals(3, len);
        counter2++;
      }
      else {
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
    assertEquals("[0x01, 0x02, 0x03, 0xFF]", JBBPUtils.array2hex(new byte[]{1, 2, 3, (byte) 0xFF}));
  }

  @Test
  public void testArray2Oct() {
    assertNull(JBBPUtils.array2hex(null));
    assertEquals("[0o001, 0o002, 0o003, 0o377]", JBBPUtils.array2oct(new byte[]{1, 2, 3, (byte) 0xFF}));
  }

  @Test
  public void testArray2Bin() {
    assertNull(JBBPUtils.array2bin(null));
    assertEquals("[0b00000001, 0b00000010, 0b00000011, 0b11111111]", JBBPUtils.array2bin(new byte[]{1, 2, 3, (byte) 0xFF}));
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

      assertEquals("Value is " + i, (byte) etalon, JBBPUtils.reverseBitsInByte((byte) i));
    }
  }

  @Test
  public void testBin2Str() {
    assertNull(JBBPUtils.bin2str(null, true));
    assertNull(JBBPUtils.bin2str(null, false));

    assertEquals("01010101 10101010", JBBPUtils.bin2str(new byte[]{0x55, (byte) 0xAA}, true));
    assertEquals("0101010110101010", JBBPUtils.bin2str(new byte[]{0x55, (byte) 0xAA}, false));
    assertEquals("00001001", JBBPUtils.bin2str(new byte[]{0x9}, false));
    assertEquals("1010101001010101", JBBPUtils.bin2str(new byte[]{0x55, (byte) 0xAA}, JBBPBitOrder.MSB0, false));
    assertEquals("0101010110101010", JBBPUtils.bin2str(new byte[]{0x55, (byte) 0xAA}));
  }

  @Test
  public void testStr2Bin_Default() {
    assertEquals(0, JBBPUtils.str2bin(null).length);

    assertArrayEquals(new byte[]{(byte) 0x80}, JBBPUtils.str2bin("10000000"));
    assertArrayEquals(new byte[]{(byte) 0x01}, JBBPUtils.str2bin("1"));
    assertArrayEquals(new byte[]{(byte) 0x80, 0x01}, JBBPUtils.str2bin("10000000X00x0Zz1"));
    assertArrayEquals(new byte[]{(byte) 0x80, 0x01, 0x07}, JBBPUtils.str2bin("10000000000000010111"));
    assertArrayEquals(new byte[]{(byte) 0x80, 0x01, 0x07}, JBBPUtils.str2bin("10000000_00000001_0111"));

    try {
      JBBPUtils.str2bin("10001021");
      fail("Must throw IAE");
    }
    catch (IllegalArgumentException ex) {

    }
  }

  @Test
  public void testStr2Bin_LSB0() {
    assertEquals(0, JBBPUtils.str2bin(null, JBBPBitOrder.LSB0).length);

    assertArrayEquals(new byte[]{(byte) 0x80}, JBBPUtils.str2bin("10000000", JBBPBitOrder.LSB0));
    assertArrayEquals(new byte[]{(byte) 0x01}, JBBPUtils.str2bin("1", JBBPBitOrder.LSB0));
    assertArrayEquals(new byte[]{(byte) 0x01}, JBBPUtils.str2bin("00000001", JBBPBitOrder.LSB0));
    assertArrayEquals(new byte[]{(byte) 0x80, (byte) 0x01}, JBBPUtils.str2bin("10000000X00x0Zz1", JBBPBitOrder.LSB0));
    assertArrayEquals(new byte[]{(byte) 0x80, (byte) 0x01, (byte) 0x07}, JBBPUtils.str2bin("10000000000000010111", JBBPBitOrder.LSB0));
    assertArrayEquals(new byte[]{(byte) 0x80, (byte) 0x01, (byte) 0x07}, JBBPUtils.str2bin("10000000_00000001_0111", JBBPBitOrder.LSB0));

    try {
      JBBPUtils.str2bin("10001021", JBBPBitOrder.MSB0);
      fail("Must throw IAE");
    }
    catch (IllegalArgumentException ex) {

    }
  }

  @Test
  public void testStr2Bin_LSB0_1bitShift() {
    final byte[] array = JBBPUtils.str2bin("0 11111111 01010101 00011000 00000001", JBBPBitOrder.LSB0);

    assertArrayEquals(new byte[]{(byte) 0x7F, (byte) 0xAA, (byte) 0x8C, (byte) 0x0, (byte) 0x01}, array);

  }

  @Test
  public void testStr2Bin_MSB() {
    assertEquals(0, JBBPUtils.str2bin(null, JBBPBitOrder.MSB0).length);

    assertArrayEquals(new byte[]{(byte) 0x01}, JBBPUtils.str2bin("1", JBBPBitOrder.MSB0));
    assertArrayEquals(new byte[]{(byte) 0x80}, JBBPUtils.str2bin("00000001", JBBPBitOrder.MSB0));
    assertArrayEquals(new byte[]{(byte) 0x01}, JBBPUtils.str2bin("10000000", JBBPBitOrder.MSB0));
    assertArrayEquals(new byte[]{(byte) 0xA9}, JBBPUtils.str2bin("10010101", JBBPBitOrder.MSB0));
    assertArrayEquals(new byte[]{(byte) 0x01, (byte) 0x80}, JBBPUtils.str2bin("10000000X00x0Zz1", JBBPBitOrder.MSB0));
    assertArrayEquals(new byte[]{(byte) 0x01, (byte) 0x80, (byte) 0x0E}, JBBPUtils.str2bin("1000000000000001 0111", JBBPBitOrder.MSB0));
    assertArrayEquals(new byte[]{(byte) 0x01, (byte) 0x80, (byte) 0x0E}, JBBPUtils.str2bin("10000000_00000001_0111", JBBPBitOrder.MSB0));
    assertArrayEquals(new byte[]{(byte) 0x03, (byte) 0x00, (byte) 0x01, (byte) 0x01}, JBBPUtils.str2bin("1_10000000_00000001_00000001", JBBPBitOrder.MSB0));

    try {
      JBBPUtils.str2bin("10001021", JBBPBitOrder.MSB0);
      fail("Must throw IAE");
    }
    catch (IllegalArgumentException ex) {

    }
  }

  @Test
  public void testSplitString() {
    assertArrayEquals(new String[]{""}, JBBPUtils.splitString("", '.'));
    assertArrayEquals(new String[]{"aaa"}, JBBPUtils.splitString("aaa", '.'));
    assertArrayEquals(new String[]{"aaa", "bbb"}, JBBPUtils.splitString("aaa.bbb", '.'));
    assertArrayEquals(new String[]{"aaa", "bbb", ""}, JBBPUtils.splitString("aaa.bbb.", '.'));
    assertArrayEquals(new String[]{"", ""}, JBBPUtils.splitString(".", '.'));
  }

  @Test
  public void testAssertNotNull() {
    JBBPUtils.assertNotNull(new Object(), "test");

    final String message = "Test message";
    try {
      JBBPUtils.assertNotNull(null, message);
      fail("Must throw NPE");
    }
    catch (NullPointerException ex) {
      assertSame(message, ex.getMessage());
    }

    try {
      JBBPUtils.assertNotNull(null, null);
      fail("Must throw NPE");
    }
    catch (NullPointerException ex) {
      assertNotNull(message, ex.getMessage());
    }
  }

  @Test
  public void testReverseArray() {
    assertNull(JBBPUtils.reverseArray(null));
    final byte[] empty = new byte[0];
    assertSame(empty, JBBPUtils.reverseArray(empty));

    assertArrayEquals(new byte[]{1}, JBBPUtils.reverseArray(new byte[]{1}));
    assertArrayEquals(new byte[]{2, 1}, JBBPUtils.reverseArray(new byte[]{1, 2}));
    assertArrayEquals(new byte[]{5, 4, 3, 2, 1}, JBBPUtils.reverseArray(new byte[]{1, 2, 3, 4, 5}));
    assertArrayEquals(new byte[]{6, 5, 4, 3, 2, 1}, JBBPUtils.reverseArray(new byte[]{1, 2, 3, 4, 5, 6}));
  }

  @Test
  public void testSplitInteger() {
    byte[] buff = null;
    assertArrayEquals(new byte[]{1, 2, 3, 4}, JBBPUtils.splitInteger(0x01020304, false, buff));

    buff = new byte[2];
    assertArrayEquals(new byte[]{1, 2, 3, 4}, JBBPUtils.splitInteger(0x01020304, false, buff));

    buff = new byte[4];
    assertArrayEquals(new byte[]{1, 2, 3, 4}, JBBPUtils.splitInteger(0x01020304, false, buff));

    buff = new byte[8];
    assertArrayEquals(new byte[]{1, 2, 3, 4, 0, 0, 0, 0}, JBBPUtils.splitInteger(0x01020304, false, buff));

    buff = null;
    assertArrayEquals(new byte[]{4, 3, 2, 1}, JBBPUtils.splitInteger(0x01020304, true, buff));

    buff = new byte[2];
    assertArrayEquals(new byte[]{4, 3, 2, 1}, JBBPUtils.splitInteger(0x01020304, true, buff));

    buff = new byte[4];
    assertArrayEquals(new byte[]{4, 3, 2, 1}, JBBPUtils.splitInteger(0x01020304, true, buff));

    buff = new byte[8];
    assertArrayEquals(new byte[]{4, 3, 2, 1, 0, 0, 0, 0}, JBBPUtils.splitInteger(0x01020304, true, buff));

  }

  @Test
  public void testSplitLong() {
    byte[] buff = null;
    assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7, 8}, JBBPUtils.splitLong(0x0102030405060708L, false, buff));

    buff = new byte[2];
    assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6 ,7 ,8}, JBBPUtils.splitLong(0x0102030405060708L, false, buff));

    buff = new byte[8];
    assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7, 8}, JBBPUtils.splitLong(0x0102030405060708L, false, buff));

    buff = new byte[10];
    assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 0, 0}, JBBPUtils.splitLong(0x0102030405060708L, false, buff));

    buff = null;
    assertArrayEquals(new byte[]{8, 7, 6, 5, 4, 3, 2, 1}, JBBPUtils.splitLong(0x0102030405060708L, true, buff));

    buff = new byte[2];
    assertArrayEquals(new byte[]{8, 7, 6, 5, 4, 3, 2, 1}, JBBPUtils.splitLong(0x0102030405060708L, true, buff));

    buff = new byte[8];
    assertArrayEquals(new byte[]{8, 7, 6, 5, 4, 3, 2, 1}, JBBPUtils.splitLong(0x0102030405060708L, true, buff));

    buff = new byte[10];
    assertArrayEquals(new byte[]{8, 7, 6, 5, 4, 3, 2, 1, 0, 0}, JBBPUtils.splitLong(0x0102030405060708L, true, buff));

  }
}
