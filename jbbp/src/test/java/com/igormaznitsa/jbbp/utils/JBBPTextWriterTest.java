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

package com.igormaznitsa.jbbp.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.it.AbstractParserIntegrationTest;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import com.igormaznitsa.jbbp.utils.JBBPTextWriter.Extra;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class JBBPTextWriterTest extends AbstractParserIntegrationTest {

  private static JBBPTextWriter makeWriter() {
    return new JBBPTextWriter(new StringWriter(), JBBPByteOrder.BIG_ENDIAN, "\n", 16, "0x", ".",
        ";", "~", ",");
  }

  @Test
  public void testMakeStrWriter() throws Exception {
    final String generated =
        JBBPTextWriter.makeStrWriter().Int(12).Byte(34).BR().Comment("Huzzaaa").Close().toString();
    assertEquals(String.format(".0x0000000C,0x22%n;Huzzaaa"), generated);
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
    assertEquals(";Hello World", makeWriter().Comment("Hello World").Close().toString());
  }

  @Test
  public void testMultilineCommentHelloWorld() throws Exception {
    assertEquals(";Hello\n;World", makeWriter().Comment("Hello\nWorld").Close().toString());
  }

  @Test
  public void testMultilineCommentAfterValue() throws Exception {
    assertEquals(".0x12345678;Hello\n           ;World",
        makeWriter().Int(0x12345678).Comment("Hello\nWorld").Close().toString());
  }

  @Test
  public void testCommentAndValue() throws Exception {
    assertEquals(";Hello World\n.0x01,0x00000001,0x0000000000000001",
        makeWriter().Comment("Hello World").Byte(1).Int(1).Long(1).Close().toString());
  }

  @Test
  public void testComment_DisableEnable() throws Exception {
    assertEquals(";Hrum\n.0x01,0x00000001,0x0000000000000001",
        makeWriter().DisableComments().Comment("Hello World").EnableComments().Comment("Hrum")
            .Byte(1).Int(1).Long(1).Close().toString());
  }

  @Test
  public void testDouble_Max_radix10() throws Exception {
    assertEquals(".1.7976931348623157E308",
        makeWriter().SetValuePrefix("").Radix(10).Double(Double.MAX_VALUE).Close().toString());
  }

  @Test
  public void testDouble_Max_radix16() throws Exception {
    assertEquals(".1.FFFFFFFFFFFFFP1023",
        makeWriter().SetValuePrefix("").Radix(16).Double(Double.MAX_VALUE).Close().toString());
  }

  @Test
  public void testFloat_Max_radix10() throws Exception {
    assertEquals(".3.4028234663852886E38",
        makeWriter().SetValuePrefix("").Radix(10).Float(Float.MAX_VALUE).Close().toString());
  }

  @Test
  public void testFloat_Min_radix10() throws Exception {
    assertEquals(".-1.401298464324817E-45",
        makeWriter().SetValuePrefix("").Radix(10).Float(-Float.MIN_VALUE).Close().toString());
  }

  @Test
  public void testFloat_Max_radix16() throws Exception {
    assertEquals(".1.FFFFFEP127",
        makeWriter().SetValuePrefix("").Radix(16).Float(Float.MAX_VALUE).Close().toString());
  }

  @Test
  public void testFloat_Min_radix16() throws Exception {
    assertEquals(".-1.0P-149",
        makeWriter().SetValuePrefix("").Radix(16).Float(-Float.MIN_VALUE).Close().toString());
  }

  @Test
  public void testValueAndMultilineComment() throws Exception {
    final String text = makeWriter()
        .Comment("Comment1", "Comment2")
        .IndentInc()
        .Int(1)
        .Comment("It's header")
        .HR()
        .IndentDec()
        .Comment("It's body")
        .IndentInc()
        .Byte(new byte[] {1, 2, 3, 4})
        .Comment("Body", "Next comment", "One more comment")
        .BR().BR()
        .Byte(new byte[] {0x0A, 0x0B})
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

    assertFileContent("testwriter.txt", text);
  }

  @Test
  public void testExtras_ErrorForNull() throws Exception {
    assertThrows(NullPointerException.class, () -> makeWriter().AddExtras((Extra[]) null));
  }

  @Test
  public void testExtras_ErrorForEmptyExtras() throws Exception {
    assertThrows(IllegalStateException.class, () -> makeWriter().Obj(0, new Object()));
  }

  @Test
  public void testExtras_NotPrintedForNull() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.AddExtras(new JBBPTextWriterExtraAdapter() {
      @Override
      public String doConvertObjToStr(JBBPTextWriter context, int id, Object obj)
          throws IOException {
        return null;
      }
    }).Obj(1, new Object());

    assertEquals("", writer.Close().toString());
  }

  @Test
  public void testMultilineComments() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.Byte(1).Comment("Hello", "World");
    assertEquals(".0x01;Hello\n     ;World", writer.Close().toString());
  }

  @Test
  public void testHorizontalRule() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.SetHR("~", 10, '>').HR().Byte(1);
    assertEquals("~>>>>>>>>>>\n.0x01", writer.Close().toString());
  }

  @Test
  public void testHorizontalRule_DisableEnable() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.SetHR("~", 10, '>').DisableComments().HR().Byte(1).EnableComments().HR();
    assertEquals("\n.0x01\n~>>>>>>>>>>\n", writer.Close().toString());
  }

  @Test
  public void testLineBreak() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.Byte(1).BR().Byte(2);
    assertEquals(".0x01\n.0x02", writer.Close().toString());
  }

  @Test
  public void testExtras_PrintInfoAboutComplexObjectIntoWriter() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.SetMaxValuesPerLine(16).AddExtras(new JBBPTextWriterExtraAdapter() {
      @Override
      public String doConvertObjToStr(final JBBPTextWriter context, final int id, final Object obj)
          throws IOException {
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
    assertFileContent("testwriter3.txt", text);
  }

  @Test
  public void testStringNumerationWithExtras() throws Exception {
    final JBBPTextWriter writer = makeWriter();

    final AtomicInteger newLineCounter = new AtomicInteger(0);
    final AtomicInteger bytePrintCounter = new AtomicInteger(0);
    final AtomicInteger closeCounter = new AtomicInteger(0);

    writer.SetMaxValuesPerLine(32).SetCommentPrefix(" // ")
        .AddExtras(new JBBPTextWriterExtraAdapter() {
          @Override
          public void onClose(final JBBPTextWriter context) throws IOException {
            context.Comment("The Last Line");
            closeCounter.incrementAndGet();
          }

          @Override
          public void onNewLine(final JBBPTextWriter context, final int lineNumber)
              throws IOException {
            newLineCounter.incrementAndGet();
          }

          @Override
          public void onBeforeFirstValue(final JBBPTextWriter context) throws IOException {
            context.write(
                JBBPUtils.ensureMinTextLength(Integer.toString(context.getLine()), 8, '0', 0) +
                    ' ');
          }

          @Override
          public String doConvertByteToStr(final JBBPTextWriter context, final int value)
              throws IOException {
            bytePrintCounter.incrementAndGet();
            return null;
          }

          @Override
          public void onReachedMaxValueNumberForLine(final JBBPTextWriter context)
              throws IOException {
            context.Comment("End of line");
          }
        });

    for (int i = 0; i < 130; i++) {
      writer.Byte(i);
    }
    assertFileContent("testwriter2.txt", writer.Close().toString());
    assertEquals(4, newLineCounter.get());
    assertEquals(130, bytePrintCounter.get());
    assertEquals(1, closeCounter.get());
  }

  @Test
  public void testByteOrder() throws Exception {
    final JBBPTextWriter writer = makeWriter();

    writer.ByteOrder(JBBPByteOrder.LITTLE_ENDIAN);
    assertEquals(JBBPByteOrder.LITTLE_ENDIAN, writer.getByteOrder());
    writer.ByteOrder(JBBPByteOrder.BIG_ENDIAN);
    assertEquals(JBBPByteOrder.BIG_ENDIAN, writer.getByteOrder());
  }

  @Test
  public void testObj_NoExtras() throws Exception {
    assertThrows(IllegalStateException.class, () -> makeWriter().Obj(123, "Str1", "Str2", "Str3"));
  }

  @Test
  public void testObj_ExtrasReturnNull() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.SetValuePrefix("").AddExtras(new JBBPTextWriterExtraAdapter() {
    });
    writer.Obj(123, "Str1", "Str2", "Str3");
    assertEquals("", writer.Close().toString());
  }

  @Test
  public void testObj_PrintIntervalFromArray() throws Exception {
    final JBBPTextWriter writer = makeWriter();

    writer.AddExtras(new JBBPTextWriterExtraAdapter() {

      @Override
      public String doConvertObjToStr(JBBPTextWriter context, int id, Object obj)
          throws IOException {
        assertEquals(1234, id);
        assertNotNull(obj);
        assertSame(writer, context);
        return obj.toString();
      }
    });

    assertEquals(".0xHello,0xWorld,0xHurraaa",
        writer.Obj(1234, new Object[] {1, 2, "Hello", "World", "Hurraaa", 3}, 2, 3).Close()
            .toString());
  }

  @Test
  public void testObj_ExtrasReturnValue() throws Exception {
    final JBBPTextWriter writer = makeWriter();

    writer.SetValuePrefix("").AddExtras(new JBBPTextWriterExtraAdapter() {

      @Override
      public String doConvertObjToStr(JBBPTextWriter context, int id, Object obj)
          throws IOException {
        return obj.toString();
      }

    });
    writer.Obj(123, "Str1", "Str2", "Str3");
    assertEquals(".Str1,Str2,Str3", writer.Close().toString());
  }

  @Test
  public void testByte_OneValue() throws Exception {
    final JBBPTextWriter writer = makeWriter();

    writer.Byte(10);
    writer.Byte(-1);
    assertEquals(".0x0A,0xFF", writer.Close().toString());
  }

  @Test
  public void testByte_OneValueAfterComment() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.Comment("Hello");
    writer.Byte(-1);
    assertEquals(";Hello\n.0xFF", writer.Close().toString());
  }

  @Test
  public void testByte_Array() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.Byte(new byte[] {(byte) 10, (byte) -1});
    assertEquals(".0x0A,0xFF", writer.Close().toString());
  }

  @Test
  public void testByte_PartOfArray() throws Exception {
    final JBBPTextWriter writer = makeWriter();

    writer.Byte(new byte[] {0, (byte) 10, (byte) -1, 0}, 1, 2);
    assertEquals(".0x0A,0xFF", writer.Close().toString());
  }

  @Test
  public void testByte_String() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.Byte("012345");
    assertEquals(".0x30,0x31,0x32,0x33,0x34,0x35", writer.Close().toString());
  }

  @Test
  public void testShort_OneValue() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.Short(10);
    writer.Short(-1);
    assertEquals(".0x000A,0xFFFF", writer.Close().toString());
  }

  @Test
  public void testShort_Array() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.Short(new short[] {(short) 0x1234, (short) -1});
    assertEquals(".0x1234,0xFFFF", writer.Close().toString());
  }

  @Test
  public void testShort_Array_InversedByteOrder() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).Short(new short[] {(short) 0x1234, (short) -1});
    assertEquals(".0x3412,0xFFFF", writer.Close().toString());
  }

  @Test
  public void testShort_PartOfArray() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.Short(new short[] {0, (short) 0x1234, (short) -1, 0}, 1, 2);
    assertEquals(".0x1234,0xFFFF", writer.Close().toString());
  }

  @Test
  public void testShort_String() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.Short("012345");
    assertEquals(".0x0030,0x0031,0x0032,0x0033,0x0034,0x0035", writer.Close().toString());
  }

  @Test
  public void testInt_OneValue() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.Int(0x12345678);
    writer.Int(-1);
    assertEquals(".0x12345678,0xFFFFFFFF", writer.Close().toString());
  }

  @Test
  public void testUInt_OneValue() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.UInt(0x12345678);
    writer.UInt(-1);
    assertEquals(".0x12345678,0xFFFFFFFF", writer.Close().toString());
  }

  @Test
  public void testInt_Array() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.Int(new int[] {0x12345678, -1});
    assertEquals(".0x12345678,0xFFFFFFFF", writer.Close().toString());
  }

  @Test
  public void testUInt_Array() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.UInt(new int[] {0x12345678, -1});
    assertEquals(".0x12345678,0xFFFFFFFF", writer.Close().toString());
  }

  @Test
  public void testInt_Array_InversedByteOrder() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).Int(new int[] {0x12345678, -1});
    assertEquals(".0x78563412,0xFFFFFFFF", writer.Close().toString());
  }

  @Test
  public void testUInt_Array_InversedByteOrder() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).UInt(new int[] {0x12345678, -1});
    assertEquals(".0x78563412,0xFFFFFFFF", writer.Close().toString());
  }

  @Test
  public void testInt_PartOfArray() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.Int(new int[] {0, 0x12345678, -1, 0}, 1, 2);
    assertEquals(".0x12345678,0xFFFFFFFF", writer.Close().toString());
  }

  @Test
  public void testUInt_PartOfArray() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.UInt(new int[] {0, 0x12345678, -1, 0}, 1, 2);
    assertEquals(".0x12345678,0xFFFFFFFF", writer.Close().toString());
  }

  @Test
  public void testLong_OneValue() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.Long(0x123456789ABCDEFFL);
    writer.Long(-1L);
    assertEquals(".0x123456789ABCDEFF,0xFFFFFFFFFFFFFFFF", writer.Close().toString());
  }

  @Test
  public void testLong_Array() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.Long(new long[] {0x123456789ABCDEFFL, -1L});
    assertEquals(".0x123456789ABCDEFF,0xFFFFFFFFFFFFFFFF", writer.Close().toString());
  }

  @Test
  public void testLong_Array_InversedByteOrder() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).Long(new long[] {0x123456789ABCDEFFL, -1L});
    assertEquals(".0xFFDEBC9A78563412,0xFFFFFFFFFFFFFFFF", writer.Close().toString());
  }

  @Test
  public void testLong_PartOfArray() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.Long(new long[] {0L, 0x123456789ABCDEFFL, -1L, 0L}, 1, 2);
    assertEquals(".0x123456789ABCDEFF,0xFFFFFFFFFFFFFFFF", writer.Close().toString());
  }

  @Test
  public void testRadix_ErrorForLessThan2() {
    assertThrows(IllegalArgumentException.class, () -> makeWriter().Radix(1));
  }

  @Test
  public void testRadix_ErrorForGreaterThan36() {
    assertThrows(IllegalArgumentException.class, () -> makeWriter().Radix(37));
  }

  @Test
  public void testRadix() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.Radix(2);
    assertEquals(2, writer.getRadix());
    writer.Radix(12);
    assertEquals(12, writer.getRadix());
    writer.SetValuePrefix("").Radix(16).Int(0x12345).Radix(2).Int(0x12345).Radix(10).Int(0x12345);
    assertEquals(".00012345,00000000000000010010001101000101,0000074565",
        writer.Close().toString());
  }

  @Test
  public void testGetLineSeparator() throws Exception {
    assertEquals("hello",
        new JBBPTextWriter(makeWriter(), JBBPByteOrder.BIG_ENDIAN, "hello", 11, "", "", "", "", "")
            .getLineSeparator());
  }

  @Test
  public void testAddDellExtras() throws Exception {
    final JBBPTextWriter writer = makeWriter();

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

  @Test
  public void testStr_ErrorForNull() throws Exception {
    assertThrows(NullPointerException.class, () -> makeWriter().Str((String[]) null));
  }

  @Test
  public void testStr() throws Exception {
    assertEquals(".0x01,Hello,World,<NULL>,0x02",
        makeWriter().Byte(1).Str("Hello", "World", null).Byte(2).Close().toString());
  }

  @Test
  public void testPrintSpecialChars() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.SetTabSpaces(4).write('\t');
    writer.write('\r');
    writer.Byte(1);
    writer.write('\n');
    assertEquals("    .0x01\n", writer.Close().toString());
  }

  @Test
  public void testStates() throws Exception {
    final JBBPTextWriter writer = makeWriter();
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
    final JBBPTextWriter writer = makeWriter();
    writer.write(new char[] {'a', 'b'});
    writer.flush();
    writer.close();
    assertEquals("ab", writer.toString());
  }

  @Test
  public void testGetLinePosition() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    assertEquals(0, writer.getLinePosition());
    writer.write("123");
    assertEquals(3, writer.getLinePosition());
    writer.write("111\n");
    assertEquals(0, writer.getLinePosition());
  }

  @Test
  public void testAppend() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.append("123");
    writer.append('4');
    writer.append("a56b", 1, 3);
    writer.append(null);
    assertEquals("123456null", writer.Close().toString());
  }

  @Test
  public void testSetValuePrefixPostfix() throws Exception {
    assertEquals(".0x01,$02^",
        makeWriter().Byte(1).SetValuePrefix("$").SetValuePostfix("^").Byte(2).Close().toString());
  }

  @Test
  public void testSetTabSpaces() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.SetTabSpaces(3).Tab().BR().IndentInc(3).Byte(1).IndentDec(2).BR().Comment("Hello");
    assertEquals("   \n         .0x01\n   ;Hello", writer.Close().toString());
  }

  @Test
  public void testSetTabSpaces_ErrorForNegative() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> makeWriter().SetTabSpaces(-1));
  }

  @Test
  public void testSetTabSpaces_ErrorForZero() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> makeWriter().SetTabSpaces(0));
  }

  @Test
  public void testPrintNumericValueByExtras() throws Exception {
    final JBBPTextWriter writer = makeWriter();
    writer.AddExtras(new JBBPTextWriterExtraAdapter() {
      @Override
      public String doConvertObjToStr(JBBPTextWriter context, int id, Object obj)
          throws IOException {
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
      public String doConvertFloatToStr(JBBPTextWriter context, float value) throws IOException {
        return "float" + value;
      }

      @Override
      public String doConvertDoubleToStr(JBBPTextWriter context, double value) throws IOException {
        return "double" + value;
      }

      @Override
      public String doConvertByteToStr(JBBPTextWriter context, int value) throws IOException {
        return "byte" + value;
      }

    });

    writer.SetValuePrefix("").Byte(1).Short(2).Int(3).Long(4).Obj(234, "Str").Float(Float.MIN_VALUE)
        .Double(Double.MAX_VALUE);

    assertEquals(".byte1,short2,int3,long4,objStr,float1.4E-45,double1.7976931348623157E308",
        writer.Close().toString());
  }

  @Test
  public void testBin_EasyCase() throws Exception {
    @Bin(name = "some class")
    class SomeClass {

      @Bin(order = 1)
      byte a;
      @Bin(order = 2, comment = "Short field")
      short b;
      @Bin(order = 3)
      int c;
      @Bin(order = 4, comment = "Long field")
      long d;
      @Bin(order = 5, comment = "some array")
      byte[] arr = new byte[128];
      @Bin(order = 6, comment = "some string")
      String str = "Hello String";
      @Bin(order = 7, comment = "some string array")
      String[] strs = new String[] {"Hello", null, "World"};
    }

    final SomeClass cl = new SomeClass();
    cl.a = 1;
    cl.b = 2;
    cl.c = 3;
    cl.d = 4;

    final JBBPTextWriter writer = makeWriter();
    writer.SetMaxValuesPerLine(16);

    final String text = writer.SetCommentPrefix("; ").Bin(cl).Close().toString();
    System.out.println(text);
    assertFileContent("testwriterbin1.txt", text);
  }

  @Test
  public void testBin_ParsedPng() throws Exception {
    final JBBPTextWriter writer = makeWriter();

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

        @Bin(order = 1)
        int Length;
        @Bin(order = 2)
        int Type;
        @Bin(order = 3)
        byte[] Data;
        @Bin(order = 4)
        int CRC;
      }

      class Png {

        @Bin(order = 1)
        long Header;
        @Bin(order = 2)
        Chunk[] Chunks;
      }

      final Png png = pngParser.parse(pngStream).mapTo(new Png(), aClass -> {
        if (aClass == Chunk.class) {
          return new Chunk();
        }
        return null;
      });

      final String text = writer.SetMaxValuesPerLine(16).Bin(png).Close().toString();

      System.out.println(text);

      assertFileContent("testwriterbin2.txt", text);
    } finally {
      JBBPUtils.closeQuietly(pngStream);
    }
  }

  @Test
  public void testBin_ParsedDoubleFloat() throws Exception {
    final JBBPTextWriter writer = makeWriter();

    final InputStream pngStream = getResourceAsInputStream("picture.png");
    final JBBPParser parser =
        JBBPParser.prepare("floatj f; doublej d; floatj [2] fa; doublej [2] da;");

    class Klazz {

      @Bin
      float f;
      @Bin
      float[] fa;
      @Bin
      double d;
      @Bin
      double[] da;
    }

    final byte[] data = new byte[4 + 8 + 4 * 2 + 8 * 2];
    new Random(111222).nextBytes(data);

    final Klazz parsed = parser.parse(data).mapTo(new Klazz());

    final String text = writer.SetMaxValuesPerLine(16).Bin(parsed).Close().toString();

    System.out.println(text);
    assertFileContent("testwriterbinfloatdouble.txt", text);
  }

  @Test
  public void testBin_ParsedPng_NonMappedRawStruct() throws Exception {
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

      final String text =
          makeWriter().SetMaxValuesPerLine(16).Bin(pngParser.parse(pngStream)).Close().toString();
      System.out.println(text);
      assertFileContent("testwriterbin2b.txt", text);
    } finally {
      JBBPUtils.closeQuietly(pngStream);
    }
  }

  @Test
  public void testBin_AllEasyTypes_NonMappedRawStruct() throws Exception {
    final JBBPParser parser = JBBPParser
        .prepare("bit:2 a1; bit:6 a2; byte a; ubyte b; short c; ushort d; int e; long f; bool g;");
    final byte[] testArray =
        new byte[] {(byte) 0xDE, (byte) 0x12, (byte) 0xFE, (byte) 0x23, (byte) 0x11, (byte) 0x45,
            (byte) 0xDA, (byte) 0x82, (byte) 0xA0, (byte) 0x33, (byte) 0x7F, (byte) 0x99,
            (byte) 0x04, (byte) 0x10, (byte) 0x45, (byte) 0xBD, (byte) 0xCA, (byte) 0xFE,
            (byte) 0x12, (byte) 0x11, (byte) 0xBA, (byte) 0xBE};

    final String text =
        makeWriter().SetMaxValuesPerLine(16).Bin(parser.parse(testArray)).Close().toString();
    System.out.println(text);
    assertFileContent("txtwrtrjbbpobj1.txt", text);
  }

  @Test
  public void testBin_ValField() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("val:123 a;");
    final String text =
        makeWriter().SetMaxValuesPerLine(16).Bin(parser.parse(new byte[0])).Close().toString();
    assertEquals(
        "~--------------------------------------------------------------------------------\n" +
            "; Start {} \n" +
            "~--------------------------------------------------------------------------------\n" +
            "    .0x0000007B; int a\n" +
            "~--------------------------------------------------------------------------------\n" +
            "; End {} \n" +
            "~--------------------------------------------------------------------------------\n",
        text);
  }

  @Test
  public void testBin_AllEasyTypes_Anonymous_NonMappedRawStruct() throws Exception {
    final JBBPParser parser =
        JBBPParser.prepare("bit:2; bit:6; byte; ubyte; short; ushort; int; long; bool; stringj;");
    final byte[] testArray = new byte[] {
        (byte) 0xDE, (byte) 0x12, (byte) 0xFE, (byte) 0x23, (byte) 0x11,
        (byte) 0x45, (byte) 0xDA, (byte) 0x82, (byte) 0xA0, (byte) 0x33,
        (byte) 0x7F, (byte) 0x99, (byte) 0x04, (byte) 0x10, (byte) 0x45,
        (byte) 0xBD, (byte) 0xCA, (byte) 0xFE, (byte) 0x12, (byte) 0x11,
        3, 65, 66, 67
    };

    final String text =
        makeWriter().SetMaxValuesPerLine(16).Bin(parser.parse(testArray)).Close().toString();
    System.out.println(text);
    assertFileContent("txtwrtrjbbpobj2.txt", text);
  }

  @Test
  public void testBin_StringFieldAndStringArray() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("stringj[2];stringj;");
    final byte[] testArray = new byte[] {
        3, 65, 66, 67,
        3, 68, 69, 70,
        3, 71, 72, 73
    };

    final String text =
        makeWriter().SetMaxValuesPerLine(16).Bin(parser.parse(testArray)).Close().toString();
    System.out.println(text);
    assertFileContent("txtwrtrjbbpobj3.txt", text);
  }

  @Test
  public void testBin_BooleanArray_NonMappedRawStruct() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("bool [_] array;");
    final byte[] testArray =
        new byte[] {(byte) 0xDE, (byte) 0x00, (byte) 0xFE, (byte) 0x00, (byte) 0x11, (byte) 0x45,
            (byte) 0xDA, (byte) 0x82, (byte) 0xA0, (byte) 0x33, (byte) 0x7F, (byte) 0x99,
            (byte) 0x04, (byte) 0x10, (byte) 0x45, (byte) 0xBD, (byte) 0xCA, (byte) 0xFE,
            (byte) 0x12, (byte) 0x11, (byte) 0x00, (byte) 0xBE};

    final String text =
        makeWriter().SetMaxValuesPerLine(16).Bin(parser.parse(testArray)).Close().toString();
    System.out.println(text);
    assertFileContent("boolarrayraw.txt", text);
  }

  @Test
  public void testBin_ByteArrayMappedToString() throws Exception {
    class Parsed {
      @Bin(type = BinType.BYTE_ARRAY)
      String str1;
      @Bin(type = BinType.UBYTE_ARRAY)
      String str2;
    }

    final Parsed parsed = JBBPParser.prepare("byte [5] str1; ubyte [4] str2;")
        .parse(new byte[] {49, 50, 51, 52, 53, 54, 55, 56, 57}).mapTo(new Parsed());
    final String text = makeWriter().Bin(parsed).Close().toString();

    System.out.println(text);

    assertFileContent("testwriterbin5.txt", text);
  }

  @Test
  public void testCustomFieldInMappedClass() throws Exception {
    class TestClass {

      @Bin(order = 1)
      int a;
      @Bin(order = 2, custom = true)
      int b;
    }

    final JBBPTextWriter writer = makeWriter();

    writer.AddExtras(new JBBPTextWriterExtraAdapter() {

      @Override
      public String doConvertObjToStr(JBBPTextWriter context, int id, Object obj)
          throws IOException {
        fail("Must not be called");
        return null;
      }

      @Override
      public String doConvertCustomField(final JBBPTextWriter context, final Object obj,
                                         final Field field, final Bin annotation)
          throws IOException {
        return "test" + field.getName();
      }

    });

    final String text =
        writer.SetHR("~", 3, '-').SetValuePrefix("").Bin(new TestClass()).Close().toString();
    System.out.println(text);
    assertFileContent("testwriterbin3.txt", text);
  }

  @Test
  public void testCustomArrayFieldInMappedClass() throws Exception {
    class TestClass {

      @Bin(order = 1)
      int a;
      @Bin(order = 2, custom = true)
      int[] b = new int[3];
    }

    final JBBPTextWriter writer = makeWriter();

    writer.AddExtras(new JBBPTextWriterExtraAdapter() {

      @Override
      public String doConvertObjToStr(JBBPTextWriter context, int id, Object obj)
          throws IOException {
        fail("Must not be called");
        return null;
      }

      @Override
      public String doConvertCustomField(JBBPTextWriter context, Object obj, Field field,
                                         Bin annotation) throws IOException {
        context.HR().Str(field.getType().isArray() ? "See on array" : "Error")
            .Comment("Line one", "Line two").HR();
        return null;
      }

    });

    final String text =
        writer.SetHR("~", 3, '-').SetValuePrefix("").Bin(new TestClass()).Close().toString();
    System.out.println(text);
    assertFileContent("testwriterbin4.txt", text);
  }
}
