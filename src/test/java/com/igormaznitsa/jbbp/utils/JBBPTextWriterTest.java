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
package com.igormaznitsa.jbbp.utils;

import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.utils.JBBPTextWriter.Extra;
import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

public class JBBPTextWriterTest {

  private JBBPTextWriter writer;

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
      if (reader != null) {
        reader.close();
      }
    }

    assertEquals("File content must be equals", fileText, text);
  }

  @Before
  public void before() {
    writer = new JBBPTextWriter(new StringWriter(), JBBPByteOrder.BIG_ENDIAN, "\n", 16, "0x", ".", ";", ",");
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
            .HR()
            .IndentDec()
            .Comment("It's body")
            .IndentInc()
            .Byte(new byte[]{1, 2, 3, 4})
            .Comment("Body", "Next comment", "One more comment")
            .BR()
            .Byte(new byte[]{0x0A, 0x0B})
            .Comment("Part", "Part line2", "Part line3")
            .HR()
            .IndentInc()
            .Comment("End")
            .IndentDec(1)
            .Comment("The End")
            .IndentDec()
            .Long(-1L)
            .Close().toString();
    assertFile("testwriter.txt", text);
  }

  @Test(expected = NullPointerException.class)
  public void testExtras_ErrorForNull() throws Exception {
    writer.AddExtras((Extra[])null);
  }

  @Test(expected = IllegalStateException.class)
  public void testExtras_ErrorForEmptyExtras() throws Exception {
    writer.Obj(0, new Object());
  }

  @Test
  public void testExtras_NotPrintedForNull() throws Exception {
    writer.AddExtras(new JBBPTextWriterExtraAdapter() {
      @Override
      public String doConvertObjToStr(JBBPTextWriter context, int id, Object obj) throws IOException {
        return null;
      }
    }).Obj(1, new Object());

    assertEquals("", writer.Close().toString());
  }

  @Test
  public void testMultilineComments() throws Exception {
    writer.Byte(1).Comment("Hello", "World");
    assertEquals(".0x01;Hello\n     ;World\n", writer.Close().toString());
  }

  @Test
  public void testHorizontalRule() throws Exception {
    writer.SetHR(10, '>').HR().Byte(1);
    assertEquals(";>>>>>>>>>>\n.0x01", writer.Close().toString());
  }

  @Test
  public void testLineBreak() throws Exception {
    writer.Byte(1).BR().Byte(2);
    assertEquals(".0x01\n.0x02", writer.Close().toString());
  }

  @Test
  public void testExtras_PrintInfoAboutComplexObjectIntoWriter() throws Exception {
    writer.SetMaxValuesPerLine(16).AddExtras(new JBBPTextWriterExtraAdapter() {
      @Override
      public String doConvertObjToStr(final JBBPTextWriter context, final int id, final Object obj) throws IOException {
        context
                .BR()
                .Comment("Complex object")
                .HR()
                .Byte(1).Comment("Header")
                .Int(0x1234).Comment("Another header")
                .IndentInc()
                .Comment("Body")
                .HR()
                .Byte(new byte[128])
                .HR()
                .IndentDec()
                .Long(0x1234567890L).Comment("End of data")
                .HR();
        return null;
      }
    });

    assertFile("testwriter3.txt", writer.Byte(0xFF).Obj(111, "Hello").Int(0xCAFEBABE).Close().toString());
  }

  @Test
  public void testStringNumerationWithExtras() throws Exception {
    final AtomicInteger newLineCounter = new AtomicInteger(0);
    final AtomicInteger bytePrintCounter = new AtomicInteger(0);
    final AtomicInteger closeCounter = new AtomicInteger(0);

    writer.SetMaxValuesPerLine(32).SetCommentPrefix(" // ").AddExtras(new JBBPTextWriterExtraAdapter() {
      @Override
      public void onClose(final JBBPTextWriter context) throws IOException {
        context.Comment("The Last Line");
        closeCounter.incrementAndGet();
      }

      @Override
      public void onNewLine(final JBBPTextWriter context, final int lineNumber) throws IOException {
        newLineCounter.incrementAndGet();
      }

      @Override
      public void onBeforeFirstValue(final JBBPTextWriter context) throws IOException {
        context.write(JBBPUtils.ensureMinTextLength(Integer.toString(context.getLine()), 8, '0', 0) + ' ');
      }

      @Override
      public String doConvertByteToStr(final JBBPTextWriter context, final int value) throws IOException {
        bytePrintCounter.incrementAndGet();
        return null;
      }

      @Override
      public void onReachedMaxValueNumberForLine(final JBBPTextWriter context) throws IOException {
        context.Comment("End of line");
      }
    });

    for (int i = 0; i < 130; i++) {
      writer.Byte(i);
    }
    assertFile("testwriter2.txt", writer.Close().toString());
    assertEquals(5, newLineCounter.get());
    assertEquals(130, bytePrintCounter.get());
    assertEquals(1, closeCounter.get());
  }
  
  @Test
  public void testByte_OneValue() throws Exception {
    writer.Byte(10);
    writer.Byte(-1);
    assertEquals(".0x0A,0xFF", writer.Close().toString());
  }

  @Test
  public void testByte_Array() throws Exception {
    writer.Byte(new byte[]{(byte)10,(byte)-1});
    assertEquals(".0x0A,0xFF", writer.Close().toString());
  }

  @Test
  public void testByte_PartOfArray() throws Exception {
    writer.Byte(new byte[]{0,(byte)10,(byte)-1,0},1,2);
    assertEquals(".0x0A,0xFF", writer.Close().toString());
  }

  @Test
  public void testByte_String() throws Exception {
    writer.Byte("012345");
    assertEquals(".0x30,0x31,0x32,0x33,0x34,0x35", writer.Close().toString());
  }

  @Test
  public void testShort_OneValue() throws Exception {
    writer.Short(10);
    writer.Short(-1);
    assertEquals(".0x000A,0xFFFF", writer.Close().toString());
  }

  @Test
  public void testShort_Array() throws Exception {
    writer.Short(new short[]{(short)0x1234,(short)-1});
    assertEquals(".0x1234,0xFFFF", writer.Close().toString());
  }

  @Test
  public void testShort_Array_InversedByteOrder() throws Exception {
    writer.ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).Short(new short[]{(short)0x1234,(short)-1});
    assertEquals(".0x3412,0xFFFF", writer.Close().toString());
  }

  @Test
  public void testShort_PartOfArray() throws Exception {
    writer.Short(new short[]{0,(short)0x1234,(short)-1,0},1,2);
    assertEquals(".0x1234,0xFFFF", writer.Close().toString());
  }

  @Test
  public void testShort_String() throws Exception {
    writer.Short("012345");
    assertEquals(".0x0030,0x0031,0x0032,0x0033,0x0034,0x0035", writer.Close().toString());
  }

  @Test
  public void testInt_OneValue() throws Exception {
    writer.Int(0x12345678);
    writer.Int(-1);
    assertEquals(".0x12345678,0xFFFFFFFF", writer.Close().toString());
  }

  @Test
  public void testInt_Array() throws Exception {
    writer.Int(new int[]{0x12345678,-1});
    assertEquals(".0x12345678,0xFFFFFFFF", writer.Close().toString());
  }

  @Test
  public void testInt_Array_InversedByteOrder() throws Exception {
    writer.ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).Int(new int[]{0x12345678,-1});
    assertEquals(".0x78563412,0xFFFFFFFF", writer.Close().toString());
  }

  @Test
  public void testInt_PartOfArray() throws Exception {
    writer.Int(new int[]{0,0x12345678,-1,0},1,2);
    assertEquals(".0x12345678,0xFFFFFFFF", writer.Close().toString());
  }

  @Test
  public void testLong_OneValue() throws Exception {
    writer.Long(0x123456789ABCDEFFL);
    writer.Long(-1L);
    assertEquals(".0x123456789ABCDEFF,0xFFFFFFFFFFFFFFFF", writer.Close().toString());
  }

  @Test
  public void testLong_Array() throws Exception {
    writer.Long(new long[]{0x123456789ABCDEFFL,-1L});
    assertEquals(".0x123456789ABCDEFF,0xFFFFFFFFFFFFFFFF", writer.Close().toString());
  }

  @Test
  public void testLong_Array_InversedByteOrder() throws Exception {
    writer.ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).Long(new long[]{0x123456789ABCDEFFL,-1L});
    assertEquals(".0xFFDEBC9A78563412,0xFFFFFFFFFFFFFFFF", writer.Close().toString());
  }

  @Test
  public void testLong_PartOfArray() throws Exception {
    writer.Long(new long[]{0L,0x123456789ABCDEFFL,-1L,0L},1,2);
    assertEquals(".0x123456789ABCDEFF,0xFFFFFFFFFFFFFFFF", writer.Close().toString());
  }

  @Test
  public void testRadix() throws Exception {
    writer.SetValuePrefix("").Radix(16).Int(0x12345).Radix(2).Int(0x12345).Radix(10).Int(0x12345);
    assertEquals(".00012345,00000000000000010010001101000101,0000074565",writer.Close().toString());
  }
  
  @Test
  public void testTab() throws Exception {
    writer.setTabSpaces(8).Tab().Byte(1).Tab().Byte(1).Tab().Long(-1).Tab().write('A');
    assertEquals("        .0x01   ,0x01   ,0xFFFFFFFFFFFFFFFF     A",writer.Close().toString());
  }
  
  @Test
  public void testPrintSpecialChars() throws Exception {
    writer.setTabSpaces(4).write('\t');
    writer.write('\r');
    writer.Byte(1);
    writer.write('\n');
    assertEquals("    .0x01\n",writer.Close().toString());
  }
}
