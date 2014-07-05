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
import com.igormaznitsa.jbbp.model.JBBPFieldArrayByte;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayStruct;
import com.igormaznitsa.jbbp.model.JBBPFieldInt;
import com.igormaznitsa.jbbp.model.JBBPFieldLong;
import com.igormaznitsa.jbbp.model.JBBPFieldStruct;
import java.io.InputStream;
import java.util.zip.CRC32;
import static org.junit.Assert.*;
import org.junit.Test;

public class PNGParsingTest extends AbstractParserIntegrationTest {

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
      
      assertEquals(0x89504E470D0A1A0AL,result.findFieldForNameAndType("header",JBBPFieldLong.class).getAsLong());
      
      final JBBPFieldArrayStruct chunks = result.findFieldForNameAndType("chunk", JBBPFieldArrayStruct.class);
      
      
      final String [] chunkNames = new String[]{"IHDR","gAMA","bKGD","pHYs","tIME","tEXt","IDAT","IEND"};
      final int [] chunkSizes = new int[]{0x0D, 0x04, 0x06, 0x09, 0x07, 0x19, 0x0E5F, 0x00};
      
      assertEquals(chunkNames.length,chunks.size());
      
      for(int i=0;i<chunks.size();i++){
        assertChunk(chunkNames[i], chunkSizes[i], (JBBPFieldStruct)chunks.getElementAt(i));
      }
    }
    finally {
      closeResource(pngStream);
    }
  }
  
  private static void assertChunk(final String name, final int length, final JBBPFieldStruct chunk){
    final int chunkName = (name.charAt(0)<<24)|(name.charAt(1)<<16)|(name.charAt(2)<<8)|name.charAt(3);
    
    assertEquals("Chunk must be "+name, chunkName, chunk.findFieldForNameAndType("type",JBBPFieldInt.class).getAsInt());
    assertEquals("Chunk length must be "+length, length, chunk.findFieldForNameAndType("length", JBBPFieldInt.class).getAsInt());
    
    final CRC32 crc32 = new CRC32();
    crc32.update(name.charAt(0));
    crc32.update(name.charAt(1));
    crc32.update(name.charAt(2));
    crc32.update(name.charAt(3));
    
    if (length!=0){
      final byte [] array = chunk.findFieldForType(JBBPFieldArrayByte.class).getArray();
      assertEquals("Data array "+name+" must be "+length, length, array.length);
      crc32.update(array);
    }
    
    final int crc = (int)crc32.getValue();
    assertEquals("CRC32 for "+name+" must be "+crc, crc, chunk.findLastFieldForType(JBBPFieldInt.class).getAsInt());
    
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

      final JBBPFieldStruct result = pngParser.parse(pngStream, new JBBPExternalValueProvider() {

        public int provideArraySize(final String fieldName, final JBBPNamedNumericFieldMap numericFieldMap, final JBBPCompiledBlock compiledBlock) {
          if ("value".equals(fieldName)){
            return numericFieldMap.findFieldForPathAndType("chunk.length", JBBPFieldInt.class).getAsInt();
          }
          fail("Unexpected variable '"+fieldName+'\'');
          return -1;
        }
      });

      assertEquals(0x89504E470D0A1A0AL, result.findFieldForNameAndType("header", JBBPFieldLong.class).getAsLong());

      final JBBPFieldArrayStruct chunks = result.findFieldForNameAndType("chunk", JBBPFieldArrayStruct.class);

      final String[] chunkNames = new String[]{"IHDR", "gAMA", "bKGD", "pHYs", "tIME", "tEXt", "IDAT", "IEND"};
      final int[] chunkSizes = new int[]{0x0D, 0x04, 0x06, 0x09, 0x07, 0x19, 0x0E5F, 0x00};

      assertEquals(chunkNames.length, chunks.size());

      for (int i = 0; i < chunks.size(); i++) {
        assertChunk(chunkNames[i], chunkSizes[i], (JBBPFieldStruct) chunks.getElementAt(i));
      }
    }
    finally {
      closeResource(pngStream);
    }
  }

}
