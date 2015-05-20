package com.igormaznitsa.jbbp.model;

import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import org.junit.Test;

import static org.junit.Assert.*;

public class JBBPFieldPackedDecimalTest {

  @Test
  public void testNameField() {
    final JBBPFieldPackedDecimal field = new JBBPFieldPackedDecimal(new JBBPNamedFieldInfo("test.field", "field", 123), 123456L);
    final JBBPNamedFieldInfo namedField = field.getNameInfo();
    assertEquals("test.field", namedField.getFieldPath());
    assertEquals("field", namedField.getFieldName());
    assertEquals(123, namedField.getFieldOffsetInCompiledBlock());
  }

  @Test
  public void testgetAsBool_True() {
    assertTrue(new JBBPFieldPackedDecimal(new JBBPNamedFieldInfo("test.field", "field", 123), 32423L).getAsBool());
  }

  @Test
  public void testgetAsBool_False() {
    assertFalse(new JBBPFieldPackedDecimal(new JBBPNamedFieldInfo("test.field", "field", 123), 0L).getAsBool());
  }

  @Test
  public void testgetAsInt() {
    assertEquals((int)23432498237439L, new JBBPFieldPackedDecimal(new JBBPNamedFieldInfo("test.field", "field", 123), 23432498237439L).getAsInt());
    assertEquals((int)-2343249987234L, new JBBPFieldPackedDecimal(new JBBPNamedFieldInfo("test.field", "field", 123), -2343249987234L).getAsInt());
  }

  @Test
  public void testgetAsLong() {
    assertEquals(23432498237439L, new JBBPFieldPackedDecimal(new JBBPNamedFieldInfo("test.field", "field", 123), 23432498237439L).getAsLong());
    assertEquals(-2343249987234L, new JBBPFieldPackedDecimal(new JBBPNamedFieldInfo("test.field", "field", 123), -2343249987234L).getAsLong());
  }

  @Test
  public void testGetAsInvertedBitOrder() {
    assertEquals(0x10E060A020C04080L, new JBBPFieldPackedDecimal(new JBBPNamedFieldInfo("test.field", "field", 123), 0x0102030405060708L).getAsInvertedBitOrder());
  }
}