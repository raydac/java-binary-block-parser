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

import com.igormaznitsa.jbbp.utils.JBBPSystemProperty;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.*;

/**
 * A Filter stream implementing a countable bit stream interface. It allows read
 * not only bytes but also bits from an input stream. The Class is not a
 * thread-safe one.
 */
public class JBBPBitInputStream extends FilterInputStream implements JBBPCountableBitStream {

  /**
   * The Initial an Array buffer size for whole stream read.
   */
  protected static final int INITIAL_ARRAY_BUFFER_SIZE = JBBPSystemProperty.PROPERTY_INPUT_INITIAL_ARRAY_BUFFER_SIZE.getAsInteger(32);

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
   * Flag shows that bit operations must be processed for MSB0 (most significant
   * bit 0) mode.
   */
  private final boolean msb0;

  /**
   * A Constructor, the LSB0 bit order will be used by default.
   *
   * @param in an input stream to be filtered.
   */
  public JBBPBitInputStream(final InputStream in) {
    this(in, JBBPBitOrder.LSB0);
  }

  /**
   * A Constructor.
   *
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
   * Read array of boolean values.
   *
   * @param items number of items to be read, if less than zero then read whole
   * stream till the end
   * @return read values as boolean array
   * @throws IOException it will be thrown for transport error
   */
  public boolean[] readBoolArray(final int items) throws IOException {
    int pos = 0;
    byte[] buffer;
    if (items < 0) {
      buffer = new byte[INITIAL_ARRAY_BUFFER_SIZE];
      // till end
      while (true) {
        final int read = this.read(buffer, pos, buffer.length - pos);
        if (read < 0) {
          break;
        }
        pos += read;

        if (buffer.length == pos) {
          final byte[] newbuffer = new byte[buffer.length << 1];
          System.arraycopy(buffer, 0, newbuffer, 0, buffer.length);
          buffer = newbuffer;
        }
      }
    }
    else {
      // number
      buffer = new byte[items];
      int len = items;
      while (len > 0) {
        final int read = this.read(buffer, pos, len);
        if (read < 0) {
          throw new EOFException("Have read only " + pos + " bit portions instead of " + items);
        }
        pos += read;
        len -= read;
      }
    }

    final boolean[] result = new boolean[pos];
    for (int i = 0; i < pos; i++) {
      result[i] = buffer[i] != 0;
    }
    return result;
  }

  /**
   * Read array of bit sequence.
   *
   * @param bitNumber bit number for each bit sequence item, must be 1..8
   * @param items number of items to be read, if less than zero then read whole
   * stream till the end
   * @return array of read bit items as a byte array
   * @throws IOException it will be thrown for any transport problem during the
   * operation
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
   *
   * @param items number of items to be read, if less than zero then read whole
   * stream till the end
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
      final int read = this.read(buffer, 0, items);
      if (read != items) {
        throw new EOFException("Have read only " + read + " byte(s) instead of " + items + " byte(s)");
      }
      return buffer;
    }
  }

  /**
   * Read number of short items from the input stream.
   *
   * @param items number of items to be read from the input stream, if less than
   * zero then all stream till the end will be read
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
   *
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
   *
   * @return the number of bytes read from the stream currently
   */
  public long getCounter() {
    return this.byteCounter;
  }

  /**
   * Get the value of the inside bit buffer
   *
   * @return the bit buffer state
   */
  public int getBitBuffer() {
    return this.bitBuffer;
  }

  /**
   * Get the number of buffered bits in the inside bit buffer
   *
   * @return the number of buffered bits in the bit buffer
   */
  public int getBufferedBitsNumber() {
    return this.bitsInBuffer;
  }

  /**
   * Get the bit order option for read operations.
   *
   * @return the bit order parameter
   * @see JBBPBitOrder#LSB0
   * @see JBBPBitOrder#MSB0
   */
  public JBBPBitOrder getBitOrder() {
    return this.msb0 ? JBBPBitOrder.MSB0 : JBBPBitOrder.LSB0;
  }

