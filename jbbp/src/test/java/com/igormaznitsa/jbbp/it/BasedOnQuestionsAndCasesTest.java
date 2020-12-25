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

package com.igormaznitsa.jbbp.it;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


import com.igormaznitsa.jbbp.JBBPCustomFieldTypeProcessor;
import com.igormaznitsa.jbbp.JBBPExternalValueProvider;
import com.igormaznitsa.jbbp.JBBPNamedNumericFieldMap;
import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.compiler.JBBPCompiledBlock;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPFieldTypeParameterContainer;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.io.JBBPCustomFieldWriter;
import com.igormaznitsa.jbbp.io.JBBPOut;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import com.igormaznitsa.jbbp.mapper.JBBPMapperCustomFieldProcessor;
import com.igormaznitsa.jbbp.model.JBBPAbstractField;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayByte;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayLong;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayString;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayStruct;
import com.igormaznitsa.jbbp.model.JBBPFieldInt;
import com.igormaznitsa.jbbp.model.JBBPFieldLong;
import com.igormaznitsa.jbbp.model.JBBPFieldString;
import com.igormaznitsa.jbbp.model.JBBPFieldStruct;
import com.igormaznitsa.jbbp.model.JBBPNumericField;
import com.igormaznitsa.jbbp.utils.JBBPDslBuilder;
import com.igormaznitsa.jbbp.utils.JBBPTextWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/**
 * Tests based on questions and cases.
 */
public class BasedOnQuestionsAndCasesTest extends AbstractParserIntegrationTest {

  /**
   * Case 13-aug-2015
   * <p>
   * 3DF8 = 0011 1101 1111 1000 where data are stored from left to right :
   * 6 bits : 0011 11 for year;
   * 4 bits : 01 11 for month
   * 5 bits : 11 100 for day,
   *
   * @throws Exception for any error
   */
  @Test
  public void testParseDayMonthYearFromBytePairInMSB0AndPackThemBack() throws Exception {
    class YearMonthDay {
      @Bin(type = BinType.BIT, bitNumber = JBBPBitNumber.BITS_6, order = 1, bitOrder = JBBPBitOrder.MSB0)
      byte year;
      @Bin(type = BinType.BIT, bitNumber = JBBPBitNumber.BITS_4, order = 2, bitOrder = JBBPBitOrder.MSB0)
      byte month;
      @Bin(type = BinType.BIT, bitNumber = JBBPBitNumber.BITS_5, order = 3, bitOrder = JBBPBitOrder.MSB0)
      byte day;
    }
    final YearMonthDay parsed =
        JBBPParser.prepare("bit:6 year; bit:4 month;  bit:5 day;", JBBPBitOrder.MSB0)
            .parse(new byte[] {(byte) 0x3D, (byte) 0xF8}).mapTo(new YearMonthDay());

    assertEquals(0x0F, parsed.year);
    assertEquals(0x07, parsed.month);
    assertEquals(0x1C, parsed.day & 0xFF);

    assertArrayEquals(new byte[] {(byte) 0x3D, (byte) 0xF8},
        JBBPOut.BeginBin(JBBPBitOrder.MSB0).Bin(parsed).End().toByteArray());
  }

