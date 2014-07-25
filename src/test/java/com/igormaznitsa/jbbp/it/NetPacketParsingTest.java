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
package com.igormaznitsa.jbbp.it;

import com.igormaznitsa.jbbp.*;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import com.igormaznitsa.jbbp.model.*;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.InputStream;
import static org.junit.Assert.*;
import org.junit.Test;

public class NetPacketParsingTest extends AbstractParserIntegrationTest {

  @Test
  public void testParsingTCPFrameInsideNetworkFrame() throws Exception {
    final InputStream tcpFrameStream = getResourceAsInputStream("tcppacket.bin");
    try {

      final JBBPParser tcpParser = JBBPParser.prepare(
              "skip:34; // skip bytes till the frame\n"
              + "ushort SourcePort;"
              + "ushort DestinationPort;"
              + "int SequenceNumber;"
              + "int AcknowledgementNumber;"
              + "bit:1 NONCE;"
              + "bit:3 RESERVED;"
              + "bit:4 HLEN;"
              + "bit:1 FIN;"
              + "bit:1 SYN;"
              + "bit:1 RST;"
              + "bit:1 PSH;"
              + "bit:1 ACK;"
              + "bit:1 URG;"
              + "bit:1 ECNECHO;"
              + "bit:1 CWR;"
              + "ushort WindowSize;"
              + "ushort TCPCheckSum;"
              + "ushort UrgentPointer;"
              + "byte [$$-34-HLEN*4] Option;"
              + "byte [_] Data;"
      );

      final JBBPFieldStruct result = tcpParser.parse(tcpFrameStream);

      assertEquals(40018, result.findFieldForNameAndType("SourcePort", JBBPFieldUShort.class).getAsInt());
      assertEquals(56344, result.findFieldForNameAndType("DestinationPort", JBBPFieldUShort.class).getAsInt());
      assertEquals(0xE0084171, result.findFieldForNameAndType("SequenceNumber", JBBPFieldInt.class).getAsInt());
      assertEquals(0xAB616F71, result.findFieldForNameAndType("AcknowledgementNumber", JBBPFieldInt.class).getAsInt());

      assertFalse(result.findFieldForNameAndType("FIN", JBBPFieldBit.class).getAsBool());
      assertFalse(result.findFieldForNameAndType("SYN", JBBPFieldBit.class).getAsBool());
      assertFalse(result.findFieldForNameAndType("RST", JBBPFieldBit.class).getAsBool());
      assertTrue(result.findFieldForNameAndType("PSH", JBBPFieldBit.class).getAsBool());
      assertTrue(result.findFieldForNameAndType("ACK", JBBPFieldBit.class).getAsBool());
      assertFalse(result.findFieldForNameAndType("URG", JBBPFieldBit.class).getAsBool());
      assertFalse(result.findFieldForNameAndType("ECNECHO", JBBPFieldBit.class).getAsBool());
      assertFalse(result.findFieldForNameAndType("CWR", JBBPFieldBit.class).getAsBool());
      assertFalse(result.findFieldForNameAndType("NONCE", JBBPFieldBit.class).getAsBool());
      assertFalse(result.findFieldForNameAndType("RESERVED", JBBPFieldBit.class).getAsBool());

      assertEquals(5, result.findFieldForNameAndType("HLEN", JBBPFieldBit.class).getAsInt());

      assertEquals(40880, result.findFieldForNameAndType("WindowSize", JBBPFieldUShort.class).getAsInt());
      assertEquals(0x8BB6, result.findFieldForNameAndType("TCPCheckSum", JBBPFieldUShort.class).getAsInt());
      assertEquals(0, result.findFieldForNameAndType("UrgentPointer", JBBPFieldUShort.class).getAsInt());

      assertEquals(0, result.findFieldForNameAndType("Option", JBBPFieldArrayByte.class).size());
      assertEquals(119, result.findFieldForNameAndType("Data", JBBPFieldArrayByte.class).size());

    }
    finally {
      JBBPUtils.closeQuietly(tcpFrameStream);
    }
  }

  @Test
  public void testParseSomePacketGettedOverTCP_ExampleFromStackOverflow() throws Exception {
    final class Parsed {
      @Bin byte begin;
      @Bin(type = BinType.BIT) int version;
      @Bin(type = BinType.BIT) int returnType;
      @Bin byte[] productCode;
      @Bin(type = BinType.USHORT) int dataLength;
    }
    final Parsed parsed = JBBPParser.prepare("byte begin; bit:4 version; bit:4 returnType; byte [5] productCode; ushort dataLength;")
            .parse(new byte[]{0x23, 0x21, (byte) 0x90, 0x23, 0x21, 0x22, 0x12, 0x00, (byte) 0xAA})
            .mapTo(Parsed.class);

    assertEquals(0x23, parsed.begin);
    assertEquals(0x01, parsed.version);
    assertEquals(0x02, parsed.returnType);
    assertArrayEquals(new byte[]{(byte) 0x90, 0x23, 0x21, 0x22, 0x12}, parsed.productCode);
    assertEquals(0x00AA, parsed.dataLength);
  }

  @Test
  public void testParseUDP() throws Exception {
    final class Parsed {
      @Bin char source;
      @Bin char destination;
      @Bin char checksum;
      @Bin byte[] data;
    }

    final Parsed parsed = JBBPParser.prepare("ushort source; ushort destination; ushort length; ushort checksum; byte [length-8] data;").parse(new byte[]{0x04, (byte) 0x89, 0x00, 0x35, 0x00, 0x2C, (byte) 0xAB, (byte) 0xB4, 0x00, 0x01, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04, 0x70, 0x6F, 0x70, 0x64, 0x02, 0x69, 0x78, 0x06, 0x6E, 0x65, 0x74, 0x63, 0x6F, 0x6D, 0x03, 0x63, 0x6F, 0x6D, 0x00, 0x00, 0x01, 0x00, 0x01}).mapTo(Parsed.class);
    
    assertEquals(0x0489, parsed.source);
    assertEquals(0x0035, parsed.destination);
    assertEquals(0xABB4, parsed.checksum);
    assertEquals(0x002C-8, parsed.data.length);
  }

}
