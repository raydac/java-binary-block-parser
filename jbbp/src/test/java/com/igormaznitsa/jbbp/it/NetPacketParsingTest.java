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

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPOut;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import com.igormaznitsa.jbbp.model.*;
import com.igormaznitsa.jbbp.utils.JBBPTextWriter;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

public class NetPacketParsingTest extends AbstractParserIntegrationTest {

  @Test
  public void testParsingTCPFrameInsideNetworkFrame() throws Exception {
    final JBBPBitInputStream netPacketStream = new JBBPBitInputStream(getResourceAsInputStream("tcppacket.bin"));
    try {

      // Ethernet header Ethernet II
      final JBBPParser ethernetParserHeaderWithout802_1QTag = JBBPParser.prepare(
          "byte[6] MacDestination;"
              + "byte[6] MacSource;"
              + "ushort EtherTypeOrLength;"
      );

      // IPv4 header
      final JBBPParser ipParserHeaderWithoutOptions = JBBPParser.prepare(
          "bit:4 InternetHeaderLength;"
              + "bit:4 Version;"
              + "bit:2 ECN;"
              + "bit:6 DSCP;"
              + "ushort TotalPacketLength;"
              + "ushort Identification;"
              + "ushort IPFlagsAndFragmentOffset;"
              + "ubyte TTL;"
              + "ubyte Protocol;"
              + "ushort HeaderChecksum;"
              + "int SourceAddress;"
              + "int DestinationAddress;"
              + "byte [(InternetHeaderLength-5)*4] Options;"
      );

      // TCP header
      final JBBPParser tcpHeader = JBBPParser.prepare(
          "ushort SourcePort;"
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
              + "byte [$$-HLEN*4] Option;"
      );


      // Check Ethernet header
      final JBBPFieldStruct parsedEthernetHeader = ethernetParserHeaderWithout802_1QTag.parse(netPacketStream);
      assertArrayEquals(new byte[] {(byte) 0x60, (byte) 0x67, (byte) 0x20, (byte) 0xE1, (byte) 0xF9, (byte) 0xF8}, parsedEthernetHeader.findFieldForNameAndType("MacDestination", JBBPFieldArrayByte.class).getArray(), "Destination MAC");
      assertArrayEquals(new byte[] {(byte) 0x00, (byte) 0x26, (byte) 0x44, (byte) 0x74, (byte) 0xFE, (byte) 0x66}, parsedEthernetHeader.findFieldForNameAndType("MacSource", JBBPFieldArrayByte.class).getArray(), "Source MAC");

      final int etherTypeOrLength = parsedEthernetHeader.findFieldForNameAndType("EtherTypeOrLength", JBBPFieldUShort.class).getAsInt();
      assertEquals(0x800, etherTypeOrLength, "Ethernet type or length");

      if (etherTypeOrLength > 1500) {
        // list of protocols http://standards-oui.ieee.org/ethertype/eth.txt
        System.out.println("Ethernet type is : 0x" + Integer.toHexString(etherTypeOrLength).toUpperCase());
      } else {
        System.out.println("Payload length : " + etherTypeOrLength);
      }


      // Check IP header
      netPacketStream.resetCounter();
      final JBBPFieldStruct parsedIPHeader = ipParserHeaderWithoutOptions.parse(netPacketStream);

      assertEquals(4, parsedIPHeader.findFieldForNameAndType("Version", JBBPFieldBit.class).getAsInt(), "IP Version");

      final int internetHeaderLength = parsedIPHeader.findFieldForNameAndType("InternetHeaderLength", JBBPFieldBit.class).getAsInt();
      assertEquals(5, internetHeaderLength, "Length of the IP header (in 4 byte items)");
      assertEquals(0, parsedIPHeader.findFieldForNameAndType("DSCP", JBBPFieldBit.class).getAsInt(), "Differentiated Services Code Point");
      assertEquals(0, parsedIPHeader.findFieldForNameAndType("ECN", JBBPFieldBit.class).getAsInt(), "Explicit Congestion Notification");

      final int ipTotalPacketLength = parsedIPHeader.findFieldForNameAndType("TotalPacketLength", JBBPFieldUShort.class).getAsInt();

      assertEquals(159, ipTotalPacketLength, "Entire IP packet size, including header and data, in bytes");
      assertEquals(30810, parsedIPHeader.findFieldForNameAndType("Identification", JBBPFieldUShort.class).getAsInt(), "Identification");

      final int ipFlagsAndFragmentOffset = parsedIPHeader.findFieldForNameAndType("IPFlagsAndFragmentOffset", JBBPFieldUShort.class).getAsInt();

      assertEquals(0x2, ipFlagsAndFragmentOffset >>> 13, "Extracted IP flags");
      assertEquals(0x00, ipFlagsAndFragmentOffset & 0x1FFF, "Extracted Fragment offset");

      assertEquals(0x39, parsedIPHeader.findFieldForNameAndType("TTL", JBBPFieldUByte.class).getAsInt(), "Time To Live");
      assertEquals(0x06, parsedIPHeader.findFieldForNameAndType("Protocol", JBBPFieldUByte.class).getAsInt(), "Protocol (RFC-790)");
      assertEquals(0x7DB6, parsedIPHeader.findFieldForNameAndType("HeaderChecksum", JBBPFieldUShort.class).getAsInt(), "IPv4 Header Checksum");
      assertEquals(0xD5C7B393, parsedIPHeader.findFieldForNameAndType("SourceAddress", JBBPFieldInt.class).getAsInt(), "Source IP address");
      assertEquals(0xC0A80145, parsedIPHeader.findFieldForNameAndType("DestinationAddress", JBBPFieldInt.class).getAsInt(), "Destination IP address");

      assertEquals(0, parsedIPHeader.findFieldForNameAndType("Options", JBBPFieldArrayByte.class).getArray().length);

      // Check TCP header
      netPacketStream.resetCounter();
      final JBBPFieldStruct parsedTcpHeader = tcpHeader.parse(netPacketStream);

      assertEquals(40018, parsedTcpHeader.findFieldForNameAndType("SourcePort", JBBPFieldUShort.class).getAsInt());
      assertEquals(56344, parsedTcpHeader.findFieldForNameAndType("DestinationPort", JBBPFieldUShort.class).getAsInt());
      assertEquals(0xE0084171, parsedTcpHeader.findFieldForNameAndType("SequenceNumber", JBBPFieldInt.class).getAsInt());
      assertEquals(0xAB616F71, parsedTcpHeader.findFieldForNameAndType("AcknowledgementNumber", JBBPFieldInt.class).getAsInt());

      assertFalse(parsedTcpHeader.findFieldForNameAndType("FIN", JBBPFieldBit.class).getAsBool());
      assertFalse(parsedTcpHeader.findFieldForNameAndType("SYN", JBBPFieldBit.class).getAsBool());
      assertFalse(parsedTcpHeader.findFieldForNameAndType("RST", JBBPFieldBit.class).getAsBool());
      assertTrue(parsedTcpHeader.findFieldForNameAndType("PSH", JBBPFieldBit.class).getAsBool());
      assertTrue(parsedTcpHeader.findFieldForNameAndType("ACK", JBBPFieldBit.class).getAsBool());
      assertFalse(parsedTcpHeader.findFieldForNameAndType("URG", JBBPFieldBit.class).getAsBool());
      assertFalse(parsedTcpHeader.findFieldForNameAndType("ECNECHO", JBBPFieldBit.class).getAsBool());
      assertFalse(parsedTcpHeader.findFieldForNameAndType("CWR", JBBPFieldBit.class).getAsBool());
      assertFalse(parsedTcpHeader.findFieldForNameAndType("NONCE", JBBPFieldBit.class).getAsBool());
      assertFalse(parsedTcpHeader.findFieldForNameAndType("RESERVED", JBBPFieldBit.class).getAsBool());

      assertEquals(5, parsedTcpHeader.findFieldForNameAndType("HLEN", JBBPFieldBit.class).getAsInt());

      assertEquals(40880, parsedTcpHeader.findFieldForNameAndType("WindowSize", JBBPFieldUShort.class).getAsInt());
      assertEquals(0x8BB6, parsedTcpHeader.findFieldForNameAndType("TCPCheckSum", JBBPFieldUShort.class).getAsInt());
      assertEquals(0, parsedTcpHeader.findFieldForNameAndType("UrgentPointer", JBBPFieldUShort.class).getAsInt());

      assertEquals(0, parsedTcpHeader.findFieldForNameAndType("Option", JBBPFieldArrayByte.class).size());

      // extract data
      final int payloadDataLength = ipTotalPacketLength - (internetHeaderLength * 4) - (int) netPacketStream.getCounter();
      final byte[] data = netPacketStream.readByteArray(payloadDataLength);
      assertEquals(119, data.length);

      System.out.println(new JBBPTextWriter(new StringWriter()).Comment("Payload data extracted from the TCP part").Byte(data).BR().toString());

      final byte[] restOfFrame = netPacketStream.readByteArray(-1);
      assertEquals(0, restOfFrame.length);

    } finally {
      JBBPUtils.closeQuietly(netPacketStream);
    }
  }