  /**
   * Case 08-feb-2016
   * <p>
   * Incoming data: 0x024281
   * Timestamp format : <a href="http://www.etsi.org/deliver/etsi_en/300300_300399/30039202/02.03.02_60/en_30039202v020302p.pdf">Terrestrial Trunked Radio</a>
   *
   * @throws Exception for any error
   */
  @Test
  public void testParseTimeStampFromTETRASavedInMSB0() throws Exception {
    final byte[] TEST_DATA = new byte[] {0x2, 0x42, (byte) 0x81};

    class TetraTimestamp {
      @Bin(type = BinType.BIT, bitNumber = JBBPBitNumber.BITS_2, order = 1, bitOrder = JBBPBitOrder.MSB0)
      byte timezone;
      @Bin(type = BinType.BIT, bitNumber = JBBPBitNumber.BITS_2, order = 2, bitOrder = JBBPBitOrder.MSB0)
      byte reserved;
      @Bin(type = BinType.BIT, bitNumber = JBBPBitNumber.BITS_4, order = 3, bitOrder = JBBPBitOrder.MSB0)
      byte month;
      @Bin(type = BinType.BIT, bitNumber = JBBPBitNumber.BITS_5, order = 4, bitOrder = JBBPBitOrder.MSB0)
      byte day;
      @Bin(type = BinType.BIT, bitNumber = JBBPBitNumber.BITS_5, order = 5, bitOrder = JBBPBitOrder.MSB0)
      byte hour;
      @Bin(type = BinType.BIT, bitNumber = JBBPBitNumber.BITS_6, order = 6, bitOrder = JBBPBitOrder.MSB0)
      byte minute;
    }


    TetraTimestamp parsed = JBBPParser.prepare(
        "bit:2 timezone; bit:2 reserved; bit:4 month; bit:5 day; bit:5 hour; bit:6 minute;",
        JBBPBitOrder.MSB0).parse(TEST_DATA).mapTo(new TetraTimestamp());

    assertEquals(2, parsed.month);
    assertEquals(8, parsed.day);
    assertEquals(10, parsed.hour);
    assertEquals(1, parsed.minute);

    assertArrayEquals(TEST_DATA,
        JBBPOut.BeginBin(JBBPBitOrder.MSB0).Bin(parsed).End().toByteArray());
  }

  /**
   * Case 28-jul-2016
   * Simultaneous usage of expression evaluator from multiple threads.
   *
   * <a href="https://github.com/raydac/java-binary-block-parser/issues/10">Issue #10, assertArrayLength throws exception in multi-thread</a>
   *
   * @throws Exception for any error
   */
  @Test
  public void testMutlithredUsageOfParser() throws Exception {
    final JBBPParser parserIP = JBBPParser.prepare("skip:14; // skip bytes till the frame\n"
        + "bit:4 InternetHeaderLength;"
        + "bit:4 Version;"
        + "bit:2 ECN;"
        + "bit:6 DSCP;"
        + "ushort TotalPacketLength;"
        + "ushort Identification;"
        + "bit:8 IPFlagsAndFragmentOffset_low;"
        + "bit:5 IPFlagsAndFragmentOffset_high;"
        + "bit:1 MoreFragment;"
        + "bit:1 DonotFragment;"
        + "bit:1 ReservedBit;"
        + "ubyte TTL;"
        + "ubyte Protocol;"
        + "ushort HeaderChecksum;"
        + "int SourceAddress;"
        + "int DestinationAddress;"
        + "byte [(InternetHeaderLength-5)*4] Options;");

    final JBBPParser parserTCP = JBBPParser.prepare("skip:34; // skip bytes till the frame\n"
        + "ushort SourcePort;"
        + "ushort DestinationPort;"
        + "int SequenceNumber;"
        + "int AcknowledgementNumber;"
        + "bit:1 NONCE;"
        + "bit:3 RESERVED;"
        + "bit:4 HLEN;"
        + "bit:1 FIN;"
        + "bit:1 SYN;"
        + "bit:1 RST;"
        + "bit:1 PSH;"
        + "bit:1 ACK;"
        + "bit:1 URG;"
        + "bit:1 ECNECHO;"
        + "bit:1 CWR;"
        + "ushort WindowSize;"
        + "ushort TCPCheckSum;"
        + "ushort UrgentPointer;"
        + "byte [HLEN*4-20] Option;"
        + "byte [_] Data;");

    byte[] testArray;
    try (InputStream inStream = getResourceAsInputStream("tcppacket.bin")) {
      testArray = new JBBPBitInputStream(inStream).readByteArray(-1);
      assertEquals(173, testArray.length);
    }

    final byte[] theData = testArray;

    final AtomicInteger errorCounter = new AtomicInteger();
    final AtomicLong parsingCounter = new AtomicLong();

    final int ITERATIONS = 1000;

    final Runnable test = () -> {
      for (int i = 0; i < ITERATIONS; i++) {
        try {
          Thread.sleep(System.nanoTime() & 0xF);
          final byte[] ippacket =
              parserTCP.parse(theData).findFieldForNameAndType("Data", JBBPFieldArrayByte.class)
                  .getArray();
          assertEquals(119, ippacket.length);
          final byte[] optionsip =
              parserIP.parse(ippacket).findFieldForNameAndType("Options", JBBPFieldArrayByte.class)
                  .getArray();
          assertEquals(4, optionsip.length);
          parsingCounter.incrementAndGet();
        } catch (Exception ex) {
          ex.printStackTrace();
          errorCounter.incrementAndGet();
        }
      }
    };

    final Thread[] threads = new Thread[15];

    for (int i = 0; i < threads.length; i++) {
      final Thread testThread = new Thread(test, "jbbp_test_thread" + i);
      testThread.setDaemon(true);
      threads[i] = testThread;
      testThread.start();
    }

    for (final Thread t : threads) {
      t.join();
    }

    assertEquals(threads.length * ITERATIONS, parsingCounter.get());
    assertEquals(0, errorCounter.get());
  }

