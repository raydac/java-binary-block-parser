package com.igormaznitsa.jbbp.utils;

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayBit;
import com.igormaznitsa.jbbp.model.JBBPFieldStruct;
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
  public void testAllowedVarName() {
    Begin().Int("a").End();
    Begin().Int("A").End();
    Begin().Int("a1").End();
    Begin().Int("a2").End();
    Begin().Int("a1_1").End();
    Begin().Int("a2_2").End();
  }

  @Test
  public void testWrongName() {
    assertThrows(IllegalArgumentException.class, () -> Begin().Int("").End());
    assertThrows(IllegalArgumentException.class, () -> Begin().Int("  ").End());
    assertThrows(IllegalArgumentException.class, () -> Begin().Int("3a").End());
    assertThrows(IllegalArgumentException.class, () -> Begin().Int("ab\n").End());
    assertThrows(IllegalArgumentException.class, () -> Begin().Int("a$\n").End());
  }

  @Test
  public void testCheckForDuplicatedNameInSameStructure() {
    final JBBPDslBuilder builder = JBBPDslBuilder.Begin().Int("test");
    assertThrows(IllegalArgumentException.class, () -> builder.Bool("test"));

    assertThrows(IllegalArgumentException.class, () -> builder.Bool("Test"));

    final JBBPDslBuilder builder1 = JBBPDslBuilder.Begin().Int("test").Struct().CloseStruct();
    assertThrows(IllegalArgumentException.class, () -> builder1.Bool("test"));

    final JBBPDslBuilder builder2 = JBBPDslBuilder.Begin().Int("test");
    assertThrows(IllegalArgumentException.class, () -> builder2.Struct("test"));

    final JBBPDslBuilder builder3 = JBBPDslBuilder.Begin().Struct("test").Int("a").CloseStruct().Bool("b");
    assertThrows(IllegalArgumentException.class, () -> builder3.Struct("test"));

  }

  @Test
  public void testCheckForDuplicatedNameInDifferentStructures() {
    JBBPDslBuilder.Begin().Int("test").Struct().Bool("test").CloseStruct().End();
    JBBPDslBuilder.Begin().Int("test").Struct().Int("test").Struct().Bool("test").CloseStruct().CloseStruct().End();
    JBBPDslBuilder.Begin().Struct().Int("test").Struct().Bool("test").CloseStruct().CloseStruct().Int("test").End();
    JBBPDslBuilder.Begin().Struct("test").Int("test").CloseStruct().End();
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
  public void testType_Custom() {
    assertEquals("some;", Begin().Custom("some").End());
    assertEquals("some huzzaa;", Begin().Custom("some", "huzzaa").End());
    assertEquals("some:(a+b);", Begin().Custom("some", null, "a+b").End());
    assertEquals("some:(a+b) huzzaa;", Begin().Custom("some", "huzzaa", "a+b").End());
  }

  @Test
  public void testType_Var() {
    assertEquals("var;", Begin().Var().End());
    assertEquals("var huzzaa;", Begin().Var("huzzaa").End());
    assertEquals("var:(a+b);", Begin().Var(null, "a+b").End());
    assertEquals("var:(a+b) huzzaa;", Begin().Var("huzzaa", "a+b").End());
  }

  @Test
  public void testComment() {
    assertEquals("// Test\n", Begin().Comment("Test").End());
    assertEquals("// //\n// Test\n", Begin().Comment("//").Comment("Test").End());
    assertEquals("// Test\n// Test2\n", Begin().Comment("Test").Comment("Test2").End());
    assertEquals("int a;// Test\n// Test2\n", Begin().Int("a").Comment("Test").Comment("Test2").End());
    assertEquals("int a;\n// Test\n// Test2\n", Begin().Int("a").NewLineComment("Test").NewLineComment("Test2").End());
    assertEquals("int a;hello{// hello\n}// end hello\n", Begin().Int("a").Struct("hello").Comment("hello").CloseStruct().Comment("end hello").End());
  }

  @Test
  public void testType_VarArray() {
    assertEquals("var[1234];", Begin().VarArray(1234).End());
    assertEquals("var[q+b];", Begin().VarArray("q+b").End());
    assertEquals("var[1234] lupus;", Begin().VarArray("lupus", 1234).End());
    assertEquals("var[a+1234] lupus;", Begin().VarArray("lupus", "a+1234").End());
    assertEquals("var:(c/2)[_] huzzaa;", Begin().VarArray("huzzaa", -1, "c/2").End());
    assertEquals("var:(c/2)[a+b] huzzaa;", Begin().VarArray("huzzaa", "a+b", "c/2").End());
  }

  @Test
  public void testType_CustomArray() {
    assertEquals("some[1234];", Begin().CustomArray("some", 1234).End());
    assertEquals("some[q+b];", Begin().CustomArray("some", "q+b").End());
    assertEquals("some[1234] lupus;", Begin().CustomArray("some", "lupus", 1234).End());
    assertEquals("some[a+1234] lupus;", Begin().CustomArray("some", "lupus", "a+1234").End());
    assertEquals("some:(c/2)[_] huzzaa;", Begin().CustomArray("some", "huzzaa", -1, "c/2").End());
    assertEquals("some:(c/2)[a+b] huzzaa;", Begin().CustomArray("some", "huzzaa", "a+b", "c/2").End());
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

    assertThrows(IllegalArgumentException.class, () -> Begin().Skip(-3));
  }

  @Test
  public void testResetCounter() {
    assertEquals("reset$$;", Begin().ResetCounter().End());
  }

  @Test
  public void testVal() {
    assertThrows(NullPointerException.class, () -> Begin().Val(null, "a+b").End());
    assertThrows(NullPointerException.class, () -> Begin().Val("hello", null).End());
    assertThrows(IllegalArgumentException.class, () -> Begin().Val("", "a+b").End());
    assertThrows(IllegalArgumentException.class, () -> Begin().Val("hello", "").End());
    assertEquals("val:(a+b) hello;", Begin().Val("hello", "a+b").End());
  }

  @Test
  public void testType_Align() {
    assertEquals("align:1;", Begin().Align().End());
    assertEquals("align:3;", Begin().Align(3).End());
    assertEquals("align:(a+b);", Begin().Align("a+b").End());

    assertThrows(IllegalArgumentException.class, () -> Begin().Align(-3));
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

    assertThrows(IllegalStateException.class, () -> Begin().CloseStruct(true));
  }

  @Test
  public void testSize() {
    final JBBPDslBuilder dsl = Begin();
    assertEquals(0, dsl.size());
    dsl.Int();
    assertEquals(1, dsl.size());
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
  public void testStructArray() {
    assertEquals("[123]{}", Begin().StructArray(123).CloseStruct().End());
    assertEquals("[_]{}", Begin().StructArray("_").CloseStruct().End());
    assertEquals("alloha[123]{}", Begin().StructArray("alloha", 123).CloseStruct().End());
    assertEquals("alloha[_]{}", Begin().StructArray("alloha", "_").CloseStruct().End());
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

    assertThrows(IllegalStateException.class, () -> Begin().CloseStruct());
  }

  @Test
  public void testType_StringArray() {
    assertEquals("stringj[a+b];", Begin().StringArray("a+b").End());
    assertEquals("stringj[2334] abc;", Begin().StringArray("abc", 2334).End());
    assertEquals("stringj[_];", Begin().StringArray(-5).End());
    assertEquals("stringj[_] abc;", Begin().StringArray("abc", -5).End());
    assertEquals("stringj[a+b] abc;", Begin().StringArray("abc", "a+b").End());
  }

  @Test
  public void testAnotatedClass_AllClassAnnotated() {
    @Bin
    class Test {
      @Bin(type = BinType.UBYTE)
      int a;
    }

    assertEquals("Test{ubyte a;}", Begin().AnnotatedClass(Test.class).End());
  }

  @Test
  public void testAnotatedClass_AnnottatedButWithoutType() {
    class Test {
      @Bin(outOrder = 1)
      int a;
      @Bin(outOrder = 3)
      int c;
      @Bin(outOrder = 2, outByteOrder = JBBPByteOrder.LITTLE_ENDIAN)
      int b;

      class Internal {
        @Bin(outOrder = 1)
        short a;
        @Bin(outOrder = 2, extra = "8")
        short[] b;
      }

      @Bin(outOrder = 4, extra = "a+b")
      Internal[] d;
    }

    assertEquals("Test{int a;<int b;int c;d[a+b]{short a;short[8] b;}}", Begin().AnnotatedClass(Test.class).End());
  }

  @Test
  public void testReportedIssue_21_IAEforEmptyExtraAttributeForArrayField() {
    class BreakJBBPDslBuilderChild {
      @Bin(outOrder = 1, comment = "Reserved", type = BinType.BYTE)
      public byte reserved;
    }

    class BreakJBBPDslBuilderParent {
      @Bin(outOrder = 1)
      public BreakJBBPDslBuilderChild[] breakJBBPDslBuilderChildArray;
    }

    class BreakJBBPDslBuilderArrayField {
      @Bin(outOrder = 1, type = BinType.BYTE_ARRAY)
      public byte[] bytes;
    }

    try {
      Begin().AnnotatedClass(BreakJBBPDslBuilderParent.class).End();
      fail();
    } catch (IllegalArgumentException ex) {
      assertTrue(ex.getMessage().contains("Bin#extra"));
    }

    try {
      Begin().AnnotatedClass(BreakJBBPDslBuilderArrayField.class).End();
      fail();
    } catch (IllegalArgumentException ex) {
      assertTrue(ex.getMessage().contains("Bin#extra"));
    }
  }

  @Test
  public void testReportedIssue_20_NPEforOutBitNumber() throws Exception {
    class BreakJBBPDslBuilder {
      @Bin(outOrder = 1, comment = "Reserved", type = BinType.BIT_ARRAY, extra = "4")
      public byte[] reserved;
    }

    final String dsl = Begin().AnnotatedClass(BreakJBBPDslBuilder.class).End();

    assertEquals("BreakJBBPDslBuilder{bit:8[4] reserved;// Reserved\n}", dsl);

    JBBPFieldStruct struct = JBBPParser.prepare(dsl).parse(new byte[] {1, 2, 3, 4});
    assertArrayEquals(new byte[] {1, 2, 3, 4}, struct.findFieldForType(JBBPFieldStruct.class).findFieldForType(JBBPFieldArrayBit.class).getArray());
  }

  @Test
  public void testAnnotatedClass_DefaultBin() {
    @Bin(name = "sometest", type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_5)
    class Test {
      byte a;
      byte b;
      byte c;
      @Bin(name = "dd", type = BinType.BOOL)
      int d;
    }
    assertEquals("Test{bit:5 a;bit:5 b;bit:5 c;bool dd;}", Begin().AnnotatedClass(Test.class).End(false));
  }

  @Test
  public void testAnnotatedClass_DefaultBin_InnerClass() {
    @Bin(name = "sometest", type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_5)
    class Test {
      byte a;
      byte b;
      byte c;
      @Bin(name = "dd", type = BinType.BOOL)
      int d;

      @Bin(type = BinType.LONG)
      class Internal {
        int a;
        int b;
        int c;
      }

      @Bin(extra = "_")
      Internal[] array;
    }
    assertEquals("Test{bit:5 a;array[_]{long a;long b;long c;}bit:5 b;bit:5 c;bool dd;}", Begin().AnnotatedClass(Test.class).End(false));
  }

  @Test
  public void testAnotatedClass_AnnottatedAllTypes() {
    class Test {
      @Bin(outOrder = 1, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_4, comment = "bit field")
      byte a;
      @Bin(outOrder = 2, outBitNumber = JBBPBitNumber.BITS_2, extra = "123")
      byte[] a1;
      @Bin(outOrder = 3)
      boolean b;
      @Bin(outOrder = 4, extra = "456")
      boolean[] b1;
      @Bin(outOrder = 5)
      byte c;
      @Bin(outOrder = 6, extra = "456")
      byte[] c1;
      @Bin(outOrder = 7, outByteOrder = JBBPByteOrder.LITTLE_ENDIAN)
      short d;
      @Bin(outOrder = 8, extra = "2")
      short[] d1;
      @Bin(outOrder = 9, type = BinType.USHORT, outByteOrder = JBBPByteOrder.LITTLE_ENDIAN)
      short e;
      @Bin(outOrder = 10, type = BinType.USHORT_ARRAY, extra = "21")
      short[] e1;
      @Bin(outOrder = 11, outByteOrder = JBBPByteOrder.LITTLE_ENDIAN)
      int f;
      @Bin(outOrder = 12, extra = "211")
      int[] f1;
      @Bin(outOrder = 13, outByteOrder = JBBPByteOrder.LITTLE_ENDIAN)
      long g;
      @Bin(outOrder = 14, extra = "211")
      long[] g1;
      @Bin(outOrder = 15, outByteOrder = JBBPByteOrder.LITTLE_ENDIAN)
      float h;
      @Bin(outOrder = 16, extra = "1211")
      float[] h1;
      @Bin(outOrder = 17, outByteOrder = JBBPByteOrder.LITTLE_ENDIAN)
      double i;
      @Bin(outOrder = 18, extra = "3")
      double[] i1;
      @Bin(outOrder = 19)
      String l;
      @Bin(outOrder = 20, extra = "a+b")
      String[] l1;

      @DslBinCustom(type = "int9", extraExpression = "a+b", arraySizeExpression = "c*d", byteOrder = JBBPByteOrder.LITTLE_ENDIAN, comment = "some comment")
      int[] cus;

      class Some {
        @Bin(outOrder = 1, type = BinType.UBYTE)
        int a;
        @Bin(outOrder = 2, type = BinType.UBYTE_ARRAY, extra = "223")
        byte[] a1;

        @Bin
        class Internal {
          int a;
        }

        @Bin(outOrder = 3)
        Internal jjj;
      }

      @Bin(outOrder = 21)
      Test.Some x;

      @Bin(outOrder = 22, extra = "998")
      Test.Some[] x1;
    }

    assertEquals("Test{\n" +
        "\t<int9:(a+b)[c*d] cus;// some comment\n" +
        "\tbit:4 a;// bit field\n" +
        "\tbyte[123] a1;\n" +
        "\tbool b;\n" +
        "\tbool[456] b1;\n" +
        "\tbyte c;\n" +
        "\tbyte[456] c1;\n" +
        "\t<short d;\n" +
        "\tshort[2] d1;\n" +
        "\t<ushort e;\n" +
        "\tushort[21] e1;\n" +
        "\t<int f;\n" +
        "\tint[211] f1;\n" +
        "\t<long g;\n" +
        "\tlong[211] g1;\n" +
        "\t<floatj h;\n" +
        "\tfloatj[1211] h1;\n" +
        "\t<doublej i;\n" +
        "\tdoublej[3] i1;\n" +
        "\tstringj l;\n" +
        "\tstringj[a+b] l1;\n" +
        "\tx{\n" +
        "\t\tubyte a;\n" +
        "\t\tubyte[223] a1;\n" +
        "\t\tjjj{\n" +
        "\t\t\tint a;\n" +
        "\t\t}\n" +
        "\t}\n" +
        "\tx1[998]{\n" +
        "\t\tubyte a;\n" +
        "\t\tubyte[223] a1;\n" +
        "\t\tjjj{\n" +
        "\t\t\tint a;\n" +
        "\t\t}\n" +
        "\t}\n" +
        "}\n", Begin().AnnotatedClass(Test.class).End(true));

    assertEquals("<int9:(a+b)[c*d] cus;// some comment\n" +
        "bit:4 a;// bit field\n" +
        "byte[123] a1;\n" +
        "bool b;\n" +
        "bool[456] b1;\n" +
        "byte c;\n" +
        "byte[456] c1;\n" +
        "<short d;\n" +
        "short[2] d1;\n" +
        "<ushort e;\n" +
        "ushort[21] e1;\n" +
        "<int f;\n" +
        "int[211] f1;\n" +
        "<long g;\n" +
        "long[211] g1;\n" +
        "<floatj h;\n" +
        "floatj[1211] h1;\n" +
        "<doublej i;\n" +
        "doublej[3] i1;\n" +
        "stringj l;\n" +
        "stringj[a+b] l1;\n" +
        "x{\n" +
        "\tubyte a;\n" +
        "\tubyte[223] a1;\n" +
        "\tjjj{\n" +
        "\t\tint a;\n" +
        "\t}\n" +
        "}\n" +
        "x1[998]{\n" +
        "\tubyte a;\n" +
        "\tubyte[223] a1;\n" +
        "\tjjj{\n" +
        "\t\tint a;\n" +
        "\t}\n" +
        "}\n", Begin().AnnotatedClassFields(Test.class).End(true));
  }
}