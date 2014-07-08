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
package com.igormaznitsa.jbbp.io;

import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class JBBPBitInputStreamTest {

  private final String TEST_BYTES = "01001100_01110000_11110000_01111100_00001111_11000000_01100111_00000000_10011111";
  private final String TEST_BYTES_EXTRABIT = "0 01001100_01110000_11110000_01111100_00001111_11000000_01100111_00000000_10011111";

  private static JBBPBitInputStream asInputStream(final byte... array) {
    return new JBBPBitInputStream(new ByteArrayInputStream(array));
  }

  private static JBBPBitInputStream asInputStreamMSB0(final byte... array) {
    return new JBBPBitInputStream(new ByteArrayInputStream(array),JBBPBitOrder.MSB0);
  }

  @Test(expected = EOFException.class)
  public void testReadUnsignedShort_EOFforEmpty_BigEndian_EOFException() throws Exception {
    asInputStream(new byte[0]).readUnsignedShort(JBBPByteOrder.BIG_ENDIAN);
  }

  @Test(expected = EOFException.class)
  public void testReadUnsignedShort_EOFforEmpty_LittleEndian_EOFException() throws Exception {
    asInputStream(new byte[0]).readUnsignedShort(JBBPByteOrder.LITTLE_ENDIAN);
  }

  @Test(expected = EOFException.class)
  public void testReadUnsignedShort_EOFforOneByte_BigEndian_EOFException() throws Exception {
    asInputStream(new byte[]{1}).readUnsignedShort(JBBPByteOrder.BIG_ENDIAN);
  }

  @Test(expected = EOFException.class)
  public void testReadUnsignedShort_EOFforOneByte_LittleEndian_EOFException() throws Exception {
    asInputStream(new byte[]{1}).readUnsignedShort(JBBPByteOrder.LITTLE_ENDIAN);
  }

  @Test
  public void testReadUnsignedShort_BigEndian() throws Exception {
    assertEquals(0x1234, asInputStream(new byte[]{0x12, 0x34}).readUnsignedShort(JBBPByteOrder.BIG_ENDIAN));
  }

  @Test
  public void testReadUnsignedShort_BigEndian_MSB0() throws Exception {
    assertEquals(0x482C, asInputStreamMSB0(new byte[]{0x12, 0x34}).readUnsignedShort(JBBPByteOrder.BIG_ENDIAN));
  }

  @Test
  public void testReadUnsignedShort_LittleEndian() throws Exception {
    assertEquals(0x3412, asInputStream(new byte[]{0x12, 0x34}).readUnsignedShort(JBBPByteOrder.LITTLE_ENDIAN));
  }

  @Test
  public void testReadUnsignedShort_LittleEndian_MSB0() throws Exception {
    assertEquals(0x2C48, asInputStreamMSB0(new byte[]{0x12, 0x34}).readUnsignedShort(JBBPByteOrder.LITTLE_ENDIAN));
  }

  @Test
  public void testReadInt_BigEndian() throws Exception {
    assertEquals(0x12345678, asInputStream(new byte[]{0x12, 0x34, 0x56, 0x78}).readInt(JBBPByteOrder.BIG_ENDIAN));
  }

  @Test
  public void testReadInt_BigEndian_MSB0() throws Exception {
    assertEquals(0x482C6A1E, asInputStreamMSB0(new byte[]{0x12, 0x34, 0x56, 0x78}).readInt(JBBPByteOrder.BIG_ENDIAN));
  }

  @Test
  public void testReadInt_LittleEndian() throws Exception {
    assertEquals(0x78563412, asInputStream(new byte[]{0x12, 0x34, 0x56, 0x78}).readInt(JBBPByteOrder.LITTLE_ENDIAN));
  }

  @Test
  public void testReadInt_LittleEndian_MSB0() throws Exception {
    assertEquals(0x1E6A2C48, asInputStreamMSB0(new byte[]{0x12, 0x34, 0x56, 0x78}).readInt(JBBPByteOrder.LITTLE_ENDIAN));
  }

  @Test
  public void testReadLong_BigEndian() throws Exception {
    assertEquals(0x12345678AABBCCDDL, asInputStream(new byte[]{0x12, 0x34, 0x56, 0x78, (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD}).readLong(JBBPByteOrder.BIG_ENDIAN));
  }

  @Test
  public void testReadLong_LittleEndian() throws Exception {
    assertEquals(0xDDCCBBAA78563412L, asInputStream(new byte[]{0x12, 0x34, 0x56, 0x78, (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD}).readLong(JBBPByteOrder.LITTLE_ENDIAN));
  }

  @Test
  public void testRead9bit() throws Exception {
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{(byte) 0xDA, 1}));

    assertEquals(0xA, in.readBits(JBBPBitNumber.BITS_4));
    assertEquals(0x1D, in.readBits(JBBPBitNumber.BITS_5));
    assertEquals(0, in.read());
    assertEquals(-1, in.read());
  }

  @Test
  public void testRead9bit_MSB0() throws Exception {
    final JBBPBitInputStream in = asInputStreamMSB0((byte) 0xD9, (byte)1);

    assertEquals(0x0B, in.readBits(JBBPBitNumber.BITS_4));
    assertEquals(0x09, in.readBits(JBBPBitNumber.BITS_5));
    assertEquals(0x40,in.read());
    assertEquals(-1, in.read());
  }

  @Test
  public void testGetOrder() throws Exception {
    assertEquals(JBBPBitOrder.MSB0, new JBBPBitInputStream(null, JBBPBitOrder.MSB0).getBitOrder());
    assertEquals(JBBPBitOrder.LSB0, new JBBPBitInputStream(null, JBBPBitOrder.LSB0).getBitOrder());
    assertEquals(JBBPBitOrder.LSB0, new JBBPBitInputStream(null).getBitOrder());
  }

  @Test
  public void testAlignByte() throws Exception {
    final byte[] testarray = JBBPUtils.str2bin("01111001 10111000");

    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(testarray));
    assertEquals(0x19, in.readBits(JBBPBitNumber.BITS_5));
    in.alignByte();
    assertEquals(0xB8, in.read());
    assertEquals(-1, in.read());
  }

  @Test
  public void testAlignByte_IfWeHaveBufferedByteInBitBuffer() throws Exception {
    final byte[] testarray = JBBPUtils.str2bin("01111001 10111000");

    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(testarray));
    assertEquals(0x19, in.readBits(JBBPBitNumber.BITS_5));
    assertEquals(0x03, in.readBits(JBBPBitNumber.BITS_3));
    assertTrue(in.hasAvailableData());
    in.alignByte();
    assertEquals(0xB8, in.read());
    assertEquals(-1, in.read());
  }

  @Test
  public void testReadStream_AsBits() throws IOException {
    final byte[] testarray = JBBPUtils.str2bin(TEST_BYTES, JBBPBitOrder.MSB0);

    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(testarray));

    assertEquals(0, in.readBits(JBBPBitNumber.BITS_1));
    assertEquals(1, in.readBits(JBBPBitNumber.BITS_1));
    assertEquals(1, in.getCounter());
    assertEquals(6, in.getBufferedBitsNumber());

    assertEquals(0, in.readBits(JBBPBitNumber.BITS_2));
    assertEquals(3, in.readBits(JBBPBitNumber.BITS_2));
    assertEquals(1, in.getCounter());
    assertEquals(2, in.getBufferedBitsNumber());

    assertEquals(0, in.readBits(JBBPBitNumber.BITS_3));
    assertEquals(7, in.readBits(JBBPBitNumber.BITS_3));
    assertEquals(2, in.getCounter());
    assertEquals(4, in.getBufferedBitsNumber());

    assertEquals(0, in.readBits(JBBPBitNumber.BITS_4));
    assertEquals(15, in.readBits(JBBPBitNumber.BITS_4));
    assertEquals(3, in.getCounter());
    assertEquals(4, in.getBufferedBitsNumber());

    assertEquals(0, in.readBits(JBBPBitNumber.BITS_5));
    assertEquals(31, in.readBits(JBBPBitNumber.BITS_5));
    assertEquals(4, in.getCounter());
    assertEquals(2, in.getBufferedBitsNumber());

    assertEquals(0, in.readBits(JBBPBitNumber.BITS_6));
    assertEquals(63, in.readBits(JBBPBitNumber.BITS_6));
    assertEquals(6, in.getCounter());
    assertEquals(6, in.getBufferedBitsNumber());

    assertEquals(0, in.readBits(JBBPBitNumber.BITS_7));
    assertEquals(0x73, in.readBits(JBBPBitNumber.BITS_7));
    assertEquals(7, in.getCounter());
    assertEquals(0, in.getBufferedBitsNumber());

    assertEquals(0, in.readBits(JBBPBitNumber.BITS_8));
    assertEquals(8, in.getCounter());
    assertEquals(0, in.getBufferedBitsNumber());

    assertEquals(0xF9, in.readBits(JBBPBitNumber.BITS_8));
    assertEquals(9, in.getCounter());
    assertEquals(0, in.getBufferedBitsNumber());

    assertEquals(-1, in.read());

    assertEquals(9, in.getCounter());
  }

  @Test
  public void testReadStream_BitByBit() throws IOException {
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{0x01}));
    assertEquals(1, in.readBits(JBBPBitNumber.BITS_1));
    assertEquals(0, in.readBits(JBBPBitNumber.BITS_1));
    assertEquals(0, in.readBits(JBBPBitNumber.BITS_1));
    assertEquals(0, in.readBits(JBBPBitNumber.BITS_1));
    assertEquals(0, in.readBits(JBBPBitNumber.BITS_1));
    assertEquals(0, in.readBits(JBBPBitNumber.BITS_1));
    assertEquals(0, in.readBits(JBBPBitNumber.BITS_1));
    assertEquals(0, in.readBits(JBBPBitNumber.BITS_1));
    assertEquals(-1, in.readBits(JBBPBitNumber.BITS_1));
  }

  @Test
  public void testReadStream_7bits_Default() throws IOException {
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils.str2bin("11011100", JBBPBitOrder.MSB0)));
    assertEquals(0x3B, in.readBits(JBBPBitNumber.BITS_7));
  }

  @Test
  public void testReadStream_AsBytes() throws IOException {
    final byte[] testarray = JBBPUtils.str2bin(TEST_BYTES, JBBPBitOrder.MSB0);

    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(testarray));

    for (int i = 0; i < testarray.length; i++) {
      assertEquals("Byte " + i, testarray[i] & 0xFF, in.read());
    }
    assertEquals(9, in.getCounter());
    assertEquals(-1, in.read());
  }

  @Test
  public void testReadStream_AsArray() throws IOException {
    final byte[] testarray = JBBPUtils.str2bin(TEST_BYTES, JBBPBitOrder.MSB0);

    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(testarray));

    final byte[] read = new byte[9];
    assertEquals(9, in.read(read));
    assertEquals(9, in.getCounter());
    assertEquals(-1, in.read());

    assertArrayEquals(testarray, read);
  }

  @Test
  public void testReadStream_AsPartOfArray() throws IOException {
    final byte[] testarray = JBBPUtils.str2bin(TEST_BYTES, JBBPBitOrder.MSB0);

    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(testarray));

    final byte[] buff = new byte[27];
    assertEquals(5, in.read(buff, 9, 5));
    assertEquals(5, in.getCounter());
    assertEquals(3, in.read());

    for (int i = 0; i < 9; i++) {
      assertEquals(0, buff[i]);
    }

    for (int i = 9; i < 14; i++) {
      assertEquals(testarray[i - 9], buff[i]);
    }

    for (int i = 14; i < 27; i++) {
      assertEquals(0, buff[i]);
    }

  }

  @Test
  public void testReadStream_AsPartOfArray_1bitOffset() throws IOException {
    final byte[] testarray = JBBPUtils.str2bin(TEST_BYTES, JBBPBitOrder.MSB0);

    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils.str2bin(TEST_BYTES_EXTRABIT, JBBPBitOrder.MSB0)));

    assertEquals(0, in.readBits(JBBPBitNumber.BITS_1));

    final byte[] read = new byte[27];
    assertEquals(5, in.read(read, 9, 5));
    assertEquals(6, in.getCounter());
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
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils.str2bin("10010110_00101000_10101010", JBBPBitOrder.MSB0)));

    assertEquals(0x9, in.readBits(JBBPBitNumber.BITS_4));
    assertEquals(0x6, in.readBits(JBBPBitNumber.BITS_6));

    assertTrue(in.markSupported());

    in.mark(1024);

    assertEquals(5, in.readBits(JBBPBitNumber.BITS_3));
    assertEquals(0, in.readBits(JBBPBitNumber.BITS_3));
    assertEquals(0x55, in.read());

    in.reset();

    assertEquals(5, in.readBits(JBBPBitNumber.BITS_3));
    assertEquals(0, in.readBits(JBBPBitNumber.BITS_3));
    assertEquals(0x55, in.read());

    assertEquals(-1, in.read());
  }

  @Test
  public void testReadBits_ExceptionForWrongArgument() throws Exception {
    final JBBPBitInputStream inLe = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils.str2bin(TEST_BYTES, JBBPBitOrder.LSB0)));

    try {
      inLe.readBits(JBBPBitNumber.decode(0));
      fail("Must throw IAE");
    }
    catch (IllegalArgumentException ex) {

    }

    try {
      inLe.readBits(JBBPBitNumber.decode(-5));
      fail("Must throw IAE");
    }
    catch (IllegalArgumentException ex) {

    }

    try {
      inLe.readBits(JBBPBitNumber.decode(9));
      fail("Must throw IAE");
    }
    catch (IllegalArgumentException ex) {

    }
  }

  @Test
  public void testSkipBytes() throws Exception {
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils.str2bin("01010101_01010101_01010101_00011000_01010101_01010101_00000001", JBBPBitOrder.MSB0)));

    assertEquals(3, in.skip(3));
    assertEquals(0x18, in.read());
    assertEquals(2, in.skip(2));
    assertEquals(0x80, in.read());
    assertEquals(-1, in.read());
  }

  @Test
  public void testRead_WithoutOffset() throws Exception {
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils.str2bin("01010111_01010111_01010111_00011000_01010101_01100011_00000001", JBBPBitOrder.MSB0)));

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
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils.str2bin("1 01010111_01010111_01010111_00011000_01010111_01100011_00101101", JBBPBitOrder.MSB0)));

    assertEquals(1, in.readBits(JBBPBitNumber.BITS_1));

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
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils.str2bin("0 01010101_01010101_01010101 00011000_01010101_010110110_0000001", JBBPBitOrder.MSB0)));

    assertEquals(0, in.readBits(JBBPBitNumber.BITS_1));

    assertEquals(3, in.skip(3));
    assertEquals(0x18, in.read());
    assertEquals(1, in.skip(1));
    assertEquals(0xDA, in.read());
    assertEquals(0x80, in.read());
    assertEquals(0, in.read());
    assertEquals(-1, in.read());
  }

  @Test
  public void testReadArray_Bits_WholeStream() throws Exception{
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils.str2bin("00101001_11001011_10111110", JBBPBitOrder.LSB0)));
    assertArrayEquals(new byte[]{1,2,2,0, 3,2,0,3, 2,3,3,2}, in.readBitsArray(-1, JBBPBitNumber.BITS_2));
  }
  
  @Test
  public void testReadArray_Bits_ThreeItems() throws Exception{
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils.str2bin("00101001_11001011_10111110", JBBPBitOrder.LSB0)));
    assertArrayEquals(new byte[]{1,2,2}, in.readBitsArray(3, JBBPBitNumber.BITS_2));
  }
  
  @Test
  public void testReadArray_Bytes_WholeStream() throws Exception{
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1,2,3,4,5,6,7,0}));
    assertArrayEquals(new byte[]{1,2,3,4,5,6,7,0}, in.readByteArray(-1));
  }
  
  @Test
  public void testReadArray_Bytes_ThreeItems() throws Exception{
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0}));
    assertArrayEquals(new byte[]{1, 2, 3,}, in.readByteArray(3));
  }

  @Test
  public void testReadArray_Short_WholeStream() throws Exception {
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0}));
    assertArrayEquals(new short[]{0x0102,0x0304,0x0506,0x0700}, in.readShortArray(-1,JBBPByteOrder.BIG_ENDIAN));
  }

  @Test
  public void testReadArray_Short_TwoItems() throws Exception {
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0}));
    assertArrayEquals(new short[]{0x0102,0x0304}, in.readShortArray(2,JBBPByteOrder.BIG_ENDIAN));
  }

  @Test
  public void testReadArray_Int_WholeStream() throws Exception {
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01}));
    assertArrayEquals(new int[]{0x01020304, 0x05060700, 0xFECABE01}, in.readIntArray(-1,JBBPByteOrder.BIG_ENDIAN));
  }

  @Test
  public void testReadArray_Int_TwoItems() throws Exception {
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0, (byte)0xFE, (byte)0xCA, (byte)0xBE, (byte)0x01}));
    assertArrayEquals(new int[]{0x01020304, 0x05060700}, in.readIntArray(2,JBBPByteOrder.BIG_ENDIAN));
  }

  @Test
  public void testReadArray_Long_WholeStream() throws Exception {
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01, 2,3,4,5,6,7,8,9,1,2,3,4}));
    assertArrayEquals(new long[]{0x0102030405060700L, 0xFECABE0102030405L, 0x0607080901020304L}, in.readLongArray(-1,JBBPByteOrder.BIG_ENDIAN));
  }

  @Test
  public void testReadArray_Long_TwoItems() throws Exception {
    final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01, 2, 3, 4, 5, 6, 7, 8, 9, 1, 2, 3, 4}));
    assertArrayEquals(new long[]{0x0102030405060700L, 0xFECABE0102030405L}, in.readLongArray(2,JBBPByteOrder.BIG_ENDIAN));
  }

  
}
