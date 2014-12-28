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

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.it.AbstractParserIntegrationTest;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.utils.JBBPTextWriter.Extra;
import java.io.*;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

public class JBBPTextWriterTest extends AbstractParserIntegrationTest {

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
  public void testConstructor_Default() {
    final JBBPTextWriter writer = new JBBPTextWriter();
    assertTrue(writer.getWrappedWriter() instanceof StringWriter);
    assertEquals(JBBPByteOrder.BIG_ENDIAN, writer.getByteOrder());
  }

  @Test
  public void testConstructor_OnlyWriter() {
    final Writer someWriter = new StringWriter();
    final JBBPTextWriter writer = new JBBPTextWriter(someWriter);
    assertSame(someWriter, writer.getWrappedWriter());
    assertEquals(JBBPByteOrder.BIG_ENDIAN, writer.getByteOrder());
  }

  @Test
  public void testConstructor_WriterAndByteOrder() {
    final Writer someWriter = new StringWriter();
    final JBBPTextWriter writer = new JBBPTextWriter(someWriter, JBBPByteOrder.LITTLE_ENDIAN);
    assertSame(someWriter, writer.getWrappedWriter());
    assertEquals(JBBPByteOrder.LITTLE_ENDIAN, writer.getByteOrder());
  }

  @Test
  public void testCommentHelloWorld() throws Exception {
    assertEquals(";Hello World", writer.Comment("Hello World").Close().toString());
  }

  @Test
  public void testMultilineCommentHelloWorld() throws Exception {
    assertEquals(";Hello\n;World", writer.Comment("Hello\nWorld").Close().toString());
  }

  @Test
  public void testMultilineCommentAfterValue() throws Exception {
    assertEquals(".0x12345678;Hello\n           ;World", writer.Int(0x12345678).Comment("Hello\nWorld").Close().toString());
  }

  @Test
  public void testCommentAndValue() throws Exception {
    assertEquals(";Hello World\n.0x01,0x00000001,0x0000000000000001", writer.Comment("Hello World").Byte(1).Int(1).Long(1).Close().toString());
  }

  @Test
  public void testComment_DisableEnable() throws Exception {
    assertEquals(";Hrum\n.0x01,0x00000001,0x0000000000000001", writer.DisableComments().Comment("Hello World").EnableComments().Comment("Hrum").Byte(1).Int(1).Long(1).Close().toString());
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
            .BR().BR()
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

    System.out.println(text);

    assertFile("testwriter.txt", text);
  }

  @Test(expected = NullPointerException.class)
  public void testExtras_ErrorForNull() throws Exception {
    writer.AddExtras((Extra[]) null);
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
    assertEquals(".0x01;Hello\n     ;World", writer.Close().toString());
  }

  @Test
  public void testHorizontalRule() throws Exception {
    writer.SetHR(10, '>').HR().Byte(1);
    assertEquals(";>>>>>>>>>>\n.0x01", writer.Close().toString());
  }

  @Test
  public void testHorizontalRule_DisableEnable() throws Exception {
    writer.SetHR(10, '>').DisableComments().HR().Byte(1).EnableComments().HR();
    assertEquals("\n.0x01\n;>>>>>>>>>>\n", writer.Close().toString());
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

    final String text = writer.Byte(0xFF).Obj(111, "Hello").Int(0xCAFEBABE).Close().toString();
    System.out.println(text);
    assertFile("testwriter3.txt", text);
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
    assertEquals(4, newLineCounter.get());
    assertEquals(130, bytePrintCounter.get());
    assertEquals(1, closeCounter.get());
  }

  @Test
  public void testByteOrder() throws Exception {
    writer.ByteOrder(JBBPByteOrder.LITTLE_ENDIAN);
    assertEquals(JBBPByteOrder.LITTLE_ENDIAN, writer.getByteOrder());
    writer.ByteOrder(JBBPByteOrder.BIG_ENDIAN);
    assertEquals(JBBPByteOrder.BIG_ENDIAN, writer.getByteOrder());
  }

  @Test(expected = IllegalStateException.class)
  public void testObj_NoExtras() throws Exception {
    writer.Obj(123, "Str1", "Str2", "Str3");
  }

