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

import com.igormaznitsa.jbbp.exceptions.JBBPIOException;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinFieldFilter;
import com.igormaznitsa.jbbp.mapper.JBBPMapper;
import com.igormaznitsa.jbbp.model.JBBPFieldShort;
import com.igormaznitsa.jbbp.utils.BinAnnotationWrapper;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;

/**
 * The Class implements some kind of DSL to form binary blocks. The Class is not
 * a thread-safe one.
 *
 * @since 1.0
 */
public class JBBPOut extends AbstractMappedClassFieldObserver {

  /**
   * The Default byte outOrder.
   */
  public static final JBBPByteOrder DEFAULT_BYTE_ORDER = JBBPByteOrder.BIG_ENDIAN;
  /**
   * The Default bit outOrder.
   */
  public static final JBBPBitOrder DEFAULT_BIT_ORDER = JBBPBitOrder.LSB0;
  /**
   * The Bit outOrder for operations.
   */
  private final JBBPBitOrder bitOrder;
  /**
   * The Bit stream for operations.
   */
  private final JBBPBitOutputStream outStream;
  /**
   * If the DSL session was started for an external byte array output stream
   * then it will be saved into the variable.
   */
  private final ByteArrayOutputStream originalByteArrayOutStream;
  /**
   * Flag shows that all commands must be skipped till the End.
   */
  private boolean processCommands = true;
  /**
   * The Byte outOrder for operations of multibyte value output.
   */
  private JBBPByteOrder byteOrder;
  /**
   * The Flags show that the processing has been ended.
   */
  private boolean ended;

  /**
   * The Constructor.
   *
   * @param outStream the output stream for the session, it must not be null.
   * @param byteOrder the byte outOrder for the session, it must not be null.
   * @param bitOrder  the bit outOrder for the session, it must not be null
   * @throws IllegalArgumentException if defined a bit stream which parameters
   *                                  incompatible with defined ones
   */
  private JBBPOut(final OutputStream outStream, final JBBPByteOrder byteOrder,
                  final JBBPBitOrder bitOrder) {
    JBBPUtils.assertNotNull(outStream, "Out stream must not be null");
    JBBPUtils.assertNotNull(byteOrder, "Byte order must not be null");
    JBBPUtils.assertNotNull(bitOrder, "Bit order must not be null");

    this.outStream = outStream instanceof JBBPBitOutputStream ? (JBBPBitOutputStream) outStream :
            new JBBPBitOutputStream(outStream, bitOrder);
    this.bitOrder = this.outStream.getBitOrder();
    if (this.bitOrder != bitOrder) {
      throw new IllegalArgumentException(
              "Detected JBBPBitOutputStream as argument with already defined different bit order [" +
                      this.bitOrder + ']');
    }
    this.byteOrder = byteOrder;

    if (outStream instanceof ByteArrayOutputStream) {
      this.originalByteArrayOutStream = (ByteArrayOutputStream) outStream;
    } else {
      this.originalByteArrayOutStream = null;
    }
  }

  /**
   * Start a DSL session for defined both byte outOrder and bit outOrder parameters.
   *
   * @param byteOrder the byte outOrder to be used for the session
   * @param bitOrder  the bit outOrder to be used for the session
   * @return the new DSL session generated with the parameters and inside byte
   * array stream.
   */
  public static JBBPOut BeginBin(final JBBPByteOrder byteOrder, final JBBPBitOrder bitOrder) {
    return new JBBPOut(new ByteArrayOutputStream(), byteOrder, bitOrder);
  }

  /**
   * Start a DSL session for a defined stream with defined parameters.
   *
   * @param out       the defined stream
   * @param byteOrder the byte outOrder for the session
   * @param bitOrder  the bit outOrder for the session
   * @return the new DSL session generated for the stream with parameters
   */
  public static JBBPOut BeginBin(final OutputStream out, final JBBPByteOrder byteOrder,
                                 final JBBPBitOrder bitOrder) {
    return new JBBPOut(out, byteOrder, bitOrder);
  }

  /**
   * Start a DSL session for default parameters and inside byte array stream.
   *
   * @return the new DSL session generated with the default parameters and
   * inside byte array stream.
   */
  public static JBBPOut BeginBin() {
    return new JBBPOut(new ByteArrayOutputStream(), DEFAULT_BYTE_ORDER, DEFAULT_BIT_ORDER);
  }

  /**
   * Start a DSL session for default parameters and inside byte array stream
   * with defined start size.
   *
   * @param initialSize the start size of inside buffer of the byte array output
   *                    stream, must be positive
   * @return the new DSL session generated with the default parameters and
   * inside byte array stream.
   */
  public static JBBPOut BeginBin(final int initialSize) {
    return new JBBPOut(new ByteArrayOutputStream(initialSize), DEFAULT_BYTE_ORDER,
            DEFAULT_BIT_ORDER);
  }

