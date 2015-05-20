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

import com.igormaznitsa.jbbp.exceptions.JBBPIllegalArgumentException;
import static com.igormaznitsa.jbbp.io.JBBPOut.*;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import com.igormaznitsa.jbbp.model.JBBPFieldInt;
import com.igormaznitsa.jbbp.model.JBBPFieldLong;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.*;
import java.lang.reflect.Field;
import static org.junit.Assert.*;
import org.junit.Test;

public class JBBPOutTest {

  @Test
  public void testBeginBin() throws Exception {
    assertArrayEquals(new byte[]{1}, BeginBin().Byte(1).End().toByteArray());
    assertArrayEquals(new byte[]{0x02, 0x01}, BeginBin(JBBPByteOrder.LITTLE_ENDIAN).Short(0x0102).End().toByteArray());
    assertArrayEquals(new byte[]{0x40, (byte) 0x80}, BeginBin(JBBPByteOrder.LITTLE_ENDIAN, JBBPBitOrder.MSB0).Short(0x0102).End().toByteArray());
    assertArrayEquals(new byte[]{(byte) 0x80}, BeginBin(JBBPBitOrder.MSB0).Byte(1).End().toByteArray());
    assertArrayEquals(new byte[]{(byte) 0x80}, BeginBin(1).Byte(0x80).End().toByteArray());

    final ByteArrayOutputStream buffer1 = new ByteArrayOutputStream();
    assertSame(buffer1, BeginBin(buffer1).End());

    final ByteArrayOutputStream buffer2 = new ByteArrayOutputStream();
    BeginBin(buffer2,JBBPByteOrder.LITTLE_ENDIAN, JBBPBitOrder.MSB0).Short(1234).End();
    assertArrayEquals(new byte[]{(byte)0x4b, (byte)0x20}, buffer2.toByteArray());
  }