  @Test
  public void testObj_ExtrasReturnNull() throws Exception {
    writer.SetValuePrefix("").AddExtras(new JBBPTextWriterExtraAdapter() {
    });
    writer.Obj(123, "Str1", "Str2", "Str3");
    assertEquals("", writer.Close().toString());
  }

  @Test
  public void testObj_PrintIntervalFromArray() throws Exception {
    writer.AddExtras(new JBBPTextWriterExtraAdapter() {

      @Override
      public String doConvertObjToStr(JBBPTextWriter context, int id, Object obj) throws IOException {
        assertEquals(1234, id);
        assertNotNull(obj);
        assertSame(writer, context);
        return obj.toString();
      }
    });

    assertEquals(".0xHello,0xWorld,0xHurraaa", writer.Obj(1234, new Object[]{1, 2, "Hello", "World", "Hurraaa", 3}, 2, 3).Close().toString());
  }

  @Test
  public void testObj_ExtrasReturnValue() throws Exception {
    writer.SetValuePrefix("").AddExtras(new JBBPTextWriterExtraAdapter() {

      @Override
      public String doConvertObjToStr(JBBPTextWriter context, int id, Object obj) throws IOException {
        return obj.toString();
      }

    });
    writer.Obj(123, "Str1", "Str2", "Str3");
    assertEquals(".Str1,Str2,Str3", writer.Close().toString());
  }

  @Test
  public void testByte_OneValue() throws Exception {
    writer.Byte(10);
    writer.Byte(-1);
    assertEquals(".0x0A,0xFF", writer.Close().toString());
  }

  @Test
  public void testByte_OneValueAfterComment() throws Exception {
    writer.Comment("Hello");
    writer.Byte(-1);
    assertEquals(";Hello\n.0xFF", writer.Close().toString());
  }

  @Test
  public void testByte_Array() throws Exception {
    writer.Byte(new byte[]{(byte) 10, (byte) -1});
    assertEquals(".0x0A,0xFF", writer.Close().toString());
  }

  @Test
  public void testByte_PartOfArray() throws Exception {
    writer.Byte(new byte[]{0, (byte) 10, (byte) -1, 0}, 1, 2);
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
    writer.Short(new short[]{(short) 0x1234, (short) -1});
    assertEquals(".0x1234,0xFFFF", writer.Close().toString());
  }