  /**
   * Start a DSL session for a defined output stream and default parameters.
   *
   * @param out an output stream to write session data, must not be null.
   * @return the new DSL session generated for the default parameters and the
   * output stream.
   */
  public static JBBPOut BeginBin(final OutputStream out) {
    return new JBBPOut(out, DEFAULT_BYTE_ORDER, DEFAULT_BIT_ORDER);
  }

  /**
   * Start a DSL session for default bit outOrder and defined byte outOrder. It will
   * be using inside byte array stream.
   *
   * @param byteOrder the byte outOrder for the session, it must not be null.
   * @return the new DSL session
   */
  public static JBBPOut BeginBin(final JBBPByteOrder byteOrder) {
    return new JBBPOut(new ByteArrayOutputStream(), byteOrder, DEFAULT_BIT_ORDER);
  }

  /**
   * Start a DSL session for default byte outOrder and defined bite outOrder. It will
   * be using inside byte array stream.
   *
   * @param bitOrder the bite outOrder for the session, it must not be null.
   * @return the new DSL session
   */
  public static JBBPOut BeginBin(final JBBPBitOrder bitOrder) {
    return new JBBPOut(new ByteArrayOutputStream(), DEFAULT_BYTE_ORDER, bitOrder);
  }

  /**
   * Inside wrapper of not null assertion with text for arrays.
   *
   * @param array an object to be checked for null.
   */
  private static void assertArrayNotNull(final Object array) {
    JBBPUtils.assertNotNull(array, "Array must not be null");
  }

  /**
   * Inside wrapper of not null assertion with text for strings.
   *
   * @param str an object to be checked for null.
   */
  private static void assertStringNotNull(final String str) {
    JBBPUtils.assertNotNull(str, "String must not be null");
  }

  /**
   * Align the current stream for 1 byte. If there are any bites inside bit
   * cache then they will be saved and the stream will be positioning to the
   * next byte. It works relative to the byte output counter.
   *
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   * @see JBBPOut#ResetCounter()
   */
  public JBBPOut Align() throws IOException {
    assertNotEnded();
    if (this.processCommands) {
      this.outStream.align(0);
    }
    return this;
  }

  /**
   * Align number of bytes in the stream to the value. It works relative to the
   * byte output counter.
   *
   * @param value the byte border to align the stream.
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   * @see JBBPOut#ResetCounter()
   */
  public JBBPOut Align(final int value) throws IOException {
    assertNotEnded();
    if (this.processCommands) {
      this.outStream.align(value);
    }
    return this;
  }

  /**
   * Skip number of bytes in the stream, zero bytes will be written and also
   * will be aligned inside bit cache even if the value is 0.
   *
   * @param numberOfBytes the number of bytes to be skipped
   * @return the DSL session
   * @throws IOException              it will be thrown for transport errors
   * @throws IllegalArgumentException it will be thrown if the value is negative
   *                                  one
   */
  public JBBPOut Skip(int numberOfBytes) throws IOException {
    assertNotEnded();
    if (this.processCommands) {
      if (numberOfBytes < 0) {
        throw new IllegalArgumentException("Value is negative");
      }
      this.Align();
      while (numberOfBytes > 0) {
        this.outStream.write(0);
        numberOfBytes--;
      }
    }
    return this;
  }

  /**
   * Define the byte outOrder for next session operations.
   *
   * @param value the byte outOrder to be used in next operations, must not be null
   * @return the DSL session
   */
  public JBBPOut ByteOrder(final JBBPByteOrder value) {
    assertNotEnded();
    JBBPUtils.assertNotNull(value, "Byte order must not be null");
    if (this.processCommands) {
      this.byteOrder = value;
    }
    return this;
  }

