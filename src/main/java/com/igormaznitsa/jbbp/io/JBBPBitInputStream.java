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

import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.*;

/**
 * A Filter stream implementing a countable bit stream interface. It allows read not only bytes but also bits from an input stream. The Class is not a thread-safe one.
 */
public class JBBPBitInputStream extends FilterInputStream implements JBBPCountableBitStream {

  private static final int INITIAL_ARRAY_BUFFER_SIZE = 32;

  /**
   * The Inside bit buffer,
   */
  private int bitBuffer;
  /**
   * The Inside counter of bits in the bit buffer.
   */
  private int bitsInBuffer;
  /**
   * The Byte counter.
   */
  private long byteCounter;

  /**
   * Inside temp variable to keep the bit buffer temporarily.
   */
  private int markedBitBuffer;
  /**
   * Inside temp variable to keep the bit buffer counter temporarily.
   */
  private int markedBitsInBuffer;
  /**
   * Inside temp variable to keep the byte counter temporarily.
   */
  private long markedByteCounter;

  /**
   * Flag shows that bit operations must be processed for MSB0 (most significant bit 0) mode.
   */
  private final boolean msb0;

  /**
   * A Constructor, the LSB0 bit order will be used by default.
   * @param in an input stream to be filtered.
   */
  public JBBPBitInputStream(final InputStream in) {
    this(in, JBBPBitOrder.LSB0);
  }

  /**
   * A Constructor.
   * @param in an input stream to be filtered.
   * @param order a bit order mode for the filter.
   * @see JBBPBitOrder#LSB0
   * @see JBBPBitOrder#MSB0
   */
  public JBBPBitInputStream(final InputStream in, final JBBPBitOrder order) {
    super(in);
    this.bitsInBuffer = 0;
    this.msb0 = order == JBBPBitOrder.MSB0;
  }

  /**
   * Read array of bit sequence.
   * @param bitNumber bit number for each bit sequence item, must be 1..8
   * @param items number of items to be read, if less than zero then read whole stream till the end
   * @return array of read bit items as a byte array
   * @throws IOException it will be thrown for any transport problem during the operation
   */
  public byte[] readBitsArray(final int items, final JBBPBitNumber bitNumber) throws IOException {
    int pos = 0;
    if (items < 0) {
      byte[] buffer = new byte[INITIAL_ARRAY_BUFFER_SIZE];
      // till end
      while (true) {
        final int next = readBits(bitNumber);
        if (next < 0) {
          break;
        }
        if (buffer.length == pos) {
          final byte[] newbuffer = new byte[buffer.length << 1];
          System.arraycopy(buffer, 0, newbuffer, 0, buffer.length);
          buffer = newbuffer;
        }
        buffer[pos++] = (byte) next;
      }
      if (buffer.length == pos) {
        return buffer;
      }
      final byte[] result = new byte[pos];
      System.arraycopy(buffer, 0, result, 0, pos);
      return result;
    }
    else {
      // number
      final byte[] buffer = new byte[items];
      for (int i = 0; i < items; i++) {
        final int next = readBits(bitNumber);
        if (next < 0) {
          throw new EOFException("Have read only " + i + " bit portions instead of " + items);
        }
        buffer[i] = (byte) next;
      }
      return buffer;
    }
  }

  /**
   * Read number of bytes for the stream.
   * @param items number of items to be read, if less than zero then read whole stream till the end
   * @return read byte items as a byte array
   * @throws IOException it will be thrown for any transport problem during the
   * operation
   */
  public byte[] readByteArray(final int items) throws IOException {
    int pos = 0;
    if (items < 0) {
      byte[] buffer = new byte[INITIAL_ARRAY_BUFFER_SIZE];
      // till end
      while (true) {
        final int next = read();
        if (next < 0) {
          break;
        }
        if (buffer.length == pos) {
          final byte[] newbuffer = new byte[buffer.length << 1];
          System.arraycopy(buffer, 0, newbuffer, 0, buffer.length);
          buffer = newbuffer;
        }
        buffer[pos++] = (byte) next;
      }
      if (buffer.length == pos) {
        return buffer;
      }
      final byte[] result = new byte[pos];
      System.arraycopy(buffer, 0, result, 0, pos);
      return result;
    }
    else {
      // number
      final byte[] buffer = new byte[items];
      for (int i = 0; i < items; i++) {
        final int next = read();
        if (next < 0) {
          throw new EOFException("Have read only " + i + " bit portions instead of " + items);
        }
        buffer[i] = (byte) next;
      }
      return buffer;
    }
  }

