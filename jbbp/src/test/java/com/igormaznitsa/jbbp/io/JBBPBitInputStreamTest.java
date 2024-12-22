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

package com.igormaznitsa.jbbp.io;

import static com.igormaznitsa.jbbp.io.JBBPBitNumber.BITS_1;
import static com.igormaznitsa.jbbp.io.JBBPBitNumber.BITS_2;
import static com.igormaznitsa.jbbp.io.JBBPBitNumber.BITS_3;
import static com.igormaznitsa.jbbp.io.JBBPBitNumber.BITS_4;
import static com.igormaznitsa.jbbp.io.JBBPBitNumber.BITS_5;
import static com.igormaznitsa.jbbp.io.JBBPBitNumber.BITS_8;
import static com.igormaznitsa.jbbp.io.JBBPByteOrder.BIG_ENDIAN;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.igormaznitsa.jbbp.TestUtils;
import com.igormaznitsa.jbbp.exceptions.JBBPReachedArraySizeLimitException;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Random;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

public class JBBPBitInputStreamTest {

  private static final String TEST_BYTES =
      "01001100_01110000_11110000_01111100_00001111_11000000_01100111_00000000_10011111";
  private static final String TEST_BYTES_EXTRABIT =
      "0 01001100_01110000_11110000_01111100_00001111_11000000_01100111_00000000_10011111";

  private static byte[] intArrayToByteArray(final int... array) {
    final byte[] bytearray = new byte[array.length];
    for (int i = 0; i < array.length; i++) {
      if ((array[i] & 0xFFFFFF00) != 0) {
        fail("Non-convertible byte value [" + array[i] + ']');
      }
      bytearray[i] = (byte) array[i];
    }
    return bytearray;
  }

  private static JBBPBitInputStream asInputStream(final int... array) {
    return new JBBPBitInputStream(new ByteArrayInputStream(intArrayToByteArray(array)));
  }

  private static JBBPBitInputStream asInputStreamMSB0(final int... array) {
    return new JBBPBitInputStream(new ByteArrayInputStream(intArrayToByteArray(array)),
        JBBPBitOrder.MSB0);
  }

  @Test
  public void testReadMsb0Direct() throws Exception {
    byte[] data = JBBPUtils.str2bin("00000001_101_00001000_00000_01_00_1011_00000");

    JBBPBitInputStream in =
        new JBBPBitInputStream(new ByteArrayInputStream(data), JBBPBitOrder.MSB0_DIRECT);
    assertEquals(1, in.readBits(BITS_8));
    assertEquals(5, in.readBits(BITS_3));
    assertEquals(8, in.readBits(BITS_8));
    assertEquals(0, in.readBits(BITS_5));
    assertEquals(1, in.readBits(BITS_2));
    assertEquals(0, in.readBits(BITS_2));
    assertEquals(11, in.readBits(BITS_4));
    assertEquals(0, in.readBits(BITS_5));
    assertEquals(0, in.readBits(BITS_3));
    assertEquals(-1, in.readBits(BITS_1));
  }

  @Test
  public void testReadLsb0Msb0() throws Exception {
    final byte[] data = new byte[] {0b0000_0001};

    JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(data));
    assertEquals(0b0001, in.readBits(BITS_4));
    assertEquals(0b0000, in.readBits(BITS_4));
    assertEquals(0b0000_0001, new JBBPBitInputStream(new ByteArrayInputStream(data)).read());

    in = new JBBPBitInputStream(new ByteArrayInputStream(data), JBBPBitOrder.LSB0);
    assertEquals(0b0001, in.readBits(BITS_4));
    assertEquals(0b0000, in.readBits(BITS_4));
    assertEquals(0b0000_0001,
        new JBBPBitInputStream(new ByteArrayInputStream(data), JBBPBitOrder.LSB0).read());

