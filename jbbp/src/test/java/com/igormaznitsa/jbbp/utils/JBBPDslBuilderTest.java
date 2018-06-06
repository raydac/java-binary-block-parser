package com.igormaznitsa.jbbp.utils;

import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static com.igormaznitsa.jbbp.utils.JBBPDslBuilder.Begin;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JBBPDslBuilderTest {

  @Test
  public void testEmptyStringForEmptyContent() {
    assertEquals("", Begin().build());
    assertEquals("", Begin().build(false));
    assertEquals("", Begin().build(true));
  }

  @Test
  public void testNonFormatted() {
    assertEquals("bool test;{int field1;}", Begin().Bool("test").Struct().Int("field1").EndStruct().build(false));
  }

  @Test
  public void testFormatted() {
    assertEquals("bool test;\n{\n\tint field1;\n}\n", Begin().Bool("test").Struct().Int("field1").EndStruct().build(true));
  }

  @Test
  public void testType_Bit() {
    assertEquals("bit:1;", Begin().Bit().build());
    assertEquals("bit:1 abc;", Begin().Bit("abc").build());
  }

  @Test
  public void testType_Bits() {
    assertEquals("bit:4;", Begin().Bits(4).build());
    assertEquals("bit:3;", Begin().Bits(JBBPBitNumber.BITS_3).build());
    assertEquals("bit:3 huzzaa;", Begin().Bits("huzzaa", JBBPBitNumber.BITS_3).build());
    assertEquals("bit:3;", Begin().Bits(null, JBBPBitNumber.BITS_3).build());
  }

  @Test
  public void testType_Bool() {
    assertEquals("bool;", Begin().Bool().build());
    assertEquals("bool abc;", Begin().Bool("abc").build());
  }

  @Test
  public void testType_Byte() {
    assertEquals("byte;", Begin().Byte().build());
    assertEquals("byte abc;", Begin().Byte("abc").build());
  }

  @Test
  public void testType_UByte() {
    assertEquals("ubyte;", Begin().UByte().build());
    assertEquals("ubyte abc;", Begin().UByte("abc").build());
  }

  @Test
  public void testType_Short() {
    assertEquals("short;", Begin().Short().build());
    assertEquals("short abc;", Begin().Short("abc").build());
  }

  @Test
  public void testType_UShort() {
    assertEquals("ushort;", Begin().UShort().build());
    assertEquals("ushort abc;", Begin().UShort("abc").build());
  }

  @Test
  public void testType_Int() {
    assertEquals("int;", Begin().Int().build());
    assertEquals("int abc;", Begin().Int("abc").build());
  }

  @Test
  public void testType_Long() {
    assertEquals("long;", Begin().Long().build());
    assertEquals("long abc;", Begin().Long("abc").build());
  }

  @Test
  public void testType_Float() {
    assertEquals("floatj;", Begin().Float().build());
    assertEquals("floatj abc;", Begin().Float("abc").build());
  }

  @Test
  public void testType_Double() {
    assertEquals("doublej;", Begin().Double().build());
    assertEquals("doublej abc;", Begin().Double("abc").build());
  }

  @Test
  public void testType_String() {
    assertEquals("stringj;", Begin().String().build());
    assertEquals("stringj abc;", Begin().String("abc").build());
  }

  @Test
  public void testType_Skip() {
    assertEquals("skip:1;", Begin().Skip().build());
    assertEquals("skip:3;", Begin().Skip(3).build());
    assertEquals("skip:(a+b);", Begin().Skip("a+b").build());

    assertThrows(IllegalArgumentException.class, new Executable() {
      @Override
      public void execute() throws Throwable {
       Begin().Skip(-3);
      }
    });
  }

  @Test
  public void testType_Align() {
    assertEquals("align:1;", Begin().Align().build());
    assertEquals("align:3;", Begin().Align(3).build());
    assertEquals("align:(a+b);", Begin().Align("a+b").build());

    assertThrows(IllegalArgumentException.class, new Executable() {
      @Override
      public void execute() throws Throwable {
       Begin().Align(-3);
      }
    });
  }

  @Test
  public void testType_BitArray() {
    assertEquals("bit:3[_];", Begin().BitArray(JBBPBitNumber.BITS_3, -1).build());
    assertEquals("bit:3[23];", Begin().BitArray(JBBPBitNumber.BITS_3, 23).build());
    assertEquals("bit:3[23] some;", Begin().BitArray("some", JBBPBitNumber.BITS_3, 23).build());
    assertEquals("bit:3[a+b] some;", Begin().BitArray("some", JBBPBitNumber.BITS_3, "a+b").build());
    assertEquals("bit:3[a+b];", Begin().BitArray(JBBPBitNumber.BITS_3, "a+b").build());
  }

  @Test
  public void testType_BoolArray() {
    assertEquals("bool[a+b];", Begin().BoolArray("a+b").build());
    assertEquals("bool[2334] abc;", Begin().BoolArray("abc",2334).build());
    assertEquals("bool[_];", Begin().BoolArray(-5).build());
    assertEquals("bool[_] abc;", Begin().BoolArray("abc",-5).build());
    assertEquals("bool[a+b] abc;", Begin().BoolArray("abc","a+b").build());
  }

  @Test
  public void testType_ByteArray() {
    assertEquals("byte[a+b];", Begin().ByteArray("a+b").build());
    assertEquals("byte[2334] abc;", Begin().ByteArray("abc",2334).build());
    assertEquals("byte[_];", Begin().ByteArray(-5).build());
    assertEquals("byte[_] abc;", Begin().ByteArray("abc",-5).build());
    assertEquals("byte[a+b] abc;", Begin().ByteArray("abc","a+b").build());
  }

  @Test
  public void testType_UByteArray() {
    assertEquals("ubyte[a+b];", Begin().UByteArray("a+b").build());
    assertEquals("ubyte[2334] abc;", Begin().UByteArray("abc",2334).build());
    assertEquals("ubyte[_];", Begin().UByteArray(-5).build());
    assertEquals("ubyte[_] abc;", Begin().UByteArray("abc",-5).build());
    assertEquals("ubyte[a+b] abc;", Begin().UByteArray("abc","a+b").build());
  }

  @Test
  public void testType_ShortArray() {
    assertEquals("short[a+b];", Begin().ShortArray("a+b").build());
    assertEquals("short[2334] abc;", Begin().ShortArray("abc",2334).build());
    assertEquals("short[_];", Begin().ShortArray(-5).build());
    assertEquals("short[_] abc;", Begin().ShortArray("abc",-5).build());
    assertEquals("short[a+b] abc;", Begin().ShortArray("abc","a+b").build());
  }

  @Test
  public void testType_UShortArray() {
    assertEquals("ushort[a+b];", Begin().UShortArray("a+b").build());
    assertEquals("ushort[2334] abc;", Begin().UShortArray("abc",2334).build());
    assertEquals("ushort[_];", Begin().UShortArray(-5).build());
    assertEquals("ushort[_] abc;", Begin().UShortArray("abc",-5).build());
    assertEquals("ushort[a+b] abc;", Begin().UShortArray("abc","a+b").build());
  }

  @Test
  public void testType_IntArray() {
    assertEquals("int[a+b];", Begin().IntArray("a+b").build());
    assertEquals("int[2334] abc;", Begin().IntArray("abc",2334).build());
    assertEquals("int[_];", Begin().IntArray(-5).build());
    assertEquals("int[_] abc;", Begin().IntArray("abc",-5).build());
    assertEquals("int[a+b] abc;", Begin().IntArray("abc","a+b").build());
  }

  @Test
  public void testType_LongArray() {
    assertEquals("long[a+b];", Begin().LongArray("a+b").build());
    assertEquals("long[2334] abc;", Begin().LongArray("abc",2334).build());
    assertEquals("long[_];", Begin().LongArray(-5).build());
    assertEquals("long[_] abc;", Begin().LongArray("abc",-5).build());
    assertEquals("long[a+b] abc;", Begin().LongArray("abc","a+b").build());
  }

  @Test
  public void testType_FloatArray() {
    assertEquals("floatj[a+b];", Begin().FloatArray("a+b").build());
    assertEquals("floatj[2334] abc;", Begin().FloatArray("abc",2334).build());
    assertEquals("floatj[_];", Begin().FloatArray(-5).build());
    assertEquals("floatj[_] abc;", Begin().FloatArray("abc",-5).build());
    assertEquals("floatj[a+b] abc;", Begin().FloatArray("abc","a+b").build());
  }

  @Test
  public void testType_DoubleArray() {
    assertEquals("doublej[a+b];", Begin().DoubleArray("a+b").build());
    assertEquals("doublej[2334] abc;", Begin().DoubleArray("abc",2334).build());
    assertEquals("doublej[_];", Begin().DoubleArray(-5).build());
    assertEquals("doublej[_] abc;", Begin().DoubleArray("abc",-5).build());
    assertEquals("doublej[a+b] abc;", Begin().DoubleArray("abc","a+b").build());
  }

  @Test
  public void testType_StringArray() {
    assertEquals("stringj[a+b];", Begin().StringArray("a+b").build());
    assertEquals("stringj[2334] abc;", Begin().StringArray("abc",2334).build());
    assertEquals("stringj[_];", Begin().StringArray(-5).build());
    assertEquals("stringj[_] abc;", Begin().StringArray("abc",-5).build());
    assertEquals("stringj[a+b] abc;", Begin().StringArray("abc","a+b").build());
  }
}