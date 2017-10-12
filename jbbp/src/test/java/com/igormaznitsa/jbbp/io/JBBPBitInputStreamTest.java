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
package com.igormaznitsa.jbbp.io;

import com.igormaznitsa.jbbp.utils.JBBPUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.*;

public class JBBPBitInputStreamTest {

    private static final String TEST_BYTES = "01001100_01110000_11110000_01111100_00001111_11000000_01100111_00000000_10011111";
    private static final String TEST_BYTES_EXTRABIT = "0 01001100_01110000_11110000_01111100_00001111_11000000_01100111_00000000_10011111";

    private static byte[] intArrayToByteArray(final int... array) {
        final byte[] bytearray = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            if ((array[i] & 0xFFFFFF00) != 0) {
                fail("Unconvertable byte value [" + array[i] + ']');
            }
            bytearray[i] = (byte) array[i];
        }
        return bytearray;
    }

    private static JBBPBitInputStream asInputStream(final int... array) {
        return new JBBPBitInputStream(new ByteArrayInputStream(intArrayToByteArray(array)));
    }

    private static JBBPBitInputStream asInputStreamMSB0(final int... array) {
        return new JBBPBitInputStream(new ByteArrayInputStream(intArrayToByteArray(array)), JBBPBitOrder.MSB0);
    }

    @Test(expected = EOFException.class)
    public void testReadUnsignedShort_EOFforEmpty_BigEndian_EOFException() throws Exception {
        asInputStream().readUnsignedShort(JBBPByteOrder.BIG_ENDIAN);
    }

    @Test(expected = EOFException.class)
    public void testReadUnsignedShort_EOFforEmpty_LittleEndian_EOFException() throws Exception {
        asInputStream().readUnsignedShort(JBBPByteOrder.LITTLE_ENDIAN);
    }

    @Test(expected = EOFException.class)
    public void testReadUnsignedShort_EOFforOneByte_BigEndian_EOFException() throws Exception {
        asInputStream((byte) 1).readUnsignedShort(JBBPByteOrder.BIG_ENDIAN);
    }

    @Test(expected = EOFException.class)
    public void testReadUnsignedShort_EOFforOneByte_LittleEndian_EOFException() throws Exception {
        asInputStream((byte) 1).readUnsignedShort(JBBPByteOrder.LITTLE_ENDIAN);
    }

    @Test
    public void testReadUnsignedShort_BigEndian() throws Exception {
        assertEquals(0x1234, asInputStream(0x12, 0x34).readUnsignedShort(JBBPByteOrder.BIG_ENDIAN));
    }

    @Test
    public void testReadUnsignedShort_BigEndian_MSB0() throws Exception {
        assertEquals(0x482C, asInputStreamMSB0(0x12, 0x34).readUnsignedShort(JBBPByteOrder.BIG_ENDIAN));
    }

    @Test
    public void testReadUnsignedShort_LittleEndian() throws Exception {
        assertEquals(0x3412, asInputStream(0x12, 0x34).readUnsignedShort(JBBPByteOrder.LITTLE_ENDIAN));
    }

    @Test
    public void testReadUnsignedShort_LittleEndian_MSB0() throws Exception {
        assertEquals(0x2C48, asInputStreamMSB0(0x12, 0x34).readUnsignedShort(JBBPByteOrder.LITTLE_ENDIAN));
    }

    @Test
    public void testReadInt_BigEndian() throws Exception {
        assertEquals(0x12345678, asInputStream(0x12, 0x34, 0x56, 0x78).readInt(JBBPByteOrder.BIG_ENDIAN));
    }

    @Test(expected = EOFException.class)
    public void testReadInt_BigEndian_EOF() throws Exception {
        asInputStream(0x12, 0x34, 0x56).readInt(JBBPByteOrder.BIG_ENDIAN);
    }

    @Test
    public void testReadInt_BigEndian_MSB0() throws Exception {
        assertEquals(0x482C6A1E, asInputStreamMSB0(0x12, 0x34, 0x56, 0x78).readInt(JBBPByteOrder.BIG_ENDIAN));
    }

    @Test
    public void testReadFloat_BigEndian_MSB0() throws Exception {
        assertEquals(176552.47f, asInputStreamMSB0(0x12, 0x34, 0x56, 0x78).readFloat(JBBPByteOrder.BIG_ENDIAN), 0.0f);
    }

    @Test(expected = EOFException.class)
    public void testReadInt_BigEndian_MSB0_EOF() throws Exception {
        asInputStreamMSB0(0x12, 0x34, 0x56).readInt(JBBPByteOrder.BIG_ENDIAN);
    }

    @Test(expected = EOFException.class)
    public void testReadFloat_BigEndian_MSB0_EOF() throws Exception {
        asInputStreamMSB0(0x12, 0x34, 0x56).readFloat(JBBPByteOrder.BIG_ENDIAN);
    }

    @Test
    public void testReadInt_LittleEndian() throws Exception {
        assertEquals(0x78563412, asInputStream(0x12, 0x34, 0x56, 0x78).readInt(JBBPByteOrder.LITTLE_ENDIAN));
    }

    @Test
    public void testReadFloat_LittleEndian() throws Exception {
        assertEquals(1.7378244E34f, asInputStream(0x12, 0x34, 0x56, 0x78).readFloat(JBBPByteOrder.LITTLE_ENDIAN), 0.0f);
    }

    @Test(expected = EOFException.class)
    public void testReadInt_LittleEndian_EOF() throws Exception {
        asInputStream(0x12, 0x34, 0x56).readInt(JBBPByteOrder.LITTLE_ENDIAN);
    }

    @Test(expected = EOFException.class)
    public void testReadFloat_LittleEndian_EOF() throws Exception {
        asInputStream(0x12, 0x34, 0x56).readFloat(JBBPByteOrder.LITTLE_ENDIAN);
    }

    @Test
    public void testReadInt_LittleEndian_MSB0() throws Exception {
        assertEquals(0x1E6A2C48, asInputStreamMSB0(0x12, 0x34, 0x56, 0x78).readInt(JBBPByteOrder.LITTLE_ENDIAN));
    }

    @Test
    public void testReadFloat_LittleEndian_MSB0() throws Exception {
        assertEquals(1.2397014E-20f, asInputStreamMSB0(0x12, 0x34, 0x56, 0x78).readFloat(JBBPByteOrder.LITTLE_ENDIAN) ,0.0f);
    }

    @Test(expected = EOFException.class)
    public void testReadInt_LittleEndian_MSB0_EOF() throws Exception {
        asInputStreamMSB0(0x12, 0x34, 0x56).readInt(JBBPByteOrder.LITTLE_ENDIAN);
    }

    @Test(expected = EOFException.class)
    public void testReadFloat_LittleEndian_MSB0_EOF() throws Exception {
        asInputStreamMSB0(0x12, 0x34, 0x56).readFloat(JBBPByteOrder.LITTLE_ENDIAN);
    }

    @Test
    public void testReadLong_BigEndian() throws Exception {
        assertEquals(0x12345678AABBCCDDL, asInputStream(0x12, 0x34, 0x56, 0x78, 0xAA, 0xBB, 0xCC, 0xDD).readLong(JBBPByteOrder.BIG_ENDIAN));
    }

    @Test
    public void testReadDouble_BigEndian() throws Exception {
        assertEquals(5.626349538661693E-221d, asInputStream(0x12, 0x34, 0x56, 0x78, 0xAA, 0xBB, 0xCC, 0xDD).readDouble(JBBPByteOrder.BIG_ENDIAN), 0.0d);
    }

    @Test(expected = EOFException.class)
    public void testReadLong_BigEndian_EOF() throws Exception {
        asInputStream(0x12, 0x34, 0x56, 0x78, 0xAA, 0xBB, 0xCC).readLong(JBBPByteOrder.BIG_ENDIAN);
    }

    @Test(expected = EOFException.class)
    public void testReadDouble_BigEndian_EOF() throws Exception {
        asInputStream(0x12, 0x34, 0x56, 0x78, 0xAA, 0xBB, 0xCC).readDouble(JBBPByteOrder.BIG_ENDIAN);
    }

    @Test
    public void testReadLong_LittleEndian() throws Exception {
        assertEquals(0xDDCCBBAA78563412L, asInputStream(0x12, 0x34, 0x56, 0x78, 0xAA, 0xBB, 0xCC, 0xDD).readLong(JBBPByteOrder.LITTLE_ENDIAN));
    }

    @Test
    public void testReadDouble_LittleEndian() throws Exception {
        assertEquals(-7.00761088740633E143d, asInputStream(0x12, 0x34, 0x56, 0x78, 0xAA, 0xBB, 0xCC, 0xDD).readDouble(JBBPByteOrder.LITTLE_ENDIAN), 0.0d);
    }

    @Test(expected = EOFException.class)
    public void testReadLong_LittleEndian_EOF() throws Exception {
        asInputStream(0x12, 0x34, 0x56, 0x78, 0xAA, 0xBB, 0xCC).readLong(JBBPByteOrder.LITTLE_ENDIAN);
    }

    @Test(expected = EOFException.class)
    public void testReadDouble_LittleEndian_EOF() throws Exception {
        asInputStream(0x12, 0x34, 0x56, 0x78, 0xAA, 0xBB, 0xCC).readDouble(JBBPByteOrder.LITTLE_ENDIAN);
    }

    @Test
    public void testRead9bit() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{(byte) 0xDA, 1}));

        assertEquals(0xA, in.readBits(JBBPBitNumber.BITS_4));
        assertEquals(0x1D, in.readBits(JBBPBitNumber.BITS_5));
        assertEquals(0, in.read());
        assertEquals(-1, in.read());
    }

    @Test
    public void testRead9bit_MSB0() throws Exception {
        final JBBPBitInputStream in = asInputStreamMSB0(0xD9, 1);

        assertEquals(0x0B, in.readBits(JBBPBitNumber.BITS_4));
        assertEquals(0x09, in.readBits(JBBPBitNumber.BITS_5));
        assertEquals(0x40, in.read());
        assertEquals(-1, in.read());
    }

    @Test
    public void testGetBitBuffer() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{(byte) 0xAA}));
        assertEquals(0, in.getBitBuffer());
        assertEquals(0xA, in.readBits(JBBPBitNumber.BITS_4));
        assertEquals(0xA, in.getBitBuffer());
    }

    @Test
    public void testGetOrder() throws Exception {
        assertEquals(JBBPBitOrder.MSB0, new JBBPBitInputStream(null, JBBPBitOrder.MSB0).getBitOrder());
        assertEquals(JBBPBitOrder.LSB0, new JBBPBitInputStream(null, JBBPBitOrder.LSB0).getBitOrder());
        assertEquals(JBBPBitOrder.LSB0, new JBBPBitInputStream(null).getBitOrder());
    }

    @Test
    public void testAlignByte() throws Exception {
        final byte[] testarray = JBBPUtils.str2bin("01111001 10111000");

        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(testarray));
        in.alignByte();
        assertEquals(0, in.getCounter());
        assertEquals(0x19, in.readBits(JBBPBitNumber.BITS_5));
        assertEquals(0, in.getCounter());
        in.alignByte();
        assertEquals(1, in.getCounter());
        assertEquals(0x8, in.readBits(JBBPBitNumber.BITS_4));
        assertEquals(1, in.getCounter());
        assertEquals(0xB, in.readBits(JBBPBitNumber.BITS_4));
        assertEquals(2, in.getCounter());
        assertEquals(-1, in.read());
        assertEquals(2, in.getCounter());
    }

    @Test
    public void testAlignByte_IfWeHaveBufferedByteInBitBuffer() throws Exception {
        final byte[] testarray = JBBPUtils.str2bin("01111001 10111000");

        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(testarray));
        assertEquals(0x19, in.readBits(JBBPBitNumber.BITS_5));
        assertEquals(0, in.getCounter());
        assertEquals(0x03, in.readBits(JBBPBitNumber.BITS_3));
        assertEquals(1, in.getCounter());
        assertTrue(in.hasAvailableData());
        assertEquals(1, in.getCounter());
        in.alignByte();
        assertEquals(1, in.getCounter());
        assertEquals(0xB8, in.read());
        assertEquals(-1, in.read());
        assertEquals(2, in.getCounter());
    }

    @Test
    public void testReadStream_AsBits() throws IOException {
        final byte[] testarray = JBBPUtils.str2bin(TEST_BYTES, JBBPBitOrder.MSB0);

        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(testarray));

        assertEquals(0, in.readBits(JBBPBitNumber.BITS_1));//0
        assertEquals(1, in.readBits(JBBPBitNumber.BITS_1));
        assertEquals(0, in.getCounter());
        assertEquals(6, in.getBufferedBitsNumber());

        assertEquals(0, in.readBits(JBBPBitNumber.BITS_2));//2
        assertEquals(3, in.readBits(JBBPBitNumber.BITS_2));
        assertEquals(0, in.getCounter());
        assertEquals(2, in.getBufferedBitsNumber());

        assertEquals(0, in.readBits(JBBPBitNumber.BITS_3));//6
        assertEquals(7, in.readBits(JBBPBitNumber.BITS_3));
        assertEquals(1, in.getCounter());
        assertEquals(4, in.getBufferedBitsNumber());

        assertEquals(0, in.readBits(JBBPBitNumber.BITS_4));//12
        assertEquals(2, in.getCounter());
        assertEquals(15, in.readBits(JBBPBitNumber.BITS_4));
        assertEquals(2, in.getCounter());
        assertEquals(4, in.getBufferedBitsNumber());

        assertEquals(0, in.readBits(JBBPBitNumber.BITS_5));//20
        assertEquals(31, in.readBits(JBBPBitNumber.BITS_5));
        assertEquals(3, in.getCounter());
        assertEquals(2, in.getBufferedBitsNumber());

        assertEquals(0, in.readBits(JBBPBitNumber.BITS_6));//30
        assertEquals(63, in.readBits(JBBPBitNumber.BITS_6));
        assertEquals(5, in.getCounter());
        assertEquals(6, in.getBufferedBitsNumber());

        assertEquals(0, in.readBits(JBBPBitNumber.BITS_7));//42
        assertEquals(0x73, in.readBits(JBBPBitNumber.BITS_7));
        assertEquals(7, in.getCounter());
        assertEquals(0, in.getBufferedBitsNumber());

        assertEquals(0, in.readBits(JBBPBitNumber.BITS_8));//56
        assertEquals(8, in.getCounter());
        assertEquals(0, in.getBufferedBitsNumber());

        assertEquals(0xF9, in.readBits(JBBPBitNumber.BITS_8));//64
        assertEquals(9, in.getCounter());
        assertEquals(0, in.getBufferedBitsNumber());

        assertEquals(-1, in.read());

        assertEquals(9, in.getCounter());
    }

    @Test
    public void testReadStream_BitByBit() throws IOException {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{0x01}));
        assertEquals(1, in.readBits(JBBPBitNumber.BITS_1));
        assertEquals(0, in.getCounter());
        assertEquals(0, in.readBits(JBBPBitNumber.BITS_1));
        assertEquals(0, in.readBits(JBBPBitNumber.BITS_1));
        assertEquals(0, in.readBits(JBBPBitNumber.BITS_1));
        assertEquals(0, in.readBits(JBBPBitNumber.BITS_1));
        assertEquals(0, in.readBits(JBBPBitNumber.BITS_1));
        assertEquals(0, in.readBits(JBBPBitNumber.BITS_1));
        assertEquals(0, in.readBits(JBBPBitNumber.BITS_1));
        assertEquals(-1, in.readBits(JBBPBitNumber.BITS_1));
        assertEquals(1, in.getCounter());
    }

    @Test
    public void testReadStream_7bits_Default() throws IOException {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils.str2bin("11011100", JBBPBitOrder.MSB0)));
        assertEquals(0x3B, in.readBits(JBBPBitNumber.BITS_7));
        assertEquals(0, in.getCounter());
    }

    @Test
    public void testReadStream_AsBytes() throws IOException {
        final byte[] testarray = JBBPUtils.str2bin(TEST_BYTES, JBBPBitOrder.MSB0);

        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(testarray));

        for (int i = 0; i < testarray.length; i++) {
            assertEquals("Byte " + i, testarray[i] & 0xFF, in.read());
        }
        assertEquals(9, in.getCounter());
        assertEquals(-1, in.read());
    }

    @Test
    public void testReadStream_AsArray() throws IOException {
        final byte[] testarray = JBBPUtils.str2bin(TEST_BYTES, JBBPBitOrder.MSB0);

        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(testarray));

        final byte[] read = new byte[9];
        assertEquals(9, in.read(read));
        assertEquals(-1, in.read());
        assertEquals(9, in.getCounter());

        assertArrayEquals(testarray, read);
    }

    @Test
    public void testReadStream_AsPartOfArray() throws IOException {
        final byte[] testarray = JBBPUtils.str2bin(TEST_BYTES, JBBPBitOrder.MSB0);

        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(testarray));

        final byte[] buff = new byte[27];
        assertEquals(5, in.read(buff, 9, 5));
        assertEquals(5, in.getCounter());
        assertEquals(3, in.read());

        for (int i = 0; i < 9; i++) {
            assertEquals(0, buff[i]);
        }

        for (int i = 9; i < 14; i++) {
            assertEquals(testarray[i - 9], buff[i]);
        }

        for (int i = 14; i < 27; i++) {
            assertEquals(0, buff[i]);
        }

    }

    @Test
    public void testReadStream_AsPartOfArray_MSB0() throws IOException {
        final byte[] testarray = JBBPUtils.str2bin(TEST_BYTES, JBBPBitOrder.MSB0);

        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(testarray), JBBPBitOrder.MSB0);

        final byte[] buff = new byte[27];
        assertEquals(5, in.read(buff, 9, 5));
        assertEquals(5, in.getCounter());
        assertEquals(0xC0, in.read());

        for (int i = 0; i < 9; i++) {
            assertEquals(0, buff[i]);
        }

        for (int i = 9; i < 14; i++) {
            assertEquals(JBBPUtils.reverseBitsInByte(testarray[i - 9]), buff[i]);
        }

        for (int i = 14; i < 27; i++) {
            assertEquals(0, buff[i]);
        }

    }

    @Test
    public void testReadStream_AsPartOfArray_1bitOffset() throws IOException {
        final byte[] testarray = JBBPUtils.str2bin(TEST_BYTES, JBBPBitOrder.MSB0);

        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils.str2bin(TEST_BYTES_EXTRABIT, JBBPBitOrder.MSB0)));

        assertEquals(0, in.readBits(JBBPBitNumber.BITS_1));

        final byte[] read = new byte[27];
        assertEquals(5, in.read(read, 9, 5));
        assertEquals(5, in.getCounter());
        assertEquals(3, in.read());

        for (int i = 0; i < 9; i++) {
            assertEquals(0, read[i]);
        }

        for (int i = 9; i < 14; i++) {
            assertEquals(testarray[i - 9], read[i]);
        }

        for (int i = 14; i < 27; i++) {
            assertEquals(0, read[i]);
        }

    }

    @Test
    public void testMarkForReadBits() throws IOException {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils.str2bin("10010110_00101000_10101010", JBBPBitOrder.MSB0)));

        assertEquals(0x9, in.readBits(JBBPBitNumber.BITS_4));
        assertEquals(0x6, in.readBits(JBBPBitNumber.BITS_6));

        assertTrue(in.markSupported());

        in.mark(1024);

        assertEquals(5, in.readBits(JBBPBitNumber.BITS_3));
        assertEquals(0, in.readBits(JBBPBitNumber.BITS_3));
        assertEquals(0x55, in.read());

        in.reset();

        assertEquals(5, in.readBits(JBBPBitNumber.BITS_3));
        assertEquals(0, in.readBits(JBBPBitNumber.BITS_3));
        assertEquals(0x55, in.read());

        assertEquals(-1, in.read());
    }

    @Test
    public void testReadBits_ExceptionForWrongArgument() throws Exception {
        final JBBPBitInputStream inLe = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils.str2bin(TEST_BYTES, JBBPBitOrder.LSB0)));

        try {
            inLe.readBits(JBBPBitNumber.decode(0));
            fail("Must throw IAE");
        } catch (IllegalArgumentException ex) {

        }

        try {
            inLe.readBits(JBBPBitNumber.decode(-5));
            fail("Must throw IAE");
        } catch (IllegalArgumentException ex) {

        }

        try {
            inLe.readBits(JBBPBitNumber.decode(9));
            fail("Must throw IAE");
        } catch (IllegalArgumentException ex) {

        }
    }

    @Test
    public void testSkipBytes() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils.str2bin("01010101_01010101_01010101_00011000_01010101_01010101_00000001", JBBPBitOrder.MSB0)));

        assertEquals(3, in.skip(3));
        assertEquals(0x18, in.read());
        assertEquals(2, in.skip(2));
        assertEquals(0x80, in.read());
        assertEquals(-1, in.read());
    }

    @Test
    public void testAlignBytes() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils.str2bin("01010101_01010101_01011101_00011000_01010101_01010101_00000001", JBBPBitOrder.MSB0)));

        assertEquals(0xAA, in.read());
        in.align(3);
        assertEquals(0x18, in.read());
        in.align(6);
        assertEquals(0x80, in.read());
        assertEquals(-1, in.read());

        try {
            in.align(10);
            fail("Must throw EOF");
        } catch (EOFException ex) {

        }
    }

    @Test
    public void testRead_WithoutOffset() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils.str2bin("01010111_01010111_01010111_00011000_01010101_01100011_00000001", JBBPBitOrder.MSB0)));

        assertEquals(0xEA, in.read());
        assertEquals(0xEA, in.read());
        assertEquals(0xEA, in.read());
        assertEquals(0x18, in.read());
        assertEquals(0xAA, in.read());
        assertEquals(0xC6, in.read());
        assertEquals(0x80, in.read());
        assertEquals(-1, in.read());
    }

    @Test
    public void testRead_1bitOffset() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils.str2bin("1 01010111_01010111_01010111_00011000_01010111_01100011_00101101", JBBPBitOrder.MSB0)));

        assertEquals(1, in.readBits(JBBPBitNumber.BITS_1));

        assertEquals(0xEA, in.read());
        assertEquals(0xEA, in.read());
        assertEquals(0xEA, in.read());
        assertEquals(0x18, in.read());
        assertEquals(0xEA, in.read());
        assertEquals(0xC6, in.read());
        assertEquals(0xB4, in.read());
        assertEquals(0, in.read());
        assertEquals(-1, in.read());
    }

    @Test
    public void testSkipBytes_1bitOffset() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils.str2bin("0 01010101_01010101_01010101 00011000_01010101_010110110_0000001", JBBPBitOrder.MSB0)));

        assertEquals(0, in.readBits(JBBPBitNumber.BITS_1));

        assertEquals(0, in.getCounter());

        assertEquals(3, in.skip(3));
        assertEquals(3, in.getCounter());

        assertEquals(0x18, in.read());
        assertEquals(4, in.getCounter());

        assertEquals(1, in.skip(1));
        assertEquals(5, in.getCounter());

        assertEquals(0xDA, in.read());
        assertEquals(6, in.getCounter());

        assertEquals(0x80, in.read());
        assertEquals(7, in.getCounter());

        assertEquals(0, in.read());
        assertEquals(8, in.getCounter());

        assertEquals(-1, in.read());
        assertEquals(8, in.getCounter());
    }

    @Test
    public void testReadArray_Bits_WholeStream() throws Exception {
        JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils.str2bin("00101001_11001011_10111110", JBBPBitOrder.LSB0)));
        assertArrayEquals(new byte[]{1, 2, 2, 0, 3, 2, 0, 3, 2, 3, 3, 2}, in.readBitsArray(-1, JBBPBitNumber.BITS_2));
        assertEquals(3, in.getCounter());

        final Random rnd = new Random(1234);

        final byte[] buff = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE];
        rnd.nextBytes(buff);

        in = new JBBPBitInputStream(new ByteArrayInputStream(buff));

        final byte[] read = in.readBitsArray(-1, JBBPBitNumber.BITS_8);
        assertEquals(buff.length, in.getCounter());

        assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE, read.length);
        for (int i = 0; i < read.length; i++) {
            assertEquals(buff[i], read[i]);
        }

        final byte[] big = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 128];
        rnd.nextBytes(big);

        in = new JBBPBitInputStream(new ByteArrayInputStream(big));

        final byte[] readbig = in.readBitsArray(-1, JBBPBitNumber.BITS_8);

        assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 128, readbig.length);
        for (int i = 0; i < readbig.length; i++) {
            assertEquals(big[i], readbig[i]);
        }
    }

    @Test
    public void testReadArray_Bits_ThreeItems() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils.str2bin("00101001_11001011_10111110", JBBPBitOrder.LSB0)));
        assertArrayEquals(new byte[]{1, 2, 2}, in.readBitsArray(3, JBBPBitNumber.BITS_2));
    }

    @Test(expected = EOFException.class)
    public void testReadArray_Bits_EOF() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(JBBPUtils.str2bin("00101001_11001011_10111110", JBBPBitOrder.LSB0)));
        in.readBitsArray(58, JBBPBitNumber.BITS_2);
    }

    @Test
    public void testReadArray_Bytes_WholeStream() throws Exception {
        JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0}));
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7, 0}, in.readByteArray(-1));
        assertEquals(8, in.getCounter());

        final Random rnd = new Random(1234);

        final byte[] buff = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE];
        rnd.nextBytes(buff);

        in = new JBBPBitInputStream(new ByteArrayInputStream(buff));

        final byte[] read = in.readByteArray(-1);

        assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE, read.length);
        for (int i = 0; i < read.length; i++) {
            assertEquals(buff[i], read[i]);
        }

        final byte[] big = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 128];
        rnd.nextBytes(big);

        in = new JBBPBitInputStream(new ByteArrayInputStream(big));

        final byte[] readbig = in.readByteArray(-1);

        assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 128, readbig.length);
        for (int i = 0; i < readbig.length; i++) {
            assertEquals(big[i], readbig[i]);
        }
    }

    @Test
    public void testReadArray_Bytes_ThreeItems() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0}));
        assertArrayEquals(new byte[]{1, 2, 3,}, in.readByteArray(3));
    }

    @Test
    public void testReadArray_Bytes_BIG_ENDIAN_ThreeItems() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0}));
        assertArrayEquals(new byte[]{1, 2, 3,}, in.readByteArray(3, JBBPByteOrder.BIG_ENDIAN));
    }

    @Test
    public void testReadArray_Bytes_LITTLE_ENDIAN_ThreeItems() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0}));
        assertArrayEquals(new byte[]{3, 2, 1,}, in.readByteArray(3, JBBPByteOrder.LITTLE_ENDIAN));
    }

    @Test(expected = EOFException.class)
    public void testReadArray_Bytes_EOF() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0}));
        in.readByteArray(259);
    }

    @Test
    public void testReadArray_Short_WholeStream() throws Exception {
        JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0}));
        assertArrayEquals(new short[]{0x0102, 0x0304, 0x0506, 0x0700}, in.readShortArray(-1, JBBPByteOrder.BIG_ENDIAN));
        assertEquals(8, in.getCounter());

        final Random rnd = new Random(1234);

        final byte[] buff = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 2];
        rnd.nextBytes(buff);

        in = new JBBPBitInputStream(new ByteArrayInputStream(buff));
        final short[] read = in.readShortArray(-1, JBBPByteOrder.BIG_ENDIAN);
        assertEquals(buff.length, in.getCounter());

        assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE, read.length);
        for (int i = 0; i < read.length; i++) {
            final int val = read[i];
            final int j = i * 2;
            assertEquals(val, ((buff[j] << 8) | ((buff[j + 1] & 0xFF))));
        }

        final byte[] big = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 128];
        rnd.nextBytes(big);

        in = new JBBPBitInputStream(new ByteArrayInputStream(big));

        final short[] readbig = in.readShortArray(-1, JBBPByteOrder.BIG_ENDIAN);

        assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 64, readbig.length);
        for (int i = 0; i < readbig.length; i++) {
            final int val = readbig[i];
            final int j = i * 2;
            assertEquals(val, ((big[j] << 8) | ((big[j + 1] & 0xFF))));
        }
    }

    @Test
    public void testReadArray_UShort_WholeStream() throws Exception {
        JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0}));
        assertArrayEquals(new char[]{0x0102, 0x0304, 0x0506, 0x0700}, in.readUShortArray(-1, JBBPByteOrder.BIG_ENDIAN));
        assertEquals(8, in.getCounter());

        final Random rnd = new Random(1234);

        final byte[] buff = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 2];
        rnd.nextBytes(buff);

        in = new JBBPBitInputStream(new ByteArrayInputStream(buff));
        final char[] read = in.readUShortArray(-1, JBBPByteOrder.BIG_ENDIAN);
        assertEquals(buff.length, in.getCounter());

        assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE, read.length);
        for (int i = 0; i < read.length; i++) {
            final int val = read[i];
            final int j = i * 2;
            assertEquals(val, ((buff[j] << 8) | ((buff[j + 1] & 0xFF))) & 0xFFFF);
        }

        final byte[] big = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 128];
        rnd.nextBytes(big);

        in = new JBBPBitInputStream(new ByteArrayInputStream(big));

        final char[] readbig = in.readUShortArray(-1, JBBPByteOrder.BIG_ENDIAN);

        assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 64, readbig.length);
        for (int i = 0; i < readbig.length; i++) {
            final int val = readbig[i];
            final int j = i * 2;
            assertEquals(val, ((big[j] << 8) | ((big[j + 1] & 0xFF))) & 0xFFFF);
        }
    }

    @Test
    public void testReadArray_Short_TwoItems() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0}));
        assertArrayEquals(new short[]{0x0102, 0x0304}, in.readShortArray(2, JBBPByteOrder.BIG_ENDIAN));
        assertEquals(4, in.getCounter());
    }

    @Test
    public void testReadArray_UShort_TwoItems() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0}));
        assertArrayEquals(new char[]{0x0102, 0x0304}, in.readUShortArray(2, JBBPByteOrder.BIG_ENDIAN));
        assertEquals(4, in.getCounter());
    }

    @Test(expected = EOFException.class)
    public void testReadArray_Short_EOF() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0}));
        in.readShortArray(259, JBBPByteOrder.BIG_ENDIAN);
        assertEquals(8, in.getCounter());
    }

    @Test
    public void testReadArray_Int_WholeStream() throws Exception {
        JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01}));
        assertArrayEquals(new int[]{0x01020304, 0x05060700, 0xFECABE01}, in.readIntArray(-1, JBBPByteOrder.BIG_ENDIAN));
        assertEquals(12, in.getCounter());

        final Random rnd = new Random(1234);

        final byte[] buff = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 4];
        rnd.nextBytes(buff);

        in = new JBBPBitInputStream(new ByteArrayInputStream(buff));
        final int[] read = in.readIntArray(-1, JBBPByteOrder.BIG_ENDIAN);
        assertEquals(buff.length, in.getCounter());

        assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE, read.length);
        for (int i = 0; i < read.length; i++) {
            final int val = read[i];
            final int j = i * 4;
            assertEquals(val, ((buff[j] << 24) | ((buff[j + 1] & 0xFF) << 16) | ((buff[j + 2] & 0xFF) << 8) | (buff[j + 3] & 0xFF)));
        }

        final byte[] big = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 128];
        rnd.nextBytes(big);

        in = new JBBPBitInputStream(new ByteArrayInputStream(big));

        final int[] readbig = in.readIntArray(-1, JBBPByteOrder.BIG_ENDIAN);

        assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 32, readbig.length);
        for (int i = 0; i < readbig.length; i++) {
            final int val = readbig[i];
            final int j = i * 4;
            assertEquals(val, ((big[j] << 24) | ((big[j + 1] & 0xFF) << 16) | ((big[j + 2] & 0xFF) << 8) | (big[j + 3] & 0xFF)));
        }
    }

    @Test
    public void testReadArray_Float_WholeStream() throws Exception {
        JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01}));
        assertArrayEquals(new float[]{2.3879393E-38f, 6.3019354E-36f, -1.3474531E38f}, in.readFloatArray(-1, JBBPByteOrder.BIG_ENDIAN), 0.0f);
        assertEquals(12, in.getCounter());

        final Random rnd = new Random(1234);

        final byte[] buff = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 4];
        rnd.nextBytes(buff);

        in = new JBBPBitInputStream(new ByteArrayInputStream(buff));
        final float[] read = in.readFloatArray(-1, JBBPByteOrder.BIG_ENDIAN);
        assertEquals(buff.length, in.getCounter());

        assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE, read.length);
        for (int i = 0; i < read.length; i++) {
            final float val = read[i];
            final int j = i * 4;
            assertEquals(val, Float.intBitsToFloat((buff[j] << 24) | ((buff[j + 1] & 0xFF) << 16) | ((buff[j + 2] & 0xFF) << 8) | (buff[j + 3] & 0xFF)), 0.0f);
        }

        final byte[] big = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 128];
        rnd.nextBytes(big);

        in = new JBBPBitInputStream(new ByteArrayInputStream(big));

        final float[] readbig = in.readFloatArray(-1, JBBPByteOrder.BIG_ENDIAN);

        assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 32, readbig.length);
        for (int i = 0; i < readbig.length; i++) {
            final float val = readbig[i];
            final int j = i * 4;
            assertEquals(val, Float.intBitsToFloat((big[j] << 24) | ((big[j + 1] & 0xFF) << 16) | ((big[j + 2] & 0xFF) << 8) | (big[j + 3] & 0xFF)), 0.0f);
        }
    }

    @Test
    public void testReadArray_Int_TwoItems() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01}));
        assertArrayEquals(new int[]{0x01020304, 0x05060700}, in.readIntArray(2, JBBPByteOrder.BIG_ENDIAN));
        assertEquals(8, in.getCounter());
    }

    @Test
    public void testReadArray_Float_TwoItems() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01}));
        assertArrayEquals(new float[]{2.3879393E-38f, 6.3019354E-36f}, in.readFloatArray(2, JBBPByteOrder.BIG_ENDIAN), 0.0f);
        assertEquals(8, in.getCounter());
    }

    @Test(expected = EOFException.class)
    public void testReadArray_Int_EOF() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01}));
        in.readIntArray(259, JBBPByteOrder.BIG_ENDIAN);
    }

    @Test(expected = EOFException.class)
    public void testReadArray_DoubleInt_EOF() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01}));
        in.readFloatArray(259, JBBPByteOrder.BIG_ENDIAN);
    }

    @Test
    public void testReadArray_Long_WholeStream() throws Exception {
        JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01, 2, 3, 4, 5, 6, 7, 8, 9, 1, 2, 3, 4}));
        assertArrayEquals(new long[]{0x0102030405060700L, 0xFECABE0102030405L, 0x0607080901020304L}, in.readLongArray(-1, JBBPByteOrder.BIG_ENDIAN));
        assertEquals(24, in.getCounter());

        final Random rnd = new Random(1234);

        final byte[] buff = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 8];
        rnd.nextBytes(buff);

        in = new JBBPBitInputStream(new ByteArrayInputStream(buff));
        final long[] read = in.readLongArray(-1, JBBPByteOrder.BIG_ENDIAN);
        assertEquals(buff.length, in.getCounter());

        assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE, read.length);
        for (int i = 0; i < read.length; i++) {
            final long val = read[i];
            final int j = i * 8;
            assertEquals(val, (((long) buff[j] << 56) | (((long) buff[j + 1] & 0xFFL) << 48) | (((long) buff[j + 2] & 0xFFL) << 40) | (((long) buff[j + 3] & 0xFFL) << 32) | (((long) buff[j + 4] & 0xFFL) << 24) | (((long) buff[j + 5] & 0xFFL) << 16) | (((long) buff[j + 6] & 0xFFL) << 8) | ((long) buff[j + 7] & 0xFF)));
        }

        final byte[] big = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 128];
        rnd.nextBytes(big);

        in = new JBBPBitInputStream(new ByteArrayInputStream(big));

        final long[] readbig = in.readLongArray(-1, JBBPByteOrder.BIG_ENDIAN);

        assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 16, readbig.length);
        for (int i = 0; i < readbig.length; i++) {
            final long val = readbig[i];
            final int j = i * 8;
            assertEquals(val, (((long) big[j] << 56) | (((long) big[j + 1] & 0xFFL) << 48) | (((long) big[j + 2] & 0xFFL) << 40) | (((long) big[j + 3] & 0xFFL) << 32) | (((long) big[j + 4] & 0xFFL) << 24) | (((long) big[j + 5] & 0xFFL) << 16) | (((long) big[j + 6] & 0xFFL) << 8) | ((long) big[j + 7] & 0xFF)));
        }
    }

    @Test
    public void testReadArray_Double_WholeStream() throws Exception {
        JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01, 2, 3, 4, 5, 6, 7, 8, 9, 1, 2, 3, 4}));
        assertArrayEquals(new double[]{8.207880399131826E-304d, -5.730900111929792E302d, 1.268802825418157E-279d}, in.readDoubleArray(-1, JBBPByteOrder.BIG_ENDIAN), 0.0d);
        assertEquals(24, in.getCounter());

        final Random rnd = new Random(1234);

        final byte[] buff = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 8];
        rnd.nextBytes(buff);

        in = new JBBPBitInputStream(new ByteArrayInputStream(buff));
        final double[] read = in.readDoubleArray(-1, JBBPByteOrder.BIG_ENDIAN);
        assertEquals(buff.length, in.getCounter());

        assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE, read.length);
        for (int i = 0; i < read.length; i++) {
            final double val = read[i];
            final int j = i * 8;
            assertEquals(val, Double.longBitsToDouble(((long) buff[j] << 56) | (((long) buff[j + 1] & 0xFFL) << 48) | (((long) buff[j + 2] & 0xFFL) << 40) | (((long) buff[j + 3] & 0xFFL) << 32) | (((long) buff[j + 4] & 0xFFL) << 24) | (((long) buff[j + 5] & 0xFFL) << 16) | (((long) buff[j + 6] & 0xFFL) << 8) | ((long) buff[j + 7] & 0xFF)), 0.0d);
        }

        final byte[] big = new byte[JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 128];
        rnd.nextBytes(big);

        in = new JBBPBitInputStream(new ByteArrayInputStream(big));

        final double[] readbig = in.readDoubleArray(-1, JBBPByteOrder.BIG_ENDIAN);

        assertEquals(JBBPBitInputStream.INITIAL_ARRAY_BUFFER_SIZE * 16, readbig.length);
        for (int i = 0; i < readbig.length; i++) {
            final double val = readbig[i];
            final int j = i * 8;
            assertEquals(val, Double.longBitsToDouble(((long) big[j] << 56) | (((long) big[j + 1] & 0xFFL) << 48) | (((long) big[j + 2] & 0xFFL) << 40) | (((long) big[j + 3] & 0xFFL) << 32) | (((long) big[j + 4] & 0xFFL) << 24) | (((long) big[j + 5] & 0xFFL) << 16) | (((long) big[j + 6] & 0xFFL) << 8) | ((long) big[j + 7] & 0xFF)), 0.0d);
        }
    }

    @Test
    public void testReadArray_Long_TwoItems() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01, 2, 3, 4, 5, 6, 7, 8, 9, 1, 2, 3, 4}));
        assertArrayEquals(new long[]{0x0102030405060700L, 0xFECABE0102030405L}, in.readLongArray(2, JBBPByteOrder.BIG_ENDIAN));
        assertEquals(16, in.getCounter());
    }

    @Test
    public void testReadArray_Double_TwoItems() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01, 2, 3, 4, 5, 6, 7, 8, 9, 1, 2, 3, 4}));
        assertArrayEquals(new double[]{8.207880399131826E-304d, -5.730900111929792E302d}, in.readDoubleArray(2, JBBPByteOrder.BIG_ENDIAN), 0.0d);
        assertEquals(16, in.getCounter());
    }

    @Test(expected = EOFException.class)
    public void testReadArray_Long_EOF() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01, 2, 3, 4, 5, 6, 7, 8, 9, 1, 2, 3, 4}));
        in.readLongArray(259, JBBPByteOrder.BIG_ENDIAN);
    }

    @Test(expected = EOFException.class)
    public void testReadArray_Double_EOF() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01, 2, 3, 4, 5, 6, 7, 8, 9, 1, 2, 3, 4}));
        in.readDoubleArray(259, JBBPByteOrder.BIG_ENDIAN);
    }

    @Test
    public void testResetCounter_ForStartOfStream() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01, 2, 3, 4, 5, 6, 7, 8, 9, 1, 2, 3, 4}));
        in.resetCounter();
        assertEquals(1, in.readByte());
        assertEquals(1, in.getCounter());
    }

    @Test
    public void testResetCounter_ForCachedBits() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 0, (byte) 0xFE, (byte) 0xCA, (byte) 0xBE, (byte) 0x01, 2, 3, 4, 5, 6, 7, 8, 9, 1, 2, 3, 4}));
        assertEquals(1, in.readBits(JBBPBitNumber.BITS_3));
        assertEquals(0, in.getCounter());
        assertTrue(in.getBufferedBitsNumber() != 0);
        in.resetCounter();
        assertEquals(0, in.getCounter());
        assertEquals(0, in.getBufferedBitsNumber());
        assertEquals(2, in.readByte());
    }

    @Test(expected = EOFException.class)
    public void testReadBooleanArray_EOF() throws Exception {
        asInputStream(1, 2, 3, 4, 5, 6).readBoolArray(256);
    }

    @Test
    public void testReadBooleanArray_WholeStream() throws Exception {
        final byte[] testarray = new byte[16384];
        final Random rnd = new Random(1234);
        for (int i = 0; i < testarray.length; i++) {
            testarray[i] = rnd.nextInt(100) > 50 ? 0 : (byte) rnd.nextInt(0x100);
        }

        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(testarray));

        final boolean[] read = in.readBoolArray(-1);

        assertEquals(16384, in.getCounter());


        assertEquals(testarray.length, read.length);
        for (int i = 0; i < read.length; i++) {
            assertTrue(read[i] == (testarray[i] != 0));
        }
    }

    @Test
    public void testReadNotFullByteArrayAfterBitReading() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{(byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0xDD}));
        assertEquals(0x2, in.readBits(JBBPBitNumber.BITS_4));
        assertEquals(0, in.getCounter());

        final byte[] readarray = new byte[6];
        final int read = in.read(readarray, 0, readarray.length);
        assertEquals(4, read);
        assertEquals(4, in.getCounter());
        assertArrayEquals(new byte[]{(byte) 0x41, (byte) 0x63, (byte) 0xD5, (byte) 0x0D, 0, 0}, readarray);
    }

    @Test
    public void testReadNotFullByteArrayAfterBitReading_MSB0() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{(byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0xDD}), JBBPBitOrder.MSB0);
        assertEquals(0x8, in.readBits(JBBPBitNumber.BITS_4));

        final byte[] readarray = new byte[6];
        final int read = in.read(readarray, 0, readarray.length);

        assertEquals(4, read);
        assertEquals(4, in.getCounter());

        assertArrayEquals(new byte[]{(byte) 0xC4, (byte) 0xA2, (byte) 0xB6, (byte) 0x0B, 0, 0}, readarray);
    }

    @Test
    public void testCheckThatCounterResetDoesntResetFullBitBuffer() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 0x7F}));
        assertEquals(0, in.getBufferedBitsNumber());
        assertTrue(in.hasAvailableData());
        assertEquals(0, in.getCounter());
        assertEquals(8, in.getBufferedBitsNumber());
        in.resetCounter();
        assertEquals(0, in.getCounter());
        assertEquals(8, in.getBufferedBitsNumber());
        assertEquals(1, in.readBits(JBBPBitNumber.BITS_1));
        assertEquals(7, in.getBufferedBitsNumber());
        assertEquals(0, in.getCounter());
        in.resetCounter();
        assertEquals(0, in.getBufferedBitsNumber());
        assertEquals(0x7F, in.read());
        assertEquals(1, in.getCounter());
        assertEquals(-1, in.read());
        assertEquals(1, in.getCounter());
    }

    @Test
    public void testByteCounterWithHasAvailableData() throws Exception {
        final JBBPBitInputStream in = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}));
        assertTrue(in.hasAvailableData());
        assertEquals(0, in.getCounter());

        assertTrue(in.readBits(JBBPBitNumber.BITS_1) >= 0);
        assertTrue(in.hasAvailableData());
        assertEquals(0, in.getCounter());

        assertTrue(in.readBits(JBBPBitNumber.BITS_1) >= 0);
        assertTrue(in.hasAvailableData());
        assertEquals(0, in.getCounter());

        assertTrue(in.readBits(JBBPBitNumber.BITS_1) >= 0);
        assertTrue(in.hasAvailableData());
        assertEquals(0, in.getCounter());

        assertTrue(in.readBits(JBBPBitNumber.BITS_1) >= 0);
        assertTrue(in.hasAvailableData());
        assertEquals(0, in.getCounter());

        assertTrue(in.readBits(JBBPBitNumber.BITS_1) >= 0);
        assertTrue(in.hasAvailableData());
        assertEquals(0, in.getCounter());

        assertTrue(in.readBits(JBBPBitNumber.BITS_1) >= 0);
        assertTrue(in.hasAvailableData());
        assertEquals(0, in.getCounter());

        assertTrue(in.readBits(JBBPBitNumber.BITS_1) >= 0);
        assertTrue(in.hasAvailableData());
        assertEquals(0, in.getCounter());

        assertTrue(in.readBits(JBBPBitNumber.BITS_1) >= 0);
        assertTrue(in.hasAvailableData());
        assertEquals(1, in.getCounter());

        assertTrue(in.readBits(JBBPBitNumber.BITS_1) >= 0);
        assertTrue(in.hasAvailableData());
        assertEquals(1, in.getCounter());

        assertTrue(in.readBits(JBBPBitNumber.BITS_1) >= 0);
        assertTrue(in.hasAvailableData());
        assertEquals(1, in.getCounter());

        assertTrue(in.readBits(JBBPBitNumber.BITS_1) >= 0);
        assertTrue(in.hasAvailableData());
        assertEquals(1, in.getCounter());

        assertTrue(in.readBits(JBBPBitNumber.BITS_1) >= 0);
        assertTrue(in.hasAvailableData());
        assertEquals(1, in.getCounter());

        assertTrue(in.readBits(JBBPBitNumber.BITS_1) >= 0);
        assertTrue(in.hasAvailableData());
        assertEquals(1, in.getCounter());

        assertTrue(in.readBits(JBBPBitNumber.BITS_1) >= 0);
        assertTrue(in.hasAvailableData());
        assertEquals(1, in.getCounter());

        assertTrue(in.readBits(JBBPBitNumber.BITS_1) >= 0);
        assertTrue(in.hasAvailableData());
        assertEquals(1, in.getCounter());

        assertTrue(in.readBits(JBBPBitNumber.BITS_1) >= 0);
        assertTrue(in.hasAvailableData());
        assertEquals(2, in.getCounter());
    }

}
