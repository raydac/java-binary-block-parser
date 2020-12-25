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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.Test;

public class BinTypeTest {

  @Test
  public void testNPEInFindCompatibleForNull() {
    assertThrows(NullPointerException.class, () -> BinType.findCompatible(null));
  }

  @Test
  public void testFindCompatibleArray() {
    assertEquals(BinType.BYTE_ARRAY, BinType.findCompatible(byte[].class));
    assertEquals(BinType.USHORT_ARRAY, BinType.findCompatible(char[].class));
    assertEquals(BinType.SHORT_ARRAY, BinType.findCompatible(short[].class));
    assertEquals(BinType.BOOL_ARRAY, BinType.findCompatible(boolean[].class));
    assertEquals(BinType.INT_ARRAY, BinType.findCompatible(int[].class));
    assertEquals(BinType.LONG_ARRAY, BinType.findCompatible(long[].class));
    assertEquals(BinType.STRUCT_ARRAY, BinType.findCompatible(List[].class));
    assertEquals(BinType.FLOAT_ARRAY, BinType.findCompatible(float[].class));
    assertEquals(BinType.DOUBLE_ARRAY, BinType.findCompatible(double[].class));
    assertEquals(BinType.STRING_ARRAY, BinType.findCompatible(String[].class));
  }

  @Test
  public void testFindCompatible() {
    assertEquals(BinType.BYTE, BinType.findCompatible(byte.class));
    assertEquals(BinType.USHORT, BinType.findCompatible(char.class));
    assertEquals(BinType.SHORT, BinType.findCompatible(short.class));
    assertEquals(BinType.BOOL, BinType.findCompatible(boolean.class));
    assertEquals(BinType.INT, BinType.findCompatible(int.class));
    assertEquals(BinType.LONG, BinType.findCompatible(long.class));
    assertEquals(BinType.STRUCT, BinType.findCompatible(BigInteger.class));
    assertEquals(BinType.FLOAT, BinType.findCompatible(float.class));
    assertEquals(BinType.DOUBLE, BinType.findCompatible(double.class));
    assertEquals(BinType.STRING, BinType.findCompatible(String.class));
  }

}
