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
import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import org.junit.Test;
import static org.junit.Assert.*;

public class JBBPFieldBooleanTest {

  @Test
  public void testNameField() {
    final JBBPFieldBoolean field = new JBBPFieldBoolean(new JBBPNamedFieldInfo("test.field", "field", 123), true);
    final JBBPNamedFieldInfo namedField = field.getNameInfo();
    assertEquals("test.field", namedField.getFieldPath());
    assertEquals("field", namedField.getFieldName());
    assertEquals(123, namedField.getFieldOffsetInCompiledBlock());
  }

  @Test
  public void testgetAsBool_True() {
    assertTrue(new JBBPFieldBoolean(new JBBPNamedFieldInfo("test.field", "field", 123), true).getAsBool());
  }

  @Test
  public void testgetAsBool_False() {
    assertFalse(new JBBPFieldBoolean(new JBBPNamedFieldInfo("test.field", "field", 123), false).getAsBool());
  }

  @Test
  public void testgetAsInt() {
    assertEquals(1, new JBBPFieldBoolean(new JBBPNamedFieldInfo("test.field", "field", 123), true).getAsInt());
    assertEquals(0, new JBBPFieldBoolean(new JBBPNamedFieldInfo("test.field", "field", 123), false).getAsInt());
  }

  @Test
  public void testgetAsLong() {
    assertEquals(1L, new JBBPFieldBoolean(new JBBPNamedFieldInfo("test.field", "field", 123), true).getAsLong());
    assertEquals(0L & 0xFFL, new JBBPFieldBoolean(new JBBPNamedFieldInfo("test.field", "field", 123), false).getAsLong());
  }

  @Test
  public void testGetAsInvertedBitOrder() {
    assertEquals(0L, new JBBPFieldBoolean(new JBBPNamedFieldInfo("test.field", "field", 123), false).getAsInvertedBitOrder());
    assertEquals(1L, new JBBPFieldBoolean(new JBBPNamedFieldInfo("test.field", "field", 123), true).getAsInvertedBitOrder());
  }
  
}
