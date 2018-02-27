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
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.io.JBBPOut;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import com.igormaznitsa.jbbp.utils.JBBPUtils;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TAP_ParsingTest extends AbstractParserIntegrationTest {
    public static final JBBPParser HEADER_PARSER = JBBPParser.prepare("byte type; byte [10] name; <ushort length; <ushort param1; <ushort param2;");
    public static final JBBPParser DATA_PARSER = JBBPParser.prepare("byte [_] data;");
    public static final JBBPParser TAP_FILE_PARSER = JBBPParser.prepare("tapblocks [_]{ <ushort len; byte flag; byte [len-2] data; byte checksum;}");

    private static void assertTapChecksum(final byte etalon, final byte initial, final byte[] data) {
        byte accum = initial;
        for (byte b : data) {
            accum ^= b;
        }
        assertEquals(etalon, accum, "Checkcode must be the same");
    }

    @Test
    public void testParseTap() throws Exception {
        final TapData[] parsedBlocks;
        final InputStream in = getResourceAsInputStream("test.tap");
        try {
            final TapContainer tap = TAP_FILE_PARSER.parse(in).mapTo(TapContainer.class);
            assertEquals(89410, TAP_FILE_PARSER.getFinalStreamByteCounter());

            assertEquals(6, tap.tapblocks.length);
            for (Tap t : tap.tapblocks) {
                assertTapChecksum(t.checksum, t.flag, t.data);
            }

            parsedBlocks = new TapData[tap.tapblocks.length];
            for (int i = 0; i < tap.tapblocks.length; i++) {
                final Tap t = tap.tapblocks[i];
                final TapData td;
                switch (t.flag & 0xFF) {
                    case 0: {
                        // header
                        td = HEADER_PARSER.parse(t.data).mapTo(Header.class);
                        ((Header) td).check = t.checksum;
                    }
                    break;
                    case 0xFF: {
                        // data
                        td = DATA_PARSER.parse(t.data).mapTo(Data.class);
                    }
                    break;
                    default: {
                        fail("Unexpected block type [0x" + Integer.toHexString(t.flag & 0xFF) + ']');
                        td = null;
                    }
                    break;
                }
                parsedBlocks[i] = td;
            }

        } finally {
            JBBPUtils.closeQuietly(in);
        }

        final JBBPOut ctx = JBBPOut.BeginBin(JBBPByteOrder.LITTLE_ENDIAN);
        for (final TapData td : parsedBlocks) {
            td.save(ctx);
        }

        assertResource("test.tap", ctx.End().toByteArray());
    }

    @Bin
    static abstract class TapData {
        abstract void save(JBBPOut ctx) throws IOException;
    }

    static class Header extends TapData {
        byte type;
        @Bin(type = BinType.BYTE_ARRAY)
        String name;
        @Bin(type = BinType.USHORT)
        int length;
        @Bin(type = BinType.USHORT)
        int param1;
        @Bin(type = BinType.USHORT)
        int param2;

        transient byte check;

        @Override
        public String toString() {
            return "HEADER: " + name + " (length=" + length + ", param1=" + param1 + ", param2=" + param2 + ')';
        }

        @Override
        void save(final JBBPOut ctx) throws IOException {
            ctx.Short(19).Byte(0, type).ResetCounter().Byte(name).Align(10).Short(length).Short(param1).Short(param2).Byte(check);
        }
    }

    static class Data extends TapData {
        byte[] data;

        @Override
        public String toString() {
            return "DATA: length=" + (data.length - 1);
        }

        private byte calculateCheckSum() {
            byte a = (byte) 0xFF;
            for (byte b : data) {
                a ^= b;
            }
            return a;
        }

        @Override
        void save(final JBBPOut ctx) throws IOException {
            ctx.Short(data.length + 2).Byte(0xFF).Byte(data).Byte(calculateCheckSum());
        }
    }

    @Bin
    static class Tap {
        byte flag;
        byte[] data;
        byte checksum;
    }

    @Bin
    static class TapContainer {
        Tap[] tapblocks;
    }

}
