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
}