  /**
   * Read number of short items from the input stream.
   * @param items number of items to be read from the input stream, if less than zero then all stream till the end will be read
   * @param byteOrder the order of bytes to be used to decode short values
   * @return read items as a short array
   * @throws IOException it will be thrown for any transport problem during the
   * operation
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   */
  public short[] readShortArray(final int items, final JBBPByteOrder byteOrder) throws IOException {
    int pos = 0;
    if (items < 0) {
      short[] buffer = new short[INITIAL_ARRAY_BUFFER_SIZE];
      // till end
      while (hasAvailableData()) {
        final int next = readUnsignedShort(byteOrder);
        if (buffer.length == pos) {
          final short[] newbuffer = new short[buffer.length << 1];
          System.arraycopy(buffer, 0, newbuffer, 0, buffer.length);
          buffer = newbuffer;
        }
        buffer[pos++] = (short) next;
      }
      if (buffer.length == pos) {
        return buffer;
      }
      final short[] result = new short[pos];
      System.arraycopy(buffer, 0, result, 0, pos);
      return result;
    }
    else {
      // number
      final short[] buffer = new short[items];
      for (int i = 0; i < items; i++) {
        buffer[i] = (short) readUnsignedShort(byteOrder);
      }
      return buffer;
    }
  }

  /**
   * Read number of integer items from the input stream.
   *
   * @param items number of items to be read from the input stream, if less than
   * zero then all stream till the end will be read
   * @param byteOrder the order of bytes to be used to decode values
   * @return read items as an integer array
   * @throws IOException it will be thrown for any transport problem during the
   * operation
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   */
  public int[] readIntArray(final int items, final JBBPByteOrder byteOrder) throws IOException {
    int pos = 0;
    if (items < 0) {
      int[] buffer = new int[INITIAL_ARRAY_BUFFER_SIZE];
      // till end
      while (hasAvailableData()) {
        final long next = readInt(byteOrder);
        if (buffer.length == pos) {
          final int[] newbuffer = new int[buffer.length << 1];
          System.arraycopy(buffer, 0, newbuffer, 0, buffer.length);
          buffer = newbuffer;
        }
        buffer[pos++] = (int) next;
      }
      if (buffer.length == pos) {
        return buffer;
      }
      final int[] result = new int[pos];
      System.arraycopy(buffer, 0, result, 0, pos);
      return result;
    }
    else {
      // number
      final int[] buffer = new int[items];
      for (int i = 0; i < items; i++) {
        buffer[i] = readInt(byteOrder);
      }
      return buffer;
    }
  }

  /**
   * Read number of long items from the input stream.
   *
   * @param items number of items to be read from the input stream, if less than
   * zero then all stream till the end will be read
   * @param byteOrder the order of bytes to be used to decode values
   * @return read items as a long array
   * @throws IOException it will be thrown for any transport problem during the
   * operation
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   */
  public long[] readLongArray(final int items, final JBBPByteOrder byteOrder) throws IOException {
    int pos = 0;
    if (items < 0) {
      long[] buffer = new long[INITIAL_ARRAY_BUFFER_SIZE];
      // till end
      while (hasAvailableData()) {
        final long next = readLong(byteOrder);
        if (buffer.length == pos) {
          final long[] newbuffer = new long[buffer.length << 1];
          System.arraycopy(buffer, 0, newbuffer, 0, buffer.length);
          buffer = newbuffer;
        }
        buffer[pos++] = next;
      }
      if (buffer.length == pos) {
        return buffer;
      }
      final long[] result = new long[pos];
      System.arraycopy(buffer, 0, result, 0, pos);
      return result;
    }
    else {
      // number
      final long[] buffer = new long[items];
      for (int i = 0; i < items; i++) {
        buffer[i] = readLong(byteOrder);
      }
      return buffer;
    }
  }

