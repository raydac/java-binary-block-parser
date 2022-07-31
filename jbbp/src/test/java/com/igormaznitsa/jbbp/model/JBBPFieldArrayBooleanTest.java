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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import java.io.Serializable;
import org.junit.jupiter.api.Test;

public class JBBPFieldArrayBooleanTest {

  private final boolean[] array = new boolean[] {true, false, true, true, false};
  private final JBBPFieldArrayBoolean test =
      new JBBPFieldArrayBoolean(new JBBPNamedFieldInfo("test.field", "field", 999), array);

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
    final boolean[] etalon = new boolean[] {true, false, true, true, false};
    final boolean[] array = test.getArray();
    assertEquals(etalon.length, array.length);
    for (int i = 0; i < etalon.length; i++) {
      assertEquals(etalon[i], array[i]);
    }
  }

  @Test
  public void testGetAsBool() {
    final boolean[] etalon = new boolean[] {true, false, true, true, false};
    for (int i = 0; i < etalon.length; i++) {
      assertEquals(etalon[i], test.getAsBool(i));
    }
  }

  @Test
  public void testGetAsInt() {
    final int[] etalon = new int[] {1, 0, 1, 1, 0};
    for (int i = 0; i < etalon.length; i++) {
      assertEquals(etalon[i], test.getAsInt(i));
    }
  }

  @Test
  public void testGetAsLong() {
    final long[] etalon = new long[] {1L, 0L, 1L, 1L, 0L};
    for (int i = 0; i < etalon.length; i++) {
      assertEquals(etalon[i], test.getAsLong(i));
    }
  }

  @Test
  public void testGetElementAt() {
    final boolean[] etalon = new boolean[] {true, false, true, true, false};
    final Serializable payload = new FakePayload();
    test.setPayload(payload);
    for (int i = 0; i < etalon.length; i++) {
      final JBBPFieldBoolean f = test.getElementAt(i);
      assertSame(payload, f.getPayload());
      assertEquals(etalon[i], f.getAsBool());
    }
  }

  @Test
  public void testIterable() {
    final boolean[] etalon = new boolean[] {true, false, true, true, false};
    int index = 0;
    for (final JBBPFieldBoolean f : test) {
      assertEquals(etalon[index++], f.getAsBool());
    }
  }

  @Test
  public void testGetValueArrayAsObject() {
    final boolean[] noninverted = (boolean[]) test.getValueArrayAsObject(false);
    assertEquals(array.length, noninverted.length);
    for (int i = 0; i < array.length; i++) {
      assertEquals(array[i], noninverted[i]);
    }

    final boolean[] inverted = (boolean[]) test.getValueArrayAsObject(true);
    assertEquals(array.length, inverted.length);
    for (int i = 0; i < array.length; i++) {
      assertEquals(JBBPFieldBoolean.reverseBits(array[i]) != 0L, inverted[i]);
    }
  }

}
