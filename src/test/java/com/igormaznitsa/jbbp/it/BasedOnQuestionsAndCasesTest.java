/*
 * Copyright 2015 Igor Maznitsa.
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
package com.igormaznitsa.jbbp.it;

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.io.JBBPOut;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests based on questions and cases.
 */
public class BasedOnQuestionsAndCasesTest {
  
  /**
   * Case 13-aug-2015
   * 
   * 3DF8 = 0011 1101 1111 1000 where data are stored from left to right :
   * 6 bits : 0011 11 for year; 
   * 4 bits : 01 11 for month 
   * 5 bits : 11 100 for day,
   * 
   * @throws Exception for any error
   */
  @Test
  public void testParseDayMonthYearFromBytePairInMSB0AndPackThemBack() throws Exception {
    class YearMonthDay {
      @Bin(type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_6, outOrder = 1, bitOrder = JBBPBitOrder.MSB0) byte year;
      @Bin(type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_4, outOrder = 2, bitOrder = JBBPBitOrder.MSB0) byte month;
      @Bin(type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_5, outOrder = 3, bitOrder = JBBPBitOrder.MSB0) byte day;
    }
    final YearMonthDay parsed = JBBPParser.prepare("bit:6 year; bit:4 month;  bit:5 day;", JBBPBitOrder.MSB0).parse(new byte[]{(byte) 0x3D, (byte) 0xF8}).mapTo(YearMonthDay.class);

    assertEquals(0x0F,parsed.year);
    assertEquals(0x07,parsed.month);
    assertEquals(0x1C,parsed.day & 0xFF);
    
    assertArrayEquals(new byte[]{(byte)0x3D,(byte)0xF8}, JBBPOut.BeginBin(JBBPBitOrder.MSB0).Bin(parsed).End().toByteArray());
  }

  /**
   * Case 08-feb-2016
   *
   * Incoming data: 0x024281
   * Timestamp format : <a href="http://www.etsi.org/deliver/etsi_en/300300_300399/30039202/02.03.02_60/en_30039202v020302p.pdf">Terrestrial Trunked Radio</a>
   *
   * @throws Exception for any error
   */
  @Test
  public void testParseTimeStampFromTETRASavedInMSB0() throws Exception {
    final byte [] TEST_DATA = new byte[]{0x2, 0x42, (byte) 0x81};
    
    class TetraTimestamp {
      @Bin (type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_2, outOrder = 1, bitOrder = JBBPBitOrder.MSB0)
      byte timezone;
      @Bin (type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_2, outOrder = 2, bitOrder = JBBPBitOrder.MSB0)
      byte reserved;
      @Bin (type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_4, outOrder = 3, bitOrder = JBBPBitOrder.MSB0)
      byte month;
      @Bin (type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_5, outOrder = 4, bitOrder = JBBPBitOrder.MSB0)
      byte day;
      @Bin (type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_5, outOrder = 5, bitOrder = JBBPBitOrder.MSB0)
      byte hour;
      @Bin (type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_6, outOrder = 6, bitOrder = JBBPBitOrder.MSB0)
      byte minute;
    }
    
    
    TetraTimestamp parsed = JBBPParser.prepare("bit:2 timezone; bit:2 reserved; bit:4 month; bit:5 day; bit:5 hour; bit:6 minute;", JBBPBitOrder.MSB0).parse(TEST_DATA).mapTo(TetraTimestamp.class);
    
    assertEquals(2, parsed.month);
    assertEquals(8, parsed.day);
    assertEquals(10, parsed.hour);
    assertEquals(1, parsed.minute);
    
    assertArrayEquals(TEST_DATA, JBBPOut.BeginBin(JBBPBitOrder.MSB0).Bin(parsed).End().toByteArray());
  }
}
