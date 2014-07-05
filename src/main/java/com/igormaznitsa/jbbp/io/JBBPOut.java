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

public class JBBPOut {

  private final JBBPBitOrder bitOrder;
  private JBBPByteOrder byteOrder;
  private final JBBPBitOutputStream outStream;
  private boolean ended;
  private final ByteArrayOutputStream originalByteArrayOutStream;

  public static final JBBPByteOrder DEFAULT_BYTE_ORDER = JBBPByteOrder.BIG_ENDIAN;
  public static final JBBPBitOrder DEFAULT_BIT_ORDER = JBBPBitOrder.LSB0;
  
  public static JBBPOut binStart(final JBBPByteOrder byteOrder, final JBBPBitOrder bitOrder) {
    return new JBBPOut(new ByteArrayOutputStream(), byteOrder, bitOrder);
  }

  public static JBBPOut binStart() {
    return new JBBPOut(new ByteArrayOutputStream());
  }

  public static JBBPOut binStart(final OutputStream out) {
    return new JBBPOut(out);
  }

  public static JBBPOut binStart(final JBBPByteOrder byteOrder) {
    return new JBBPOut(new ByteArrayOutputStream(), byteOrder, DEFAULT_BIT_ORDER);
  }

  public static JBBPOut binStart(final JBBPBitOrder bitOrder) {
    return new JBBPOut(new ByteArrayOutputStream(), DEFAULT_BYTE_ORDER, bitOrder);
  }

  public JBBPOut(final OutputStream outStream) {
    this(outStream, DEFAULT_BYTE_ORDER, DEFAULT_BIT_ORDER);
  }

  public JBBPOut(final OutputStream outStream, final JBBPByteOrder byteOrder, final JBBPBitOrder bitOrder) {
    JBBPUtils.assertNotNull(outStream, "Out stream must not be null");
    JBBPUtils.assertNotNull(byteOrder, "Byte order must not be null");
    JBBPUtils.assertNotNull(bitOrder, "Bit order must not be null");

    this.outStream = outStream instanceof JBBPBitOutputStream ? (JBBPBitOutputStream) outStream : new JBBPBitOutputStream(outStream, bitOrder);
    this.bitOrder = this.outStream.getOrder();
    if (this.bitOrder != bitOrder) {
      throw new IllegalArgumentException("Detected JBBPBitOutputStream as argument with already defined different bit order [" + this.bitOrder + ']');
    }
    this.byteOrder = byteOrder;

    if (outStream instanceof ByteArrayOutputStream) {
      this.originalByteArrayOutStream = (ByteArrayOutputStream) outStream;
    }
    else {
      this.originalByteArrayOutStream = null;
    }
  }

  public JBBPOut Align() throws IOException {
    assertNotEnded();
    final int numberOfBufferedBits = this.outStream.getBufferedBitsNumber();
    if (numberOfBufferedBits > 0) {
      this.outStream.writeBits(8 - numberOfBufferedBits, 0);
    }
    return this;
  }

  public JBBPOut Align(final int alignForBorder) throws IOException {
    assertNotEnded();
    this.Align();

    if (this.outStream.getCounter()>0){
      while(this.outStream.getCounter() % alignForBorder!=0){
        this.outStream.write(0);
      }
    }
    
    return this;
  }

  public JBBPOut ByteOrder(final JBBPByteOrder value) throws IOException {
    assertNotEnded();
    JBBPUtils.assertNotNull(value, "Byte order must not be null");
    this.byteOrder = value;
    return this;
  }

  public JBBPOut Bit(final boolean value) throws IOException {
    assertNotEnded();
    this.outStream.writeBits(1, value ? 1 : 0);
    return this;
  }

  public JBBPOut Bit(final byte value) throws IOException {
    assertNotEnded();
    this.outStream.writeBits(1, value & 1);
    return this;
  }

  public JBBPOut Bit(final byte... value) throws IOException {
    assertNotEnded();
    JBBPUtils.assertNotNull(value, "Array must not be null");
    for (final byte b : value) {
      this.Bit(b);
    }
    return this;
  }

