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

public class JBBPBitInputStream extends FilterInputStream implements JBBPBitStream {

  private static final int INITIAL_ARRAY_BUFFER_SIZE = 32;

  private int bitBuffer;
  private int bitsInBuffer;
  private long byteCounter;

  private int markedBitBuffer;
  private int markedBitsInBuffer;
  private long markedByteCounter;

  private final boolean msb0;

  public JBBPBitInputStream(final InputStream in) {
    this(in, JBBPBitOrder.LSB0);
  }

  public JBBPBitInputStream(final InputStream in, final JBBPBitOrder order) {
    super(in);
    this.bitsInBuffer = 0;
    this.msb0 = order == JBBPBitOrder.MSB0;
  }

  public byte[] readBitsArray(final int bitNumber, final int items) throws IOException {
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

  public int readInt(final JBBPByteOrder byteOrder) throws IOException {
    if (byteOrder == JBBPByteOrder.BIG_ENDIAN) {
      return (readUnsignedShort(byteOrder) << 16) | readUnsignedShort(byteOrder);
    }
    else {
      return readUnsignedShort(byteOrder) | (readUnsignedShort(byteOrder) << 16);
    }
  }

  public long readLong(final JBBPByteOrder byteOrder) throws IOException {
    if (byteOrder == JBBPByteOrder.BIG_ENDIAN) {
      return (((long) readInt(byteOrder) & 0xFFFFFFFFL) << 32) | ((long) readInt(byteOrder) & 0xFFFFFFFFL);
    }
    else {
      return ((long) readInt(byteOrder) & 0xFFFFFFFFL) | (((long) readInt(byteOrder) & 0xFFFFFFFFL) << 32);
    }
  }

  public long getCounter() {
    return this.byteCounter;
  }

  public int getBitBuffer() {
    return this.bitBuffer;
  }

  public int getBufferedBitsNumber() {
    return this.bitsInBuffer;
  }

  public JBBPBitOrder getOrder() {
    return this.msb0 ? JBBPBitOrder.MSB0 : JBBPBitOrder.LSB0;
  }

  public int readBits(final int numOfBitsToRead) throws IOException {
    if (numOfBitsToRead <= 0 || numOfBitsToRead > 8) {
      throw new IllegalArgumentException("Only 1..8 bits allowed to be read by the method");
    }

    int result;

    if (bitsInBuffer == 0 && numOfBitsToRead == 8) {
      result = this.readByteFromStream();
      return result;
    }
    else {
      result = 0;
      int i = numOfBitsToRead;
      int mask = 0x80;

      while (i > 0) {
        if (this.bitsInBuffer == 0) {
          final int nextByte = this.readByteFromStream();
          if (nextByte < 0) {
            if (i == numOfBitsToRead) {
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
        final int nxt = readBits(8);
        if (nxt < 0) {
          break;
        }
        count++;
        i--;
      }
      return count;
    }
  }

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

  private int loadNextByteInBuffer() throws IOException {
    final int value = this.readByteFromStream();
    if (value < 0) {
      return value;
    }

    this.bitBuffer = value;
    this.bitsInBuffer = 8;

    return value;
  }

  public void alignByte() {
    if (this.bitsInBuffer > 0) {
      this.bitsInBuffer = 0;
    }
  }

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
        final int nextByte = this.readBits(8);
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
      return this.readBits(8);
    }
  }

}