  /**
   * Read a unsigned short value from the stream.
   * @param byteOrder he order of bytes to be used to decode the read value
   * @return the unsigned short value read from stream
   * @throws IOException it will be thrown for any transport problem during the
   * operation
   * @throws EOFException if the end of the stream has been reached
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   */
  public int readUnsignedShort(final JBBPByteOrder byteOrder) throws IOException {
    final int b0 = this.read();
    if (b0 < 0) {
      throw new EOFException();
    }
    final int b1 = this.read();
    if (b1 < 0) {
      throw new EOFException();
    }
    return byteOrder == JBBPByteOrder.BIG_ENDIAN ? (b0 << 8) | b1 : (b1 << 8) | b0;
  }

  /**
   * Read an integer value from the stream.
   *
   * @param byteOrder he order of bytes to be used to decode the read value
   * @return the unsigned short value read from stream or -1 if the end of
   * stream has been reached
   * @throws IOException it will be thrown for any transport problem during the
   * operation
   * @throws EOFException if the end of the stream has been reached
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   */
  public int readInt(final JBBPByteOrder byteOrder) throws IOException {
    if (byteOrder == JBBPByteOrder.BIG_ENDIAN) {
      return (readUnsignedShort(byteOrder) << 16) | readUnsignedShort(byteOrder);
    }
    else {
      return readUnsignedShort(byteOrder) | (readUnsignedShort(byteOrder) << 16);
    }
  }

  /**
   * Read a long value from the stream.
   *
   * @param byteOrder he order of bytes to be used to decode the read value
   * @return the unsigned short value read from stream or -1 if the end of
   * stream has been reached
   * @throws IOException it will be thrown for any transport problem during the
   * operation
   * @throws EOFException if the end of the stream has been reached
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   */
  public long readLong(final JBBPByteOrder byteOrder) throws IOException {
    if (byteOrder == JBBPByteOrder.BIG_ENDIAN) {
      return (((long) readInt(byteOrder) & 0xFFFFFFFFL) << 32) | ((long) readInt(byteOrder) & 0xFFFFFFFFL);
    }
    else {
      return ((long) readInt(byteOrder) & 0xFFFFFFFFL) | (((long) readInt(byteOrder) & 0xFFFFFFFFL) << 32);
    }
  }

  /**
   * Get the current byte counter state.
   * @return the number of bytes read from the stream currently
   */
  public long getCounter() {
    return this.byteCounter;
  }

  /**
   * Get the value of the inside bit buffer
   * @return the bit buffer state
   */
  public int getBitBuffer() {
    return this.bitBuffer;
  }

  /**
   * Get the number of buffered bits in the inside bit buffer
   * @return the number of buffered bits in the bit buffer
   */
  public int getBufferedBitsNumber() {
    return this.bitsInBuffer;
  }

  /**
   * Get the bit order option for read operations.
   * @return the bit order parameter
   * @see JBBPBitOrder#LSB0
   * @see JBBPBitOrder#MSB0
   */
  public JBBPBitOrder getBitOrder() {
    return this.msb0 ? JBBPBitOrder.MSB0 : JBBPBitOrder.LSB0;
  }

  /**
   * Read number of bits from the input stream.
   * @param numOfBitsToRead the number of bits to be read, must be 1..8
   * @return the read bits as integer, -1 if the end of stream has been reached
   * @throws IOException it will be thrown for transport errors
   * @throws IllegalArgumentException it will be thrown for wrong number of bits to be read
   */
  public int readBits(final JBBPBitNumber numOfBitsToRead) throws IOException {
    JBBPUtils.assertNotNull(numOfBitsToRead, "Number of bits must not be null");
    
    int result;

    final int numOfBitsAsNumber = numOfBitsToRead.getBitNumber();
    
    if (bitsInBuffer == 0 && numOfBitsAsNumber == 8) {
      result = this.readByteFromStream();
      return result;
    }
    else {
      result = 0;
      int i = numOfBitsToRead.getBitNumber();
      int mask = 0x80;

      while (i > 0) {
        if (this.bitsInBuffer == 0) {
          final int nextByte = this.readByteFromStream();
          if (nextByte < 0) {
            if (i == numOfBitsAsNumber) {
              return nextByte;
            }
            else {
              i = 0;
              continue;
            }
          }
          else {
            this.bitsInBuffer = 8;
            this.bitBuffer = nextByte;
          }
        }

        final int bit = this.bitBuffer & 1;
        this.bitBuffer >>>= 1;
        this.bitsInBuffer--;

        result |= bit == 0 ? 0 : mask;
        mask >>>= 1;

        i--;
      }

      return JBBPUtils.reverseByte((byte) result) & 0xFF;
    }
  }

