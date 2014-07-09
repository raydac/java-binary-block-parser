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

public class JBBPFieldArrayStructTest {

  private final JBBPFieldArrayStruct test = new JBBPFieldArrayStruct(
          new JBBPNamedFieldInfo("test.field", "field", 999), new JBBPFieldStruct[]{
            new JBBPFieldStruct(null, new JBBPAbstractField[0]), 
            new JBBPFieldStruct(null, new JBBPAbstractField[0])
          }
  );

  @Test
  public void testNameAndOffset() {
    assertEquals("test.field", test.getFieldPath());
    assertEquals("field", test.getFieldName());
    assertNotNull(test.getNameInfo());
    assertEquals(999, test.getNameInfo().getFieldOffsetInCompiledBlock());
  }

  @Test
  public void testSize() {
    assertEquals(2, test.size());
  }

  @Test
  public void testGetArray() {
    assertEquals(2, test.getArray().length);
    assertNotNull(test.getArray()[0]);
    assertNotNull(test.getArray()[1]);
  }

  @Test
  public void testGetAsBool() {
    try {
      test.getAsBool(0);
      fail("Must throw UOE");
    }
    catch (UnsupportedOperationException ex) {

    }
  }

  @Test
  public void testGetAsInt() {
    try {
      test.getAsInt(0);
      fail("Must throw UOE");
    }
    catch (UnsupportedOperationException ex) {

    }
  }

  @Test
  public void testGetAsLong() {
    try {
      test.getAsLong(0);
      fail("Must throw UOE");
    }
    catch (UnsupportedOperationException ex) {

    }
  }

  @Test
  public void testIterable() {
    int counter = 0;
    for (final JBBPFieldStruct f : test) {
      assertNotNull(f);
      counter++;
    }
    assertEquals(2, counter);
  }

}
