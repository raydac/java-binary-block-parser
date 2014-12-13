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

  public interface Extras {

    void onNewLine(JBBPTextWriter context, int lineNumber) throws IOException;

    void onBeforeFirstVar(JBBPTextWriter context) throws IOException;

    void onClose(JBBPTextWriter context) throws IOException;

    String doByteToStr(JBBPTextWriter context, int value) throws IOException;

    String doShortToStr(JBBPTextWriter context, int value) throws IOException;

    String doIntToStr(JBBPTextWriter context, int value) throws IOException;

    String doLongToStr(JBBPTextWriter context, long value) throws IOException;

    String doObjToStr(JBBPTextWriter context, String id, Object obj) throws IOException;
  }

  private static final String DEFAULT_COMMENT_PREFIX = ";";
  private static final String DEFAULT_VALUE_LINE_PREFIX = "";
  private static final String DEFAULT_VALUE_PREFIX = "";
  private static final String DEFAULT_VALUE_DELIMITER = " ";
  private static final int DEFAULT_RADIX = 16;

  private final String lineSeparator;

  private static final int MODE_START_LINE = 0;
  private static final int MODE_VALUES = 1;
  private static final int MODE_COMMENTS = 2;

  private int prevMode = MODE_START_LINE;
  private int mode = MODE_START_LINE;

  private int radix;
  private int maxCharsRadixForByte;
  private int maxCharsRadixForShort;
  private int maxCharsRadixForInt;
  private int maxCharsRadixForLong;

  private JBBPByteOrder byteOrder;
  private String valueLinePrefix;
  private String valuePrefix;
  private String valueDelimiter;
  private String commentPrefix;

  private int linePosition = 0;
  private int lineNumber = 0;
  private boolean valuesPresentedOnLine;
  private int spacesInTab = 4;
  private int indent = 0;

  private final char[] CHAR_BUFFER = new char[64];
  private int lastCommentLinePositionStart = 0;

  private final List<Extras> extrases = new ArrayList<Extras>();

  public JBBPTextWriter() {
    this(new StringWriter(1024), JBBPByteOrder.BIG_ENDIAN, System.getProperty("line.separator"), DEFAULT_RADIX, DEFAULT_VALUE_PREFIX, DEFAULT_VALUE_LINE_PREFIX, DEFAULT_COMMENT_PREFIX, DEFAULT_VALUE_DELIMITER);
  }

  public JBBPTextWriter(final Writer out) {
    this(out, JBBPByteOrder.BIG_ENDIAN, System.getProperty("line.separator"), DEFAULT_RADIX, DEFAULT_VALUE_PREFIX, DEFAULT_VALUE_LINE_PREFIX, DEFAULT_COMMENT_PREFIX, DEFAULT_VALUE_DELIMITER);
  }

  public JBBPTextWriter(final Writer out, final JBBPByteOrder byteOrder) {
    this(out, byteOrder, System.getProperty("line.separator"), DEFAULT_RADIX, DEFAULT_VALUE_PREFIX, DEFAULT_VALUE_LINE_PREFIX, DEFAULT_COMMENT_PREFIX, DEFAULT_VALUE_DELIMITER);
  }

  public JBBPTextWriter(
          final Writer out,
          final JBBPByteOrder byteOrder,
          final String lineSeparator,
          final int radix,
          final String valuePrefix,
          final String valueLinePrefix,
          final String commentPrefix,
          final String valueDelimiter) {
    super(out);
    JBBPUtils.assertNotNull(lineSeparator, "Line separator must not be null");
    this.lineSeparator = lineSeparator;

    JBBPUtils.assertNotNull(out, "Writer must not be null");
    ByteOrder(byteOrder);
    ValDlmtr(valueDelimiter);
    PrfxValLine(valueLinePrefix);
    PrfxComment(commentPrefix);
    PrfxVal(valuePrefix);
    Radix(radix);
  }
  
  private void changeMode(final int mode) {
    this.prevMode = this.mode;
    this.mode = mode;
  }

  private void ensureValueMode() throws IOException {
    switch (this.mode) {
      case MODE_START_LINE: {
        changeMode(MODE_VALUES);
        for (final Extras e : extrases) {
          e.onBeforeFirstVar(this);
        }
        writeIndent();
        this.writeString(this.valueLinePrefix);
      }
      break;
      case MODE_COMMENTS: {
        this.newLine();
        writeIndent();
        changeMode(MODE_VALUES);
        for (final Extras e : extrases) {
          e.onBeforeFirstVar(this);
        }
        this.writeString(this.valueLinePrefix);
      }
      break;
    }
  }

  private void writeIndent() throws IOException {
    if (this.indent > 0) {
      int i = this.indent;
      while (i > 0) {
        this.write(' ');
        i--;
      }
    }
  }

  private void ensureCommentMode() throws IOException {
    switch (this.mode) {
      case MODE_START_LINE:
        writeIndent();
        this.lastCommentLinePositionStart = this.linePosition;
        this.writeString(this.commentPrefix);
        changeMode(MODE_COMMENTS);
        break;
      case MODE_VALUES: {
        this.lastCommentLinePositionStart = this.linePosition;
        this.writeString(this.commentPrefix);
        changeMode(MODE_COMMENTS);
      }
      break;
      case MODE_COMMENTS: {
        newLine();
        while (this.linePosition < this.lastCommentLinePositionStart) {
          this.write(' ');
        }
        this.writeString(this.commentPrefix);
      }
      break;
      default:
        throw new Error("Unexpected state");
    }
  }

  private void ensureNewLineMode() throws IOException {
    if (this.mode != MODE_START_LINE) {
      this.write(this.lineSeparator);
    }
    valuesPresentedOnLine = false;
  }

  private void printPrefixedValue(final String value) throws IOException {
    if (this.valuesPresentedOnLine) {
      if (this.valueDelimiter.length() > 0) {
        this.write(this.valueDelimiter);
      }
    }

    if (this.valuePrefix.length() > 0) {
      this.write(this.valuePrefix);
    }
    this.valuesPresentedOnLine = true;
    this.write(value);
  }

  public static String alignValueByZeroes(final String valueText, final int maxLen) throws IOException {
    final int numberOfPrefixingZero = maxLen - valueText.length();
    if (numberOfPrefixingZero <= 0) {
      return valueText;
    }
    final StringBuilder result = new StringBuilder(maxLen);
    final int zeros = maxLen - valueText.length();
    for (int i = 0; i < zeros; i++) {
      result.append('0');
    }
    return result.append(valueText).toString();
  }

  public JBBPTextWriter Byte(final int value) throws IOException {
    ensureValueMode();

    String convertedByExtras = null;
    for (final Extras e : this.extrases) {
      convertedByExtras = e.doByteToStr(this, value);
      if (convertedByExtras != null) {
        break;
      }
    }

    if (convertedByExtras == null) {
      printPrefixedValue(alignValueByZeroes(JBBPUtils.ulong2str(value & 0xFF, this.radix, CHAR_BUFFER), this.maxCharsRadixForByte));
    }
    else {
      printPrefixedValue(convertedByExtras);
    }

    return this;
  }

  public JBBPTextWriter Byte(final byte[] values) throws IOException {
    for (final byte b : values) {
      Byte(b);
    }
    return this;
  }

  public JBBPTextWriter Byte(final byte[] values, int off, int len) throws IOException {
    ensureValueMode();
    while (len-- > 0) {
      Byte(values[off++]);
    }

    return this;
  }

  public JBBPTextWriter PrfxValLine(final String text) {
    this.valueLinePrefix = text == null ? "" : text;
    return this;
  }

  public JBBPTextWriter PrfxVal(final String text) {
    this.valuePrefix = text == null ? "" : text;
    return this;
  }

  public JBBPTextWriter PrfxComment(final String text) {
    this.commentPrefix = text == null ? "" : text;
    return this;
  }

  public JBBPTextWriter ValDlmtr(final String text) {
    this.valueDelimiter = text == null ? "" : text;
    return this;
  }

  public JBBPTextWriter RegExtras(final Extras extras) {
    JBBPUtils.assertNotNull(extras, "Extras must not be null");
    this.extrases.add(extras);
    return this;
  }

  public JBBPTextWriter TabSpaces(final int numberOfSpacesPerTab) {
    if (numberOfSpacesPerTab <= 0) {
      throw new IllegalArgumentException("Tab must contains positive number of space chars [" + numberOfSpacesPerTab + ']');
    }
    final int currentIdentSteps = this.indent / this.spacesInTab;
    this.spacesInTab = numberOfSpacesPerTab;
    this.indent = currentIdentSteps * this.spacesInTab;
    return this;
  }

  public JBBPTextWriter ByteOrder(final JBBPByteOrder order) {
    JBBPUtils.assertNotNull(order, "Byte order must not be null");
    this.byteOrder = order;
    return this;
  }

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

  public JBBPTextWriter Short(final short value) throws IOException {
    ensureValueMode();

    String convertedByExtras = null;
    for (final Extras e : this.extrases) {
      convertedByExtras = e.doShortToStr(this, value);
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
      printPrefixedValue(alignValueByZeroes(JBBPUtils.ulong2str(valueToWrite & 0xFFFF, this.radix, CHAR_BUFFER), this.maxCharsRadixForShort));
    }
    else {
      printPrefixedValue(convertedByExtras);
    }
    return this;
  }

  public JBBPTextWriter Short(final short[] values) throws IOException {
    return this.Short(values, 0, values.length);
  }

  public JBBPTextWriter Short(final short[] values, int off, int len) throws IOException {
    while (len-- > 0) {
      this.Short(values[off++]);
    }
    return this;
  }

  public JBBPTextWriter Int(final int value) throws IOException {
    ensureValueMode();

    String convertedByExtras = null;
    for (final Extras e : this.extrases) {
      convertedByExtras = e.doIntToStr(this, value);
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
      printPrefixedValue(alignValueByZeroes(JBBPUtils.ulong2str(valueToWrite & 0xFFFFFFFFL, this.radix, CHAR_BUFFER), this.maxCharsRadixForInt));
    }
    else {
      printPrefixedValue(convertedByExtras);
    }
    return this;
  }

  public JBBPTextWriter Int(final int[] values) throws IOException {
    return this.Int(values, 0, values.length);
  }

  public JBBPTextWriter Int(final int[] values, int off, int len) throws IOException {
    while (len-- > 0) {
      this.Int(values[off++]);
    }
    return this;
  }

  public JBBPTextWriter IndentInc() throws IOException {
    this.indent += this.spacesInTab;
    return this;
  }

  public JBBPTextWriter IndentInc(final int count) throws IOException {
    for (int i = 0; i < count; i++) {
      IndentInc();
    }
    return this;
  }

  public JBBPTextWriter IndentDec(final int count) throws IOException {
    for (int i = 0; i < count; i++) {
      IndentDec();
    }
    return this;
  }

  public JBBPTextWriter IndentDec() throws IOException {
    if (this.indent > 0) {
      this.indent = Math.max(0, this.indent - this.spacesInTab);
    }
    return this;
  }

  public JBBPTextWriter Long(final long value) throws IOException {
    ensureValueMode();

    String convertedByExtras = null;
    for (final Extras e : this.extrases) {
      convertedByExtras = e.doLongToStr(this, value);
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
      printPrefixedValue(alignValueByZeroes(JBBPUtils.ulong2str(valueToWrite, this.radix, CHAR_BUFFER), this.maxCharsRadixForLong));
    }
    else {
      printPrefixedValue(convertedByExtras);
    }
    return this;
  }

  public JBBPTextWriter Long(final long[] values) throws IOException {
    return this.Long(values, 0, values.length);
  }

  public JBBPTextWriter Long(final long[] values, int off, int len) throws IOException {
    while (len-- > 0) {
      this.Long(values[off++]);
    }
    return this;
  }

  public JBBPTextWriter newLine() throws IOException {
    this.write(this.lineSeparator);
    ensureNewLineMode();
    return this;
  }

  public JBBPTextWriter Separator() throws IOException {
    ensureNewLineMode();
    this.writeString(this.commentPrefix);
    for (int i = 0; i < 80; i++) {
      this.write('-');
    }
    newLine();
    return this;
  }

  public JBBPTextWriter Comment(final String... comment) throws IOException {
    if (comment != null) {
      for (final String c : comment) {
        ensureCommentMode();
        writeString(c);
      }
      newLine();
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

  public JBBPTextWriter Obj(final String objId, final Object... obj) throws IOException {
    if (this.extrases.isEmpty()) {
      throw new IllegalStateException("There is not any registered extras");
    }
    ensureValueMode();

    for (final Object c : obj) {
      String str = null;
      for (final Extras e : this.extrases) {
        str = e.doObjToStr(this, objId, c);
        if (str != null) {
          break;
        }
      }

      JBBPUtils.assertNotNull(str, "Object has not been converted to string [" + objId + ',' + c + ']');
      printPrefixedValue(str);
    }
    return this;
  }

  public JBBPTextWriter Close() throws IOException {
    for (final Extras e : extrases) {
      e.onClose(this);
    }
    super.close();
    return this;
  }

  public JBBPTextWriter Flush() throws IOException {
    super.flush();
    return this;
  }

  public JBBPTextWriter Tab() throws IOException {
    this.Space(this.linePosition % this.spacesInTab);
    return this;
  }

  public JBBPTextWriter setTabSpaces(final int value) {
    if (value < 1) {
      throw new IllegalArgumentException("Space number must be positive number [" + value + ']');
    }
    this.spacesInTab = value;
    return this;
  }

  public JBBPTextWriter Space(final int numberOfSpaces) throws IOException {
    for (int i = 0; i < numberOfSpaces; i++) {
      writeChar(' ');
    }
    return this;
  }

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
        for (final Extras e : extrases) {
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

  public int getLine() {
    return this.lineNumber;
  }

  public int getLinePosition() {
    return this.linePosition;
  }

  private void writeString(final String str) throws IOException {
    this.writeString(str, 0, str.length());
  }

  private void writeString(final String str, int off, int len) throws IOException {
    while (len-- > 0) {
      this.writeChar(str.charAt(off++));
    }
  }

  private void writeString(final char[] buff, int off, int len) throws IOException {
    while (len-- > 0) {
      this.writeChar(buff[off++]);
    }
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
  public void write(final String str, final int off, final int len) throws IOException {
    this.writeString(str, off, len);
  }

  @Override
  public void write(final char[] cbuf, int off, int len) throws IOException {
    this.writeString(cbuf, off, len);
  }

  @Override
  public void write(final int c) throws IOException {
    this.writeChar((char) c);
  }

}
