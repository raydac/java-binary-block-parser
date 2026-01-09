package com.igormaznitsa.jbbp.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.BitSet;
import java.util.Random;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class JBBPBitOrderTest {

  @ParameterizedTest
  @EnumSource(JBBPBitOrder.class)
  void testBitWriteRead(final JBBPBitOrder bitOrder) throws Exception {
    final int bits = 10_000_000;

    final Random rnd = new Random(1232345L);

    final BitSet bitSet = new BitSet(bits * 5);
    for (int i = 0; i < bits; i++) {
      bitSet.set(rnd.nextInt(bitSet.size()));
    }

    final byte[] array = bitSet.toByteArray();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    final JBBPBitOutputStream bitOutputStream = new JBBPBitOutputStream(outputStream, bitOrder);

    final ByteArrayOutputStream portions = new ByteArrayOutputStream();
    int dataIndex = 0;
    while (dataIndex < array.length) {
      int bitsLeft = 8;
      int acc = array[dataIndex++] & 0xFF;
      while (bitsLeft > 0) {
        final int portion = Math.min(bitsLeft, rnd.nextInt(8) + 1);
        bitsLeft -= portion;

        final JBBPBitNumber bitNumber = JBBPBitNumber.decode(portion);

        bitOutputStream.writeBits(acc, JBBPBitNumber.decode(portion));
        acc >>>= portion;
        portions.write(portion);
      }
    }
    bitOutputStream.close();
    outputStream.close();

    final byte[] portionsArray = portions.toByteArray();
    final byte[] dataArray = outputStream.toByteArray();

    final JBBPBitInputStream inputStream =
        new JBBPBitInputStream(new ByteArrayInputStream(dataArray), bitOrder);

    final ByteArrayOutputStream restoredDataStream = new ByteArrayOutputStream();

    for (int i = 0; i < portionsArray.length; ) {
      int readBits = 0;
      int acc = 0;
      while (readBits < 8) {
        final int portion = portionsArray[i++];
        final JBBPBitNumber bitNumber = JBBPBitNumber.decode(portion);
        final int data = inputStream.readBits(bitNumber);
        acc |= ((data & bitNumber.getMask()) << readBits);

        readBits += portion;
        if (readBits > 8) {
          throw new Error("unexpected");
        }
        if (readBits == 8) {
          restoredDataStream.write(acc);
        }
      }
    }
    restoredDataStream.close();

    final byte[] readData = restoredDataStream.toByteArray();

    assertEquals(array.length, readData.length);
    assertArrayEquals(array, readData);
  }

}