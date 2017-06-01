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

import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import org.junit.Test;
import static org.junit.Assert.*;

public class JBBPFieldByteTest {
  
  @Test
  public void testNameField() {
    final JBBPFieldByte field = new JBBPFieldByte(new JBBPNamedFieldInfo("test.field", "field", 123), (byte)123);
    final JBBPNamedFieldInfo namedField = field.getNameInfo();
    assertEquals("test.field", namedField.getFieldPath());
    assertEquals("field", namedField.getFieldName());
    assertEquals(123, namedField.getFieldOffsetInCompiledBlock());
  }

  @Test
  public void testgetAsBool_True() {
    assertTrue(new JBBPFieldByte(new JBBPNamedFieldInfo("test.field", "field", 123), (byte)123).getAsBool());
  }

  @Test
  public void testgetAsBool_False() {
    assertFalse(new JBBPFieldByte(new JBBPNamedFieldInfo("test.field", "field", 123), (byte)0).getAsBool());
  }

  @Test
  public void testgetAsInt() {
    assertEquals(12, new JBBPFieldByte(new JBBPNamedFieldInfo("test.field", "field", 123), (byte)12).getAsInt());
    assertEquals(-12, new JBBPFieldByte(new JBBPNamedFieldInfo("test.field", "field", 123), (byte)-12).getAsInt());
  }

  @Test
  public void testgetAsLong() {
    assertEquals(12L, new JBBPFieldByte(new JBBPNamedFieldInfo("test.field", "field", 123), (byte)12).getAsLong());
    assertEquals(-12L, new JBBPFieldByte(new JBBPNamedFieldInfo("test.field", "field", 123), (byte)-12).getAsLong());
  }

  @Test
  public void testGetAsInvertedBitOrder() {
    assertEquals(0xFFFFFFFFFFFFFF80L, new JBBPFieldByte(new JBBPNamedFieldInfo("test.field", "field", 123), (byte)1).getAsInvertedBitOrder());
    assertEquals(0xFFFFFFFFFFFFFFE0L, new JBBPFieldByte(new JBBPNamedFieldInfo("test.field", "field", 123), (byte)7).getAsInvertedBitOrder());
    assertEquals(0x0FL, new JBBPFieldByte(new JBBPNamedFieldInfo("test.field", "field", 123), (byte)0xF0).getAsInvertedBitOrder());
  }

}
