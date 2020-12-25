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
import static org.junit.jupiter.api.Assertions.assertNotSame;


import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import org.junit.jupiter.api.Test;

public class JBBPFieldArrayStructTest {

  private final JBBPFieldArrayStruct test = new JBBPFieldArrayStruct(
      new JBBPNamedFieldInfo("test.field", "field", 999), new JBBPFieldStruct[] {
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
  public void testIterable() {
    int counter = 0;
    for (final JBBPFieldStruct f : test) {
      assertNotNull(f);
      counter++;
    }
    assertEquals(2, counter);
  }

  @Test
  public void testGetValueArrayAsObject() throws Exception {
    final Object resultForFalse = test.getValueArrayAsObject(false);
    final Object resultForTrue = test.getValueArrayAsObject(true);

    assertNotSame(test.getArray(), resultForFalse);
    assertNotSame(test.getArray(), resultForTrue);
    assertNotSame(resultForTrue, resultForFalse);

    assertArrayEquals((Object[]) resultForTrue, test.getArray());
    assertArrayEquals((Object[]) resultForTrue, (Object[]) resultForFalse);
  }
}
