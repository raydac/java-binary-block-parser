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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class BitIOCommonTest {

  private final Random rnd = new Random(1234);
  private final int[] BYTE_MASK = new int[] {0, 1, 3, 7, 15, 31, 63, 127, 255};

  @Test
  public void testWriteReadString() throws Exception {
    final StringBuilder buffer = new StringBuilder("ФgЯ");
    final Random rnd = new Random(112233);

    for (int i = 0; i < 18231; i++) {
      final ByteArrayOutputStream buffBig = new ByteArrayOutputStream(16384);
      final JBBPBitOutputStream outBig = new JBBPBitOutputStream(buffBig);
      final ByteArrayOutputStream buffLittl = new ByteArrayOutputStream(16384);
      final JBBPBitOutputStream outLittl = new JBBPBitOutputStream(buffLittl);

      final String text = buffer.toString();

      outBig.writeString(text, JBBPByteOrder.BIG_ENDIAN);
      outLittl.writeString(text, JBBPByteOrder.LITTLE_ENDIAN);

      outBig.close();
      outLittl.close();

      final byte[] writtenBig = buffBig.toByteArray();
      final byte[] writtenLittle = buffLittl.toByteArray();

      final String restoredBig = new JBBPBitInputStream(new ByteArrayInputStream(writtenBig))
          .readString(JBBPByteOrder.BIG_ENDIAN);
      final String restoredLittle = new JBBPBitInputStream(new ByteArrayInputStream(writtenLittle))
          .readString(JBBPByteOrder.LITTLE_ENDIAN);


      assertEquals(text, restoredBig, "Iteration#" + i);
      assertEquals(text, restoredLittle, "Iteration#" + i);

      buffer.append((char) ('A' + rnd.nextInt(11)));
      buffer.append((char) ('Б' + rnd.nextInt(22)));
    }
  }

  @Test
  public void testWriteRead() throws Exception {
    final int LEN = 10000;

    final byte[] array = new byte[LEN];
    final int[] len = new int[LEN];

    rnd.nextBytes(array);

    for (int i = 0; i < LEN; i++) {
      final int l = rnd.nextInt(8) + 1;
      len[i] = l;
      array[i] = (byte) (array[i] & BYTE_MASK[l]);
    }

    final ByteArrayOutputStream buff = new ByteArrayOutputStream();

    final JBBPBitOutputStream out = new JBBPBitOutputStream(buff);

    int writtenBits = 0;
    for (int i = 0; i < LEN; i++) {
      writtenBits += len[i];
      out.writeBits(array[i], JBBPBitNumber.decode(len[i]));
    }

    out.close();

    final JBBPBitInputStream in =
        new JBBPBitInputStream(new ByteArrayInputStream(buff.toByteArray()));
    for (int i = 0; i < LEN; i++) {
      assertEquals(array[i] & 0xFF, in.readBits(JBBPBitNumber.decode(len[i])), "Index i=" + i);
    }

    if (writtenBits % 8 == 0) {
      assertEquals(-1, in.read());
    } else {
      assertEquals(0, in.read());
      assertEquals(-1, in.read());
    }
  }

  @Test
  public void testWriteRead_MSB0() throws Exception {
    final int LEN = 10000;

    final byte[] array = new byte[LEN];
    final int[] len = new int[LEN];

    rnd.nextBytes(array);
    for (int i = 0; i < LEN; i++) {
      final int l = rnd.nextInt(8) + 1;
      len[i] = l;
      array[i] = (byte) (array[i] & BYTE_MASK[l]);
    }

    final ByteArrayOutputStream buff = new ByteArrayOutputStream();

    final JBBPBitOutputStream out = new JBBPBitOutputStream(buff, JBBPBitOrder.MSB0);

    int writtenBits = 0;
    for (int i = 0; i < LEN; i++) {
      writtenBits += len[i];
      out.writeBits(array[i], JBBPBitNumber.decode(len[i]));
    }

    out.close();

    final JBBPBitInputStream in =
        new JBBPBitInputStream(new ByteArrayInputStream(buff.toByteArray()), JBBPBitOrder.MSB0);
    for (int i = 0; i < LEN; i++) {
      assertEquals(array[i] & 0xFF, in.readBits(JBBPBitNumber.decode(len[i])), "Index i=" + i);
    }

    if (writtenBits % 8 == 0) {
      assertEquals(-1, in.read());
    } else {
      assertEquals(0, in.read());
      assertEquals(-1, in.read());
    }
  }

  @Test
  public void testWriteRead_NotFullByteAsLSB0AndReadAsMSB0() throws Exception {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    final JBBPBitOutputStream out = new JBBPBitOutputStream(buffer, JBBPBitOrder.LSB0);
    out.writeBits(1, JBBPBitNumber.BITS_1);
    out.writeBits(0, JBBPBitNumber.BITS_1);
    out.writeBits(1, JBBPBitNumber.BITS_1);
    out.writeBits(1, JBBPBitNumber.BITS_1);
    out.writeBits(0, JBBPBitNumber.BITS_1);
    out.flush();

    final JBBPBitInputStream in =
        new JBBPBitInputStream(new ByteArrayInputStream(buffer.toByteArray()), JBBPBitOrder.MSB0);
    assertEquals(0, in.readBits(JBBPBitNumber.BITS_1));
    assertEquals(0, in.readBits(JBBPBitNumber.BITS_1));
    assertEquals(0, in.readBits(JBBPBitNumber.BITS_1));
    assertEquals(0, in.readBits(JBBPBitNumber.BITS_1));
    assertEquals(1, in.readBits(JBBPBitNumber.BITS_1));
    assertEquals(1, in.readBits(JBBPBitNumber.BITS_1));
    assertEquals(0, in.readBits(JBBPBitNumber.BITS_1));
    assertEquals(1, in.readBits(JBBPBitNumber.BITS_1));

    assertEquals(-1, in.readBits(JBBPBitNumber.BITS_1));

  }

  @Test
  public void testWriteRead_LSB0() throws Exception {
    final int LEN = 10000;

    final byte[] array = new byte[LEN];
    final int[] len = new int[LEN];

    rnd.nextBytes(array);
    for (int i = 0; i < LEN; i++) {
      final int l = rnd.nextInt(8) + 1;
      len[i] = l;
      array[i] = (byte) (array[i] & BYTE_MASK[l]);
    }

    final ByteArrayOutputStream buff = new ByteArrayOutputStream();

    final JBBPBitOutputStream out = new JBBPBitOutputStream(buff, JBBPBitOrder.LSB0);

    int writtenBits = 0;

    for (int i = 0; i < LEN; i++) {
      writtenBits += len[i];
      out.writeBits(array[i], JBBPBitNumber.decode(len[i]));
    }

    out.close();

    final JBBPBitInputStream in =
        new JBBPBitInputStream(new ByteArrayInputStream(buff.toByteArray()), JBBPBitOrder.LSB0);
    for (int i = 0; i < LEN; i++) {
      assertEquals(array[i] & 0xFF, in.readBits(JBBPBitNumber.decode(len[i])), "Index i=" + i);
    }

    if (writtenBits % 8 == 0) {
      assertEquals(-1, in.read());
    } else {
      assertEquals(0, in.read());
      assertEquals(-1, in.read());
    }
  }

}
