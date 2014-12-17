/*
 * Copyright 2014 Igor Maznitsa.
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

import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * the Writer allows to make text describes some bin data, it supports output of
 * commentaries and values, also it is possible to register own extras to
 * process complex data and cases. The Class is not thread safe.
 *
 * @since 1.1
 */
public class JBBPTextWriter extends FilterWriter {

  /**
   * The INterface describes some extras for the writer which can make
   * extra-work.
   */
  public interface Extra {

    /**
     * Notification about the start new line.
     *
     * @param context the context, must not be null
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
     * @param value the unsigned byte value to be converted
     * @return string representation of the byte value, must not return null
     * @throws IOException it can be thrown for transport error
     */
    String doConvertByteToStr(JBBPTextWriter context, int value) throws IOException;

    /**
     * Convert short to string representation.
     *
     * @param context the context, must not be null
     * @param value the unsigned short value to be converted
     * @return string representation of the short value, must not return null
     * @throws IOException it can be thrown for transport error
     */
    String doConvertShortToStr(JBBPTextWriter context, int value) throws IOException;

    /**
     * Convert integer value to string representation.
     *
     * @param context the context, must not be null
     * @param value the integer value to be converted
     * @return string representation of the integer value, must not return null
     * @throws IOException it can be thrown for transport error
     */
    String doConvertIntToStr(JBBPTextWriter context, int value) throws IOException;

    /**
     * Convert long value to string representation.
     *
     * @param context the context, must not be null
     * @param value the long value to be converted
     * @return string representation of the long value, must not return null
     * @throws IOException it can be thrown for transport error
     */
    String doConvertLongToStr(JBBPTextWriter context, long value) throws IOException;

    /**
     * Convert an object to its string representation.
     *
     * @param context the context, must not be null
     * @param id an optional object id
     * @param obj an object to be converted into string, must not be null
     * @return
     * @throws IOException
     */
    String doConvertObjToStr(JBBPTextWriter context, int id, Object obj) throws IOException;
  }

  /**
   * The Default comment prefix.
   */
  private static final String DEFAULT_COMMENT_PREFIX = ";";
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
  private static final char DEFAUTL_HR_CHAR = '-';

  /**
   * The Line separator, must not be null.
   */
  private final String lineSeparator;

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
  private char hrChar = DEFAUTL_HR_CHAR;

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
   * The Current byte order.
   */
  private JBBPByteOrder byteOrder;

  /**
   * The Current first value at line prefix.
   */
  private String prefixFirtValueAtLine;

  /**
   * The Current value prefix.
   */
  private String prefixValue;

  /**
   * The Current value delimiter.
   */
  private String valueSeparator;

  /**
   * The Current comment prefix.
   */
  private String prefixComment;

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
   * Inside char buffer to be used for converting operations.
   */
  private final char[] CHAR_BUFFER = new char[64];

  /**
   * The Value contains the start of commentaries on the previous line.
   */
  private int prevLineCommentsStartPosition = 0;

  /**
   * The List contains all registered extras.
   */
  private final List<Extra> extras = new ArrayList<Extra>();

  /**
   * The Default constructor. A StringWriter will be used inside.
   */
  public JBBPTextWriter() {
    this(new StringWriter(1024), JBBPByteOrder.BIG_ENDIAN, System.getProperty("line.separator"), DEFAULT_RADIX, DEFAULT_VALUE_PREFIX, DEFAULT_FIRST_VALUE_LINE_PREFIX, DEFAULT_COMMENT_PREFIX, DEFAULT_VALUE_DELIMITER);
  }

  /**
   * Constructor to wrap a writer,
   *
   * @param out a writer to be wrapped, must not be null.
   */
  public JBBPTextWriter(final Writer out) {
    this(out, JBBPByteOrder.BIG_ENDIAN, System.getProperty("line.separator"), DEFAULT_RADIX, DEFAULT_VALUE_PREFIX, DEFAULT_FIRST_VALUE_LINE_PREFIX, DEFAULT_COMMENT_PREFIX, DEFAULT_VALUE_DELIMITER);
  }

  /**
   * Constructor to wrap a writer with defined byte order.
   *
   * @param out a writer to be wrapped, must not be null.
   * @param byteOrder a byte order to be used, it must not be null.
   */
  public JBBPTextWriter(final Writer out, final JBBPByteOrder byteOrder) {
    this(out, byteOrder, System.getProperty("line.separator"), DEFAULT_RADIX, DEFAULT_VALUE_PREFIX, DEFAULT_FIRST_VALUE_LINE_PREFIX, DEFAULT_COMMENT_PREFIX, DEFAULT_VALUE_DELIMITER);
  }

