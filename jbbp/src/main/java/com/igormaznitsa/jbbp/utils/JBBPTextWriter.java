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

package com.igormaznitsa.jbbp.utils;

import com.igormaznitsa.jbbp.exceptions.JBBPIOException;
import com.igormaznitsa.jbbp.io.AbstractMappedClassFieldObserver;
import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.model.JBBPAbstractArrayField;
import com.igormaznitsa.jbbp.model.JBBPAbstractField;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayBit;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayBoolean;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayByte;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayDouble;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayFloat;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayInt;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayLong;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayShort;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayString;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayStruct;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayUByte;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayUInt;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayUShort;
import com.igormaznitsa.jbbp.model.JBBPFieldBit;
import com.igormaznitsa.jbbp.model.JBBPFieldBoolean;
import com.igormaznitsa.jbbp.model.JBBPFieldByte;
import com.igormaznitsa.jbbp.model.JBBPFieldDouble;
import com.igormaznitsa.jbbp.model.JBBPFieldFloat;
import com.igormaznitsa.jbbp.model.JBBPFieldInt;
import com.igormaznitsa.jbbp.model.JBBPFieldLong;
import com.igormaznitsa.jbbp.model.JBBPFieldShort;
import com.igormaznitsa.jbbp.model.JBBPFieldString;
import com.igormaznitsa.jbbp.model.JBBPFieldStruct;
import com.igormaznitsa.jbbp.model.JBBPFieldUByte;
import com.igormaznitsa.jbbp.model.JBBPFieldUInt;
import com.igormaznitsa.jbbp.model.JBBPFieldUShort;
import com.igormaznitsa.jbbp.model.JBBPNumericField;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * the Writer allows to make text describes some bin data, it supports output of
 * commentaries and values, also it is possible to register own extras to
 * process complex data and cases. The Class is not thread safe.
 *
 * @since 1.1
 */
@SuppressWarnings({"resource", "SystemGetProperty"})
public class JBBPTextWriter extends FilterWriter {

  /**
   * The Default comment prefix.
   */
  private static final String DEFAULT_COMMENT_PREFIX = "; ";
  /**
   * The Default value postfix.
   */
  private static final String DEFAULT_VALUE_POSTFIX = "";
  /**
   * The Default horizontal rule prefix.
   */
  private static final String DEFAULT_HR_PREFIX = ";";
  /**
   * The Default first value line.
   */
  private static final String DEFAULT_FIRST_VALUE_LINE_PREFIX = "";
  /**
   * The Default value prefix.
   */
  private static final String DEFAULT_VALUE_PREFIX = "";
  /**
   * The Default value delimiter.
   */
  private static final String DEFAULT_VALUE_DELIMITER = " ";
  /**
   * The Default max value number per line.
   */
  private static final int DEFAULT_MAX_VALUES_PER_LINE = -1;
  /**
   * The Default radix.
   */
  private static final int DEFAULT_RADIX = 16;
  /**
   * The Default length of horizontal rule in chars.
   */
  private static final int DEFAULT_HR_LENGTH = 80;
  /**
   * The Default char to draw horizontal rule.
   */
  private static final char DEFAULT_HR_CHAR = '-';
  /**
   * The Mode shows that the start line.
   */
  private static final int MODE_START_LINE = 0;
  /**
   * the Mode shows that values are printed.
   */
  private static final int MODE_VALUES = 1;
  /**
   * The Mode shows that commentaries printing.
   */
  private static final int MODE_COMMENTS = 2;
  /**
   * The Line separator, must not be null.
   */
  private final String lineSeparator;
  /**
   * Inside char buffer to be used for converting operations.
   */
  private final char[] CHAR_BUFFER = new char[64];
  /**
   * The List contains all registered extras.
   */
  private final List<Extra> extras = new ArrayList<>();
  /**
   * Lazy initialized field to keep field observer for Bin marked classes.
   */
  private MappedObjectLogger mappedClassObserver;
  /**
   * The Variable contains the previous activity mode.
   */
  private int prevMode = MODE_START_LINE;

  /**
   * The Variable contains the current activity mode.
   */
  private int mode = MODE_START_LINE;

  /**
   * The Current horizontal rule length.
   */
  private int hrLength = DEFAULT_HR_LENGTH;

  /**
   * The Current horizontal rule char.
   */
  private char hrChar = DEFAULT_HR_CHAR;

  /**
   * The Current radix.
   */
  private int radix;

  /**
   * The Variable contains number of max chars to show byte for current radix.
   */
  private int maxCharsRadixForByte;

  /**
   * The Variable contains number of max chars to show short for current radix.
   */
  private int maxCharsRadixForShort;

  /**
   * The Variable contains number of max chars to show integer for current
   * radix.
   */
  private int maxCharsRadixForInt;

  /**
   * The Variable contains number of max chars to show long for current radix.
   */
  private int maxCharsRadixForLong;

  /**
   * The flag allows print of comments.
   */
  private boolean flagCommentsAllowed;

  /**
   * The Current byte order.
   */
  private JBBPByteOrder byteOrder;

  /**
   * The Current first value at line prefix.
   */
  private String prefixFirstValueAtLine;

  /**
   * The Current value prefix.
   */
  private String prefixValue;

  /**
   * The Current value postfix.
   */
  private String postfixValue = DEFAULT_VALUE_POSTFIX;

  /**
   * The Current value delimiter.
   */
  private String valueSeparator;

  /**
   * The Current comment prefix.
   */
  private String prefixComment;

  /**
   * The Current HR prefix.
   */
  private String prefixHR;

  /**
   * The Current line position, 0 is first one.
   */
  private int linePosition = 0;

  /**
   * The Current line number, 0 is the first one.
   */
  private int lineNumber = 0;

  /**
   * Number of space chars representing the tab.
   */
  private int spacesInTab = 4;

  /**
   * The Current indent.
   */
  private int indent = 0;

  /**
   * Counter of printed values on the current line.
   */
  private int valuesLineCounter;

  /**
   * The Max number of values to be presented on single line.
   */
  private int maxValuesPerLine = DEFAULT_MAX_VALUES_PER_LINE;
  /**
   * The Value contains the start of commentaries on the previous line.
   */
  private int prevLineCommentsStartPosition = 0;

  /**
   * The Default constructor. A StringWriter will be used inside.
   */
  public JBBPTextWriter() {
    this(new StringWriter(1024), JBBPByteOrder.BIG_ENDIAN, System.getProperty("line.separator"),
        DEFAULT_RADIX, DEFAULT_VALUE_PREFIX, DEFAULT_FIRST_VALUE_LINE_PREFIX,
        DEFAULT_COMMENT_PREFIX, DEFAULT_HR_PREFIX, DEFAULT_VALUE_DELIMITER);
  }

  /**
   * Constructor to wrap a writer,
   *
   * @param out a writer to be wrapped, must not be null.
   */
  public JBBPTextWriter(final Writer out) {
    this(out, JBBPByteOrder.BIG_ENDIAN, System.getProperty("line.separator"), DEFAULT_RADIX,
        DEFAULT_VALUE_PREFIX, DEFAULT_FIRST_VALUE_LINE_PREFIX, DEFAULT_COMMENT_PREFIX,
        DEFAULT_HR_PREFIX, DEFAULT_VALUE_DELIMITER);
  }