  @Test
  public void testParseSomePacketGettedOverTCP_ExampleFromStackOverflow() throws Exception {
    final class Parsed {

      @Bin(outOrder = 1)
      byte begin;
      @Bin(outOrder = 2, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_4)
      int version;
      @Bin(outOrder = 3, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_4)
      int returnType;
      @Bin(outOrder = 4)
      byte[] productCode;
      @Bin(outOrder = 5, type = BinType.USHORT)
      int dataLength;
    }

    final byte[] testArray = new byte[] {0x23, 0x21, (byte) 0x90, 0x23, 0x21, 0x22, 0x12, 0x00, (byte) 0xAA};

    final Parsed parsed = JBBPParser.prepare("byte begin; bit:4 version; bit:4 returnType; byte [5] productCode; ushort dataLength;")
        .parse(testArray)
        .mapTo(Parsed.class);

    assertEquals(0x23, parsed.begin);
    assertEquals(0x01, parsed.version);
    assertEquals(0x02, parsed.returnType);
    assertArrayEquals(new byte[] {(byte) 0x90, 0x23, 0x21, 0x22, 0x12}, parsed.productCode);
    assertEquals(0x00AA, parsed.dataLength);

    assertArrayEquals(testArray, JBBPOut.BeginBin().Bin(parsed).End().toByteArray());
  }