  /**
   * Write a bit into the session.
   *
   * @param value true if the bit is 1, 0 otherwise
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPOut Bit(final boolean value) throws IOException {
    assertNotEnded();
    if (this.processCommands) {
      this.outStream.writeBits(value ? 1 : 0, JBBPBitNumber.BITS_1);
    }
    return this;
  }

  /**
   * Write the lowest bit from a byte value.
   *
   * @param value the byte value which lowest bit will be written into the
   *              stream
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPOut Bit(final byte value) throws IOException {
    assertNotEnded();
    if (this.processCommands) {
      this._writeBits(JBBPBitNumber.BITS_1, value);
    }
    return this;
  }

  /**
   * Write the lowest bits of bytes from an array.
   *
   * @param value a byte array, lowest bit of each byte will be saved as a bit
   *              into the output stream, it must not be null
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPOut Bit(final byte[] value) throws IOException {
    assertNotEnded();
    assertArrayNotNull(value);
    if (this.processCommands) {
      for (final byte b : value) {
        this._writeBits(JBBPBitNumber.BITS_1, b);
      }
    }
    return this;
  }

  /**
   * Write the lowest bits of integers from an array.
   *
   * @param value an integer array, lowest bit of each integer value will be
   *              saved as a bit into the output stream, it must not be null
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPOut Bit(final int... value) throws IOException {
    assertNotEnded();
    assertArrayNotNull(value);
    if (this.processCommands) {
      for (final int b : value) {
        this._writeBits(JBBPBitNumber.BITS_1, b);
      }
    }
    return this;
  }

  /**
   * Write bits represented as boolean flags into the output stream.
   *
   * @param value a boolean array which values will be saved into the output
   *              stream as bits, true is a bit on, false is bit off. It must not be null
   * @return the DSL session.
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPOut Bit(final boolean... value) throws IOException {
    assertNotEnded();
    assertArrayNotNull(value);
    if (this.processCommands) {
      for (final boolean b : value) {
        this._writeBits(JBBPBitNumber.BITS_1, b ? 1 : 0);
      }
    }
    return this;
  }

  /**
   * Inside auxiliary method to write bits into output stream without check of
   * the session end
   *
   * @param numberOfBits number of bits from value to save, it must not be null
   * @param value        the value which bits must be saved
   * @throws IOException it will be thrown for transport errors
   */
  private void _writeBits(final JBBPBitNumber numberOfBits, final int value) throws IOException {
    this.outStream.writeBits(value, numberOfBits);
  }

  /**
   * Write bits from a value into the output stream
   *
   * @param numberOfBits the number of bits to be saved
   * @param value        the value which bits must be saved
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPOut Bits(final JBBPBitNumber numberOfBits, final int value) throws IOException {
    assertNotEnded();
    JBBPUtils.assertNotNull(numberOfBits, "Number of bits must not be null");
    if (this.processCommands) {
      _writeBits(numberOfBits, value);
    }
    return this;
  }

  /**
   * Write bits of each integer value from an array into the output stream.
   *
   * @param numberOfBits the number of bits to be saved
   * @param value        an integer array which elements will be used as sources of
   *                     bits, it must not be null
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPOut Bits(final JBBPBitNumber numberOfBits, final int... value) throws IOException {
    assertNotEnded();
    JBBPUtils.assertNotNull(value, "Array must not be null");
    if (this.processCommands) {
      for (final int v : value) {
        _writeBits(numberOfBits, v);
      }
    }
    return this;
  }

  /**
   * Write bits of each byte value from an array into the output stream.
   *
   * @param numberOfBits the number of bits to be saved
   * @param value        a byte array which elements will be used as sources of bits,
   *                     it must not be null
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPOut Bits(final JBBPBitNumber numberOfBits, final byte[] value) throws IOException {
    assertNotEnded();
    JBBPUtils.assertNotNull(value, "Array must not be null");
    if (this.processCommands) {
      for (final byte b : value) {
        _writeBits(numberOfBits, b);
      }
    }
    return this;
  }

  /**
   * Inside auxiliary speed version of byte writing method
   *
   * @param value a byte to write into session stream
   * @throws IOException it will be thrown for transport errors
   */
  private void _writeByte(final int value) throws IOException {
    this.outStream.write(value);
  }

  /**
   * Write the lower byte of an integer value into the session stream.
   *
   * @param value an integer value which byte should be written into
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPOut Byte(final int value) throws IOException {
    assertNotEnded();
    if (this.processCommands) {
      _writeByte(value);
    }
    return this;
  }

  /**
   * Write the lower byte of an integer value into the session stream.
   *
   * @param value an integer array which values will be byte sources to write
   *              their lower byte into the stream
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPOut Byte(final int... value) throws IOException {
    assertNotEnded();
    assertArrayNotNull(value);
    if (this.processCommands) {
      for (final int v : value) {
        _writeByte(v);
      }
    }
    return this;
  }

  /**
   * Write a byte array into the session stream.
   *
   * @param value a byte array to be written
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPOut Byte(final byte[] value) throws IOException {
    assertNotEnded();
    assertArrayNotNull(value);
    if (this.processCommands) {
      this.outStream.write(value);
    }
    return this;
  }

  /**
   * Write String chars trimmed to bytes, only the lower 8 bit will be saved per
   * char code.
   *
   * @param str a String which chars should be trimmed to bytes and saved
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPOut Byte(final String str) throws IOException {
    assertNotEnded();
    assertStringNotNull(str);
    if (this.processCommands) {
      for (int i = 0; i < str.length(); i++) {
        this.outStream.write(str.charAt(i));
      }
    }
    return this;
  }

  /**
   * Write String chars trimmed to bytes, only the lower 8 bit will be saved per
   * char code.
   *
   * @param str      a String which chars should be trimmed to bytes and saved
   * @param bitOrder the bit outOrder to save bytes
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   * @since 1.1
   */
  public JBBPOut Byte(final String str, final JBBPBitOrder bitOrder) throws IOException {
    assertNotEnded();
    assertStringNotNull(str);
    if (this.processCommands) {
      for (int i = 0; i < str.length(); i++) {
        byte value = (byte) str.charAt(i);
        if (bitOrder == JBBPBitOrder.MSB0) {
          value = JBBPUtils.reverseBitsInByte(value);
        }
        this.outStream.write(value);
      }
    }
    return this;
  }

