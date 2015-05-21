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
package com.igormaznitsa.jbbp.it;

import com.igormaznitsa.jbbp.*;
import com.igormaznitsa.jbbp.compiler.JBBPCompiledBlock;
import com.igormaznitsa.jbbp.io.JBBPOut;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.model.*;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import static org.junit.Assert.*;
import org.junit.Test;

public class PNGParsingTest extends AbstractParserIntegrationTest {

  @Test
  public void testPngParsing_Mapping() throws Exception {
    final InputStream pngStream = getResourceAsInputStream("picture.png");
    try {

      final JBBPParser pngParser = JBBPParser.prepare(
              "long header;"
              + "// chunks\n"
              + "chunk [_]{"
              + "   int length; "
              + "   int type; "
              + "   byte[length] data; "
              + "   int crc;"
              + "}"
      );

      @Bin
      class Chunk {

        int length;
        int type;
        byte[] data;
        int crc;
      }
      @Bin
      class Png {

        long hEAder;
        Chunk[] chuNK;
      }

      final Png png = pngParser.parse(pngStream).mapTo(Png.class);

      assertEquals(0x89504E470D0A1A0AL, png.hEAder);

      final String[] chunkNames = new String[]{"IHDR", "gAMA", "bKGD", "pHYs", "tIME", "tEXt", "IDAT", "IEND"};
      final int[] chunkSizes = new int[]{0x0D, 0x04, 0x06, 0x09, 0x07, 0x19, 0x0E5F, 0x00};

      assertEquals(chunkNames.length, png.chuNK.length);

      for (int i = 0; i < png.chuNK.length; i++) {
        assertChunk(chunkNames[i], chunkSizes[i], png.chuNK[i].type, png.chuNK[i].length, png.chuNK[i].crc, png.chuNK[i].data);
      }

      assertEquals(3847, pngParser.getFinalStreamByteCounter());

    }
    finally {
      JBBPUtils.closeQuietly(pngStream);
    }
  }

  @Test
  public void testPngParsing() throws Exception {
    final InputStream pngStream = getResourceAsInputStream("picture.png");
    try {

      final JBBPParser pngParser = JBBPParser.prepare(
              "long header;"
              + "// chunks\n"
              + "chunk [_]{"
              + "   int length; "
              + "   int type; "
              + "   byte[length] data; "
              + "   int crc;"
              + "}"
      );

      final JBBPFieldStruct result = pngParser.parse(pngStream);

      assertEquals(0x89504E470D0A1A0AL, result.findFieldForNameAndType("header", JBBPFieldLong.class).getAsLong());

      final JBBPFieldArrayStruct chunks = result.findFieldForNameAndType("chunk", JBBPFieldArrayStruct.class);

      final String[] chunkNames = new String[]{"IHDR", "gAMA", "bKGD", "pHYs", "tIME", "tEXt", "IDAT", "IEND"};
      final int[] chunkSizes = new int[]{0x0D, 0x04, 0x06, 0x09, 0x07, 0x19, 0x0E5F, 0x00};

      assertEquals(chunkNames.length, chunks.size());

      for (int i = 0; i < chunks.size(); i++) {
        assertChunk(chunkNames[i], chunkSizes[i], chunks.getElementAt(i));
      }

      assertEquals(3847, pngParser.getFinalStreamByteCounter());
    }
    finally {
      JBBPUtils.closeQuietly(pngStream);
    }
  }

  private static void assertChunk(final String name, final int length, final JBBPFieldStruct chunk) {
    final int chunkName = (name.charAt(0) << 24) | (name.charAt(1) << 16) | (name.charAt(2) << 8) | name.charAt(3);

    assertEquals("Chunk must be " + name, chunkName, chunk.findFieldForNameAndType("type", JBBPFieldInt.class).getAsInt());
    assertEquals("Chunk length must be " + length, length, chunk.findFieldForNameAndType("length", JBBPFieldInt.class).getAsInt());

    final CRC32 crc32 = new CRC32();
    crc32.update(name.charAt(0));
    crc32.update(name.charAt(1));
    crc32.update(name.charAt(2));
    crc32.update(name.charAt(3));

    if (length != 0) {
      final byte[] array = chunk.findFieldForType(JBBPFieldArrayByte.class).getArray();
      assertEquals("Data array " + name + " must be " + length, length, array.length);
      crc32.update(array);
    }

    final int crc = (int) crc32.getValue();
    assertEquals("CRC32 for " + name + " must be " + crc, crc, chunk.findLastFieldForType(JBBPFieldInt.class).getAsInt());

  }

