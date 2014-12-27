/*
 * Copyright 2014 Igor Maznitsa.
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
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.io.JBBPOut;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.InputStream;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Test for parsing of SNA snapshots for ZX-Spectrum emulator.
 */
public class SNAParsingTest extends AbstractParserIntegrationTest {

  private static final JBBPParser PARSER_SNA_48 = JBBPParser.prepare(
          "ubyte regI;"
          + "<ushort altHL; <ushort altDE; <ushort altBC; <ushort altAF;"
          + "<ushort regHL; <ushort regDE; <ushort regBC; <ushort regIY; <ushort regIX;"
          + "ubyte iff; ubyte regR;"
          + "<ushort regAF; <ushort regSP;"
          + "ubyte im;"
          + "ubyte borderColor;"
          + "byte [49152] ramDump;");

  private class SNA {

    @Bin(type = BinType.UBYTE,  outOrder = 1)
    int regI;
    @Bin(type = BinType.USHORT, outOrder = 2, name = "altHL")
    int altRegHL;
    @Bin(type = BinType.USHORT, outOrder = 3, name = "altDE")
    int altRegDE;
    @Bin(type = BinType.USHORT, outOrder = 4, name = "altBC")
    int altRegBC;
    @Bin(type = BinType.USHORT, outOrder = 5, name = "altAF")
    int altRegAF;
    @Bin(type = BinType.USHORT, outOrder = 6)
    int regHL;
    @Bin(type = BinType.USHORT, outOrder = 7)
    int regDE;
    @Bin(type = BinType.USHORT, outOrder = 8)
    int regBC;
    @Bin(type = BinType.USHORT, outOrder = 9)
    int regIY;
    @Bin(type = BinType.USHORT, outOrder = 10)
    int regIX;
    @Bin(type = BinType.UBYTE, outOrder = 11)
    int iff;
    @Bin(type = BinType.UBYTE, outOrder = 12)
    int regR;
    @Bin(type = BinType.USHORT, outOrder = 13)
    int regAF;
    @Bin(type = BinType.USHORT, outOrder = 14)
    int regSP;
    @Bin(type = BinType.UBYTE, outOrder = 15)
    int im;
    @Bin(type = BinType.UBYTE, outOrder = 16)
    int borderColor;
    @Bin(outOrder = 17)
    byte[] ramDump;
  }

  @Test
  public void testPareseAndSave() throws Exception {
    final SNA sna;
    final InputStream in = getResourceAsInputStream("zexall.sna");
    try {
      sna = PARSER_SNA_48.parse(in).mapTo(SNA.class);
    }
    finally {
      JBBPUtils.closeQuietly(in);
    }

    assertEquals(0x3F,sna.regI);
    assertEquals(0x2758,sna.altRegHL);
    assertEquals(0x369B,sna.altRegDE);
    assertEquals(0x1721,sna.altRegBC);
    assertEquals(0x0044,sna.altRegAF);

    assertEquals(0x2D2B,sna.regHL);
    assertEquals(0x80ED,sna.regDE);
    assertEquals(0x803E,sna.regBC);
    assertEquals(0x5C3A,sna.regIY);
    assertEquals(0x03D4,sna.regIX);
    
    assertEquals(0x00,sna.iff);
    assertEquals(0xAE,sna.regR);
    assertEquals(0x14A1,sna.regAF);
    assertEquals(0x7E62,sna.regSP);
    
    assertEquals(0x01,sna.im);
    assertEquals(0x07,sna.borderColor);
    
    assertEquals(49152, sna.ramDump.length);
    
    final byte [] packed = JBBPOut.BeginBin(JBBPByteOrder.LITTLE_ENDIAN).Bin(sna).End().toByteArray();
    assertResource("zexall.sna", packed);
  }
  
}