  /**
   * Write chars of a String as encoded Utf8 byte array. There will not be aby information about string length.
   *
   * @param str a String which bytes should be written as Utf8, must not be null
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPOut Utf8(final String str) throws IOException {
    assertNotEnded();
    assertStringNotNull(str);
    if (this.processCommands) {
      this.outStream.write(JBBPUtils.strToUtf8(str));
    }
    return this;
  }

  /**
   * Write string into output stream with length information.
   * <b>the byte order in saved char data will be BIG_ENDIAN</b>
   *
   * @param str string to be written, it can be null
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   * @see JBBPBitOutputStream#writeString(String, JBBPByteOrder)
   * @since 1.4.0
   */
  public JBBPOut String(String str) throws IOException {
    this.outStream.writeString(str, this.byteOrder);
    return this;
  }

  /**
   * Write string array as sequence of strings with information about string length.
   * <b>the byte order in saved char data will be BIG_ENDIAN</b>
   *
   * @param strings array of strings, must not be null but can contain null
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   * @see JBBPBitOutputStream#writeString(String, JBBPByteOrder)
   * @since 1.4.0
   */
  public JBBPOut Strings(final String... strings) throws IOException {
    for (final String s : strings) {
      this.String(s);
    }
    return this;
  }

  /**
   * Write a boolean value into the session stream as a byte.
   *
   * @param value a boolean value to be written, true is 1, false is 0
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPOut Bool(final boolean value) throws IOException {
    assertNotEnded();
    if (this.processCommands) {
      this.outStream.write(value ? 1 : 0);
    }
    return this;
  }

  /**
   * Write a boolean value into the session stream as a byte.
   *
   * @param value    a boolean value to be written, true is 1, false is 0
   * @param bitOrder bit outOrder for saving data
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   * @since 1.1
   */
  public JBBPOut Bool(final boolean value, final JBBPBitOrder bitOrder) throws IOException {
    assertNotEnded();
    if (this.processCommands) {
      this.outStream.write(value ? bitOrder == JBBPBitOrder.MSB0 ? 0x80 : 1 : 0);
    }
    return this;
  }

  /**
   * Write boolean values from an array into the session stream as bytes.
   *
   * @param value a boolean array to be saved as bytes, true is 1, false is 0.
   *              It must not be null
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPOut Bool(final boolean... value) throws IOException {
    assertNotEnded();
    assertArrayNotNull(value);
    if (this.processCommands) {
      for (final boolean b : value) {
        this.outStream.write(b ? 1 : 0);
      }
    }
    return this;
  }

  /**
   * Inside auxiliary method to write a short value into the session stream
   *
   * @param value a value to be written
   * @throws IOException it will be thrown for transport errors
   */
  private void _writeShort(final int value) throws IOException {
    this.outStream.writeShort(value, this.byteOrder);
  }

  /**
   * Write lower pair of bytes of an integer value into the session stream as a
   * short value.
   *
   * @param value an integer value which lower pair of bytes will be written
   *              into
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPOut Short(final int value) throws IOException {
    assertNotEnded();
    if (this.processCommands) {
      _writeShort(value);
    }
    return this;
  }

  /**
   * Write codes of chars as 16 bit values into the stream.
   *
   * @param str the string which chars will be written, must not be null
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   * @since 1.1
   */
  public JBBPOut Short(final String str) throws IOException {
    assertNotEnded();
    if (this.processCommands) {
      for (int i = 0; i < str.length(); i++) {
        _writeShort(str.charAt(i));
      }
    }
    return this;
  }