  /**
   * Read number of bits from the input stream. It reads bits from input stream
   * since 0 bit and make reversion to return bits in the right order when 0 bit
   * is 0 bit. if the stream is completed early than the data read then reading
   * is just stopped and read value returned. The First read bit is placed as
   * 0th bit.
   *
   * @param numOfBitsToRead the number of bits to be read, must be 1..8
   * @return the read bits as integer, -1 if the end of stream has been reached
   * @throws IOException it will be thrown for transport errors to be read
   * @throws NullPointerException if number of bits to be read is null
   */
  public int readBits(final JBBPBitNumber numOfBitsToRead) throws IOException {
    int result;

    final int numOfBitsAsNumber = numOfBitsToRead.getBitNumber();

    if (this.bitsInBuffer == 0 && numOfBitsAsNumber == 8) {
      result = this.readByteFromStream();
      this.byteCounter++;
      return result;
    }
    else {
      result = 0;

      if (numOfBitsAsNumber == this.bitsInBuffer) {
        result = this.bitBuffer;
        this.bitBuffer = 0;
        this.bitsInBuffer = 0;
        if (numOfBitsAsNumber == 8) {
          this.byteCounter++;
        }
        return result;
      }

      int i = numOfBitsAsNumber;
      int theBitBuffer = this.bitBuffer;
      int theBitBufferCounter = this.bitsInBuffer;

      while (i > 0) {
        if (theBitBufferCounter == 0) {
          final int nextByte = this.readByteFromStream();
          if (nextByte < 0) {
            if (i == numOfBitsAsNumber) {
              return nextByte;
            }
            else {
              break;
            }
          }
          else {
            theBitBuffer = nextByte;
            theBitBufferCounter = 8;
            this.byteCounter++;
          }
        }

        result = (result << 1) | (theBitBuffer & 1);
        theBitBuffer >>= 1;
        theBitBufferCounter--;
        i--;
      }

      this.bitBuffer = theBitBuffer;
      this.bitsInBuffer = theBitBufferCounter;

      return (JBBPUtils.reverseByte((byte) result) & 0xFF) >> (8 - (numOfBitsAsNumber - i));
    }
  }

  /**
   * Read a boolean value saved as a byte.
   *
   * @return true if the value is not zero, false if the value is zero
   * @throws IOException it will be thrown for transport errors.
   */
  public boolean readBoolean() throws IOException {
    final int read = this.read();
    if (read < 0) {
      throw new EOFException("Can't read a boolean value");
    }
    return read != 0;
  }

  /**
   * Read a byte value.
   *
   * @return the byte value read from the stream
   * @throws IOException it will be thrown for transport errors.
   */
  public int readByte() throws IOException {
    final int read = this.read();
    if (read < 0) {
      throw new EOFException("Can't read a byte value");
    }
    return read;
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

  /**
   * Read padding bytes from the stream and ignore them to align the stream
   * counter.
   *
   * @param alignByteNumber the byte number to align the stream
   * @throws IOException it will be thrown for transport errors
   * @throws EOFException it will be thrown if the stream end has been reached
   * the before align border.
   */
  public void align(final long alignByteNumber) throws IOException {
    this.alignByte();

    if (alignByteNumber > 0) {
      long padding = (alignByteNumber - (this.byteCounter % alignByteNumber)) % alignByteNumber;

      while (padding > 0) {
        final int skippedByte = this.read();
        if (skippedByte < 0) {
          throw new EOFException("Can't align for " + alignByteNumber + " byte(s)");
        }
        padding--;
      }
    }

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
   *
   * @return the read byte or -1 if the end of the stream has been reached
   * @throws IOException it will be thrown for transport errors
   */
  private int readByteFromStream() throws IOException {
    int result = this.in.read();
    if (result >= 0) {
      if (this.msb0) {
        result = JBBPUtils.reverseByte((byte) result) & 0xFF;
      }
    }
    return result;
  }

  /**
   * Read the next stream byte into bit buffer.
   *
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
   * Align read byte. If there are unread bits in inside bit buffer, it will be
   * aligned to read the next byte from stream instead of reading from the bit
   * buffer.
   */
  public void alignByte() {
    if (this.bitsInBuffer > 0 && this.bitsInBuffer < 8) {
      this.bitsInBuffer = 0;
    }
  }

  /**
   * Check that there is available data to read from stream. NB: It will read
   * one byte from the stream and will save it into inside bit buffer.
   *
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
      int readBytes = 0;
      int tmpoffset = offset;
      int tmplen = length;
      while (tmplen > 0) {
        int read = this.in.read(array, tmpoffset, tmplen);
        if (read < 0) {
          readBytes = readBytes == 0 ? read : readBytes;
          break;
        }
        tmplen -= read;
        tmpoffset += read;
        readBytes += read;
        this.byteCounter += read;
      }

      if (this.msb0) {
        int index = offset;
        int number = readBytes;
        while (number > 0) {
          array[index] = JBBPUtils.reverseByte(array[index]);
          index++;
          number--;
        }
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

  /**
   * Reset the byte counter for the stream. The Inside bit buffer will be reset
   * also if it is not full.
   *
   * @see #align(long)
   * @see #alignByte()
   * @see #getBitBuffer()
   * @see #getBufferedBitsNumber()
   * @see #hasAvailableData() .
   */
  public void resetCounter() {
    if (this.bitsInBuffer < 8) {
      this.bitsInBuffer = 0;
      this.bitBuffer = 0;
    }
    this.byteCounter = 0L;
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
      if (result >= 0) {
        this.byteCounter++;
      }
      return result;
    }
    else {
      return this.readBits(JBBPBitNumber.BITS_8);
    }
  }

}
