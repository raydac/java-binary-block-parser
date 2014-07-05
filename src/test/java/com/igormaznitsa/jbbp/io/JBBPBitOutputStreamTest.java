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
import com.igormaznitsa.jbbp.utils.SpecialTestUtils;
import java.io.*;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

public class JBBPBitOutputStreamTest {

  @Test
  public void testWrite9bit_MSB() throws Exception {
    final ByteArrayOutputStream buff = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(buff);

    out.writeBits(4, 0x9);
    out.writeBits(5, 0x1D);
    out.close();

    assertArrayEquals(new byte[]{(byte) 0xD9, 1}, buff.toByteArray());
  }

  @Test
  public void testGetOrder() throws Exception {
    assertEquals(JBBPBitOrder.MSB0, new JBBPBitOutputStream(null, JBBPBitOrder.MSB0).getOrder());
    assertEquals(JBBPBitOrder.LSB0, new JBBPBitOutputStream(null, JBBPBitOrder.LSB0).getOrder());
    assertEquals(JBBPBitOrder.LSB0, new JBBPBitOutputStream(null).getOrder());
  }

  @Test
  public void testWriteByte() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer);
    out.write(0x12);
    out.flush();
    assertArrayEquals(new byte[]{0x12}, outBiuffer.toByteArray());
  }

  @Test
  public void testWriteByte_MSB0() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer,JBBPBitOrder.MSB0);
    out.write(0x12);
    out.flush();
    assertArrayEquals(new byte[]{0x48}, outBiuffer.toByteArray());
  }

  @Test
  public void testWriteShort_BigEndian() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer);
    out.writeShort(0x1234, JBBPByteOrder.BIG_ENDIAN);
    out.flush();
    assertArrayEquals(new byte[]{0x12, 0x34}, outBiuffer.toByteArray());
  }

  @Test
  public void testWriteShort_BigEndian_MSB0() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer,JBBPBitOrder.MSB0);
    out.writeShort(0x1234, JBBPByteOrder.BIG_ENDIAN);
    out.flush();
    assertArrayEquals(new byte[]{0x48, 0x2C}, outBiuffer.toByteArray());
  }

  @Test
  public void testWriteShort_LittleEndian() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer);
    out.writeShort(0x1234, JBBPByteOrder.LITTLE_ENDIAN);
    out.flush();
    assertArrayEquals(new byte[]{0x34, 0x12}, outBiuffer.toByteArray());
  }

  @Test
  public void testWriteShort_LittleEndian_MSB0() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer,JBBPBitOrder.MSB0);
    out.writeShort(0x1234, JBBPByteOrder.LITTLE_ENDIAN);
    out.flush();
    assertArrayEquals(new byte[]{0x2C, 0x48}, outBiuffer.toByteArray());
  }

  @Test
  public void testWriteInt_BigEndian() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer);
    out.writeInt(0x12345678, JBBPByteOrder.BIG_ENDIAN);
    out.flush();
    assertArrayEquals(new byte[]{0x12, 0x34, 0x56, 0x78}, outBiuffer.toByteArray());
  }

  @Test
  public void testWriteInt_LittleEndian() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer);
    out.writeInt(0x12345678, JBBPByteOrder.LITTLE_ENDIAN);
    out.flush();
    assertArrayEquals(new byte[]{0x78, 0x56, 0x34, 0x12}, outBiuffer.toByteArray());
  }

  @Test
  public void testWriteLong_BigEndian() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer);
    out.writeLong(0x12345678AABBCCDDL, JBBPByteOrder.BIG_ENDIAN);
    out.flush();
    assertArrayEquals(new byte[]{0x12, 0x34, 0x56, 0x78, (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD}, outBiuffer.toByteArray());
  }

  @Test
  public void testWriteLong_LittleEndian() throws Exception {
    final ByteArrayOutputStream outBiuffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(outBiuffer);
    out.writeLong(0x12345678AABBCCDDL, JBBPByteOrder.LITTLE_ENDIAN);
    out.flush();
    assertArrayEquals(new byte[]{(byte) 0xDD, (byte) 0xCC, (byte) 0XBB, (byte) 0xAA, 0x78, 0x56, 0x34, 0x12}, outBiuffer.toByteArray());
  }

  @Test
  public void testWriteArrayPartly() throws Exception {
    final ByteArrayOutputStream buff = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(buff);

    final Random rnd = new Random(12345);

    final byte[] array = new byte[1024];
    rnd.nextBytes(array);

    out.write(array, 100, 572);
    out.close();

    assertArrayEquals(SpecialTestUtils.copyOfRange(array, 100, 672), buff.toByteArray());
  }

  @Test
  public void testWriteArrayPartlyWithOffset1Bit() throws Exception {
    final ByteArrayOutputStream buff = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(buff);

    final byte[] ORIG_ARRAY = JBBPUtils.str2bin("10101001 01100100 10000101 01000010 10010100 10010010 00100100 10001010", JBBPBitOrder.MSB0);
    final byte[] ARRAY_1BIT_OFFSET = JBBPUtils.str2bin("1 10000101 01000010 10010100 10010010 00100100", JBBPBitOrder.MSB0);

    out.writeBits(1, 1);
    out.write(ORIG_ARRAY, 2, 5);
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
    out.close();

    assertArrayEquals(array, buff.toByteArray());
  }

  @Test
  public void testGetBufferedBitsNumber() throws Exception {
    final JBBPBitOutputStream out = new JBBPBitOutputStream(new ByteArrayOutputStream());
    out.writeBits(1, 1);
    out.writeBits(2, 3);
    assertEquals(3,out.getBufferedBitsNumber());
  }

  @Test
  public void testGetBitBuffer() throws Exception {
    final JBBPBitOutputStream out = new JBBPBitOutputStream(new ByteArrayOutputStream());
    out.writeBits(1, 1);
    out.writeBits(2, 3);
    assertEquals(7,out.getBitBuffer());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWriteBit_ErrorForZeroSize() throws Exception {
    new JBBPBitOutputStream(new ByteArrayOutputStream()).writeBits(0, 4);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testWriteBit_ErrorForNegativeSize() throws Exception {
    new JBBPBitOutputStream(new ByteArrayOutputStream()).writeBits(-1, 4);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testWriteBit_ErrorForTooBigSize() throws Exception {
    new JBBPBitOutputStream(new ByteArrayOutputStream()).writeBits(9, 4);
  }
  
  @Test
  public void testWriteWholeArrayWith1Bit() throws Exception {
    final ByteArrayOutputStream buff = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(buff);

    final byte[] ORIG_ARRAY = JBBPUtils.str2bin("10101001 01100100 10000101 01000010 10010100 10010010 00100100 10001010", JBBPBitOrder.MSB0);
    final byte[] ORIG_ARRAY_1BIT_OFFSET = JBBPUtils.str2bin("1 10101001 01100100 10000101 01000010 10010100 10010010 00100100 10001010", JBBPBitOrder.MSB0);

    out.writeBits(1, 1);
    out.write(ORIG_ARRAY);
    out.close();

    assertArrayEquals(ORIG_ARRAY_1BIT_OFFSET, buff.toByteArray());
  }

  @Test
  public void testWriteBit_BitByBit() throws IOException {
    final ByteArrayOutputStream buff = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(buff);

    out.writeBits(1, 0);
    out.writeBits(1, 0);
    out.writeBits(1, 0);
    out.writeBits(1, 0);
    out.writeBits(1, 0);
    out.writeBits(1, 0);
    out.writeBits(1, 0);
    out.writeBits(1, 1);

    out.close();

    assertArrayEquals(new byte[]{(byte) 0x80}, buff.toByteArray());
  }

  @Test
  public void testWriteBit_BitByBit_MSB0() throws IOException {
    final ByteArrayOutputStream buff = new ByteArrayOutputStream();
    final JBBPBitOutputStream out = new JBBPBitOutputStream(buff,JBBPBitOrder.MSB0);

    out.writeBits(1, 0);
    out.writeBits(1, 0);
    out.writeBits(1, 0);
    out.writeBits(1, 0);
    out.writeBits(1, 0);
    out.writeBits(1, 0);
    out.writeBits(1, 0);
    out.writeBits(1, 1);

    out.close();

    assertArrayEquals(new byte[]{(byte) 0x01}, buff.toByteArray());
  }

  @Test
  public void testWrite() throws Exception {
    final ByteArrayOutputStream buff = new ByteArrayOutputStream();

    final JBBPBitOutputStream out = new JBBPBitOutputStream(buff);

    for (int i = 0; i < 256;) {
      out.write(i++);
      out.writeBits(8, i++);
    }

    assertEquals(256, out.getCounter());

    final byte[] written = buff.toByteArray();

    assertEquals(256, written.length);

    for (int i = 0; i < 256; i++) {
      assertEquals("Pos " + i, i, written[i] & 0xFF);
    }
  }

}