  /**
   * Constructor to wrap a writer with defined byte order.
   *
   * @param out       a writer to be wrapped, must not be null.
   * @param byteOrder a byte order to be used, it must not be null.
   */
  public JBBPTextWriter(final Writer out, final JBBPByteOrder byteOrder) {
    this(out, byteOrder, System.getProperty("line.separator"), DEFAULT_RADIX, DEFAULT_VALUE_PREFIX,
        DEFAULT_FIRST_VALUE_LINE_PREFIX, DEFAULT_COMMENT_PREFIX, DEFAULT_HR_PREFIX,
        DEFAULT_VALUE_DELIMITER);
  }

  /**
   * Constructor.
   *
   * @param out                  a writer to be wrapper, must not be null.
   * @param byteOrder            byte order to be used for converting, must not be null.
   * @param lineSeparator        line separator, must not be null.
   * @param radix                radix, must be 2..36.
   * @param valuePrefix          prefix before each value, can be null.
   * @param startValueLinePrefix prefix before the first value on line, can be
   *                             null.
   * @param commentPrefix        prefix before comments, can be null.
   * @param hrPrefix             prefix for horizontal rule
   * @param valueDelimiter       delimiter between values, can be null
   */
  public JBBPTextWriter(
      final Writer out,
      final JBBPByteOrder byteOrder,
      final String lineSeparator,
      final int radix,
      final String valuePrefix,
      final String startValueLinePrefix,
      final String commentPrefix,
      final String hrPrefix,
      final String valueDelimiter) {
    super(out);
    JBBPUtils.assertNotNull(lineSeparator, "Line separator must not be null");

    this.flagCommentsAllowed = true;
    this.prefixHR = hrPrefix == null ? "" : hrPrefix;
    this.lineSeparator = lineSeparator;

    JBBPUtils.assertNotNull(out, "Writer must not be null");
    ByteOrder(byteOrder);
    SetValueSeparator(valueDelimiter);
    SetValueLinePrefix(startValueLinePrefix);
    SetCommentPrefix(commentPrefix);
    SetValuePrefix(valuePrefix);
    Radix(radix);
  }

  /**
   * Auxiliary method allows to build writer over StringWriter with system-depended next line and hex radix.
   * The Method allows fast instance create.
   *
   * @return the text writer instance, must not be null
   * @since 1.4.0
   */
  public static JBBPTextWriter makeStrWriter() {
    final String lineSeparator = System.setProperty("line.separator", "\n");
    return new JBBPTextWriter(new StringWriter(), JBBPByteOrder.BIG_ENDIAN, lineSeparator, 16, "0x",
        ".", ";", "~", ",");
  }

  protected static String makeFieldComment(final JBBPAbstractField field) {
    final String path = field.getFieldPath();
    final StringBuilder result = new StringBuilder(128);
    result.append(field.getTypeAsString()).append(' ');
    result.append(Objects.requireNonNullElse(path, "<anonymous>"));
    return result.toString();
  }

  /**
   * Get the wrapped writer.
   *
   * @return the wrapped writer
   */
  public Writer getWrappedWriter() {
    return this.out;
  }

  /**
   * Get the current byte order.
   *
   * @return the current byte order.
   */
  public JBBPByteOrder getByteOrder() {
    return this.byteOrder;
  }

  /**
   * get the current radix.
   *
   * @return the current radix
   */
  public int getRadix() {
    return this.radix;
  }

  /**
   * Get the current line separator.
   *
   * @return the current line separator as string
   */
  public String getLineSeparator() {
    return this.lineSeparator;
  }

  /**
   * Change current mode.
   *
   * @param mode the new mode value
   */
  private void changeMode(final int mode) {
    this.prevMode = this.mode;
    this.mode = mode;
  }

  /**
   * Ensure the value mode.
   *
   * @throws IOException it will be thrown for transport errors
   */
  private void ensureValueMode() throws IOException {
    switch (this.mode) {
      case MODE_START_LINE: {
        changeMode(MODE_VALUES);
        for (final Extra e : extras) {
          e.onBeforeFirstValue(this);
        }
        writeIndent();
        this.write(this.prefixFirstValueAtLine);
      }
      break;
      case MODE_COMMENTS: {
        this.BR();
        writeIndent();
        changeMode(MODE_VALUES);
        for (final Extra e : extras) {
          e.onBeforeFirstValue(this);
        }
        this.write(this.prefixFirstValueAtLine);
      }
      break;
      case MODE_VALUES:
        break;
      default:
        throw new Error("Unexpected state");
    }
  }

  /**
   * Write current indent to line as a bunch of spaces.
   *
   * @throws IOException it will be thrown for transport errors
   */
  private void writeIndent() throws IOException {
    if (this.indent > 0) {
      int i = this.indent;
      while (i > 0) {
        this.write(' ');
        i--;
      }
    }
  }

  /**
   * Ensure the comment mode.
   *
   * @throws IOException it will be thrown for transport errors
   */
  private void ensureCommentMode() throws IOException {
    switch (this.mode) {
      case MODE_START_LINE:
        writeIndent();
        this.prevLineCommentsStartPosition = this.linePosition;
        this.write(this.prefixComment);
        changeMode(MODE_COMMENTS);
        break;
      case MODE_VALUES: {
        this.prevLineCommentsStartPosition = this.linePosition;
        this.write(this.prefixComment);
        changeMode(MODE_COMMENTS);
      }
      break;
      case MODE_COMMENTS: {
        BR();
        writeIndent();
        while (this.linePosition < this.prevLineCommentsStartPosition) {
          this.write(' ');
        }
        this.write(this.prefixComment);
      }
      break;
      default:
        throw new Error("Unexpected state");
    }
  }

  /**
   * Ensure the new line.
   *
   * @throws IOException it will be thrown for transport errors
   */
  private void ensureNewLineMode() throws IOException {
    if (this.mode != MODE_START_LINE) {
      this.write(this.lineSeparator);
      this.mode = MODE_START_LINE;
    }
    valuesLineCounter = 0;
  }

  /**
   * Print a value represented by its string.
   *
   * @param value the value to be printed, must not be null
   * @throws IOException it will be thrown for transport errors
   */
  private void printValueString(final String value) throws IOException {
    if (this.valuesLineCounter > 0 && !this.valueSeparator.isEmpty()) {
      this.write(this.valueSeparator);
    }

    if (!this.prefixValue.isEmpty()) {
      this.write(this.prefixValue);
    }

    this.write(value);

    if (!this.postfixValue.isEmpty()) {
      this.write(this.postfixValue);
    }
    this.valuesLineCounter++;

    if (this.maxValuesPerLine > 0 && this.valuesLineCounter >= this.maxValuesPerLine) {
      for (final Extra e : this.extras) {
        e.onReachedMaxValueNumberForLine(this);
      }
      ensureNewLineMode();
    }
  }

  /**
   * Enable print comments.
   *
   * @return the context
   */
  public JBBPTextWriter EnableComments() {
    this.flagCommentsAllowed = true;
    return this;
  }

  /**
   * Disable print comments.
   *
   * @return the context
   */
  public JBBPTextWriter DisableComments() {
    this.flagCommentsAllowed = false;
    return this;
  }

