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

public class JBBPBitOutputStream extends FilterOutputStream implements JBBPBitStream {

  private int bitBuffer;
  private int bitBufferCount;
  private long byteCounter;
  private final boolean msb0;

  public JBBPBitOutputStream(final OutputStream out) {
    this(out, JBBPBitOrder.LSB0);
  }

  public JBBPBitOutputStream(final OutputStream out, final JBBPBitOrder order) {
    super(out);
    this.msb0 = order == JBBPBitOrder.MSB0;
  }

  public JBBPBitOrder getOrder() {
    return this.msb0 ? JBBPBitOrder.MSB0 : JBBPBitOrder.LSB0;
  }

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

  public long getCounter() {
    return this.byteCounter;
  }

  public int getBitBuffer() {
    return this.bitBuffer;
  }

  public int getBufferedBitsNumber() {
    return this.bitBufferCount;
  }

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

  public void writeBits(final int bitNumber, final int value) throws IOException {
    if (bitNumber <= 0 || bitNumber > 8) {
      throw new IllegalArgumentException("Number of bits to be saved must be 1..8");
    }

    if (this.bitBufferCount == 0 && bitNumber == 8) {
      write(value);
    }
    else {
      final int initialMask;
      int mask;
      initialMask = 1;
      mask = initialMask << this.bitBufferCount;

      int accum = value;
      int i = bitNumber;

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

  private void writeByte(int b) throws IOException {
    if (this.msb0){
      b = JBBPUtils.reverseByte((byte)b) & 0xFF;
    }
    this.out.write(b);
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
      writeBits(8, value);
    }
  }

}
