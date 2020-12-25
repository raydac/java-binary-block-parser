package com.igormaznitsa.jbbp.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;


import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import java.io.Serializable;
import org.junit.jupiter.api.Test;

public class JBBPFieldArrayStringTest {
  private final String[] array = new String[] {"012", null, "ABC"};
  private final JBBPFieldArrayString test =
      new JBBPFieldArrayString(new JBBPNamedFieldInfo("test.field", "field", 999), array);

  @Test
  public void testNameAndOffset() {
    assertEquals("test.field", test.getFieldPath());
    assertEquals("field", test.getFieldName());
    assertNotNull(test.getNameInfo());
    assertEquals(999, test.getNameInfo().getFieldOffsetInCompiledBlock());
  }

  @Test
  public void testSize() {
    assertEquals(3, test.size());
  }

  @Test
  public void testGetArray() {
    assertArrayEquals(new String[] {"012", null, "ABC"}, test.getArray());
  }

  @Test
  public void testGetElementAt() {
    final String[] etalon = new String[] {"012", null, "ABC"};
    final Serializable payload = new FakePayload();
    test.setPayload(payload);
    for (int i = 0; i < etalon.length; i++) {
      final JBBPFieldString f = test.getElementAt(i);
      assertSame(payload, f.getPayload());
      assertEquals(etalon[i], f.getAsString());
    }
  }

  @Test
  public void testIterable() {
    final String[] etalon = new String[] {"012", null, "ABC"};
    int index = 0;
    for (final JBBPFieldString f : test) {
      assertEquals(etalon[index++], f.getAsString());
    }
  }

  @Test
  public void testGetValueArrayAsObject() {
    assertArrayEquals(array, (String[]) test.getValueArrayAsObject(false));

    final String[] inverted = (String[]) test.getValueArrayAsObject(true);
    assertEquals(array.length, inverted.length);
    for (int i = 0; i < array.length; i++) {
      assertEquals(JBBPFieldString.reverseBits(array[i]), inverted[i]);
    }
  }

}