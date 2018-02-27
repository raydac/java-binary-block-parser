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

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayByte;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayStruct;
import com.igormaznitsa.jbbp.model.JBBPFieldInt;
import com.igormaznitsa.jbbp.model.JBBPFieldStruct;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WAVParsingTest extends AbstractParserIntegrationTest {

  private static final JBBPParser wavParser = JBBPParser.prepare(
      "<int ChunkID;"
          + "<int ChunkSize;"
          + "<int Format;"
          + "SubChunks [_]{"
          + "  <int SubChunkID;"
          + "  <int SubChunkSize;"
          + "  byte [SubChunkSize] data;"
          + "  align:2;"
          + "}"
  );

  private static String wavInt2Str(final int value) {
    return new String(new char[] {(char) (value & 0xFF), (char) ((value >>> 8) & 0xFF), (char) ((value >>> 16) & 0xFF), (char) (value >>> 24)});
  }

  private static void assertWavChunks(final JBBPFieldStruct parsedWav, final String... chunks) {
    assertEquals(0x46464952, parsedWav.findFieldForNameAndType("ChunkID", JBBPFieldInt.class).getAsInt());
    assertEquals(0x45564157, parsedWav.findFieldForNameAndType("Format", JBBPFieldInt.class).getAsInt());

    int calculatedSize = 4;

    int index = 0;

    assertEquals(chunks.length, parsedWav.findFieldForNameAndType("SubChunks", JBBPFieldArrayStruct.class).size(), "Number of parsed subchunks must be [" + chunks.length + ']');

    for (final JBBPFieldStruct subchunk : parsedWav.findFieldForNameAndType("SubChunks", JBBPFieldArrayStruct.class)) {
      final String strChunkId = chunks[index++];
      assertEquals(4, strChunkId.length(), "WAV subchunk must have 4 char length [" + strChunkId + ']');
      assertEquals(strChunkId, wavInt2Str(subchunk.findFieldForNameAndType("SubChunkID", JBBPFieldInt.class).getAsInt()));
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
    } finally {
      JBBPUtils.closeQuietly(wavFileStream);
    }
  }

  @Test
  public void testParsingWAVFile_M1F1float64WEAFsp() throws Exception {
    final InputStream wavFileStream = getResourceAsInputStream("M1F1-float64WE-AFsp.wav");
    try {
      assertWavChunks(wavParser.parse(wavFileStream), "fmt ", "fact", "data", "afsp", "LIST");
    } finally {
      JBBPUtils.closeQuietly(wavFileStream);
    }
  }

  @Test
  public void testParsingWAVFile_Drmapan() throws Exception {
    final InputStream wavFileStream = getResourceAsInputStream("drmapan.wav");
    try {
      assertWavChunks(wavParser.parse(wavFileStream), "fmt ", "PEAK", "data");
    } finally {
      JBBPUtils.closeQuietly(wavFileStream);
    }
  }

  @Test
  public void testParsingWAVFile_11kgsm() throws Exception {
    final InputStream wavFileStream = getResourceAsInputStream("11kgsm.wav");
    try {
      assertWavChunks(wavParser.parse(wavFileStream), "fmt ", "fact", "data");
    } finally {
      JBBPUtils.closeQuietly(wavFileStream);
    }
  }

  @Test
  public void testParsingWAVFile_8ksbc12() throws Exception {
    final InputStream wavFileStream = getResourceAsInputStream("8ksbc12.wav");
    try {
      assertWavChunks(wavParser.parse(wavFileStream), "fmt ", "fact", "data");
    } finally {
      JBBPUtils.closeQuietly(wavFileStream);
    }
  }
}
