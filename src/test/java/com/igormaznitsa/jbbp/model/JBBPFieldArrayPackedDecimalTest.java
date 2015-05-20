package com.igormaznitsa.jbbp.model;

import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import org.junit.Test;

import static org.junit.Assert.*;

public class JBBPFieldArrayPackedDecimalTest {
  private final long [] array = new long[]{-278349872364L, 12223423987439324L, 0L, -2782346872343L, 37238468273412L};
  private final JBBPFieldArrayPackedDecimal test = new JBBPFieldArrayPackedDecimal(new JBBPNamedFieldInfo("test.field", "field", 999), array);

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
    assertArrayEquals(new long[]{-278349872364L, 12223423987439324L, 0L, -2782346872343L, 37238468273412L}, test.getArray());
  }

  @Test
  public void testGetAsBool() {
    final boolean[] etalon = new boolean[]{true, true, false, true, true};
    for (int i = 0; i < etalon.length; i++) {
      assertEquals(etalon[i], test.getAsBool(i));
    }
  }

  @Test
  public void testGetAsInt() {
    final int[] etalon = new int[]{(int)-278349872364L, (int)12223423987439324L, (int)0L, (int)-2782346872343L, (int)37238468273412L};
    for (int i = 0; i < etalon.length; i++) {
      assertEquals(etalon[i], test.getAsInt(i));
    }
  }

  @Test
  public void testGetAsLong() {
    final long[] etalon = new long[]{-278349872364L, 12223423987439324L, 0L, -2782346872343L, 37238468273412L};
    for (int i = 0; i < etalon.length; i++) {
      assertEquals(etalon[i], test.getAsLong(i));
    }
  }

  @Test
  public void testIterable() {
    final long[] etalon = new long[]{-278349872364L, 12223423987439324L, 0L, -2782346872343L, 37238468273412L};
    int index = 0;
    for (final JBBPFieldPackedDecimal f : test) {
      assertEquals(etalon[index++], f.getAsLong());
    }
  }

  @Test
  public void testGetValueArrayAsObject() {
    assertArrayEquals(array, (long[]) test.getValueArrayAsObject(false));

    final long[] inverted = (long[]) test.getValueArrayAsObject(true);
    assertEquals(array.length, inverted.length);
    for (int i = 0; i < array.length; i++) {
      assertEquals(JBBPFieldPackedDecimal.reverseBits(array[i]), inverted[i]);
    }
  }

}