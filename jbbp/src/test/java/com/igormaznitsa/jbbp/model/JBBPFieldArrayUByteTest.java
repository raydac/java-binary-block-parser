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

package com.igormaznitsa.jbbp.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;


import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import java.io.Serializable;
import org.junit.jupiter.api.Test;

public class JBBPFieldArrayUByteTest {
  private final byte[] array = new byte[] {(byte) -1, 0, 1, 2, 3};
  private final JBBPFieldArrayUByte test =
      new JBBPFieldArrayUByte(new JBBPNamedFieldInfo("test.field", "field", 999), array);

  @Test
  public void testNameAndOffset() {
    assertEquals("test.field", test.getFieldPath());
    assertEquals("field", test.getFieldName());
    assertNotNull(test.getNameInfo());
    assertEquals(999, test.getNameInfo().getFieldOffsetInCompiledBlock());
  }

  @Test
  public void testSize() {
    assertEquals(5, test.size());
  }

  @Test
  public void testGetArray() {
    assertArrayEquals(new byte[] {(byte) -1, 0, 1, 2, 3}, test.getArray());
  }

  @Test
  public void testGetAsBool() {
    final boolean[] etalon = new boolean[] {true, false, true, true, true};
    for (int i = 0; i < etalon.length; i++) {
      assertEquals(etalon[i], test.getAsBool(i));
    }
  }

  @Test
  public void testGetAsInt() {
    final int[] etalon = new int[] {255, 0, 1, 2, 3};
    for (int i = 0; i < etalon.length; i++) {
      assertEquals(etalon[i], test.getAsInt(i));
    }
  }

  @Test
  public void testGetAsLong() {
    final long[] etalon = new long[] {255L, 0L, 1L, 2L, 3L};
    for (int i = 0; i < etalon.length; i++) {
      assertEquals(etalon[i], test.getAsLong(i));
    }
  }

  @Test
  public void testGetElementAt() {
    final int[] etalon = new int[] {255, 0, 1, 2, 3};
    final Serializable payload = new FakePayload();
    test.setPayload(payload);
    for (int i = 0; i < etalon.length; i++) {
      final JBBPFieldUByte f = test.getElementAt(i);
      assertSame(payload, f.getPayload());
      assertEquals(etalon[i], f.getAsInt());
    }
  }

  @Test
  public void testIterable() {
    final int[] etalon = new int[] {255, 0, 1, 2, 3};
    int index = 0;
    for (final JBBPFieldUByte f : test) {
      assertEquals(etalon[index++], f.getAsInt());
    }
  }

  @Test
  public void testGetValueArrayAsObject() {
    assertArrayEquals(array, (byte[]) test.getValueArrayAsObject(false));

    final byte[] inverted = (byte[]) test.getValueArrayAsObject(true);
    assertEquals(array.length, inverted.length);
    for (int i = 0; i < array.length; i++) {
      assertEquals(JBBPFieldByte.reverseBits(array[i]), inverted[i]);
    }
  }
}