  @Test
  public void testParseBitsThroughDslBasedScriptAndMapping() throws Exception {
    class Bits {
      @Bin(name = "a", type = BinType.BIT_ARRAY, bitNumber = JBBPBitNumber.BITS_1, arraySizeExpr = "_")
      byte[] bit;
    }

    JBBPParser parser =
        JBBPParser.prepare(JBBPDslBuilder.Begin().AnnotatedClassFields(Bits.class).End());

    Bits parsed = parser.parse(new byte[] {73}).mapTo(new Bits());

    System.out.println(JBBPTextWriter.makeStrWriter().Bin(parsed).Close().toString());

    assertArrayEquals(new byte[] {1, 0, 0, 1, 0, 0, 1, 0}, parsed.bit);
  }

  /**
   * Case 03-feb-2020
   * <a href="https://github.com/raydac/java-binary-block-parser/issues/26">Issue #26, Bug in parsing of stringj written in MSB0</a>
   *
   * @throws Exception for any error
   */
  @Test
  public void testStringMsb0() throws Exception {
    JBBPOut joparam =
        JBBPOut.BeginBin(JBBPByteOrder.BIG_ENDIAN, JBBPBitOrder.MSB0).String("zzzz").Int(12345);
    final byte[] array = joparam.End().toByteArray();
    assertArrayEquals(new byte[] {32, 94, 94, 94, 94, 0, 0, 0x0C, (byte) 0x9C}, array);
    final JBBPFieldStruct bitflds =
        JBBPParser.prepare("stringj fin; int i;", JBBPBitOrder.MSB0).parse(array);
    assertEquals("zzzz",
        bitflds.findFieldForNameAndType("fin", JBBPFieldString.class).getAsString());
    assertEquals(12345, bitflds.findFieldForNameAndType("i", JBBPFieldInt.class).getAsInt());
  }

  /**
   * Case 18-feb-2020, #27 Strings in other codecs
   * Example how to implement custom ASCII string format
   *
   * @throws Exception for any error
   */
  @Test
  public void testAscIIPascalString() throws Exception {
    final class AscIIPascalString implements JBBPCustomFieldTypeProcessor {
      private final String[] TYPES = new String[] {"asciistr"};

      @Override
      public String[] getCustomFieldTypes() {
        return TYPES;
      }

      @Override
      public boolean isAllowed(
          final JBBPFieldTypeParameterContainer fieldType,
          final String fieldName,
          final int extraData,
          final boolean isArray
      ) {
        return extraData == 0;
      }

      @Override
      public JBBPAbstractField readCustomFieldType(
          final JBBPBitInputStream in,
          final JBBPBitOrder bitOrder,
          final int parserFlags,
          final JBBPFieldTypeParameterContainer customTypeFieldInfo,
          final JBBPNamedFieldInfo fieldName,
          final int extraData,
          final boolean readWholeStream,
          final int arrayLength
      ) throws IOException {
        if (arrayLength < 0) {
          return new JBBPFieldString(fieldName, readPascalAscIIString(in));
        } else {
          final String[] loadedStrings;
          if (readWholeStream) {
            final List<String> strings = new ArrayList<>();
            while (in.hasAvailableData()) {
              strings.add(readPascalAscIIString(in));
            }
            loadedStrings = strings.toArray(new String[0]);
          } else {
            loadedStrings = new String[arrayLength];
            for (int i = 0; i < arrayLength; i++) {
              loadedStrings[i] = readPascalAscIIString(in);
            }
          }
          return new JBBPFieldArrayString(fieldName, loadedStrings);
        }
      }

      private String readPascalAscIIString(final JBBPBitInputStream in) throws IOException {
        final byte[] charArray = in.readByteArray(in.readByte());
        return new String(charArray, StandardCharsets.US_ASCII);
      }
    }

    final JBBPParser parserSingle =
        JBBPParser.prepare("asciistr str1; asciistr str2;", new AscIIPascalString());
    final JBBPFieldStruct parsedSingle = parserSingle.parse(new byte[] {5, 65, 66, 67, 68, 69, 0});
    assertEquals("ABCDE",
        parsedSingle.findFieldForNameAndType("str1", JBBPFieldString.class).getAsString());
    assertEquals("",
        parsedSingle.findFieldForNameAndType("str2", JBBPFieldString.class).getAsString());

    final JBBPParser parserArray =
        JBBPParser.prepare("asciistr [2] str1; asciistr [_] str2;", new AscIIPascalString());
    final JBBPFieldStruct parsedArrays =
        parserArray.parse(new byte[] {2, 65, 66, 1, 67, 3, 68, 69, 70, 2, 71, 72, 1, 73});
    assertArrayEquals(new String[] {"AB", "C"},
        parsedArrays.findFieldForNameAndType("str1", JBBPFieldArrayString.class).getArray());
    assertArrayEquals(new String[] {"DEF", "GH", "I"},
        parsedArrays.findFieldForNameAndType("str2", JBBPFieldArrayString.class).getArray());
  }

