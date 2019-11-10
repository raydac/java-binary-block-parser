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

import static com.igormaznitsa.jbbp.TestUtils.assertPngChunk;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.JBBPOut;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayByte;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayStruct;
import com.igormaznitsa.jbbp.model.JBBPFieldInt;
import com.igormaznitsa.jbbp.model.JBBPFieldLong;
import com.igormaznitsa.jbbp.model.JBBPFieldStruct;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.InputStream;
import org.apache.commons.codec.digest.PureJavaCrc32;
import org.junit.jupiter.api.Test;

public class PNGParsingTest extends AbstractParserIntegrationTest {

  private static void assertChunk(final String name, final int length, final JBBPFieldStruct chunk) {
    final int chunkName = (name.charAt(0) << 24) | (name.charAt(1) << 16) | (name.charAt(2) << 8) | name.charAt(3);

    assertEquals(chunkName, chunk.findFieldForNameAndType("type", JBBPFieldInt.class).getAsInt(), "Chunk must be " + name);
    assertEquals(length, chunk.findFieldForNameAndType("length", JBBPFieldInt.class).getAsInt(), "Chunk length must be " + length);

    final PureJavaCrc32 crc32 = new PureJavaCrc32();
    crc32.update(name.charAt(0));
    crc32.update(name.charAt(1));
    crc32.update(name.charAt(2));
    crc32.update(name.charAt(3));

    if (length != 0) {
      final byte[] array = chunk.findFieldForType(JBBPFieldArrayByte.class).getArray();
      assertEquals(length, array.length, "Data array " + name + " must be " + length);
      for (final byte b : array) {
        crc32.update(b & 0xFF);
      }
    }

    final int crc = (int) crc32.getValue();
    assertEquals(crc, chunk.findLastFieldForType(JBBPFieldInt.class).getAsInt(), "CRC32 for " + name + " must be " + crc);

  }

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

        Chunk makeChunk() {
          return new Chunk();
        }
      }

      final Png result = new Png();
      final Png png = pngParser.parse(pngStream).mapTo(result, aClass -> {
        if (aClass == Chunk.class) {
          return result.makeChunk();
        }
        return null;
      });

      assertEquals(0x89504E470D0A1A0AL, png.hEAder);

      final String[] chunkNames = new String[] {"IHDR", "gAMA", "bKGD", "pHYs", "tIME", "tEXt", "IDAT", "IEND"};
      final int[] chunkSizes = new int[] {0x0D, 0x04, 0x06, 0x09, 0x07, 0x19, 0x0E5F, 0x00};

      assertEquals(chunkNames.length, png.chuNK.length);

      for (int i = 0; i < png.chuNK.length; i++) {
        assertPngChunk(chunkNames[i], chunkSizes[i], png.chuNK[i].type, png.chuNK[i].length, png.chuNK[i].crc, png.chuNK[i].data);
      }

      assertEquals(3847, pngParser.getFinalStreamByteCounter());

    } finally {
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

      final String[] chunkNames = new String[] {"IHDR", "gAMA", "bKGD", "pHYs", "tIME", "tEXt", "IDAT", "IEND"};
      final int[] chunkSizes = new int[] {0x0D, 0x04, 0x06, 0x09, 0x07, 0x19, 0x0E5F, 0x00};

      assertEquals(chunkNames.length, chunks.size());

      for (int i = 0; i < chunks.size(); i++) {
        assertChunk(chunkNames[i], chunkSizes[i], chunks.getElementAt(i));
      }

      assertEquals(3847, pngParser.getFinalStreamByteCounter());
    } finally {
      JBBPUtils.closeQuietly(pngStream);
    }
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

      final JBBPFieldStruct result = pngParser.parse(pngStream, null, (fieldName, numericFieldMap, compiledBlock) -> {
        if ("value".equals(fieldName)) {
          return numericFieldMap.findFieldForPathAndType("chunk.length", JBBPFieldInt.class).getAsInt();
        }
        fail("Unexpected variable '" + fieldName + '\'');
        return -1;
      });

      assertEquals(0x89504E470D0A1A0AL, result.findFieldForNameAndType("header", JBBPFieldLong.class).getAsLong());

      final JBBPFieldArrayStruct chunks = result.findFieldForNameAndType("chunk", JBBPFieldArrayStruct.class);

      final String[] chunkNames = new String[] {"IHDR", "gAMA", "bKGD", "pHYs", "tIME", "tEXt", "IDAT", "IEND"};
      final int[] chunkSizes = new int[] {0x0D, 0x04, 0x06, 0x09, 0x07, 0x19, 0x0E5F, 0x00};

      assertEquals(chunkNames.length, chunks.size());

      for (int i = 0; i < chunks.size(); i++) {
        assertChunk(chunkNames[i], chunkSizes[i], chunks.getElementAt(i));
      }

      assertEquals(3847, pngParser.getFinalStreamByteCounter());
    } finally {
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

        @Bin(order = 1)
        int length;
        @Bin(order = 2)
        int type;
        @Bin(order = 3)
        byte[] data;
        @Bin(order = 4)
        int crc;
      }
      class Png {

        @Bin(order = 1)
        long hEAder;
        @Bin(order = 2)
        Chunk[] chuNK;

        Chunk makeNewChunk() {
          return new Chunk();
        }
      }

      final Png result = new Png();
      final Png parsedAndMapped = pngParser.parse(pngStream).mapTo(result, aClass -> {
        if (aClass == Chunk.class) {
          return result.makeNewChunk();
        }
        return null;
      });
      final byte[] saved = JBBPOut.BeginBin().Bin(parsedAndMapped).End().toByteArray();

      assertResource("picture.png", saved);
    } finally {
      JBBPUtils.closeQuietly(pngStream);
    }
  }

}