  @Test
  public void testParseUDP() throws Exception {
    final class Parsed {

      @Bin(outOrder = 1)
      char source;
      @Bin(outOrder = 2)
      char destination;
      @Bin(outOrder = 3)
      char length;
      @Bin(outOrder = 4)
      char checksum;
      @Bin(outOrder = 5)
      byte[] data;
    }

    final byte[] testArray = new byte[] {0x04, (byte) 0x89, 0x00, 0x35, 0x00, 0x2C, (byte) 0xAB, (byte) 0xB4, 0x00, 0x01, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04, 0x70, 0x6F, 0x70, 0x64, 0x02, 0x69, 0x78, 0x06, 0x6E, 0x65, 0x74, 0x63, 0x6F, 0x6D, 0x03, 0x63, 0x6F, 0x6D, 0x00, 0x00, 0x01, 0x00, 0x01};

    final Parsed parsed = JBBPParser.prepare("ushort source; ushort destination; ushort length; ushort checksum; byte [length-8] data;").parse(testArray).mapTo(Parsed.class);

    assertEquals(0x0489, parsed.source);
    assertEquals(0x0035, parsed.destination);
    assertEquals(0xABB4, parsed.checksum);
    assertEquals(0x002C - 8, parsed.data.length);

    assertArrayEquals(testArray, JBBPOut.BeginBin().Bin(parsed).End().toByteArray());
  }

}