  /**
   * Case 10-aug-2017
   * NullPointer exception when referencing a JBBPCustomFieldTypeProcessor parsed field.
   *
   * <a href="https://github.com/raydac/java-binary-block-parser/issues/16">Issue #16, NullPointer exception when referencing a JBBPCustomFieldTypeProcessor parsed field</a>
   *
   * @throws Exception for any error
   */
  @Test
  public void testNPEWhenReferencingCustomFieldProcessorAsArrayLength() throws Exception {
    final class Uint32 implements JBBPCustomFieldTypeProcessor {

      private final String[] TYPES = new String[] {"uint32"};

      private long uint32_read(final JBBPBitInputStream in, final JBBPByteOrder byteOrder,
                               final JBBPBitOrder bitOrder) throws IOException {
        final int signedInt = in.readInt(byteOrder);
        return signedInt & 0xffffffffL;
      }

      @Override
      public String[] getCustomFieldTypes() {
        return TYPES;
      }

      @Override
      public boolean isAllowed(final JBBPFieldTypeParameterContainer fieldType,
                               final String fieldName, final int extraData, final boolean isArray) {
        return extraData == 0;
      }

      private long[] convertLongs(List<Long> longs) {
        long[] ret = new long[longs.size()];
        Iterator<Long> iterator = longs.iterator();
        for (int i = 0; i < ret.length; i++) {
          ret[i] = iterator.next();
        }
        return ret;
      }

      @Override
      public JBBPAbstractField readCustomFieldType(final JBBPBitInputStream in,
                                                   final JBBPBitOrder bitOrder,
                                                   final int parserFlags,
                                                   final JBBPFieldTypeParameterContainer customTypeFieldInfo,
                                                   final JBBPNamedFieldInfo fieldName,
                                                   final int extraData,
                                                   final boolean readWholeStream,
                                                   final int arrayLength) throws IOException {
        if (arrayLength < 0) {
          final long uint32_val = uint32_read(in, customTypeFieldInfo.getByteOrder(), bitOrder);
          return new JBBPFieldLong(fieldName, uint32_val);
        } else {
          if (readWholeStream) {
            ArrayList<Long> laLaLaLaLong = new ArrayList<>();
            try {
              while (!Thread.currentThread().isInterrupted()) {
                laLaLaLaLong.add(uint32_read(in, customTypeFieldInfo.getByteOrder(), bitOrder));
              }
            } catch (EOFException e) {

            }

            long[] longs = convertLongs(laLaLaLaLong);

            return new JBBPFieldArrayLong(fieldName, longs);

          } else {
            final long[] array = new long[arrayLength];
            for (int i = 0; i < arrayLength; i++) {
              array[i] = uint32_read(in, customTypeFieldInfo.getByteOrder(), bitOrder);
            }
            return new JBBPFieldArrayLong(fieldName, array);
          }
        }
      }
    }

    final JBBPParser sasParser = JBBPParser.prepare(
        ">uint32 keycount;" +
            "key [keycount] {" +
            "byte[1] contentId; " +
            "byte[1] keyData; " +
            "}"
        , new Uint32()
    );

    JBBPFieldStruct result = sasParser.parse(new byte[] {0, 0, 0, 0, 0x01, (byte) 0xFC, 0x05});
    assertEquals(0, ((JBBPNumericField) result.findFieldForName("keycount")).getAsInt());

    result = sasParser.parse(new byte[] {0, 0, 0, 2, 1, 2, 3, 4});
    assertEquals(2, ((JBBPNumericField) result.findFieldForName("keycount")).getAsInt());
  }