  /**
   * Print string values.
   *
   * @param str array of string values, must not be null but may contain nulls
   * @return the context
   * @throws IOException it will be thrown for error
   */
  public JBBPTextWriter Str(final String... str) throws IOException {
    JBBPUtils.assertNotNull(str, "String must not be null");

    final String oldPrefix = this.prefixValue;
    final String oldPostfix = this.postfixValue;
    this.prefixValue = "";
    this.postfixValue = "";

    for (final String s : str) {
      ensureValueMode();
      printValueString(s == null ? "<NULL>" : s);
    }

    this.prefixValue = oldPrefix;
    this.postfixValue = oldPostfix;
    return this;
  }

  /**
   * Print byte value.
   *
   * @param value a byte value
   * @return the context
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPTextWriter Byte(final int value) throws IOException {
    ensureValueMode();

    String convertedByExtras = null;
    for (final Extra e : this.extras) {
      convertedByExtras = e.doConvertByteToStr(this, value & 0xFF);
      if (convertedByExtras != null) {
        break;
      }
    }

    printValueString(Objects.requireNonNullElseGet(convertedByExtras, () -> JBBPUtils
        .ensureMinTextLength(JBBPUtils.ulong2str(value & 0xFF, this.radix, CHAR_BUFFER),
            this.maxCharsRadixForByte, '0', 0)));

    return this;
  }

  /**
   * Print byte array defined as string.
   *
   * @param value string which codes should be printed as byte array.
   * @return the context
   * @throws IOException it will be thrown for transport error
   */
  public JBBPTextWriter Byte(final String value) throws IOException {
    for (int i = 0; i < value.length(); i++) {
      this.Byte(value.charAt(i));
    }
    return this;
  }

  /**
   * Print byte array.
   *
   * @param values a byte array, must not be null
   * @return the context
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPTextWriter Byte(final byte[] values) throws IOException {
    return Byte(values, 0, values.length);
  }

  /**
   * Print values from byte array.
   *
   * @param array source byte array, must not be null
   * @param off   the offset of the first element in array
   * @param len   number of bytes to be printed
   * @return the context
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPTextWriter Byte(final byte[] array, int off, int len) throws IOException {
    ensureValueMode();
    while (len-- > 0) {
      Byte(array[off++]);
    }

    return this;
  }

  /**
   * Check that line start mode is active.
   *
   * @return true if mode is line start, false otherwise
   */
  public boolean isLineStart() {
    return this.mode == MODE_START_LINE;
  }

  /**
   * Check that comment mode is active.
   *
   * @return true if comment mode is active, false otherwise
   */
  public boolean isComments() {
    return this.mode == MODE_COMMENTS;
  }

  /**
   * Check that value mode is active.
   *
   * @return true if value mode is active, false otherwise
   */
  public boolean isValues() {
    return this.mode == MODE_VALUES;
  }

  /**
   * Set prefix to be printed before start of values on every line.
   *
   * @param text string to be used as value line prefix, can be null
   * @return the context
   */
  public final JBBPTextWriter SetValueLinePrefix(final String text) {
    this.prefixFirstValueAtLine = text == null ? "" : text;
    return this;
  }

  /**
   * Set prefix to be printed before every value.
   *
   * @param text string to be used as value prefix, can be null
   * @return the context
   */
  public final JBBPTextWriter SetValuePrefix(final String text) {
    this.prefixValue = text == null ? "" : text;
    return this;
  }

  /**
   * Set postfix to be printed after every value.
   *
   * @param text string to be used as value postfix, can be null
   * @return the context
   */
  public final JBBPTextWriter SetValuePostfix(final String text) {
    this.postfixValue = text == null ? "" : text;
    return this;
  }

  /**
   * Set prefix to be printed as comment start.
   *
   * @param text string to be used as comment start, can be null
   * @return the context
   */
  public final JBBPTextWriter SetCommentPrefix(final String text) {
    this.prefixComment = text == null ? "" : text;
    return this;
  }

  /**
   * Set delimiter to be printed between values.
   *
   * @param text string to be used as separator between values.
   * @return the context
   */
  public final JBBPTextWriter SetValueSeparator(final String text) {
    this.valueSeparator = text == null ? "" : text;
    return this;
  }

  /**
   * Add extras to context.
   *
   * @param extras extras to be added to context, must not be null
   * @return the context
   */
  public JBBPTextWriter AddExtras(final Extra... extras) {
    JBBPUtils.assertNotNull(extras, "Extras must not be null");
    for (final Extra e : extras) {
      JBBPUtils.assertNotNull(e, "Extras must not be null");
      this.extras.add(0, e);
    }
    return this;
  }

  /**
   * Remove extras from context
   *
   * @param extras extras to be removed, must not be null
   * @return the context
   */
  public JBBPTextWriter DelExtras(final Extra... extras) {
    JBBPUtils.assertNotNull(extras, "Extras must not be null");
    for (final Extra e : extras) {
      JBBPUtils.assertNotNull(e, "Extras must not be null");
      this.extras.remove(e);
    }
    return this;
  }

  /**
   * Set max number of values to be printed in one line.
   *
   * @param value max number of values to be presented on line, 0 or negative to
   *              turn off checking
   * @return the context
   */
  public JBBPTextWriter SetMaxValuesPerLine(final int value) {
    this.maxValuesPerLine = value;
    return this;
  }

  /**
   * Set number of spaces per tab.
   *
   * @param numberOfSpacesPerTab number of spaces, must be greater than zero
   * @return the context
   */
  public JBBPTextWriter SetTabSpaces(final int numberOfSpacesPerTab) {
    if (numberOfSpacesPerTab <= 0) {
      throw new IllegalArgumentException(
          "Tab must contains positive number of space chars [" + numberOfSpacesPerTab + ']');
    }
    final int currentIdentSteps = this.indent / this.spacesInTab;
    this.spacesInTab = numberOfSpacesPerTab;
    this.indent = currentIdentSteps * this.spacesInTab;
    return this;
  }

  /**
   * Set byte order.
   *
   * @param order new byte order, must not be null
   * @return the context
   */
  public final JBBPTextWriter ByteOrder(final JBBPByteOrder order) {
    JBBPUtils.assertNotNull(order, "Byte order must not be null");
    this.byteOrder = order;
    return this;
  }

  /**
   * Set radix.
   *
   * @param radix new radix value, must be 2..36
   * @return the context
   */
  public final JBBPTextWriter Radix(final int radix) {
    if (radix < 2 || radix > 36) {
      throw new IllegalArgumentException("Unsupported radix value [" + radix + ']');
    }
    this.radix = radix;
    this.maxCharsRadixForByte = JBBPUtils.ulong2str(0xFFL, this.radix, CHAR_BUFFER).length();
    this.maxCharsRadixForShort = JBBPUtils.ulong2str(0xFFFFL, this.radix, CHAR_BUFFER).length();
    this.maxCharsRadixForInt = JBBPUtils.ulong2str(0xFFFFFFFFL, this.radix, CHAR_BUFFER).length();
    this.maxCharsRadixForLong =
        JBBPUtils.ulong2str(0xFFFFFFFFFFFFFFFFL, this.radix, CHAR_BUFFER).length();

    return this;
  }

