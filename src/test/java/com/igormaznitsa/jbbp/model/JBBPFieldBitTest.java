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

public class JBBPFieldBitTest {

  @Test(expected = NullPointerException.class)
  public void testConstructor_NPEForNullBitNumber() {
    new JBBPFieldBit(new JBBPNamedFieldInfo("test.field", "field", 123), 123, null);
  }
  
  @Test
  public void testNameField() {
    final JBBPFieldBit field = new JBBPFieldBit(new JBBPNamedFieldInfo("test.field", "field", 123), 123, JBBPBitNumber.BITS_4);
    final JBBPNamedFieldInfo namedField = field.getNameInfo();
    assertEquals("test.field", namedField.getFieldPath());
    assertEquals("field", namedField.getFieldName());
    assertEquals(123, namedField.getFieldOffsetInCompiledBlock());
    assertEquals(JBBPBitNumber.BITS_4, field.getBitWidth());
  }

  @Test
  public void testgetAsBool_True() {
    assertTrue(new JBBPFieldBit(new JBBPNamedFieldInfo("test.field", "field", 123), 123, JBBPBitNumber.BITS_1).getAsBool());
  }

  @Test
  public void testgetAsBool_False() {
    assertFalse(new JBBPFieldBit(new JBBPNamedFieldInfo("test.field", "field", 123), 0, JBBPBitNumber.BITS_1).getAsBool());
  }

  @Test
  public void testgetAsInt() {
    assertEquals(12, new JBBPFieldBit(new JBBPNamedFieldInfo("test.field", "field", 123), 12, JBBPBitNumber.BITS_3).getAsInt());
    assertEquals(-12 & 0xFF, new JBBPFieldBit(new JBBPNamedFieldInfo("test.field", "field", 123), -12, JBBPBitNumber.BITS_4).getAsInt());
  }

  @Test
  public void testgetAsLong() {
    assertEquals(12L, new JBBPFieldBit(new JBBPNamedFieldInfo("test.field", "field", 123), 12, JBBPBitNumber.BITS_6).getAsLong());
    assertEquals(-12L & 0xFFL, new JBBPFieldBit(new JBBPNamedFieldInfo("test.field", "field", 123), -12, JBBPBitNumber.BITS_5).getAsLong());
  }

  @Test
  public void testGetAsInvertedBitOrder(){
    assertEquals(1L, new JBBPFieldBit(new JBBPNamedFieldInfo("test.field", "field", 123), 7, JBBPBitNumber.BITS_1).getAsInvertedBitOrder());
    assertEquals(4L, new JBBPFieldBit(new JBBPNamedFieldInfo("test.field", "field", 123), 1, JBBPBitNumber.BITS_3).getAsInvertedBitOrder());
    assertEquals(0xF0L, new JBBPFieldBit(new JBBPNamedFieldInfo("test.field", "field", 123), 0xF, JBBPBitNumber.BITS_8).getAsInvertedBitOrder());
    assertEquals(0x8FL, new JBBPFieldBit(new JBBPNamedFieldInfo("test.field", "field", 123), 0xFF1, JBBPBitNumber.BITS_8).getAsInvertedBitOrder());
    assertEquals(0x8L, new JBBPFieldBit(new JBBPNamedFieldInfo("test.field", "field", 123), 0xFF1, JBBPBitNumber.BITS_4).getAsInvertedBitOrder());
  }
  
}
