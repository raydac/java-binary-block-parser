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

public class JBBPFieldUShortTest {
  
  @Test
  public void testNameField() {
    final JBBPFieldUShort field = new JBBPFieldUShort(new JBBPNamedFieldInfo("test.field", "field", 123), (short) 23456);
    final JBBPNamedFieldInfo namedField = field.getNameInfo();
    assertEquals("test.field", namedField.getFieldPath());
    assertEquals("field", namedField.getFieldName());
    assertEquals(123, namedField.getFieldOffsetInCompiledBlock());
  }

  @Test
  public void testgetAsBool_True() {
    assertTrue(new JBBPFieldUShort(new JBBPNamedFieldInfo("test.field", "field", 123), (short) 32423).getAsBool());
  }

  @Test
  public void testgetAsBool_False() {
    assertFalse(new JBBPFieldUShort(new JBBPNamedFieldInfo("test.field", "field", 123), (short) 0).getAsBool());
  }

  @Test
  public void testgetAsInt() {
    assertEquals(23432, new JBBPFieldUShort(new JBBPNamedFieldInfo("test.field", "field", 123), (short) 23432).getAsInt());
    assertEquals(-23432 & 0xFFFF, new JBBPFieldUShort(new JBBPNamedFieldInfo("test.field", "field", 123), (short) -23432).getAsInt());
  }

  @Test
  public void testgetAsLong() {
    assertEquals(23432L, new JBBPFieldUShort(new JBBPNamedFieldInfo("test.field", "field", 123), (short) 23432).getAsLong());
    assertEquals(-23432L & 0xFFFFL, new JBBPFieldUShort(new JBBPNamedFieldInfo("test.field", "field", 123), (short) -23432).getAsLong());
  }
  
}