  /**
   * Print short value.
   *
   * @param value short value to be printed
   * @return the context
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPTextWriter Short(final int value) throws IOException {
    ensureValueMode();
    String convertedByExtras = null;
    for (final Extra e : this.extras) {
      convertedByExtras = e.doConvertShortToStr(this, value & 0xFFFF);
      if (convertedByExtras != null) {
        break;
      }
    }

    if (convertedByExtras == null) {
      final long valueToWrite;
      if (this.byteOrder == JBBPByteOrder.LITTLE_ENDIAN) {
        valueToWrite = JBBPUtils.reverseByteOrder(value, 2);
      } else {
        valueToWrite = value;
      }
      printValueString(JBBPUtils
          .ensureMinTextLength(JBBPUtils.ulong2str(valueToWrite & 0xFFFFL, this.radix, CHAR_BUFFER),
              this.maxCharsRadixForShort, '0', 0));
    } else {
      printValueString(convertedByExtras);
    }
    return this;
  }

  /**
   * Print float value.
   *
   * @param value float value to be printed
   * @return the context
   * @throws IOException it will be thrown for transport errors
   * @since 1.4.0
   */
  public JBBPTextWriter Float(final float value) throws IOException {
    ensureValueMode();
    String convertedByExtras = null;
    for (final Extra e : this.extras) {
      convertedByExtras = e.doConvertFloatToStr(this, value);
      if (convertedByExtras != null) {
        break;
      }
    }

    if (convertedByExtras == null) {
      final float valueToWrite;
      if (this.byteOrder == JBBPByteOrder.LITTLE_ENDIAN) {
        valueToWrite =
            Float.intBitsToFloat((int) JBBPFieldInt.reverseBits(Float.floatToIntBits(value)));
      } else {
        valueToWrite = value;
      }
      printValueString(JBBPUtils.ensureMinTextLength(JBBPUtils.float2str(valueToWrite, this.radix),
          this.maxCharsRadixForShort, '0', 0));
    } else {
      printValueString(convertedByExtras);
    }
    return this;
  }

  /**
   * Print double value.
   *
   * @param value double value to be printed
   * @return the context
   * @throws IOException it will be thrown for transport errors
   * @since 1.4.0
   */
  public JBBPTextWriter Double(final double value) throws IOException {
    ensureValueMode();
    String convertedByExtras = null;
    for (final Extra e : this.extras) {
      convertedByExtras = e.doConvertDoubleToStr(this, value);
      if (convertedByExtras != null) {
        break;
      }
    }

    if (convertedByExtras == null) {
      final double valueToWrite;
      if (this.byteOrder == JBBPByteOrder.LITTLE_ENDIAN) {
        valueToWrite =
            Double.longBitsToDouble(JBBPFieldLong.reverseBits(Double.doubleToLongBits(value)));
      } else {
        valueToWrite = value;
      }
      printValueString(JBBPUtils.ensureMinTextLength(JBBPUtils.double2str(valueToWrite, this.radix),
          this.maxCharsRadixForShort, '0', 0));
    } else {
      printValueString(convertedByExtras);
    }
    return this;
  }

  /**
   * Print char codes of string as short array.
   *
   * @param value string which codes should be printed, must not be null
   * @return the context
   * @throws IOException it will be thrown if any transport error
   */
  public JBBPTextWriter Short(final String value) throws IOException {
    for (int i = 0; i < value.length(); i++) {
      this.Short(value.charAt(i));
    }
    return this;
  }

  /**
   * Print array of short values.
   *
   * @param values array of short values, must not be null
   * @return the context
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPTextWriter Short(final short[] values) throws IOException {
    return this.Short(values, 0, values.length);
  }

  /**
   * Print array of float values.
   *
   * @param values array of float values, must not be null
   * @return the context
   * @throws IOException it will be thrown for transport errors
   * @since 1.4.0
   */
  public JBBPTextWriter Float(final float[] values) throws IOException {
    return this.Float(values, 0, values.length);
  }

  /**
   * Print array of double values.
   *
   * @param values array of double values, must not be null
   * @return the context
   * @throws IOException it will be thrown for transport errors
   * @since 1.4.0
   */
  public JBBPTextWriter Double(final double[] values) throws IOException {
    return this.Double(values, 0, values.length);
  }

  /**
   * Print values from short array
   *
   * @param values short value array, must not be null
   * @param off    offset to the first element
   * @param len    number of elements to print
   * @return the context
   * @throws IOException it will be thrown for transport error
   */
  public JBBPTextWriter Short(final short[] values, int off, int len) throws IOException {
    while (len-- > 0) {
      this.Short(values[off++]);
    }
    return this;
  }

  /**
   * Print values from float array
   *
   * @param values float value array, must not be null
   * @param off    offset to the first element
   * @param len    number of elements to print
   * @return the context
   * @throws IOException it will be thrown for transport error
   * @since 1.4.0
   */
  public JBBPTextWriter Float(final float[] values, int off, int len) throws IOException {
    while (len-- > 0) {
      this.Float(values[off++]);
    }
    return this;
  }

  /**
   * Print values from double array
   *
   * @param values double value array, must not be null
   * @param off    offset to the first element
   * @param len    number of elements to print
   * @return the context
   * @throws IOException it will be thrown for transport error
   * @since 1.4.0
   */
  public JBBPTextWriter Double(final double[] values, int off, int len) throws IOException {
    while (len-- > 0) {
      this.Double(values[off++]);
    }
    return this;
  }

  /**
   * Print integer value
   *
   * @param value value to be printed
   * @return the context
   * @throws IOException it will be thrown for transport error
   */
  public JBBPTextWriter Int(final int value) throws IOException {
    ensureValueMode();

    String convertedByExtras = null;
    for (final Extra e : this.extras) {
      convertedByExtras = e.doConvertIntToStr(this, value);
      if (convertedByExtras != null) {
        break;
      }
    }

    if (convertedByExtras == null) {
      final long valueToWrite;
      if (this.byteOrder == JBBPByteOrder.LITTLE_ENDIAN) {
        valueToWrite = JBBPUtils.reverseByteOrder(value, 4);
      } else {
        valueToWrite = value;
      }
      printValueString(JBBPUtils.ensureMinTextLength(
          JBBPUtils.ulong2str(valueToWrite & 0xFFFFFFFFL, this.radix, CHAR_BUFFER),
          this.maxCharsRadixForInt, '0', 0));
    } else {
      printValueString(convertedByExtras);
    }
    return this;
  }

  /**
   * Print unsigned integer value
   *
   * @param value value to be printed
   * @return the context
   * @throws IOException it will be thrown for transport error
   * @since 2.0.4
   */
  public JBBPTextWriter UInt(final int value) throws IOException {
    ensureValueMode();

    String convertedByExtras = null;
    for (final Extra e : this.extras) {
      convertedByExtras = e.doConvertUIntToStr(this, value);
      if (convertedByExtras != null) {
        break;
      }
    }

    if (convertedByExtras == null) {
      final long valueToWrite;
      if (this.byteOrder == JBBPByteOrder.LITTLE_ENDIAN) {
        valueToWrite = JBBPUtils.reverseByteOrder(value, 4);
      } else {
        valueToWrite = value;
      }
      printValueString(JBBPUtils.ensureMinTextLength(
          JBBPUtils.ulong2str(valueToWrite & 0xFFFFFFFFL, this.radix, CHAR_BUFFER),
          this.maxCharsRadixForInt, '0', 0));
    } else {
      printValueString(convertedByExtras);
    }
    return this;
  }