  @Test
  public void testSkip() throws Exception {
    assertArrayEquals(new byte[]{(byte) 0x01, (byte) 0xFF}, JBBPOut.BeginBin().Bit(1).Skip(0).Byte(0xFF).End().toByteArray());
    assertArrayEquals(new byte[]{(byte) 0x01, 0x00, (byte) 0xFF}, JBBPOut.BeginBin().Bit(1).Skip(1).Byte(0xFF).End().toByteArray());
    assertArrayEquals(new byte[]{(byte) 0x01, 0x00, 0x00, (byte) 0xFF}, JBBPOut.BeginBin().Bit(1).Skip(2).Byte(0xFF).End().toByteArray());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSkip_ErrorForNegativeValue() throws Exception {
    JBBPOut.BeginBin().Skip(-1);
  }

  @Test
  public void testAlign() throws Exception {
    assertArrayEquals(new byte[]{(byte) 0x01, (byte) 0xFF}, JBBPOut.BeginBin().Bit(1).Align().Byte(0xFF).End().toByteArray());
  }

  @Test
  public void testResetCounter() throws Exception {
    assertArrayEquals(new byte[]{1, 2, 0, 0, (byte) 0xFF}, JBBPOut.BeginBin().Byte(1).ResetCounter().Byte(2).Align(3).Byte(0xFF).End().toByteArray());
  }

  @Test
  public void testGetByteCounter() throws Exception {
    final JBBPOut out = JBBPOut.BeginBin();
    assertEquals(0, out.getByteCounter());
    out.Bit(true);
    out.Bit(true);
    out.Bit(true);
    out.Bit(true);
    out.Bit(true);
    out.Bit(true);
    out.Bit(true);
    assertEquals(0, out.getByteCounter());
    out.Bit(true);
    assertEquals(1, out.getByteCounter());
    out.Bit(true);
    out.Byte(new byte[1234]);
    assertEquals(1235, out.getByteCounter());
    out.ResetCounter();
    assertEquals(0, out.getByteCounter());
    out.Bit(true);
    assertEquals(0, out.getByteCounter());
    out.End();
    assertEquals(1, out.getByteCounter());
  }

  @Test
  public void testAlignWithArgument() throws Exception {
    assertEquals(0, JBBPOut.BeginBin().Align(2).End().toByteArray().length);
    assertArrayEquals(new byte[]{(byte) 0x01, (byte) 0xFF}, JBBPOut.BeginBin().Bit(1).Align(1).Byte(0xFF).End().toByteArray());
    assertArrayEquals(new byte[]{(byte) 0xFF}, JBBPOut.BeginBin().Align(3).Byte(0xFF).End().toByteArray());
    assertArrayEquals(new byte[]{0x01, (byte) 0xFF}, JBBPOut.BeginBin().Bit(1).Align(1).Byte(0xFF).End().toByteArray());
    assertArrayEquals(new byte[]{0x01, 0x00, (byte) 0xFF}, JBBPOut.BeginBin().Bit(1).Align(2).Byte(0xFF).End().toByteArray());
    assertArrayEquals(new byte[]{0x01, 0x00, 0x00, 0x00, (byte) 0xFF}, JBBPOut.BeginBin().Bit(1).Align(4).Byte(0xFF).End().toByteArray());
    assertArrayEquals(new byte[]{0x01, 0x02, 0x00, 0x00, (byte) 0xFF}, JBBPOut.BeginBin().Byte(1, 2).Align(4).Byte(0xFF).End().toByteArray());
    assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x00, 0x00, (byte) 0xFF}, JBBPOut.BeginBin().Byte(1, 2, 3).Align(5).Byte(0xFF).End().toByteArray());
    assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04, 0x00, (byte) 0xFF}, JBBPOut.BeginBin().Byte(1, 2, 3, 4).Align(5).Byte(0xFF).End().toByteArray());
    assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, (byte) 0xFF}, JBBPOut.BeginBin().Byte(1, 2, 3, 4, 5).Align(5).Byte(0xFF).End().toByteArray());
    assertArrayEquals(new byte[]{(byte) 0x01, 0x00, 0x02, 0x00, (byte) 0x03}, JBBPOut.BeginBin().Align(2).Byte(1).Align(2).Byte(2).Align(2).Byte(3).End().toByteArray());
    assertArrayEquals(new byte[]{(byte) 0xF1, 0x00, (byte) 0x01, 0x00, 0x02, 0x00, (byte) 0x03}, JBBPOut.BeginBin().Byte(0xF1).Align(2).Byte(1).Align(2).Byte(2).Align(2).Byte(3).End().toByteArray());
    assertArrayEquals(new byte[]{(byte) 0xF1, 0x00, 0x00, (byte) 0x01, 0x00, 00, 0x02, 0x00, 00, (byte) 0x03}, JBBPOut.BeginBin().Byte(0xF1).Align(3).Byte(1).Align(3).Byte(2).Align(3).Byte(3).End().toByteArray());
    assertArrayEquals(new byte[]{0x01, 0x02, 03, 0x04, 0x00, (byte) 0xF1}, JBBPOut.BeginBin().Int(0x01020304).Align(5).Byte(0xF1).End().toByteArray());
    assertArrayEquals(new byte[]{0x01, 0x00, 0x00, 0x00, 0x00, (byte) 0xF1}, JBBPOut.BeginBin().Bit(1).Align(5).Byte(0xF1).End().toByteArray());
  }

  @Test
  public void testEmptyArray() throws Exception {
    assertEquals(0, JBBPOut.BeginBin().End().toByteArray().length);
  }

  @Test
  public void testByte() throws Exception {
    assertArrayEquals(new byte[]{-34}, BeginBin().Byte(-34).End().toByteArray());
  }

  @Test
  public void testByteArrayAsInts() throws Exception {
    assertArrayEquals(new byte[]{1, 3, 0, 2, 4, 1, 3, 7}, BeginBin().Byte(1, 3, 0, 2, 4, 1, 3, 7).End().toByteArray());
  }

  @Test
  public void testByteArrayAsByteArray() throws Exception {
    assertArrayEquals(new byte[]{1, 3, 0, 2, 4, 1, 3, 7}, BeginBin().Byte(new byte[]{1, 3, 0, 2, 4, 1, 3, 7}).End().toByteArray());
  }

  @Test
  public void testByteArrayAsString() throws Exception {
    assertArrayEquals(new byte[]{(byte) 'a', (byte) 'b', (byte) 'c'}, BeginBin().Byte("abc").End().toByteArray());
  }

  @Test
  public void testByteArrayAsString_RussianChars() throws Exception {
    assertArrayEquals(new byte[]{(byte) 0x20, (byte) 0x43, (byte) 0x41}, BeginBin().Byte("Рус").End().toByteArray());
  }

  @Test
  public void testUtf8_OnlyLatinChars() throws Exception {
    assertArrayEquals(new byte[]{(byte) 'a', (byte) 'b', (byte) 'c'}, BeginBin().Utf8("abc").End().toByteArray());
  }

  @Test
  public void testUtf8_RussianChars() throws Exception {
    assertArrayEquals(new byte[]{(byte) 0xD0, (byte) 0xA0, (byte) 0xD1, (byte) 0x83, (byte) 0xD1, (byte) 0x81}, BeginBin().Utf8("Рус").End().toByteArray());
  }

  @Test
  public void testBit() throws Exception {
    assertArrayEquals(new byte[]{1}, BeginBin().Bit(1).End().toByteArray());
  }

  @Test
  public void testBit_MSB0() throws Exception {
    assertArrayEquals(new byte[]{(byte) 0x80}, BeginBin(JBBPByteOrder.BIG_ENDIAN, JBBPBitOrder.MSB0).Bit(1).End().toByteArray());
  }

  @Test
  public void testBit_LSB0() throws Exception {
    assertArrayEquals(new byte[]{(byte) 0x01}, BeginBin(JBBPByteOrder.BIG_ENDIAN, JBBPBitOrder.LSB0).Bit(1).End().toByteArray());
  }

  @Test
  public void testBits_Int() throws Exception {
    assertArrayEquals(new byte[]{0xD}, BeginBin().Bits(JBBPBitNumber.BITS_4, 0xFD).End().toByteArray());
  }

  @Test
  public void testBits_IntArray() throws Exception {
    assertArrayEquals(new byte[]{(byte) 0xED}, BeginBin().Bits(JBBPBitNumber.BITS_4, 0xFD, 0xFE).End().toByteArray());
  }

  @Test
  public void testBits_ByteArray() throws Exception {
    assertArrayEquals(new byte[]{(byte) 0xED}, BeginBin().Bits(JBBPBitNumber.BITS_4, new byte[]{(byte) 0xFD, (byte) 0x8E}).End().toByteArray());
  }

  @Test
  public void testBitArrayAsInts() throws Exception {
    assertArrayEquals(new byte[]{(byte) 0xE3}, BeginBin().Bit(1, 3, 0, 2, 4, 1, 3, 7).End().toByteArray());
    assertArrayEquals(new byte[]{(byte) 0x0B}, BeginBin().Bit(1, 3, 0, 7).End().toByteArray());
  }

  @Test
  public void testBitArrayAsBytes() throws Exception {
    assertArrayEquals(new byte[]{(byte) 0xE3}, BeginBin().Bit(new byte[]{(byte) 1, (byte) 3, (byte) 0, (byte) 2, (byte) 4, (byte) 1, (byte) 3, (byte) 7}).End().toByteArray());
    assertArrayEquals(new byte[]{(byte) 0x0B}, BeginBin().Bit(new byte[]{(byte) 1, (byte) 3, (byte) 0, (byte) 7}).End().toByteArray());
  }

  @Test
  public void testBitArrayAsBooleans() throws Exception {
    assertArrayEquals(new byte[]{(byte) 0xE3}, BeginBin().Bit(true, true, false, false, false, true, true, true).End().toByteArray());
    assertArrayEquals(new byte[]{(byte) 0x0B}, BeginBin().Bit(true, true, false, true).End().toByteArray());
  }

  @Test
  public void testShort() throws Exception {
    assertArrayEquals(new byte[]{0x01, 02}, BeginBin().Short(0x0102).End().toByteArray());
  }

  @Test
  public void testShort_BigEndian() throws Exception {
    assertArrayEquals(new byte[]{0x01, 02}, BeginBin().ByteOrder(JBBPByteOrder.BIG_ENDIAN).Short(0x0102).End().toByteArray());
  }

  @Test(expected = NullPointerException.class)
  public void testShort_String_NPEForNullString() throws Exception {
    BeginBin().Short((String) null).End();
  }

  @Test
  public void testShort_String_BigEndian() throws Exception {
    assertArrayEquals(JBBPUtils.str2UnicodeByteArray(JBBPByteOrder.BIG_ENDIAN, "Hello"), BeginBin().ByteOrder(JBBPByteOrder.BIG_ENDIAN).Short("Hello").End().toByteArray());
  }

  @Test
  public void testShort_String_LittleEndian() throws Exception {
    assertArrayEquals(JBBPUtils.str2UnicodeByteArray(JBBPByteOrder.LITTLE_ENDIAN, "Hello"), BeginBin().ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).Short("Hello").End().toByteArray());
  }

  @Test
  public void testShort_LittleEndian() throws Exception {
    assertArrayEquals(new byte[]{0x02, 01}, BeginBin().ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).Short(0x0102).End().toByteArray());
  }

  @Test
  public void testShortArray_AsIntegers() throws Exception {
    assertArrayEquals(new byte[]{1, 2, 3, 4}, BeginBin().Short(0x0102, 0x0304).End().toByteArray());
  }

  @Test
  public void testShortArray_AsIntegers_BigEndian() throws Exception {
    assertArrayEquals(new byte[]{1, 2, 3, 4}, BeginBin().ByteOrder(JBBPByteOrder.BIG_ENDIAN).Short(0x0102, 0x0304).End().toByteArray());
  }

  @Test
  public void testShortArray_AsIntegers_LittleEndian() throws Exception {
    assertArrayEquals(new byte[]{2, 1, 4, 3}, BeginBin().ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).Short(0x0102, 0x0304).End().toByteArray());
  }

  @Test
  public void testShortArray_AsShorts() throws Exception {
    assertArrayEquals(new byte[]{1, 2, 3, 4}, BeginBin().Short(new short[]{(short) 0x0102, (short) 0x0304}).End().toByteArray());
  }

  @Test
  public void testShortArray_AsShortArray() throws Exception {
    assertArrayEquals(new byte[]{1, 2, 3, 4}, BeginBin().Short(new short[]{(short) 0x0102, (short) 0x0304}).End().toByteArray());
  }

  @Test
  public void testInt() throws Exception {
    assertArrayEquals(new byte[]{0x01, 02, 0x03, 0x04}, BeginBin().Int(0x01020304).End().toByteArray());
  }

  @Test
  public void testIntArray() throws Exception {
    assertArrayEquals(new byte[]{0x01, 02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08}, BeginBin().Int(0x01020304, 0x05060708).End().toByteArray());
  }

  @Test
  public void testInt_BigEndian() throws Exception {
    assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04}, BeginBin().ByteOrder(JBBPByteOrder.BIG_ENDIAN).Int(0x01020304).End().toByteArray());
  }

  @Test
  public void testInt_LittleEndian() throws Exception {
    assertArrayEquals(new byte[]{0x04, 0x03, 0x02, 0x01}, BeginBin().ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).Int(0x01020304).End().toByteArray());
  }

  @Test
  public void testFloat_BigEndian() throws Exception {
    final int flt = Float.floatToIntBits(Float.MAX_VALUE);
    assertArrayEquals(new byte[]{(byte) (flt >>> 24), (byte) (flt >>> 16), (byte) (flt >>> 8), (byte) flt}, BeginBin().ByteOrder(JBBPByteOrder.BIG_ENDIAN).Float(Float.MAX_VALUE).End().toByteArray());
  }

  @Test
  public void testFloat_LittleEndian() throws Exception {
    final int flt = Float.floatToIntBits(Float.MAX_VALUE);
    assertArrayEquals(new byte[]{(byte) flt, (byte) (flt >>> 8), (byte) (flt >>> 16), (byte) (flt >>> 24)}, BeginBin().ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).Float(Float.MAX_VALUE).End().toByteArray());
  }

  @Test
  public void testLong() throws Exception {
    assertArrayEquals(new byte[]{0x01, 02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08}, BeginBin().Long(0x0102030405060708L).End().toByteArray());
  }

  @Test
  public void testLongArray() throws Exception {
    assertArrayEquals(new byte[]{0x01, 02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18}, BeginBin().Long(0x0102030405060708L, 0x1112131415161718L).End().toByteArray());
  }

  @Test
  public void testLong_BigEndian() throws Exception {
    assertArrayEquals(new byte[]{0x01, 02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08}, BeginBin().ByteOrder(JBBPByteOrder.BIG_ENDIAN).Long(0x0102030405060708L).End().toByteArray());
  }

  @Test
  public void testLong_LittleEndian() throws Exception {
    assertArrayEquals(new byte[]{0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01}, BeginBin().ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).Long(0x0102030405060708L).End().toByteArray());
  }

  @Test
  public void testDouble_BigEndian() throws Exception {
    final long dbl = Double.doubleToLongBits(Double.MAX_VALUE);
    final byte[] array = BeginBin().ByteOrder(JBBPByteOrder.BIG_ENDIAN).Double(Double.MAX_VALUE).End().toByteArray();
    assertArrayEquals(new byte[]{(byte) (dbl >>> 56), (byte) (dbl >>> 48), (byte) (dbl >>> 40), (byte) (dbl >>> 32), (byte) (dbl >>> 24), (byte) (dbl >>> 16), (byte) (dbl >>> 8), (byte) dbl}, array);
  }

  @Test
  public void testDouble_LittleEndian() throws Exception {
    final long dbl = Double.doubleToLongBits(Double.MAX_VALUE);
    assertArrayEquals(new byte[]{(byte) dbl, (byte) (dbl >>> 8), (byte) (dbl >>> 16), (byte) (dbl >>> 24), (byte) (dbl >>> 32), (byte) (dbl >>> 40), (byte) (dbl >>> 48), (byte) (dbl >>> 56)}, BeginBin().ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).Double(Double.MAX_VALUE).End().toByteArray());
  }

  @Test
  public void testFlush() throws Exception {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    final JBBPOut out = BeginBin(buffer);

    out.Bit(true);
    assertEquals(0, buffer.size());
    out.Flush();
    assertEquals(1, buffer.size());
  }

  @Test
  public void testExceptionForOperatioOverEndedProcess() throws Exception {
    final JBBPOut out = BeginBin();
    out.ByteOrder(JBBPByteOrder.BIG_ENDIAN).Long(0x0102030405060708L).End();
    try {
      out.Align();
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }

    try {
      out.Align(3);
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }

    try {
      out.Bit(true);
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }

    try {
      out.Bit(true, false);
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }

    try {
      out.Bit((byte) 34);
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }

    try {
      out.Bit(new byte[]{(byte) 34, (byte) 12});
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }

    try {
      out.Bit(34, 12);
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }

    try {
      out.Bits(JBBPBitNumber.BITS_3, 12);
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }

    try {
      out.Bits(JBBPBitNumber.BITS_3, 12, 13, 14);
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }

    try {
      out.Bits(JBBPBitNumber.BITS_3, new byte[]{(byte) 1, (byte) 2, (byte) 3});
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }

    try {
      out.Bool(true);
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }

    try {
      out.Bool(true, false);
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }

    try {
      out.Byte(new byte[]{(byte) 1, (byte) 2, (byte) 3});
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }

    try {
      out.Byte(1);
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }

    try {
      out.Byte((String) null);
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }

    try {
      out.Utf8((String) null);
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }

    try {
      out.Byte(1, 2, 3);
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }

    try {
      out.ByteOrder(JBBPByteOrder.BIG_ENDIAN);
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }

    try {
      out.Flush();
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }

    try {
      out.Int(1);
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }

    try {
      out.Int(1, 2);
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }

    try {
      out.Long(1L);
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }

    try {
      out.Long(1L, 2L);
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }

    try {
      out.Short(1);
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }

    try {
      out.Short(1, 2, 3);
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }

    try {
      out.Short(new short[]{(short) 1, (short) 2, (short) 3});
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }

    try {
      out.End();
      fail("Must throw ISE");
    }
    catch (IllegalStateException ex) {
    }
  }

  @Test
  public void testExternalStreamButNoByteArrayOutputStream() throws Exception {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    final DataOutputStream dout = new DataOutputStream(buffer);

    assertNull(BeginBin(dout).Byte(1, 2, 3).End());
    assertArrayEquals(new byte[]{1, 2, 3}, buffer.toByteArray());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExceptionForBitOrderConfilctInCaseOfUsageBitOutputStream() throws Exception {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream bitstream = new JBBPBitOutputStream(buffer, JBBPBitOrder.LSB0);

    JBBPOut.BeginBin(bitstream, JBBPByteOrder.BIG_ENDIAN, JBBPBitOrder.MSB0);
  }

  @Test
  public void testComplexWriting_1() throws Exception {
    final byte[] array
            = BeginBin().
            Bit(1, 2, 3, 0).
            Bit(true, false, true).
            Align().
            Byte(5).
            Short(1, 2, 3, 4, 5).
            Bool(true, false, true, true).
            Int(0xABCDEF23, 0xCAFEBABE).
            Long(0x123456789ABCDEF1L, 0x212356239091AB32L).
            Utf8("JFIF").
            Byte("Рус").
            End().toByteArray();

    assertEquals(47, array.length);
    assertArrayEquals(new byte[]{
      (byte) 0x55, 5, 0, 1, 0, 2, 0, 3, 0, 4, 0, 5, 1, 0, 1, 1,
      (byte) 0xAB, (byte) 0xCD, (byte) 0xEF, 0x23, (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE,
      0x12, 0x34, 0x56, 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF1, 0x21, 0x23, 0x56, 0x23, (byte) 0x90, (byte) 0x91, (byte) 0xAB, 0x32,
      0x4A, 0x46, 0x49, 0x46,
      (byte) 0x20, (byte) 0x43, (byte) 0x41
    }, array);
  }

  @Test(expected = NullPointerException.class)
  public void testVar_NPEForNullProcessor() throws Exception {
    BeginBin().Var(null).End();
  }

  @Test
  public void testVar_ProcessRest() throws Exception {
    class Test {

      @Bin(outOrder = 1)
      byte a;
      @Bin(outOrder = 2)
      byte b;

      Test(int a, int b) {
        this.a = (byte) a;
        this.b = (byte) b;
      }
    }

    final byte[] array = BeginBin().
            Byte(0xCC).
            Var(new JBBPOutVarProcessor() {

              public boolean processVarOut(final JBBPOut context, final JBBPBitOutputStream outStream, final Object... args) throws IOException {
                assertNotNull(context);
                assertNotNull(outStream);
                assertEquals(0, args.length);
                outStream.write(0xDD);
                return true;
              }
            }).
            Byte(0xAA).
            Bin(new Test(0x12, 0x13)).
            End().toByteArray();

    assertArrayEquals(new byte[]{(byte) 0xCC, (byte) 0xDD, (byte) 0xAA, (byte) 0x12, (byte) 0x13}, array);
  }

  @Test
  public void testVar_SkipRest() throws Exception {
    class Test {

      @Bin(outOrder = 1)
      byte a;
      @Bin(outOrder = 2)
      byte b;

      Test(int a, int b) {
        this.a = (byte) a;
        this.b = (byte) b;
      }
    }

    final byte[] array = BeginBin().
            Byte(0xCC).
            Var(new JBBPOutVarProcessor() {

              public boolean processVarOut(final JBBPOut context, final JBBPBitOutputStream outStream, final Object... args) throws IOException {
                assertNotNull(context);
                assertNotNull(outStream);
                assertEquals(0, args.length);
                outStream.write(0xDD);
                return false;
              }
            }).
            Byte(0xAA).
            Align(15).
            Align().
            Bit(true).
            Bit(34).
            Bit(true, false).
            Bit((byte) 11).
            Bit(new byte[]{(byte) 11, (byte) 45}).
            Bit(111, 222).
            Bits(JBBPBitNumber.BITS_5, 0xFF).
            Bits(JBBPBitNumber.BITS_5, 0xFF, 0xAB).
            Bits(JBBPBitNumber.BITS_5, new byte[]{(byte) 0xFF, (byte) 0xAB}).
            Bool(true).
            Bool(false, false).
            Byte("HURRAAA").
            Byte(new byte[]{(byte) 1, (byte) 2, (byte) 3}).
            Byte(232324).
            Byte(2322342, 2323232).
            ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).
            Int(23432432).
            Int(234234234, 234234234).
            Long(234823948234L).
            Long(234823948234L, 234233243243L).
            Short(234).
            Short(234, 233).
            Short(new short[]{(short) 234, (short) 233}).
            Skip(332).
            Bin(new Test(12, 34)).
            Utf8("werwerew").
            Var(new JBBPOutVarProcessor() {

              public boolean processVarOut(JBBPOut context, JBBPBitOutputStream outStream, Object... args) throws IOException {
                fail("Must not be called");
                return false;
              }
            }).
            End().toByteArray();

    assertArrayEquals(new byte[]{(byte) 0xCC, (byte) 0xDD}, array);
  }

  @Test
  public void testVar_VariableContent() throws Exception {
    final JBBPOutVarProcessor var = new JBBPOutVarProcessor() {
      public boolean processVarOut(JBBPOut context, JBBPBitOutputStream outStream, Object... args) throws IOException {
        final int type = (Integer) args[0];
        switch (type) {
          case 0: {
            context.Int(0x01020304);
          }
          break;
          case 1: {
            context.Int(0x05060708);
          }
          break;
          default: {
            fail("Unexpected parameter [" + type + ']');
          }
          break;
        }
        return true;
      }
    };

    final byte[] array = JBBPOut.BeginBin().
            Var(var, 0).
            Var(var, 1).
            End().toByteArray();

    assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7, 8}, array);
  }

  @Test
  public void testBin_UndefinedType_Byte() throws Exception {
    class Test {

      @Bin(outOrder = 3)
      byte c;
      @Bin(outOrder = 2, bitOrder = JBBPBitOrder.MSB0)
      byte b;
      @Bin(outOrder = 1)
      byte a;

      Test(byte a, byte b, byte c) {
        this.a = a;
        this.b = b;
        this.c = c;
      }
    }
    assertArrayEquals(new byte[]{1, (byte) 0x40, 3}, JBBPOut.BeginBin().Bin(new Test((byte) 1, (byte) 2, (byte) 3)).End().toByteArray());
  }

  @Test
  public void testBin_Byte_StringAsByteArray() throws Exception {
    assertArrayEquals(new byte[0], JBBPOut.BeginBin().Byte("", JBBPBitOrder.LSB0).End().toByteArray());
    assertArrayEquals(new byte[0], JBBPOut.BeginBin().Byte("", JBBPBitOrder.MSB0).End().toByteArray());
    assertArrayEquals(new byte[]{65,66,67,68}, JBBPOut.BeginBin().Byte("ABCD", JBBPBitOrder.LSB0).End().toByteArray());
    assertArrayEquals(new byte[]{(byte)130,66,(byte)194,34}, JBBPOut.BeginBin().Byte("ABCD", JBBPBitOrder.MSB0).End().toByteArray());
  }
  
  @Test
  public void testBin_Byte_StringAsShortArray() throws Exception {
    assertArrayEquals(new byte[0], JBBPOut.BeginBin().Short("", JBBPBitOrder.LSB0).End().toByteArray());
    assertArrayEquals(new byte[0], JBBPOut.BeginBin().Short("", JBBPBitOrder.MSB0).End().toByteArray());
    assertArrayEquals(new byte[]{0x04, 0x10, 0x04, 0x11, 0x04, 0x12, 0x04, 0x13, 0x04, 0x14}, JBBPOut.BeginBin().Short("АБВГД", JBBPBitOrder.LSB0).End().toByteArray());
    assertArrayEquals(new byte[]{0x08, 0x20, (byte)0x88, 0x20, (byte)0x48, 0x20, (byte)0xC8, 0x20, (byte)0x28, 0x20}, JBBPOut.BeginBin().Short("АБВГД", JBBPBitOrder.MSB0).End().toByteArray());
  }
  
  @Bin
  private static class TestWithStaticField {
    static int some = 111;
    
    @Bin(outOrder = 3)
    byte c;
    @Bin(outOrder = 2, bitOrder = JBBPBitOrder.MSB0)
    byte b;
    @Bin(outOrder = 1)
    byte a;

    TestWithStaticField(byte a, byte b, byte c) {
      this.a = a;
      this.b = b;
      this.c = c;
    }
  }

  @Test
  public void testBin_StaticField() throws Exception {    
    assertArrayEquals(new byte[]{1, (byte) 0x40, 3}, JBBPOut.BeginBin().Bin(new TestWithStaticField((byte) 1, (byte) 2, (byte) 3)).End().toByteArray());
  }

  @Test
  public void testBin_UndefinedType_Boolean() throws Exception {
    class Test {

      @Bin(outOrder = 3)
      boolean c;
      @Bin(outOrder = 2, bitOrder = JBBPBitOrder.MSB0)
      boolean b;
      @Bin(outOrder = 1)
      boolean a;

      Test(boolean a, boolean b, boolean c) {
        this.a = a;
        this.b = b;
        this.c = c;
      }
    }
    assertArrayEquals(new byte[]{1, (byte) 0x80, 0}, JBBPOut.BeginBin().Bin(new Test(true, true, false)).End().toByteArray());
  }

  @Test
  public void testBin_BitType_Bits() throws Exception {
    class Test {

      @Bin(outBitNumber = JBBPBitNumber.BITS_4, outOrder = 3, type = BinType.BIT)
      byte c;
      @Bin(outBitNumber = JBBPBitNumber.BITS_4, outOrder = 2, type = BinType.BIT, bitOrder = JBBPBitOrder.MSB0)
      byte b;
      @Bin(outBitNumber = JBBPBitNumber.BITS_4, type = BinType.BIT, outOrder = 1)
      byte a;

      Test(byte a, byte b, byte c) {
        this.a = a;
        this.b = b;
        this.c = c;
      }
    }
    assertArrayEquals(new byte[]{(byte) 0x55, 0x0C}, JBBPOut.BeginBin().Bin(new Test((byte) 0x05, (byte) 0x0A, (byte) 0x0C)).End().toByteArray());
  }

  @Test
  public void testBin_UndefinedType_Short() throws Exception {
    class Test {

      @Bin(outOrder = 3)
      short c;
      @Bin(outOrder = 2, bitOrder = JBBPBitOrder.MSB0)
      short b;
      @Bin(outOrder = 1)
      short a;

      Test(short a, short b, short c) {
        this.a = a;
        this.b = b;
        this.c = c;
      }
    }

    assertArrayEquals(new byte[]{0x01, 0x02, 0x20, (byte) 0xC0, 0x05, 0x06}, JBBPOut.BeginBin().Bin(new Test((short) 0x0102, (short) 0x0304, (short) 0x0506)).End().toByteArray());
  }

  @Test
  public void testBin_UndefinedType_Char() throws Exception {
    class Test {

      @Bin(outOrder = 3)
      char c;
      @Bin(outOrder = 2, bitOrder = JBBPBitOrder.MSB0)
      char b;
      @Bin(outOrder = 1)
      char a;

      Test(char a, char b, char c) {
        this.a = a;
        this.b = b;
        this.c = c;
      }
    }

    assertArrayEquals(new byte[]{0x01, 0x02, 0x20, (byte) 0xC0, 0x05, 0x06}, JBBPOut.BeginBin().Bin(new Test((char) 0x0102, (char) 0x0304, (char) 0x0506)).End().toByteArray());
  }

  @Test
  public void testBin_UndefinedType_Int() throws Exception {
    class Test {

      @Bin(outOrder = 3)
      int c;
      @Bin(outOrder = 2, bitOrder = JBBPBitOrder.MSB0)
      int b;
      @Bin(outOrder = 1)
      int a;

      Test(int a, int b, int c) {
        this.a = a;
        this.b = b;
        this.c = c;
      }
    }

    assertArrayEquals(new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD, (byte) 0x22, (byte) 0xCC, (byte) 0x44, (byte) 0x88, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD, (byte) 0xEE}, JBBPOut.BeginBin().Bin(new Test(0xAABBCCDD, 0x11223344, 0xBBCCDDEE)).End().toByteArray());
  }

  @Test
  public void testBin_UndefinedType_Float() throws Exception {
    class Test {

      @Bin(outOrder = 3)
      float c;
      @Bin(outOrder = 2, bitOrder = JBBPBitOrder.MSB0)
      float b;
      @Bin(outOrder = 1)
      float a;

      Test(float a, float b, float c) {
        this.a = a;
        this.b = b;
        this.c = c;
      }
    }

    assertArrayEquals(JBBPUtils.concat(
            JBBPUtils.splitInteger(Float.floatToIntBits(0.456f), false, null),
            JBBPUtils.splitInteger((int) JBBPFieldInt.reverseBits(Float.floatToIntBits(8.1123f)), false, null),
            JBBPUtils.splitInteger(Float.floatToIntBits(56.123f), false, null)
    ), JBBPOut.BeginBin().Bin(new Test(0.456f, 8.1123f, 56.123f)).End().toByteArray());
  }

  @Test
  public void testBin_UndefinedType_Long() throws Exception {
    class Test {

      @Bin(outOrder = 3)
      long c;
      @Bin(outOrder = 2, bitOrder = JBBPBitOrder.MSB0)
      long b;
      @Bin(outOrder = 1)
      long a;

      Test(long a, long b, long c) {
        this.a = a;
        this.b = b;
        this.c = c;
      }
    }

    assertArrayEquals(JBBPUtils.concat(
            JBBPUtils.splitLong(0xFFAABBCCDD001122L, false, null),
            JBBPUtils.splitLong(JBBPFieldLong.reverseBits(0x0102030405060708L), false, null),
            JBBPUtils.splitLong(0x11223344556677AAL, false, null)
    ), JBBPOut.BeginBin().Bin(new Test(0xFFAABBCCDD001122L, 0x0102030405060708L, 0x11223344556677AAL)).End().toByteArray());
  }

  @Test
  public void testBin_UndefinedType_Double() throws Exception {
    class Test {

      @Bin(outOrder = 3)
      double c;
      @Bin(outOrder = 2, bitOrder = JBBPBitOrder.MSB0)
      double b;
      @Bin(outOrder = 1)
      double a;

      Test(double a, double b, double c) {
        this.a = a;
        this.b = b;
        this.c = c;
      }
    }

    assertArrayEquals(JBBPUtils.concat(
            JBBPUtils.splitLong(Double.doubleToLongBits(34350.456d), false, null),
            JBBPUtils.splitLong(JBBPFieldLong.reverseBits(Double.doubleToLongBits(8829374.1123d)), false, null),
            JBBPUtils.splitLong(Double.doubleToLongBits(3256.123d), false, null)
    ), JBBPOut.BeginBin().Bin(new Test(34350.456d, 8829374.1123d, 3256.123d)).End().toByteArray());
  }

  @Test
  public void testBin_UndefinedType_Struct() throws Exception {
    class Inside {

      @Bin(outOrder = 1)
      byte a;
      @Bin(outOrder = 2)
      byte b;

      Inside(byte a, byte b) {
        this.a = a;
        this.b = b;
      }
    }

    class Test {

      @Bin(outOrder = 2)
      Inside c;
      @Bin(outOrder = 1)
      byte a;
      @Bin(outOrder = 3)
      byte b;

      Test(byte a, byte b, Inside c) {
        this.a = a;
        this.b = b;
        this.c = c;
      }
    }

    assertArrayEquals(new byte[]{1, 3, 4, 2}, JBBPOut.BeginBin().Bin(new Test((byte) 1, (byte) 2, new Inside((byte) 3, (byte) 4))).End().toByteArray());
  }

  @Test
  public void testBin_DefinedType_Array_Bits() throws Exception {
    class Test {

      @Bin(outOrder = 2, outBitNumber = JBBPBitNumber.BITS_4, type = BinType.BIT_ARRAY)
      byte[] array;
      @Bin(outOrder = 3, outBitNumber = JBBPBitNumber.BITS_4, type = BinType.BIT_ARRAY, bitOrder = JBBPBitOrder.MSB0)
      byte[] lsbarray;
      @Bin(outOrder = 1)
      byte prefix;

      Test(byte prefix, byte[] array, byte[] lsbarray) {
        this.prefix = prefix;
        this.array = array;
        this.lsbarray = lsbarray;
      }
    }
    assertArrayEquals(new byte[]{(byte) 0xAA, (byte) 0x21, (byte) 0x43, (byte) 0x6A, (byte) 0x0E},
            JBBPOut.BeginBin().Bin(new Test((byte) 0xAA, new byte[]{1, 2, 3, 4}, new byte[]{5, 6, 7})).End().toByteArray());
  }

  @Test
  public void testBin_UndefinedType_Array_Bytes() throws Exception {
    class Test {

      @Bin(outOrder = 2, bitOrder = JBBPBitOrder.MSB0)
      byte[] lsbarray;
      @Bin(outOrder = 1)
      byte[] array;

      Test(byte[] array, byte[] lsbarray) {
        this.array = array;
        this.lsbarray = lsbarray;
      }
    }
    assertArrayEquals(new byte[]{(byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0xA0, (byte) 0x60, (byte) 0xE0},
            JBBPOut.BeginBin().Bin(new Test(new byte[]{1, 2, 3}, new byte[]{5, 6, 7})).End().toByteArray());
  }

  @Test
  public void testBin_UndefinedType_Array_Boolean() throws Exception {
    class Test {

      @Bin(outOrder = 2, bitOrder = JBBPBitOrder.MSB0)
      boolean[] lsbarray;
      @Bin(outOrder = 1)
      boolean[] array;

      Test(boolean[] array, boolean[] lsbarray) {
        this.array = array;
        this.lsbarray = lsbarray;
      }
    }
    assertArrayEquals(new byte[]{0x01, 0x00, 0x01, (byte) 0x80, 0x00, (byte) 0x80},
            JBBPOut.BeginBin().Bin(new Test(new boolean[]{true, false, true}, new boolean[]{true, false, true})).End().toByteArray());
  }

  @Test
  public void testBin_UndefinedType_Array_Bytes_String() throws Exception {
    class Test {

      @Bin(outOrder = 2, bitOrder = JBBPBitOrder.MSB0)
      String lsbarray;
      @Bin(outOrder = 1)
      String array;

      Test(String array, String lsbarray) {
        this.array = array;
        this.lsbarray = lsbarray;
      }
    }
    assertArrayEquals(new byte[]{(byte) 'H', (byte) 'A', (byte) 'L', (byte) 0x32, (byte) 0xF2},
            JBBPOut.BeginBin().Bin(new Test("HAL", "LO")).End().toByteArray());
  }

  @Test
  public void testBin_UndefinedType_Array_Short() throws Exception {
    class Test {

      @Bin(outOrder = 2, bitOrder = JBBPBitOrder.MSB0)
      short[] lsbarray;
      @Bin(outOrder = 1)
      short[] array;

      Test(short[] array, short[] lsbarray) {
        this.array = array;
        this.lsbarray = lsbarray;
      }
    }
    assertArrayEquals(new byte[]{(byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0xA0, (byte) 0x60, (byte) 0x60, (byte) 0xE0, (byte) 0xE0, (byte) 0x00},
            JBBPOut.BeginBin().Bin(new Test(new short[]{0x0101, 0x0102, 0x0103}, new short[]{0x0605, 0x0706, 0x0007})).End().toByteArray());
  }

  @Test
  public void testBin_DefinedType_Array_Short_String() throws Exception {
    class Test {

      @Bin(outOrder = 2, bitOrder = JBBPBitOrder.MSB0, type = BinType.SHORT_ARRAY)
      String lsbarray;
      @Bin(outOrder = 1, type = BinType.SHORT_ARRAY)
      String array;

      Test(String array, String lsbarray) {
        this.array = array;
        this.lsbarray = lsbarray;
      }
    }
    assertArrayEquals(new byte[]{(byte) 0x04, (byte) 0x1F, (byte) 0x04, (byte) 0x20, (byte) 0x18, (byte) 0x20, (byte) 0x48, (byte) 0x20},
            JBBPOut.BeginBin().Bin(new Test("ПР", "ИВ")).End().toByteArray());
  }

  @Test
  public void testBin_UndefinedType_Array_Int() throws Exception {
    class Test {

      @Bin(outOrder = 2, bitOrder = JBBPBitOrder.MSB0)
      int[] lsbarray;
      @Bin(outOrder = 1)
      int[] array;

      Test(int[] array, int[] lsbarray) {
        this.array = array;
        this.lsbarray = lsbarray;
      }
    }
    assertArrayEquals(new byte[]{(byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x66, (byte) 0x77, (byte) 0x88,
      (byte) 0x48, (byte) 0xF7, (byte) 0xB3, (byte) 0xD5},
            JBBPOut.BeginBin().Bin(new Test(new int[]{0x11223344, 0x55667788}, new int[]{0xABCDEF12})).End().toByteArray());
  }

  @Test
  public void testBin_UndefinedType_Array_Float() throws Exception {
    class Test {

      @Bin(outOrder = 2, bitOrder = JBBPBitOrder.MSB0)
      float[] lsbarray;
      @Bin(outOrder = 1)
      float[] array;

      Test(float[] array, float[] lsbarray) {
        this.array = array;
        this.lsbarray = lsbarray;
      }
    }
    assertArrayEquals(JBBPUtils.concat(
            JBBPUtils.splitInteger(Float.floatToIntBits(23.4546f), false, null), JBBPUtils.splitInteger(Float.floatToIntBits(123.32f), false, null),
            JBBPUtils.splitInteger((int) JBBPFieldInt.reverseBits(Float.floatToIntBits(11.98872f)), false, null),
            JBBPUtils.splitInteger((int) JBBPFieldInt.reverseBits(Float.floatToIntBits(-234.322f)), false, null)
    ),
            JBBPOut.BeginBin().Bin(new Test(new float[]{23.4546f, 123.32f}, new float[]{11.98872f, -234.322f})).End().toByteArray());
  }

  @Test
  public void testBin_UndefinedType_Array_Long() throws Exception {
    class Test {

      @Bin(outOrder = 2, bitOrder = JBBPBitOrder.MSB0)
      long[] lsbarray;
      @Bin(outOrder = 1)
      long[] array;

      Test(long[] array, long[] lsbarray) {
        this.array = array;
        this.lsbarray = lsbarray;
      }
    }
    assertArrayEquals(JBBPUtils.concat(
            JBBPUtils.splitLong(0x1122334455667788L, false, null), JBBPUtils.splitLong(0xAABBCCDDEEFF1122L, false, null),
            JBBPUtils.splitLong(JBBPFieldLong.reverseBits(0x0102030405060708L), false, null),
            JBBPUtils.splitLong(JBBPFieldLong.reverseBits(0xCAFEBABE12345334L), false, null)
    ),
            JBBPOut.BeginBin().Bin(new Test(new long[]{0x1122334455667788L, 0xAABBCCDDEEFF1122L}, new long[]{0x0102030405060708L, 0xCAFEBABE12345334L})).End().toByteArray());
  }

  @Test
  public void testBin_UndefinedType_Array_Object() throws Exception {
    class Inner {

      @Bin(outOrder = 1)
      byte a;
      @Bin(outOrder = 2)
      byte b;

      Inner(byte a, byte b) {
        this.a = a;
        this.b = b;
      }
    }

    class Test {

      @Bin(outOrder = 2)
      Inner[] inner;
      @Bin(outOrder = 1)
      byte prefix;

      Test(byte prefix, Inner[] inner) {
        this.inner = inner;
        this.prefix = prefix;
      }
    }
    assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07},
            JBBPOut.BeginBin().Bin(new Test((byte) 0x01, new Inner[]{new Inner((byte) 0x02, (byte) 0x03), new Inner((byte) 0x04, (byte) 0x05), new Inner((byte) 0x06, (byte) 0x07)})).End().toByteArray());

  }
  
  @Test
  public void testBin_TwoFieldWithTheSameorder() throws Exception {
    class Test {

      @Bin(outOrder = 1)
      byte a;
      @Bin(outOrder = 1)
      byte b;

      Test(byte a, byte b) {
        this.a = a;
        this.b = b;
      }
    }

    assertEquals(2, JBBPOut.BeginBin().Bin(new Test((byte)12,(byte)24)).End().toByteArray().length);
  }
  
  @Test(expected = JBBPIllegalArgumentException.class)
  public void testBin_CustomField_ErrorBecauseNoCustomWriter() throws Exception {
    class Test {

      @Bin(outOrder = 1)
      byte a;
      @Bin(outOrder = 2,custom = true)
      byte b;

      Test(byte a, byte b) {
        this.a = a;
        this.b = b;
      }
    }

    JBBPOut.BeginBin().Bin(new Test((byte)12,(byte)24));
  }
  
  @Test
  public void testBin_CustomField_NoError() throws Exception {
    class Test {
      @Bin(outOrder = 1)
      byte a;
      @Bin(outOrder = 2,custom = true)
      byte b;

      Test(byte a, byte b) {
        this.a = a;
        this.b = b;
      }
    }

    assertArrayEquals(new byte[]{1,2,3}, JBBPOut.BeginBin().Bin(new Test((byte)1,(byte)0),new JBBPCustomFieldWriter() {
      public void writeCustomField(JBBPOut context, JBBPBitOutputStream outStream, Object instanceToSave, Field instanceCustomField, Bin fieldAnnotation, Object value) throws IOException {
        assertNotNull(context);
        assertNotNull(outStream);
        assertNotNull(instanceToSave);
        assertNotNull(instanceCustomField);
        assertNotNull(fieldAnnotation);
        assertEquals("b",instanceCustomField.getName());
        assertTrue(instanceToSave.getClass() == instanceCustomField.getDeclaringClass());
        
        context.Byte(2,3);
      }
    }).End().toByteArray());
  }
  
  

}
