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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


import com.igormaznitsa.jbbp.utils.JBBPUtils;
import com.igormaznitsa.jbbp.utils.SpecialTestUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class JBBPBitOutputStreamTest {

  private static byte[] writeString(final JBBPByteOrder order, final String str)
      throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(bos);
    out.writeString(str, order);
    out.close();
    return bos.toByteArray();
  }

  private static byte[] writeStrings(final JBBPByteOrder order, final String... array)
      throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(bos);
    out.writeStringArray(array, order);
    out.close();
    return bos.toByteArray();
  }

  @Test
  public void testResetCounter_BitBufferEmpty() throws Exception {
    final ByteArrayOutputStream buff = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(buff);

    assertEquals(0L, out.getCounter());
    out.write(1);
    assertTrue(out.getBufferedBitsNumber() == 0);
    assertEquals(1L, out.getCounter());
    out.resetCounter();
    assertTrue(out.getBufferedBitsNumber() == 0);
    assertEquals(0L, out.getCounter());
  }

  @Test
  public void testResetCounter_BitBufferNotEmpty() throws Exception {
    final ByteArrayOutputStream buff = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(buff);

    assertEquals(0L, out.getCounter());
    out.write(1);
    out.writeBits(3, JBBPBitNumber.BITS_7);
    assertTrue(out.getBufferedBitsNumber() > 0);
    assertEquals(1L, out.getCounter());
    out.resetCounter();
    assertTrue(out.getBufferedBitsNumber() == 0);
    assertEquals(0L, out.getCounter());
    out.write(2);
    out.close();
    assertArrayEquals(new byte[] {1, 2}, buff.toByteArray());
  }

  @Test
  public void testWrite9bit_MSB() throws Exception {
    final ByteArrayOutputStream buff = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(buff);

    out.writeBits(0x9, JBBPBitNumber.BITS_4);
    out.writeBits(0x1D, JBBPBitNumber.BITS_5);
    out.close();

    assertArrayEquals(new byte[] {(byte) 0xD9, 1}, buff.toByteArray());
  }

  @Test
  public void testGetOrder() throws Exception {
    assertEquals(JBBPBitOrder.MSB0, new JBBPBitOutputStream(null, JBBPBitOrder.MSB0).getBitOrder());
    assertEquals(JBBPBitOrder.LSB0, new JBBPBitOutputStream(null, JBBPBitOrder.LSB0).getBitOrder());
    assertEquals(JBBPBitOrder.LSB0, new JBBPBitOutputStream(null).getBitOrder());
  }

  @Test
  public void testWriteBytes_BIG_ENDIAN_wholeArray() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer);
    assertEquals(0, out.getCounter());
    out.writeBytes(new byte[] {1, 2, 3, 4, 5}, -1, JBBPByteOrder.BIG_ENDIAN);
    out.flush();
    assertEquals(5, out.getCounter());
    assertArrayEquals(new byte[] {1, 2, 3, 4, 5}, outBiuffer.toByteArray());
  }

  @Test
  public void testWriteBytes_BIG_ENDIAN_zeroArray() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer);
    assertEquals(0, out.getCounter());
    out.writeBytes(new byte[] {1, 2, 3, 4, 5}, 0, JBBPByteOrder.BIG_ENDIAN);
    out.flush();
    assertEquals(0, out.getCounter());
    assertArrayEquals(new byte[0], outBiuffer.toByteArray());
  }

  @Test
  public void testWriteBytes_BIG_ENDIAN_threeItemsOfArray() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer);
    assertEquals(0, out.getCounter());
    out.writeBytes(new byte[] {1, 2, 3, 4, 5}, 3, JBBPByteOrder.BIG_ENDIAN);
    out.flush();
    assertEquals(3, out.getCounter());
    assertArrayEquals(new byte[] {1, 2, 3}, outBiuffer.toByteArray());
  }

  @Test
  public void testWriteBytes_LITTLE_ENDIAN_threeItemsOfArray() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer);
    assertEquals(0, out.getCounter());
    out.writeBytes(new byte[] {1, 2, 3, 4, 5}, 3, JBBPByteOrder.LITTLE_ENDIAN);
    out.flush();
    assertEquals(3, out.getCounter());
    assertArrayEquals(new byte[] {3, 2, 1}, outBiuffer.toByteArray());
  }

  @Test
  public void testWriteBytes_LITTLE_ENDIAN_zeroArray() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer);
    assertEquals(0, out.getCounter());
    out.writeBytes(new byte[] {1, 2, 3, 4, 5}, 0, JBBPByteOrder.LITTLE_ENDIAN);
    out.flush();
    assertEquals(0, out.getCounter());
    assertArrayEquals(new byte[0], outBiuffer.toByteArray());
  }

  @Test
  public void testWriteBytes_LITTLE_ENDIAN_wholeArray() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer);
    assertEquals(0, out.getCounter());
    out.writeBytes(new byte[] {1, 2, 3, 4, 5}, -1, JBBPByteOrder.LITTLE_ENDIAN);
    out.flush();
    assertEquals(5, out.getCounter());
    assertArrayEquals(new byte[] {5, 4, 3, 2, 1}, outBiuffer.toByteArray());
  }

  @Test
  public void testWriteByte() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer);
    assertEquals(0, out.getCounter());
    out.write(0x12);
    assertEquals(1, out.getCounter());
    out.flush();
    assertEquals(1, out.getCounter());
    assertArrayEquals(new byte[] {0x12}, outBiuffer.toByteArray());
  }

  @Test
  public void testWriteByte_MSB0() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer, JBBPBitOrder.MSB0);
    assertEquals(0, out.getCounter());
    out.write(0x12);
    assertEquals(1, out.getCounter());
    out.flush();
    assertEquals(1, out.getCounter());
    assertArrayEquals(new byte[] {0x48}, outBiuffer.toByteArray());
  }

  @Test
  public void testWriteShort_BigEndian() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer);
    assertEquals(0, out.getCounter());
    out.writeShort(0x1234, JBBPByteOrder.BIG_ENDIAN);
    assertEquals(2, out.getCounter());
    out.flush();
    assertEquals(2, out.getCounter());
    assertArrayEquals(new byte[] {0x12, 0x34}, outBiuffer.toByteArray());
  }

  @Test
  public void testWriteShort_BigEndian_MSB0() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer, JBBPBitOrder.MSB0);
    assertEquals(0, out.getCounter());
    out.writeShort(0x1234, JBBPByteOrder.BIG_ENDIAN);
    assertEquals(2, out.getCounter());
    out.flush();
    assertEquals(2, out.getCounter());
    assertArrayEquals(new byte[] {0x48, 0x2C}, outBiuffer.toByteArray());
  }

  @Test
  public void testWriteShort_LittleEndian() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer);
    assertEquals(0, out.getCounter());
    out.writeShort(0x1234, JBBPByteOrder.LITTLE_ENDIAN);
    assertEquals(2, out.getCounter());
    out.flush();
    assertEquals(2, out.getCounter());
    assertArrayEquals(new byte[] {0x34, 0x12}, outBiuffer.toByteArray());
  }

  @Test
  public void testWriteShort_LittleEndian_MSB0() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer, JBBPBitOrder.MSB0);
    assertEquals(0, out.getCounter());
    out.writeShort(0x1234, JBBPByteOrder.LITTLE_ENDIAN);
    assertEquals(2, out.getCounter());
    out.flush();
    assertEquals(2, out.getCounter());
    assertArrayEquals(new byte[] {0x2C, 0x48}, outBiuffer.toByteArray());
  }

  @Test
  public void testWriteInt_BigEndian() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer);
    assertEquals(0, out.getCounter());
    out.writeInt(0x12345678, JBBPByteOrder.BIG_ENDIAN);
    assertEquals(4, out.getCounter());
    out.flush();
    assertEquals(4, out.getCounter());
    assertArrayEquals(new byte[] {0x12, 0x34, 0x56, 0x78}, outBiuffer.toByteArray());
  }

  @Test
  public void testWriteFloat_BigEndian() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer);
    assertEquals(0, out.getCounter());
    out.writeFloat(9.2345f, JBBPByteOrder.BIG_ENDIAN);
    assertEquals(4, out.getCounter());
    out.flush();
    assertEquals(4, out.getCounter());
    assertArrayEquals(new byte[] {(byte) 65, (byte) 19, (byte) -64, (byte) -125},
        outBiuffer.toByteArray());
  }

  @Test
  public void testWriteInt_LittleEndian() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer);
    assertEquals(0, out.getCounter());
    out.writeInt(0x12345678, JBBPByteOrder.LITTLE_ENDIAN);
    assertEquals(4, out.getCounter());
    out.flush();
    assertEquals(4, out.getCounter());
    assertArrayEquals(new byte[] {0x78, 0x56, 0x34, 0x12}, outBiuffer.toByteArray());
  }

  @Test
  public void testWriteFloat_LittleEndian() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer);
    assertEquals(0, out.getCounter());
    out.writeFloat(9.2345f, JBBPByteOrder.LITTLE_ENDIAN);
    assertEquals(4, out.getCounter());
    out.flush();
    assertEquals(4, out.getCounter());
    assertArrayEquals(new byte[] {(byte) -125, (byte) -64, (byte) 19, (byte) 65},
        outBiuffer.toByteArray());
  }

  @Test
  public void testWriteStringArray_BigEndian() throws Exception {
    assertArrayEquals(new byte[] {(byte) 0xFF, 0, 0x03, 65, 66, 67},
        writeStrings(JBBPByteOrder.BIG_ENDIAN, null, "", "ABC"));
  }

  @Test
  public void testWriteStringArray_LittleEndian() throws Exception {
    assertArrayEquals(new byte[] {(byte) 0xFF, 0, 0x03, 65, 66, 67},
        writeStrings(JBBPByteOrder.LITTLE_ENDIAN, null, "", "ABC"));
  }

  @Test
  public void testWriteString_BigEndian_Null() throws Exception {
    assertArrayEquals(new byte[] {(byte) 0xFF}, writeString(JBBPByteOrder.BIG_ENDIAN, null));
  }

  @Test
  public void testWriteString_BigEndian_Empty() throws Exception {
    assertArrayEquals(new byte[] {0}, writeString(JBBPByteOrder.BIG_ENDIAN, ""));
  }

  @Test
  public void testWriteString_BigEndian_ShortString() throws Exception {
    assertArrayEquals(new byte[] {0x03, 65, 66, 67}, writeString(JBBPByteOrder.BIG_ENDIAN, "ABC"));
  }

  @Test
  public void testWriteString_LittleEndian_Null() throws Exception {
    assertArrayEquals(new byte[] {(byte) 0xFF}, writeString(JBBPByteOrder.LITTLE_ENDIAN, null));
  }

  @Test
  public void testWriteString_LittleEndian_Empty() throws Exception {
    assertArrayEquals(new byte[] {0}, writeString(JBBPByteOrder.LITTLE_ENDIAN, ""));
  }

  @Test
  public void testWriteString_LittleEndian_ShortString() throws Exception {
    assertArrayEquals(new byte[] {0x03, 65, 66, 67},
        writeString(JBBPByteOrder.LITTLE_ENDIAN, "ABC"));
  }

  @Test
  public void testWriteLong_BigEndian() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer);
    assertEquals(0, out.getCounter());
    out.writeLong(0x12345678AABBCCDDL, JBBPByteOrder.BIG_ENDIAN);
    assertEquals(8, out.getCounter());
    out.flush();
    assertEquals(8, out.getCounter());
    assertArrayEquals(
        new byte[] {0x12, 0x34, 0x56, 0x78, (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD},
        outBiuffer.toByteArray());
  }

  @Test
  public void testWriteDouble_BigEndian() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer);
    assertEquals(0, out.getCounter());
    out.writeDouble(1.12345678234324d, JBBPByteOrder.BIG_ENDIAN);
    assertEquals(8, out.getCounter());
    out.flush();
    assertEquals(8, out.getCounter());
    assertArrayEquals(
        new byte[] {(byte) 63, (byte) -15, (byte) -7, (byte) -83, (byte) -47, (byte) -86, (byte) 35,
            (byte) 64}, outBiuffer.toByteArray());
  }

  @Test
  public void testWriteLong_LittleEndian() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer);
    assertEquals(0, out.getCounter());
    out.writeLong(0x12345678AABBCCDDL, JBBPByteOrder.LITTLE_ENDIAN);
    assertEquals(8, out.getCounter());
    out.flush();
    assertEquals(8, out.getCounter());
    assertArrayEquals(
        new byte[] {(byte) 0xDD, (byte) 0xCC, (byte) 0XBB, (byte) 0xAA, 0x78, 0x56, 0x34, 0x12},
        outBiuffer.toByteArray());
  }

  @Test
  public void testWriteDouble_LittleEndian() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer);
    assertEquals(0, out.getCounter());
    out.writeDouble(-23.345213455d, JBBPByteOrder.LITTLE_ENDIAN);
    assertEquals(8, out.getCounter());
    out.flush();
    assertEquals(8, out.getCounter());
    assertArrayEquals(
        new byte[] {(byte) 58, (byte) 93, (byte) -77, (byte) -24, (byte) 95, (byte) 88, (byte) 55,
            (byte) -64}, outBiuffer.toByteArray());
  }

  @Test
  public void testWriteArrayPartly() throws Exception {
    final ByteArrayOutputStream buff = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(buff);
    assertEquals(0, out.getCounter());

    final Random rnd = new Random(12345);

    final byte[] array = new byte[1024];
    rnd.nextBytes(array);

    out.write(array, 100, 572);
    assertEquals(572, out.getCounter());

    out.close();

    assertArrayEquals(SpecialTestUtils.copyOfRange(array, 100, 672), buff.toByteArray());
  }

  @Test
  public void testWriteArrayPartlyWithOffset1Bit() throws Exception {
    final ByteArrayOutputStream buff = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(buff);

    assertEquals(0, out.getCounter());

    final byte[] ORIG_ARRAY = JBBPUtils
        .str2bin("10101001 01100100 10000101 01000010 10010100 10010010 00100100 10001010",
            JBBPBitOrder.MSB0);
    final byte[] ARRAY_1BIT_OFFSET =
        JBBPUtils.str2bin("1 10000101 01000010 10010100 10010010 00100100", JBBPBitOrder.MSB0);

    out.writeBits(1, JBBPBitNumber.BITS_1);
    assertEquals(0, out.getCounter());

    out.write(ORIG_ARRAY, 2, 5);
    assertEquals(5, out.getCounter());

    out.close();

    assertArrayEquals(ARRAY_1BIT_OFFSET, buff.toByteArray());
  }

  @Test
  public void testWriteWholeArray() throws Exception {
    final ByteArrayOutputStream buff = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(buff);

    final Random rnd = new Random(12345);

    final byte[] array = new byte[1024];
    rnd.nextBytes(array);

    out.write(array);
    assertEquals(1024, out.getCounter());

    out.close();


    assertArrayEquals(array, buff.toByteArray());
  }

  @Test
  public void testGetBufferedBitsNumber() throws Exception {
    final JBBPBitOutputStream out = new JBBPBitOutputStream(new ByteArrayOutputStream());
    out.writeBits(1, JBBPBitNumber.BITS_1);
    out.writeBits(3, JBBPBitNumber.BITS_2);
    assertEquals(3, out.getBufferedBitsNumber());
    assertEquals(0, out.getCounter());
  }

  @Test
  public void testGetBitBuffer() throws Exception {
    final JBBPBitOutputStream out = new JBBPBitOutputStream(new ByteArrayOutputStream());
    out.writeBits(1, JBBPBitNumber.BITS_1);
    out.writeBits(3, JBBPBitNumber.BITS_2);
    assertEquals(7, out.getBitBuffer());
    assertEquals(0, out.getCounter());
  }

  @Test
  public void testWriteBit_ErrorForZeroSize() throws Exception {
    assertThrows(IllegalArgumentException.class,
        () -> new JBBPBitOutputStream(new ByteArrayOutputStream())
            .writeBits(4, JBBPBitNumber.decode(0)));
  }

  @Test
  public void testWriteBit_ErrorForNegativeSize() throws Exception {
    assertThrows(IllegalArgumentException.class,
        () -> new JBBPBitOutputStream(new ByteArrayOutputStream())
            .writeBits(4, JBBPBitNumber.decode(-1)));
  }

  @Test
  public void testWriteBit_ErrorForTooBigSize() throws Exception {
    assertThrows(IllegalArgumentException.class,
        () -> new JBBPBitOutputStream(new ByteArrayOutputStream())
            .writeBits(4, JBBPBitNumber.decode(9)));
  }

  @Test
  public void testWriteWholeArrayWith1Bit() throws Exception {
    final ByteArrayOutputStream buff = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(buff);

    final byte[] ORIG_ARRAY = JBBPUtils
        .str2bin("10101001 01100100 10000101 01000010 10010100 10010010 00100100 10001010",
            JBBPBitOrder.MSB0);
    final byte[] ORIG_ARRAY_1BIT_OFFSET = JBBPUtils
        .str2bin("1 10101001 01100100 10000101 01000010 10010100 10010010 00100100 10001010",
            JBBPBitOrder.MSB0);

    out.writeBits(1, JBBPBitNumber.BITS_1);
    out.write(ORIG_ARRAY);
    assertEquals(8, out.getCounter());

    out.close();


    assertArrayEquals(ORIG_ARRAY_1BIT_OFFSET, buff.toByteArray());
  }

  @Test
  public void testWriteBit_BitByBit() throws IOException {
    final ByteArrayOutputStream buff = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(buff);

    out.writeBits(0, JBBPBitNumber.BITS_1);
    out.writeBits(0, JBBPBitNumber.BITS_1);
    out.writeBits(0, JBBPBitNumber.BITS_1);
    out.writeBits(0, JBBPBitNumber.BITS_1);
    out.writeBits(0, JBBPBitNumber.BITS_1);
    out.writeBits(0, JBBPBitNumber.BITS_1);
    out.writeBits(0, JBBPBitNumber.BITS_1);
    assertEquals(0, out.getCounter());

    out.writeBits(1, JBBPBitNumber.BITS_1);

    assertEquals(1, out.getCounter());

    out.close();
    assertArrayEquals(new byte[] {(byte) 0x80}, buff.toByteArray());
  }

  @Test
  public void testWriteBit_BitByBit_MSB0() throws IOException {
    final ByteArrayOutputStream buff = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(buff, JBBPBitOrder.MSB0);

    out.writeBits(0, JBBPBitNumber.BITS_1);
    out.writeBits(0, JBBPBitNumber.BITS_1);
    out.writeBits(0, JBBPBitNumber.BITS_1);
    out.writeBits(0, JBBPBitNumber.BITS_1);
    out.writeBits(0, JBBPBitNumber.BITS_1);
    out.writeBits(0, JBBPBitNumber.BITS_1);
    out.writeBits(0, JBBPBitNumber.BITS_1);
    assertEquals(0, out.getCounter());

    out.writeBits(1, JBBPBitNumber.BITS_1);
    assertEquals(1, out.getCounter());

    out.close();

    assertArrayEquals(new byte[] {(byte) 0x01}, buff.toByteArray());
  }

  @Test
  public void testWrite() throws Exception {
    final ByteArrayOutputStream buff = new ByteArrayOutputStream();

    final JBBPBitOutputStream out = new JBBPBitOutputStream(buff);

    for (int i = 0; i < 256; ) {
      out.write(i++);
      out.writeBits(i++, JBBPBitNumber.BITS_8);
    }
    assertEquals(256, out.getCounter());

    final byte[] written = buff.toByteArray();

    assertEquals(256, written.length);

    for (int i = 0; i < 256; i++) {
      assertEquals(i, written[i] & 0xFF, "Pos " + i);
    }
  }

}
