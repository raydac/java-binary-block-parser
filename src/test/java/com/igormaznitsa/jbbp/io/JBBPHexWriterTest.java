/*
 * Copyright 2014 Igor Maznitsa.
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

import java.io.*;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

public class JBBPHexWriterTest {

  private JBBPHexWriter writer;

  private void assertFile(final String fileName, final String text) throws Exception {
    final InputStream in = this.getClass().getResourceAsStream(fileName);
    assertNotNull("Can't find file [" + fileName + "]", in);
    Reader reader = null;
    String fileText = null;
    try {
      reader = new InputStreamReader(in, "UTF-8");
      final StringWriter wr = new StringWriter();

      while (true) {
        final int chr = reader.read();
        if (chr < 0) {
          break;
        }
        wr.write(chr);
      }
      wr.close();
      fileText = wr.toString();
    }
    finally {
      reader.close();
    }

    assertEquals("File content must be equals", fileText, text);
  }

  @Before
  public void before() {
    writer = new JBBPHexWriter(new StringWriter(), JBBPByteOrder.BIG_ENDIAN, "\n", 16, "0x", ".", ";", ",");
  }

  @Test
  public void testCommentHelloWorld() throws Exception {
    assertEquals(";Hello World\n", writer.Comment("Hello World").Close().toString());
  }

  @Test
  public void testCommentAndValue() throws Exception {
    assertEquals(";Hello World\n.0x01,0x00000001,0x0000000000000001", writer.Comment("Hello World").Byte(1).Int(1).Long(1).Close().toString());
  }

  @Test
  public void testValueAndMultilineComment() throws Exception {
    final String text = writer
            .Comment("Comment1", "Comment2")
            .IndentInc()
            .Int(1)
            .Comment("It's header")
            .Separator()
            .IndentDec()
            .Comment("It's body")
            .IndentInc()
            .Byte(new byte[]{1, 2, 3, 4})
            .Comment("Body", "Next comment", "One more comment")
            .newLine()
            .Byte(new byte[]{0x0A, 0x0B})
            .Comment("Part", "Part line2", "Part line3")
            .Separator()
            .IndentInc()
            .Comment("End")
            .IndentDec(1)
            .Comment("The End")
            .IndentDec()
            .Long(-1L)
            .Close().toString();
    assertFile("testwriter.txt", text);
  }

}