  /**
   * Print integer array.
   *
   * @param values integer array to be printed, must not be null
   * @return the context
   * @throws IOException it will be thrown for transport error
   */
  public JBBPTextWriter Int(final int[] values) throws IOException {
    return this.Int(values, 0, values.length);
  }

  /**
   * Print unsigned integer array.
   *
   * @param values unsigned integer array to be printed, must not be null
   * @return the context
   * @throws IOException it will be thrown for transport error
   * @since 2.0.4
   */
  public JBBPTextWriter UInt(final int[] values) throws IOException {
    return this.UInt(values, 0, values.length);
  }

  /**
   * Print values from integer array.
   *
   * @param values integer array, must not be null
   * @param off    offset to the first element in array
   * @param len    number of elements to print
   * @return the context
   * @throws IOException it will be thrown for transport error
   */
  public JBBPTextWriter Int(final int[] values, int off, int len) throws IOException {
    while (len-- > 0) {
      this.Int(values[off++]);
    }
    return this;
  }

  /**
   * Print values from unsigned integer array.
   *
   * @param values unsigned integer array, must not be null
   * @param off    offset to the first element in array
   * @param len    number of elements to print
   * @return the context
   * @throws IOException it will be thrown for transport error
   * @since 2.0.4
   */
  public JBBPTextWriter UInt(final int[] values, int off, int len) throws IOException {
    while (len-- > 0) {
      this.UInt(values[off++]);
    }
    return this;
  }

  /**
   * Increase indent.
   *
   * @return the context
   */
  public JBBPTextWriter IndentInc() {
    this.indent += this.spacesInTab;
    return this;
  }

  /**
   * Increase indent.
   *
   * @param count number of indents
   * @return the context
   * @throws IOException it will be thrown for transport error
   */
  public JBBPTextWriter IndentInc(final int count) throws IOException {
    for (int i = 0; i < count; i++) {
      IndentInc();
    }
    return this;
  }

  /**
   * Decrease indent.
   *
   * @return the context
   */
  public JBBPTextWriter IndentDec() {
    if (this.indent > 0) {
      this.indent = Math.max(0, this.indent - this.spacesInTab);
    }
    return this;
  }

  /**
   * Decrease indent.
   *
   * @param count number of indents
   * @return the context
   * @throws IOException it will be thrown for transport error
   */
  public JBBPTextWriter IndentDec(final int count) throws IOException {
    for (int i = 0; i < count; i++) {
      IndentDec();
    }
    return this;
  }

  /**
   * Print long value
   *
   * @param value value to be printed
   * @return the context
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPTextWriter Long(final long value) throws IOException {
    ensureValueMode();

    String convertedByExtras = null;
    for (final Extra e : this.extras) {
      convertedByExtras = e.doConvertLongToStr(this, value);
      if (convertedByExtras != null) {
        break;
      }
    }

    if (convertedByExtras == null) {
      final long valueToWrite;
      if (this.byteOrder == JBBPByteOrder.LITTLE_ENDIAN) {
        valueToWrite = JBBPUtils.reverseByteOrder(value, 8);
      } else {
        valueToWrite = value;
      }
      printValueString(JBBPUtils
          .ensureMinTextLength(JBBPUtils.ulong2str(valueToWrite, this.radix, CHAR_BUFFER),
              this.maxCharsRadixForLong, '0', 0));
    } else {
      printValueString(convertedByExtras);
    }
    return this;
  }

  /**
   * Print values from long array.
   *
   * @param values array to be printed, must not be null
   * @return the context
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPTextWriter Long(final long[] values) throws IOException {
    return this.Long(values, 0, values.length);
  }

  /**
   * print values from long array.
   *
   * @param values array to be printed, must not be null
   * @param off    offset to the first element to print
   * @param len    number of elements to be printed
   * @return the context
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPTextWriter Long(final long[] values, int off, int len) throws IOException {
    while (len-- > 0) {
      this.Long(values[off++]);
    }
    return this;
  }

  /**
   * Make new line.
   *
   * @return the context
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPTextWriter BR() throws IOException {
    this.write(this.lineSeparator);
    ensureNewLineMode();
    return this;
  }

  /**
   * Change parameters for horizontal rule.
   *
   * @param prefix the prefix to be printed before rule, it can be null
   * @param length the length in symbols.
   * @param ch     symbol to draw
   * @return the context
   */
  public JBBPTextWriter SetHR(final String prefix, final int length, final char ch) {
    this.prefixHR = prefix == null ? "" : prefix;
    this.hrChar = ch;
    this.hrLength = length;
    return this;
  }

  /**
   * Print horizontal rule. If comments are disabled then only next line will be
   * added.
   *
   * @return the context
   * @throws IOException it will be thrown for transport errors
   * @see #EnableComments()
   * @see #DisableComments()
   */
  public JBBPTextWriter HR() throws IOException {
    if (this.flagCommentsAllowed) {
      this.ensureNewLineMode();
      this.writeIndent();
      this.write(this.prefixHR);
      for (int i = 0; i < this.hrLength; i++) {
        this.write(this.hrChar);
      }
    }
    this.BR();
    return this;
  }

  /**
   * Print comments. Wilt aligning of line start for multi-line comment.
   * Comments will be printed only if they are allowed.
   *
   * @param comment array of string to be printed as comment lines.
   * @return the context
   * @throws IOException it will be thrown for transport errors
   * @see #EnableComments()
   * @see #DisableComments()
   */
  public JBBPTextWriter Comment(final String... comment) throws IOException {
    if (this.flagCommentsAllowed) {
      if (comment != null) {
        for (final String c : comment) {
          if (c == null) {
            continue;
          }

          if (c.indexOf('\n') >= 0) {
            final String[] split = c.split("\\n", -1);
            for (final String s : split) {
              this.ensureCommentMode();
              this.write(s);
            }
          } else {
            this.ensureCommentMode();
            this.write(c);
          }
        }
        this.prevLineCommentsStartPosition = 0;
      }
    } else {
      ensureNewLineMode();
    }
    return this;
  }

  @Override
  public String toString() {
    final String result;
    if (this.out instanceof StringWriter) {
      result = this.out.toString();
    } else {
      result = JBBPTextWriter.class.getName() + '(' + this.out.getClass().getName() + ")@" +
          System.identityHashCode(this);
    }
    return result;
  }

  /**
   * Print objects.
   *
   * @param objId object id which will be provided to a converter as extra info
   * @param obj   objects to be converted and printed, must not be null
   * @return the context
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPTextWriter Obj(final int objId, final Object... obj) throws IOException {
    if (this.extras.isEmpty()) {
      throw new IllegalStateException("There is not any registered extras");
    }

    for (final Object c : obj) {
      String str = null;
      for (final Extra e : this.extras) {
        str = e.doConvertObjToStr(this, objId, c);
        if (str != null) {
          break;
        }
      }

      if (str != null) {
        ensureValueMode();
        printValueString(str);
      }
    }
    return this;
  }

  /**
   * Print objects from array.
   *
   * @param objId object id which will be provided to a converter as extra info
   * @param array array of objects, must not be null
   * @param off   offset to the first element to be printed
   * @param len   number of elements to be printed
   * @return the context
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPTextWriter Obj(final int objId, final Object[] array, int off, int len)
      throws IOException {
    while (len-- > 0) {
      this.Obj(objId, array[off++]);
    }
    return this;
  }

  /**
   * Print objects which marked by Bin annotation or successors of JBBPAbstractField.
   * <b>NB! Keep in mind that values of fields will be processed for their attributes before printing
   * and for instance a bit field with inversion will be shown as inverted one.</b>
   *
   * @param objs array of object marked by Bin annotation or successors of
   *             JBBPAbstractField
   * @return the context
   * @throws IOException it will be thrown if transport errors
   */
  public JBBPTextWriter Bin(final Object... objs) throws IOException {
    if (this.mappedClassObserver == null) {
      this.mappedClassObserver = new MappedObjectLogger();
    }

    ensureNewLineMode();

    for (final Object obj : objs) {
      if (obj == null) {
        write("<NULL>");
      } else {
        if (obj instanceof JBBPAbstractField) {
          printAbstractFieldObject(null, (JBBPAbstractField) obj);
        } else {
          this.mappedClassObserver.init();
          this.mappedClassObserver.processObject(obj);
        }
      }
    }

    return this;
  }

