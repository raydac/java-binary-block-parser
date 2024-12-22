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

import static com.igormaznitsa.jbbp.io.JBBPArraySizeLimiter.isBreakReadWholeStream;

import com.igormaznitsa.jbbp.exceptions.JBBPReachedArraySizeLimitException;
import com.igormaznitsa.jbbp.utils.JBBPSystemProperty;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A Filter stream implementing a countable bit stream interface. It allows read
 * not only bytes but also bits from an input stream. The Class is not a
 * thread-safe one.
 *
 * @since 1.0
 */
public class JBBPBitInputStream extends FilterInputStream implements JBBPCountableBitStream {

  /**
   * The Initial an Array buffer size for whole stream read.
   */
  protected static final int INITIAL_ARRAY_BUFFER_SIZE =
      JBBPSystemProperty.PROPERTY_INPUT_INITIAL_ARRAY_BUFFER_SIZE.getAsInteger(32);
  /**
   * Contains bit mode for bit operations.
   */
  private final JBBPBitOrder bitOrderMode;
  /**
   * Internal bit buffer,
   */
  private int bitBuffer;
  /**
   * Internal counter of bits in the bit buffer.
   */
  private int bitsInBuffer;
  /**
   * The Byte counter.
   */
  private long byteCounter;
  /**
   * Internal temp variable to keep the bit buffer temporarily.
   */
  private int markedBitBuffer;
  /**
   * Internal temp variable to keep the bit buffer counter temporarily.
   */
  private int markedBitsInBuffer;
  /**
   * Internal temp variable to keep the byte counter temporarily.
   */
  private long markedByteCounter;

  /**
   * Internal flag shows that read stopped for whole stream array read limit reach.
   *
   * @since 2.1.0
   */
  private boolean detectedArrayLimit;

  /**
   * Flag shows that read of array was stopped for array limiter restrictions.
   *
   * @return true if array limiter restrictions detected, false otherwise
   * @see JBBPArraySizeLimiter
   * @since 2.1.0
   */
  public boolean isDetectedArrayLimit() {
    return this.detectedArrayLimit;
  }

  /**
   * Set value for array limit detected flag. It is important to set the flag for correct processing of parsing.
   *
   * @param value true or false.
   * @see com.igormaznitsa.jbbp.JBBPParser
   * @since 2.1.0
   */
  public void setDetectedArrayLimit(final boolean value) {
    this.detectedArrayLimit = value;
  }

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
   * @param in    an input stream to be filtered.
   * @param order a bit order mode for the filter.
   * @see JBBPBitOrder#LSB0
   * @see JBBPBitOrder#MSB0
   */
  public JBBPBitInputStream(final InputStream in, final JBBPBitOrder order) {
    super(in);
    this.bitsInBuffer = 0;
    this.bitOrderMode = order;
  }

  /**
   * Read array of boolean values.
   *
   * @param items number of items to be read, if less than zero then read whole
   *              stream till the end
   * @return read values as boolean array
   * @throws IOException it will be thrown for transport error
   */
  public boolean[] readBoolArray(final int items) throws IOException {
    return this.readBoolArray(items, JBBPArraySizeLimiter.NO_LIMIT_FOR_ARRAY_SIZE);
  }

