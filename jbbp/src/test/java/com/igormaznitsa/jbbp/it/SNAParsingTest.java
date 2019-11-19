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

package com.igormaznitsa.jbbp.it;

import static com.igormaznitsa.jbbp.io.JBBPByteOrder.LITTLE_ENDIAN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.JBBPOut;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import com.igormaznitsa.jbbp.model.JBBPFieldStruct;
import com.igormaznitsa.jbbp.utils.JBBPDslBuilder;
import com.igormaznitsa.jbbp.utils.JBBPTextWriter;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

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

  @Test
  public void testParseAndSave() throws Exception {
    final SNA sna;
    final InputStream in = getResourceAsInputStream("zexall.sna");
    try {
      sna = PARSER_SNA_48.parse(in).mapTo(new SNA());
    } finally {
      JBBPUtils.closeQuietly(in);
    }

    assertEquals(0x3F, sna.regI);
    assertEquals(0x2758, sna.altRegHL);
    assertEquals(0x369B, sna.altRegDE);
    assertEquals(0x1721, sna.altRegBC);
    assertEquals(0x0044, sna.altRegAF);

    assertEquals(0x2D2B, sna.regHL);
    assertEquals(0x80ED, sna.regDE);
    assertEquals(0x803E, sna.regBC);
    assertEquals(0x5C3A, sna.regIY);
    assertEquals(0x03D4, sna.regIX);

    assertEquals(0x00, sna.iff);
    assertEquals(0xAE, sna.regR);
    assertEquals(0x14A1, sna.regAF);
    assertEquals(0x7E62, sna.regSP);

    assertEquals(0x01, sna.im);
    assertEquals(0x07, sna.borderColor);

    assertEquals(49152, sna.ramDump.length);

    final byte[] packed = JBBPOut.BeginBin(LITTLE_ENDIAN).Bin(sna).End().toByteArray();
    assertResource("zexall.sna", packed);

    final String text = new JBBPTextWriter().ByteOrder(LITTLE_ENDIAN).SetMaxValuesPerLine(32).Bin(sna).Close().toString();
    assertTrue(text.length() > 10000);
    System.out.println(text);
  }

  @Test
  public void testParseAndSave_ThroughDslBuilder() throws Exception {
    final JBBPParser parser = JBBPParser.prepare(JBBPDslBuilder.Begin().AnnotatedClass(SNA.class).End());

    final InputStream in = getResourceAsInputStream("zexall.sna");

    JBBPFieldStruct parsed;
    try {
      parsed = parser.parse(in);
    } finally {
      JBBPUtils.closeQuietly(in);
    }

    final SNA mapped = parsed.findFieldForNameAndType("SNA", JBBPFieldStruct.class).mapTo(new SNA());
    assertResource("zexall.sna", JBBPOut.BeginBin().Bin(mapped).End().toByteArray());
  }

  @Bin(comment = "Parsed SNA snapshot")
  private class SNA {

    @Bin(type = BinType.UBYTE, order = 1, comment = "Register I")
    int regI;
    @Bin(type = BinType.USHORT, order = 2, name = "altHL", comment = "Register pair HL'", byteOrder = LITTLE_ENDIAN)
    int altRegHL;
    @Bin(type = BinType.USHORT, order = 3, name = "altDE", comment = "Register pair DE'", byteOrder = LITTLE_ENDIAN)
    int altRegDE;
    @Bin(type = BinType.USHORT, order = 4, name = "altBC", comment = "Registe pair BC'", byteOrder = LITTLE_ENDIAN)
    int altRegBC;
    @Bin(type = BinType.USHORT, order = 5, name = "altAF", comment = "Register pair AF'", byteOrder = LITTLE_ENDIAN)
    int altRegAF;
    @Bin(type = BinType.USHORT, order = 6, comment = "Register pair HL", byteOrder = LITTLE_ENDIAN)
    int regHL;
    @Bin(type = BinType.USHORT, order = 7, comment = "Register pair DE", byteOrder = LITTLE_ENDIAN)
    int regDE;
    @Bin(type = BinType.USHORT, order = 8, comment = "Register pair BC", byteOrder = LITTLE_ENDIAN)
    int regBC;
    @Bin(type = BinType.USHORT, order = 9, comment = "Register IY", byteOrder = LITTLE_ENDIAN)
    int regIY;
    @Bin(type = BinType.USHORT, order = 10, comment = "Register IX", byteOrder = LITTLE_ENDIAN)
    int regIX;
    @Bin(type = BinType.UBYTE, order = 11, comment = "IFF1 and IFF2 values")
    int iff;
    @Bin(type = BinType.UBYTE, order = 12, comment = "Register R")
    int regR;
    @Bin(type = BinType.USHORT, order = 13, comment = "Register pair AF", byteOrder = LITTLE_ENDIAN)
    int regAF;
    @Bin(type = BinType.USHORT, order = 14, comment = "Register SP", byteOrder = LITTLE_ENDIAN)
    int regSP;
    @Bin(type = BinType.UBYTE, order = 15, comment = "Interruption mode (0-IM0, 1-IM1, 2-IM2")
    int im;
    @Bin(type = BinType.UBYTE, order = 16, comment = "Border color")
    int borderColor;
    @Bin(order = 17, comment = "Dump of memory since 16384 address", arraySizeExpr = "49152")
    byte[] ramDump;
  }

}