  /**
   * Write codes of chars as 16 bit values into the stream.
   *
   * @param str      the string which chars will be written, must not be null
   * @param bitOrder the bit outOrder
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   * @since 1.1
   */
  public JBBPOut Short(final String str, final JBBPBitOrder bitOrder) throws IOException {
    assertNotEnded();
    if (this.processCommands) {
      final boolean msb0 = bitOrder == JBBPBitOrder.MSB0;
      for (int i = 0; i < str.length(); i++) {
        short value = (short) str.charAt(i);
        if (msb0) {
          value = (short) JBBPFieldShort.reverseBits(value);
        }
        _writeShort(value);
      }
    }
    return this;
  }

  /**
   * Write short values from an array
   *
   * @param value a short value array which values should be written into, it
   *              must not be null
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPOut Short(final short[] value) throws IOException {
    assertNotEnded();
    assertArrayNotNull(value);
    if (this.processCommands) {
      for (final short v : value) {
        this._writeShort(v);
      }
    }
    return this;
  }

  /**
   * Write short values from a char array
   *
   * @param value a char array which values should be written into, it
   *              must not be null
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   * @since 1.3
   */
  public JBBPOut Short(final char[] value) throws IOException {
    assertNotEnded();
    assertArrayNotNull(value);
    if (this.processCommands) {
      for (final char v : value) {
        this._writeShort(v);
      }
    }
    return this;
  }

  /**
   * Write lower pair of bytes of each integer value from an integer array into
   * the session stream as a short value.
   *
   * @param value an integer array which values will be written into
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPOut Short(final int... value) throws IOException {
    assertNotEnded();
    assertArrayNotNull(value);
    if (this.processCommands) {
      for (final int v : value) {
        this._writeShort(v);
      }
    }
    return this;
  }

  /**
   * Inside auxiliary method to write an integer value into the session stream
   * without extra checking.
   *
   * @param value an integer value to be written into
   * @throws IOException it will be thrown for transport errors
   */
  private void _writeInt(final int value) throws IOException {
    this.outStream.writeInt(value, this.byteOrder);
  }

  /**
   * Write an integer value into the session stream.
   *
   * @param value an integer value to be written into
   * @return the DSl session
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPOut Int(final int value) throws IOException {
    assertNotEnded();
    if (this.processCommands) {
      _writeInt(value);
    }
    return this;
  }

  /**
   * Write each integer value from an integer array into the session stream.
   *
   * @param value an integer array which values should be written into
   * @return the DSl session
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPOut Int(final int... value) throws IOException {
    assertNotEnded();
    assertArrayNotNull(value);
    if (this.processCommands) {
      for (final int v : value) {
        _writeInt(v);
      }
    }
    return this;
  }

  @Override
  protected void onFieldUInt(final Object obj, final Field field, final Bin annotation,
                             final int value) {
    final JBBPByteOrder old = this.byteOrder;
    try {
      this.byteOrder = annotation.byteOrder();
      this.UInt(value);
    } catch (IOException ex) {
      throw new JBBPIOException("Can't write unsigned int value", ex);
    } finally {
      this.byteOrder = old;
    }
  }

  /**
   * Write a float value array as integer bits into the stream.
   *
   * @param value a float array which values will be written into
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   * @see Float#floatToIntBits(float)
   * @since 1.4.0
   */
  public JBBPOut Float(final float... value) throws IOException {
    assertNotEnded();
    assertArrayNotNull(value);
    if (this.processCommands) {
      for (final float f : value) {
        _writeFloat(f);
      }
    }
    return this;
  }

  /**
   * Inside auxiliary method to write a long value into the session stream
   * without checking.
   *
   * @param value a long value to be written into
   * @throws IOException it will be thrown for transport errors
   */
  private void _writeLong(final long value) throws IOException {
    this.outStream.writeLong(value, this.byteOrder);
  }

  /**
   * Inside auxiliary method to write a double value into the session stream
   * without checking.
   *
   * @param value a double value to be written into
   * @throws IOException it will be thrown for transport errors
   * @since 1.4.0
   */
  private void _writeDouble(final double value) throws IOException {
    this.outStream.writeDouble(value, this.byteOrder);
  }

  /**
   * Inside auxiliary method to write a float value into the session stream
   * without checking.
   *
   * @param value a float value to be written into
   * @throws IOException it will be thrown for transport errors
   * @since 1.4.0
   */
  private void _writeFloat(final float value) throws IOException {
    this.outStream.writeFloat(value, this.byteOrder);
  }

  /**
   * Write a long value into the session stream.
   *
   * @param value a long value to be written into
   * @return the DSL session
   * @throws IOException it will b e thrown for transport errors
   */
  public JBBPOut Long(final long value) throws IOException {
    assertNotEnded();
    if (this.processCommands) {
      _writeLong(value);
    }
    return this;
  }