  protected void printAbstractFieldObject(final String postText, final JBBPAbstractField field)
      throws IOException {
    final String postfix = (postText == null ? "" : " " + postText);

    if (field instanceof JBBPAbstractArrayField || field instanceof JBBPFieldStruct) {
      HR();
      Comment(" Start " + makeFieldComment(field) + postfix);
      HR();
      IndentInc();
      if (field instanceof JBBPAbstractArrayField) {
        final JBBPAbstractArrayField<? extends JBBPAbstractField> array =
            (JBBPAbstractArrayField<? extends JBBPAbstractField>) field;
        if (array.size() > 0) {
          if (array instanceof JBBPFieldArrayBit) {
            Byte(((JBBPFieldArrayBit) array).getArray());
          } else if (array instanceof JBBPFieldArrayBoolean) {
            final boolean[] boolArray = ((JBBPFieldArrayBoolean) array).getArray();
            final String[] arrayToPrint = new String[boolArray.length];
            for (int i = 0; i < boolArray.length; i++) {
              arrayToPrint[i] = boolArray[i] ? "T" : "F";
            }
            Str(arrayToPrint);
          } else if (array instanceof JBBPFieldArrayByte) {
            Byte(((JBBPFieldArrayByte) array).getArray());
          } else if (array instanceof JBBPFieldArrayInt) {
            Int(((JBBPFieldArrayInt) array).getArray());
          } else if (array instanceof JBBPFieldArrayUInt) {
            UInt(((JBBPFieldArrayUInt) array).getInternalArray());
          } else if (array instanceof JBBPFieldArrayLong) {
            Long(((JBBPFieldArrayLong) array).getArray());
          } else if (array instanceof JBBPFieldArrayShort) {
            Short(((JBBPFieldArrayShort) array).getArray());
          } else if (array instanceof JBBPFieldArrayStruct) {
            final JBBPFieldArrayStruct structArray = (JBBPFieldArrayStruct) array;
            int index = 0;
            for (final JBBPFieldStruct s : structArray.getArray()) {
              printAbstractFieldObject('[' + Integer.toString(index++) + ']', s);
            }
          } else if (array instanceof JBBPFieldArrayUByte) {
            Byte(((JBBPFieldArrayUByte) array).getArray());
          } else if (array instanceof JBBPFieldArrayUShort) {
            Short(((JBBPFieldArrayUShort) array).getArray());
          } else if (array instanceof JBBPFieldArrayFloat) {
            Float(((JBBPFieldArrayFloat) array).getArray());
          } else if (array instanceof JBBPFieldArrayDouble) {
            Double(((JBBPFieldArrayDouble) array).getArray());
          } else if (array instanceof JBBPFieldArrayString) {
            final String[] strArray = ((JBBPFieldArrayString) array).getArray();
            final String[] arrayToPrint = new String[strArray.length];
            for (int i = 0; i < strArray.length; i++) {
              arrayToPrint[i] = strArray[i] == null ? "<NULL>" : '\"' + strArray[i] + '\"';
            }
            Str(arrayToPrint);
          } else {
            throw new Error("Unexpected field [" + field.getClass() + ']');
          }
        }
      } else {
        final JBBPFieldStruct struct = (JBBPFieldStruct) field;
        for (final JBBPAbstractField f : struct.getArray()) {
          printAbstractFieldObject(null, f);
        }
      }
      IndentDec();
      HR();
      Comment(" End " + makeFieldComment(field) + postfix);
      HR();
    } else {
      if (field instanceof JBBPNumericField) {
        final JBBPNumericField numeric = (JBBPNumericField) field;
        if (numeric instanceof JBBPFieldBit) {
          Byte(numeric.getAsInt());
        } else if (numeric instanceof JBBPFieldBoolean) {
          Str(numeric.getAsBool() ? "T" : "F");
        } else if (numeric instanceof JBBPFieldByte) {
          Byte(numeric.getAsInt());
        } else if (numeric instanceof JBBPFieldInt) {
          Int(numeric.getAsInt());
        } else if (numeric instanceof JBBPFieldUInt) {
          UInt(numeric.getAsInt());
        } else if (numeric instanceof JBBPFieldLong) {
          Long(numeric.getAsLong());
        } else if (numeric instanceof JBBPFieldShort) {
          Short(numeric.getAsInt());
        } else if (numeric instanceof JBBPFieldUByte) {
          Byte(numeric.getAsInt());
        } else if (numeric instanceof JBBPFieldUShort) {
          Short(numeric.getAsInt());
        } else if (numeric instanceof JBBPFieldFloat) {
          Float(numeric.getAsFloat());
        } else if (numeric instanceof JBBPFieldDouble) {
          Double(numeric.getAsDouble());
        } else {
          throw new Error("Unexpected field [" + field.getClass() + ']');
        }
        Comment(" " + makeFieldComment(field) + postfix);
      } else if (field instanceof JBBPFieldString) {
        final String value = ((JBBPFieldString) field).getAsString();
        Str(value == null ? "<NULL>" : '\"' + value + '\"');
        Comment(" " + makeFieldComment(field) + postfix);
      } else {
        throw new Error("Unexpected field [" + field.getClass() + ']');
      }
    }
  }

  /**
   * Close the wrapped writer.
   *
   * @return the context
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPTextWriter Close() throws IOException {
    for (final Extra e : extras) {
      e.onClose(this);
    }
    super.close();
    return this;
  }

  @Override
  public void close() throws IOException {
    this.Close();
  }

  /**
   * Flush buffers in wrapped writer.
   *
   * @return the context
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPTextWriter Flush() throws IOException {
    super.flush();
    return this;
  }

  @Override
  public void flush() throws IOException {
    this.Flush();
  }

  /**
   * Print tab as space chars.
   *
   * @return the context
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPTextWriter Tab() throws IOException {
    this.Space(this.spacesInTab - (this.linePosition % this.spacesInTab));
    return this;
  }

  /**
   * Print number of spaces.
   *
   * @param numberOfSpaces number of spaces to print
   * @return the context
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPTextWriter Space(final int numberOfSpaces) throws IOException {
    for (int i = 0; i < numberOfSpaces; i++) {
      writeChar(' ');
    }
    return this;
  }

  /**
   * Main method writing a char into wrapped writer.
   *
   * @param chr a char to be written.
   * @throws IOException it will be thrown for transport errors.
   */
  private void writeChar(final char chr) throws IOException {
    switch (chr) {
      case '\t':
        this.Tab();
        break;
      case '\n': {
        this.out.write(this.lineSeparator);
        this.lineNumber++;
        this.prevMode = this.mode;
        this.mode = MODE_START_LINE;
        this.linePosition = 0;
        for (final Extra e : extras) {
          e.onNewLine(this, this.lineNumber);
        }
      }
      break;
      case '\r':
        break;
      default: {
        if (!Character.isISOControl(chr)) {
          this.out.write(chr);
          this.linePosition++;
          if (this.mode == MODE_START_LINE) {
            this.mode = this.prevMode;
          }
        }
      }
      break;
    }
  }

