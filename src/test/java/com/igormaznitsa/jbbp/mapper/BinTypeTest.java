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

import org.junit.Test;
import static org.junit.Assert.*;

public class BinTypeTest {

  @Test(expected = NullPointerException.class)
  public void testNPEInFindCompatibleForNull() {
    BinType.findCompatible(null);
  }
  
  @Test
  public void testFindCompatiblePrimitiveArray() {
    assertEquals(BinType.BYTE_ARRAY, BinType.findCompatible(byte[].class));
    assertEquals(BinType.USHORT_ARRAY, BinType.findCompatible(char[].class));
    assertEquals(BinType.SHORT_ARRAY, BinType.findCompatible(short[].class));
    assertEquals(BinType.BOOL_ARRAY, BinType.findCompatible(boolean[].class));
    assertEquals(BinType.INT_ARRAY, BinType.findCompatible(int[].class));
    assertEquals(BinType.LONG_ARRAY, BinType.findCompatible(long[].class));
    assertEquals(BinType.STRUCT_ARRAY, BinType.findCompatible(String[].class));
    assertEquals(BinType.INT_ARRAY,BinType.findCompatible(float[].class));
    assertEquals(BinType.LONG_ARRAY,BinType.findCompatible(double[].class));
  }
  
  @Test
  public void testFindCompatiblePrimitive() {
    assertEquals(BinType.BYTE, BinType.findCompatible(byte.class));
    assertEquals(BinType.USHORT, BinType.findCompatible(char.class));
    assertEquals(BinType.SHORT, BinType.findCompatible(short.class));
    assertEquals(BinType.BOOL, BinType.findCompatible(boolean.class));
    assertEquals(BinType.INT, BinType.findCompatible(int.class));
    assertEquals(BinType.LONG, BinType.findCompatible(long.class));
    assertEquals(BinType.STRUCT, BinType.findCompatible(String.class));
    assertEquals(BinType.INT,BinType.findCompatible(float.class));
    assertEquals(BinType.LONG,BinType.findCompatible(double.class));
  }
  
}
