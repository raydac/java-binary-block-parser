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
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.io.JBBPOut;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayByte;
import org.junit.Test;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Tests based on questions and cases.
 */
public class BasedOnQuestionsAndCasesTest extends AbstractParserIntegrationTest {

    /**
     * Case 13-aug-2015
     * <p>
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
            @Bin(type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_6, outOrder = 1, bitOrder = JBBPBitOrder.MSB0)
            byte year;
            @Bin(type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_4, outOrder = 2, bitOrder = JBBPBitOrder.MSB0)
            byte month;
            @Bin(type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_5, outOrder = 3, bitOrder = JBBPBitOrder.MSB0)
            byte day;
        }
        final YearMonthDay parsed = JBBPParser.prepare("bit:6 year; bit:4 month;  bit:5 day;", JBBPBitOrder.MSB0).parse(new byte[]{(byte) 0x3D, (byte) 0xF8}).mapTo(YearMonthDay.class);

        assertEquals(0x0F, parsed.year);
        assertEquals(0x07, parsed.month);
        assertEquals(0x1C, parsed.day & 0xFF);

        assertArrayEquals(new byte[]{(byte) 0x3D, (byte) 0xF8}, JBBPOut.BeginBin(JBBPBitOrder.MSB0).Bin(parsed).End().toByteArray());
    }

    /**
     * Case 08-feb-2016
     * <p>
     * Incoming data: 0x024281
     * Timestamp format : <a href="http://www.etsi.org/deliver/etsi_en/300300_300399/30039202/02.03.02_60/en_30039202v020302p.pdf">Terrestrial Trunked Radio</a>
     *
     * @throws Exception for any error
     */
    @Test
    public void testParseTimeStampFromTETRASavedInMSB0() throws Exception {
        final byte[] TEST_DATA = new byte[]{0x2, 0x42, (byte) 0x81};

        class TetraTimestamp {
            @Bin(type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_2, outOrder = 1, bitOrder = JBBPBitOrder.MSB0)
            byte timezone;
            @Bin(type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_2, outOrder = 2, bitOrder = JBBPBitOrder.MSB0)
            byte reserved;
            @Bin(type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_4, outOrder = 3, bitOrder = JBBPBitOrder.MSB0)
            byte month;
            @Bin(type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_5, outOrder = 4, bitOrder = JBBPBitOrder.MSB0)
            byte day;
            @Bin(type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_5, outOrder = 5, bitOrder = JBBPBitOrder.MSB0)
            byte hour;
            @Bin(type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_6, outOrder = 6, bitOrder = JBBPBitOrder.MSB0)
            byte minute;
        }


        TetraTimestamp parsed = JBBPParser.prepare("bit:2 timezone; bit:2 reserved; bit:4 month; bit:5 day; bit:5 hour; bit:6 minute;", JBBPBitOrder.MSB0).parse(TEST_DATA).mapTo(TetraTimestamp.class);

        assertEquals(2, parsed.month);
        assertEquals(8, parsed.day);
        assertEquals(10, parsed.hour);
        assertEquals(1, parsed.minute);

        assertArrayEquals(TEST_DATA, JBBPOut.BeginBin(JBBPBitOrder.MSB0).Bin(parsed).End().toByteArray());
    }

    /**
     * Case 28-jul-2016
     * Simultaneous usage of expression evaluator from multiple threads.
     * <p>
     * <a href="https://github.com/raydac/java-binary-block-parser/issues/10">Issue #10, assertArrayLength throws exception in multi-thread</a>
     *
     * @throws Exception for any error
     */
    @Test
    public void testMutlithredUsageOfParser() throws Exception {
        final JBBPParser parserIP = JBBPParser.prepare("skip:14; // skip bytes till the frame\n"
                + "bit:4 InternetHeaderLength;"
                + "bit:4 Version;"
                + "bit:2 ECN;"
                + "bit:6 DSCP;"
                + "ushort TotalPacketLength;"
                + "ushort Identification;"
                + "bit:8 IPFlagsAndFragmentOffset_low;"
                + "bit:5 IPFlagsAndFragmentOffset_high;"
                + "bit:1 MoreFragment;"
                + "bit:1 DonotFragment;"
                + "bit:1 ReservedBit;"
                + "ubyte TTL;"
                + "ubyte Protocol;"
                + "ushort HeaderChecksum;"
                + "int SourceAddress;"
                + "int DestinationAddress;"
                + "byte [(InternetHeaderLength-5)*4] Options;");

        final JBBPParser parserTCP = JBBPParser.prepare("skip:34; // skip bytes till the frame\n"
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
                + "byte [HLEN*4-20] Option;"
                + "byte [_] Data;");

        InputStream inStream = getResourceAsInputStream("tcppacket.bin");
        byte[] testArray = null;
        try {
            testArray = new JBBPBitInputStream(inStream).readByteArray(-1);
            assertEquals(173, testArray.length);
        } finally {
            inStream.close();
        }

        final byte[] theData = testArray;

        final AtomicInteger errorCounter = new AtomicInteger();
        final AtomicLong parsingCounter = new AtomicLong();

        final int ITERATIONS = 1000;

        final Runnable test = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < ITERATIONS; i++) {
                    try {
                        Thread.sleep(System.nanoTime() & 0xF);
                        final byte[] ippacket = parserTCP.parse(theData).findFieldForNameAndType("Data", JBBPFieldArrayByte.class).getArray();
                        assertEquals(119, ippacket.length);
                        final byte[] optionsip = parserIP.parse(ippacket).findFieldForNameAndType("Options", JBBPFieldArrayByte.class).getArray();
                        assertEquals(4, optionsip.length);
                        parsingCounter.incrementAndGet();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        errorCounter.incrementAndGet();
                    }
                }
            }
        };

        final Thread[] threads = new Thread[15];

        for (int i = 0; i < threads.length; i++) {
            final Thread testThread = new Thread(test, "jbbp_test_thread" + i);
            testThread.setDaemon(true);
            threads[i] = testThread;
            testThread.start();
        }

        for (final Thread t : threads) t.join();

        assertEquals(threads.length * ITERATIONS, parsingCounter.get());
        assertEquals(0, errorCounter.get());
    }
}
