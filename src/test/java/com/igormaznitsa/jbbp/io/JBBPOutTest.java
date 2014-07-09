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
package com.igormaznitsa.jbbp.io;

import static com.igormaznitsa.jbbp.io.JBBPOut.*;
import java.io.*;
import static org.junit.Assert.*;
import org.junit.Test;

public class JBBPOutTest {

  @Test
  public void testBeginBin() throws Exception {
    assertArrayEquals(new byte[]{1},BeginBin().Byte(1).End().toByteArray());
    assertArrayEquals(new byte[]{0x02,0x01},BeginBin(JBBPByteOrder.LITTLE_ENDIAN).Short(0x0102).End().toByteArray());
    assertArrayEquals(new byte[]{0x40,(byte)0x80},BeginBin(JBBPByteOrder.LITTLE_ENDIAN,JBBPBitOrder.MSB0).Short(0x0102).End().toByteArray());
    assertArrayEquals(new byte[]{(byte)0x80},BeginBin(JBBPBitOrder.MSB0).Byte(1).End().toByteArray());
  
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    assertSame(buffer, BeginBin(buffer).End());
  }

  @Test
  public void testSkip() throws Exception {
    assertArrayEquals(new byte[]{(byte) 0x01, (byte) 0xFF}, JBBPOut.BeginBin().Bit(1).Skip(0).Byte(0xFF).End().toByteArray());
    assertArrayEquals(new byte[]{(byte) 0x01, 0x00, (byte) 0xFF}, JBBPOut.BeginBin().Bit(1).Skip(1).Byte(0xFF).End().toByteArray());
    assertArrayEquals(new byte[]{(byte) 0x01, 0x00, 0x00, (byte) 0xFF}, JBBPOut.BeginBin().Bit(1).Skip(2).Byte(0xFF).End().toByteArray());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSkip_ErrorForNegativeValue() throws Exception {
    JBBPOut.BeginBin().Skip(-1);
  }

  
  @Test
  public void testAlign() throws Exception {
    assertArrayEquals(new byte[]{(byte) 0x01, (byte) 0xFF}, JBBPOut.BeginBin().Bit(1).Align().Byte(0xFF).End().toByteArray());
  }

  @Test
  public void testAlignWithArgument() throws Exception {
    assertEquals(0, JBBPOut.BeginBin().Align(2).End().toByteArray().length);
    assertArrayEquals(new byte[]{(byte) 0x01, (byte) 0xFF}, JBBPOut.BeginBin().Bit(1).Align(1).Byte(0xFF).End().toByteArray());
    assertArrayEquals(new byte[]{(byte) 0xFF}, JBBPOut.BeginBin().Align(3).Byte(0xFF).End().toByteArray());
    assertArrayEquals(new byte[]{0x01, (byte) 0xFF}, JBBPOut.BeginBin().Bit(1).Align(1).Byte(0xFF).End().toByteArray());
    assertArrayEquals(new byte[]{0x01, 0x00, (byte) 0xFF}, JBBPOut.BeginBin().Bit(1).Align(2).Byte(0xFF).End().toByteArray());
    assertArrayEquals(new byte[]{0x01, 0x00, 0x00, 0x00, (byte) 0xFF}, JBBPOut.BeginBin().Bit(1).Align(4).Byte(0xFF).End().toByteArray());
    assertArrayEquals(new byte[]{0x01, 0x02, 0x00, 0x00, (byte) 0xFF}, JBBPOut.BeginBin().Byte(1, 2).Align(4).Byte(0xFF).End().toByteArray());
    assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x00, 0x00, (byte) 0xFF}, JBBPOut.BeginBin().Byte(1, 2, 3).Align(5).Byte(0xFF).End().toByteArray());
    assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04, 0x00, (byte) 0xFF}, JBBPOut.BeginBin().Byte(1, 2, 3, 4).Align(5).Byte(0xFF).End().toByteArray());
    assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, (byte) 0xFF}, JBBPOut.BeginBin().Byte(1, 2, 3, 4, 5).Align(5).Byte(0xFF).End().toByteArray());
    assertArrayEquals(new byte[]{(byte) 0x01, 0x00, 0x02, 0x00, (byte) 0x03}, JBBPOut.BeginBin().Align(2).Byte(1).Align(2).Byte(2).Align(2).Byte(3).End().toByteArray());
    assertArrayEquals(new byte[]{(byte) 0xF1, 0x00, (byte) 0x01, 0x00, 0x02, 0x00, (byte) 0x03}, JBBPOut.BeginBin().Byte(0xF1).Align(2).Byte(1).Align(2).Byte(2).Align(2).Byte(3).End().toByteArray());
    assertArrayEquals(new byte[]{(byte) 0xF1, 0x00, 0x00, (byte) 0x01, 0x00, 00, 0x02, 0x00, 00, (byte) 0x03}, JBBPOut.BeginBin().Byte(0xF1).Align(3).Byte(1).Align(3).Byte(2).Align(3).Byte(3).End().toByteArray());
    assertArrayEquals(new byte[]{0x01, 0x02, 03, 0x04, 0x00, (byte)0xF1}, JBBPOut.BeginBin().Int(0x01020304).Align(5).Byte(0xF1).End().toByteArray());
    assertArrayEquals(new byte[]{0x01, 0x00, 0x00, 0x00, 0x00, (byte)0xF1}, JBBPOut.BeginBin().Bit(1).Align(5).Byte(0xF1).End().toByteArray());
  }

  @Test
  public void testEmptyArray() throws Exception {
    assertEquals(0, JBBPOut.BeginBin().End().toByteArray().length);
  }

  @Test
  public void testByte() throws Exception {
    assertArrayEquals(new byte[]{-34}, BeginBin().Byte(-34).End().toByteArray());
  }

  @Test
  public void testByteArrayAsInts() throws Exception {
    assertArrayEquals(new byte[]{1, 3, 0, 2, 4, 1, 3, 7}, BeginBin().Byte(1, 3, 0, 2, 4, 1, 3, 7).End().toByteArray());
  }

  @Test
  public void testByteArrayAsByteArray() throws Exception {
    assertArrayEquals(new byte[]{1, 3, 0, 2, 4, 1, 3, 7}, BeginBin().Byte(new byte[]{1, 3, 0, 2, 4, 1, 3, 7}).End().toByteArray());
  }

  @Test
  public void testBit() throws Exception {
    assertArrayEquals(new byte[]{1}, BeginBin().Bit(1).End().toByteArray());
  }

  @Test
  public void testBit_MSB0() throws Exception {
    assertArrayEquals(new byte[]{(byte) 0x80}, BeginBin(JBBPByteOrder.BIG_ENDIAN, JBBPBitOrder.MSB0).Bit(1).End().toByteArray());
  }

  @Test
  public void testBit_LSB0() throws Exception {
    assertArrayEquals(new byte[]{(byte) 0x01}, BeginBin(JBBPByteOrder.BIG_ENDIAN, JBBPBitOrder.LSB0).Bit(1).End().toByteArray());
  }

  @Test
  public void testBits_Int() throws Exception {
    assertArrayEquals(new byte[]{0xD}, BeginBin().Bits(JBBPBitNumber.BITS_4, 0xFD).End().toByteArray());
  }

  @Test
  public void testBits_IntArray() throws Exception {
    assertArrayEquals(new byte[]{(byte) 0xED}, BeginBin().Bits(JBBPBitNumber.BITS_4, 0xFD, 0xFE).End().toByteArray());
  }

  @Test
  public void testBits_ByteArray() throws Exception {
    assertArrayEquals(new byte[]{(byte) 0xED}, BeginBin().Bits(JBBPBitNumber.BITS_4, (byte) 0xFD, (byte) 0x8E).End().toByteArray());
  }

  @Test
  public void testBitArrayAsInts() throws Exception {
    assertArrayEquals(new byte[]{(byte) 0xE3}, BeginBin().Bit(1, 3, 0, 2, 4, 1, 3, 7).End().toByteArray());
    assertArrayEquals(new byte[]{(byte) 0x0B}, BeginBin().Bit(1, 3, 0, 7).End().toByteArray());
  }

  @Test
  public void testBitArrayAsBytes() throws Exception {
    assertArrayEquals(new byte[]{(byte) 0xE3}, BeginBin().Bit(new byte[]{(byte) 1, (byte) 3, (byte) 0, (byte) 2, (byte) 4, (byte) 1, (byte) 3, (byte) 7}).End().toByteArray());
    assertArrayEquals(new byte[]{(byte) 0x0B}, BeginBin().Bit(new byte[]{(byte) 1, (byte) 3, (byte) 0, (byte) 7}).End().toByteArray());
  }

  @Test
  public void testBitArrayAsBooleans() throws Exception {
    assertArrayEquals(new byte[]{(byte) 0xE3}, BeginBin().Bit(true, true, false, false, false, true, true, true).End().toByteArray());
    assertArrayEquals(new byte[]{(byte) 0x0B}, BeginBin().Bit(true, true, false, true).End().toByteArray());
  }

  @Test
  public void testShort() throws Exception {
    assertArrayEquals(new byte []{0x01,02}, BeginBin().Short(0x0102).End().toByteArray());
  }
  
  @Test
  public void testShort_BigEndian() throws Exception {
    assertArrayEquals(new byte []{0x01,02}, BeginBin().ByteOrder(JBBPByteOrder.BIG_ENDIAN).Short(0x0102).End().toByteArray());
  }
  
  @Test
  public void testShort_LittleEndian() throws Exception {
    assertArrayEquals(new byte []{0x02,01}, BeginBin().ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).Short(0x0102).End().toByteArray());
  }
  
  @Test
  public void testShortArray_AsIntegers() throws Exception {
    assertArrayEquals(new byte []{1,2,3,4}, BeginBin().Short(0x0102,0x0304).End().toByteArray());
  }
  
  @Test
  public void testShortArray_AsIntegers_BigEndian() throws Exception {
    assertArrayEquals(new byte []{1,2,3,4}, BeginBin().ByteOrder(JBBPByteOrder.BIG_ENDIAN).Short(0x0102,0x0304).End().toByteArray());
  }
  
  @Test
  public void testShortArray_AsIntegers_LittleEndian() throws Exception {
    assertArrayEquals(new byte []{2,1,4,3}, BeginBin().ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).Short(0x0102,0x0304).End().toByteArray());
  }

  @Test
  public void testShortArray_AsShorts() throws Exception {
    assertArrayEquals(new byte[]{1, 2, 3, 4}, BeginBin().Short((short)0x0102, (short)0x0304).End().toByteArray());
  }
  
  @Test
  public void testShortArray_AsShortArray() throws Exception {
    assertArrayEquals(new byte[]{1, 2, 3, 4}, BeginBin().Short(new short[]{(short)0x0102, (short)0x0304}).End().toByteArray());
  }
  
  @Test
  public void testInt() throws Exception {
    assertArrayEquals(new byte []{0x01,02,0x03,0x04}, BeginBin().Int(0x01020304).End().toByteArray());
  }
  
  @Test
  public void testIntArray() throws Exception {
    assertArrayEquals(new byte []{0x01,02,0x03,0x04,0x05,0x06,0x07,0x08}, BeginBin().Int(0x01020304, 0x05060708).End().toByteArray());
  }
  
  @Test
  public void testInt_BigEndian() throws Exception {
    assertArrayEquals(new byte []{0x01,0x02,0x03,0x04}, BeginBin().ByteOrder(JBBPByteOrder.BIG_ENDIAN).Int(0x01020304).End().toByteArray());
  }
  
  @Test
  public void testInt_LittleEndian() throws Exception {
    assertArrayEquals(new byte[]{0x04, 0x03, 0x02, 0x01}, BeginBin().ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).Int(0x01020304).End().toByteArray());
  }
  
  @Test
  public void testLong() throws Exception {
    assertArrayEquals(new byte []{0x01,02,0x03,0x04,0x05,0x06,0x07,0x08}, BeginBin().Long(0x0102030405060708L).End().toByteArray());
  }
  
  @Test
  public void testLongArray() throws Exception {
    assertArrayEquals(new byte []{0x01,02,0x03,0x04,0x05,0x06,0x07,0x08, 0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18}, BeginBin().Long(0x0102030405060708L,0x1112131415161718L).End().toByteArray());
  }
  
  @Test
  public void testLong_BigEndian() throws Exception {
    assertArrayEquals(new byte []{0x01, 02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08}, BeginBin().ByteOrder(JBBPByteOrder.BIG_ENDIAN).Long(0x0102030405060708L).End().toByteArray());
  }
  
  @Test
  public void testLong_LittleEndian() throws Exception {
    assertArrayEquals(new byte[]{0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01}, BeginBin().ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).Long(0x0102030405060708L).End().toByteArray());
  }
  
  @Test
  public void testFlush() throws Exception {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    final JBBPOut out = BeginBin(buffer);

    out.Bit(true);
    assertEquals(0, buffer.size());
    out.Flush();
    assertEquals(1, buffer.size());
  }

  @Test
  public void testExceptionForOperatioOverEndedProcess() throws Exception {
    final JBBPOut out = BeginBin();
    out.ByteOrder(JBBPByteOrder.BIG_ENDIAN).Long(0x0102030405060708L).End();
    try{
      out.Align();
      fail("Must throw ISE");
    }catch(IllegalStateException ex){
    }
    
    try{
      out.Align(3);
      fail("Must throw ISE");
    }catch(IllegalStateException ex){
    }

    try{
      out.Bit(true);
      fail("Must throw ISE");
    }catch(IllegalStateException ex){
    }

    try{
      out.Bit(true,false);
      fail("Must throw ISE");
    }catch(IllegalStateException ex){
    }

    try{
      out.Bit((byte)34);
      fail("Must throw ISE");
    }catch(IllegalStateException ex){
    }

    try{
      out.Bit((byte)34,(byte)12);
      fail("Must throw ISE");
    }catch(IllegalStateException ex){
    }

    try{
      out.Bit(34,12);
      fail("Must throw ISE");
    }catch(IllegalStateException ex){
    }

    try{
      out.Bits(JBBPBitNumber.BITS_3, 12);
      fail("Must throw ISE");
    }catch(IllegalStateException ex){
    }

    try{
      out.Bits(JBBPBitNumber.BITS_3, 12,13,14);
      fail("Must throw ISE");
    }catch(IllegalStateException ex){
    }

    try{
      out.Bits(JBBPBitNumber.BITS_3, (byte)1,(byte)2,(byte)3);
      fail("Must throw ISE");
    }catch(IllegalStateException ex){
    }

    try{
      out.Bool(true);
      fail("Must throw ISE");
    }catch(IllegalStateException ex){
    }

    try{
      out.Bool(true,false);
      fail("Must throw ISE");
    }catch(IllegalStateException ex){
    }

    try{
      out.Byte((byte)1,(byte)2,(byte)3);
      fail("Must throw ISE");
    }catch(IllegalStateException ex){
    }

    try{
      out.Byte(1);
      fail("Must throw ISE");
    }catch(IllegalStateException ex){
    }

    try{
      out.Byte(1,2,3);
      fail("Must throw ISE");
    }catch(IllegalStateException ex){
    }

    try{
      out.ByteOrder(JBBPByteOrder.BIG_ENDIAN);
      fail("Must throw ISE");
    }catch(IllegalStateException ex){
    }

    try{
      out.Flush();
      fail("Must throw ISE");
    }catch(IllegalStateException ex){
    }

    try{
      out.Int(1);
      fail("Must throw ISE");
    }catch(IllegalStateException ex){
    }

    try{
      out.Int(1,2);
      fail("Must throw ISE");
    }catch(IllegalStateException ex){
    }

    try{
      out.Long(1L);
      fail("Must throw ISE");
    }catch(IllegalStateException ex){
    }

    try{
      out.Long(1L,2L);
      fail("Must throw ISE");
    }catch(IllegalStateException ex){
    }

    try{
      out.Short(1);
      fail("Must throw ISE");
    }catch(IllegalStateException ex){
    }

    try{
      out.Short(1,2,3);
      fail("Must throw ISE");
    }catch(IllegalStateException ex){
    }

    try{
      out.Short((short)1,(short)2,(short)3);
      fail("Must throw ISE");
    }catch(IllegalStateException ex){
    }

    try{
      out.End();
      fail("Must throw ISE");
    }catch(IllegalStateException ex){
    }
  }
  
  @Test
  public void testExternalStreamButNoByteArrayOutputStream() throws Exception {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    final DataOutputStream dout = new DataOutputStream(buffer);
    
    assertNull(BeginBin(dout).Byte(1,2,3).End());
    assertArrayEquals(new byte[]{1,2,3}, buffer.toByteArray());
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testExceptionForBitOrderConfilctInCaseOfUsageBitOutputStream() throws Exception {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream bitstream = new JBBPBitOutputStream(buffer,JBBPBitOrder.LSB0);
    
    JBBPOut.BeginBin(bitstream, JBBPByteOrder.BIG_ENDIAN,JBBPBitOrder.MSB0);
  }
  
  @Test
  public void testComplexWriting_1() throws Exception {
    final byte [] array = 
          BeginBin().
            Bit(1, 2, 3, 0).
            Bit(true, false, true).
            Align().
            Byte(5).
            Short(1, 2, 3, 4, 5).
            Bool(true, false, true, true).
            Int(0xABCDEF23, 0xCAFEBABE).
            Long(0x123456789ABCDEF1L, 0x212356239091AB32L).
          End().toByteArray();

    assertEquals(40, array.length);
    assertArrayEquals(new byte[]{
      (byte) 0x55, 5, 0, 1, 0, 2, 0, 3, 0, 4, 0, 5, 1, 0, 1, 1,
      (byte) 0xAB, (byte) 0xCD, (byte) 0xEF, 0x23, (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE,
      0x12, 0x34, 0x56, 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF1, 0x21, 0x23, 0x56, 0x23, (byte) 0x90, (byte) 0x91, (byte) 0xAB, 0x32

    }, array);
  }

}