  public JBBPOut Bit(final int... value) throws IOException {
    assertNotEnded();
    JBBPUtils.assertNotNull(value, "Array must not be null");
    for (final int b : value) {
      this.Bit((byte) b);
    }
    return this;
  }

  public JBBPOut Bit(final boolean... value) throws IOException {
    assertNotEnded();
    JBBPUtils.assertNotNull(value, "Array must not be null");
    for (final boolean b : value) {
      this.Bit(b);
    }
    return this;
  }

  public JBBPOut Bits(final JBBPNumberOfBits numberOfBits, final int value) throws IOException {
    assertNotEnded();
    JBBPUtils.assertNotNull(numberOfBits, "Number of bits must not be null");
    this.outStream.writeBits(numberOfBits.getNumberOfBits(), value);
    return this;
  }

  public JBBPOut Bits(final JBBPNumberOfBits numberOfBits, final int... value) throws IOException {
    assertNotEnded();
    JBBPUtils.assertNotNull(value, "Array must not be null");
    for (final int v : value) {
      this.Bits(numberOfBits, v);
    }
    return this;
  }

  public JBBPOut Bits(final JBBPNumberOfBits numberOfBits, final byte... value) throws IOException {
    assertNotEnded();
    JBBPUtils.assertNotNull(value, "Array must not be null");
    for (final byte b : value) {
      this.Bits(numberOfBits, b);
    }
    return this;
  }

  public JBBPOut Byte(final int value) throws IOException {
    assertNotEnded();
    this.outStream.write(value);
    return this;
  }

  public JBBPOut Byte(final int... value) throws IOException {
    assertNotEnded();
    JBBPUtils.assertNotNull(value, "Array must not be null");
    for (final int v : value) {
      this.Byte(v);
    }
    return this;
  }

  public JBBPOut Byte(final byte... value) throws IOException {
    assertNotEnded();
    JBBPUtils.assertNotNull(value, "Array must not be null");
    for (final byte v : value) {
      this.Byte((int) v);
    }
    return this;
  }

  public JBBPOut Bool(final boolean value) throws IOException {
    assertNotEnded();
    this.outStream.write(value ? 1 : 0);
    return this;
  }

  public JBBPOut Bool(final boolean... value) throws IOException {
    assertNotEnded();
    JBBPUtils.assertNotNull(value, "Array must not be null");
    for (final boolean b : value) {
      this.Bool(b);
    }
    return this;
  }

  public JBBPOut Short(final int value) throws IOException {
    assertNotEnded();
    this.outStream.writeShort(value, this.byteOrder);
    return this;
  }

  public JBBPOut Short(final short... value) throws IOException {
    assertNotEnded();
    JBBPUtils.assertNotNull(value, "Array must not be null");
    for (final short v : value) {
      this.Short(v);
    }
    return this;
  }

  public JBBPOut Short(final int... value) throws IOException {
    assertNotEnded();
    JBBPUtils.assertNotNull(value, "Array must not be null");
    for (final int v : value) {
      this.Short(v);
    }
    return this;
  }

  public JBBPOut Int(final int value) throws IOException {
    assertNotEnded();
    this.outStream.writeInt(value, this.byteOrder);
    return this;
  }

  public JBBPOut Int(final int... value) throws IOException {
    assertNotEnded();
    JBBPUtils.assertNotNull(value, "Array must not be null");
    for (final int v : value) {
      this.Int(v);
    }
    return this;
  }

  public JBBPOut Long(final long value) throws IOException {
    assertNotEnded();
    this.outStream.writeLong(value, this.byteOrder);
    return this;
  }

  public JBBPOut Long(final long... value) throws IOException {
    assertNotEnded();
    JBBPUtils.assertNotNull(value, "Array must not be null");
    for (final long l : value) {
      this.Long(l);
    }
    return this;
  }

  public JBBPOut Flush() throws IOException {
    assertNotEnded();
    this.outStream.flush();
    return this;
  }

  public ByteArrayOutputStream end() throws IOException {
    assertNotEnded();
    this.ended = true;
    this.outStream.flush();
    return this.originalByteArrayOutStream;
  }

  private void assertNotEnded() {
    if (this.ended) {
      throw new IllegalStateException(JBBPOut.class.getSimpleName()+" has been eneded");
    }
  }
}