  /**
   * Write a double value array as long bits into the stream.
   *
   * @param value a double array which values will be written into
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   * @see Double#doubleToLongBits(double)
   * @since 1.4.0
   */
  public JBBPOut Double(final double... value) throws IOException {
    assertNotEnded();
    assertArrayNotNull(value);
    if (this.processCommands) {
      for (final double d : value) {
        _writeDouble(d);
      }
    }
    return this;
  }

  /**
   * Reset the byte counter and the inside bit buffer of the output stream. it
   * is useful to align command because the command makes alignment for
   * the counter.
   *
   * @return the DSL context
   * @see JBBPOut#Align()
   * @see JBBPOut#Align(int)
   */
  public JBBPOut ResetCounter() {
    assertNotEnded();
    if (this.processCommands) {
      this.outStream.resetCounter();
    }
    return this;
  }

  /**
   * Write each long value from a long value array into the session stream.
   *
   * @param value a long value array which values will be written into
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPOut Long(final long... value) throws IOException {
    assertNotEnded();
    assertArrayNotNull(value);
    if (this.processCommands) {
      for (final long l : value) {
        _writeLong(l);
      }
    }
    return this;
  }

  /**
   * Output data externally.
   *
   * @param processor a processor which will get the stream to write data, must
   *                  not be null
   * @param args      optional arguments to be provided to the processor
   * @return the DSL context
   * @throws IOException          it will be thrown for transport errors
   * @throws NullPointerException it will be thrown for null as a processor
   */
  public JBBPOut Var(final JBBPOutVarProcessor processor, final Object... args) throws IOException {
    assertNotEnded();
    JBBPUtils.assertNotNull(processor, "Var processor must not be null");
    if (this.processCommands) {
      this.processCommands = processor.processVarOut(this, this.outStream, args);
    }
    return this;
  }

  /**
   * Flush inside buffers into the stream. Be careful with bit operations
   * because the operation will flush the inside bit buffer.
   *
   * @return the DSL session
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPOut Flush() throws IOException {
    assertNotEnded();
    if (this.processCommands) {
      this.outStream.flush();
    }
    return this;
  }

  /**
   * Flush the stream and end the session.
   *
   * @return if the session output stream is based on a byte array output stream
   * then the stream will be returned, null otherwise
   * @throws IOException it will be thrown for transport errors.
   */
  public ByteArrayOutputStream End() throws IOException {
    assertNotEnded();
    this.ended = true;
    this.outStream.flush();
    return this.originalByteArrayOutStream;
  }

  /**
   * get the current byte counter value for the underlying stream. it has
   * appropriate value only if it was not reset.
   *
   * @return the current byte counter for the underlying stream.
   */
  public long getByteCounter() {
    return this.outStream.getCounter();
  }

  /**
   * Assert that the session has not ended.
   *
   * @throws IllegalStateException if the session has been ended
   */
  protected void assertNotEnded() {
    if (this.ended) {
      throw new IllegalStateException(JBBPOut.class.getSimpleName() + " has been ended");
    }
  }

  /**
   * Save fields of an object marked by Bin annotation. Fields will be ordered
   * through {@link Bin#order()} field, NB! By default, Java doesn't keep field
   * outOrder. Ordered fields of class will be saved into internal cache for speed
   * but the cache can be reset through {@link JBBPMapper#clearFieldCache()}
   * <b>Warning!</b> it doesn't affect byte order provided in Bin annotations of object.
   *
   * @param object an object to be saved into stream, must not be null
   * @return the context
   * @throws IOException it will be thrown for any transport error
   * @see JBBPMapper#clearFieldCache()
   * @see #BinForceByteOrder(Object)
   * @see Bin
   * @since 1.1
   */
  public JBBPOut Bin(final Object object) throws IOException {
    return this.Bin(object, null, null, null);
  }

  /**
   * Save fields of an object marked by Bin annotation. Fields will be ordered
   * through {@link Bin#order()} field, NB! By default, Java doesn't keep field
   * outOrder. Ordered fields of class will be saved into internal cache for speed
   * but the cache can be reset through {@link JBBPMapper#clearFieldCache()}
   * <b>Warning!</b> it doesn't affect byte order provided in Bin annotations of object.
   *
   * @param object         an object to be saved into stream, must not be null
   * @param binFieldFilter filter to exclude some fields from process, can be null
   * @return the context
   * @throws IOException it will be thrown for any transport error
   * @see JBBPMapper#clearFieldCache()
   * @see #BinForceByteOrder(Object)
   * @see Bin
   * @since 2.0.4
   */
  public JBBPOut Bin(final Object object, final BinFieldFilter binFieldFilter) throws IOException {
    return this.Bin(object, null, null, binFieldFilter);
  }