  private static void assertChunk(final String name, final int length, final int chunkType, final int chunkLength, final int chunkCrc, final byte[] data) {
    final int chunkName = (name.charAt(0) << 24) | (name.charAt(1) << 16) | (name.charAt(2) << 8) | name.charAt(3);

    assertEquals("Chunk must be " + name, chunkName, chunkName);
    assertEquals("Chunk length must be " + length, length, chunkLength);

    final CRC32 crc32 = new CRC32();
    crc32.update(name.charAt(0));
    crc32.update(name.charAt(1));
    crc32.update(name.charAt(2));
    crc32.update(name.charAt(3));

    if (length != 0) {
      assertEquals("Data array " + name + " must be " + length, length, data.length);
      crc32.update(data);
    }

    final int crc = (int) crc32.getValue();
    assertEquals("CRC32 for " + name + " must be " + crc, crc, chunkCrc);

  }

  @Test
  public void testPngParsing_WithExternalValue() throws Exception {
    final InputStream pngStream = getResourceAsInputStream("picture.png");
    try {

      final JBBPParser pngParser = JBBPParser.prepare(
              "long header;"
              + "// chunks\n"
              + "chunk [_]{"
              + "   int length; "
              + "   int type; "
              + "   byte[$value] data; "
              + "   int crc;"
              + "}"
      );

      final JBBPFieldStruct result = pngParser.parse(pngStream, null, new JBBPExternalValueProvider() {

        public int provideArraySize(final String fieldName, final JBBPNamedNumericFieldMap numericFieldMap, final JBBPCompiledBlock compiledBlock) {
          if ("value".equals(fieldName)) {
            return numericFieldMap.findFieldForPathAndType("chunk.length", JBBPFieldInt.class).getAsInt();
          }
          fail("Unexpected variable '" + fieldName + '\'');
          return -1;
        }
      });

      assertEquals(0x89504E470D0A1A0AL, result.findFieldForNameAndType("header", JBBPFieldLong.class).getAsLong());

      final JBBPFieldArrayStruct chunks = result.findFieldForNameAndType("chunk", JBBPFieldArrayStruct.class);

      final String[] chunkNames = new String[]{"IHDR", "gAMA", "bKGD", "pHYs", "tIME", "tEXt", "IDAT", "IEND"};
      final int[] chunkSizes = new int[]{0x0D, 0x04, 0x06, 0x09, 0x07, 0x19, 0x0E5F, 0x00};

      assertEquals(chunkNames.length, chunks.size());

      for (int i = 0; i < chunks.size(); i++) {
        assertChunk(chunkNames[i], chunkSizes[i], chunks.getElementAt(i));
      }

      assertEquals(3847, pngParser.getFinalStreamByteCounter());
    }
    finally {
      JBBPUtils.closeQuietly(pngStream);
    }
  }

  @Test
  public void testPngParsingAndSynthesisThroughMapping() throws Exception {
    final InputStream pngStream = getResourceAsInputStream("picture.png");
    try {

      final JBBPParser pngParser = JBBPParser.prepare(
              "long header;"
              + "// chunks\n"
              + "chunk [_]{"
              + "   int length; "
              + "   int type; "
              + "   byte[length] data; "
              + "   int crc;"
              + "}"
      );

      class Chunk {

        @Bin(outOrder = 1)
        int length;
        @Bin(outOrder = 2)
        int type;
        @Bin(outOrder = 3)
        byte[] data;
        @Bin(outOrder = 4)
        int crc;
      }
      class Png {

        @Bin(outOrder = 1)
        long hEAder;
        @Bin(outOrder = 2)
        Chunk[] chuNK;
      }

      final Png parsedAndMapped = pngParser.parse(pngStream).mapTo(Png.class);
      final byte[] saved = JBBPOut.BeginBin().Bin(parsedAndMapped).End().toByteArray();

      assertResource("picture.png", saved);
    }
    finally {
      JBBPUtils.closeQuietly(pngStream);
    }
  }