  /**
   * Get the current line number.
   *
   * @return the current line number, the first one is zero
   */
  public int getLine() {
    return this.lineNumber;
  }

  /**
   * Get the current line position.
   *
   * @return the current line position, the first one is zero
   */
  public int getLinePosition() {
    return this.linePosition;
  }

  @Override
  public void write(final String str) throws IOException {
    this.write(str, 0, str.length());
  }

  @Override
  public void write(final char[] cbuf) throws IOException {
    this.write(cbuf, 0, cbuf.length);
  }

  @Override
  public void write(final String str, int off, int len) throws IOException {
    while (len-- > 0) {
      this.writeChar(str.charAt(off++));
    }
  }

  @Override
  public void write(final char[] cbuf, int off, int len) throws IOException {
    while (len-- > 0) {
      this.writeChar(cbuf[off++]);
    }
  }

  @Override
  public void write(final int c) throws IOException {
    this.writeChar((char) c);
  }

  @Override
  public Writer append(final char c) throws IOException {
    write(c);
    return this;
  }

  @Override
  public Writer append(final CharSequence csq, final int start, final int end) throws IOException {
    final CharSequence cs = (csq == null ? "null" : csq);
    this.write(cs.subSequence(start, end).toString());
    return this;
  }

  @Override
  public Writer append(final CharSequence csq) throws IOException {
    if (csq == null) {
      this.write("null");
    } else {
      this.write(csq.toString());
    }
    return this;
  }

  /**
   * The Interface describes some extras for the writer which can make
   * extra-work.
   */
  public interface Extra {

    /**
     * Notification about the start new line.
     *
     * @param context    the context, must not be null
     * @param lineNumber the current line number (0 is the first one)
     * @throws IOException it can be thrown for transport error
     */
    void onNewLine(JBBPTextWriter context, int lineNumber) throws IOException;

    /**
     * Notification about print of the first value on the line.
     *
     * @param context the context, must not be null
     * @throws IOException it can be thrown for transport error
     */
    void onBeforeFirstValue(JBBPTextWriter context) throws IOException;

    /**
     * Notification that reached defined maximal number of values per string
     * line.
     *
     * @param context the context, must not be null
     * @throws IOException it can be thrown for transport error
     */
    void onReachedMaxValueNumberForLine(JBBPTextWriter context) throws IOException;

    /**
     * Notification about close
     *
     * @param context the context, must not be null
     * @throws IOException it can be thrown for transport error
     */
    void onClose(JBBPTextWriter context) throws IOException;

    /**
     * Convert byte to string representation.
     *
     * @param context the context, must not be null
     * @param value   the unsigned byte value to be converted
     * @return string representation of the byte value, must not return null
     * @throws IOException it can be thrown for transport error
     */
    String doConvertByteToStr(JBBPTextWriter context, int value) throws IOException;

    /**
     * Convert short to string representation.
     *
     * @param context the context, must not be null
     * @param value   the unsigned short value to be converted
     * @return string representation of the short value, must not return null
     * @throws IOException it can be thrown for transport error
     */
    String doConvertShortToStr(JBBPTextWriter context, int value) throws IOException;

    /**
     * Convert integer value to string representation.
     *
     * @param context the context, must not be null
     * @param value   the integer value to be converted
     * @return string representation of the integer value, must not return null
     * @throws IOException it can be thrown for transport error
     */
    String doConvertIntToStr(JBBPTextWriter context, int value) throws IOException;

    /**
     * Convert unsigned integer value to string representation.
     *
     * @param context the context, must not be null
     * @param value   the unsigned integer value to be converted
     * @return string representation of the integer value, must not return null
     * @throws IOException it can be thrown for transport error
     * @since 2.0.4
     */
    String doConvertUIntToStr(JBBPTextWriter context, int value) throws IOException;

    /**
     * Convert float value to string representation.
     *
     * @param context the context, must not be null
     * @param value   the float value to be converted
     * @return string representation of the float value, must not return null
     * @throws IOException it can be thrown for transport error
     * @since 1.4.0
     */
    String doConvertFloatToStr(JBBPTextWriter context, float value) throws IOException;

    /**
     * Convert double value to string representation.
     *
     * @param context the context, must not be null
     * @param value   the double value to be converted
     * @return string representation of the double value, must not return null
     * @throws IOException it can be thrown for transport error
     * @since 1.4.0
     */
    String doConvertDoubleToStr(JBBPTextWriter context, double value) throws IOException;

    /**
     * Convert long value to string representation.
     *
     * @param context the context, must not be null
     * @param value   the long value to be converted
     * @return string representation of the long value, must not return null
     * @throws IOException it can be thrown for transport error
     */
    String doConvertLongToStr(JBBPTextWriter context, long value) throws IOException;

    /**
     * Convert an object to its string representation.
     *
     * @param context the context, must not be null
     * @param id      an optional object id
     * @param obj     an object to be converted into string, must not be null
     * @return string representation of the object
     * @throws IOException it can be thrown for transport error
     */
    String doConvertObjToStr(JBBPTextWriter context, int id, Object obj) throws IOException;

    /**
     * Convert a custom field into string.
     *
     * @param context    the context, must not be null
     * @param obj        an object instance which field must be converted into string,
     *                   must not be null
     * @param field      the field of the object which must be converted, must not be
     *                   null
     * @param annotation the bin annotation for the field, must not be null
     * @return the text representation of the field or null if to not print
     * anything
     * @throws IOException it will be thrown for transport error
     */
    String doConvertCustomField(JBBPTextWriter context, final Object obj, final Field field,
                                final Bin annotation) throws IOException;
  }

  private final class MappedObjectLogger extends AbstractMappedClassFieldObserver {

    private final Deque<Integer> counterStack = new ArrayDeque<>();
    private int arrayCounter;

    private void init() {
      arrayCounter = 0;
      counterStack.clear();
    }

    private String makeFieldDescription(final Field field, final Bin annotation) {
      final StringBuilder result = new StringBuilder();
      if (annotation.name().isEmpty()) {
        result.append(field.getName());
      } else {
        result.append(annotation.name());
      }
      if (!annotation.comment().isEmpty()) {
        result.append(", ").append(annotation.comment());
      }
      return result.toString();
    }