  /**
   * Save fields of an object marked by Bin annotation. Fields will be ordered
   * through {@link Bin#order()} field, NB! By default, Java doesn't keep field
   * outOrder. Ordered fields of class will be saved into internal cache for speed
   * but the cache can be reset through {@link JBBPMapper#clearFieldCache()}
   * <b>Warning!</b> it doesn't affect byte order provided in Bin annotations of object.
   *
   * @param object            an object to be saved into stream, must not be null
   * @param customFieldWriter a custom field writer to be used for saving of
   *                          custom fields of the object, it can be null
   * @return the context
   * @see JBBPMapper#clearFieldCache()
   * @see Bin
   * @see #BinForceByteOrder(Object, JBBPCustomFieldWriter)
   * @since 1.1
   */
  public JBBPOut Bin(final Object object, final JBBPCustomFieldWriter customFieldWriter) {
    return this.Bin(object, null, customFieldWriter);
  }

  /**
   * Save fields of an object marked by Bin annotation. Fields will be ordered
   * through {@link Bin#order()} field, NB! By default, Java doesn't keep field
   * outOrder. Ordered fields of class will be saved into internal cache for speed
   * but the cache can be reset through {@link JBBPMapper#clearFieldCache()}
   * <b>Warning!</b> it doesn't affect byte order provided in Bin annotations of object.
   *
   * @param object            an object to be saved into stream, must not be null
   * @param customFieldWriter a custom field writer to be used for saving of
   *                          custom fields of the object, it can be null
   * @param binFieldFilter    filter to exclude fields from process, can be null
   * @return the context
   * @see JBBPMapper#clearFieldCache()
   * @see Bin
   * @see #BinForceByteOrder(Object, JBBPCustomFieldWriter)
   * @since 2.0.4
   */
  public JBBPOut Bin(final Object object, final JBBPCustomFieldWriter customFieldWriter, final BinFieldFilter binFieldFilter) {
    return this.Bin(object, null, customFieldWriter, binFieldFilter);
  }

  /**
   * Save fields of object but bin annotation wrapper can be provided to replace some annotation field values in <b>all</b> field annotations.
   *
   * @param object               an object to be saved into stream, must not be null
   * @param binAnnotationWrapper wrapper for all bin annotations, can be null
   * @param customFieldWriter    a custom field writer to be used for saving of
   *                             custom fields of the object, it can be null
   * @return the context
   * @since 2.0.2
   */
  public JBBPOut Bin(final Object object,
                     final BinAnnotationWrapper binAnnotationWrapper,
                     final JBBPCustomFieldWriter customFieldWriter) {
    return this.Bin(object, binAnnotationWrapper, customFieldWriter, null);
  }

  /**
   * Save fields of object but bin annotation wrapper can be provided to replace some annotation field values in <b>all</b> field annotations.
   *
   * @param object               an object to be saved into stream, must not be null
   * @param binAnnotationWrapper wrapper for all bin annotations, can be null
   * @param customFieldWriter    a custom field writer to be used for saving of
   *                             custom fields of the object, it can be null
   * @param binFieldFilter       filter to exclude some fields from process, can be null
   * @return the context
   * @since 2.0.4
   */
  public JBBPOut Bin(final Object object,
                     final BinAnnotationWrapper binAnnotationWrapper,
                     final JBBPCustomFieldWriter customFieldWriter,
                     final BinFieldFilter binFieldFilter) {
    if (this.processCommands) {
      this.processObject(object, null, binAnnotationWrapper, binFieldFilter, customFieldWriter);
    }
    return this;
  }

  /**
   * Works like {@link #Bin(Object)} but forcing override of all annotation byte order values by the JBBPOut byte order.
   *
   * @param object an object to be saved into stream, must not be null
   * @return the context
   * @throws IOException it will be thrown for any transport error
   * @see JBBPMapper#clearFieldCache()
   * @see Bin
   * @see Bin#byteOrder()
   * @since 2.0.2
   */
  public JBBPOut BinForceByteOrder(final Object object) throws IOException {
    return this.BinForceByteOrder(object, null);
  }

