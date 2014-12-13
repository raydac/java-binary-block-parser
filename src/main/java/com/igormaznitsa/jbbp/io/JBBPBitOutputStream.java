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
 * The Filter allows to write bit by bit into an output stream and count the written byte number. The Class is not a thread-safe one.
 * @since 1.0
 */
public class JBBPBitOutputStream extends FilterOutputStream implements JBBPCountableBitStream {
  /**
   * Inside bit buffer.
   */
  private int bitBuffer;
  /**
   * Number of bits inside the bit buffer.
   */
  private int bitBufferCount;
  /**
   * The byte counter of written bytes.
   */
  private long byteCounter;
  /**
   * Flag shows that bit operations must be processed for MSB0 (most significant
   * bit 0) mode.
   */
  private final boolean msb0;

  /**
   * A Constructor. The Default LSB0 bit mode will be used for bit writing operations.
   * @param out the output stream to be filtered.
   */
  public JBBPBitOutputStream(final OutputStream out) {
    this(out, JBBPBitOrder.LSB0);
  }

  /**
   * A Constructor.
   * @param out an output stream to be filtered.
   * @param order a bit writing mode to used for writing operations.
   * @see JBBPBitOrder#LSB0
   * @see JBBPBitOrder#MSB0
   */
  public JBBPBitOutputStream(final OutputStream out, final JBBPBitOrder order) {
    super(out);
    this.msb0 = order == JBBPBitOrder.MSB0;
  }

  /**
   * Get the bit mode for writing operations.
   * @return the bit order for reading operations.
   * @see JBBPBitOrder#LSB0
   * @see JBBPBitOrder#MSB0
   */
  public JBBPBitOrder getBitOrder() {
    return this.msb0 ? JBBPBitOrder.MSB0 : JBBPBitOrder.LSB0;
  }

  /**
   * Write a signed short value into the output stream.
   * @param value a value to be written. Only two bytes will be written.
   * @param byteOrder the byte order of the value bytes to be used for writing.
   * @throws IOException it will be thrown for transport errors
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   */
  public void writeShort(final int value, final JBBPByteOrder byteOrder) throws IOException {
    if (byteOrder == JBBPByteOrder.BIG_ENDIAN) {
      this.write(value >>> 8);
      this.write(value);
    }
    else {
      this.write(value);
      this.write(value >>> 8);
    }
  }

  /**
   * Write an integer value into the output stream.
   * @param value a value to be written into the output stream.
   * @param byteOrder the byte order of the value bytes to be used for writing.
   * @throws IOException it will be thrown for transport errors
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   */
  public void writeInt(final int value, final JBBPByteOrder byteOrder) throws IOException {
    if (byteOrder == JBBPByteOrder.BIG_ENDIAN) {
      this.writeShort(value >>> 16, byteOrder);
      this.writeShort(value, byteOrder);
    }
    else {
      this.writeShort(value, byteOrder);
      this.writeShort(value >>> 16, byteOrder);
    }
  }

  /**
   * Write a long value into the output stream.
   *
   * @param value a value to be written into the output stream.
   * @param byteOrder the byte order of the value bytes to be used for writing.
   * @throws IOException it will be thrown for transport errors
   * @see JBBPByteOrder#BIG_ENDIAN
   * @see JBBPByteOrder#LITTLE_ENDIAN
   */
  public void writeLong(final long value, final JBBPByteOrder byteOrder) throws IOException {
    if (byteOrder == JBBPByteOrder.BIG_ENDIAN) {
      this.writeInt((int) (value >>> 32), byteOrder);
      this.writeInt((int) value, byteOrder);
    }
    else {
      this.writeInt((int) value, byteOrder);
      this.writeInt((int) (value >>> 32), byteOrder);
    }
  }

  /**
   * Get number of bytes written into the output stream.
   * @return the long value contains number of bytes written into the stream
   */
  public long getCounter() {
    return this.byteCounter;
  }

