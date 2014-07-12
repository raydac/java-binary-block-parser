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
import com.igormaznitsa.jbbp.model.JBBPFieldArrayByte;
import com.igormaznitsa.jbbp.model.JBBPFieldBit;
import com.igormaznitsa.jbbp.model.JBBPFieldInt;
import com.igormaznitsa.jbbp.model.JBBPFieldStruct;
import com.igormaznitsa.jbbp.model.JBBPFieldUShort;
import java.io.InputStream;
import static org.junit.Assert.*;
import org.junit.Test;

public class TCPParsingTest extends AbstractParserIntegrationTest {

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
      closeResource(tcpFrameStream);
    }
  }
}