    in = new JBBPBitInputStream(new ByteArrayInputStream(data), JBBPBitOrder.MSB0);
    assertEquals(0b0000, in.readBits(BITS_4));
    assertEquals(0b1000, in.readBits(BITS_4));
    assertEquals(0b1000_0000,
        new JBBPBitInputStream(new ByteArrayInputStream(data), JBBPBitOrder.MSB0).read());
  }

  @Test
  public void testReadUnsignedShort_EOFforEmpty_BigEndian_EOFException() throws Exception {
    assertThrows(EOFException.class,
        () -> asInputStream().readUnsignedShort(BIG_ENDIAN));
  }

  @Test
  public void testReadUnsignedShort_EOFforEmpty_LittleEndian_EOFException() throws Exception {
    assertThrows(EOFException.class,
        () -> asInputStream().readUnsignedShort(JBBPByteOrder.LITTLE_ENDIAN));
  }

  @Test
  public void testReadUnsignedShort_EOFforOneByte_BigEndian_EOFException() throws Exception {
    assertThrows(EOFException.class,
        () -> asInputStream((byte) 1).readUnsignedShort(BIG_ENDIAN));
  }

  @Test
  public void testReadUnsignedShort_EOFforOneByte_LittleEndian_EOFException() throws Exception {
    assertThrows(EOFException.class,
        () -> asInputStream((byte) 1).readUnsignedShort(JBBPByteOrder.LITTLE_ENDIAN));
  }

  @Test
  public void testReadUnsignedShort_BigEndian() throws Exception {
    assertEquals(0x1234, asInputStream(0x12, 0x34).readUnsignedShort(BIG_ENDIAN));
  }

  @Test
  public void testReadUnsignedShort_BigEndian_MSB0() throws Exception {
    assertEquals(0x482C, asInputStreamMSB0(0x12, 0x34).readUnsignedShort(BIG_ENDIAN));
  }

  @Test
  public void testReadUnsignedShort_LittleEndian() throws Exception {
    assertEquals(0x3412, asInputStream(0x12, 0x34).readUnsignedShort(JBBPByteOrder.LITTLE_ENDIAN));
  }

  @Test
  public void testReadUnsignedShort_LittleEndian_MSB0() throws Exception {
    assertEquals(0x2C48,
        asInputStreamMSB0(0x12, 0x34).readUnsignedShort(JBBPByteOrder.LITTLE_ENDIAN));
  }

  @Test
  public void testReadInt_BigEndian() throws Exception {
    assertEquals(0x12345678,
        asInputStream(0x12, 0x34, 0x56, 0x78).readInt(BIG_ENDIAN));
  }

  @Test
  public void testReadInt_BigEndian_EOF() throws Exception {
    assertThrows(EOFException.class,
        () -> asInputStream(0x12, 0x34, 0x56).readInt(BIG_ENDIAN));
  }

  @Test
  public void testReadInt_BigEndian_MSB0() throws Exception {
    assertEquals(0x482C6A1E,
        asInputStreamMSB0(0x12, 0x34, 0x56, 0x78).readInt(BIG_ENDIAN));
  }

  @Test
  public void testReadStringArray_BigEndan_FixedSize() throws Exception {
    assertArrayEquals(new String[] {null, "", "ABC"}, asInputStream(0xFF, 0x00, 3, 65, 66, 67, 0, 0)
        .readStringArray(3, BIG_ENDIAN));
  }

  @Test
  public void testReadStringArray_ErrorForEOF() {
    assertThrows(IOException.class, () -> asInputStream(0xFF, 0x00, 3, 65, 66, 67, 0, 0)
        .readStringArray(8, BIG_ENDIAN));
    assertThrows(IOException.class,
        () -> asInputStream().readStringArray(8, BIG_ENDIAN));
    assertThrows(IOException.class,
        () -> asInputStream().readStringArray(8, JBBPByteOrder.LITTLE_ENDIAN));
    assertThrows(IOException.class, () -> asInputStream(0xFF, 0x00, 3, 65, 66, 67, 0, 0)
        .readStringArray(8, JBBPByteOrder.LITTLE_ENDIAN));
  }

  @Test
  public void testReadStringArray_ErrorForWrongPrefix() {
    assertThrows(IOException.class, () -> asInputStream(0x80, 0x00, 3, 65, 66, 67, 0, 0)
        .readStringArray(3, BIG_ENDIAN));
    assertThrows(IOException.class, () -> asInputStream(0x91, 0x00, 3, 65, 66, 67, 0, 0)
        .readStringArray(3, JBBPByteOrder.LITTLE_ENDIAN));
  }

  @Test
  public void testReadStringArray_LittleEndan_FixedSize() throws Exception {
    assertArrayEquals(new String[] {null, "", "ABC"}, asInputStream(0xFF, 0x00, 3, 65, 66, 67, 0, 0)
        .readStringArray(3, JBBPByteOrder.LITTLE_ENDIAN));
  }

  @Test
  public void testReadStringArray_WholeStream_ResultEmptyArray() throws Exception {
    assertArrayEquals(new String[0],
        asInputStream().readStringArray(-1, JBBPByteOrder.LITTLE_ENDIAN));
    assertArrayEquals(new String[0], asInputStream().readStringArray(-1, BIG_ENDIAN));
  }

  @Test
  public void testReadStringArray_BigEndan_WholeStream() throws Exception {
    assertArrayEquals(new String[] {null, "", "ABC", "", ""},
        asInputStream(0xFF, 0x00, 3, 65, 66, 67, 0, 0)
            .readStringArray(-1, BIG_ENDIAN));
  }

  @Test
  public void testReadStringArray_LittleEndan_WholeStream() throws Exception {
    assertArrayEquals(new String[] {null, "", "ABC", "", ""},
        asInputStream(0xFF, 0x00, 3, 65, 66, 67, 0, 0)
            .readStringArray(-1, JBBPByteOrder.LITTLE_ENDIAN));
  }

  @Test
  public void testReadString_BigEndan_Null() throws Exception {
    assertNull(asInputStream(0xFF).readString(BIG_ENDIAN));
  }

  @Test
  public void testReadString_BigEndan_Empty() throws Exception {
    assertEquals("", asInputStream(0x00).readString(BIG_ENDIAN));
  }

  @Test
  public void testReadString_BigEndan_ShortString() throws Exception {
    assertEquals("ABC", asInputStream(0x03, 65, 66, 67).readString(BIG_ENDIAN));
  }

  @Test
  public void testReadString_BigEndan_Msb0_ShortString() throws Exception {
    assertEquals("zzzz",
        asInputStreamMSB0(0x20, '^', '^', '^', '^').readString(BIG_ENDIAN));
  }

  @Test
  public void testReadString_LittleEndan_Null() throws Exception {
    assertNull(asInputStream(0xFF).readString(JBBPByteOrder.LITTLE_ENDIAN));
  }

  @Test
  public void testReadString_LittleEndian_Empty() throws Exception {
    assertEquals("", asInputStream(0x00).readString(JBBPByteOrder.LITTLE_ENDIAN));
  }

  @Test
  public void testReadString_LittleEndian_ShortString() throws Exception {
    assertEquals("ABC", asInputStream(0x03, 65, 66, 67).readString(JBBPByteOrder.LITTLE_ENDIAN));
  }

  @Test
  public void testReadFloat_BigEndian_MSB0() throws Exception {
    assertEquals(176552.47f,
        asInputStreamMSB0(0x12, 0x34, 0x56, 0x78).readFloat(BIG_ENDIAN),
        TestUtils.FLOAT_DELTA);
  }

  @Test
  public void testReadInt_BigEndian_MSB0_EOF() throws Exception {
    assertThrows(EOFException.class,
        () -> asInputStreamMSB0(0x12, 0x34, 0x56).readInt(BIG_ENDIAN));
  }

  @Test
  public void testReadFloat_BigEndian_MSB0_EOF() throws Exception {
    assertThrows(EOFException.class,
        () -> asInputStreamMSB0(0x12, 0x34, 0x56).readFloat(BIG_ENDIAN));
  }

  @Test
  public void testReadInt_LittleEndian() throws Exception {
    assertEquals(0x78563412,
        asInputStream(0x12, 0x34, 0x56, 0x78).readInt(JBBPByteOrder.LITTLE_ENDIAN));
  }

  @Test
  public void testReadFloat_LittleEndian() throws Exception {
    assertEquals(1.7378244E34f,
        asInputStream(0x12, 0x34, 0x56, 0x78).readFloat(JBBPByteOrder.LITTLE_ENDIAN),
        TestUtils.FLOAT_DELTA);
  }

  @Test
  public void testReadInt_LittleEndian_EOF() throws Exception {
    assertThrows(EOFException.class,
        () -> asInputStream(0x12, 0x34, 0x56).readInt(JBBPByteOrder.LITTLE_ENDIAN));
  }

  @Test
  public void testReadFloat_LittleEndian_EOF() {
    assertThrows(EOFException.class,
        () -> asInputStream(0x12, 0x34, 0x56).readFloat(JBBPByteOrder.LITTLE_ENDIAN));
  }

  @Test
  public void testReadInt_LittleEndian_MSB0() throws Exception {
    assertEquals(0x1E6A2C48,
        asInputStreamMSB0(0x12, 0x34, 0x56, 0x78).readInt(JBBPByteOrder.LITTLE_ENDIAN));
  }

  @Test
  public void testReadFloat_LittleEndian_MSB0() throws Exception {
    assertEquals(1.2397014E-20f,
        asInputStreamMSB0(0x12, 0x34, 0x56, 0x78).readFloat(JBBPByteOrder.LITTLE_ENDIAN),
        TestUtils.FLOAT_DELTA);
  }

  @Test
  public void testReadInt_LittleEndian_MSB0_EOF() {
    assertThrows(EOFException.class,
        () -> asInputStreamMSB0(0x12, 0x34, 0x56).readInt(JBBPByteOrder.LITTLE_ENDIAN));
  }

  @Test
  public void testReadFloat_LittleEndian_MSB0_EOF() throws Exception {
    assertThrows(EOFException.class,
        () -> asInputStreamMSB0(0x12, 0x34, 0x56).readFloat(JBBPByteOrder.LITTLE_ENDIAN));
  }

  @Test
  public void testReadLong_BigEndian() throws Exception {
    assertEquals(0x12345678AABBCCDDL, asInputStream(0x12, 0x34, 0x56, 0x78, 0xAA, 0xBB, 0xCC, 0xDD)
        .readLong(BIG_ENDIAN));
  }

  @Test
  public void testReadDouble_BigEndian() throws Exception {
    assertEquals(5.626349538661693E-221d,
        asInputStream(0x12, 0x34, 0x56, 0x78, 0xAA, 0xBB, 0xCC, 0xDD)
            .readDouble(BIG_ENDIAN), TestUtils.FLOAT_DELTA);
  }

  @Test
  public void testReadLong_BigEndian_EOF() {
    assertThrows(EOFException.class, () -> asInputStream(0x12, 0x34, 0x56, 0x78, 0xAA, 0xBB, 0xCC)
        .readLong(BIG_ENDIAN));
  }

  @Test
  public void testReadDouble_BigEndian_EOF() throws Exception {
    assertThrows(EOFException.class, () -> asInputStream(0x12, 0x34, 0x56, 0x78, 0xAA, 0xBB, 0xCC)
        .readDouble(BIG_ENDIAN));
  }

  @Test
  public void testReadLong_LittleEndian() throws Exception {
    assertEquals(0xDDCCBBAA78563412L, asInputStream(0x12, 0x34, 0x56, 0x78, 0xAA, 0xBB, 0xCC, 0xDD)
        .readLong(JBBPByteOrder.LITTLE_ENDIAN));
  }

  @Test
  public void testReadDouble_LittleEndian() throws Exception {
    assertEquals(-7.00761088740633E143d,
        asInputStream(0x12, 0x34, 0x56, 0x78, 0xAA, 0xBB, 0xCC, 0xDD)
            .readDouble(JBBPByteOrder.LITTLE_ENDIAN), TestUtils.FLOAT_DELTA);
  }

  @Test
  public void testReadLong_LittleEndian_EOF() {
    assertThrows(EOFException.class, () -> asInputStream(0x12, 0x34, 0x56, 0x78, 0xAA, 0xBB, 0xCC)
        .readLong(JBBPByteOrder.LITTLE_ENDIAN));
  }

  @Test
  public void testReadDouble_LittleEndian_EOF() {
    assertThrows(EOFException.class, () -> asInputStream(0x12, 0x34, 0x56, 0x78, 0xAA, 0xBB, 0xCC)
        .readDouble(JBBPByteOrder.LITTLE_ENDIAN));
  }

  @Test
  public void testRead9bit() throws Exception {
    final JBBPBitInputStream in =
        new JBBPBitInputStream(new ByteArrayInputStream(new byte[] {(byte) 0xDA, 1}));

    assertEquals(0xA, in.readBits(BITS_4));
    assertEquals(0x1D, in.readBits(BITS_5));
    assertEquals(0, in.read());
    assertEquals(-1, in.read());
  }

  @Test
  public void testRead9bit_MSB0() throws Exception {
    final JBBPBitInputStream in = asInputStreamMSB0(0xD9, 1);

    assertEquals(0x0B, in.readBits(BITS_4));
    assertEquals(0x09, in.readBits(BITS_5));
    assertEquals(0x40, in.read());
    assertEquals(-1, in.read());
  }

  @Test
  public void testGetBitBuffer() throws Exception {
    final JBBPBitInputStream in =
        new JBBPBitInputStream(new ByteArrayInputStream(new byte[] {(byte) 0xAA}));
    assertEquals(0, in.getBitBuffer());
    assertEquals(0xA, in.readBits(BITS_4));
    assertEquals(0xA, in.getBitBuffer());
  }

  @Test
  public void testGetOrder() {
    assertEquals(JBBPBitOrder.MSB0, new JBBPBitInputStream(null, JBBPBitOrder.MSB0).getBitOrder());
    assertEquals(JBBPBitOrder.LSB0, new JBBPBitInputStream(null, JBBPBitOrder.LSB0).getBitOrder());
    assertEquals(JBBPBitOrder.LSB0, new JBBPBitInputStream(null).getBitOrder());
  }

  @Test
  public void testAlignByte() throws Exception {
    final byte[] testarray = JBBPUtils.str2bin("01111001 10111000");

    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(testarray));
    in.alignByte();
    assertEquals(0, in.getCounter());
    assertEquals(0x19, in.readBits(BITS_5));
    assertEquals(0, in.getCounter());
    in.alignByte();
    assertEquals(1, in.getCounter());
    assertEquals(0x8, in.readBits(BITS_4));
    assertEquals(1, in.getCounter());
    assertEquals(0xB, in.readBits(BITS_4));
    assertEquals(2, in.getCounter());
    assertEquals(-1, in.read());
    assertEquals(2, in.getCounter());
  }

  @Test
  public void testAlignByte_IfWeHaveBufferedByteInBitBuffer() throws Exception {
    final byte[] testArray = JBBPUtils.str2bin("01111001 10111000");

    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(testArray));
    assertEquals(0x19, in.readBits(BITS_5));
    assertEquals(0, in.getCounter());
    assertEquals(0x03, in.readBits(BITS_3));
    assertEquals(1, in.getCounter());
    assertTrue(in.hasAvailableData());
    assertEquals(1, in.getCounter());
    in.alignByte();
    assertEquals(1, in.getCounter());
    assertEquals(0xB8, in.read());
    assertEquals(-1, in.read());
    assertEquals(2, in.getCounter());
  }

  @Test
  public void testReadStream_AsBits() throws IOException {
    final byte[] testArray = JBBPUtils.str2bin(TEST_BYTES, JBBPBitOrder.MSB0);

    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(testArray));

    assertEquals(0, in.readBits(BITS_1));//0
    assertEquals(1, in.readBits(BITS_1));
    assertEquals(0, in.getCounter());
    assertEquals(6, in.getBufferedBitsNumber());

    assertEquals(0, in.readBits(BITS_2));//2
    assertEquals(3, in.readBits(BITS_2));
    assertEquals(0, in.getCounter());
    assertEquals(2, in.getBufferedBitsNumber());

    assertEquals(0, in.readBits(BITS_3));//6
    assertEquals(7, in.readBits(BITS_3));
    assertEquals(1, in.getCounter());
    assertEquals(4, in.getBufferedBitsNumber());

    assertEquals(0, in.readBits(BITS_4));//12
    assertEquals(2, in.getCounter());
    assertEquals(15, in.readBits(BITS_4));
    assertEquals(2, in.getCounter());
    assertEquals(4, in.getBufferedBitsNumber());

    assertEquals(0, in.readBits(BITS_5));//20
    assertEquals(31, in.readBits(BITS_5));
    assertEquals(3, in.getCounter());
    assertEquals(2, in.getBufferedBitsNumber());

    assertEquals(0, in.readBits(JBBPBitNumber.BITS_6));//30
    assertEquals(63, in.readBits(JBBPBitNumber.BITS_6));
    assertEquals(5, in.getCounter());
    assertEquals(6, in.getBufferedBitsNumber());

    assertEquals(0, in.readBits(JBBPBitNumber.BITS_7));//42
    assertEquals(0x73, in.readBits(JBBPBitNumber.BITS_7));
    assertEquals(7, in.getCounter());
    assertEquals(0, in.getBufferedBitsNumber());

    assertEquals(0, in.readBits(JBBPBitNumber.BITS_8));//56
    assertEquals(8, in.getCounter());
    assertEquals(0, in.getBufferedBitsNumber());

    assertEquals(0xF9, in.readBits(JBBPBitNumber.BITS_8));//64
    assertEquals(9, in.getCounter());
    assertEquals(0, in.getBufferedBitsNumber());

    assertEquals(-1, in.read());

    assertEquals(9, in.getCounter());
  }

  @Test
  public void testReadStream_BitByBit() throws IOException {
    final JBBPBitInputStream in =
        new JBBPBitInputStream(new ByteArrayInputStream(new byte[] {0x01}));
    assertEquals(1, in.readBits(BITS_1));
    assertEquals(0, in.getCounter());
    assertEquals(0, in.readBits(BITS_1));
    assertEquals(0, in.readBits(BITS_1));
    assertEquals(0, in.readBits(BITS_1));
    assertEquals(0, in.readBits(BITS_1));
    assertEquals(0, in.readBits(BITS_1));
    assertEquals(0, in.readBits(BITS_1));
    assertEquals(0, in.readBits(BITS_1));
    assertEquals(-1, in.readBits(BITS_1));
    assertEquals(1, in.getCounter());
  }

  @Test
  public void testReadStream_7bits_Default() throws IOException {
    final JBBPBitInputStream in = new JBBPBitInputStream(
        new ByteArrayInputStream(JBBPUtils.str2bin("11011100", JBBPBitOrder.MSB0)));
    assertEquals(0x3B, in.readBits(JBBPBitNumber.BITS_7));
    assertEquals(0, in.getCounter());
  }

  @Test
  public void testReadStream_AsBytes() throws IOException {
    final byte[] testArray = JBBPUtils.str2bin(TEST_BYTES, JBBPBitOrder.MSB0);

    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(testArray));

    for (int i = 0; i < testArray.length; i++) {
      assertEquals(testArray[i] & 0xFF, in.read(), "Byte " + i);
    }
    assertEquals(9, in.getCounter());
    assertEquals(-1, in.read());
  }

  @Test
  public void testReadStream_AsArray() throws IOException {
    final byte[] testArray = JBBPUtils.str2bin(TEST_BYTES, JBBPBitOrder.MSB0);

    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(testArray));

    final byte[] read = new byte[9];
    assertEquals(9, in.read(read));
    assertEquals(-1, in.read());
    assertEquals(9, in.getCounter());

    assertArrayEquals(testArray, read);
  }

  @Test
  public void testReadStream_AsPartOfArray() throws IOException {
    final byte[] testArray = JBBPUtils.str2bin(TEST_BYTES, JBBPBitOrder.MSB0);

    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(testArray));

    final byte[] buff = new byte[27];
    assertEquals(5, in.read(buff, 9, 5));
    assertEquals(5, in.getCounter());
    assertEquals(3, in.read());

    for (int i = 0; i < 9; i++) {
      assertEquals(0, buff[i]);
    }

    for (int i = 9; i < 14; i++) {
      assertEquals(testArray[i - 9], buff[i]);
    }

    for (int i = 14; i < 27; i++) {
      assertEquals(0, buff[i]);
    }

  }

  @Test
  public void testReadStream_AsPartOfArray_MSB0() throws IOException {
    final byte[] testArray = JBBPUtils.str2bin(TEST_BYTES, JBBPBitOrder.MSB0);

    final JBBPBitInputStream in =
        new JBBPBitInputStream(new ByteArrayInputStream(testArray), JBBPBitOrder.MSB0);

    final byte[] buff = new byte[27];
    assertEquals(5, in.read(buff, 9, 5));
    assertEquals(5, in.getCounter());
    assertEquals(0xC0, in.read());

    for (int i = 0; i < 9; i++) {
      assertEquals(0, buff[i]);
    }

    for (int i = 9; i < 14; i++) {
      assertEquals(JBBPUtils.reverseBitsInByte(testArray[i - 9]), buff[i]);
    }

    for (int i = 14; i < 27; i++) {
      assertEquals(0, buff[i]);
    }

  }

  @Test
  public void testReadStream_AsPartOfArray_1bitOffset() throws IOException {
    final byte[] testarray = JBBPUtils.str2bin(TEST_BYTES, JBBPBitOrder.MSB0);

    final JBBPBitInputStream in = new JBBPBitInputStream(
        new ByteArrayInputStream(JBBPUtils.str2bin(TEST_BYTES_EXTRABIT, JBBPBitOrder.MSB0)));

    assertEquals(0, in.readBits(BITS_1));

    final byte[] read = new byte[27];
    assertEquals(5, in.read(read, 9, 5));
    assertEquals(5, in.getCounter());
    assertEquals(3, in.read());

    for (int i = 0; i < 9; i++) {
      assertEquals(0, read[i]);
    }

    for (int i = 9; i < 14; i++) {
      assertEquals(testarray[i - 9], read[i]);
    }

    for (int i = 14; i < 27; i++) {
      assertEquals(0, read[i]);
    }

  }

  @Test
  public void testMarkForReadBits() throws IOException {
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(
        JBBPUtils.str2bin("10010110_00101000_10101010", JBBPBitOrder.MSB0)));

    assertEquals(0x9, in.readBits(BITS_4));
    assertEquals(0x6, in.readBits(JBBPBitNumber.BITS_6));

    assertTrue(in.markSupported());

    in.mark(1024);

    assertEquals(5, in.readBits(BITS_3));
    assertEquals(0, in.readBits(BITS_3));
    assertEquals(0x55, in.read());

    in.reset();

    assertEquals(5, in.readBits(BITS_3));
    assertEquals(0, in.readBits(BITS_3));
    assertEquals(0x55, in.read());

    assertEquals(-1, in.read());
  }

  @Test
  public void testReadBits_ExceptionForWrongArgument() {
    final JBBPBitInputStream inLe = new JBBPBitInputStream(
        new ByteArrayInputStream(JBBPUtils.str2bin(TEST_BYTES, JBBPBitOrder.LSB0)));
    assertThrows(IllegalArgumentException.class, () -> inLe.readBits(JBBPBitNumber.decode(0)));
    assertThrows(IllegalArgumentException.class, () -> inLe.readBits(JBBPBitNumber.decode(-5)));
    assertThrows(IllegalArgumentException.class, () -> inLe.readBits(JBBPBitNumber.decode(9)));
  }

  @Test
  public void testSkipBytes() throws Exception {
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils
        .str2bin("01010101_01010101_01010101_00011000_01010101_01010101_00000001",
            JBBPBitOrder.MSB0)));

    assertEquals(3, in.skip(3));
    assertEquals(0x18, in.read());
    assertEquals(2, in.skip(2));
    assertEquals(0x80, in.read());
    assertEquals(-1, in.read());
  }

  @Test
  public void testAlignBytes() throws Exception {
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils
        .str2bin("01010101_01010101_01011101_00011000_01010101_01010101_00000001",
            JBBPBitOrder.MSB0)));

    assertEquals(0xAA, in.read());
    in.align(3);
    assertEquals(0x18, in.read());
    in.align(6);
    assertEquals(0x80, in.read());
    assertEquals(-1, in.read());
    assertThrows(EOFException.class, () -> in.align(10));
  }

  @Test
  public void testRead_WithoutOffset() throws Exception {
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils
        .str2bin("01010111_01010111_01010111_00011000_01010101_01100011_00000001",
            JBBPBitOrder.MSB0)));

    assertEquals(0xEA, in.read());
    assertEquals(0xEA, in.read());
    assertEquals(0xEA, in.read());
    assertEquals(0x18, in.read());
    assertEquals(0xAA, in.read());
    assertEquals(0xC6, in.read());
    assertEquals(0x80, in.read());
    assertEquals(-1, in.read());
  }

  @Test
  public void testRead_1bitOffset() throws Exception {
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils
        .str2bin("1 01010111_01010111_01010111_00011000_01010111_01100011_00101101",
            JBBPBitOrder.MSB0)));

    assertEquals(1, in.readBits(BITS_1));

    assertEquals(0xEA, in.read());
    assertEquals(0xEA, in.read());
    assertEquals(0xEA, in.read());
    assertEquals(0x18, in.read());
    assertEquals(0xEA, in.read());
    assertEquals(0xC6, in.read());
    assertEquals(0xB4, in.read());
    assertEquals(0, in.read());
    assertEquals(-1, in.read());
  }

  @Test
  public void testSkipBytes_1bitOffset() throws Exception {
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils
        .str2bin("0 01010101_01010101_01010101 00011000_01010101_010110110_0000001",
            JBBPBitOrder.MSB0)));

    assertEquals(0, in.readBits(BITS_1));

    assertEquals(0, in.getCounter());

    assertEquals(3, in.skip(3));
    assertEquals(3, in.getCounter());

    assertEquals(0x18, in.read());
    assertEquals(4, in.getCounter());

    assertEquals(1, in.skip(1));
    assertEquals(5, in.getCounter());

    assertEquals(0xDA, in.read());
    assertEquals(6, in.getCounter());

    assertEquals(0x80, in.read());
    assertEquals(7, in.getCounter());

    assertEquals(0, in.read());
    assertEquals(8, in.getCounter());

    assertEquals(-1, in.read());
    assertEquals(8, in.getCounter());
  }

  @Test
  public void testReadArray_Bits_WholeStream() throws Exception {
    JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(
        JBBPUtils.str2bin("00101001_11001011_10111110", JBBPBitOrder.LSB0)));
    assertArrayEquals(new byte[] {1, 2, 2, 0, 3, 2, 0, 3, 2, 3, 3, 2},
        in.readBitsArray(-1, BITS_2));
    assertEquals(3, in.getCounter());

    final Random rnd = new Random(1234);

    final byte[] buff = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE];
    rnd.nextBytes(buff);

    in = new JBBPBitInputStream(new ByteArrayInputStream(buff));

    final byte[] read = in.readBitsArray(-1, JBBPBitNumber.BITS_8);
    assertEquals(buff.length, in.getCounter());

    assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE, read.length);
    for (int i = 0; i < read.length; i++) {
      assertEquals(buff[i], read[i]);
    }

    final byte[] big = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 128];
    rnd.nextBytes(big);

    in = new JBBPBitInputStream(new ByteArrayInputStream(big));

    final byte[] readbig = in.readBitsArray(-1, JBBPBitNumber.BITS_8);

    assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 128, readbig.length);
    for (int i = 0; i < readbig.length; i++) {
      assertEquals(big[i], readbig[i]);
    }
  }

  @Test
  public void testReadArray_Bits_ThreeItems() throws Exception {
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(
        JBBPUtils.str2bin("00101001_11001011_10111110", JBBPBitOrder.LSB0)));
    assertArrayEquals(new byte[] {1, 2, 2}, in.readBitsArray(3, BITS_2));
  }

  @Test
  public void testReadArray_Bits_EOF() {
    assertThrows(EOFException.class, () -> {
      final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(
          JBBPUtils.str2bin("00101001_11001011_10111110", JBBPBitOrder.LSB0)));
      in.readBitsArray(58, BITS_2);
    });
  }

  @Test
  public void testReadArray_Bytes_WholeStream() throws Exception {
    JBBPBitInputStream in =
        new JBBPBitInputStream(new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5, 6, 7, 0}));
    assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6, 7, 0}, in.readByteArray(-1));
    assertEquals(8, in.getCounter());

    final Random rnd = new Random(1234);

    final byte[] buff = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE];
    rnd.nextBytes(buff);

    in = new JBBPBitInputStream(new ByteArrayInputStream(buff));

    final byte[] read = in.readByteArray(-1);

    assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE, read.length);
    for (int i = 0; i < read.length; i++) {
      assertEquals(buff[i], read[i]);
    }

    final byte[] big = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 128];
    rnd.nextBytes(big);

    in = new JBBPBitInputStream(new ByteArrayInputStream(big));

    final byte[] readbig = in.readByteArray(-1);

    assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 128, readbig.length);
    for (int i = 0; i < readbig.length; i++) {
      assertEquals(big[i], readbig[i]);
    }
  }

  @Test
  public void testReadArray_Bytes_ThreeItems() throws Exception {
    final JBBPBitInputStream in =
        new JBBPBitInputStream(new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5, 6, 7, 0}));
    assertArrayEquals(new byte[] {1, 2, 3,}, in.readByteArray(3));
  }

  @Test
  public void testReadArray_Bytes_BIG_ENDIAN_ThreeItems() throws Exception {
    final JBBPBitInputStream in =
        new JBBPBitInputStream(new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5, 6, 7, 0}));
    assertArrayEquals(new byte[] {1, 2, 3,}, in.readByteArray(3, BIG_ENDIAN));
  }

  @Test
  public void testReadArray_Bytes_LITTLE_ENDIAN_ThreeItems() throws Exception {
    final JBBPBitInputStream in =
        new JBBPBitInputStream(new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5, 6, 7, 0}));
    assertArrayEquals(new byte[] {3, 2, 1,}, in.readByteArray(3, JBBPByteOrder.LITTLE_ENDIAN));
  }

  @Test
  public void testReadArray_Bytes_EOF() {
    assertThrows(EOFException.class, () -> {
      final JBBPBitInputStream in =
          new JBBPBitInputStream(new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5, 6, 7, 0}));
      in.readByteArray(259);
    });
  }

  @Test
  public void testReadArray_Short_WholeStream() throws Exception {
    JBBPBitInputStream in =
        new JBBPBitInputStream(new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5, 6, 7, 0}));
    assertArrayEquals(new short[] {0x0102, 0x0304, 0x0506, 0x0700},
        in.readShortArray(-1, BIG_ENDIAN));
    assertEquals(8, in.getCounter());

    final Random rnd = new Random(1234);

    final byte[] buff = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 2];
    rnd.nextBytes(buff);

    in = new JBBPBitInputStream(new ByteArrayInputStream(buff));
    final short[] read = in.readShortArray(-1, BIG_ENDIAN);
    assertEquals(buff.length, in.getCounter());

    assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE, read.length);
    for (int i = 0; i < read.length; i++) {
      final int val = read[i];
      final int j = i * 2;
      assertEquals(val, ((buff[j] << 8) | ((buff[j + 1] & 0xFF))));
    }

    final byte[] big = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 128];
    rnd.nextBytes(big);

    in = new JBBPBitInputStream(new ByteArrayInputStream(big));

    final short[] readbig = in.readShortArray(-1, BIG_ENDIAN);

    assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 64, readbig.length);
    for (int i = 0; i < readbig.length; i++) {
      final int val = readbig[i];
      final int j = i * 2;
      assertEquals(val, ((big[j] << 8) | ((big[j + 1] & 0xFF))));
    }
  }

  @Test
  public void testReadArray_UShort_WholeStream() throws Exception {
    JBBPBitInputStream in =
        new JBBPBitInputStream(new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5, 6, 7, 0}));
    assertArrayEquals(new char[] {0x0102, 0x0304, 0x0506, 0x0700},
        in.readUShortArray(-1, BIG_ENDIAN));
    assertEquals(8, in.getCounter());

    final Random rnd = new Random(1234);

    final byte[] buff = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 2];
    rnd.nextBytes(buff);

    in = new JBBPBitInputStream(new ByteArrayInputStream(buff));
    final char[] read = in.readUShortArray(-1, BIG_ENDIAN);
    assertEquals(buff.length, in.getCounter());

    assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE, read.length);
    for (int i = 0; i < read.length; i++) {
      final int val = read[i];
      final int j = i * 2;
      assertEquals(val, ((buff[j] << 8) | ((buff[j + 1] & 0xFF))) & 0xFFFF);
    }

    final byte[] big = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 128];
    rnd.nextBytes(big);

    in = new JBBPBitInputStream(new ByteArrayInputStream(big));

    final char[] readbig = in.readUShortArray(-1, BIG_ENDIAN);

    assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 64, readbig.length);
    for (int i = 0; i < readbig.length; i++) {
      final int val = readbig[i];
      final int j = i * 2;
      assertEquals(val, ((big[j] << 8) | ((big[j + 1] & 0xFF))) & 0xFFFF);
    }
  }

  @Test
  public void testReadArray_Short_TwoItems() throws Exception {
    final JBBPBitInputStream in =
        new JBBPBitInputStream(new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5, 6, 7, 0}));
    assertArrayEquals(new short[] {0x0102, 0x0304}, in.readShortArray(2, BIG_ENDIAN));
    assertEquals(4, in.getCounter());
  }

  @Test
  public void testReadArray_UShort_TwoItems() throws Exception {
    final JBBPBitInputStream in =
        new JBBPBitInputStream(new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5, 6, 7, 0}));
    assertArrayEquals(new char[] {0x0102, 0x0304}, in.readUShortArray(2, BIG_ENDIAN));
    assertEquals(4, in.getCounter());
  }

  @Test
  public void testReadArray_Short_EOF() {
    assertThrows(EOFException.class, () -> {
      final JBBPBitInputStream in =
          new JBBPBitInputStream(new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5, 6, 7, 0}));
      in.readShortArray(259, BIG_ENDIAN);
    });
  }

  @Test
  public void testReadArray_Int_WholeStream() throws Exception {
    JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(
        new byte[] {1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01}));
    assertArrayEquals(new int[] {0x01020304, 0x05060700, 0xFECABE01},
        in.readIntArray(-1, BIG_ENDIAN));
    assertEquals(12, in.getCounter());

    final Random rnd = new Random(1234);

    final byte[] buff = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 4];
    rnd.nextBytes(buff);

    in = new JBBPBitInputStream(new ByteArrayInputStream(buff));
    final int[] read = in.readIntArray(-1, BIG_ENDIAN);
    assertEquals(buff.length, in.getCounter());

    assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE, read.length);
    for (int i = 0; i < read.length; i++) {
      final int val = read[i];
      final int j = i * 4;
      assertEquals(val,
          ((buff[j] << 24) | ((buff[j + 1] & 0xFF) << 16) | ((buff[j + 2] & 0xFF) << 8) |
              (buff[j + 3] & 0xFF)));
    }

    final byte[] big = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 128];
    rnd.nextBytes(big);

    in = new JBBPBitInputStream(new ByteArrayInputStream(big));

    final int[] readbig = in.readIntArray(-1, BIG_ENDIAN);

    assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 32, readbig.length);
    for (int i = 0; i < readbig.length; i++) {
      final int val = readbig[i];
      final int j = i * 4;
      assertEquals(val, ((big[j] << 24) | ((big[j + 1] & 0xFF) << 16) | ((big[j + 2] & 0xFF) << 8) |
          (big[j + 3] & 0xFF)));
    }
  }

  private void testWholeStreamArrayRead(
      final int expectedReadWholeSize,
      final StreamAndIntSupplier readWhole,
      final StreamAndIntSupplier readWholeWithException,
      final int expectedReadLimitedSize,
      final StreamAndIntSupplier readWholeLimited
  ) throws Exception {
    final Pair<JBBPBitInputStream, Integer> readWholeData = readWhole.getData();
    assertEquals(expectedReadWholeSize, readWholeData.getRight());
    assertFalse(readWholeData.getLeft().isDetectedArrayLimit());

    assertThrows(JBBPReachedArraySizeLimitException.class, readWholeWithException::getData);

    final Pair<JBBPBitInputStream, Integer> readWholeLimitedData = readWholeLimited.getData();
    assertEquals(expectedReadLimitedSize, readWholeLimitedData.getRight());
    assertTrue(readWholeLimitedData.getLeft().isDetectedArrayLimit());
  }

  @Test
  public void testReadArray_WholeWithLimiter_Bits() throws Exception {
    final byte[] testData = new byte[] {1, 2, 3};
    this.testWholeStreamArrayRead(
        6, () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readBitsArray(-1, BITS_4, () -> 0).length);
        },
        () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readBitsArray(-1,
              BITS_4, () -> 3).length);
        },
        2, () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readBitsArray(-1,
              BITS_4, () -> -2).length);
        });
  }

  @Test
  public void testReadArray_WholeWithLimiter_Boolean() throws Exception {
    final byte[] testData = new byte[] {1, 2, 3, 4, 5, 6};
    this.testWholeStreamArrayRead(
        6, () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readBoolArray(-1,
              () -> 0).length);
        },
        () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readBoolArray(-1,
              () -> 3).length);
        },
        2, () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readBoolArray(-1,
              () -> -2).length);
        });
  }

  @Test
  public void testReadArray_WholeWithLimiter_Byte() throws Exception {
    final byte[] testData = new byte[] {1, 2, 3, 4, 5, 6};
    this.testWholeStreamArrayRead(
        6, () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readByteArray(-1,
              () -> 0).length);
        },
        () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readByteArray(-1,
              () -> 3).length);
        },
        2, () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readByteArray(-1,
              () -> -2).length);
        });
  }

  @Test
  public void testReadArray_WholeWithLimiter_Short() throws Exception {
    final byte[] testData = new byte[] {1, 2, 3, 4, 5, 6};
    this.testWholeStreamArrayRead(
        3, () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readShortArray(-1,
              BIG_ENDIAN, () -> 0).length);
        },
        () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readShortArray(-1,
              BIG_ENDIAN, () -> 2).length);
        },
        2, () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readShortArray(-1,
              BIG_ENDIAN, () -> -2).length);
        });
  }

  @Test
  public void testReadArray_WholeWithLimiter_UShort() throws Exception {
    final byte[] testData = new byte[] {1, 2, 3, 4, 5, 6};
    this.testWholeStreamArrayRead(
        3, () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readUShortArray(-1,
              BIG_ENDIAN, () -> 0).length);
        },
        () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readUShortArray(-1,
              BIG_ENDIAN, () -> 2).length);
        },
        2, () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readUShortArray(-1,
              BIG_ENDIAN, () -> -2).length);
        });
  }

  @Test
  public void testReadArray_WholeWithLimiter_Int() throws Exception {
    final byte[] testData = TestUtils.getRandomBytes(128);
    this.testWholeStreamArrayRead(
        32, () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readIntArray(-1,
              BIG_ENDIAN, () -> 0).length);
        },
        () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readIntArray(-1,
              BIG_ENDIAN, () -> 2).length);
        },
        2, () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readIntArray(-1,
              BIG_ENDIAN, () -> -2).length);
        });
  }

  @Test
  public void testReadArray_WholeWithLimiter_UInt() throws Exception {
    final byte[] testData = TestUtils.getRandomBytes(128);
    this.testWholeStreamArrayRead(
        32, () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readUIntArray(-1,
              BIG_ENDIAN, () -> 0).length);
        },
        () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readUIntArray(-1,
              BIG_ENDIAN, () -> 2).length);
        },
        2, () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readUIntArray(-1,
              BIG_ENDIAN, () -> -2).length);
        });
  }

  @Test
  public void testReadArray_WholeWithLimiter_Long() throws Exception {
    final byte[] testData = TestUtils.getRandomBytes(128);
    this.testWholeStreamArrayRead(
        16, () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readLongArray(-1,
              BIG_ENDIAN, () -> 0).length);
        },
        () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readLongArray(-1,
              BIG_ENDIAN, () -> 2).length);
        },
        2, () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readLongArray(-1,
              BIG_ENDIAN, () -> -2).length);
        });
  }

  @Test
  public void testReadArray_WholeWithLimiter_Float() throws Exception {
    final byte[] testData = TestUtils.getRandomBytes(128);
    this.testWholeStreamArrayRead(
        32, () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readFloatArray(-1,
              BIG_ENDIAN, () -> 0).length);
        },
        () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readFloatArray(-1,
              BIG_ENDIAN, () -> 2).length);
        },
        2, () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readFloatArray(-1,
              BIG_ENDIAN, () -> -2).length);
        });
  }

  @Test
  public void testReadArray_WholeWithLimiter_String() throws Exception {
    final byte[] testData =
        TestUtils.makeStringArray(BIG_ENDIAN, "hello", "world", "one", "two", "three", "four");
    this.testWholeStreamArrayRead(
        6, () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readStringArray(-1,
              BIG_ENDIAN, () -> 0).length);
        },
        () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readStringArray(-1,
              BIG_ENDIAN, () -> 2).length);
        },
        2, () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readStringArray(-1,
              BIG_ENDIAN, () -> -2).length);
        });
  }

  @Test
  public void testReadArray_WholeWithLimiter_Double() throws Exception {
    final byte[] testData = TestUtils.getRandomBytes(128);
    this.testWholeStreamArrayRead(
        16, () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readDoubleArray(-1,
              BIG_ENDIAN, () -> 0).length);
        },
        () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readDoubleArray(-1,
              BIG_ENDIAN, () -> 2).length);
        },
        2, () -> {
          JBBPBitInputStream stream = new JBBPBitInputStream(new ByteArrayInputStream(testData));
          return Pair.of(stream, stream.readDoubleArray(-1,
              BIG_ENDIAN, () -> -2).length);
        });
  }

  @Test
  public void testReadArray_UInt_WholeStream() throws Exception {
    JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(
        new byte[] {1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01}));
    assertArrayEquals(new long[] {0x01020304L, 0x05060700L, 0xFECABE01L},
        in.readUIntArray(-1, BIG_ENDIAN));
    assertEquals(12, in.getCounter());

    final Random rnd = new Random(1234);

    final byte[] buff = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 4];
    rnd.nextBytes(buff);

    in = new JBBPBitInputStream(new ByteArrayInputStream(buff));
    final long[] read = in.readUIntArray(-1, BIG_ENDIAN);
    assertEquals(buff.length, in.getCounter());

    assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE, read.length);
    for (int i = 0; i < read.length; i++) {
      final long val = read[i];
      final int j = i * 4;
      assertEquals(val,
          ((long) ((buff[j] << 24) | ((buff[j + 1] & 0xFF) << 16) | ((buff[j + 2] & 0xFF) << 8) |
              (buff[j + 3] & 0xFF))) & 0xFFFFFFFFL);
    }

    final byte[] big = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 128];
    rnd.nextBytes(big);

    in = new JBBPBitInputStream(new ByteArrayInputStream(big));

    final long[] readBig = in.readUIntArray(-1, BIG_ENDIAN);

    assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 32, readBig.length);
    for (int i = 0; i < readBig.length; i++) {
      final long val = readBig[i];
      final int j = i * 4;
      assertEquals(val,
          ((long) ((big[j] << 24) | ((big[j + 1] & 0xFF) << 16) | ((big[j + 2] & 0xFF) << 8) |
              (big[j + 3] & 0xFF))) & 0xFFFFFFFFL);
    }
  }

  @Test
  public void testReadArray_Float_WholeStream() throws Exception {
    JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(
        new byte[] {1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01}));
    assertArrayEquals(new float[] {2.3879393E-38f, 6.3019354E-36f, -1.3474531E38f},
        in.readFloatArray(-1, BIG_ENDIAN), TestUtils.FLOAT_DELTA);
    assertEquals(12, in.getCounter());

    final Random rnd = new Random(1234);

    final byte[] buff = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 4];
    rnd.nextBytes(buff);

    in = new JBBPBitInputStream(new ByteArrayInputStream(buff));
    final float[] read = in.readFloatArray(-1, BIG_ENDIAN);
    assertEquals(buff.length, in.getCounter());

    assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE, read.length);
    for (int i = 0; i < read.length; i++) {
      final float val = read[i];
      final int j = i * 4;
      assertEquals(val, Float.intBitsToFloat(
          (buff[j] << 24) | ((buff[j + 1] & 0xFF) << 16) | ((buff[j + 2] & 0xFF) << 8) |
              (buff[j + 3] & 0xFF)), TestUtils.FLOAT_DELTA);
    }

    final byte[] big = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 128];
    rnd.nextBytes(big);

    in = new JBBPBitInputStream(new ByteArrayInputStream(big));

    final float[] readbig = in.readFloatArray(-1, BIG_ENDIAN);

    assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 32, readbig.length);
    for (int i = 0; i < readbig.length; i++) {
      final float val = readbig[i];
      final int j = i * 4;
      assertEquals(val, Float.intBitsToFloat(
          (big[j] << 24) | ((big[j + 1] & 0xFF) << 16) | ((big[j + 2] & 0xFF) << 8) |
              (big[j + 3] & 0xFF)), TestUtils.FLOAT_DELTA);
    }
  }

  @Test
  public void testReadArray_Int_TwoItems() throws Exception {
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(
        new byte[] {1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01}));
    assertArrayEquals(new int[] {0x01020304, 0x05060700},
        in.readIntArray(2, BIG_ENDIAN));
    assertEquals(8, in.getCounter());
  }

  @Test
  public void testReadArray_UInt_TwoItems() throws Exception {
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(
        new byte[] {1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01}));
    assertArrayEquals(new long[] {0x01020304L, 0x05060700L},
        in.readUIntArray(2, BIG_ENDIAN));
    assertEquals(8, in.getCounter());
  }

  @Test
  public void testReadArray_Float_TwoItems() throws Exception {
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(
        new byte[] {1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01}));
    assertArrayEquals(new float[] {2.3879393E-38f, 6.3019354E-36f},
        in.readFloatArray(2, BIG_ENDIAN), TestUtils.FLOAT_DELTA);
    assertEquals(8, in.getCounter());
  }

  @Test
  public void testReadArray_Int_EOF() {
    assertThrows(EOFException.class, () -> {
      final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(
          new byte[] {1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01}));
      in.readIntArray(259, BIG_ENDIAN);
    });
  }

  @Test
  public void testReadArray_UInt_EOF() {
    assertThrows(EOFException.class, () -> {
      final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(
          new byte[] {1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01}));
      in.readUIntArray(259, BIG_ENDIAN);
    });
  }

  @Test
  public void testReadArray_DoubleInt_EOF() throws Exception {
    assertThrows(EOFException.class, () -> {
      final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(
          new byte[] {1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01}));
      in.readFloatArray(259, BIG_ENDIAN);
    });
  }

  @Test
  public void testReadArray_Long_WholeStream() throws Exception {
    JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(
        new byte[] {1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01, 2,
            3, 4, 5, 6, 7, 8, 9, 1, 2, 3, 4}));
    assertArrayEquals(new long[] {0x0102030405060700L, 0xFECABE0102030405L, 0x0607080901020304L},
        in.readLongArray(-1, BIG_ENDIAN));
    assertEquals(24, in.getCounter());

    final Random rnd = new Random(1234);

    final byte[] buff = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 8];
    rnd.nextBytes(buff);

    in = new JBBPBitInputStream(new ByteArrayInputStream(buff));
    final long[] read = in.readLongArray(-1, BIG_ENDIAN);
    assertEquals(buff.length, in.getCounter());

    assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE, read.length);
    for (int i = 0; i < read.length; i++) {
      final long val = read[i];
      final int j = i * 8;
      assertEquals(val, (((long) buff[j] << 56) | (((long) buff[j + 1] & 0xFFL) << 48) |
          (((long) buff[j + 2] & 0xFFL) << 40) | (((long) buff[j + 3] & 0xFFL) << 32) |
          (((long) buff[j + 4] & 0xFFL) << 24) | (((long) buff[j + 5] & 0xFFL) << 16) |
          (((long) buff[j + 6] & 0xFFL) << 8) | ((long) buff[j + 7] & 0xFF)));
    }

    final byte[] big = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 128];
    rnd.nextBytes(big);

    in = new JBBPBitInputStream(new ByteArrayInputStream(big));

    final long[] readbig = in.readLongArray(-1, BIG_ENDIAN);

    assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 16, readbig.length);
    for (int i = 0; i < readbig.length; i++) {
      final long val = readbig[i];
      final int j = i * 8;
      assertEquals(val, (((long) big[j] << 56) | (((long) big[j + 1] & 0xFFL) << 48) |
          (((long) big[j + 2] & 0xFFL) << 40) | (((long) big[j + 3] & 0xFFL) << 32) |
          (((long) big[j + 4] & 0xFFL) << 24) | (((long) big[j + 5] & 0xFFL) << 16) |
          (((long) big[j + 6] & 0xFFL) << 8) | ((long) big[j + 7] & 0xFF)));
    }
  }

  @Test
  public void testReadArray_Double_WholeStream() throws Exception {
    JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(
        new byte[] {1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01, 2,
            3, 4, 5, 6, 7, 8, 9, 1, 2, 3, 4}));
    assertArrayEquals(
        new double[] {8.207880399131826E-304d, -5.730900111929792E302d, 1.268802825418157E-279d},
        in.readDoubleArray(-1, BIG_ENDIAN), TestUtils.FLOAT_DELTA);
    assertEquals(24, in.getCounter());

    final Random rnd = new Random(1234);

    final byte[] buff = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 8];
    rnd.nextBytes(buff);

    in = new JBBPBitInputStream(new ByteArrayInputStream(buff));
    final double[] read = in.readDoubleArray(-1, BIG_ENDIAN);
    assertEquals(buff.length, in.getCounter());

    assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE, read.length);
    for (int i = 0; i < read.length; i++) {
      final double val = read[i];
      final int j = i * 8;
      assertEquals(val, Double.longBitsToDouble(
              ((long) buff[j] << 56) | (((long) buff[j + 1] & 0xFFL) << 48) |
                  (((long) buff[j + 2] & 0xFFL) << 40) | (((long) buff[j + 3] & 0xFFL) << 32) |
                  (((long) buff[j + 4] & 0xFFL) << 24) | (((long) buff[j + 5] & 0xFFL) << 16) |
                  (((long) buff[j + 6] & 0xFFL) << 8) | ((long) buff[j + 7] & 0xFF)),
          TestUtils.FLOAT_DELTA);
    }

    final byte[] big = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 128];
    rnd.nextBytes(big);

    in = new JBBPBitInputStream(new ByteArrayInputStream(big));

    final double[] readbig = in.readDoubleArray(-1, BIG_ENDIAN);

    assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 16, readbig.length);
    for (int i = 0; i < readbig.length; i++) {
      final double val = readbig[i];
      final int j = i * 8;
      assertEquals(val, Double.longBitsToDouble(
              ((long) big[j] << 56) | (((long) big[j + 1] & 0xFFL) << 48) |
                  (((long) big[j + 2] & 0xFFL) << 40) | (((long) big[j + 3] & 0xFFL) << 32) |
                  (((long) big[j + 4] & 0xFFL) << 24) | (((long) big[j + 5] & 0xFFL) << 16) |
                  (((long) big[j + 6] & 0xFFL) << 8) | ((long) big[j + 7] & 0xFF)),
          TestUtils.FLOAT_DELTA);
    }
  }

  @Test
  public void testReadArray_Long_TwoItems() throws Exception {
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(
        new byte[] {1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01, 2,
            3, 4, 5, 6, 7, 8, 9, 1, 2, 3, 4}));
    assertArrayEquals(new long[] {0x0102030405060700L, 0xFECABE0102030405L},
        in.readLongArray(2, BIG_ENDIAN));
    assertEquals(16, in.getCounter());
  }

  @Test
  public void testReadArray_Double_TwoItems() throws Exception {
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(
        new byte[] {1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01, 2,
            3, 4, 5, 6, 7, 8, 9, 1, 2, 3, 4}));
    assertArrayEquals(new double[] {8.207880399131826E-304d, -5.730900111929792E302d},
        in.readDoubleArray(2, BIG_ENDIAN), TestUtils.FLOAT_DELTA);
    assertEquals(16, in.getCounter());
  }

  @Test
  public void testReadArray_Long_EOF() throws Exception {
    assertThrows(EOFException.class, () -> {
      final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(
          new byte[] {1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01, 2,
              3, 4, 5, 6, 7, 8, 9, 1, 2, 3, 4}));
      in.readLongArray(259, BIG_ENDIAN);
    });
  }

  @Test
  public void testReadArray_Double_EOF() throws Exception {
    assertThrows(EOFException.class, () -> {
      final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(
          new byte[] {1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01, 2,
              3, 4, 5, 6, 7, 8, 9, 1, 2, 3, 4}));
      in.readDoubleArray(259, BIG_ENDIAN);
    });
  }

  @Test
  public void testResetCounter_ForStartOfStream() throws Exception {
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(
        new byte[] {1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01, 2,
            3, 4, 5, 6, 7, 8, 9, 1, 2, 3, 4}));
    in.resetCounter();
    assertEquals(1, in.readByte());
    assertEquals(1, in.getCounter());
  }

  @Test
  public void testResetCounter_ForCachedBits() throws Exception {
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(
        new byte[] {1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01, 2,
            3, 4, 5, 6, 7, 8, 9, 1, 2, 3, 4}));
    assertEquals(1, in.readBits(BITS_3));
    assertEquals(0, in.getCounter());
    assertTrue(in.getBufferedBitsNumber() != 0);
    in.resetCounter();
    assertEquals(0, in.getCounter());
    assertEquals(0, in.getBufferedBitsNumber());
    assertEquals(2, in.readByte());
  }

  @Test
  public void testReadBooleanArray_EOF() throws Exception {
    assertThrows(EOFException.class, () -> asInputStream(1, 2, 3, 4, 5, 6).readBoolArray(256));
  }

  @Test
  public void testReadBooleanArray_WholeStream() throws Exception {
    final byte[] testarray = new byte[16384];
    final Random rnd = new Random(1234);
    for (int i = 0; i < testarray.length; i++) {
      testarray[i] = rnd.nextInt(100) > 50 ? 0 : (byte) rnd.nextInt(0x100);
    }

    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(testarray));

    final boolean[] read = in.readBoolArray(-1);

    assertEquals(16384, in.getCounter());


    assertEquals(testarray.length, read.length);
    for (int i = 0; i < read.length; i++) {
      assertEquals(read[i], (testarray[i] != 0));
    }
  }

  @Test
  public void testReadNotFullByteArrayAfterBitReading() throws Exception {
    final JBBPBitInputStream in = new JBBPBitInputStream(
        new ByteArrayInputStream(new byte[] {(byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0xDD}));
    assertEquals(0x2, in.readBits(BITS_4));
    assertEquals(0, in.getCounter());

    final byte[] readarray = new byte[6];
    final int read = in.read(readarray, 0, readarray.length);
    assertEquals(4, read);
    assertEquals(4, in.getCounter());
    assertArrayEquals(new byte[] {(byte) 0x41, (byte) 0x63, (byte) 0xD5, (byte) 0x0D, 0, 0},
        readarray);
  }

  @Test
  public void testReadNotFullByteArrayAfterBitReading_MSB0() throws Exception {
    final JBBPBitInputStream in = new JBBPBitInputStream(
        new ByteArrayInputStream(new byte[] {(byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0xDD}),
        JBBPBitOrder.MSB0);
    assertEquals(0x8, in.readBits(BITS_4));

    final byte[] readarray = new byte[6];
    final int read = in.read(readarray, 0, readarray.length);

    assertEquals(4, read);
    assertEquals(4, in.getCounter());

    assertArrayEquals(new byte[] {(byte) 0xC4, (byte) 0xA2, (byte) 0xB6, (byte) 0x0B, 0, 0},
        readarray);
  }

  @Test
  public void testCheckThatCounterResetDoesntResetFullBitBuffer() throws Exception {
    final JBBPBitInputStream in =
        new JBBPBitInputStream(new ByteArrayInputStream(new byte[] {1, 0x7F}));
    assertEquals(0, in.getBufferedBitsNumber());
    assertTrue(in.hasAvailableData());
    assertEquals(0, in.getCounter());
    assertEquals(8, in.getBufferedBitsNumber());
    in.resetCounter();
    assertEquals(0, in.getCounter());
    assertEquals(8, in.getBufferedBitsNumber());
    assertEquals(1, in.readBits(BITS_1));
    assertEquals(7, in.getBufferedBitsNumber());
    assertEquals(0, in.getCounter());
    in.resetCounter();
    assertEquals(0, in.getBufferedBitsNumber());
    assertEquals(0x7F, in.read());
    assertEquals(1, in.getCounter());
    assertEquals(-1, in.read());
    assertEquals(1, in.getCounter());
  }

  @Test
  public void testByteCounterWithHasAvailableData() throws Exception {
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(
        new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}));
    assertTrue(in.hasAvailableData());
    assertEquals(0, in.getCounter());

    assertTrue(in.readBits(BITS_1) >= 0);
    assertTrue(in.hasAvailableData());
    assertEquals(0, in.getCounter());

    assertTrue(in.readBits(BITS_1) >= 0);
    assertTrue(in.hasAvailableData());
    assertEquals(0, in.getCounter());

    assertTrue(in.readBits(BITS_1) >= 0);
    assertTrue(in.hasAvailableData());
    assertEquals(0, in.getCounter());

    assertTrue(in.readBits(BITS_1) >= 0);
    assertTrue(in.hasAvailableData());
    assertEquals(0, in.getCounter());

    assertTrue(in.readBits(BITS_1) >= 0);
    assertTrue(in.hasAvailableData());
    assertEquals(0, in.getCounter());

    assertTrue(in.readBits(BITS_1) >= 0);
    assertTrue(in.hasAvailableData());
    assertEquals(0, in.getCounter());

    assertTrue(in.readBits(BITS_1) >= 0);
    assertTrue(in.hasAvailableData());
    assertEquals(0, in.getCounter());

    assertTrue(in.readBits(BITS_1) >= 0);
    assertTrue(in.hasAvailableData());
    assertEquals(1, in.getCounter());

    assertTrue(in.readBits(BITS_1) >= 0);
    assertTrue(in.hasAvailableData());
    assertEquals(1, in.getCounter());

    assertTrue(in.readBits(BITS_1) >= 0);
    assertTrue(in.hasAvailableData());
    assertEquals(1, in.getCounter());

    assertTrue(in.readBits(BITS_1) >= 0);
    assertTrue(in.hasAvailableData());
    assertEquals(1, in.getCounter());

    assertTrue(in.readBits(BITS_1) >= 0);
    assertTrue(in.hasAvailableData());
    assertEquals(1, in.getCounter());

    assertTrue(in.readBits(BITS_1) >= 0);
    assertTrue(in.hasAvailableData());
    assertEquals(1, in.getCounter());

    assertTrue(in.readBits(BITS_1) >= 0);
    assertTrue(in.hasAvailableData());
    assertEquals(1, in.getCounter());

    assertTrue(in.readBits(BITS_1) >= 0);
    assertTrue(in.hasAvailableData());
    assertEquals(1, in.getCounter());

    assertTrue(in.readBits(BITS_1) >= 0);
    assertTrue(in.hasAvailableData());
    assertEquals(2, in.getCounter());
  }

  @FunctionalInterface
  private interface StreamAndIntSupplier {
    Pair<JBBPBitInputStream, Integer> getData() throws Exception;
  }

}