  @Test
  public void testShort_Array_InversedByteOrder() throws Exception {
    writer.ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).Short(new short[]{(short) 0x1234, (short) -1});
    assertEquals(".0x3412,0xFFFF", writer.Close().toString());
  }

  @Test
  public void testShort_PartOfArray() throws Exception {
    writer.Short(new short[]{0, (short) 0x1234, (short) -1, 0}, 1, 2);
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
    writer.Int(new int[]{0x12345678, -1});
    assertEquals(".0x12345678,0xFFFFFFFF", writer.Close().toString());
  }

  @Test
  public void testInt_Array_InversedByteOrder() throws Exception {
    writer.ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).Int(new int[]{0x12345678, -1});
    assertEquals(".0x78563412,0xFFFFFFFF", writer.Close().toString());
  }

  @Test
  public void testInt_PartOfArray() throws Exception {
    writer.Int(new int[]{0, 0x12345678, -1, 0}, 1, 2);
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
    writer.Long(new long[]{0x123456789ABCDEFFL, -1L});
    assertEquals(".0x123456789ABCDEFF,0xFFFFFFFFFFFFFFFF", writer.Close().toString());
  }

  @Test
  public void testLong_Array_InversedByteOrder() throws Exception {
    writer.ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).Long(new long[]{0x123456789ABCDEFFL, -1L});
    assertEquals(".0xFFDEBC9A78563412,0xFFFFFFFFFFFFFFFF", writer.Close().toString());
  }

  @Test
  public void testLong_PartOfArray() throws Exception {
    writer.Long(new long[]{0L, 0x123456789ABCDEFFL, -1L, 0L}, 1, 2);
    assertEquals(".0x123456789ABCDEFF,0xFFFFFFFFFFFFFFFF", writer.Close().toString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRadix_ErrorForLessThan2() {
    writer.Radix(1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRadix_ErrorForGreaterThan36() {
    writer.Radix(37);
  }

  @Test
  public void testRadix() throws Exception {
    writer.Radix(2);
    assertEquals(2, writer.getRadix());
    writer.Radix(12);
    assertEquals(12, writer.getRadix());
    writer.SetValuePrefix("").Radix(16).Int(0x12345).Radix(2).Int(0x12345).Radix(10).Int(0x12345);
    assertEquals(".00012345,00000000000000010010001101000101,0000074565", writer.Close().toString());
  }

  @Test
  public void testGetLineSeparator() throws Exception {
    assertEquals("hello", new JBBPTextWriter(writer, JBBPByteOrder.BIG_ENDIAN, "hello", 11, "", "", "", "").getLineSeparator());
  }

  @Test
  public void testAddDellExtras() throws Exception {
    final JBBPTextWriterExtraAdapter extras1 = new JBBPTextWriterExtraAdapter() {

      @Override
      public String doConvertByteToStr(JBBPTextWriter context, int value) throws IOException {
        return "bbb" + value;
      }

    };

    final JBBPTextWriterExtraAdapter extras2 = new JBBPTextWriterExtraAdapter() {
      @Override
      public String doConvertByteToStr(JBBPTextWriter context, int value) throws IOException {
        return "aaa" + value;
      }
    };

    writer.AddExtras(extras1, extras2).SetValuePrefix("");

    writer.Byte(1);
    writer.DelExtras(extras2);
    writer.Byte(2);

    assertEquals(".aaa1,bbb2", writer.Close().toString());
  }

  @Test(expected = NullPointerException.class)
  public void testStr_ErrorForNull() throws Exception {
    writer.Str((String[])null);
  }

  @Test
  public void testStr() throws Exception {
    assertEquals(".0x01,Hello,World,<NULL>,0x02", writer.Byte(1).Str("Hello", "World", null).Byte(2).Close().toString());
  }

  @Test
  public void testPrintSpecialChars() throws Exception {
    writer.SetTabSpaces(4).write('\t');
    writer.write('\r');
    writer.Byte(1);
    writer.write('\n');
    assertEquals("    .0x01\n", writer.Close().toString());
  }

  @Test
  public void testStates() throws Exception {
    assertTrue(writer.isLineStart());
    assertFalse(writer.isComments());
    assertFalse(writer.isValues());

    writer.Byte(-1);

    assertFalse(writer.isLineStart());
    assertFalse(writer.isComments());
    assertTrue(writer.isValues());

    writer.Comment("Hello", "World");

    assertFalse(writer.isLineStart());
    assertTrue(writer.isComments());
    assertFalse(writer.isValues());

    writer.write(" state not changed");

    assertFalse(writer.isLineStart());
    assertTrue(writer.isComments());
    assertFalse(writer.isValues());

    writer.BR();

    assertTrue(writer.isLineStart());
    assertFalse(writer.isComments());
    assertFalse(writer.isValues());
  }

  @Test
  public void testFlushAndClose() throws Exception {
    writer.write(new char[]{'a', 'b'});
    writer.flush();
    writer.close();
    assertEquals("ab", writer.toString());
  }

  @Test
  public void testGetLinePosition() throws Exception {
    assertEquals(0, writer.getLinePosition());
    writer.write("123");
    assertEquals(3, writer.getLinePosition());
    writer.write("111\n");
    assertEquals(0, writer.getLinePosition());
  }

  @Test
  public void testAppend() throws Exception {
    writer.append("123");
    writer.append('4');
    writer.append("a56b", 1, 3);
    writer.append(null);
    assertEquals("123456null", writer.Close().toString());
  }

  @Test
  public void testSetTabSpaces() throws Exception {
    writer.SetTabSpaces(3).Tab().BR().IndentInc(3).Byte(1).IndentDec(2).BR().Comment("Hello");
    assertEquals("   \n         .0x01\n   ;Hello", writer.Close().toString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetTabSpaces_ErrorForNegative() throws Exception {
    writer.SetTabSpaces(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetTabSpaces_ErrorForZero() throws Exception {
    writer.SetTabSpaces(0);
  }

  @Test
  public void testPrintNumericValueByExtras() throws Exception {
    writer.AddExtras(new JBBPTextWriterExtraAdapter() {

      @Override
      public String doConvertObjToStr(JBBPTextWriter context, int id, Object obj) throws IOException {
        assertEquals(234, id);
        return "obj" + obj;
      }

      @Override
      public String doConvertLongToStr(JBBPTextWriter context, long value) throws IOException {
        return "long" + value;
      }

      @Override
      public String doConvertIntToStr(JBBPTextWriter context, int value) throws IOException {
        return "int" + value;
      }

      @Override
      public String doConvertShortToStr(JBBPTextWriter context, int value) throws IOException {
        return "short" + value;
      }

      @Override
      public String doConvertByteToStr(JBBPTextWriter context, int value) throws IOException {
        return "byte" + value;
      }

    });

    writer.SetValuePrefix("").Byte(1).Short(2).Int(3).Long(4).Obj(234, "Str");

    assertEquals(".byte1,short2,int3,long4,objStr", writer.Close().toString());
  }

  @Test
  public void testBin_EasyCase() throws Exception {
    @Bin(name = "some class")
    class SomeClass {

      @Bin(outOrder = 1)
      byte a;
      @Bin(outOrder = 2, comment = "Short field")
      short b;
      @Bin(outOrder = 3)
      int c;
      @Bin(outOrder = 4, comment = "Long field")
      long d;
      @Bin(outOrder = 5, comment = "some array")
      byte[] arr = new byte[128];
    }

    final SomeClass cl = new SomeClass();
    cl.a = 1;
    cl.b = 2;
    cl.c = 3;
    cl.d = 4;

    writer.SetMaxValuesPerLine(16);

    final String text = writer.SetCommentPrefix("; ").Bin(cl).Close().toString();
    System.out.println(text);
    assertFile("testwriterbin1.txt", text);;
  }

  @Test
  public void testBin_ParsedPng() throws Exception {
    final InputStream pngStream = getResourceAsInputStream("picture.png");
    try {

      final JBBPParser pngParser = JBBPParser.prepare(
              "long header;"
              + "// chunks\n"
              + "chunks [_]{"
              + "   int length; "
              + "   int type; "
              + "   byte[length] data; "
              + "   int crc;"
              + "}"
      );

      class Chunk {

        @Bin(outOrder = 1)
        int Length;
        @Bin(outOrder = 2)
        int Type;
        @Bin(outOrder = 3)
        byte[] Data;
        @Bin(outOrder = 4)
        int CRC;
      }

      class Png {

        @Bin(outOrder = 1)
        long Header;
        @Bin(outOrder = 2)
        Chunk[] Chunks;
      }

      final Png png = pngParser.parse(pngStream).mapTo(Png.class);

      final String text = writer.SetMaxValuesPerLine(16).Bin(png).Close().toString();

      System.out.println(text);

      assertFile("testwriterbin2.txt", text);;
    }
    finally {
      JBBPUtils.closeQuietly(pngStream);
    }
  }

  @Test
  public void testCustomFieldInMappedClass() throws Exception {
    class TestClass {

      @Bin(outOrder = 1)
      int a;
      @Bin(outOrder = 2, custom = true)
      int b;
    }

    writer.AddExtras(new JBBPTextWriterExtraAdapter() {

      @Override
      public String doConvertObjToStr(JBBPTextWriter context, int id, Object obj) throws IOException {
        fail("Must not be called");
        return null;
      }

      @Override
      public String doConvertCustomField(final JBBPTextWriter context, final Object obj, final Field field, final Bin annotation) throws IOException {
        return "test" + field.getName();
      }

    });

    final String text = writer.SetHR(3, '-').SetValuePrefix("").Bin(new TestClass()).Close().toString();
    System.out.println(text);
    assertFile("testwriterbin3.txt", text);
  }

  @Test
  public void testCustomArrayFieldInMappedClass() throws Exception {
    class TestClass {

      @Bin(outOrder = 1)
      int a;
      @Bin(outOrder = 2, custom = true)
      int[] b = new int[3];
    }

    writer.AddExtras(new JBBPTextWriterExtraAdapter() {

      @Override
      public String doConvertObjToStr(JBBPTextWriter context, int id, Object obj) throws IOException {
        fail("Must not be called");
        return null;
      }

      @Override
      public String doConvertCustomField(JBBPTextWriter context, Object obj, Field field, Bin annotation) throws IOException {
        context.HR().Str(field.getType().isArray() ? "See on array" : "Error").Comment("Line one", "Line two").HR();
        return null;
      }

    });

    final String text = writer.SetHR(3, '-').SetValuePrefix("").Bin(new TestClass()).Close().toString();
    System.out.println(text);
    assertFile("testwriterbin4.txt", text);
  }
}