  /**
   * Case 09-jun-2020
   * Example how to use external variable value provider.
   *
   * @throws Exception for any error
   */
  @Test
  public void testByteArrayWhichLengthProvidedExternally() throws Exception {
    class BKlazz {
      @Bin(order = 1, type = BinType.BYTE_ARRAY)
      byte[] a;
      @Bin(order = 2, type = BinType.BYTE_ARRAY)
      byte[] b;
      @Bin(order = 3, type = BinType.BYTE_ARRAY)
      byte[] c;
    }

    JBBPParser parser = JBBPParser.prepare("byte [$alen] a; byte [$blen] b; byte [$clen] c;");

    BKlazz parsed = parser.parse(new byte[] {1, 2, 3}, null, new JBBPExternalValueProvider() {
      @Override
      public int provideArraySize(String fieldName,
                                  JBBPNamedNumericFieldMap numericFieldMap,
                                  JBBPCompiledBlock compiledBlock) {
        if ("alen".equals(fieldName)) {
          return 0;
        } else if ("blen".equals(fieldName)) {
          return 3;
        } else if ("clen".equals(fieldName)) {
          return 0;
        } else {
          throw new IllegalArgumentException("Unknown name: " + fieldName);
        }

      }
    }).mapTo(new BKlazz());

    assertArrayEquals(new byte[0], parsed.a);
    assertArrayEquals(new byte[] {1, 2, 3}, parsed.b);
    assertArrayEquals(new byte[0], parsed.c);
  }

