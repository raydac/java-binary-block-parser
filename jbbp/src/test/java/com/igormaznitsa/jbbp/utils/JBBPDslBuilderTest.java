package com.igormaznitsa.jbbp.utils;

import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static com.igormaznitsa.jbbp.utils.JBBPDslBuilder.Begin;
import static org.junit.jupiter.api.Assertions.*;

class JBBPDslBuilderTest {

  @Test
  public void testEmptyStringForEmptyContent() {
    assertEquals("", Begin().End());
    assertEquals("", Begin().End(false));
    assertEquals("", Begin().End(true));
  }

  @Test
  public void testNonFormatted() {
    assertEquals("bool test;{int field1;}", Begin().Bool("test").Struct().Int("field1").CloseStruct().End(false));
  }

  @Test
  public void testFormatted() {
    assertEquals("bool test;\n{\n\tint field1;\n}\n", Begin().Bool("test").Struct().Int("field1").CloseStruct().End(true));
  }

  @Test
  public void testType_Bit() {
    assertEquals("bit:1;", Begin().Bit().End());
    assertEquals("bit:1 abc;", Begin().Bit("abc").End());
  }

  @Test
  public void testType_Bits() {
    assertEquals("bit:4;", Begin().Bits(4).End());
    assertEquals("bit:3;", Begin().Bits(JBBPBitNumber.BITS_3).End());
    assertEquals("bit:3 huzzaa;", Begin().Bits("huzzaa", JBBPBitNumber.BITS_3).End());
    assertEquals("bit:3;", Begin().Bits(null, JBBPBitNumber.BITS_3).End());
    assertEquals("bit:(a+b);", Begin().Bits(null, "a+b").End());
    assertEquals("bit:(a+b) abc;", Begin().Bits("abc", "a+b").End());
  }

  @Test
  public void testType_Bool() {
    assertEquals("bool;", Begin().Bool().End());
    assertEquals("bool abc;", Begin().Bool("abc").End());
  }

  @Test
  public void testType_Byte() {
    assertEquals("byte;", Begin().Byte().End());
    assertEquals("byte abc;", Begin().Byte("abc").End());
  }

  @Test
  public void testType_UByte() {
    assertEquals("ubyte;", Begin().UByte().End());
    assertEquals("ubyte abc;", Begin().UByte("abc").End());
  }

  @Test
  public void testType_Short() {
    assertEquals("short;", Begin().Short().End());
    assertEquals("short abc;", Begin().Short("abc").End());
  }

  @Test
  public void testType_UShort() {
    assertEquals("ushort;", Begin().UShort().End());
    assertEquals("ushort abc;", Begin().UShort("abc").End());
  }

  @Test
  public void testType_Int() {
    assertEquals("int;", Begin().Int().End());
    assertEquals("int abc;", Begin().Int("abc").End());
  }

  @Test
  public void testType_Long() {
    assertEquals("long;", Begin().Long().End());
    assertEquals("long abc;", Begin().Long("abc").End());
  }

  @Test
  public void testType_Float() {
    assertEquals("floatj;", Begin().Float().End());
    assertEquals("floatj abc;", Begin().Float("abc").End());
  }

  @Test
  public void testType_Double() {
    assertEquals("doublej;", Begin().Double().End());
    assertEquals("doublej abc;", Begin().Double("abc").End());
  }

  @Test
  public void testType_String() {
    assertEquals("stringj;", Begin().String().End());
    assertEquals("stringj abc;", Begin().String("abc").End());
  }

  @Test
  public void testType_Skip() {
    assertEquals("skip:1;", Begin().Skip().End());
    assertEquals("skip:3;", Begin().Skip(3).End());
    assertEquals("skip:(a+b);", Begin().Skip("a+b").End());

    assertThrows(IllegalArgumentException.class, new Executable() {
      @Override
      public void execute() throws Throwable {
        Begin().Skip(-3);
      }
    });
  }

  @Test
  public void testType_Align() {
    assertEquals("align:1;", Begin().Align().End());
    assertEquals("align:3;", Begin().Align(3).End());
    assertEquals("align:(a+b);", Begin().Align("a+b").End());

    assertThrows(IllegalArgumentException.class, new Executable() {
      @Override
      public void execute() throws Throwable {
        Begin().Align(-3);
      }
    });
  }

  @Test
  public void testType_BitArray() {
    assertEquals("bit:3[_];", Begin().BitArray(JBBPBitNumber.BITS_3, -1).End());
    assertEquals("bit:3[23];", Begin().BitArray(JBBPBitNumber.BITS_3, 23).End());
    assertEquals("bit:3[23] some;", Begin().BitArray("some", JBBPBitNumber.BITS_3, 23).End());
    assertEquals("bit:3[a+b] some;", Begin().BitArray("some", JBBPBitNumber.BITS_3, "a+b").End());
    assertEquals("bit:(a+b)[a+b] some;", Begin().BitArray("some", "a+b", "a+b").End());
    assertEquals("bit:3[a+b];", Begin().BitArray(JBBPBitNumber.BITS_3, "a+b").End());
  }

  @Test
  public void testType_BoolArray() {
    assertEquals("bool[a+b];", Begin().BoolArray("a+b").End());
    assertEquals("bool[2334] abc;", Begin().BoolArray("abc", 2334).End());
    assertEquals("bool[_];", Begin().BoolArray(-5).End());
    assertEquals("bool[_] abc;", Begin().BoolArray("abc", -5).End());
    assertEquals("bool[a+b] abc;", Begin().BoolArray("abc", "a+b").End());
  }