  @Test
  public void testPngParsing_asMap() throws Exception {
    final InputStream pngStream = getResourceAsInputStream("picture.png");
    try {

      final JBBPParser pngParser = JBBPParser.prepare(
          "long header;"
              + "// chunks\n"
              + "chunk [_]{"
              + "   int length; "
              + "   int type; "
              + "   byte[length] data; "
              + "   int crc;"
              + "}"
      );

      final JBBPFieldStruct result = pngParser.parse(pngStream);
      final Map<String, Object> resultMap = result.asMap();

      assertEquals(0x89504E470D0A1A0AL, resultMap.get("header"));

      final List<Object> chunks = (List<Object>)resultMap.get("chunk");
      final String[] chunkNames = new String[]{"IHDR", "gAMA", "bKGD", "pHYs", "tIME", "tEXt", "IDAT", "IEND"};
      final int[] chunkSizes = new int[]{0x0D, 0x04, 0x06, 0x09, 0x07, 0x19, 0x0E5F, 0x00};

      assertEquals(chunkNames.length, chunks.size());

      for (int i = 0; i < chunks.size(); i++) {
        final String name = chunkNames[i];
        final int length = chunkSizes[i];
        final Map<String, Object> chunk = (Map<String, Object>)chunks.get(i);
        final int chunkName = (name.charAt(0) << 24) | (name.charAt(1) << 16) | (name.charAt(2) << 8) | name.charAt(3);

        assertEquals("Chunk must be " + name, chunkName, chunk.get("type"));
        assertEquals("Chunk length must be " + length, length, chunk.get("length"));

        final CRC32 crc32 = new CRC32();
        crc32.update(name.charAt(0));
        crc32.update(name.charAt(1));
        crc32.update(name.charAt(2));
        crc32.update(name.charAt(3));

        if (length != 0) {
          assertEquals("Data array " + name + " must be " + length, length, ((List)chunk.get("data")).size());
          final byte[] array = new byte[length];
          final List<Integer> data = (List<Integer>)chunk.get("data");
          for (int cnt = 0; cnt < length; cnt++) {
            array[cnt] = data.get(cnt).byteValue();
          }
          crc32.update(array);
        }

        final int crc = (int) crc32.getValue();
        assertEquals("CRC32 for " + name + " must be " + crc, crc, chunk.get("crc"));
      }

      assertEquals(3847, pngParser.getFinalStreamByteCounter());
    }
    finally {
      JBBPUtils.closeQuietly(pngStream);
    }
  }

  @Test
  public void testPngParsing_asMap_unnamed() throws Exception {
    final InputStream pngStream = getResourceAsInputStream("picture.png");
    try {

      final JBBPParser pngParser = JBBPParser.prepare(
          "long;"
              + "// chunks\n"
              + "[_]{"
              + "   int length; "
              + "   int; "
              + "   byte[length]; "
              + "   int;"
              + "}"
      );

      final JBBPFieldStruct result = pngParser.parse(pngStream);
      final Map<String, Object> resultMap = result.asMap();
      assertEquals(0x89504E470D0A1A0AL, resultMap.get("field_long_0"));

      final List<Object> chunks = (List<Object>)resultMap.get("array_struct_1");
      final String[] chunkNames = new String[]{"IHDR", "gAMA", "bKGD", "pHYs", "tIME", "tEXt", "IDAT", "IEND"};
      final int[] chunkSizes = new int[]{0x0D, 0x04, 0x06, 0x09, 0x07, 0x19, 0x0E5F, 0x00};

      assertEquals(chunkNames.length, chunks.size());

      for (int i = 0; i < chunks.size(); i++) {
        final String name = chunkNames[i];
        final int length = chunkSizes[i];
        final Map<String, Object> chunk = (Map<String, Object>)chunks.get(i);
        final int chunkName = (name.charAt(0) << 24) | (name.charAt(1) << 16) | (name.charAt(2) << 8) | name.charAt(3);

        assertEquals("Chunk must be " + name, chunkName, chunk.get("field_int_1"));
        assertEquals("Chunk length must be " + length, length, chunk.get("length"));

        final CRC32 crc32 = new CRC32();
        crc32.update(name.charAt(0));
        crc32.update(name.charAt(1));
        crc32.update(name.charAt(2));
        crc32.update(name.charAt(3));

        if (length != 0) {
          assertEquals("Data array " + name + " must be " + length, length, ((List)chunk.get("array_byte_2")).size());
          final byte[] array = new byte[length];
          final List<Integer> data = (List<Integer>)chunk.get("array_byte_2");
          for (int cnt = 0; cnt < length; cnt++) {
            array[cnt] = data.get(cnt).byteValue();
          }
          crc32.update(array);
        }

        final int crc = (int) crc32.getValue();
        assertEquals("CRC32 for " + name + " must be " + crc, crc, chunk.get("field_int_3"));
      }

      assertEquals(3847, pngParser.getFinalStreamByteCounter());
    }
    finally {
      JBBPUtils.closeQuietly(pngStream);
    }
  }
}
