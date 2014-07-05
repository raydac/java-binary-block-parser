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

import java.io.ByteArrayOutputStream;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.igormaznitsa.jbbp.io.JBBPOut.*;

public class JBBPOutTest {

  @Test
  public void testEmptyArray() throws Exception {
    assertEquals(0,new JBBPOut(new ByteArrayOutputStream()).end().toByteArray().length);
  }
  
  @Test
  public void testByte() throws Exception {
    assertArrayEquals(new byte[]{-34}, binStart().Byte(-34).end().toByteArray());
  }


  @Test
  public void testByteArrayAsInts() throws Exception {
    assertArrayEquals(new byte[]{1,3,0,2,4,1,3,7}, binStart().Byte(1, 3, 0, 2, 4, 1, 3, 7).end().toByteArray());
  }

  @Test
  public void testByteArrayAsByteArray() throws Exception {
    assertArrayEquals(new byte[]{1,3,0,2,4,1,3,7}, binStart().Byte(new byte[]{1, 3, 0, 2, 4, 1, 3, 7}).end().toByteArray());
  }

  @Test
  public void testBit() throws Exception {
    assertArrayEquals(new byte[]{1}, binStart().Bit(1).end().toByteArray());
  }
  
  @Test
  public void testBit_MSB0() throws Exception {
    assertArrayEquals(new byte[]{(byte)0x80}, binStart(JBBPByteOrder.BIG_ENDIAN,JBBPBitOrder.MSB0).Bit(1).end().toByteArray());
  }
  
  @Test
  public void testBit_LSB0() throws Exception {
    assertArrayEquals(new byte[]{(byte) 0x01}, binStart(JBBPByteOrder.BIG_ENDIAN, JBBPBitOrder.LSB0).Bit(1).end().toByteArray());
  }
  
  @Test
  public void testBits_Int() throws Exception {
    assertArrayEquals(new byte[]{0xD}, binStart().Bits(JBBPNumberOfBits.BITS_4, 0xFD).end().toByteArray());
  }
  
  @Test
  public void testBits_IntArray() throws Exception {
    assertArrayEquals(new byte[]{(byte)0xED}, binStart().Bits(JBBPNumberOfBits.BITS_4, 0xFD, 0xFE).end().toByteArray());
  }
 
  @Test
  public void testBits_ByteArray() throws Exception {
    assertArrayEquals(new byte[]{(byte)0xED}, binStart().Bits(JBBPNumberOfBits.BITS_4, new byte[]{ (byte)0xFD, (byte)0x8E}).end().toByteArray());
  }
  
  @Test
  public void testBitArrayAsInts() throws Exception {
    assertArrayEquals(new byte[]{(byte)0xE3}, binStart().Bit(1,3,0,2,4,1,3,7).end().toByteArray());
    assertArrayEquals(new byte[]{(byte)0x0B}, binStart().Bit(1,3,0,7).end().toByteArray());
  }
  
  @Test
  public void testBitArrayAsBytes() throws Exception {
    assertArrayEquals(new byte[]{(byte)0xE3}, binStart().Bit(new byte[]{(byte)1,(byte)3,(byte)0,(byte)2,(byte)4,(byte)1,(byte)3,(byte)7}).end().toByteArray());
    assertArrayEquals(new byte[]{(byte)0x0B}, binStart().Bit(new byte[]{(byte)1,(byte)3,(byte)0,(byte)7}).end().toByteArray());
  }
  
  @Test
  public void testBitArrayAsBooleans() throws Exception {
    assertArrayEquals(new byte[]{(byte)0xE3}, binStart().Bit(true, true, false, false, false, true, true,true).end().toByteArray());
    assertArrayEquals(new byte[]{(byte)0x0B}, binStart().Bit(true, true, false, true).end().toByteArray());
  }
  
  @Test
  public void testFlush() throws Exception {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    final JBBPOut out = binStart(buffer);
    
    out.Bit(true);
    assertEquals(0,buffer.size());
    out.Flush();
    assertEquals(1,buffer.size());
  }
  
  @Test
  public void testComplexWriting_1() throws Exception {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream(16384);

    final JBBPOut begin = new JBBPOut(buffer);

    begin.
            Bit(1,2,3,0).
            Bit(true, false, true).
            Align().
            Byte(5).
            Short(1,2,3,4,5).
            Bool(true, false, true, true).
            Int(0xABCDEF23, 0xCAFEBABE).
            Long(0x123456789ABCDEF1L, 0x212356239091AB32L).
    end();

    final byte[] array = buffer.toByteArray();

    assertEquals(40, array.length);
    assertArrayEquals(new byte[]{
      (byte)0x55, 5, 0, 1, 0, 2, 0, 3, 0, 4, 0, 5, 1, 0, 1, 1, 
      (byte) 0xAB, (byte) 0xCD, (byte) 0xEF, 0x23, (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE,
      0x12,0x34,0x56,0x78,(byte)0x9A,(byte)0xBC,(byte)0xDE,(byte)0xF1,0x21,0x23,0x56,0x23,(byte)0x90,(byte)0x91,(byte)0xAB,0x32
            
    }, array);
  }

}
