/* 
 * Copyright 2014 Igor Maznitsa (http://www.igormaznitsa.com).
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

import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import org.junit.Test;
import static org.junit.Assert.*;

public class JBBPFieldLongTest {

    @Test
  public void testNameField() {
    final JBBPFieldLong field = new JBBPFieldLong(new JBBPNamedFieldInfo("test.field", "field", 123), 123456L);
    final JBBPNamedFieldInfo namedField = field.getNameInfo();
    assertEquals("test.field", namedField.getFieldPath());
    assertEquals("field", namedField.getFieldName());
    assertEquals(123, namedField.getFieldOffsetInCompiledBlock());
  }

  @Test
  public void testgetAsBool_True() {
    assertTrue(new JBBPFieldLong(new JBBPNamedFieldInfo("test.field", "field", 123), 32423L).getAsBool());
  }

  @Test
  public void testgetAsBool_False() {
    assertFalse(new JBBPFieldLong(new JBBPNamedFieldInfo("test.field", "field", 123), 0L).getAsBool());
  }

  @Test
  public void testgetAsInt() {
    assertEquals((int)23432498237439L, new JBBPFieldLong(new JBBPNamedFieldInfo("test.field", "field", 123), 23432498237439L).getAsInt());
    assertEquals((int)-2343249987234L, new JBBPFieldLong(new JBBPNamedFieldInfo("test.field", "field", 123), -2343249987234L).getAsInt());
  }

  @Test
  public void testgetAsLong() {
    assertEquals(23432498237439L, new JBBPFieldLong(new JBBPNamedFieldInfo("test.field", "field", 123), 23432498237439L).getAsLong());
    assertEquals(-2343249987234L, new JBBPFieldLong(new JBBPNamedFieldInfo("test.field", "field", 123), -2343249987234L).getAsLong());
  }

  @Test
  public void testGetAsInvertedBitOrder() {
    assertEquals(0x10E060A020C04080L, new JBBPFieldLong(new JBBPNamedFieldInfo("test.field", "field", 123), 0x0102030405060708L).getAsInvertedBitOrder());
  }

  
}
