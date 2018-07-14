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

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.TestUtils;
import com.igormaznitsa.jbbp.exceptions.JBBPMapperException;
import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPOut;
import com.igormaznitsa.jbbp.mapper.instantiators.JBBPClassInstantiator;
import com.igormaznitsa.jbbp.mapper.instantiators.JBBPClassInstantiatorFactory;
import com.igormaznitsa.jbbp.mapper.instantiators.JBBPClassInstantiatorType;
import com.igormaznitsa.jbbp.mapper.instantiators.JBBPSafeInstantiator;
import com.igormaznitsa.jbbp.mapper.instantiators.JBBPUnsafeInstantiator;
import com.igormaznitsa.jbbp.model.JBBPFieldInt;
import com.igormaznitsa.jbbp.model.JBBPFieldStruct;
import com.igormaznitsa.jbbp.utils.JBBPSystemProperty;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.jupiter.api.function.Executable;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class JBBPMapperTest {

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_InsideStructAndClass() throws Exception {
    class Mapped {

      @Bin
      byte a;
    }
    assertEquals(3, JBBPParser.prepare("byte a; some{struc {byte a;}}").parse(new byte[] {1, 3}).mapTo("some.struc", Mapped.class).a);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_RootStructToClassWithNullCustomProcessor() throws Exception {
    class Mapped {

      @Bin
      byte a;
    }
    assertEquals(3, JBBPMapper.map(JBBPParser.prepare("byte a;").parse(new byte[] {3}), Mapped.class).a);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_Byte() throws Exception {
    class Mapped {

      @Bin
      byte a;
    }
    assertEquals(3, JBBPParser.prepare("byte a;").parse(new byte[] {3}).mapTo(Mapped.class).a);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_Short() throws Exception {
    class Mapped {

      @Bin
      short a;
    }
    assertEquals(0x0304, JBBPParser.prepare("short a;").parse(new byte[] {3, 4}).mapTo(Mapped.class).a);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_Boolean() throws Exception {
    class Mapped {

      @Bin
      boolean a;
      @Bin
      boolean b;
      @Bin
      boolean c;
    }
    final Mapped mapped = JBBPParser.prepare("bool a; bool b; bool c;").parse(new byte[] {23, 0, 12}).mapTo(Mapped.class);
    assertTrue(mapped.a);
    assertFalse(mapped.b);
    assertTrue(mapped.c);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_String() throws Exception {
    class Mapped {

      @Bin
      String a;
      @Bin
      String b;
    }
    final Mapped mapped = JBBPParser.prepare("stringj a; stringj b;").parse(new byte[] {3, 65, 66, 67, 2, 68, 69}).mapTo(Mapped.class);
    assertEquals("ABC", mapped.a);
    assertEquals("DE", mapped.b);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_StringArrayToStringArray() throws Exception {
    class Mapped {

      @Bin
      String[] a;
    }
    final Mapped mapped = JBBPParser.prepare("stringj [_] a;").parse(new byte[] {3, 65, 66, 67, 2, 68, 69}).mapTo(Mapped.class);
    assertArrayEquals(new String[] {"ABC", "DE"}, mapped.a);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_IgnoreStaticField() throws Exception {
    final MappedWithStaticField mapped = JBBPParser.prepare("int a;").parse(new byte[] {1, 2, 3, 4}).mapTo(MappedWithStaticField.class);
    assertEquals(0x01020304, mapped.a);
    assertEquals(111, MappedWithStaticField.ignored);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_Bit() throws Exception {
    class Mapped {

      @Bin(type = BinType.BIT)
      byte a;
      @Bin(type = BinType.BIT)
      byte b;
      @Bin(type = BinType.BIT)
      byte c;
    }
    final Mapped mapped = JBBPParser.prepare("bit:3 a; bit:2 b; bit:3 c; ").parse(new byte[] {(byte) 0xDD}).mapTo(Mapped.class);
    assertEquals(5, mapped.a);
    assertEquals(3, mapped.b);
    assertEquals(6, mapped.c);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_Int() throws Exception {
    class Mapped {

      @Bin
      int a;
    }
    assertEquals(0x01020304, JBBPParser.prepare("int a;").parse(new byte[] {1, 2, 3, 4}).mapTo(Mapped.class).a);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_MapIntToFloat() throws Exception {
    class Mapped {

      @Bin(type = BinType.INT)
      float a;
    }

    final byte[] max = JBBPOut.BeginBin().Int(Float.floatToIntBits(Float.MAX_VALUE)).End().toByteArray();
    assertEquals(Float.MAX_VALUE, JBBPParser.prepare("int a;").parse(max).mapTo(Mapped.class).a, 0.005d);
    final byte[] min = JBBPOut.BeginBin().Int(Float.floatToIntBits(Float.MIN_VALUE)).End().toByteArray();
    assertEquals(Float.MIN_VALUE, JBBPParser.prepare("int a;").parse(min).mapTo(Mapped.class).a, 0.005d);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_MapIntArrayToFloatArray() throws Exception {
    class Mapped {

      @Bin(type = BinType.INT_ARRAY)
      float[] a;
    }

    final byte[] max = JBBPOut.BeginBin().Float(Float.MAX_VALUE, Float.MIN_VALUE).End().toByteArray();
    final Mapped result = JBBPParser.prepare("int [_] a;").parse(max).mapTo(Mapped.class);
    assertEquals(2, result.a.length);
    assertEquals(Float.MAX_VALUE, result.a[0], TestUtils.FLOAT_DELTA);
    assertEquals(Float.MIN_VALUE, result.a[1], TestUtils.FLOAT_DELTA);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_MapLongArrayToDoubleArray() throws Exception {
    class Mapped {

      @Bin(type = BinType.LONG_ARRAY)
      double[] a;
    }

    final byte[] max = JBBPOut.BeginBin().Double(Double.MAX_VALUE, Double.MIN_VALUE).End().toByteArray();
    final Mapped result = JBBPParser.prepare("long [_] a;").parse(max).mapTo(Mapped.class);
    assertEquals(2, result.a.length);
    assertEquals(Double.MAX_VALUE, result.a[0], TestUtils.FLOAT_DELTA);
    assertEquals(Double.MIN_VALUE, result.a[1], TestUtils.FLOAT_DELTA);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_MapFloatToFloat() throws Exception {
    class Mapped {

      @Bin
      float a;
    }

    final byte[] max = JBBPOut.BeginBin().Float(-1.234567f).End().toByteArray();
    assertEquals(-1.234567f, JBBPParser.prepare("floatj a;").parse(max).mapTo(Mapped.class).a, TestUtils.FLOAT_DELTA);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_MapFloatArrayToFloatArray() throws Exception {
    class Mapped {

      @Bin
      float[] a;
    }

    final byte[] max = JBBPOut.BeginBin().Float(-1.234567f, 1.234567f).End().toByteArray();
    assertArrayEquals(new float[] {-1.234567f, 1.234567f}, JBBPParser.prepare("floatj [_] a;").parse(max).mapTo(Mapped.class).a, TestUtils.FLOAT_DELTA);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_MapLongToDouble() throws Exception {
    class Mapped {

      @Bin(type = BinType.LONG)
      double a;
    }

    final byte[] max = JBBPOut.BeginBin().Long(Double.doubleToLongBits(Double.MAX_VALUE)).End().toByteArray();
    assertEquals(Double.MAX_VALUE, JBBPParser.prepare("long a;").parse(max).mapTo(Mapped.class).a, TestUtils.FLOAT_DELTA);
    final byte[] min = JBBPOut.BeginBin().Long(Double.doubleToLongBits(Double.MIN_VALUE)).End().toByteArray();
    assertEquals(Double.MIN_VALUE, JBBPParser.prepare("long a;").parse(min).mapTo(Mapped.class).a, TestUtils.FLOAT_DELTA);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_MapDoubleToDouble() throws Exception {
    class Mapped {

      @Bin
      double a;
    }

    final byte[] max = JBBPOut.BeginBin().Double(-1.2345678912345d).End().toByteArray();
    assertEquals(-1.2345678912345d, JBBPParser.prepare("doublej a;").parse(max).mapTo(Mapped.class).a, TestUtils.FLOAT_DELTA);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_MapDoubleArrayToDoubleArray() throws Exception {
    class Mapped {

      @Bin
      double[] a;
    }

    final byte[] max = JBBPOut.BeginBin().Double(-1.2345678912345d, 45.3334d).End().toByteArray();
    assertArrayEquals(new double[] {-1.2345678912345d, 45.3334d}, JBBPParser.prepare("doublej [_] a;").parse(max).mapTo(Mapped.class).a, TestUtils.FLOAT_DELTA);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_Long() throws Exception {
    class Mapped {

      @Bin
      long a;
    }
    assertEquals(0x0102030405060708L, JBBPParser.prepare("long a;").parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}).mapTo(Mapped.class).a);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_UByte() throws Exception {
    class Mapped {

      @Bin(type = BinType.UBYTE)
      int a;
    }
    assertEquals(0xFE, JBBPParser.prepare("ubyte a;").parse(new byte[] {(byte) 0xFE}).mapTo(Mapped.class).a);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_UShort() throws Exception {
    class Mapped {

      @Bin
      char a;
    }
    assertEquals(0x0102, JBBPParser.prepare("ushort a;").parse(new byte[] {1, 2}).mapTo(Mapped.class).a);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_ByteArray() throws Exception {
    class Mapped {

      @Bin
      byte[] a;
    }
    assertArrayEquals(new byte[] {1, 2, 3, 4}, JBBPParser.prepare("byte [_] a;").parse(new byte[] {1, 2, 3, 4}).mapTo(Mapped.class).a);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_UByteArrayToString() throws Exception {
    class Mapped {

      @Bin(type = BinType.UBYTE_ARRAY)
      String a;
    }
    assertEquals("JFIF", JBBPParser.prepare("ubyte [_] a;").parse(new byte[] {(byte) 0x4A, (byte) 0x46, (byte) 0x49, (byte) 0x46}).mapTo(Mapped.class).a);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_BitArrayToString() throws Exception {
    class Mapped {

      @Bin(type = BinType.BIT_ARRAY)
      String a;
    }
    assertEquals(new String(new char[] {0xA, 0x4, 0x6, 0x4, 0x9, 0x4, 0x6, 0x4}), JBBPParser.prepare("bit:4 [_] a;").parse(new byte[] {(byte) 0x4A, (byte) 0x46, (byte) 0x49, (byte) 0x46}).mapTo(Mapped.class).a);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_BitArrayToStringWhenWholeByte() throws Exception {
    class Mapped {

      @Bin(type = BinType.BIT_ARRAY)
      String a;
    }
    assertEquals(new String(new char[] {0xFF, 0xED, 0x01, 0x36}), JBBPParser.prepare("bit:8 [_] a;").parse(new byte[] {(byte) 0xFF, (byte) 0xED, (byte) 0x01, (byte) 0x36}).mapTo(Mapped.class).a);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_ByteArrayToString() throws Exception {
    class Mapped {

      @Bin(type = BinType.BYTE_ARRAY)
      String a;
    }
    assertEquals("JFIF", JBBPParser.prepare("byte [_] a;").parse(new byte[] {(byte) 0x4A, (byte) 0x46, (byte) 0x49, (byte) 0x46}).mapTo(Mapped.class).a);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_ShortArrayToString() throws Exception {
    class Mapped {

      @Bin(type = BinType.SHORT_ARRAY)
      String a;
    }
    assertEquals("JFIF", JBBPParser.prepare("short [_] a;").parse(new byte[] {0, (byte) 0x4A, 0, (byte) 0x46, 0, (byte) 0x49, 0, (byte) 0x46}).mapTo(Mapped.class).a);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_IntArrayToString_Error() throws Exception {
    class Mapped {

      @Bin(type = BinType.INT_ARRAY)
      String a;
    }

    assertThrows(JBBPMapperException.class, new Executable() {
      @Override
      public void execute() throws Throwable {
        JBBPParser.prepare("int [_] a;").parse(new byte[] {0, (byte) 0x4A, 0, (byte) 0x46, 0, (byte) 0x49, 0, (byte) 0x46}).mapTo(Mapped.class);
      }
    });
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_UShortArrayToString() throws Exception {
    class Mapped {

      @Bin(type = BinType.USHORT_ARRAY)
      String a;
    }
    assertEquals("JFIF", JBBPParser.prepare("ushort [_] a;").parse(new byte[] {0, (byte) 0x4A, 0, (byte) 0x46, 0, (byte) 0x49, 0, (byte) 0x46}).mapTo(Mapped.class).a);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_BitArray() throws Exception {
    class Mapped {

      @Bin(type = BinType.BIT_ARRAY)
      byte[] a;
    }
    assertArrayEquals(new byte[] {2, 0, 3, 2}, JBBPParser.prepare("bit:2 [_] a;").parse(new byte[] {(byte) 0xB2}).mapTo(Mapped.class).a);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_ShortArray() throws Exception {
    class Mapped {

      @Bin
      short[] a;
    }
    assertArrayEquals(new short[] {0x0102, 0x0304}, JBBPParser.prepare("short [_] a;").parse(new byte[] {1, 2, 3, 4}).mapTo(Mapped.class).a);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_BoolArray() throws Exception {
    class Mapped {

      @Bin
      boolean[] a;
    }
    final Mapped mapped = JBBPParser.prepare("bool [_] a;").parse(new byte[] {1, 0, 0, 4, 8, 0}).mapTo(Mapped.class);
    assertEquals(6, mapped.a.length);
    assertTrue(mapped.a[0]);
    assertFalse(mapped.a[1]);
    assertFalse(mapped.a[2]);
    assertTrue(mapped.a[3]);
    assertTrue(mapped.a[4]);
    assertFalse(mapped.a[5]);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_UShortArray() throws Exception {
    class Mapped {

      @Bin
      char[] a;
    }
    assertArrayEquals(new char[] {0x0102, 0x0304}, JBBPParser.prepare("ushort [_] a;").parse(new byte[] {1, 2, 3, 4}).mapTo(Mapped.class).a);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_IntArray() throws Exception {
    class Mapped {

      @Bin
      int[] a;
    }
    assertArrayEquals(new int[] {0x01020304, 0x05060708}, JBBPParser.prepare("int [_] a;").parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}).mapTo(Mapped.class).a);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_LongArray() throws Exception {
    class Mapped {

      @Bin
      long[] a;
    }
    assertArrayEquals(new long[] {0x0102030405060708L, 0x1112131415161718L}, JBBPParser.prepare("long [_] a;").parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18}).mapTo(Mapped.class).a);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
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
    final Mapped mapped = JBBPParser.prepare("byte b; a{ int a; }").parse(new byte[] {1, 2, 3, 4, 5}).mapTo(Mapped.class);
    assertEquals(0x02030405, mapped.a.a);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_StructArray() throws Exception {
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
    final Mapped mapped = JBBPParser.prepare("byte b; a [_]{ int a; }").parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9}).mapTo(Mapped.class);
    assertEquals(2, mapped.a.length);
    assertEquals(0x02030405, mapped.a[0].a);
    assertEquals(0x06070809, mapped.a[1].a);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_ErrorForMappingStructureToPrimitiveField() throws Exception {
    class Mapped {

      @Bin(name = "test", type = BinType.STRUCT)
      long a;
    }

    assertThrows(JBBPMapperException.class, new Executable() {
      @Override
      public void execute() throws Throwable {
        JBBPParser.prepare("test { byte [_] a;}").parse(new byte[] {1, 2, 3, 4}).mapTo(Mapped.class);
      }
    });
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_mapInsideStructureDefinedByItsPath() throws Exception {
    class Mapped {

      @Bin
      long a;
    }
    final Mapped mapped = JBBPParser.prepare("byte f; test { inside {long a;} }").parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9}).mapTo("test.inside", Mapped.class);
    assertEquals(0x0203040506070809L, mapped.a);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_mapInsideStructureDefinedByItsPath_ErrorForNonStructure() throws Exception {
    class Mapped {

      @Bin
      long a;
    }
    assertThrows(JBBPMapperException.class, new Executable() {
      @Override
      public void execute() throws Throwable {
        JBBPParser.prepare("byte f; test { inside {long a;} }").parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9}).mapTo("f", Mapped.class);
      }
    });
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_privateFieldInPackagelevelClass() throws Exception {
    final ClassWithPrivateFields fld = JBBPParser.prepare("int field;").parse(new byte[] {1, 2, 3, 4}).mapTo(ClassWithPrivateFields.class);
    assertNull(AccessController.doPrivileged(new PrivilegedAction<Void>() {

      @Override
      public Void run() {
        try {
          final Field field = fld.getClass().getDeclaredField("field");
          field.setAccessible(true);
          assertEquals(0x01020304, field.getInt(fld));
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
        return null;
      }

    }));

  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_customMappingFields_Class() throws Exception {
    final class Mapped {

      @Bin
      int a;
      @Bin(custom = true, extra = "TEST_TEXT")
      String b;
      @Bin
      int c;
    }
    final Mapped mapped = JBBPParser.prepare("int a; int b; int c;").parse(new byte[] {1, 2, 3, 4, 0x4A, 0x46, 0x49, 0x46, 5, 6, 7, 8}).mapTo(Mapped.class, new JBBPMapperCustomFieldProcessor() {

      @Override
      public Object prepareObjectForMapping(final JBBPFieldStruct parsedBlock, final Bin annotation, final Field field) {
        if ("b".equals(field.getName()) && "TEST_TEXT".equals(annotation.extra())) {
          final int bvalue = parsedBlock.findFieldForNameAndType("b", JBBPFieldInt.class).getAsInt();
          return String.valueOf((char) ((bvalue >>> 24) & 0xFF)) + (char) ((bvalue >>> 16) & 0xFF) + (char) ((bvalue >>> 8) & 0xFF) + (char) (bvalue & 0xFF);
        } else {
          fail("Unexpected state" + field);
          return null;
        }
      }
    });

    assertEquals(0x01020304, mapped.a);
    assertEquals("JFIF", mapped.b);
    assertEquals(0x05060708, mapped.c);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_customMappingFields_ClassInstance() throws Exception {
    final class Mapped {

      @Bin
      int a;
      @Bin(custom = true, extra = "TEST_TEXT")
      String b;
      @Bin
      int c;
    }

    final Mapped mapped = new Mapped();

    final Mapped result = (Mapped) JBBPParser.prepare("int a; int b; int c;").parse(new byte[] {1, 2, 3, 4, 0x4A, 0x46, 0x49, 0x46, 5, 6, 7, 8}).mapTo(mapped, new JBBPMapperCustomFieldProcessor() {

      @Override
      public Object prepareObjectForMapping(final JBBPFieldStruct parsedBlock, final Bin annotation, final Field field) {
        if ("b".equals(field.getName()) && "TEST_TEXT".equals(annotation.extra())) {
          final int bvalue = parsedBlock.findFieldForNameAndType("b", JBBPFieldInt.class).getAsInt();
          return String.valueOf((char) ((bvalue >>> 24) & 0xFF)) + (char) ((bvalue >>> 16) & 0xFF) + (char) ((bvalue >>> 8) & 0xFF) + (char) (bvalue & 0xFF);
        } else {
          fail("Unexpected state" + field);
          return null;
        }
      }
    });

    assertSame(mapped, result);

    assertEquals(0x01020304, mapped.a);
    assertEquals("JFIF", mapped.b);
    assertEquals(0x05060708, mapped.c);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_AnnotationForWholeClass() throws Exception {
    @Bin
    final class Parsed {

      int a;
      int b;
      @Bin(type = BinType.BYTE_ARRAY)
      String c;
    }

    final Parsed parsed = JBBPParser.prepare("int a; int b; byte [_] c;").parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd'}).mapTo(Parsed.class);
    assertEquals(0x01020304, parsed.a);
    assertEquals(0x05060708, parsed.b);
    assertEquals("abcd", parsed.c);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_InstanceOfInnerClass() throws Exception {
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

    final Outer newouter = (Outer) JBBPParser.prepare("int value; inner{ byte a; byte b;}").parse(new byte[] {1, 2, 3, 4, 5, 6}).mapTo(oldouter);

    assertSame(oldouter, newouter);
    assertSame(inner, newouter.inner);
    assertEquals(0x01020304, oldouter.value);
    assertEquals(5, inner.a);
    assertEquals(6, inner.b);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_InstanceOfInnerClassPreparedArray() throws Exception {
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

    final Outer newouter = (Outer) JBBPParser.prepare("int value; inner [2] { byte a; byte b;}").parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}).mapTo(oldouter);

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

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_InstanceOfInnerClassNonPreparedArray() throws Exception {
    final class Outer {

      @Bin
      int value;
      @Bin
      Inner[] inner;

      public Outer() {
        inner = new Outer.Inner[2];
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

    final Outer newouter = (Outer) JBBPParser.prepare("int value; inner [2] { byte a; byte b;}").parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}).mapTo(oldouter);

    assertSame(oldouter, newouter);
    assertSame(inner, newouter.inner);
    assertEquals(0x01020304, oldouter.value);
    assertEquals(5, inner[0].a);
    assertEquals(6, inner[0].b);
    assertEquals(7, inner[1].a);
    assertEquals(8, inner[1].b);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_InstanceOfInnerClassNonPreparedArray_ErrorForDifferentSize() throws Exception {
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

    assertThrows(JBBPMapperException.class, new Executable() {
      @Override
      public void execute() throws Throwable {
        JBBPParser.prepare("int value; inner [2] { byte a; byte b;}").parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}).mapTo(new Outer());
      }
    });
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_MapToClassHierarchyWithAnnotationInheritance() throws Exception {
    @Bin
    class Ancestor {

      int a;
    }

    class Successor extends Ancestor {

      int b;
    }

    final Successor successor = JBBPParser.prepare("int a; int b;").parse(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}).mapTo(Successor.class);

    assertEquals(0x01020304, successor.a);
    assertEquals(0x05060708, successor.b);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_MapElementsByTheirPaths() throws Exception {
    class Parsed {

      @Bin(path = "struct.a")
      byte num;
      @Bin(path = "struct.b", type = BinType.BYTE_ARRAY)
      String str;
    }

    final Parsed parsed = JBBPParser.prepare("int start; struct { byte a; byte [3] b; } int end;").parse(new byte[] {1, 2, 3, 4, 5, (byte) 'a', (byte) 'b', (byte) 'c', 6, 7, 8, 9}).mapTo(Parsed.class);
    assertEquals(0x05, parsed.num);
    assertEquals("abc", parsed.str);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_MapElementsByTheirPaths_ErrorForUnknownField() throws Exception {
    class Parsed {

      @Bin(path = "struct.a")
      byte num;
      @Bin(path = "struct.c", type = BinType.BYTE_ARRAY)
      String str;
    }

    assertThrows(JBBPMapperException.class, new Executable() {
      @Override
      public void execute() throws Throwable {
        JBBPParser.prepare("int start; struct { byte a; byte [3] b; } int end;").parse(new byte[] {1, 2, 3, 4, 5, (byte) 'a', (byte) 'b', (byte) 'c', 6, 7, 8, 9}).mapTo(Parsed.class);
      }
    });
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_MapElementsByTheirPaths_ErrorForFieldIncompatibleType() throws Exception {
    class Parsed {

      @Bin(path = "struct.a")
      byte num;
      @Bin(path = "struct.b", type = BinType.UBYTE_ARRAY)
      String str;
    }

    assertThrows(JBBPMapperException.class, new Executable() {
      @Override
      public void execute() throws Throwable {
        JBBPParser.prepare("int start; struct { byte a; byte [3] b; } int end;").parse(new byte[] {1, 2, 3, 4, 5, (byte) 'a', (byte) 'b', (byte) 'c', 6, 7, 8, 9}).mapTo(Parsed.class);
      }
    });
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_MapElementsByTheirPaths_ErrorForFieldIncompatibleType_ArrayMappingField() throws Exception {
    class Parsed {

      @Bin(path = "struct.a", type = BinType.BYTE)
      byte[] num;
      @Bin(path = "struct.b", type = BinType.BYTE_ARRAY)
      String str;
    }

    assertThrows(JBBPMapperException.class, new Executable() {
      @Override
      public void execute() throws Throwable {
        JBBPParser.prepare("int start; struct { byte a; byte [3] b; } int end;").parse(new byte[] {1, 2, 3, 4, 5, (byte) 'a', (byte) 'b', (byte) 'c', 6, 7, 8, 9}).mapTo(Parsed.class);
      }
    });
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_MapElementsByTheirPaths_ErrorForFieldIncompatibleType_ArrayBinField() throws Exception {
    class Parsed {

      @Bin(path = "struct.a")
      byte num;
      @Bin(path = "struct.b", type = BinType.BYTE_ARRAY)
      byte str;
    }

    assertThrows(JBBPMapperException.class, new Executable() {
      @Override
      public void execute() throws Throwable {
        JBBPParser.prepare("int start; struct { byte a; byte [3] b; } int end;").parse(new byte[] {1, 2, 3, 4, 5, (byte) 'a', (byte) 'b', (byte) 'c', 6, 7, 8, 9}).mapTo(Parsed.class);
      }
    });
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_IgnoreMarkedFieldByDefaultIfTransient() throws Exception {
    @Bin
    class Parsed {

      @Bin(path = "struct.a")
      byte num;
      @Bin(path = "struct.b", type = BinType.BYTE_ARRAY)
      String str;
      transient String ignored;
    }

    final Parsed parsed = JBBPParser.prepare("int start; struct { byte a; byte [3] b; } int end;").parse(new byte[] {1, 2, 3, 4, 5, (byte) 'a', (byte) 'b', (byte) 'c', 6, 7, 8, 9}).mapTo(Parsed.class);
    assertEquals(0x05, parsed.num);
    assertEquals("abc", parsed.str);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_IgnoreMarkedFieldForTransient() throws Exception {
    @Bin
    class Parsed {

      @Bin(path = "struct.a")
      byte num;
      @Bin(path = "struct.b", type = BinType.BYTE_ARRAY)
      String str;
      @Bin(path = "struct.c", type = BinType.BYTE_ARRAY)
      transient String ignored;
    }

    final Parsed parsed = JBBPParser.prepare("int start; struct { byte a; byte [3] b; } int end;").parse(new byte[] {1, 2, 3, 4, 5, (byte) 'a', (byte) 'b', (byte) 'c', 6, 7, 8, 9}).mapTo(Parsed.class);
    assertEquals(0x05, parsed.num);
    assertEquals("abc", parsed.str);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_IgnoreNonMarkedField() throws Exception {
    class Parsed {

      @Bin(path = "struct.a")
      byte num;
      @Bin(path = "struct.b", type = BinType.BYTE_ARRAY)
      String str;
      String ignored;
    }

    final Parsed parsed = JBBPParser.prepare("int start; struct { byte a; byte [3] b; } int end;").parse(new byte[] {1, 2, 3, 4, 5, (byte) 'a', (byte) 'b', (byte) 'c', 6, 7, 8, 9}).mapTo(Parsed.class);
    assertEquals(0x05, parsed.num);
    assertEquals("abc", parsed.str);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_Structure_WholeStream_LocalClassesNonDefaultConstructorsAndFinalFields() throws Exception {
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

    final Parsed parsed = JBBPParser.prepare("struct [_] { byte a; byte b; }").parse(new ByteArrayInputStream(array)).mapTo(Parsed.class);
    assertEquals(array.length / 2, parsed.struct.length);

    for (int i = 0; i < array.length; i += 2) {
      final Struct s = parsed.struct[i / 2];
      assertEquals(array[i], s.a);
      assertEquals(array[i + 1], s.b);
    }
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_FieldWithDefinedBitNumberToBitField_FieldPresented() throws Exception {
    class Parsed {

      @Bin(outBitNumber = JBBPBitNumber.BITS_5)
      byte field;
    }
    final Parsed parsed = JBBPParser.prepare("int fieldint; bit:5 field;").parse(new byte[] {1, 2, 3, 4, 0x35}).mapTo(Parsed.class);
    assertEquals(0x15, parsed.field);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_FieldWithDefinedBitNumberToBitField_FieldPresentedWithDifferentBitNumber() throws Exception {
    class Parsed {

      @Bin(outBitNumber = JBBPBitNumber.BITS_5)
      byte field;
    }

    assertThrows(JBBPMapperException.class, new Executable() {
      @Override
      public void execute() throws Throwable {
        JBBPParser.prepare("int fieldint; bit:6 field;").parse(new byte[] {1, 2, 3, 4, 0x35}).mapTo(Parsed.class);
      }
    });
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_ArrayFieldWithDefinedBitNumberToArrayBitField_FieldPresented() throws Exception {
    class Parsed {

      @Bin(outBitNumber = JBBPBitNumber.BITS_4)
      byte[] field;
    }
    final Parsed parsed = JBBPParser.prepare("int fieldint; bit:4 [2] field;").parse(new byte[] {1, 2, 3, 4, 0x35}).mapTo(Parsed.class);
    assertArrayEquals(new byte[] {5, 3}, parsed.field);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_ArrayFieldIgnoredBitNumberFieldForDefinedType() throws Exception {
    class Parsed {

      @Bin(type = BinType.INT_ARRAY, outBitNumber = JBBPBitNumber.BITS_4)
      int[] field;
    }
    final Parsed parsed = JBBPParser.prepare("int fieldint; int [2] field;").parse(new byte[] {1, 2, 3, 4, 0x5, 0x6, 0x7, 0x8, 0x9, 0x0A, 0x0B, 0x0C}).mapTo(Parsed.class);
    assertArrayEquals(new int[] {0x05060708, 0x090A0B0C}, parsed.field);
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_ArrayFieldWithDefinedBitNumberToArrayBitField_FieldPresentedWithDifferentBitNumber() throws Exception {
    class Parsed {

      @Bin(outBitNumber = JBBPBitNumber.BITS_4)
      byte field;
    }
    assertThrows(JBBPMapperException.class, new Executable() {
      @Override
      public void execute() throws Throwable {
        JBBPParser.prepare("int fieldint; bit:3 [2] field;").parse(new byte[] {1, 2, 3, 4, 0x35}).mapTo(Parsed.class);
      }
    });
  }

  @TestTemplate
  @ExtendWith(MapperTestProvider.class)
  public void testMap_IgnoreNotFoundFields() throws Exception {
    class Parsed {

      @Bin
      int a;
      @Bin
      int b;
    }

    final Parsed parsed = JBBPParser.prepare("int a; int b;", JBBPParser.FLAG_SKIP_REMAINING_FIELDS_IF_EOF).parse(new byte[] {1, 2, 3, 4}).mapTo(Parsed.class, JBBPMapper.FLAG_IGNORE_MISSING_VALUES);
    assertEquals(0x01020304, parsed.a);
    assertEquals(0, parsed.b);
  }

  final static class MapperTestProvider implements TestTemplateInvocationContextProvider {

    @Override
    public boolean supportsTestTemplate(final ExtensionContext context) {
      return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(final ExtensionContext context) {
      return Arrays.asList(makeInvocationContext(JBBPUnsafeInstantiator.class.getName()), makeInvocationContext(JBBPSafeInstantiator.class.getName())).stream();
    }

    private TestTemplateInvocationContext makeInvocationContext(final String parameter) {
      return new TestTemplateInvocationContext() {
        @Override
        public String getDisplayName(int invocationIndex) {
          return parameter;
        }

        @Override
        public List<Extension> getAdditionalExtensions() {
          final List<Extension> result = new ArrayList<Extension>();
          result.add(makeParameterResolver(parameter));
          return result;
        }

      };
    }

    private ParameterResolver makeParameterResolver(final String className) {
      return new ParameterResolver() {
        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
          return parameterContext.getParameter()
              .getType()
              .equals(String.class);
        }

        @Override
        public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
          JBBPSystemProperty.PROPERTY_INSTANTIATOR_CLASS.set(className);
          final JBBPClassInstantiator instance = JBBPClassInstantiatorFactory.getInstance().make(JBBPClassInstantiatorType.AUTO);
          JBBPSystemProperty.PROPERTY_INSTANTIATOR_CLASS.remove();
          try {
            TestUtils.injectDeclaredFinalFieldValue(JBBPMapper.class, null, "CLASS_INSTANTIATOR", instance);
          } catch (Exception ex) {
            throw new Error("Can't inject field value", ex);
          }
          return className;
        }
      };
    }
  }

  @Bin
  private static class MappedWithStaticField {

    static int ignored = 111;
    int a;
  }
}
