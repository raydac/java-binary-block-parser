package com.igormaznitsa.jbbp.utils;

import com.igormaznitsa.jbbp.exceptions.JBBPOutException;
import com.igormaznitsa.jbbp.io.JBBPPackedDecimalType;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class PackedDecimalUtilsTest {

  PackedDecimalUtils pdUtils = new PackedDecimalUtils();

  @Test
  public void testReadPackedDecimal_Unsigned() throws Exception {

    // test Long.MAX_VALUE, with and without trailing UNSIGNED nibble
    assertEquals(9223372036854775807L, pdUtils.readValueFromPackedDecimal(new byte[]{(byte)0x92, 0x23, 0x37, 0x20, 0x36, (byte)0x85, 0x47, 0x75, (byte)0x80, 0x7F}, JBBPPackedDecimalType.UNSIGNED));
    assertEquals(9223372036854775807L, pdUtils.readValueFromPackedDecimal(new byte[]{0x09, 0x22, 0x33, 0x72, 0x03, 0x68, 0x54, 0x77, 0x58, 0x07}, JBBPPackedDecimalType.UNSIGNED));

    // test other values for sign (ignored)
    assertEquals(1L, pdUtils.readValueFromPackedDecimal(new byte[]{0x0, 0x1A}, JBBPPackedDecimalType.UNSIGNED));
    assertEquals(1L, pdUtils.readValueFromPackedDecimal(new byte[]{0x0, 0x1B}, JBBPPackedDecimalType.UNSIGNED));
    assertEquals(1L, pdUtils.readValueFromPackedDecimal(new byte[]{0x0, 0x1C}, JBBPPackedDecimalType.UNSIGNED));
    assertEquals(1L, pdUtils.readValueFromPackedDecimal(new byte[]{0x0, 0x1D}, JBBPPackedDecimalType.UNSIGNED));
    assertEquals(1L, pdUtils.readValueFromPackedDecimal(new byte[]{0x0, 0x1E}, JBBPPackedDecimalType.UNSIGNED));

    // test leading zeroes
    assertEquals(1L, pdUtils.readValueFromPackedDecimal(new byte[]{0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x01}, JBBPPackedDecimalType.UNSIGNED));

    // test 0
    assertEquals(0L, pdUtils.readValueFromPackedDecimal(new byte[]{0x0F}, JBBPPackedDecimalType.UNSIGNED));
  }

  @Test
  public void testReadPackedDecimal_Signed() throws Exception {
    // test all positive sign values (0xA, 0xC, 0xE, 0xF)
    assertEquals(9223372036854775807L, pdUtils.readValueFromPackedDecimal(new byte[]{(byte)0x92, 0x23, 0x37, 0x20, 0x36, (byte)0x85, 0x47, 0x75, (byte)0x80, 0x7A}, JBBPPackedDecimalType.SIGNED));
    assertEquals(9223372036854775807L, pdUtils.readValueFromPackedDecimal(new byte[]{(byte)0x92, 0x23, 0x37, 0x20, 0x36, (byte)0x85, 0x47, 0x75, (byte)0x80, 0x7C}, JBBPPackedDecimalType.SIGNED));
    assertEquals(9223372036854775807L, pdUtils.readValueFromPackedDecimal(new byte[]{(byte)0x92, 0x23, 0x37, 0x20, 0x36, (byte)0x85, 0x47, 0x75, (byte)0x80, 0x7E}, JBBPPackedDecimalType.SIGNED));
    assertEquals(9223372036854775807L, pdUtils.readValueFromPackedDecimal(new byte[]{(byte)0x92, 0x23, 0x37, 0x20, 0x36, (byte)0x85, 0x47, 0x75, (byte)0x80, 0x7F}, JBBPPackedDecimalType.SIGNED));

    // test all negative sign values (0xB, 0xD)
    assertEquals(-9223372036854775808L, pdUtils.readValueFromPackedDecimal(new byte[]{(byte)0x92, 0x23, 0x37, 0x20, 0x36, (byte)0x85, 0x47, 0x75, (byte)0x80, (byte)0x8B}, JBBPPackedDecimalType.SIGNED));
    assertEquals(-9223372036854775808L, pdUtils.readValueFromPackedDecimal(new byte[]{(byte)0x92, 0x23, 0x37, 0x20, 0x36, (byte)0x85, 0x47, 0x75, (byte)0x80, (byte)0x8D}, JBBPPackedDecimalType.SIGNED));

    // positive 1, 0
    assertEquals(1L, pdUtils.readValueFromPackedDecimal(new byte[]{0x0, 0x1C}, JBBPPackedDecimalType.SIGNED));
    assertEquals(0L, pdUtils.readValueFromPackedDecimal(new byte[]{0x0, 0x0C}, JBBPPackedDecimalType.SIGNED));

    // negative 1, 0
    assertEquals(-1L, pdUtils.readValueFromPackedDecimal(new byte[]{0x0, 0x1D}, JBBPPackedDecimalType.SIGNED));
    assertEquals(0L, pdUtils.readValueFromPackedDecimal(new byte[]{0x0, 0x0D}, JBBPPackedDecimalType.SIGNED));
  }

  @Test(expected = NumberFormatException.class)
  public void testReadPackedDecimal_Unsigned_OutOfRange() throws Exception {
    pdUtils.readValueFromPackedDecimal(new byte[]{(byte)0x92, 0x23, 0x37, 0x20, 0x36, (byte)0x85, 0x47, 0x75, (byte)0x80, (byte)0x8F}, JBBPPackedDecimalType.UNSIGNED);
  }

  @Test(expected = NumberFormatException.class)
  public void testReadPackedDecimal_Signed_OutOfRange_High() throws Exception {
    pdUtils.readValueFromPackedDecimal(new byte[]{(byte)0x92, 0x23, 0x37, 0x20, 0x36, (byte)0x85, 0x47, 0x75, (byte)0x80, (byte)0x8C}, JBBPPackedDecimalType.SIGNED);
  }

  @Test(expected = NumberFormatException.class)
  public void testReadPackedDecimal_Signed_OutOfRange_Low() throws Exception {
    pdUtils.readValueFromPackedDecimal(new byte[]{(byte)0x92, 0x23, 0x37, 0x20, 0x36, (byte)0x85, 0x47, 0x75, (byte)0x80, (byte)0x9D}, JBBPPackedDecimalType.SIGNED);
  }

  @Test
  public void testWritePackedDecimal_Unsigned() throws Exception {
    assertArrayEquals(new byte[]{0x09, 0x22, 0x33, 0x72, 0x03, 0x68, 0x54, 0x77, 0x58, 0x07}, pdUtils.writeValueToPackedDecimal(10, 9223372036854775807L, JBBPPackedDecimalType.UNSIGNED));
    assertArrayEquals(new byte[]{0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x01}, pdUtils.writeValueToPackedDecimal(10, 1, JBBPPackedDecimalType.UNSIGNED));
    assertArrayEquals(new byte[]{0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x00}, pdUtils.writeValueToPackedDecimal(10, 0, JBBPPackedDecimalType.UNSIGNED));
    assertArrayEquals(new byte[]{(byte)0x99, (byte)0x99}, pdUtils.writeValueToPackedDecimal(2, 9999, JBBPPackedDecimalType.UNSIGNED));
  }

  @Test
  public void testWritePackedDecimal_Signed() throws Exception {
    assertArrayEquals(new byte[]{(byte)0x92, 0x23, 0x37, 0x20, 0x36, (byte)0x85, 0x47, 0x75, (byte)0x80, 0x7C}, pdUtils.writeValueToPackedDecimal(10, 9223372036854775807L, JBBPPackedDecimalType.SIGNED));
    assertArrayEquals(new byte[]{0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x1C}, pdUtils.writeValueToPackedDecimal(10, 1, JBBPPackedDecimalType.SIGNED));
    assertArrayEquals(new byte[]{0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0C}, pdUtils.writeValueToPackedDecimal(10, 0, JBBPPackedDecimalType.SIGNED));
    assertArrayEquals(new byte[]{0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x1D}, pdUtils.writeValueToPackedDecimal(10, -1L, JBBPPackedDecimalType.SIGNED));
    assertArrayEquals(new byte[]{(byte)0x92, 0x23, 0x37, 0x20, 0x36, (byte)0x85, 0x47, 0x75, (byte)0x80, (byte)0x8D}, pdUtils.writeValueToPackedDecimal(10, -9223372036854775808L, JBBPPackedDecimalType.SIGNED));
    assertArrayEquals(new byte[]{0x1C}, pdUtils.writeValueToPackedDecimal(1, 1, JBBPPackedDecimalType.SIGNED));
    assertArrayEquals(new byte[]{0x0C}, pdUtils.writeValueToPackedDecimal(1, 0, JBBPPackedDecimalType.SIGNED));
    assertArrayEquals(new byte[]{0x1D}, pdUtils.writeValueToPackedDecimal(1, -1, JBBPPackedDecimalType.SIGNED));
  }

  @Test(expected = JBBPOutException.class)
  public void testWritePackedDecimal_WrongSign() throws Exception {
    pdUtils.writeValueToPackedDecimal(10, -1L, JBBPPackedDecimalType.UNSIGNED);
  }

  @Test(expected = JBBPOutException.class)
  public void testWritePackedDecimal_Overflow() throws Exception {
    pdUtils.writeValueToPackedDecimal(1, 100L, JBBPPackedDecimalType.UNSIGNED);
  }
}