  /**
   * Case 09-jun-2020
   * Example how to write custom field type read-write-mapping processor for nullable byte array.
   *
   * @throws Exception for any error
   */
  @Test
  public void testNullableByteArrayField() throws Exception {
    class NullableByteArrayProcessor
        implements JBBPCustomFieldWriter, JBBPMapperCustomFieldProcessor,
        JBBPCustomFieldTypeProcessor {

      private final String TYPE = "nullableByteArray";
      private final String[] CUSTOM_TYPE = new String[] {TYPE.toLowerCase(Locale.ENGLISH)};

      @Override
      public String[] getCustomFieldTypes() {
        return CUSTOM_TYPE;
      }

      @Override
      public boolean isAllowed(JBBPFieldTypeParameterContainer fieldType, String fieldName,
                               int extraData, boolean isArray) {
        return !isArray;
      }

      private byte[] readFromStream(JBBPByteOrder byteOrder, JBBPBitInputStream in)
          throws IOException {
        final int len = in.readInt(byteOrder);
        return len < 0 ? null : in.readByteArray(len);
      }

      @Override
      public JBBPAbstractField readCustomFieldType(JBBPBitInputStream in, JBBPBitOrder bitOrder,
                                                   int parserFlags,
                                                   JBBPFieldTypeParameterContainer customTypeFieldInfo,
                                                   JBBPNamedFieldInfo fieldName, int extraData,
                                                   boolean readWholeStream, int arrayLength)
          throws IOException {
        if (arrayLength < 0) {
          return toStruct(fieldName, readFromStream(customTypeFieldInfo.getByteOrder(), in));
        } else {
          throw new IllegalArgumentException("Array of nullable byte arrays is unsupported");
        }
      }

      private void writeTo(final JBBPBitOutputStream outStream, final JBBPByteOrder order,
                           final byte[] data) throws IOException {
        if (data == null) {
          outStream.writeInt(-1, order);
        } else {
          outStream.writeInt(data.length, order);
          outStream.write(data);
        }
      }

      @Override
      public void writeCustomField(JBBPOut context, JBBPBitOutputStream outStream,
                                   Object instanceToSave, Field instanceCustomField,
                                   Bin fieldAnnotation, Object value) throws IOException {
        if (fieldAnnotation.customType().equals(TYPE)) {
          writeTo(outStream, fieldAnnotation.byteOrder(), (byte[]) value);
        } else {
          throw new IllegalArgumentException(
              "Unsupported custom type: " + fieldAnnotation.customType());
        }
      }

      private JBBPFieldStruct toStruct(JBBPNamedFieldInfo fieldName, final byte[] array) {
        if (array == null) {
          return new JBBPFieldStruct(fieldName, new JBBPAbstractField[0]);
        } else {
          return new JBBPFieldStruct(fieldName,
              new JBBPAbstractField[] {new JBBPFieldArrayByte(null, array)});
        }
      }

      private byte[] fromStruct(final JBBPFieldStruct struct) {
        final JBBPAbstractField[] fields = struct.getArray();
        return fields.length == 0 ? null : ((JBBPFieldArrayByte) struct.getArray()[0]).getArray();
      }

      @Override
      public Object prepareObjectForMapping(JBBPFieldStruct parsedBlock,
                                            Bin annotation,
                                            Field field) {
        if (annotation.customType().equals(TYPE)) {
          if (field.getType() == byte[][].class) {
            final JBBPFieldArrayStruct structs =
                parsedBlock.findFieldForNameAndType(field.getName(), JBBPFieldArrayStruct.class);
            final byte[][] result = new byte[structs.size()][];
            for (int i = 0; i < structs.size(); i++) {
              result[i] = fromStruct(structs.getElementAt(i));
            }
            return result;
          } else {
            return fromStruct(
                parsedBlock.findFieldForNameAndType(field.getName(), JBBPFieldStruct.class));
          }
        } else {
          throw new IllegalArgumentException("Unexpected custom type: " + annotation.customType());
        }
      }
    }
    ;

    final NullableByteArrayProcessor nullableByteArrayProcessor = new NullableByteArrayProcessor();

    class Klazz {
      @Bin
      int a;
      @Bin(custom = true, customType = "nullableByteArray")
      byte[] b;
      @Bin
      int c;
    }
    ;

    Klazz object = new Klazz();
    object.a = 12345;
    object.b = null;
    object.c = 7890;

    final byte[] withNullField =
        JBBPOut.BeginBin().Bin(object, nullableByteArrayProcessor).End().toByteArray();

    assertArrayEquals(
        new byte[] {0, 0, 48, 57, (byte) -1, (byte) -1, (byte) -1, (byte) -1, 0, 0, 30, (byte) -46},
        withNullField);

    object = new Klazz();
    object.a = 12345;
    object.b = new byte[] {1, 2, 3};
    object.c = 7890;

    final byte[] withContent =
        JBBPOut.BeginBin().Bin(object, nullableByteArrayProcessor).End().toByteArray();
    assertArrayEquals(new byte[] {0, 0, 48, 57, 0, 0, 0, 3, 1, 2, 3, 0, 0, 30, (byte) -46},
        withContent);

    object = new Klazz();
    object.a = 12345;
    object.b = new byte[0];
    object.c = 7890;

    final byte[] withZeroLength =
        JBBPOut.BeginBin().Bin(object, nullableByteArrayProcessor).End().toByteArray();
    assertArrayEquals(new byte[] {0, 0, 48, 57, 0, 0, 0, 0, 0, 0, 30, (byte) -46}, withZeroLength);

    JBBPParser parser =
        JBBPParser.prepare("int a; nullableByteArray b; int c;", nullableByteArrayProcessor);

    Klazz parsed = parser.parse(withNullField).mapTo(new Klazz(), nullableByteArrayProcessor);

    assertEquals(12345, parsed.a);
    assertNull(parsed.b);
    assertEquals(7890, parsed.c);

    parsed = parser.parse(withZeroLength).mapTo(new Klazz(), nullableByteArrayProcessor);
    assertEquals(12345, parsed.a);
    assertArrayEquals(new byte[0], parsed.b);
    assertEquals(7890, parsed.c);

    parsed = parser.parse(withContent).mapTo(new Klazz(), nullableByteArrayProcessor);
    assertEquals(12345, parsed.a);
    assertArrayEquals(new byte[] {1, 2, 3}, parsed.b);
    assertEquals(7890, parsed.c);
  }

}