  /**
   * Constructor.
   *
   * @param out a writer to be wrapper, must not be null.
   * @param byteOrder byte order to be used for converting, must not be null.
   * @param lineSeparator line separator, must not be null.
   * @param radix radix, must be 2..36.
   * @param valuePrefix prefix before each value, can be null.
   * @param startValueLinePrefix prefix before the first value on line, can be
   * null.
   * @param commentPrefix prefix before comments, can be null.
   * @param valueDelimiter delimiter between values, can be null
   */
  public JBBPTextWriter(
          final Writer out,
          final JBBPByteOrder byteOrder,
          final String lineSeparator,
          final int radix,
          final String valuePrefix,
          final String startValueLinePrefix,
          final String commentPrefix,
          final String valueDelimiter) {
    super(out);
    JBBPUtils.assertNotNull(lineSeparator, "Line separator must not be null");
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
        this.write(this.prefixFirtValueAtLine);
      }
      break;
      case MODE_COMMENTS: {
        this.BR();
        writeIndent();
        changeMode(MODE_VALUES);
        for (final Extra e : extras) {
          e.onBeforeFirstValue(this);
        }
        this.write(this.prefixFirtValueAtLine);
      }
      break;
    }
  }

  /**
   * Write current indent to line as bunch of spaces.
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
    if (this.valuesLineCounter > 0) {
      if (this.valueSeparator.length() > 0) {
        this.write(this.valueSeparator);
      }
    }

    if (this.prefixValue.length() > 0) {
      this.write(this.prefixValue);
    }
    this.write(value);
    this.valuesLineCounter++;

    if (this.maxValuesPerLine > 0 && this.valuesLineCounter >= this.maxValuesPerLine) {
      for (final Extra e : this.extras) {
        e.onReachedMaxValueNumberForLine(this);
      }
      ensureNewLineMode();
    }
  }

  /**
   * Print byte value.
   *
   * @param value a byte value
   * @return the context
   * @throws IOException will be thrown for transport errors
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

    if (convertedByExtras == null) {
      printValueString(JBBPUtils.ensureMinTextLength(JBBPUtils.ulong2str(value & 0xFF, this.radix, CHAR_BUFFER), this.maxCharsRadixForByte, '0', 0));
    }
    else {
      printValueString(convertedByExtras);
    }

    return this;
  }

  /**
   * Print byte array.
   *
   * @param values a byte array, must not be null
   * @return the context
   * @throws IOException will be thrown for transport errors
   */
  public JBBPTextWriter Byte(final byte[] values) throws IOException {
    return Byte(values, 0, values.length);
  }

  /**
   * Print values from byte array.
   *
   * @param array source byte array, must not be null
   * @param off the offset of the first element in array
   * @param len number of bytes to be printed
   * @return the context
   * @throws IOException will be thrown for transport errors
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
  public JBBPTextWriter SetValueLinePrefix(final String text) {
    this.prefixFirtValueAtLine = text == null ? "" : text;
    return this;
  }

  /**
   * Set prefix to be printed before every value.
   *
   * @param text string to be used as value prefix, can be null
   * @return the context
   */
  public JBBPTextWriter SetValuePrefix(final String text) {
    this.prefixValue = text == null ? "" : text;
    return this;
  }

  /**
   * Set prefix to be printed as comment start.
   *
   * @param text string to be used as comment start, can be null
   * @return the context
   */
  public JBBPTextWriter SetCommentPrefix(final String text) {
    this.prefixComment = text == null ? "" : text;
    return this;
  }

  /**
   * Set delimiter to be printed between values.
   *
   * @param text string to be used as separator between values.
   * @return the context
   */
  public JBBPTextWriter SetValueSeparator(final String text) {
    this.valueSeparator = text == null ? "" : text;
    return this;
  }

  /**
   * Add extras to context.
   *
   * @param extras extras to be added to context, must not be null
   * @return the context
   */
  public JBBPTextWriter AddExtras(final Extra ... extras) {
    JBBPUtils.assertNotNull(extras, "Extras must not be null");
    for(final Extra e : extras){
      JBBPUtils.assertNotNull(e, "Extras must not be null");
      this.extras.add(e);
    }
    return this;
  }

  /**
   * Remove extras from context
   * @param extras extras to be removed, must not be null
   * @return the context
   */
  public JBBPTextWriter DelExtras(final Extra ... extras) {    
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
   * turn off checking
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
  public JBBPTextWriter TabSpaces(final int numberOfSpacesPerTab) {
    if (numberOfSpacesPerTab <= 0) {
      throw new IllegalArgumentException("Tab must contains positive number of space chars [" + numberOfSpacesPerTab + ']');
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
  public JBBPTextWriter ByteOrder(final JBBPByteOrder order) {
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
  public JBBPTextWriter Radix(final int radix) {
    if (radix < 2 || radix > 36) {
      throw new IllegalArgumentException("Unsupported radix value [" + radix + ']');
    }
    this.radix = radix;
    this.maxCharsRadixForByte = JBBPUtils.ulong2str(0xFFL, this.radix, CHAR_BUFFER).length();
    this.maxCharsRadixForShort = JBBPUtils.ulong2str(0xFFFFL, this.radix, CHAR_BUFFER).length();
    this.maxCharsRadixForInt = JBBPUtils.ulong2str(0xFFFFFFFFL, this.radix, CHAR_BUFFER).length();
    this.maxCharsRadixForLong = JBBPUtils.ulong2str(0xFFFFFFFFFFFFFFFFL, this.radix, CHAR_BUFFER).length();

    return this;
  }

  /**
   * Print short value.
   *
   * @param value short value to be printed
   * @return the context
   * @throws IOException will be thrown for transport errors
   */
  public JBBPTextWriter Short(final short value) throws IOException {
    ensureValueMode();
    String convertedByExtras = null;
    for (final Extra e : this.extras) {
      convertedByExtras = e.doConvertShortToStr(this, value & 0xFFFF);
      if (convertedByExtras != null) {
        break;
      }
    }

    if (convertedByExtras == null) {
      final short valueToWrite;
      if (this.byteOrder == JBBPByteOrder.LITTLE_ENDIAN) {
        valueToWrite = (short) JBBPUtils.reverseByteOrder(value, 2);
      }
      else {
        valueToWrite = value;
      }
      printValueString(JBBPUtils.ensureMinTextLength(JBBPUtils.ulong2str(valueToWrite & 0xFFFF, this.radix, CHAR_BUFFER), this.maxCharsRadixForShort, '0', 0));
    }
    else {
      printValueString(convertedByExtras);
    }
    return this;
  }

  /**
   * Print array of short values.
   *
   * @param values array of short values, must not be null
   * @return the context
   * @throws IOException will be thrown for transport errors
   */
  public JBBPTextWriter Short(final short[] values) throws IOException {
    return this.Short(values, 0, values.length);
  }

  /**
   * Print values from short array/
   *
   * @param values short value array, must not be null
   * @param off offset to the first element
   * @param len number of elements to print
   * @return the context
   * @throws IOException will be thrown for transport error
   */
  public JBBPTextWriter Short(final short[] values, int off, int len) throws IOException {
    while (len-- > 0) {
      this.Short(values[off++]);
    }
    return this;
  }

  /**
   * Print integer value
   *
   * @param value value to be printed
   * @return the context
   * @throws IOException will be thrown for transport error
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
      final int valueToWrite;
      if (this.byteOrder == JBBPByteOrder.LITTLE_ENDIAN) {
        valueToWrite = (int) JBBPUtils.reverseByteOrder(value, 4);
      }
      else {
        valueToWrite = value;
      }
      printValueString(JBBPUtils.ensureMinTextLength(JBBPUtils.ulong2str(valueToWrite & 0xFFFFFFFFL, this.radix, CHAR_BUFFER), this.maxCharsRadixForInt, '0', 0));
    }
    else {
      printValueString(convertedByExtras);
    }
    return this;
  }

  /**
   * Print integer array.
   *
   * @param values integer array to be printed, must not be null
   * @return the context
   * @throws IOException will be thrown for transport error
   */
  public JBBPTextWriter Int(final int[] values) throws IOException {
    return this.Int(values, 0, values.length);
  }

  /**
   * Print values from integer array.
   *
   * @param values integer array, must not be null
   * @param off offset to the first element in array
   * @param len number of elements to print
   * @return the context
   * @throws IOException will be thrown for transport error
   */
  public JBBPTextWriter Int(final int[] values, int off, int len) throws IOException {
    while (len-- > 0) {
      this.Int(values[off++]);
    }
    return this;
  }

  /**
   * Increase indent.
   *
   * @return the context
   * @throws IOException will be thrown for transport error
   */
  public JBBPTextWriter IndentInc() throws IOException {
    this.indent += this.spacesInTab;
    return this;
  }

  /**
   * Increase indent.
   *
   * @param count number of indents
   * @return the context
   * @throws IOException will be thrown for transport error
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
   * @throws IOException will be thrown for transport error
   */
  public JBBPTextWriter IndentDec() throws IOException {
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
   * @throws IOException will be thrown for transport error
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
   * @throws IOException will be thrown for transport errors
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
      }
      else {
        valueToWrite = value;
      }
      printValueString(JBBPUtils.ensureMinTextLength(JBBPUtils.ulong2str(valueToWrite, this.radix, CHAR_BUFFER), this.maxCharsRadixForLong, '0', 0));
    }
    else {
      printValueString(convertedByExtras);
    }
    return this;
  }

  /**
   * Print values from long array.
   *
   * @param values array to be printed, must not be null
   * @return the context
   * @throws IOException will be thrown for transport errors
   */
  public JBBPTextWriter Long(final long[] values) throws IOException {
    return this.Long(values, 0, values.length);
  }

  /**
   * print values from long array.
   *
   * @param values array to be printed, must not be null
   * @param off offset to the first element to print
   * @param len number of elements to be printed
   * @return the context
   * @throws IOException will be thrown for transport errors
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
   * @throws IOException will be thrown for transport errors
   */
  public JBBPTextWriter BR() throws IOException {
    this.write(this.lineSeparator);
    ensureNewLineMode();
    return this;
  }

  /**
   * Change parameters for horizontal rule.
   *
   * @param length the length in symbols.
   * @param ch symbol to draw
   * @return the context
   */
  public JBBPTextWriter SetHR(final int length, final char ch) {
    this.hrChar = ch;
    this.hrLength = length;
    return this;
  }

  /**
   * Print horizontal rule.
   *
   * @return the context
   * @throws IOException will be thrown for transport errors
   */
  public JBBPTextWriter HR() throws IOException {
    ensureNewLineMode();
    this.write(this.prefixComment);
    for (int i = 0; i < this.hrLength; i++) {
      this.write(this.hrChar);
    }
    BR();
    return this;
  }

  /**
   * Print comments.
   *
   * @param comment array of string to be printed as comment lines.
   * @return the context
   * @throws IOException will be thrown for transport errors
   */
  public JBBPTextWriter Comment(final String... comment) throws IOException {
    if (comment != null) {
      for (final String c : comment) {
        ensureCommentMode();
        write(c);
      }
      BR();
    }
    return this;
  }

  @Override
  public String toString() {
    final String result;
    if (this.out instanceof StringWriter) {
      result = ((StringWriter) this.out).toString();
    }
    else {
      result = JBBPTextWriter.class.getName() + '(' + this.out.getClass().getName() + ")@" + System.identityHashCode(this);
    }
    return result;
  }

  /**
   * Print objects.
   *
   * @param objId object id which will be provided to a converter as extra info
   * @param obj objects to be converted and printed, must not be null
   * @return the context
   * @throws IOException will be thrown for transport errors
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
   * @param off offset to the first element to be printed
   * @param len number of elements to be printed
   * @return the context
   * @throws IOException will be thrown for transport errors
   */
  public JBBPTextWriter Obj(final int objId, final Object[] array, int off, int len) throws IOException {
    while (len-- > 0) {
      this.Obj(objId, array[off++]);
    }
    return this;
  }

  /**
   * Close the wrapped writer.
   *
   * @return the context
   * @throws IOException will be thrown for transport errors
   */
  public JBBPTextWriter Close() throws IOException {
    for (final Extra e : extras) {
      e.onClose(this);
    }
    super.close();
    return this;
  }

  /**
   * Flush buffers in wrapped writer.
   *
   * @return the context
   * @throws IOException will be thrown for transport errors
   */
  public JBBPTextWriter Flush() throws IOException {
    super.flush();
    return this;
  }

  /**
   * Print tab as space chars.
   *
   * @return the context
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPTextWriter Tab() throws IOException {
    this.Space(this.linePosition % this.spacesInTab);
    return this;
  }

  /**
   * Set number of spaces for tab simulation and indents.
   *
   * @param value number of spaces, must be equal or greater than one
   * @return the context
   */
  public JBBPTextWriter setTabSpaces(final int value) {
    if (value < 1) {
      throw new IllegalArgumentException("Space number must be positive number [" + value + ']');
    }
    this.spacesInTab = value;
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
   * @throws IOException will be thrown for transport errors.
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

}
