package com.igormaznitsa.jbbp.compiler.conversion;

import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.testaux.AbstractJBBPToJavaConverterTest;
import com.igormaznitsa.jbbp.utils.JBBPDslBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RandomAutoTest extends AbstractJBBPToJavaConverterTest {

  private final Random RND = new Random(12345);

  private static class StructLen {
    final int arrayLength;

    int bitLength = 0;

    StructLen() {
      this(1);
    }

    StructLen(final int arrayLength) {
      this.arrayLength = arrayLength;
    }

    void add(final int bitLength) {
      this.bitLength += bitLength;
    }

    int make() {
      return this.arrayLength * bitLength;
    }

  }

  static class Result {
    final String script;
    final int bitLength;
    final int fieldsNumber;
    final int structNumber;
    final int booleanDataItemCounter;
    final long typeFlags;

    Result(final String script, final int bitLength, final int fieldsNumber, final int structNumber, final int booleanDtaItemCounter, final long typeFlags) {
      this.script = script;
      this.bitLength = bitLength;
      this.fieldsNumber = fieldsNumber;
      this.structNumber = structNumber;
      this.booleanDataItemCounter = booleanDtaItemCounter;
      this.typeFlags = typeFlags;
    }
  }

  int makeArrayLengthNumber() {
    return RND.nextInt(16) + 1;
  }

  JBBPBitNumber makeRndBitNumber() {
    return JBBPBitNumber.decode(1 + RND.nextInt(8));
  }

  String generateComment() {
    final int length = 3 + RND.nextInt(24);
    final StringBuilder result = new StringBuilder();

    for (int i = 0; i < length; i++) {
      result.append((char) ('a' + RND.nextInt(26)));
    }

    return result.toString();
  }

  String makeRndName() {
    final StringBuilder result = new StringBuilder();
    for (int i = 0; i < 3; i++) {
      result.append((char) ('a' + RND.nextInt(26)));
    }
    for (int i = 0; i < 3; i++) {
      result.append((char) ('0' + RND.nextInt(9)));
    }
    for (int i = 0; i < 3; i++) {
      result.append((char) ('a' + RND.nextInt(26)));
    }
    return result.toString();
  }

  String genRandomString(final int length) {
    final StringBuilder builder = new StringBuilder(length);
    for(int i=0;i<length;i++){
      builder.append((char)(' '+this.RND.nextInt(100)));
    }
    return builder.toString();
  }

  Result generate(final int items, final boolean generateNames) {
    final JBBPDslBuilder builder = JBBPDslBuilder.Begin();

    final List<StructLen> counterStack = new ArrayList<>();
    counterStack.add(new StructLen());

    int structsTotal = 0;
    int fieldsTotal = 0;
    int booleanDataItems = 0;

    int activeStructCounter = 0;

    long typeFlags = 0;

    for (int i = 0; i < items; i++) {

      if (RND.nextInt(50) > 48) {
        builder.ByteOrder(JBBPByteOrder.LITTLE_ENDIAN);
      } else {
        builder.ByteOrder(JBBPByteOrder.BIG_ENDIAN);
      }

      if (activeStructCounter > 0 && RND.nextInt(100) > 90) {
        i--;
        activeStructCounter--;
        builder.CloseStruct();
        final StructLen len = counterStack.remove(0);
        counterStack.get(0).add(len.make());
      } else {
        final int rndType = RND.nextInt(27);
        typeFlags |= (1 << rndType);
        switch (rndType) {
          case 0: { // STRUCT
            builder.Struct(generateNames ? makeRndName() : null);
            counterStack.add(0, new StructLen());
            activeStructCounter++;
            structsTotal++;
          }
          break;
          case 1: { // STRUCT_ARRAY
            final int arrayLen = makeArrayLengthNumber();
            builder.StructArray(generateNames ? makeRndName() : null, String.valueOf(arrayLen));
            counterStack.add(0, new StructLen(arrayLen));
            activeStructCounter++;
            structsTotal++;
          }
          break;
          case 2: { // BIT
            final JBBPBitNumber bits = makeRndBitNumber();
            builder.Bits(generateNames ? makeRndName() : null, bits);
            counterStack.get(0).add(bits.getBitNumber());
            fieldsTotal++;
          }
          break;
          case 3: { // BIT_ARRAY
            final JBBPBitNumber bits = makeRndBitNumber();
            final int arrayLen = makeArrayLengthNumber();
            builder.BitArray(generateNames ? makeRndName() : null, bits, String.valueOf(arrayLen));
            counterStack.get(0).add(bits.getBitNumber() * arrayLen);
            fieldsTotal++;
          }
          break;
          case 4: { // SKIP
            final int arrayLen = makeArrayLengthNumber();
            final int bitlen;
            if (RND.nextBoolean()) {
              builder.Skip();
              bitlen = 8;
            } else {
              final int len = makeArrayLengthNumber();
              builder.Skip(String.valueOf(len));
              bitlen = len * 8;
            }
            counterStack.get(0).add(bitlen);
          }
          break;
          case 5: { // BOOL
            builder.Bool(generateNames ? makeRndName() : null);
            counterStack.get(0).add(8);
            fieldsTotal++;
            booleanDataItems ++;
          }
          break;
          case 6: { // BOOL_ARRAY
            final int arrayLen = makeArrayLengthNumber();
            builder.BoolArray(generateNames ? makeRndName() : null, String.valueOf(arrayLen));
            counterStack.get(0).add(8 * arrayLen);
            fieldsTotal++;
            booleanDataItems += arrayLen;
          }
          break;
          case 7: { // BYTE
            builder.Byte(generateNames ? makeRndName() : null);
            counterStack.get(0).add(8);
            fieldsTotal++;
          }
          break;
          case 8: { // BYTE_ARRAY
            final int arrayLen = makeArrayLengthNumber();
            builder.ByteArray(generateNames ? makeRndName() : null, String.valueOf(arrayLen));
            counterStack.get(0).add(arrayLen * 8);
            fieldsTotal++;
          }
          break;
          case 9: { // UBYTE
            builder.UByte(generateNames ? makeRndName() : null);
            counterStack.get(0).add(8);
            fieldsTotal++;
          }
          break;
          case 10: { // UBYTE_ARRAY
            final int arrayLen = makeArrayLengthNumber();
            builder.UByteArray(generateNames ? makeRndName() : null, String.valueOf(arrayLen));
            counterStack.get(0).add(8 * arrayLen);
            fieldsTotal++;
          }
          break;
          case 11: { // SHORT
            builder.Short(generateNames ? makeRndName() : null);
            counterStack.get(0).add(16);
            fieldsTotal++;
          }
          break;
          case 12: { // SHORT_ARRAY
            final int arrayLen = makeArrayLengthNumber();
            builder.ShortArray(generateNames ? makeRndName() : null, String.valueOf(arrayLen));
            counterStack.get(0).add(arrayLen * 16);
            fieldsTotal++;
          }
          break;
          case 13: { // USHORT
            builder.UShort(generateNames ? makeRndName() : null);
            counterStack.get(0).add(16);
            fieldsTotal++;
          }
          break;
          case 14: { // USHORT_ARRAY
            final int arrayLen = makeArrayLengthNumber();
            builder.UShortArray(generateNames ? makeRndName() : null, String.valueOf(arrayLen));
            counterStack.get(0).add(16 * arrayLen);
            fieldsTotal++;
          }
          break;
          case 15: { // INT
            builder.Int(generateNames ? makeRndName() : null);
            counterStack.get(0).add(32);
            fieldsTotal++;
          }
          break;
          case 16: { // INT_ARRAY
            final int arrayLen = makeArrayLengthNumber();
            builder.IntArray(generateNames ? makeRndName() : null, String.valueOf(arrayLen));
            counterStack.get(0).add(32 * arrayLen);
            fieldsTotal++;
          }
          break;
          case 17: { // Comment
            builder.Comment(generateComment());
          }
          break;
          case 18: { // LONG
            builder.Long(generateNames ? makeRndName() : null);
            counterStack.get(0).add(64);
            fieldsTotal++;
          }
          break;
          case 19: { // LONG_ARRAY
            final int arrayLen = makeArrayLengthNumber();
            builder.LongArray(generateNames ? makeRndName() : null, String.valueOf(arrayLen));
            counterStack.get(0).add(arrayLen * 64);
            fieldsTotal++;
          }
          break;
          case 20: { // FLOAT
            builder.Float(generateNames ? makeRndName() : null);
            counterStack.get(0).add(32);
            fieldsTotal++;
          }
          break;
          case 21: { // FLOAT_ARRAY
            final int arrayLen = makeArrayLengthNumber();
            builder.FloatArray(generateNames ? makeRndName() : null, String.valueOf(arrayLen));
            counterStack.get(0).add(arrayLen * 32);
            fieldsTotal++;
          }
          break;
          case 22: { // DOUBLE
            builder.Double(generateNames ? makeRndName() : null);
            counterStack.get(0).add(64);
            fieldsTotal++;
          }
          break;
          case 23: { // DOUBLE_ARRAY
            final int arrayLen = makeArrayLengthNumber();
            builder.DoubleArray(generateNames ? makeRndName() : null, String.valueOf(arrayLen));
            counterStack.get(0).add(arrayLen * 64);
            fieldsTotal++;
          }
          break;
          case 24: { // STRUCT END
            if (activeStructCounter > 0) {
              i--;
              activeStructCounter--;
              builder.CloseStruct();
              final StructLen len = counterStack.remove(0);
              counterStack.get(0).add(len.make());
            }
          }
          break;
          case 25: { // COMMENT
            builder.Comment(genRandomString(this.RND.nextInt(32)));
          }
          break;
          case 26: { // COMMENT NEW LINE
            builder.NewLineComment(genRandomString(this.RND.nextInt(32)));
          }
          break;
        }
      }
    }

    while (activeStructCounter > 0) {
      activeStructCounter--;
      builder.CloseStruct();
      final StructLen len = counterStack.remove(0);
      counterStack.get(0).add(len.make());
    }

    return new Result(builder.End(), counterStack.get(0).make(), fieldsTotal, structsTotal, booleanDataItems, typeFlags);
  }

  private byte[] makeRandomDataArray(final int bitLength) {
    final int bytelen = (bitLength / 8) + ((bitLength & 7) != 0 ? 1 : 0);
    assertTrue(bytelen > 0, "Bit length : " + bitLength);
    final byte[] result = new byte[bytelen];
    RND.nextBytes(result);
    return result;
  }

  @Test
  public void testCompileParseAndWriteArray() throws Exception {
    int testIndex = 1;

    long generatedFields = 0L;

    for (int i = 5; i < 500; i += 3) {
      Result result;
      do {
        result = generate(i, true);
      } while (result.bitLength > 10000000);

      generatedFields |= result.typeFlags;

      System.out.println(String.format("Test %d, data bit length = %d, fields = %d, sructs = %d", testIndex, result.bitLength, result.fieldsNumber, result.structNumber));

      final byte[] testData = makeRandomDataArray(result.bitLength);
      final Object clazzInstance = compileAndMakeInstance(result.script);
      callRead(clazzInstance, testData);
      assertEquals(testData.length, callWrite(clazzInstance).length, result.script);

      testIndex++;
    }

    assertEquals(0x7FFFFFFL, generatedFields, "All field types must be presented");
  }


}
