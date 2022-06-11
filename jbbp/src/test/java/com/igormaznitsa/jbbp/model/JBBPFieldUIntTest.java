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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import org.junit.jupiter.api.Test;

public class JBBPFieldUIntTest {

  @Test
  public void testNameField() {
    final JBBPFieldUInt field =
        new JBBPFieldUInt(new JBBPNamedFieldInfo("test.field", "field", 123), 123456);
    final JBBPNamedFieldInfo namedField = field.getNameInfo();
    assertEquals("test.field", namedField.getFieldPath());
    assertEquals("field", namedField.getFieldName());
    assertEquals(123, namedField.getFieldOffsetInCompiledBlock());
  }

  @Test
  public void testGetAsBool_True() {
    assertTrue(
        new JBBPFieldUInt(new JBBPNamedFieldInfo("test.field", "field", 123), 32423).getAsBool());
  }

  @Test
  public void testGetAsBool_False() {
    assertFalse(
        new JBBPFieldUInt(new JBBPNamedFieldInfo("test.field", "field", 123), 0).getAsBool());
  }

  @Test
  public void testGetAsInt() {
    assertEquals(234324,
        new JBBPFieldUInt(new JBBPNamedFieldInfo("test.field", "field", 123), 234324).getAsInt());

    assertThrows(IllegalStateException.class, () ->
        new JBBPFieldUInt(new JBBPNamedFieldInfo("test.field", "field", 123), -234324).getAsInt());
  }

  @Test
  public void testGetAsLong() {
    assertEquals(234324L,
        new JBBPFieldUInt(new JBBPNamedFieldInfo("test.field", "field", 123), 234324).getAsLong());
    assertEquals(4294732972L,
        new JBBPFieldUInt(new JBBPNamedFieldInfo("test.field", "field", 123),
            4294732972L).getAsLong());
  }


  @Test
  public void testGetAsInvertedBitOrder() {
    assertEquals(0x0000000020C04080L,
        new JBBPFieldUInt(new JBBPNamedFieldInfo("test.field", "field", 123), 0x01020304)
            .getAsInvertedBitOrder());
    assertEquals(0x000000007FFFFFFFL,
        new JBBPFieldUInt(new JBBPNamedFieldInfo("test.field", "field", 123), 0xFFFFFFFE)
            .getAsInvertedBitOrder());
    assertEquals(0x0000000080000000L,
        new JBBPFieldUInt(new JBBPNamedFieldInfo("test.field", "field", 123), 0x00000001)
            .getAsInvertedBitOrder());
  }


}