  @Override
  public boolean markSupported() {
    return super.markSupported();
  }

  @Override
  public synchronized void reset() throws IOException {
    in.reset();
    this.bitBuffer = this.markedBitBuffer;
    this.byteCounter = this.markedByteCounter;
    this.bitsInBuffer = this.markedBitsInBuffer;
  }

  @Override
  public synchronized void mark(final int readlimit) {
    in.mark(readlimit);
    this.markedBitBuffer = this.bitBuffer;
    this.markedByteCounter = this.byteCounter;
    this.markedBitsInBuffer = this.bitsInBuffer;
  }

  @Override
  public int available() throws IOException {
    return super.available();
  }

  @Override
  public long skip(final long numOfBytes) throws IOException {
    if (this.bitsInBuffer == 0) {
      final long r = in.skip(numOfBytes);
      this.byteCounter += (int) r;
      return r;
    }
    else {
      long i = numOfBytes;
      long count = 0L;
      while (i > 0) {
        final int nxt = readBits(JBBPBitNumber.BITS_8);
        if (nxt < 0) {
          break;
        }
        count++;
        i--;
      }
      return count;
    }
  }

  /**
   * Inside method to read a byte from stream.
   * @return the read byte or -1 if the end of the stream has been reached
   * @throws IOException it will be thrown for transport errors
   */
  private int readByteFromStream() throws IOException {
    int result = this.in.read();
    if (result >= 0) {
      if (this.msb0) {
        result = JBBPUtils.reverseByte((byte) result) & 0xFF;
      }
      this.byteCounter++;
    }
    return result;
  }

  /**
   * Read the next stream byte into bit buffer.
   * @return the read byte or -1 if the endo of stream has been reached.
   * @throws IOException it will be thrown for transport errors
   */
  private int loadNextByteInBuffer() throws IOException {
    final int value = this.readByteFromStream();
    if (value < 0) {
      return value;
    }

    this.bitBuffer = value;
    this.bitsInBuffer = 8;

    return value;
  }

  /**
   * Align read byte. If there are unread bits in inside bit buffer, it will be aligned to read the next byte from stream instead of reading from the bit buffer. 
   */
  public void alignByte() {
    if (this.bitsInBuffer > 0 && this.bitsInBuffer<8) {
      this.bitsInBuffer = 0;
    }
  }

  /**
   * Check that there is available data to read from stream.
   * @return true if there is data to be read
   * @throws IOException it will be thrown for transport errors
   */
  public boolean hasAvailableData() throws IOException {
    if (this.bitsInBuffer > 0) {
      return true;
    }
    return loadNextByteInBuffer() >= 0;
  }

  @Override
  public int read(final byte[] array, final int offset, final int length) throws IOException {
    if (this.bitsInBuffer == 0) {
      int readBytes;
      readBytes = 0;
      int i = offset;
      int y = length;
      while (y > 0) {
        int value = this.read();
        if (value < 0) {
          break;
        }
        array[i++] = (byte) value;
        y--;
        readBytes++;
      }
      return readBytes;
    }
    else {
      int count = length;
      int i = offset;
      while (count > 0) {
        final int nextByte = this.readBits(JBBPBitNumber.BITS_8);
        if (nextByte < 0) {
          break;
        }
        count--;
        array[i++] = (byte) nextByte;
      }
      return length - count;
    }
  }

  @Override
  public int read(final byte[] array) throws IOException {
    return this.read(array, 0, array.length);
  }

  @Override
  public int read() throws IOException {
    final int result;
    if (this.bitsInBuffer == 0) {
      result = this.readByteFromStream();
      if (result < 0) {
        return result;
      }
      return result;
    }
    else {
      return this.readBits(JBBPBitNumber.BITS_8);
    }
  }

}
