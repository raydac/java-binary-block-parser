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

package com.igormaznitsa.jbbp.mapper;

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.TestUtils;
import com.igormaznitsa.jbbp.exceptions.JBBPMapperException;
import com.igormaznitsa.jbbp.io.JBBPOut;
import com.igormaznitsa.jbbp.mapper.instantiators.*;
import com.igormaznitsa.jbbp.model.*;
import com.igormaznitsa.jbbp.utils.JBBPSystemProperty;
import java.io.ByteArrayInputStream;
import java.lang.reflect.*;
import java.security.*;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class JBBPMapperTest {
  
  @Parameterized.Parameters
  public static Collection<String[]>data(){
    return Arrays.asList(new String[]{JBBPUnsafeInstantiator.class.getName()},
    new String[]{JBBPSafeInstantiator.class.getName()});
  }
  
  private String className;
  
  public JBBPMapperTest(final String instantiatorClassName){
    this.className = instantiatorClassName;
  }
  
  @Before
  public void injectInstantiator() throws Exception {
    JBBPSystemProperty.PROPERTY_INSTANTIATOR_CLASS.set(this.className);
    final JBBPClassInstantiator instance = JBBPClassInstantiatorFactory.getInstance().make(JBBPClassInstantiatorType.AUTO);
    JBBPSystemProperty.PROPERTY_INSTANTIATOR_CLASS.remove();
    TestUtils.injectDeclaredFinalFieldValue(JBBPMapper.class, null, "klazzInstantiator", instance);
  }
  
  @Test
  public void testMap_InsideStructAndClass() throws Exception {
    class Mapped {
      @Bin byte a;
    }
    assertEquals(3, JBBPParser.prepare("byte a; some{struc {byte a;}}").parse(new byte[]{1,3}).mapTo("some.struc",Mapped.class).a);
  }
  
  @Test
  public void testMap_RootStructToClassWithNullCustomProcessor() throws Exception {
    class Mapped {
      @Bin byte a;
    }
    assertEquals(3, JBBPMapper.map(JBBPParser.prepare("byte a;").parse(new byte[]{3}),Mapped.class).a);
  }
  
  @Test
  public void testMap_Byte() throws Exception {
    class Mapped {
      @Bin byte a;
    }
    assertEquals(3,JBBPParser.prepare("byte a;").parse(new byte[]{3}).mapTo(Mapped.class).a);
  }

  @Test
  public void testMap_Short() throws Exception {
    class Mapped {
      @Bin short a;
    }
    assertEquals(0x0304,JBBPParser.prepare("short a;").parse(new byte[]{3,4}).mapTo(Mapped.class).a);
  }

  @Test
  public void testMap_Boolean() throws Exception {
    class Mapped {
      @Bin boolean a;
      @Bin boolean b;
      @Bin boolean c;
    }
    final Mapped mapped = JBBPParser.prepare("bool a; bool b; bool c;").parse(new byte[]{23, 0, 12}).mapTo(Mapped.class);
    assertTrue(mapped.a);
    assertFalse(mapped.b);
    assertTrue(mapped.c);
  }

  @Test
  public void testMap_Bit() throws Exception {
    class Mapped {
      @Bin(type = BinType.BIT) byte a;
      @Bin(type = BinType.BIT) byte b;
      @Bin(type = BinType.BIT) byte c;
    }
    final Mapped mapped = JBBPParser.prepare("bit:3 a; bit:2 b; bit:3 c; ").parse(new byte[]{(byte)0xDD}).mapTo(Mapped.class);
    assertEquals(5,mapped.a);
    assertEquals(3,mapped.b);
    assertEquals(6,mapped.c);
  }

  @Test
  public void testMap_Int() throws Exception {
    class Mapped {
      @Bin int a;
    }
    assertEquals(0x01020304, JBBPParser.prepare("int a;").parse(new byte[]{1,2,3,4}).mapTo(Mapped.class).a);
  }

  @Test
  public void testMap_MapIntToFloat() throws Exception {
    class Mapped {

      @Bin
      float a;
    }

    final byte[] max = JBBPOut.BeginBin().Int(Float.floatToIntBits(Float.MAX_VALUE)).End().toByteArray();
    assertEquals(Float.MAX_VALUE, JBBPParser.prepare("int a;").parse(max).mapTo(Mapped.class).a, 0.005d);
    final byte[] min = JBBPOut.BeginBin().Int(Float.floatToIntBits(Float.MIN_VALUE)).End().toByteArray();
    assertEquals(Float.MIN_VALUE, JBBPParser.prepare("int a;").parse(min).mapTo(Mapped.class).a, 0.005d);
  }

  @Test
  public void testMap_Long() throws Exception {
    class Mapped {
      @Bin long a;
    }
    assertEquals(0x0102030405060708L, JBBPParser.prepare("long a;").parse(new byte[]{1,2,3,4,5,6,7,8}).mapTo(Mapped.class).a);
  }

  @Test
  public void testMap_MapLongToDouble() throws Exception {
    class Mapped {
      @Bin double a;
    }
    
    final byte [] max = JBBPOut.BeginBin().Long(Double.doubleToLongBits(Double.MAX_VALUE)).End().toByteArray();
    assertEquals(Double.MAX_VALUE, JBBPParser.prepare("long a;").parse(max).mapTo(Mapped.class).a,0.005d);
    final byte [] min = JBBPOut.BeginBin().Long(Double.doubleToLongBits(Double.MIN_VALUE)).End().toByteArray();
    assertEquals(Double.MIN_VALUE, JBBPParser.prepare("long a;").parse(min).mapTo(Mapped.class).a,0.005d);
  }

  @Test
  public void testMap_UByte() throws Exception {
    class Mapped {
      @Bin(type = BinType.UBYTE) int a;
    }
    assertEquals(0xFE,JBBPParser.prepare("ubyte a;").parse(new byte[]{(byte)0xFE}).mapTo(Mapped.class).a);
  }

  @Test
  public void testMap_UShort() throws Exception {
    class Mapped {
      @Bin char a;
    }
    assertEquals(0x0102,JBBPParser.prepare("ushort a;").parse(new byte[]{1,2}).mapTo(Mapped.class).a);
  }

  @Test
  public void testMap_ByteArray() throws Exception {
    class Mapped {
      @Bin byte [] a;
    }
    assertArrayEquals(new byte[]{1,2,3,4},JBBPParser.prepare("byte [_] a;").parse(new byte[]{1,2,3,4}).mapTo(Mapped.class).a);
  }

  @Test
  public void testMap_UByteArrayToString() throws Exception {
    class Mapped {
      @Bin(type = BinType.UBYTE_ARRAY)
      String a;
    }
    assertEquals("JFIF", JBBPParser.prepare("ubyte [_] a;").parse(new byte[]{(byte) 0x4A, (byte) 0x46, (byte) 0x49, (byte) 0x46}).mapTo(Mapped.class).a);
  }

  @Test
  public void testMap_BitArrayToString() throws Exception {
    class Mapped {
      @Bin(type = BinType.BIT_ARRAY) String a;
    }
    assertEquals(new String(new char[]{0xA, 0x4, 0x6, 0x4, 0x9, 0x4, 0x6, 0x4}),JBBPParser.prepare("bit:4 [_] a;").parse(new byte[]{(byte)0x4A, (byte)0x46, (byte)0x49, (byte)0x46}).mapTo(Mapped.class).a);
  }

  @Test
  public void testMap_BitArrayToStringWhenWholeByte() throws Exception {
    class Mapped {
      @Bin(type = BinType.BIT_ARRAY) String a;
    }
    assertEquals(new String(new char[]{0xFF, 0xED, 0x01, 0x36}),JBBPParser.prepare("bit:8 [_] a;").parse(new byte[]{(byte)0xFF, (byte)0xED, (byte)0x01, (byte)0x36}).mapTo(Mapped.class).a);
  }

  @Test
  public void testMap_ByteArrayToString() throws Exception {
    class Mapped {
      @Bin(type = BinType.BYTE_ARRAY) String a;
    }
    assertEquals("JFIF",JBBPParser.prepare("byte [_] a;").parse(new byte[]{(byte)0x4A, (byte)0x46, (byte)0x49, (byte)0x46}).mapTo(Mapped.class).a);
  }

  @Test
  public void testMap_ShortArrayToString() throws Exception {
    class Mapped {
      @Bin(type = BinType.SHORT_ARRAY) String a;
    }
    assertEquals("JFIF",JBBPParser.prepare("short [_] a;").parse(new byte[]{0,(byte)0x4A, 0,(byte)0x46, 0,(byte)0x49, 0,(byte)0x46}).mapTo(Mapped.class).a);
  }

  @Test(expected = JBBPMapperException.class)
  public void testMap_IntArrayToString_Error() throws Exception {
    class Mapped {
      @Bin(type = BinType.INT_ARRAY) String a;
    }
    JBBPParser.prepare("int [_] a;").parse(new byte[]{0,(byte)0x4A, 0,(byte)0x46, 0,(byte)0x49, 0,(byte)0x46}).mapTo(Mapped.class);
  }

  @Test
  public void testMap_UShortArrayToString() throws Exception {
    class Mapped {
      @Bin(type = BinType.USHORT_ARRAY) String a;
    }
    assertEquals("JFIF",JBBPParser.prepare("ushort [_] a;").parse(new byte[]{0,(byte)0x4A, 0,(byte)0x46, 0,(byte)0x49, 0,(byte)0x46}).mapTo(Mapped.class).a);
  }

  @Test
  public void testMap_BitArray() throws Exception {
    class Mapped {
      @Bin(type = BinType.BIT_ARRAY) byte [] a;
    }
    assertArrayEquals(new byte[]{2,0,3,2},JBBPParser.prepare("bit:2 [_] a;").parse(new byte[]{(byte)0xB2}).mapTo(Mapped.class).a);
  }

  @Test
  public void testMap_ShortArray() throws Exception {
    class Mapped {
      @Bin short [] a;
    }
    assertArrayEquals(new short[]{0x0102,0x0304},JBBPParser.prepare("short [_] a;").parse(new byte[]{1,2,3,4}).mapTo(Mapped.class).a);
  }

  @Test
  public void testMap_BoolArray() throws Exception {
    class Mapped {
      @Bin boolean [] a;
    }
    final Mapped mapped = JBBPParser.prepare("bool [_] a;").parse(new byte[]{1, 0, 0, 4, 8, 0}).mapTo(Mapped.class);
    assertEquals(6,mapped.a.length);
    assertTrue(mapped.a[0]);
    assertFalse(mapped.a[1]);
    assertFalse(mapped.a[2]);
    assertTrue(mapped.a[3]);
    assertTrue(mapped.a[4]);
    assertFalse(mapped.a[5]);
  }

  @Test
  public void testMap_UShortArray() throws Exception {
    class Mapped {
      @Bin char[] a;
    }
    assertArrayEquals(new char[]{0x0102, 0x0304}, JBBPParser.prepare("ushort [_] a;").parse(new byte[]{1, 2, 3, 4}).mapTo(Mapped.class).a);
  }

  @Test
  public void testMap_IntArray() throws Exception {
    class Mapped {
      @Bin int[] a;
    }
    assertArrayEquals(new int []{0x01020304, 0x05060708}, JBBPParser.prepare("int [_] a;").parse(new byte[]{1, 2, 3, 4, 5, 6, 7,8}).mapTo(Mapped.class).a);
  }

  @Test
  public void testMap_LongArray() throws Exception {
    class Mapped {
      @Bin long[] a;
    }
    assertArrayEquals(new long []{0x0102030405060708L, 0x1112131415161718L}, JBBPParser.prepare("long [_] a;").parse(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18}).mapTo(Mapped.class).a);
  }

  @Test
  public void testMap_Struct() throws Exception {
    class Inside {
      @Bin int a;
    }
    class Mapped {
      @Bin byte b;
      @Bin Inside a;
    }
    final Mapped mapped = JBBPParser.prepare("byte b; a{ int a; }").parse(new byte[]{1, 2, 3, 4, 5}).mapTo(Mapped.class);
    assertEquals(0x02030405, mapped.a.a);
  }

  @Test
  public void testMap_StructArray() throws Exception {
    class Inside {
      @Bin int a;
    }
    class Mapped {
      @Bin byte b;
      @Bin Inside [] a;
    }
    final Mapped mapped = JBBPParser.prepare("byte b; a [_]{ int a; }").parse(new byte[]{1, 2, 3, 4, 5,6,7,8,9}).mapTo(Mapped.class);
    assertEquals(2, mapped.a.length);
    assertEquals(0x02030405, mapped.a[0].a);
    assertEquals(0x06070809, mapped.a[1].a);
  }
  
  @Test(expected = JBBPMapperException.class)
  public void testMap_ErrorForMappingStructureToPrimitiveField() throws Exception {
    class Mapped {
      @Bin (name = "test", type = BinType.STRUCT) long a;
    }
    JBBPParser.prepare("test { byte [_] a;}").parse(new byte[]{1, 2, 3, 4}).mapTo(Mapped.class);
  }

  @Test
  public void testMap_mapInsideStructureDefinedByItsPath() throws Exception {
    class Mapped {
      @Bin long a;
    }
    final Mapped mapped = JBBPParser.prepare("byte f; test { inside {long a;} }").parse(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9}).mapTo("test.inside",Mapped.class);
    assertEquals(0x0203040506070809L, mapped.a);
  }

  @Test(expected=JBBPMapperException.class)
  public void testMap_mapInsideStructureDefinedByItsPath_ErrorForNonStructure() throws Exception {
    class Mapped {
      @Bin long a;
    }
    JBBPParser.prepare("byte f; test { inside {long a;} }").parse(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9}).mapTo("f",Mapped.class);
  }

  @Test
  public void testMap_privateFieldInPackagelevelClass() throws Exception {
    final ClassWithPrivateFields fld = JBBPParser.prepare("int field;").parse(new byte[]{1, 2, 3, 4}).mapTo(ClassWithPrivateFields.class);
    AccessController.doPrivileged(new PrivilegedAction<Void>(){

      public Void run() {
        try{
          final Field field = fld.getClass().getDeclaredField("field");
          field.setAccessible(true);
          assertEquals(0x01020304, field.getInt(fld));
        }catch(Exception ex){
          throw new RuntimeException(ex);
        }
        return null;
      };
      
    });
    
  }

  @Test
  public void testMap_customMappingFields_Class() throws Exception {
    final class Mapped { @Bin int a; @Bin(custom = true, extra = "TEST_TEXT") String b; @Bin int c;}
    final Mapped mapped = JBBPParser.prepare("int a; int b; int c;").parse(new byte []{1,2,3,4, 0x4A, 0x46, 0x49, 0x46, 5,6,7,8}).mapTo(Mapped.class, new JBBPMapperCustomFieldProcessor() {

      public Object prepareObjectForMapping(final JBBPFieldStruct parsedBlock, final Bin annotation, final Field field) {
        if ("b".equals(field.getName()) && "TEST_TEXT".equals(annotation.extra())){
          final int bvalue = parsedBlock.findFieldForNameAndType("b", JBBPFieldInt.class).getAsInt();
          final StringBuilder result = new StringBuilder();
          result.append((char)((bvalue>>>24)&0xFF)).append((char) ((bvalue >>> 16) & 0xFF)).append((char) ((bvalue >>> 8) & 0xFF)).append((char) (bvalue & 0xFF));
          return result.toString();
        }else{
          fail("Unexpected state"+field);
          return null;
        }
      }
    });
    
    assertEquals(0x01020304, mapped.a);
    assertEquals("JFIF", mapped.b);
    assertEquals(0x05060708, mapped.c);
  }

  @Test
  public void testMap_customMappingFields_ClassInstance() throws Exception {
    final class Mapped { @Bin int a; @Bin(custom = true, extra = "TEST_TEXT") String b; @Bin int c;}

    final Mapped mapped = new Mapped(); 
    
    final Mapped result = (Mapped)JBBPParser.prepare("int a; int b; int c;").parse(new byte []{1,2,3,4, 0x4A, 0x46, 0x49, 0x46, 5,6,7,8}).mapTo(mapped, new JBBPMapperCustomFieldProcessor() {

      public Object prepareObjectForMapping(final JBBPFieldStruct parsedBlock, final Bin annotation, final Field field) {
        if ("b".equals(field.getName()) && "TEST_TEXT".equals(annotation.extra())){
          final int bvalue = parsedBlock.findFieldForNameAndType("b", JBBPFieldInt.class).getAsInt();
          final StringBuilder result = new StringBuilder();
          result.append((char)((bvalue>>>24)&0xFF)).append((char) ((bvalue >>> 16) & 0xFF)).append((char) ((bvalue >>> 8) & 0xFF)).append((char) (bvalue & 0xFF));
          return result.toString();
        }else{
          fail("Unexpected state"+field);
          return null;
        }
      }
    });
    
    assertSame(mapped, result);
    
    assertEquals(0x01020304, mapped.a);
    assertEquals("JFIF", mapped.b);
    assertEquals(0x05060708, mapped.c);
  }

  @Test
  public void testMap_AnnotationForWholeClass() throws Exception {
    @Bin
    final class Parsed{
      int a;
      int b;
      @Bin(type = BinType.BYTE_ARRAY) String c;
    }
  
    final Parsed parsed = JBBPParser.prepare("int a; int b; byte [_] c;").parse(new byte[]{1,2,3,4,5,6,7,8,(byte)'a',(byte)'b',(byte)'c',(byte)'d'}).mapTo(Parsed.class);
    assertEquals(0x01020304, parsed.a);
    assertEquals(0x05060708, parsed.b);
    assertEquals("abcd", parsed.c);
  }
  
  @Test
  public void testMap_InstanceOfInnerClass() throws Exception {
    final class Outer {
      final class Inner {
        @Bin byte a;
        @Bin byte b;
      }
      
      @Bin int val;
      @Bin Inner inner;
      
      public Outer(){
        inner = new Outer.Inner();
      }
    }
    
    final Outer oldouter = new Outer();
    final Outer.Inner inner = oldouter.inner;
    
    final Outer newouter = (Outer)JBBPParser.prepare("int val; inner{ byte a; byte b;}").parse(new byte []{1,2,3,4,5,6}).mapTo(oldouter);
    
    assertSame(oldouter, newouter);
    assertSame(inner, newouter.inner);
    assertEquals(0x01020304, oldouter.val);
    assertEquals(5, inner.a);
    assertEquals(6, inner.b);
  }
  
  @Test
  public void testMap_InstanceOfInnerClassPreparedArray() throws Exception {
    final class Outer {
      final class Inner {
        @Bin byte a;
        @Bin byte b;
      }
      
      @Bin int val;
      @Bin Inner [] inner;
      
      public Outer(){
        inner = new Outer.Inner [2];
        inner[0] = new Outer.Inner();
        inner[1] = new Outer.Inner();
      }
    }
    
    final Outer oldouter = new Outer();
    final Outer.Inner [] inner = oldouter.inner;
    final Outer.Inner inner0 = oldouter.inner[0];
    final Outer.Inner inner1 = oldouter.inner[1];
    
    final Outer newouter = (Outer)JBBPParser.prepare("int val; inner [2] { byte a; byte b;}").parse(new byte []{1,2,3,4,5,6,7,8}).mapTo(oldouter);
    
    assertSame(oldouter, newouter);
    assertSame(inner, newouter.inner);
    assertSame(inner0, newouter.inner[0]);
    assertSame(inner1, newouter.inner[1]);
    assertEquals(0x01020304, oldouter.val);
    assertEquals(5, inner[0].a);
    assertEquals(6, inner[0].b);
    assertEquals(7, inner[1].a);
    assertEquals(8, inner[1].b);
  }
  
  @Test
  public void testMap_InstanceOfInnerClassNonPreparedArray() throws Exception {
    final class Outer {
      final class Inner {
        @Bin byte a;
        @Bin byte b;
      }
      
      @Bin int val;
      @Bin Inner [] inner;
      
      public Outer(){
        inner = new Outer.Inner [2];
      }
    }
    
    final Outer oldouter = new Outer();
    final Outer.Inner [] inner = oldouter.inner;
    assertNull(inner[0]);
    assertNull(inner[1]);
    
    final Outer newouter = (Outer)JBBPParser.prepare("int val; inner [2] { byte a; byte b;}").parse(new byte []{1,2,3,4,5,6,7,8}).mapTo(oldouter);
    
    assertSame(oldouter, newouter);
    assertSame(inner, newouter.inner);
    assertEquals(0x01020304, oldouter.val);
    assertEquals(5, inner[0].a);
    assertEquals(6, inner[0].b);
    assertEquals(7, inner[1].a);
    assertEquals(8, inner[1].b);
  }
  
  @Test(expected = JBBPMapperException.class)
  public void testMap_InstanceOfInnerClassNonPreparedArray_ErrorForDifferentSize() throws Exception {
    final class Outer {
      final class Inner {
        @Bin byte a;
        @Bin byte b;
      }
      
      @Bin int val;
      @Bin Inner [] inner;
      
      public Outer(){
        inner = new Outer.Inner [3];
      }
    }
    
    JBBPParser.prepare("int val; inner [2] { byte a; byte b;}").parse(new byte []{1,2,3,4,5,6,7,8}).mapTo(new Outer());
  }
  
  @Test
  public void testMap_MapToClassHierarchyWithAnnotationInheritance() throws Exception {
    @Bin
    class Ancestor {
      int a;
    }

    class Successor extends Ancestor {
      int b;
    }
    
    final Successor successor = JBBPParser.prepare("int a; int b;").parse(new byte []{1,2,3,4,5,6,7,8}).mapTo(Successor.class);
    
    assertEquals(0x01020304, successor.a);
    assertEquals(0x05060708, successor.b);
  }
  
  @Test
  public void testMap_MapElementsByTheirPaths() throws Exception {
    class Parsed {
      @Bin(path = "struct.a") byte num;
      @Bin(path = "struct.b",type = BinType.BYTE_ARRAY) String str;
    }
    
    final Parsed parsed = JBBPParser.prepare("int start; struct { byte a; byte [3] b; } int end;").parse(new byte[]{1,2,3,4, 5, (byte)'a', (byte)'b', (byte)'c', 6,7,8,9}).mapTo(Parsed.class);
    assertEquals(0x05, parsed.num);
    assertEquals("abc", parsed.str);
  }
  
  @Test(expected = JBBPMapperException.class)
  public void testMap_MapElementsByTheirPaths_ErrorForUnknownField() throws Exception {
    class Parsed {
      @Bin(path = "struct.a") byte num;
      @Bin(path = "struct.c",type = BinType.BYTE_ARRAY) String str;
    }
    
    JBBPParser.prepare("int start; struct { byte a; byte [3] b; } int end;").parse(new byte[]{1,2,3,4, 5, (byte)'a', (byte)'b', (byte)'c', 6,7,8,9}).mapTo(Parsed.class);
  }
  
  @Test(expected = JBBPMapperException.class)
  public void testMap_MapElementsByTheirPaths_ErrorForFieldIncompatibleType() throws Exception {
    class Parsed {
      @Bin(path = "struct.a") byte num;
      @Bin(path = "struct.b",type = BinType.UBYTE_ARRAY) String str;
    }
    
    JBBPParser.prepare("int start; struct { byte a; byte [3] b; } int end;").parse(new byte[]{1,2,3,4, 5, (byte)'a', (byte)'b', (byte)'c', 6,7,8,9}).mapTo(Parsed.class);
  }
  
  @Test(expected = JBBPMapperException.class)
  public void testMap_MapElementsByTheirPaths_ErrorForFieldIncompatibleType_ArrayMappingField() throws Exception {
    class Parsed {
      @Bin(path = "struct.a", type = BinType.BYTE) byte [] num;
      @Bin(path = "struct.b", type = BinType.BYTE_ARRAY) String str;
    }
    
    JBBPParser.prepare("int start; struct { byte a; byte [3] b; } int end;").parse(new byte[]{1,2,3,4, 5, (byte)'a', (byte)'b', (byte)'c', 6,7,8,9}).mapTo(Parsed.class);
  }
  
  @Test(expected = JBBPMapperException.class)
  public void testMap_MapElementsByTheirPaths_ErrorForFieldIncompatibleType_ArrayBinField() throws Exception {
    class Parsed {
      @Bin(path = "struct.a") byte num;
      @Bin(path = "struct.b", type = BinType.BYTE_ARRAY) byte str;
    }
    
    JBBPParser.prepare("int start; struct { byte a; byte [3] b; } int end;").parse(new byte[]{1,2,3,4, 5, (byte)'a', (byte)'b', (byte)'c', 6,7,8,9}).mapTo(Parsed.class);
  }
  
  @Test
  public void testMap_IgnoreMarkedFieldByDefaultIfTransient() throws Exception {
    @Bin
    class Parsed {
      @Bin(path = "struct.a") byte num;
      @Bin(path = "struct.b", type = BinType.BYTE_ARRAY) String str;
      transient String ignored;
    }

    final Parsed parsed = JBBPParser.prepare("int start; struct { byte a; byte [3] b; } int end;").parse(new byte[]{1, 2, 3, 4, 5, (byte) 'a', (byte) 'b', (byte) 'c', 6, 7, 8, 9}).mapTo(Parsed.class);
    assertEquals(0x05, parsed.num);
    assertEquals("abc", parsed.str);
  }
  
  @Test
  public void testMap_IgnoreMarkedFieldForTransient() throws Exception {
    @Bin
    class Parsed {
      @Bin(path = "struct.a") byte num;
      @Bin(path = "struct.b", type = BinType.BYTE_ARRAY) String str;
      @Bin(path = "struct.c", type = BinType.BYTE_ARRAY) transient String ignored;
    }

    final Parsed parsed = JBBPParser.prepare("int start; struct { byte a; byte [3] b; } int end;").parse(new byte[]{1, 2, 3, 4, 5, (byte) 'a', (byte) 'b', (byte) 'c', 6, 7, 8, 9}).mapTo(Parsed.class);
    assertEquals(0x05, parsed.num);
    assertEquals("abc", parsed.str);
  }
  
  @Test
  public void testMap_IgnoreNonMarkedField() throws Exception {
    class Parsed {
      @Bin(path = "struct.a") byte num;
      @Bin(path = "struct.b", type = BinType.BYTE_ARRAY) String str;
      String ignored;
    }

    final Parsed parsed = JBBPParser.prepare("int start; struct { byte a; byte [3] b; } int end;").parse(new byte[]{1, 2, 3, 4, 5, (byte) 'a', (byte) 'b', (byte) 'c', 6, 7, 8, 9}).mapTo(Parsed.class);
    assertEquals(0x05, parsed.num);
    assertEquals("abc", parsed.str);
  }
  
  @Test
  public void testMap_Structure_WholeStream_LocalClassesNonDefaultConstructorsAndFinalFields() throws Exception {
    @Bin
    class Struct {
      final byte a;
      final byte b;
      
      Struct(byte a, byte b){
        this.a = a;
        this.b = b;
      }
    }
    
    @Bin
    class Parsed {
      final Struct [] struct;
      
      Parsed(Struct [] s){
        this.struct = s;
      }
    }

    final Random rnd = new Random(1234);
    final byte [] array = new byte [1024];
    rnd.nextBytes(array);
    
    final Parsed parsed = JBBPParser.prepare("struct [_] { byte a; byte b; }").parse(new ByteArrayInputStream(array)).mapTo(Parsed.class);
    assertEquals(array.length/2, parsed.struct.length);
  
    for(int i=0; i < array.length; i+=2){
      final Struct s = parsed.struct[i/2];
      assertEquals(array[i],s.a);
      assertEquals(array[i+1],s.b);
    }
  }
}
