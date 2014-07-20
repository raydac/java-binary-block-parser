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
import com.igormaznitsa.jbbp.exceptions.JBBPMapperException;
import org.junit.Test;
import static org.junit.Assert.*;

public class JBBPMapperTest {
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
  public void testMap_Long() throws Exception {
    class Mapped {
      @Bin long a;
    }
    assertEquals(0x0102030405060708L, JBBPParser.prepare("long a;").parse(new byte[]{1,2,3,4,5,6,7,8}).mapTo(Mapped.class).a);
  }

  @Test
  public void testMap_UByte() throws Exception {
    class Mapped {
      @Bin(type = BinType.UBYTE) int a;
    };
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
    };
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
}