  /**
   * Get the inside bit buffer value.
   * @return the inside bit buffer value
   */
  public int getBitBuffer() {
    return this.bitBuffer;
  }

  /**
   * Get the number of bits cached in the inside bit buffer.
   * @return the number of cached bits in the bit buffer
   */
  public int getBufferedBitsNumber() {
    return this.bitBufferCount;
  }

  /**
   * Flush the bit buffer into the output stream
   * @throws IOException it will be thrown for transport errors
   */
  private void flushBitBuffer() throws IOException {
    if (this.bitBufferCount > 0) {
      this.bitBufferCount = 0;
      writeByte(this.bitBuffer);
    }
  }

  @Override
  public void flush() throws IOException {
    flushBitBuffer();
    this.out.flush();
  }

  @Override
  public void write(final byte[] b, final int off, final int len) throws IOException {
    if (this.bitBufferCount == 0) {
      out.write(b, off, len);
      this.byteCounter += len;
    }
    else {
      int i = off;
      int cnt = len;
      while (cnt > 0) {
        this.write((int) b[i++]);
        cnt--;
      }
    }
  }

  @Override
  public void write(final byte[] b) throws IOException {
    this.write(b, 0, b.length);
  }

  /**
   * Write bits into the output stream.
   * @param value the value which bits will be written in the output stream
   * @param bitNumber number of bits from the value to be written, must be in 1..8
   * @throws IOException it will be thrown for transport errors
   * @throws IllegalArgumentException it will be thrown for wrong bit number
   */
  public void writeBits(final int value, final JBBPBitNumber bitNumber) throws IOException {
    if (this.bitBufferCount == 0 && bitNumber == JBBPBitNumber.BITS_8) {
      write(value);
    }
    else {
      final int initialMask;
      int mask;
      initialMask = 1;
      mask = initialMask << this.bitBufferCount;

      int accum = value;
      int i = bitNumber.getBitNumber();

      while (i > 0) {
        this.bitBuffer = this.bitBuffer | ((accum & 1) == 0 ? 0 : mask);
        accum >>= 1;

        mask = mask << 1;

        i--;
        this.bitBufferCount++;
        if (this.bitBufferCount == 8) {
          this.bitBufferCount = 0;
          writeByte(this.bitBuffer);
          mask = initialMask;
          this.bitBuffer = 0;
        }
      }
    }
  }

  /**
   * Write padding bytes to align the stream counter for the border.
   * @param alignByteNumber the alignment border
   * @throws IOException it will be thrown for transport errors
   */
  public void align(final long alignByteNumber) throws IOException {
    if (this.bitBufferCount > 0) {
      this.writeBits(0, JBBPBitNumber.decode(8 - this.bitBufferCount));
    }
    
    if (alignByteNumber>0){
      long padding = (alignByteNumber - (this.byteCounter % alignByteNumber)) % alignByteNumber;
      while(padding>0){
        this.out.write(0);
        this.byteCounter++;
        padding--;
      }
    }
  }
  
  /**
   * Inside method to write a byte into wrapped stream.
   * @param value a byte value to be written
   * @throws IOException it will be thrown for transport problems
   */
  private void writeByte(int value) throws IOException {
    if (this.msb0){
      value = JBBPUtils.reverseBitsInByte((byte)value) & 0xFF;
    }
    this.out.write(value);
    this.byteCounter++;
  }

  @Override
  public void close() throws IOException {
    this.flush();
    this.out.close();
  }

  @Override
  public void write(final int value) throws IOException {
    if (this.bitBufferCount == 0) {
      writeByte(value);
    }
    else {
      writeBits(value, JBBPBitNumber.BITS_8);
    }
  }

  /**
   * Reset the byte counter for the stream. The Inside bit buffer will be reset also.
   */
  public void resetCounter() {
    this.bitBuffer = 0;
    this.bitBufferCount = 0;
    this.byteCounter = 0L;
  }

}