    private String makeStructDescription(final Object obj, final Field field,
                                         final Bin annotation) {
      final StringBuilder result = new StringBuilder();

      final Class<?> objClass = obj.getClass();
      Bin classAnno = objClass.getAnnotation(Bin.class);

      if (classAnno != null && classAnno.equals(annotation)) {
        classAnno = null;
      }

      final String typeName;
      if (field == null) {
        typeName = obj.getClass().getSimpleName();
      } else {
        final Class<?> fieldType = field.getType();
        typeName = fieldType.isArray() ? fieldType.getComponentType().getSimpleName() :
            fieldType.getSimpleName();
      }

      final String name =
          annotation == null || annotation.name().isEmpty() ? typeName : annotation.name();
      final String fieldComment =
          annotation == null || annotation.comment().isEmpty() ? null : annotation.comment();
      final String objectComment =
          classAnno == null || classAnno.comment().isEmpty() ? null : classAnno.comment();

      result.append(name);
      if (fieldComment != null) {
        result.append(", ").append(fieldComment);
      }
      if (objectComment != null) {
        if (fieldComment != null) {
          result.append('\n');
        } else {
          result.append(", ");
        }
        result.append(objectComment);
      }

      return result.toString();
    }

    private String makeArrayDescription(final Field field, final Bin annotation) {
      return makeFieldDescription(field, annotation);
    }

    @Override
    protected void onArrayEnd(final Object obj, final Field field, final Bin annotation) {
      try {
        IndentDec();
        HR();
        if (field.getType() == String.class) {
          Comment("END STRING : " + makeArrayDescription(field, annotation));
        } else {
          Comment("END ARRAY : " + makeArrayDescription(field, annotation));
        }
        HR();
      } catch (IOException ex) {
        throw new JBBPIOException("Can't log array field", ex);
      } finally {
        arrayCounter--;
      }
    }

    @Override
    protected void onArrayStart(final Object obj, final Field field, final Bin annotation,
                                final int length) {
      try {
        HR();
        if (field.getType() == String.class) {
          Comment("STRING: " + makeFieldDescription(field, annotation));
        } else {
          Comment("START ARRAY : " + makeArrayDescription(field, annotation) + " OF " +
              field.getType().getComponentType().getSimpleName() + " [" + length + ']');
        }
        HR();
        IndentInc();
      } catch (IOException ex) {
        throw new JBBPIOException("Can't log array field", ex);
      } finally {
        arrayCounter++;
      }
    }

    @Override
    protected void onStructEnd(final Object obj, final Field field, final Bin annotation) {
      try {
        IndentDec();
        HR();
        Comment("END : " + makeStructDescription(obj, field, annotation));
        HR();
        this.arrayCounter = this.counterStack.pop();
      } catch (IOException ex) {
        throw new JBBPIOException("Can't log struct field", ex);
      }
    }

    @Override
    protected void onStructStart(final Object obj, final Field field, final Bin annotation) {
      try {
        this.counterStack.add(this.arrayCounter);
        this.arrayCounter = 0;
        HR();
        Comment("START : " + makeStructDescription(obj, field, annotation));
        HR();
        IndentInc();
      } catch (IOException ex) {
        throw new JBBPIOException("Can't log short field", ex);
      }
    }

    @Override
    protected void onFieldLong(final Object obj, final Field field, final Bin annotation,
                               final long value) {
      try {
        Long(value);
        if (this.arrayCounter == 0) {
          Comment(makeFieldDescription(field, annotation));
        }
      } catch (IOException ex) {
        throw new JBBPIOException("Can't log short field", ex);
      }
    }

    @Override
    protected void onFieldInt(final Object obj, final Field field, final Bin annotation,
                              final int value) {
      try {
        Int(value);
        if (this.arrayCounter == 0) {
          Comment(makeFieldDescription(field, annotation));
        }
      } catch (IOException ex) {
        throw new JBBPIOException("Can't log int field", ex);
      }
    }

    @Override
    protected void onFieldUInt(Object obj, Field field, Bin annotation,
                               int value) {
      try {
        UInt(value);
        if (this.arrayCounter == 0) {
          Comment(makeFieldDescription(field, annotation));
        }
      } catch (IOException ex) {
        throw new JBBPIOException("Can't log uint field", ex);
      }
    }

    @Override
    protected void onFieldFloat(final Object obj, final Field field, final Bin annotation,
                                final float value) {
      try {
        Float(value);
        if (this.arrayCounter == 0) {
          Comment(makeFieldDescription(field, annotation));
        }
      } catch (IOException ex) {
        throw new JBBPIOException("Can't log float field", ex);
      }
    }

    @Override
    protected void onFieldString(final Object obj, final Field field, final Bin annotation,
                                 final String value) {
      try {
        ensureValueMode();
        final String prefix = prefixValue;
        prefixValue = "";
        printValueString(value == null ? "<NULL>" : '\"' + value + '\"');
        prefixValue = prefix;
        if (this.arrayCounter == 0) {
          Comment(makeFieldDescription(field, annotation));
        }
      } catch (IOException ex) {
        throw new JBBPIOException("Can't log string field", ex);
      }
    }

    @Override
    protected void onFieldDouble(final Object obj, final Field field, final Bin annotation,
                                 final double value) {
      try {
        Double(value);
        if (this.arrayCounter == 0) {
          Comment(makeFieldDescription(field, annotation));
        }
      } catch (IOException ex) {
        throw new JBBPIOException("Can't log double field", ex);
      }
    }

    @Override
    protected void onFieldShort(final Object obj, final Field field, final Bin annotation,
                                final boolean signed, final int value) {
      try {
        Short(value);
        if (this.arrayCounter == 0) {
          Comment(makeFieldDescription(field, annotation));
        }
      } catch (IOException ex) {
        throw new JBBPIOException("Can't log short field", ex);
      }
    }

    @Override
    protected void onFieldByte(final Object obj, final Field field, final Bin annotation,
                               final boolean signed, final int value) {
      try {
        Byte(value);
        if (this.arrayCounter == 0) {
          Comment(makeFieldDescription(field, annotation));
        }
      } catch (IOException ex) {
        throw new JBBPIOException("Can't log byte field", ex);
      }
    }

    @Override
    protected void onFieldBool(final Object obj, final Field field, final Bin annotation,
                               final boolean value) {
      try {
        Byte(value ? 0x01 : 0x00);
        if (this.arrayCounter == 0) {
          Comment(makeFieldDescription(field, annotation));
        }
      } catch (IOException ex) {
        throw new JBBPIOException("Can't log boolean field", ex);
      }
    }

    @Override
    protected void onFieldBits(final Object obj, final Field field, final Bin annotation,
                               final JBBPBitNumber bitNumber, final int value) {
      try {
        Byte(value & bitNumber.getMask());
        if (this.arrayCounter == 0) {
          Comment(makeFieldDescription(field, annotation));
        }
      } catch (IOException ex) {
        throw new JBBPIOException("Can't log bit field", ex);
      }
    }

    @Override
    protected void onFieldCustom(final Object obj, final Field field, final Bin annotation,
                                 final Object customFieldProcessor, final Object value) {
      try {
        if (extras.isEmpty()) {
          throw new IllegalStateException("There is not any registered extras");
        }

        String str = null;
        for (final Extra e : extras) {
          str = e.doConvertCustomField(JBBPTextWriter.this, obj, field, annotation);
          if (str != null) {
            break;
          }
        }

        if (str != null) {
          ensureValueMode();
          printValueString(str);
          Comment(makeFieldDescription(field, annotation));
        }
      } catch (IOException ex) {
        throw new JBBPIOException("Can't log custom field", ex);
      }
    }

    public void processObject(final Object obj) {
      super.processObject(obj, null, null, this);
    }
  }

}