  /**
   * Read array of boolean values.
   *
   * @param items            number of items to be read, if less than zero then read whole
   *                         stream till the end
   * @param arraySizeLimiter limiter provides number of allowed array items, must not be null
   * @return read values as boolean array
   * @throws IOException                        it will be thrown for transport error
   * @throws JBBPReachedArraySizeLimitException if reached limit of read
   * @since 2.1.0
   */
  public boolean[] readBoolArray(final int items,
                                 final JBBPArraySizeLimiter arraySizeLimiter)
      throws IOException {
    this.setDetectedArrayLimit(false);
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

        if (isBreakReadWholeStream(pos, arraySizeLimiter)) {
          this.setDetectedArrayLimit(true);
          final int limit = arraySizeLimiter.getArrayItemsLimit();
          if (limit < 0) {
            pos = Math.min(pos, Math.abs(limit));
          }
          break;
        }

        if (buffer.length == pos) {
          final byte[] newBuffer = new byte[buffer.length << 1];
          System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
          buffer = newBuffer;
        }
      }
    } else {
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

  @Override
  public int read(final byte[] array, final int offset, final int length) throws IOException {
    if (this.bitsInBuffer == 0) {
      int readBytes = 0;
      int tempOffset = offset;
      int tempLength = length;
      while (tempLength > 0) {
        int read = this.in.read(array, tempOffset, tempLength);
        if (read < 0) {
          readBytes = readBytes == 0 ? read : readBytes;
          break;
        }
        tempLength -= read;
        tempOffset += read;
        readBytes += read;
        this.byteCounter += read;
      }

      if (this.bitOrderMode == JBBPBitOrder.MSB0) {
        int index = offset;
        int number = readBytes;
        while (number > 0) {
          array[index] = JBBPUtils.reverseBitsInByte(array[index]);
          index++;
          number--;
        }
      }

      return readBytes;
    } else {
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
   * Read array of bit sequence.
   *
   * @param bitNumber bit number for each bit sequence item, must be 1..8
   * @param items     number of items to be read, if less than zero then read whole
   *                  stream till the end
   * @return array of read bit items as a byte array
   * @throws IOException it will be thrown for any transport problem during the
   *                     operation
   */
  public byte[] readBitsArray(final int items, final JBBPBitNumber bitNumber) throws IOException {
    return this.readBitsArray(items, bitNumber, JBBPArraySizeLimiter.NO_LIMIT_FOR_ARRAY_SIZE);
  }

  /**
   * Read array of bit sequence.
   *
   * @param items            number of items to be read, if less than zero then read whole
   * @param bitNumber        bit number for each bit sequence item, must be 1..8
   *                         stream till the end
   * @param arraySizeLimiter limiter provides number of allowed array items for non-limited array, must not be null
   * @return array of read bit items as a byte array
   * @throws JBBPReachedArraySizeLimitException if reached limit of read
   * @throws IOException                        it will be thrown for any transport problem during the
   *                                            operation
   * @since 2.1.0
   */
  public byte[] readBitsArray(final int items, final JBBPBitNumber bitNumber,
                              final JBBPArraySizeLimiter arraySizeLimiter)
      throws IOException {
    return internalReadArray(items, bitNumber, arraySizeLimiter);
  }

  /**
   * Read number of bytes for the stream.
   *
   * @param items number of items to be read, if less than zero then read whole
   *              stream till the end
   * @return read byte items as a byte array
   * @throws IOException it will be thrown for any transport problem during the
   *                     operation
   */
  public byte[] readByteArray(final int items) throws IOException {
    return this.readBitsArray(items, null, JBBPArraySizeLimiter.NO_LIMIT_FOR_ARRAY_SIZE);
  }

  /**
   * Read number of bytes for the stream.
   *
   * @param items            number of items to be read, if less than zero then read whole
   *                         stream till the end
   * @param arraySizeLimiter limiter provides number of allowed array items for non-limited array, must not be null
   * @return read byte items as a byte array
   * @throws IOException it will be thrown for any transport problem during the
   *                     operation
   * @since 2.1.0
   */
  public byte[] readByteArray(final int items, final JBBPArraySizeLimiter arraySizeLimiter)
      throws IOException {
    return internalReadArray(items, null, arraySizeLimiter);
  }

  /**
   * Read number of bytes for the stream. Invert their order if byte order is LITTLE_ENDIAN
   *
   * @param items     number of items to be read, if less than zero then read whole
   *                  stream till the end
   * @param byteOrder desired order of bytes
   * @return read byte items as a byte array, if byte order is LITTLE_ENDIAN then the result array will be reversed one
   * @throws IOException it will be thrown for any transport problem during the
   *                     operation
   * @see JBBPByteOrder#LITTLE_ENDIAN
   * @since 1.3.0
   */
  public byte[] readByteArray(final int items, final JBBPByteOrder byteOrder) throws IOException {
    return this.readByteArray(items, byteOrder, JBBPArraySizeLimiter.NO_LIMIT_FOR_ARRAY_SIZE);
  }

  /**
   * Read number of bytes for the stream. Invert their order if byte order is LITTLE_ENDIAN
   *
   * @param items            number of items to be read, if less than zero then read whole
   *                         stream till the end
   * @param byteOrder        desired order of bytes
   * @param arraySizeLimiter limiter provides number of allowed array items for non-limited array, must not be null
   * @return read byte items as a byte array, if byte order is LITTLE_ENDIAN then the result array will be reversed one
   * @throws IOException                        it will be thrown for any transport problem during the
   *                                            operation
   * @throws JBBPReachedArraySizeLimitException if reached limit of array read
   * @see JBBPByteOrder#LITTLE_ENDIAN
   * @since 2.1.0
   */
  public byte[] readByteArray(
      final int items,
      final JBBPByteOrder byteOrder,
      final JBBPArraySizeLimiter arraySizeLimiter
  ) throws IOException {
    final byte[] result = internalReadArray(items, null, arraySizeLimiter);
    if (byteOrder == JBBPByteOrder.LITTLE_ENDIAN) {
      JBBPUtils.reverseArray(result);
    }
    return result;
  }

  @Override
  public void mark(final int readLimit) {
    in.mark(readLimit);
    this.markedBitBuffer = this.bitBuffer;
    this.markedByteCounter = this.byteCounter;
    this.markedBitsInBuffer = this.bitsInBuffer;
  }

  private byte[] internalReadArray(
      final int items,
      final JBBPBitNumber bitNumber,
      final JBBPArraySizeLimiter streamLimiter
  ) throws IOException {
    final boolean readByteArray = bitNumber == null;
    this.setDetectedArrayLimit(false);
    int pos = 0;
    if (items < 0) {
      byte[] buffer = new byte[INITIAL_ARRAY_BUFFER_SIZE];
      // till end
      while (true) {
        final int next = readByteArray ? read() : readBits(bitNumber);
        if (next < 0) {
          break;
        }
        if (buffer.length == pos) {
          final byte[] newBuffer = new byte[buffer.length << 1];
          System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
          buffer = newBuffer;
        }
        buffer[pos++] = (byte) next;
        if (isBreakReadWholeStream(pos, streamLimiter)) {
          this.setDetectedArrayLimit(true);
          break;
        }
      }
      if (buffer.length == pos) {
        return buffer;
      }
      final byte[] result = new byte[pos];
      System.arraycopy(buffer, 0, result, 0, pos);
      return result;
    } else {
      // number
      final byte[] buffer = new byte[items];
      if (readByteArray) {
        final int read = this.read(buffer, 0, items);
        if (read != items) {
          throw new EOFException(
              "Have read only " + read + " byte(s) instead of " + items + " byte(s)");
        }
      } else {
        for (int i = 0; i < items; i++) {
          final int next = readBits(bitNumber);
          if (next < 0) {
            throw new EOFException("Have read only " + i + " bit portions instead of " + items);
          }
          buffer[i] = (byte) next;
        }
      }
      return buffer;
    }
  }

  /**
   * Read number of short items from the input stream.
   *
   * @param items     number of items to be read from the input stream, if less than
   *                  zero then all stream till the end will be read
   * @param byteOrder the order of bytes to be used to decode short values
   * @return read items as a short array
   * @throws IOException it will be thrown for any transport problem during the
   *                     operation
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   */
  public short[] readShortArray(final int items, final JBBPByteOrder byteOrder) throws IOException {
    return this.readShortArray(items, byteOrder, JBBPArraySizeLimiter.NO_LIMIT_FOR_ARRAY_SIZE);
  }

  /**
   * Read number of short items from the input stream.
   *
   * @param items            number of items to be read from the input stream, if less than
   *                         zero then all stream till the end will be read
   * @param byteOrder        the order of bytes to be used to decode short values
   * @param arraySizeLimiter limiter provides number of allowed array items for non-limited array, must not be null
   * @return read items as a short array
   * @throws IOException                        it will be thrown for any transport problem during the
   *                                            operation
   * @throws JBBPReachedArraySizeLimitException if reached limit of array read
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   * @since 2.1.0
   */
  public short[] readShortArray(final int items, final JBBPByteOrder byteOrder,
                                final JBBPArraySizeLimiter arraySizeLimiter)
      throws IOException {
    this.setDetectedArrayLimit(false);
    int pos = 0;
    if (items < 0) {
      short[] buffer = new short[INITIAL_ARRAY_BUFFER_SIZE];
      // till end
      while (hasAvailableData()) {
        final int next = readUnsignedShort(byteOrder);
        if (buffer.length == pos) {
          final short[] newBuffer = new short[buffer.length << 1];
          System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
          buffer = newBuffer;
        }
        buffer[pos++] = (short) next;
        if (isBreakReadWholeStream(pos, arraySizeLimiter)) {
          this.setDetectedArrayLimit(true);
          break;
        }
      }
      if (buffer.length == pos) {
        return buffer;
      }
      final short[] result = new short[pos];
      System.arraycopy(buffer, 0, result, 0, pos);
      return result;
    } else {
      // number
      final short[] buffer = new short[items];
      for (int i = 0; i < items; i++) {
        buffer[i] = (short) readUnsignedShort(byteOrder);
      }
      return buffer;
    }
  }

  /**
   * Read number of unsigned integer items from the input stream.
   *
   * @param items     number of items to be read from the input stream, if less than
   *                  zero then all stream till the end will be read
   * @param byteOrder the order of bytes to be used to decode values
   * @return read items as an unsigned integer array represented through long
   * @throws IOException it will be thrown for any transport problem during the
   *                     operation
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   * @since 2.0.4
   */
  public long[] readUIntArray(final int items, final JBBPByteOrder byteOrder) throws IOException {
    return this.readUIntArray(items, byteOrder, JBBPArraySizeLimiter.NO_LIMIT_FOR_ARRAY_SIZE);
  }

  /**
   * Read number of unsigned integer items from the input stream.
   *
   * @param items            number of items to be read from the input stream, if less than
   *                         zero then all stream till the end will be read
   * @param byteOrder        the order of bytes to be used to decode values
   * @param arraySizeLimiter limiter provides number of allowed array items for non-limited array, must not be null
   * @return read items as an unsigned integer array represented through long
   * @throws IOException                        it will be thrown for any transport problem during the
   *                                            operation
   * @throws JBBPReachedArraySizeLimitException if reached limit of array read
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   * @since 2.1.0
   */
  public long[] readUIntArray(
      final int items,
      final JBBPByteOrder byteOrder,
      final JBBPArraySizeLimiter arraySizeLimiter
  ) throws IOException {
    this.setDetectedArrayLimit(false);
    int pos = 0;
    if (items < 0) {
      long[] buffer = new long[INITIAL_ARRAY_BUFFER_SIZE];
      // till end
      while (hasAvailableData()) {
        final long next = readUInt(byteOrder);
        if (buffer.length == pos) {
          final long[] newBuffer = new long[buffer.length << 1];
          System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
          buffer = newBuffer;
        }
        buffer[pos++] = next;
        if (isBreakReadWholeStream(pos, arraySizeLimiter)) {
          this.setDetectedArrayLimit(true);
          break;
        }
      }
      if (buffer.length == pos) {
        return buffer;
      }
      final long[] result = new long[pos];
      System.arraycopy(buffer, 0, result, 0, pos);
      return result;
    } else {
      // number
      final long[] buffer = new long[items];
      for (int i = 0; i < items; i++) {
        buffer[i] = readUInt(byteOrder);
      }
      return buffer;
    }
  }

  /**
   * Read number of unsigned short items from the input stream.
   *
   * @param items     number of items to be read from the input stream, if less than
   *                  zero then all stream till the end will be read
   * @param byteOrder the order of bytes to be used to decode short values
   * @return read items as a char array
   * @throws IOException it will be thrown for any transport problem during the
   *                     operation
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   * @since 1.3
   */
  public char[] readUShortArray(final int items, final JBBPByteOrder byteOrder) throws IOException {
    return this.readUShortArray(items, byteOrder, JBBPArraySizeLimiter.NO_LIMIT_FOR_ARRAY_SIZE);
  }

  /**
   * Read number of unsigned short items from the input stream.
   *
   * @param items            number of items to be read from the input stream, if less than
   *                         zero then all stream till the end will be read
   * @param byteOrder        the order of bytes to be used to decode short values
   * @param arraySizeLimiter limiter provides number of allowed array items for non-limited array, must not be null
   * @return read items as a char array
   * @throws IOException                        it will be thrown for any transport problem during the
   *                                            operation
   * @throws JBBPReachedArraySizeLimitException if reached limit of array read
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   * @since 2.1.0
   */
  public char[] readUShortArray(final int items, final JBBPByteOrder byteOrder,
                                final JBBPArraySizeLimiter arraySizeLimiter)
      throws IOException {
    this.setDetectedArrayLimit(false);
    int pos = 0;
    if (items < 0) {
      char[] buffer = new char[INITIAL_ARRAY_BUFFER_SIZE];
      // till end
      while (hasAvailableData()) {
        final int next = readUnsignedShort(byteOrder);
        if (buffer.length == pos) {
          final char[] newBuffer = new char[buffer.length << 1];
          System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
          buffer = newBuffer;
        }
        buffer[pos++] = (char) next;
        if (isBreakReadWholeStream(pos, arraySizeLimiter)) {
          this.setDetectedArrayLimit(true);
          break;
        }
      }
      if (buffer.length == pos) {
        return buffer;
      }
      final char[] result = new char[pos];
      System.arraycopy(buffer, 0, result, 0, pos);
      return result;
    } else {
      // number
      final char[] buffer = new char[items];
      for (int i = 0; i < items; i++) {
        buffer[i] = (char) readUnsignedShort(byteOrder);
      }
      return buffer;
    }
  }

  /**
   * Read number of integer items from the input stream.
   *
   * @param items     number of items to be read from the input stream, if less than
   *                  zero then all stream till the end will be read
   * @param byteOrder the order of bytes to be used to decode values
   * @return read items as an integer array
   * @throws IOException it will be thrown for any transport problem during the
   *                     operation
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   */
  public int[] readIntArray(final int items, final JBBPByteOrder byteOrder) throws IOException {
    return this.readIntArray(items, byteOrder, JBBPArraySizeLimiter.NO_LIMIT_FOR_ARRAY_SIZE);
  }

  /**
   * Read number of integer items from the input stream.
   *
   * @param items            number of items to be read from the input stream, if less than
   *                         zero then all stream till the end will be read
   * @param byteOrder        the order of bytes to be used to decode values
   * @param arraySizeLimiter limiter provides number of allowed array items for non-limited array, must not be null
   * @return read items as an integer array
   * @throws IOException                        it will be thrown for any transport problem during the
   *                                            operation
   * @throws JBBPReachedArraySizeLimitException if reached limit of array read
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   */
  public int[] readIntArray(final int items, final JBBPByteOrder byteOrder,
                            final JBBPArraySizeLimiter arraySizeLimiter) throws IOException {
    this.setDetectedArrayLimit(false);
    int pos = 0;
    if (items < 0) {
      int[] buffer = new int[INITIAL_ARRAY_BUFFER_SIZE];
      // till end
      while (hasAvailableData()) {
        final int next = readInt(byteOrder);
        if (buffer.length == pos) {
          final int[] newBuffer = new int[buffer.length << 1];
          System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
          buffer = newBuffer;
        }
        buffer[pos++] = next;
        if (isBreakReadWholeStream(pos, arraySizeLimiter)) {
          this.setDetectedArrayLimit(true);
          break;
        }
      }
      if (buffer.length == pos) {
        return buffer;
      }
      final int[] result = new int[pos];
      System.arraycopy(buffer, 0, result, 0, pos);
      return result;
    } else {
      // number
      final int[] buffer = new int[items];
      for (int i = 0; i < items; i++) {
        buffer[i] = readInt(byteOrder);
      }
      return buffer;
    }
  }

  /**
   * Read number of float items from the input stream.
   *
   * @param items     number of items to be read from the input stream, if less than
   *                  zero then all stream till the end will be read
   * @param byteOrder the order of bytes to be used to decode values
   * @return read items as float array
   * @throws IOException it will be thrown for any transport problem during the
   *                     operation
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   * @since 1.4.0
   */
  public float[] readFloatArray(final int items, final JBBPByteOrder byteOrder) throws IOException {
    return this.readFloatArray(items, byteOrder, JBBPArraySizeLimiter.NO_LIMIT_FOR_ARRAY_SIZE);
  }

  /**
   * Read number of float items from the input stream.
   *
   * @param items            number of items to be read from the input stream, if less than
   *                         zero then all stream till the end will be read
   * @param byteOrder        the order of bytes to be used to decode values
   * @param arraySizeLimiter limiter provides number of allowed array items for non-limited array, must not be null
   * @return read items as float array
   * @throws IOException                        it will be thrown for any transport problem during the
   *                                            operation
   * @throws JBBPReachedArraySizeLimitException if reached limit of array read
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   * @since 2.1.0
   */
  public float[] readFloatArray(
      final int items,
      final JBBPByteOrder byteOrder,
      final JBBPArraySizeLimiter arraySizeLimiter
  ) throws IOException {
    this.setDetectedArrayLimit(false);
    int pos = 0;
    if (items < 0) {
      float[] buffer = new float[INITIAL_ARRAY_BUFFER_SIZE];
      // till end
      while (hasAvailableData()) {
        final float next = readFloat(byteOrder);
        if (buffer.length == pos) {
          final float[] newBuffer = new float[buffer.length << 1];
          System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
          buffer = newBuffer;
        }
        buffer[pos++] = next;
        if (isBreakReadWholeStream(pos, arraySizeLimiter)) {
          this.setDetectedArrayLimit(true);
          break;
        }
      }
      if (buffer.length == pos) {
        return buffer;
      }
      final float[] result = new float[pos];
      System.arraycopy(buffer, 0, result, 0, pos);
      return result;
    } else {
      // number
      final float[] buffer = new float[items];
      for (int i = 0; i < items; i++) {
        buffer[i] = readFloat(byteOrder);
      }
      return buffer;
    }
  }

  /**
   * Read a unsigned short value from the stream.
   *
   * @param byteOrder the order of bytes to be used to decode the read value
   * @return the unsigned short value read from stream
   * @throws IOException  it will be thrown for any transport problem during the
   *                      operation
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
   * @param byteOrder the order of bytes to be used to decode the read value
   * @return the integer value from the stream
   * @throws IOException  it will be thrown for any transport problem during the
   *                      operation
   * @throws EOFException if the end of the stream has been reached
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   */
  public int readInt(final JBBPByteOrder byteOrder) throws IOException {
    if (byteOrder == JBBPByteOrder.BIG_ENDIAN) {
      return (readUnsignedShort(byteOrder) << 16) | readUnsignedShort(byteOrder);
    } else {
      return readUnsignedShort(byteOrder) | (readUnsignedShort(byteOrder) << 16);
    }
  }

  /**
   * Read an unsigned integer value from the stream.
   *
   * @param byteOrder the order of bytes to be used to decode the read value
   * @return the unsigned integer value from the stream
   * @throws IOException  it will be thrown for any transport problem during the
   *                      operation
   * @throws EOFException if the end of the stream has been reached
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   * @since 2.0.4
   */
  public long readUInt(final JBBPByteOrder byteOrder) throws IOException {
    if (byteOrder == JBBPByteOrder.BIG_ENDIAN) {
      return (((long) readUnsignedShort(byteOrder) << 16) | readUnsignedShort(byteOrder));
    } else {
      return readUnsignedShort(byteOrder) | ((long) readUnsignedShort(byteOrder) << 16);
    }
  }

  /**
   * Read a float value from the stream.
   *
   * @param byteOrder the order of bytes to be used to decode the read value
   * @return the float value from the stream
   * @throws IOException  it will be thrown for any transport problem during the
   *                      operation
   * @throws EOFException if the end of the stream has been reached
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   * @since 1.4.0
   */
  public float readFloat(final JBBPByteOrder byteOrder) throws IOException {
    final int value;
    if (byteOrder == JBBPByteOrder.BIG_ENDIAN) {
      value = (readUnsignedShort(byteOrder) << 16) | readUnsignedShort(byteOrder);
    } else {
      value = readUnsignedShort(byteOrder) | (readUnsignedShort(byteOrder) << 16);
    }
    return Float.intBitsToFloat(value);
  }

  /**
   * Read a long value from the stream.
   *
   * @param byteOrder the order of bytes to be used to decode the read value
   * @return the long value from stream
   * @throws IOException  it will be thrown for any transport problem during the
   *                      operation
   * @throws EOFException if the end of the stream has been reached
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   */
  public long readLong(final JBBPByteOrder byteOrder) throws IOException {
    if (byteOrder == JBBPByteOrder.BIG_ENDIAN) {
      return (((long) readInt(byteOrder) & 0xFFFFFFFFL) << 32) |
          ((long) readInt(byteOrder) & 0xFFFFFFFFL);
    } else {
      return ((long) readInt(byteOrder) & 0xFFFFFFFFL) |
          (((long) readInt(byteOrder) & 0xFFFFFFFFL) << 32);
    }
  }

  /**
   * Read a double value from the stream.
   *
   * @param byteOrder the order of bytes to be used to decode the read value
   * @return the double value from stream
   * @throws IOException  it will be thrown for any transport problem during the
   *                      operation
   * @throws EOFException if the end of the stream has been reached
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   * @since 1.4.0
   */
  public double readDouble(final JBBPByteOrder byteOrder) throws IOException {
    final long value;
    if (byteOrder == JBBPByteOrder.BIG_ENDIAN) {
      value = (((long) readInt(byteOrder) & 0xFFFFFFFFL) << 32) |
          ((long) readInt(byteOrder) & 0xFFFFFFFFL);
    } else {
      value = ((long) readInt(byteOrder) & 0xFFFFFFFFL) |
          (((long) readInt(byteOrder) & 0xFFFFFFFFL) << 32);
    }
    return Double.longBitsToDouble(value);
  }

  /**
   * Get the current fully read byte counter.
   *
   * @return the number of bytes read fully from the stream
   */
  @Override
  public long getCounter() {
    return this.byteCounter;
  }

  /**
   * Get the value of the inside bit buffer
   *
   * @return the bit buffer state
   */
  @Override
  public int getBitBuffer() {
    return this.bitBuffer;
  }

  /**
   * Get the number of buffered bits in the inside bit buffer
   *
   * @return the number of buffered bits in the bit buffer
   */
  @Override
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
  @Override
  public JBBPBitOrder getBitOrder() {
    return this.bitOrderMode;
  }

  /**
   * Read bit field from the stream, if it is impossible then IOException will be thrown.
   *
   * @param numOfBitsToRead field width, must not be null
   * @return read value from the stream
   * @throws IOException it will be thrown if EOF or troubles to read the stream
   * @since 1.3.0
   */
  public byte readBitField(final JBBPBitNumber numOfBitsToRead) throws IOException {
    final int value = this.readBits(numOfBitsToRead);
    if (value < 0) {
      throw new EOFException("Can't read bits from stream [" + numOfBitsToRead + ']');
    }
    return (byte) value;
  }

  /**
   * Read number of bits from the input stream. It reads bits from input stream
   * since 0 bit and make reversion to return bits in the right order when 0 bit
   * is 0 bit. if the stream is completed earlier than the data read then reading
   * is just stopped and read value returned. The First read bit is placed as
   * 0th bit.
   *
   * @param numOfBitsToRead the number of bits to be read, must be 1..8
   * @return the read bits as integer, -1 if the end of stream has been reached
   * @throws IOException          it will be thrown for transport errors to be read
   * @throws NullPointerException if number of bits to be read is null
   */
  public int readBits(final JBBPBitNumber numOfBitsToRead)
      throws IOException {
    int result;

    final int numOfBitsAsNumber = numOfBitsToRead.getBitNumber();

    if (this.bitsInBuffer == 0 && numOfBitsAsNumber == 8) {
      result = this.readByteFromStream();
      if (result >= 0) {
        this.byteCounter++;
      }
    } else {
      result = 0;

      if (numOfBitsAsNumber == this.bitsInBuffer) {
        result = this.bitBuffer;
        if (this.bitOrderMode == JBBPBitOrder.MSB0_DIRECT) {
          result >>>= this.bitsInBuffer;
        }
        this.bitBuffer = 0;
        this.bitsInBuffer = 0;
        this.byteCounter++;
        return result;
      }

      int i = numOfBitsAsNumber;
      int theBitBuffer = this.bitBuffer;
      int theBitBufferCounter = this.bitsInBuffer;

      final boolean doIncCounter = theBitBufferCounter != 0;

      if (this.bitOrderMode == JBBPBitOrder.MSB0_DIRECT) {
        while (i > 0) {
          if (theBitBufferCounter == 0) {
            if (doIncCounter) {
              this.byteCounter++;
            }
            final int nextByte = this.readByteFromStream();
            if (nextByte < 0) {
              if (i == numOfBitsAsNumber) {
                return nextByte;
              } else {
                break;
              }
            } else {
              theBitBuffer = nextByte;
              theBitBufferCounter = 8;
            }
          }

          result = (result << 1) | ((theBitBuffer >>> 7) & 1);
          theBitBuffer = (theBitBuffer << 1) & 0xFF;
          theBitBufferCounter--;
          i--;
        }
      } else {
        while (i > 0) {
          if (theBitBufferCounter == 0) {
            if (doIncCounter) {
              this.byteCounter++;
            }
            final int nextByte = this.readByteFromStream();
            if (nextByte < 0) {
              if (i == numOfBitsAsNumber) {
                return nextByte;
              } else {
                break;
              }
            } else {
              theBitBuffer = nextByte;
              theBitBufferCounter = 8;
            }
          }

          result = (result << 1) | (theBitBuffer & 1);
          theBitBuffer >>= 1;
          theBitBufferCounter--;
          i--;
        }
        result = JBBPUtils.reverseBitsInByte(JBBPBitNumber.decode(numOfBitsAsNumber - i),
            (byte) result) &
            0xFF;
      }

      this.bitBuffer = theBitBuffer;
      this.bitsInBuffer = theBitBufferCounter;
    }
    return result;
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
  public void reset() throws IOException {
    in.reset();
    this.bitBuffer = this.markedBitBuffer;
    this.byteCounter = this.markedByteCounter;
    this.bitsInBuffer = this.markedBitsInBuffer;
  }

  /**
   * Read number of long items from the input stream.
   *
   * @param items     number of items to be read from the input stream, if less than
   *                  zero then all stream till the end will be read
   * @param byteOrder the order of bytes to be used to decode values
   * @return read items as a long array
   * @throws IOException it will be thrown for any transport problem during the
   *                     operation
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   */
  public long[] readLongArray(final int items, final JBBPByteOrder byteOrder) throws IOException {
    return this.readLongArray(items, byteOrder, JBBPArraySizeLimiter.NO_LIMIT_FOR_ARRAY_SIZE);
  }

  /**
   * Read number of long items from the input stream.
   *
   * @param items            number of items to be read from the input stream, if less than
   *                         zero then all stream till the end will be read
   * @param byteOrder        the order of bytes to be used to decode values
   * @param arraySizeLimiter limiter provides number of allowed array items for non-limited array, must not be null
   * @return read items as a long array
   * @throws IOException                        it will be thrown for any transport problem during the
   *                                            operation
   * @throws JBBPReachedArraySizeLimitException if reached limit of read
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   * @since 2.1.0
   */
  public long[] readLongArray(
      final int items,
      final JBBPByteOrder byteOrder,
      final JBBPArraySizeLimiter arraySizeLimiter
  ) throws IOException {
    this.setDetectedArrayLimit(false);
    int pos = 0;
    if (items < 0) {
      long[] buffer = new long[INITIAL_ARRAY_BUFFER_SIZE];
      // till end
      while (hasAvailableData()) {
        final long next = readLong(byteOrder);
        if (buffer.length == pos) {
          final long[] newBuffer = new long[buffer.length << 1];
          System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
          buffer = newBuffer;
        }
        buffer[pos++] = next;
        if (isBreakReadWholeStream(pos, arraySizeLimiter)) {
          this.setDetectedArrayLimit(true);
          break;
        }
      }
      if (buffer.length == pos) {
        return buffer;
      }
      final long[] result = new long[pos];
      System.arraycopy(buffer, 0, result, 0, pos);
      return result;
    } else {
      // number
      final long[] buffer = new long[items];
      for (int i = 0; i < items; i++) {
        buffer[i] = readLong(byteOrder);
      }
      return buffer;
    }
  }

  /**
   * Read padding bytes from the stream and ignore them to align the stream
   * counter.
   *
   * @param alignByteNumber the byte number to align the stream
   * @throws IOException  it will be thrown for transport errors
   * @throws EOFException it will be thrown if the stream end has been reached
   *                      the before align border.
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
    } else {
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
    if (result >= 0 && this.bitOrderMode == JBBPBitOrder.MSB0) {
      result = JBBPUtils.reverseBitsInByte((byte) result) & 0xFF;
    }
    return result;
  }

  /**
   * Read number of double items from the input stream.
   *
   * @param items     number of items to be read from the input stream, if less than
   *                  zero then all stream till the end will be read
   * @param byteOrder the order of bytes to be used to decode values
   * @return read items as a double array
   * @throws IOException it will be thrown for any transport problem during the
   *                     operation
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   * @since 1.4.0
   */
  public double[] readDoubleArray(final int items, final JBBPByteOrder byteOrder)
      throws IOException {
    return this.readDoubleArray(items, byteOrder, JBBPArraySizeLimiter.NO_LIMIT_FOR_ARRAY_SIZE);
  }

  /**
   * Read number of double items from the input stream.
   *
   * @param items            number of items to be read from the input stream, if less than
   *                         zero then all stream till the end will be read
   * @param byteOrder        the order of bytes to be used to decode values
   * @param arraySizeLimiter limiter provides number of allowed array items for non-limited array, must not be null
   * @return read items as a double array
   * @throws IOException                        it will be thrown for any transport problem during the
   *                                            operation
   * @throws JBBPReachedArraySizeLimitException if reached limit of array read
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   * @since 2.1.0
   */
  public double[] readDoubleArray(
      final int items,
      final JBBPByteOrder byteOrder,
      final JBBPArraySizeLimiter arraySizeLimiter
  ) throws IOException {
    this.setDetectedArrayLimit(false);
    int pos = 0;
    if (items < 0) {
      double[] buffer = new double[INITIAL_ARRAY_BUFFER_SIZE];
      // till end
      while (hasAvailableData()) {
        final long next = readLong(byteOrder);
        if (buffer.length == pos) {
          final double[] newBuffer = new double[buffer.length << 1];
          System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
          buffer = newBuffer;
        }
        buffer[pos++] = Double.longBitsToDouble(next);
        if (isBreakReadWholeStream(pos, arraySizeLimiter)) {
          this.setDetectedArrayLimit(true);
          break;
        }
      }
      if (buffer.length == pos) {
        return buffer;
      }
      final double[] result = new double[pos];
      System.arraycopy(buffer, 0, result, 0, pos);
      return result;
    } else {
      // number
      final double[] buffer = new double[items];
      for (int i = 0; i < items; i++) {
        buffer[i] = readDouble(byteOrder);
      }
      return buffer;
    }
  }

  /**
   * Align read byte. If there are unread bits in inside bit buffer, it will be
   * aligned to read the next byte from stream instead of reading from the bit
   * buffer.
   */
  public void alignByte() {
    if (this.bitsInBuffer > 0 && this.bitsInBuffer < 8) {
      this.byteCounter++;
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
    return this.bitsInBuffer > 0 || loadNextByteInBuffer() >= 0;
  }

  /**
   * Read the next stream byte into bit buffer.
   *
   * @return the read byte or -1 if the end of stream has been reached.
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
   * Reset the byte counter for the stream. The Inside bit buffer will be reset
   * also if it is not full.
   *
   * @see #align(long)
   * @see #alignByte()
   * @see #getBitBuffer()
   * @see #getBufferedBitsNumber()
   * @see #hasAvailableData() .
   */
  @Override
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
    } else {
      return this.readBits(JBBPBitNumber.BITS_8);
    }
  }

  private IOException makeIOExceptionForWrongPrefix(final int prefix) {
    return new IOException("Wrong string prefix:" + prefix);
  }

  /**
   * Read string in UTF8 format.
   *
   * @param byteOrder byte order, must not be null
   * @return read string, can be null
   * @throws IOException it will be thrown for transport error or wrong format
   * @see JBBPBitOutputStream#writeString(String, JBBPByteOrder)
   * @since 1.4.0
   */
  public String readString(final JBBPByteOrder byteOrder) throws IOException {
    final int prefix = this.readByte();
    final int len;
    if (prefix == 0) {
      len = 0;
    } else if (prefix == 0xFF) {
      len = -1;
    } else if (prefix < 0x80) {
      len = prefix;
    } else if ((prefix & 0xF0) == 0x80) {
      switch (prefix & 0x0F) {
        case 1: {
          len = this.readByte();
        }
        break;
        case 2: {
          len = this.readUnsignedShort(byteOrder);
        }
        break;
        case 3: {
          int buffer;
          if (byteOrder == JBBPByteOrder.BIG_ENDIAN) {
            buffer = (this.readByte() << 16) | (this.readByte() << 8) | this.readByte();
          } else {
            buffer = this.readByte() | (this.readByte() << 8) | (this.readByte() << 16);
          }
          len = buffer;
        }
        break;
        case 4: {
          len = this.readInt(byteOrder);
        }
        break;
        default: {
          throw makeIOExceptionForWrongPrefix(prefix);
        }
      }
    } else {
      throw makeIOExceptionForWrongPrefix(prefix);
    }

    final String result;
    if (len < 0) {
      result = null;
    } else if (len == 0) {
      result = "";
    } else {
      result = JBBPUtils.utf8ToStr(this.readByteArray(len));
    }

    return result;
  }

  /**
   * Read array of strings from stream.
   *
   * @param items     number of items, or -1 if read whole stream
   * @param byteOrder order of bytes in structure, must not be null
   * @return array, it can contain null among values, must not be null
   * @throws IOException thrown for transport errors
   * @see JBBPBitOutputStream#writeStringArray(String[], JBBPByteOrder)
   * @since 1.4.0
   */
  public String[] readStringArray(final int items, final JBBPByteOrder byteOrder)
      throws IOException {
    return this.readStringArray(items, byteOrder, JBBPArraySizeLimiter.NO_LIMIT_FOR_ARRAY_SIZE);
  }

  /**
   * Read array of strings from stream.
   *
   * @param items            number of items, or -1 if read whole stream
   * @param byteOrder        order of bytes in structure, must not be null
   * @param arraySizeLimiter limiter provides number of allowed array items for non-limited array, must not be null
   * @return array, it can contain null among values, must not be null
   * @throws IOException                        thrown for transport errors
   * @throws JBBPReachedArraySizeLimitException if reached limit of array read
   * @see JBBPBitOutputStream#writeStringArray(String[], JBBPByteOrder)
   * @since 2.1.0
   */
  public String[] readStringArray(
      final int items,
      final JBBPByteOrder byteOrder,
      final JBBPArraySizeLimiter arraySizeLimiter
  ) throws IOException {
    this.setDetectedArrayLimit(false);
    int pos = 0;
    if (items < 0) {
      String[] buffer = new String[INITIAL_ARRAY_BUFFER_SIZE];
      // till end
      while (hasAvailableData()) {
        final String next = readString(byteOrder);
        if (buffer.length == pos) {
          final String[] newBuffer = new String[buffer.length << 1];
          System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
          buffer = newBuffer;
        }
        buffer[pos++] = next;
        if (isBreakReadWholeStream(pos, arraySizeLimiter)) {
          this.setDetectedArrayLimit(true);
          break;
        }
      }
      if (buffer.length == pos) {
        return buffer;
      }
      final String[] result = new String[pos];
      System.arraycopy(buffer, 0, result, 0, pos);
      return result;
    } else {
      // number
      final String[] buffer = new String[items];
      for (int i = 0; i < items; i++) {
        buffer[i] = readString(byteOrder);
      }
      return buffer;
    }

  }

}