  @Test
  public void testType_ByteArray() {
    assertEquals("byte[a+b];", Begin().ByteArray("a+b").End());
    assertEquals("byte[2334] abc;", Begin().ByteArray("abc", 2334).End());
    assertEquals("byte[_];", Begin().ByteArray(-5).End());
    assertEquals("byte[_] abc;", Begin().ByteArray("abc", -5).End());
    assertEquals("byte[a+b] abc;", Begin().ByteArray("abc", "a+b").End());
  }

  @Test
  public void testType_UByteArray() {
    assertEquals("ubyte[a+b];", Begin().UByteArray("a+b").End());
    assertEquals("ubyte[2334] abc;", Begin().UByteArray("abc", 2334).End());
    assertEquals("ubyte[_];", Begin().UByteArray(-5).End());
    assertEquals("ubyte[_] abc;", Begin().UByteArray("abc", -5).End());
    assertEquals("ubyte[a+b] abc;", Begin().UByteArray("abc", "a+b").End());
  }

  @Test
  public void testType_ShortArray() {
    assertEquals("short[a+b];", Begin().ShortArray("a+b").End());
    assertEquals("short[2334] abc;", Begin().ShortArray("abc", 2334).End());
    assertEquals("short[_];", Begin().ShortArray(-5).End());
    assertEquals("short[_] abc;", Begin().ShortArray("abc", -5).End());
    assertEquals("short[a+b] abc;", Begin().ShortArray("abc", "a+b").End());
  }

  @Test
  public void testType_UShortArray() {
    assertEquals("ushort[a+b];", Begin().UShortArray("a+b").End());
    assertEquals("ushort[2334] abc;", Begin().UShortArray("abc", 2334).End());
    assertEquals("ushort[_];", Begin().UShortArray(-5).End());
    assertEquals("ushort[_] abc;", Begin().UShortArray("abc", -5).End());
    assertEquals("ushort[a+b] abc;", Begin().UShortArray("abc", "a+b").End());
  }

  @Test
  public void testType_IntArray() {
    assertEquals("int[a+b];", Begin().IntArray("a+b").End());
    assertEquals("int[2334] abc;", Begin().IntArray("abc", 2334).End());
    assertEquals("int[_];", Begin().IntArray(-5).End());
    assertEquals("int[_] abc;", Begin().IntArray("abc", -5).End());
    assertEquals("int[a+b] abc;", Begin().IntArray("abc", "a+b").End());
  }

  @Test
  public void testType_LongArray() {
    assertEquals("long[a+b];", Begin().LongArray("a+b").End());
    assertEquals("long[2334] abc;", Begin().LongArray("abc", 2334).End());
    assertEquals("long[_];", Begin().LongArray(-5).End());
    assertEquals("long[_] abc;", Begin().LongArray("abc", -5).End());
    assertEquals("long[a+b] abc;", Begin().LongArray("abc", "a+b").End());
  }

  @Test
  public void testType_FloatArray() {
    assertEquals("floatj[a+b];", Begin().FloatArray("a+b").End());
    assertEquals("floatj[2334] abc;", Begin().FloatArray("abc", 2334).End());
    assertEquals("floatj[_];", Begin().FloatArray(-5).End());
    assertEquals("floatj[_] abc;", Begin().FloatArray("abc", -5).End());
    assertEquals("floatj[a+b] abc;", Begin().FloatArray("abc", "a+b").End());
  }

  @Test
  public void testType_DoubleArray() {
    assertEquals("doublej[a+b];", Begin().DoubleArray("a+b").End());
    assertEquals("doublej[2334] abc;", Begin().DoubleArray("abc", 2334).End());
    assertEquals("doublej[_];", Begin().DoubleArray(-5).End());
    assertEquals("doublej[_] abc;", Begin().DoubleArray("abc", -5).End());
    assertEquals("doublej[a+b] abc;", Begin().DoubleArray("abc", "a+b").End());
  }

  @Test
  public void testStruct_CloseStruct_All() {
    assertEquals("{{{}}}", Begin().Struct().Struct().Struct().CloseStruct(true).End());
    assertEquals("{\n" +
        "\t{\n" +
        "\t\t{\n" +
        "\t\t}\n" +
        "\t}\n" +
        "}\n", Begin().Struct().Struct().Struct().CloseStruct(true).End(true));

    assertThrows(IllegalStateException.class, new Executable() {
      @Override
      public void execute() throws Throwable {
        Begin().CloseStruct(true);
      }
    });
  }

  @Test
  public void testHasOpenedStructs() {
    final JBBPDslBuilder dsl = Begin();
    assertFalse(dsl.hasOpenedStructs());
    dsl.Struct();
    assertTrue(dsl.hasOpenedStructs());
    dsl.CloseStruct();
    assertFalse(dsl.hasOpenedStructs());
  }

  @Test
  public void testStruct_CloseStruct() {
    assertEquals("{{{}}}", Begin().Struct().Struct().Struct().CloseStruct().CloseStruct().CloseStruct().End());
    assertEquals("{\n" +
        "\t{\n" +
        "\t\t{\n" +
        "\t\t}\n" +
        "\t}\n" +
        "}\n", Begin().Struct().Struct().Struct().CloseStruct().CloseStruct().CloseStruct().End(true));

    assertThrows(IllegalStateException.class, new Executable() {
      @Override
      public void execute() throws Throwable {
        Begin().CloseStruct();
      }
    });
  }

  @Test
  public void testType_StringArray() {
    assertEquals("stringj[a+b];", Begin().StringArray("a+b").End());
    assertEquals("stringj[2334] abc;", Begin().StringArray("abc", 2334).End());
    assertEquals("stringj[_];", Begin().StringArray(-5).End());
    assertEquals("stringj[_] abc;", Begin().StringArray("abc", -5).End());
    assertEquals("stringj[a+b] abc;", Begin().StringArray("abc", "a+b").End());
  }
}