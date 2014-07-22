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
import com.igormaznitsa.jbbp.model.*;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.InputStream;
import org.junit.Test;
import static org.junit.Assert.*;

public class WAVParsingTest extends AbstractParserIntegrationTest {

  private static final JBBPParser wavParser = JBBPParser.prepare(
          "int ChunkID;"
          + "<int ChunkSize;"
          + "int Format;"
          + "SubChunks [_]{"
          + "  int SubChunkID;"
          + "  <int SubChunkSize;"
          + "  byte [SubChunkSize] data;"
          + "  align:2;"
          + "}"
  );

  private static void assertWavChunks(final JBBPFieldStruct parsedWav, final String... chunks) {
    assertEquals(0x52494646, parsedWav.findFieldForNameAndType("ChunkID", JBBPFieldInt.class).getAsInt());
    assertEquals(0x57415645, parsedWav.findFieldForNameAndType("Format", JBBPFieldInt.class).getAsInt());

    int calculatedSize = 4;

    int index = 0;

    assertEquals("Number of parsed subchunks must be [" + chunks.length + ']', chunks.length, parsedWav.findFieldForNameAndType("SubChunks", JBBPFieldArrayStruct.class).size());

    for (final JBBPFieldStruct subchunk : parsedWav.findFieldForNameAndType("SubChunks", JBBPFieldArrayStruct.class)) {
      final String strChunkId = chunks[index++];
      assertEquals("WAV subchunk must have 4 char length [" + strChunkId + ']', 4, strChunkId.length());
      final int subChunkId = (strChunkId.charAt(0) << 24) | (strChunkId.charAt(1) << 16) | (strChunkId.charAt(2) << 8) | strChunkId.charAt(3);
      assertEquals(subChunkId, subchunk.findFieldForNameAndType("SubChunkID", JBBPFieldInt.class).getAsInt());
      final int subChunkSize = subchunk.findFieldForNameAndType("SubChunkSize", JBBPFieldInt.class).getAsInt();
      assertEquals(subChunkSize, subchunk.findFieldForNameAndType("data", JBBPFieldArrayByte.class).size());
      calculatedSize += subChunkSize + 8 + (subChunkSize & 1);
    }

    assertEquals(calculatedSize, parsedWav.findFieldForNameAndType("ChunkSize", JBBPFieldInt.class).getAsInt());
  }

  @Test
  public void testParsingWAVFile_TruSpeech() throws Exception {
    final InputStream wavFileStream = getResourceAsInputStream("truspech.wav");
    try {
      assertWavChunks(wavParser.parse(wavFileStream), "fmt ", "fact", "data");
    }
    finally {
      JBBPUtils.closeQuietly(wavFileStream);
    }
  }

  @Test
  public void testParsingWAVFile_M1F1float64WEAFsp() throws Exception {
    final InputStream wavFileStream = getResourceAsInputStream("M1F1-float64WE-AFsp.wav");
    try {
      assertWavChunks(wavParser.parse(wavFileStream), "fmt ", "fact", "data", "afsp", "LIST");
    }
    finally {
      JBBPUtils.closeQuietly(wavFileStream);
    }
  }

  @Test
  public void testParsingWAVFile_Drmapan() throws Exception {
    final InputStream wavFileStream = getResourceAsInputStream("drmapan.wav");
    try {
      assertWavChunks(wavParser.parse(wavFileStream), "fmt ", "PEAK", "data");
    }
    finally {
      JBBPUtils.closeQuietly(wavFileStream);
    }
  }

  @Test
  public void testParsingWAVFile_11kgsm() throws Exception {
    final InputStream wavFileStream = getResourceAsInputStream("11kgsm.wav");
    try {
      assertWavChunks(wavParser.parse(wavFileStream), "fmt ", "fact", "data");
    }
    finally {
      JBBPUtils.closeQuietly(wavFileStream);
    }
  }

  @Test
  public void testParsingWAVFile_8ksbc12() throws Exception {
    final InputStream wavFileStream = getResourceAsInputStream("8ksbc12.wav");
    try {
      assertWavChunks(wavParser.parse(wavFileStream), "fmt ", "fact", "data");
    }
    finally {
      JBBPUtils.closeQuietly(wavFileStream);
    }
  }
}
