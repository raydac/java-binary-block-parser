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

package com.igormaznitsa.jbbp.mapper;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.TestUtils;
import com.igormaznitsa.jbbp.exceptions.JBBPMapperException;
import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPOut;
import com.igormaznitsa.jbbp.model.JBBPFieldInt;
import java.io.ByteArrayInputStream;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class JBBPMapperTest {

  @Test
  void testMakeNewInstanceInLocalThroughDefaultConstructors() throws Exception {
    final StaticTopNoInstanceMethod result = JBBPParser.prepare("levelOne { levelTwos[_]{byte a;}}").parse(new byte[] {1, 2, 3}).mapTo(new StaticTopNoInstanceMethod());

    assertNotNull(result.levelOne);
    assertNotNull(result.levelOne.levelTwos);
    assertEquals(3, result.levelOne.levelTwos.length);
  }

  @Test
  void testMakeNewInstanceInLocalStaticClasses() throws Exception {
    final StaticTop result = JBBPParser.prepare("levelOne { levelTwos[_]{byte a;}}").parse(new byte[] {1, 2, 3}).mapTo(new StaticTop());

    assertNotNull(result.levelOne);
    assertNotNull(result.levelOne.levelTwos);
    assertEquals(3, result.levelOne.levelTwos.length);
  }

  @Test
  void testMakeNewInstanceInLocalNonStaticClasses() throws Exception {
    class Top {
      @Bin
      LevelOne levelOne;

      public Object newInstance(final Class<?> klazz) {
        if (klazz == LevelOne.class) {
          return new LevelOne();
        }
        return null;
      }

      class LevelOne {
        @Bin
        LevelTwo[] levelTwos;

        public Object newInstance(final Class<?> klazz) {
          if (klazz == LevelTwo.class) {
            return new LevelTwo();
          }
          return null;
        }

        class LevelTwo {
          @Bin
          byte a;
        }
      }
    }

    final Top result = JBBPParser.prepare("levelOne { levelTwos[_]{byte a;}}").parse(new byte[] {1, 2, 3}).mapTo(new Top());

    assertNotNull(result.levelOne);
    assertNotNull(result.levelOne.levelTwos);
    assertEquals(3, result.levelOne.levelTwos.length);
  }

  @Test
  void testMap_InsideStructAndClass() throws Exception {
    class Mapped {
      @Bin
      byte a;
    }
    assertEquals(3, JBBPParser.prepare("byte a; some{struc {byte a;}}").parse(new byte[] {1, 3}).mapTo("some.struc", new Mapped()).a);
  }

  @Test
  void testMap_RootStructToClassWithNullCustomProcessor() throws Exception {
    class Mapped {
      @Bin
      byte a;
    }
    assertEquals(3, JBBPMapper.map(JBBPParser.prepare("byte a;").parse(new byte[] {3}), new Mapped()).a);
  }

  @Test
  void testMap_Byte() throws Exception {
    class Mapped {
      @Bin
      byte a;
    }
    assertEquals(3, JBBPParser.prepare("byte a;").parse(new byte[] {3}).mapTo(new Mapped()).a);
  }

  @Test
  void testMap_Short() throws Exception {
    class Mapped {
      @Bin
      short a;
    }
    assertEquals(0x0304, JBBPParser.prepare("short a;").parse(new byte[] {3, 4}).mapTo(new Mapped()).a);
  }

  @Test
  void testMap_Boolean() throws Exception {
    class Mapped {
      @Bin
      boolean a;
      @Bin
      boolean b;
      @Bin
      boolean c;
    }
    final Mapped mapped = JBBPParser.prepare("bool a; bool b; bool c;").parse(new byte[] {23, 0, 12}).mapTo(new Mapped());
    assertTrue(mapped.a);
    assertFalse(mapped.b);
    assertTrue(mapped.c);
  }

  @Test
  void testMap_String() throws Exception {
    class Mapped {
      @Bin
      String a;
      @Bin
      String b;
    }
    final Mapped mapped = JBBPParser.prepare("stringj a; stringj b;").parse(new byte[] {3, 65, 66, 67, 2, 68, 69}).mapTo(new Mapped());
    assertEquals("ABC", mapped.a);
    assertEquals("DE", mapped.b);
  }

  @Test
  void testMap_StringArrayToStringArray() throws Exception {
    class Mapped {
      @Bin
      String[] a;
    }
    final Mapped mapped = JBBPParser.prepare("stringj [_] a;").parse(new byte[] {3, 65, 66, 67, 2, 68, 69}).mapTo(new Mapped());
    assertArrayEquals(new String[] {"ABC", "DE"}, mapped.a);
  }

  @Test
  void testMap_IgnoreStaticField() throws Exception {
    final MappedWithStaticField mapped = JBBPParser.prepare("int a;").parse(new byte[] {1, 2, 3, 4}).mapTo(new MappedWithStaticField());
    assertEquals(0x01020304, mapped.a);
    assertEquals(111, MappedWithStaticField.ignored);
  }

  @Test
  void testMap_Bit() throws Exception {
    class Mapped {
      @Bin(type = BinType.BIT)
      byte a;
      @Bin(type = BinType.BIT)
      byte b;
      @Bin(type = BinType.BIT)
      byte c;
    }
    final Mapped mapped = JBBPParser.prepare("bit:3 a; bit:2 b; bit:3 c; ").parse(new byte[] {(byte) 0xDD}).mapTo(new Mapped());
    assertEquals(5, mapped.a);
    assertEquals(3, mapped.b);
    assertEquals(6, mapped.c);
  }

  @Test
  void testMap_Int() throws Exception {
    class Mapped {
      @Bin
      int a;
    }
    assertEquals(0x01020304, JBBPParser.prepare("int a;").parse(new byte[] {1, 2, 3, 4}).mapTo(new Mapped()).a);
  }

  @Test
  void testMap_MapIntToFloat() throws Exception {
    class Mapped {
      @Bin(type = BinType.INT)
      float a;
    }

    final byte[] max = JBBPOut.BeginBin().Int(Float.floatToIntBits(Float.MAX_VALUE)).End().toByteArray();
    assertEquals(Float.MAX_VALUE, JBBPParser.prepare("int a;").parse(max).mapTo(new Mapped()).a, 0.005d);
    final byte[] min = JBBPOut.BeginBin().Int(Float.floatToIntBits(Float.MIN_VALUE)).End().toByteArray();
    assertEquals(Float.MIN_VALUE, JBBPParser.prepare("int a;").parse(min).mapTo(new Mapped()).a, 0.005d);
  }

  @Test
  void testMap_MapIntArrayToFloatArray() throws Exception {
    class Mapped {
      @Bin(type = BinType.INT_ARRAY)
      float[] a;
    }

    final byte[] max = JBBPOut.BeginBin().Float(Float.MAX_VALUE, Float.MIN_VALUE).End().toByteArray();
    final Mapped result = JBBPParser.prepare("int [_] a;").parse(max).mapTo(new Mapped());
    assertEquals(2, result.a.length);
    assertEquals(Float.MAX_VALUE, result.a[0], TestUtils.FLOAT_DELTA);
    assertEquals(Float.MIN_VALUE, result.a[1], TestUtils.FLOAT_DELTA);
  }

  @Test
  void testMap_MapLongArrayToDoubleArray() throws Exception {
    class Mapped {

      @Bin(type = BinType.LONG_ARRAY)
      double[] a;
    }

    final byte[] max = JBBPOut.BeginBin().Double(Double.MAX_VALUE, Double.MIN_VALUE).End().toByteArray();
    final Mapped result = JBBPParser.prepare("long [_] a;").parse(max).mapTo(new Mapped());
    assertEquals(2, result.a.length);
    assertEquals(Double.MAX_VALUE, result.a[0], TestUtils.FLOAT_DELTA);
    assertEquals(Double.MIN_VALUE, result.a[1], TestUtils.FLOAT_DELTA);
  }

  @Test
  void testMap_MapFloatToFloat() throws Exception {
    class Mapped {
      @Bin
      float a;
    }

    final byte[] max = JBBPOut.BeginBin().Float(-1.234567f).End().toByteArray();
    assertEquals(-1.234567f, JBBPParser.prepare("floatj a;").parse(max).mapTo(new Mapped()).a, TestUtils.FLOAT_DELTA);
  }

  @Test
  void testMap_MapFloatArrayToFloatArray() throws Exception {
    class Mapped {
      @Bin
      float[] a;
    }

    final byte[] max = JBBPOut.BeginBin().Float(-1.234567f, 1.234567f).End().toByteArray();
    assertArrayEquals(new float[] {-1.234567f, 1.234567f}, JBBPParser.prepare("floatj [_] a;").parse(max).mapTo(new Mapped()).a, TestUtils.FLOAT_DELTA);
  }

  @Test
  void testMap_MapLongToDouble() throws Exception {
    class Mapped {
      @Bin(type = BinType.LONG)
      double a;
    }

    final byte[] max = JBBPOut.BeginBin().Long(Double.doubleToLongBits(Double.MAX_VALUE)).End().toByteArray();
    assertEquals(Double.MAX_VALUE, JBBPParser.prepare("long a;").parse(max).mapTo(new Mapped()).a, TestUtils.FLOAT_DELTA);
    final byte[] min = JBBPOut.BeginBin().Long(Double.doubleToLongBits(Double.MIN_VALUE)).End().toByteArray();
    assertEquals(Double.MIN_VALUE, JBBPParser.prepare("long a;").parse(min).mapTo(new Mapped()).a, TestUtils.FLOAT_DELTA);
  }

  @Test
  void testMap_MapDoubleToDouble() throws Exception {
    class Mapped {
      @Bin
      double a;
    }

    final byte[] max = JBBPOut.BeginBin().Double(-1.2345678912345d).End().toByteArray();
    assertEquals(-1.2345678912345d, JBBPParser.prepare("doublej a;").parse(max).mapTo(new Mapped()).a, TestUtils.FLOAT_DELTA);
  }

  @Test
  void testMap_MapDoubleArrayToDoubleArray() throws Exception {
    class Mapped {
      @Bin
      double[] a;
    }

    final byte[] max = JBBPOut.BeginBin().Double(-1.2345678912345d, 45.3334d).End().toByteArray();
    assertArrayEquals(new double[] {-1.2345678912345d, 45.3334d}, JBBPParser.prepare("doublej [_] a;").parse(max).mapTo(new Mapped()).a, TestUtils.FLOAT_DELTA);
  }

  @Test
  void testMap_Long() throws Exception {
    class Mapped {
      @Bin
      long a;
    }
    assertEquals(0x0102030405060708L, JBBPParser.prepare("long a;").parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}).mapTo(new Mapped()).a);
  }

  @Test
  void testMap_UByte() throws Exception {
    class Mapped {
      @Bin(type = BinType.UBYTE)
      int a;
    }
    assertEquals(0xFE, JBBPParser.prepare("ubyte a;").parse(new byte[] {(byte) 0xFE}).mapTo(new Mapped()).a);
  }

  @Test
  void testMap_UShort() throws Exception {
    class Mapped {
      @Bin
      char a;
    }
    assertEquals(0x0102, JBBPParser.prepare("ushort a;").parse(new byte[] {1, 2}).mapTo(new Mapped()).a);
  }

  @Test
  void testMap_ByteArray() throws Exception {
    class Mapped {
      @Bin
      byte[] a;
    }
    assertArrayEquals(new byte[] {1, 2, 3, 4}, JBBPParser.prepare("byte [_] a;").parse(new byte[] {1, 2, 3, 4}).mapTo(new Mapped()).a);
  }

  @Test
  void testMap_UByteArrayToString() throws Exception {
    class Mapped {
      @Bin(type = BinType.UBYTE_ARRAY)
      String a;
    }
    assertEquals("JFIF", JBBPParser.prepare("ubyte [_] a;").parse(new byte[] {(byte) 0x4A, (byte) 0x46, (byte) 0x49, (byte) 0x46}).mapTo(new Mapped()).a);
  }

  @Test
  void testMap_BitArrayToString() throws Exception {
    class Mapped {
      @Bin(type = BinType.BIT_ARRAY)
      String a;
    }
    assertEquals(new String(new char[] {0xA, 0x4, 0x6, 0x4, 0x9, 0x4, 0x6, 0x4}), JBBPParser.prepare("bit:4 [_] a;").parse(new byte[] {(byte) 0x4A, (byte) 0x46, (byte) 0x49, (byte) 0x46}).mapTo(new Mapped()).a);
  }

  @Test
  void testMap_BitArrayToStringWhenWholeByte() throws Exception {
    class Mapped {
      @Bin(type = BinType.BIT_ARRAY)
      String a;
    }
    assertEquals(new String(new char[] {0xFF, 0xED, 0x01, 0x36}), JBBPParser.prepare("bit:8 [_] a;").parse(new byte[] {(byte) 0xFF, (byte) 0xED, (byte) 0x01, (byte) 0x36}).mapTo(new Mapped()).a);
  }

  @Test
  void testMap_ByteArrayToString() throws Exception {
    class Mapped {
      @Bin(type = BinType.BYTE_ARRAY)
      String a;
    }
    assertEquals("JFIF", JBBPParser.prepare("byte [_] a;").parse(new byte[] {(byte) 0x4A, (byte) 0x46, (byte) 0x49, (byte) 0x46}).mapTo(new Mapped()).a);
  }

  @Test
  void testMap_ShortArrayToString() throws Exception {
    class Mapped {
      @Bin(type = BinType.SHORT_ARRAY)
      String a;
    }
    assertEquals("JFIF", JBBPParser.prepare("short [_] a;").parse(new byte[] {0, (byte) 0x4A, 0, (byte) 0x46, 0, (byte) 0x49, 0, (byte) 0x46}).mapTo(new Mapped()).a);
  }

  @Test
  void testMap_IntArrayToString_Error() throws Exception {
    class Mapped {
      @Bin(type = BinType.INT_ARRAY)
      String a;
    }
    assertThrows(JBBPMapperException.class, () -> JBBPParser.prepare("int [_] a;").parse(new byte[] {0, (byte) 0x4A, 0, (byte) 0x46, 0, (byte) 0x49, 0, (byte) 0x46}).mapTo(new Mapped()));
  }

  @Test
  void testMap_UShortArrayToString() throws Exception {
    class Mapped {
      @Bin(type = BinType.USHORT_ARRAY)
      String a;
    }
    assertEquals("JFIF", JBBPParser.prepare("ushort [_] a;").parse(new byte[] {0, (byte) 0x4A, 0, (byte) 0x46, 0, (byte) 0x49, 0, (byte) 0x46}).mapTo(new Mapped()).a);
  }

  @Test
  void testMap_BitArray() throws Exception {
    class Mapped {
      @Bin(type = BinType.BIT_ARRAY)
      byte[] a;
    }
    assertArrayEquals(new byte[] {2, 0, 3, 2}, JBBPParser.prepare("bit:2 [_] a;").parse(new byte[] {(byte) 0xB2}).mapTo(new Mapped()).a);
  }

  @Test
  void testMap_ShortArray() throws Exception {
    class Mapped {
      @Bin
      short[] a;
    }
    assertArrayEquals(new short[] {0x0102, 0x0304}, JBBPParser.prepare("short [_] a;").parse(new byte[] {1, 2, 3, 4}).mapTo(new Mapped()).a);
  }

  @Test
  void testMap_BoolArray() throws Exception {
    class Mapped {
      @Bin
      boolean[] a;
    }
    final Mapped mapped = JBBPParser.prepare("bool [_] a;").parse(new byte[] {1, 0, 0, 4, 8, 0}).mapTo(new Mapped());
    assertEquals(6, mapped.a.length);
    assertTrue(mapped.a[0]);
    assertFalse(mapped.a[1]);
    assertFalse(mapped.a[2]);
    assertTrue(mapped.a[3]);
    assertTrue(mapped.a[4]);
    assertFalse(mapped.a[5]);
  }

  @Test
  void testMap_UShortArray() throws Exception {
    class Mapped {
      @Bin
      char[] a;
    }
    assertArrayEquals(new char[] {0x0102, 0x0304}, JBBPParser.prepare("ushort [_] a;").parse(new byte[] {1, 2, 3, 4}).mapTo(new Mapped()).a);
  }

  @Test
  void testMap_IntArray() throws Exception {
    class Mapped {
      @Bin
      int[] a;
    }
    assertArrayEquals(new int[] {0x01020304, 0x05060708}, JBBPParser.prepare("int [_] a;").parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}).mapTo(new Mapped()).a);
  }

  @Test
  public void testMap_Struct() throws Exception {
    class Inside {

      @Bin
      int a;
    }
    class Mapped {

      @Bin
      byte b;
      @Bin
      Inside a;
    }
    final Mapped mapped = JBBPParser.prepare("byte b; a{ int a; }").parse(new byte[] {1, 2, 3, 4, 5}).mapTo(new Mapped(), aClass -> {
      if (aClass == Inside.class) {
        return new Inside();
      }
      return null;
    });
    assertEquals(0x02030405, mapped.a.a);
  }

  @Test
  void testMap_StructArray() throws Exception {
    class Inside {
      @Bin
      int a;
    }
    class Mapped {

      @Bin
      byte b;
      @Bin
      Inside[] a;
    }
    final Mapped mapped = JBBPParser.prepare("byte b; a [_]{ int a; }").parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9}).mapTo(new Mapped(), aClass -> {
      if (aClass == Inside.class) {
        return new Inside();
      }
      return null;
    });
    assertEquals(2, mapped.a.length);
    assertEquals(0x02030405, mapped.a[0].a);
    assertEquals(0x06070809, mapped.a[1].a);
  }

  @Test
  void testMap_LongArray() throws Exception {
    class Mapped {
      @Bin
      long[] a;
    }
    assertArrayEquals(new long[] {0x0102030405060708L, 0x1112131415161718L}, JBBPParser.prepare("long [_] a;").parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18}).mapTo(new Mapped()).a);
  }

  @Test
  void testMap_ErrorForMappingStructureToPrimitiveField() {
    class Mapped {
      @Bin(name = "test", type = BinType.STRUCT)
      long a;
    }
    assertThrows(JBBPMapperException.class, () -> JBBPParser.prepare("test { byte [_] a;}").parse(new byte[] {1, 2, 3, 4}).mapTo(new Mapped()));
  }

  @Test
  void testMap_mapInsideStructureDefinedByItsPath() throws Exception {
    class Mapped {
      @Bin
      long a;
    }
    final Mapped mapped = JBBPParser.prepare("byte f; test { inside {long a;} }").parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9}).mapTo("test.inside", new Mapped());
    assertEquals(0x0203040506070809L, mapped.a);
  }

  @Test
  void testMap_mapInsideStructureDefinedByItsPath_ErrorForNonStructure() throws Exception {
    class Mapped {
      @Bin
      long a;
    }

    assertThrows(JBBPMapperException.class, () -> JBBPParser.prepare("byte f; test { inside {long a;} }").parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9}).mapTo("f", new Mapped()));
  }

  @Test
  void testMap_privateFieldInPackagelevelClass() {
    assertThrows(JBBPMapperException.class, () -> JBBPParser.prepare("int field;").parse(new byte[] {1, 2, 3, 4}).mapTo(new ClassWithPrivateFields()));
  }

  @Test
  void testMap_privateFieldWithSetterInPackagelevelClass() throws Exception {
    final ClassWithPrivateFieldsAndSetterGetter instance = JBBPParser.prepare("int field;").parse(new byte[] {1, 2, 3, 4}).mapTo(new ClassWithPrivateFieldsAndSetterGetter());
    assertEquals(0x1020304, instance.getField());
  }

  @Test
  public void testMap_classWithGettersSettersAndGenerator() throws Exception {
    final ClassWithPFGSG instance = JBBPParser.prepare("byte a; byte b; i { byte c; ii { byte d;}}")
        .parse(new byte[] {1, 2, 3, 4})
        .mapTo(new ClassWithPFGSG());

    assertEquals(1, instance.getA());
    assertEquals(2, instance.getB());
    assertEquals(3, instance.getI().getC());
    assertEquals(4, instance.getI().getIi().getD());

    assertArrayEquals(new byte[] {1, 2, 3, 4}, JBBPOut.BeginBin().Bin(instance).End().toByteArray());
  }

  @Test
  void testMap_customMappingFields_Class() throws Exception {
    final class Mapped {

      @Bin
      int a;
      @Bin(custom = true, typeExtraPartExpression = "TEST_TEXT")
      String b;
      @Bin
      int c;
    }

    final Mapped mapped = JBBPParser.prepare("int a; int b; int c;").parse(new byte[] {1, 2, 3, 4, 0x4A, 0x46, 0x49, 0x46, 5, 6, 7, 8}).mapTo(new Mapped(), (parsedBlock, annotation, field) -> {
      if ("b".equals(field.getName()) && "TEST_TEXT".equals(annotation.typeExtraPartExpression())) {
        final int bvalue = parsedBlock.findFieldForNameAndType("b", JBBPFieldInt.class).getAsInt();
        return String.valueOf((char) ((bvalue >>> 24) & 0xFF)) + (char) ((bvalue >>> 16) & 0xFF) + (char) ((bvalue >>> 8) & 0xFF) + (char) (bvalue & 0xFF);
      } else {
        fail("Unexpected state" + field);
        return null;
      }
    });

    assertEquals(0x01020304, mapped.a);
    assertEquals("JFIF", mapped.b);
    assertEquals(0x05060708, mapped.c);
  }

  @Test
  void testMap_customMappingFields_ClassInstance() throws Exception {
    final class Mapped {
      @Bin
      int a;
      @Bin(custom = true, typeExtraPartExpression = "TEST_TEXT")
      String b;
      @Bin
      int c;
    }

    final Mapped mapped = new Mapped();

    final Mapped result = JBBPParser.prepare("int a; int b; int c;").parse(new byte[] {1, 2, 3, 4, 0x4A, 0x46, 0x49, 0x46, 5, 6, 7, 8}).mapTo(mapped, (parsedBlock, annotation, field) -> {
      if ("b".equals(field.getName()) && "TEST_TEXT".equals(annotation.typeExtraPartExpression())) {
        final int bvalue = parsedBlock.findFieldForNameAndType("b", JBBPFieldInt.class).getAsInt();
        return String.valueOf((char) ((bvalue >>> 24) & 0xFF)) + (char) ((bvalue >>> 16) & 0xFF) + (char) ((bvalue >>> 8) & 0xFF) + (char) (bvalue & 0xFF);
      } else {
        fail("Unexpected state" + field);
        return null;
      }
    });

    assertSame(mapped, result);

    assertEquals(0x01020304, mapped.a);
    assertEquals("JFIF", mapped.b);
    assertEquals(0x05060708, mapped.c);
  }

  @Test
  void testMap_AnnotationForWholeClass() throws Exception {
    @Bin
    final class Parsed {
      int a;
      int b;
      @Bin(type = BinType.BYTE_ARRAY)
      String c;
    }

    final Parsed parsed = JBBPParser.prepare("int a; int b; byte [_] c;").parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd'}).mapTo(new Parsed());
    assertEquals(0x01020304, parsed.a);
    assertEquals(0x05060708, parsed.b);
    assertEquals("abcd", parsed.c);
  }

  @Test
  void testMap_InstanceOfInnerClass() throws Exception {
    final class Outer {
      @Bin
      int value;
      @Bin
      Inner inner;

      public Outer() {
        inner = new Outer.Inner();
      }

      final class Inner {

        @Bin
        byte a;
        @Bin
        byte b;
      }
    }

    final Outer oldouter = new Outer();
    final Outer.Inner inner = oldouter.inner;

    final Outer newouter = JBBPParser.prepare("int value; inner{ byte a; byte b;}").parse(new byte[] {1, 2, 3, 4, 5, 6}).mapTo(oldouter);

    assertSame(oldouter, newouter);
    assertSame(inner, newouter.inner);
    assertEquals(0x01020304, oldouter.value);
    assertEquals(5, inner.a);
    assertEquals(6, inner.b);
  }

  @Test
  void testMap_InstanceOfInnerClassPreparedArray() throws Exception {
    final class Outer {
      @Bin
      int value;
      @Bin
      Inner[] inner;

      public Outer() {
        inner = new Outer.Inner[2];
        inner[0] = new Outer.Inner();
        inner[1] = new Outer.Inner();
      }

      final class Inner {

        @Bin
        byte a;
        @Bin
        byte b;
      }
    }

    final Outer oldouter = new Outer();
    final Outer.Inner[] inner = oldouter.inner;
    final Outer.Inner inner0 = oldouter.inner[0];
    final Outer.Inner inner1 = oldouter.inner[1];

    final Outer newouter = JBBPParser.prepare("int value; inner [2] { byte a; byte b;}").parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}).mapTo(oldouter);

    assertSame(oldouter, newouter);
    assertSame(inner, newouter.inner);
    assertSame(inner0, newouter.inner[0]);
    assertSame(inner1, newouter.inner[1]);
    assertEquals(0x01020304, oldouter.value);
    assertEquals(5, inner[0].a);
    assertEquals(6, inner[0].b);
    assertEquals(7, inner[1].a);
    assertEquals(8, inner[1].b);
  }

  @Test
  void testMap_InstanceOfInnerClassNonPreparedArray() throws Exception {
    final class Outer {

      @Bin
      int value;
      @Bin
      Inner[] inner;

      public Outer() {
        inner = new Outer.Inner[2];
      }

      Inner makeInner() {
        return new Inner();
      }

      final class Inner {

        @Bin
        byte a;
        @Bin
        byte b;
      }
    }

    final Outer oldouter = new Outer();
    final Outer.Inner[] inner = oldouter.inner;
    assertNull(inner[0]);
    assertNull(inner[1]);

    final Outer newouter = JBBPParser.prepare("int value; inner [2] { byte a; byte b;}").parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}).mapTo(oldouter, aClass -> {
      if (aClass == Outer.Inner.class) {
        return oldouter.makeInner();
      }
      return null;
    });

    assertSame(oldouter, newouter);
    assertSame(inner, newouter.inner);
    assertEquals(0x01020304, oldouter.value);
    assertEquals(5, inner[0].a);
    assertEquals(6, inner[0].b);
    assertEquals(7, inner[1].a);
    assertEquals(8, inner[1].b);
  }

  @Test
  void testMap_InstanceOfInnerClassNonPreparedArray_ErrorForDifferentSize() {
    final class Outer {

      @Bin
      int val;
      @Bin
      Inner[] inner;

      public Outer() {
        inner = new Outer.Inner[3];
      }

      final class Inner {

        @Bin
        byte a;
        @Bin
        byte b;
      }
    }

    assertThrows(JBBPMapperException.class, () -> JBBPParser.prepare("int value; inner [2] { byte a; byte b;}").parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}).mapTo(new Outer()));
  }

  @Test
  void testMap_MapToClassHierarchyWithAnnotationInheritance() throws Exception {
    @Bin
    class Ancestor {

      int a;
    }

    class Successor extends Ancestor {

      int b;
    }

    final Successor successor = JBBPParser.prepare("int a; int b;").parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}).mapTo(new Successor());

    assertEquals(0x01020304, successor.a);
    assertEquals(0x05060708, successor.b);
  }

  @Test
  void testMap_MapElementsByTheirPaths() throws Exception {
    class Parsed {

      @Bin(path = "struct.a")
      byte num;
      @Bin(path = "struct.b", type = BinType.BYTE_ARRAY)
      String str;
    }

    final Parsed parsed = JBBPParser.prepare("int start; struct { byte a; byte [3] b; } int end;").parse(new byte[] {1, 2, 3, 4, 5, (byte) 'a', (byte) 'b', (byte) 'c', 6, 7, 8, 9}).mapTo(new Parsed());
    assertEquals(0x05, parsed.num);
    assertEquals("abc", parsed.str);
  }

  @Test
  void testMap_MapElementsByTheirPaths_ErrorForUnknownField() {
    class Parsed {

      @Bin(path = "struct.a")
      byte num;
      @Bin(path = "struct.c", type = BinType.BYTE_ARRAY)
      String str;
    }

    assertThrows(JBBPMapperException.class, () -> JBBPParser.prepare("int start; struct { byte a; byte [3] b; } int end;").parse(new byte[] {1, 2, 3, 4, 5, (byte) 'a', (byte) 'b', (byte) 'c', 6, 7, 8, 9}).mapTo(new Parsed()));
  }

  @Test
  void testMap_MapElementsByTheirPaths_ErrorForFieldIncompatibleType() {
    class Parsed {

      @Bin(path = "struct.a")
      byte num;
      @Bin(path = "struct.b", type = BinType.UBYTE_ARRAY)
      String str;
    }

    assertThrows(JBBPMapperException.class, () -> JBBPParser.prepare("int start; struct { byte a; byte [3] b; } int end;").parse(new byte[] {1, 2, 3, 4, 5, (byte) 'a', (byte) 'b', (byte) 'c', 6, 7, 8, 9}).mapTo(new Parsed()));
  }

  @Test
  void testMap_MapElementsByTheirPaths_ErrorForFieldIncompatibleType_ArrayMappingField() {
    class Parsed {

      @Bin(path = "struct.a", type = BinType.BYTE)
      byte[] num;
      @Bin(path = "struct.b", type = BinType.BYTE_ARRAY)
      String str;
    }

    assertThrows(JBBPMapperException.class, () -> JBBPParser.prepare("int start; struct { byte a; byte [3] b; } int end;").parse(new byte[] {1, 2, 3, 4, 5, (byte) 'a', (byte) 'b', (byte) 'c', 6, 7, 8, 9}).mapTo(new Parsed()));
  }

  @Test
  void testMap_MapElementsByTheirPaths_ErrorForFieldIncompatibleType_ArrayBinField() {
    class Parsed {
      @Bin(path = "struct.a")
      byte num;
      @Bin(path = "struct.b", type = BinType.BYTE_ARRAY)
      byte str;
    }

    assertThrows(JBBPMapperException.class, () -> JBBPParser.prepare("int start; struct { byte a; byte [3] b; } int end;").parse(new byte[] {1, 2, 3, 4, 5, (byte) 'a', (byte) 'b', (byte) 'c', 6, 7, 8, 9}).mapTo(new Parsed()));
  }

  @Test
  void testMap_IgnoreMarkedFieldByDefaultIfTransient() throws Exception {
    @Bin
    class Parsed {
      @Bin(path = "struct.a")
      byte num;
      @Bin(path = "struct.b", type = BinType.BYTE_ARRAY)
      String str;
      transient String ignored;
    }

    final Parsed parsed = JBBPParser.prepare("int start; struct { byte a; byte [3] b; } int end;").parse(new byte[] {1, 2, 3, 4, 5, (byte) 'a', (byte) 'b', (byte) 'c', 6, 7, 8, 9}).mapTo(new Parsed());
    assertEquals(0x05, parsed.num);
    assertEquals("abc", parsed.str);
  }

  @Test
  void testMap_ParsedMarkedTransientField() throws Exception {
    @Bin
    class Parsed {
      @Bin(path = "struct.a")
      byte num;
      @Bin(path = "struct.b", type = BinType.BYTE_ARRAY)
      String str;
      @Bin(path = "struct.c", type = BinType.BYTE_ARRAY)
      transient String trans;
    }

    final Parsed parsed = JBBPParser.prepare("int start; struct { byte a; byte [3] b; byte [3] c; } byte end;").parse(new byte[] {1, 2, 3, 4, 5, (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f', 9}).mapTo(new Parsed());
    assertEquals(0x05, parsed.num);
    assertEquals("abc", parsed.str);
    assertEquals("def", parsed.trans);
  }

  @Test
  void testMap_IgnoreNonMarkedField() throws Exception {
    class Parsed {

      @Bin(path = "struct.a")
      byte num;
      @Bin(path = "struct.b", type = BinType.BYTE_ARRAY)
      String str;
      String ignored;
    }

    final Parsed parsed = JBBPParser.prepare("int start; struct { byte a; byte [3] b; } int end;").parse(new byte[] {1, 2, 3, 4, 5, (byte) 'a', (byte) 'b', (byte) 'c', 6, 7, 8, 9}).mapTo(new Parsed());
    assertEquals(0x05, parsed.num);
    assertEquals("abc", parsed.str);
  }

  @Test
  void testMap_Structure_IgnoringFinalFieldsInMapping() throws Exception {
    @Bin
    class Struct {

      final byte a;
      final byte b;

      Struct(byte a, byte b) {
        this.a = a;
        this.b = b;
      }
    }

    @Bin
    class Parsed {

      final Struct[] struct;

      Parsed(Struct[] s) {
        this.struct = s;
      }
    }

    final Random rnd = new Random(1234);
    final byte[] array = new byte[1024];
    rnd.nextBytes(array);

    final Parsed parsed = JBBPParser.prepare("struct [_] { byte a; byte b; }").parse(new ByteArrayInputStream(array)).mapTo(new Parsed(null), aClass -> {
      fail("Must not be called");
      return null;
    });
    assertNull(parsed.struct);
  }

  @Test
  void testMap_FieldWithDefinedBitNumberToBitField_FieldPresented() throws Exception {
    class Parsed {
      @Bin(bitNumber = JBBPBitNumber.BITS_5)
      byte field;
    }
    final Parsed parsed = JBBPParser.prepare("int fieldint; bit:5 field;").parse(new byte[] {1, 2, 3, 4, 0x35}).mapTo(new Parsed());
    assertEquals(0x15, parsed.field);
  }

  @Test
  void testMap_FieldWithDefinedBitNumberToBitField_FieldPresentedWithDifferentBitNumber() throws Exception {
    class Parsed {
      @Bin(bitNumber = JBBPBitNumber.BITS_5)
      byte field;
    }
    assertThrows(JBBPMapperException.class, () -> JBBPParser.prepare("int fieldint; bit:6 field;").parse(new byte[] {1, 2, 3, 4, 0x35}).mapTo(new Parsed()));
  }

  @Test
  void testMap_ArrayFieldWithDefinedBitNumberToArrayBitField_FieldPresented() throws Exception {
    class Parsed {
      @Bin(bitNumber = JBBPBitNumber.BITS_4)
      byte[] field;
    }

    final Parsed parsed = JBBPParser.prepare("int fieldint; bit:4 [2] field;").parse(new byte[] {1, 2, 3, 4, 0x35}).mapTo(new Parsed());
    assertArrayEquals(new byte[] {5, 3}, parsed.field);
  }

  @Test
  void testMap_ArrayFieldIgnoredBitNumberFieldForDefinedType() throws Exception {
    class Parsed {
      @Bin(type = BinType.INT_ARRAY, bitNumber = JBBPBitNumber.BITS_4)
      int[] field;
    }
    final Parsed parsed = JBBPParser.prepare("int fieldint; int [2] field;").parse(new byte[] {1, 2, 3, 4, 0x5, 0x6, 0x7, 0x8, 0x9, 0x0A, 0x0B, 0x0C}).mapTo(new Parsed());
    assertArrayEquals(new int[] {0x05060708, 0x090A0B0C}, parsed.field);
  }

  @Test
  void testMap_ArrayFieldWithDefinedBitNumberToArrayBitField_FieldPresentedWithDifferentBitNumber() throws Exception {
    class Parsed {

      @Bin(bitNumber = JBBPBitNumber.BITS_4)
      byte field;
    }
    assertThrows(JBBPMapperException.class, () -> JBBPParser.prepare("int fieldint; bit:3 [2] field;").parse(new byte[] {1, 2, 3, 4, 0x35}).mapTo(new Parsed()));
  }

  @Test
  void testMap_IgnoreNotFoundFields() throws Exception {
    class Parsed {

      @Bin
      int a;
      @Bin
      int b;
    }

    final Parsed parsed = JBBPParser.prepare("int a; int b;", JBBPParser.FLAG_SKIP_REMAINING_FIELDS_IF_EOF).parse(new byte[] {1, 2, 3, 4}).mapTo(new Parsed(), JBBPMapper.FLAG_IGNORE_MISSING_VALUES);
    assertEquals(0x01020304, parsed.a);
    assertEquals(0, parsed.b);
  }

  public static class StaticTop {
    @Bin
    public StaticLevelOne levelOne;

    public Object newInstance(final Class<?> klazz) {
      if (klazz == StaticTop.StaticLevelOne.class) {
        return new StaticTop.StaticLevelOne();
      }
      if (klazz == StaticTop.StaticLevelOne.StaticLevelTwo.class) {
        return new StaticTop.StaticLevelOne.StaticLevelTwo();
      }
      return null;
    }

    public static class StaticLevelOne {
      @Bin
      public StaticLevelTwo[] levelTwos;

      public static class StaticLevelTwo {
        @Bin
        byte a;
      }
    }
  }

  public static class StaticTopNoInstanceMethod {
    @Bin
    public StaticLevelOne levelOne;

    public static class StaticLevelOne {
      @Bin
      public StaticLevelTwo[] levelTwos;

      public static class StaticLevelTwo {
        @Bin
        byte a;
      }
    }
  }

  @Bin
  private static class MappedWithStaticField {

    static int ignored = 111;
    int a;
  }
}