  /**
   * Works like {@link #Bin(Object, JBBPCustomFieldWriter)} but forcing override of all annotation byte order values by the context byte order.
   *
   * @param object            an object to be saved into stream, must not be null
   * @param customFieldWriter a custom field writer to be used for saving of
   *                          custom fields of the object, it can be null
   * @return the context
   * @see #ByteOrder(JBBPByteOrder)
   * @see Bin#byteOrder()
   * @since 2.0.2
   */
  public JBBPOut BinForceByteOrder(final Object object,
                                   final JBBPCustomFieldWriter customFieldWriter) {
    return this
            .Bin(object, new BinAnnotationWrapper().setByteOrder(this.byteOrder), customFieldWriter);
  }

  @Override
  protected void onFieldFloat(final Object obj, final Field field, final Bin annotation,
                              final float value) {
    final JBBPByteOrder old = this.byteOrder;
    try {
      this.byteOrder = annotation.byteOrder();
      this.Float(value);
    } catch (IOException ex) {
      throw new JBBPIOException("Can't write float value", ex);
    } finally {
      this.byteOrder = old;
    }
  }

  @Override
  protected void onFieldString(final Object obj, final Field field, final Bin annotation,
                               final String value) {
    final JBBPByteOrder old = this.byteOrder;
    try {
      this.byteOrder = annotation.byteOrder();
      this.String(value);
    } catch (IOException ex) {
      throw new JBBPIOException("Can't write string value", ex);
    } finally {
      this.byteOrder = old;
    }
  }

  @Override
  protected void onFieldDouble(final Object obj, final Field field, final Bin annotation,
                               final double value) {
    final JBBPByteOrder old = this.byteOrder;
    try {
      this.byteOrder = annotation.byteOrder();
      this.Double(value);
    } catch (IOException ex) {
      throw new JBBPIOException("Can't write double value", ex);
    } finally {
      this.byteOrder = old;
    }
  }

  @Override
  protected void onFieldLong(final Object obj, final Field field, final Bin annotation,
                             final long value) {
    final JBBPByteOrder old = this.byteOrder;
    try {
      this.byteOrder = annotation.byteOrder();
      this.Long(value);
    } catch (IOException ex) {
      throw new JBBPIOException("Can't write long value", ex);
    } finally {
      this.byteOrder = old;
    }
  }

  @Override
  protected void onFieldInt(final Object obj, final Field field, final Bin annotation,
                            final int value) {
    final JBBPByteOrder old = this.byteOrder;
    try {
      this.byteOrder = annotation.byteOrder();
      this.Int(value);
    } catch (IOException ex) {
      throw new JBBPIOException("Can't write int value", ex);
    } finally {
      this.byteOrder = old;
    }
  }

  /**
   * Write each long value as unsigned integer one into the session stream.
   *
   * @param value a long value array which values should be written into
   * @return the DSl session
   * @throws IOException it will be thrown for transport errors
   * @since 2.0.4
   */
  public JBBPOut UInt(final long... value) throws IOException {
    assertNotEnded();
    assertArrayNotNull(value);
    if (this.processCommands) {
      for (final long v : value) {
        _writeInt((int) v);
      }
    }
    return this;
  }

  @Override
  protected void onFieldShort(final Object obj, final Field field, final Bin annotation,
                              final boolean signed, final int value) {
    final JBBPByteOrder old = this.byteOrder;
    try {
      this.byteOrder = annotation.byteOrder();
      this.Short(value);
    } catch (IOException ex) {
      throw new JBBPIOException("Can't write short value", ex);
    } finally {
      this.byteOrder = old;
    }
  }

  @Override
  protected void onFieldByte(final Object obj, final Field field, final Bin annotation,
                             final boolean signed, final int value) {
    try {
      this.Byte(value);
    } catch (IOException ex) {
      throw new JBBPIOException("Can't write byte value", ex);
    }
  }

  @Override
  protected void onFieldBool(final Object obj, final Field field, final Bin annotation,
                             final boolean value) {
    try {
      this.Bool(value, annotation.bitOrder());
    } catch (IOException ex) {
      throw new JBBPIOException("Can't write bool value", ex);
    }
  }

  @Override
  protected void onFieldBits(final Object obj, final Field field, final Bin annotation,
                             final JBBPBitNumber bitNumber, final int value) {
    try {
      this.Bits(bitNumber, value);
    } catch (IOException ex) {
      throw new JBBPIOException("Can't write bit value", ex);
    }
  }

  @Override
  protected void onFieldCustom(final Object obj, final Field field, final Bin annotation,
                               final Object customFieldProcessor, final Object value) {
    try {
      final JBBPCustomFieldWriter writer = (JBBPCustomFieldWriter) customFieldProcessor;
      writer.writeCustomField(this, this.outStream, obj, field, annotation, value);
    } catch (IOException ex) {
      throw new JBBPIOException("Can't write custom field", ex);
    }
  }


}
