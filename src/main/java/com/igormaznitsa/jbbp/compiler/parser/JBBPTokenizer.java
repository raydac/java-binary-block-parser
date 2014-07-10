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
package com.igormaznitsa.jbbp.compiler.parser;

import com.igormaznitsa.jbbp.exceptions.JBBPTokenizerException;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.util.*;
import java.util.regex.*;

/**
 * The Class implements a token parser which parses a String to binary block
 * parser tokens and check their format.
 */
public final class JBBPTokenizer implements Iterable<JBBPToken>, Iterator<JBBPToken> {

  /**
   * The Next available token.
   */
  private JBBPToken nextItem;

  /**
   * The Field contains deferred error.
   */
  private JBBPTokenizerException detectedError;

  /**
   * The Pattern to break a string to tokens.
   */
  private static final Pattern PATTERN = Pattern.compile("\\s*\\/\\/.*$|\\s*(\\})|\\s*([^\\s\\;\\[\\]\\}\\{\\/]+)?\\s*(?:\\[\\s*(\\S+)\\s*\\])?\\s*([^\\d\\s\\;\\[\\]\\}\\{\\/][^\\s\\;\\[\\]\\}\\{\\/]*)?\\s*([\\{\\;])", Pattern.MULTILINE);

  /**
   * The Pattern to break field type to parameters.
   */
  private static final Pattern FIELD_TYPE_BREAK_PATTERN = Pattern.compile("^([<>])?(\\w+)(?::(\\d+))?$");

  /**
   * Inside table to keep disabled names for fields.
   */
  private static final Set<String> disabledFieldNames;

  static {
    disabledFieldNames = new HashSet<String>();
    disabledFieldNames.add("bit");
    disabledFieldNames.add("bool");
    disabledFieldNames.add("byte");
    disabledFieldNames.add("ubyte");
    disabledFieldNames.add("short");
    disabledFieldNames.add("ushort");
    disabledFieldNames.add("int");
    disabledFieldNames.add("long");
    disabledFieldNames.add("$");
  }

  private final Matcher matcher;
  private int lastCharSubstingFound = -1;
  private final String processingString;

  /**
   * The Constructor.
   *
   * @param str a string to be parsed, must not be null.
   */
  public JBBPTokenizer(final String str) {
    JBBPUtils.assertNotNull(str, "String must not be null");
    this.processingString = str;
    this.matcher = PATTERN.matcher(this.processingString);
    readNextItem();
  }

  /**
   * Inside method to read the next token from the string and place it into
   * inside storage.
   */
  private void readNextItem() {
    if (matcher.find()) {
      final String groupWholeFound = this.matcher.group(0);
      final String groupWholeFoundTrimmed = groupWholeFound.trim();

      final String groupCloseStruct = this.matcher.group(1);
      final String groupTypeOrName = this.matcher.group(2);
      final String groupArrayLength = this.matcher.group(3);
      final String groupName = this.matcher.group(4);
      final String groupEnder = this.matcher.group(5);

      final String skipString = this.processingString.substring(Math.max(this.lastCharSubstingFound, 0), matcher.start()).trim();
      if (skipString.length() != 0 && !skipString.startsWith("//")) {
        this.detectedError = new JBBPTokenizerException(skipString, Math.max(this.lastCharSubstingFound, 0));
      }
      else {
        JBBPTokenType type = JBBPTokenType.ATOM;

        if (groupWholeFoundTrimmed.startsWith("//")) {
          type = JBBPTokenType.COMMENT;
        }
        else if ("{".equals(groupEnder)) {
          // {
          type = JBBPTokenType.STRUCT_START;
          if (groupName != null) {
            final int position = matcher.start() + groupWholeFound.length() - groupWholeFoundTrimmed.length();
            this.detectedError = new JBBPTokenizerException("Wrong structure format, it must have only name (and may be array definition)", position);
            return;
          }
        }
        else if (groupCloseStruct != null) {
          type = JBBPTokenType.STRUCT_END;
        }
        else if (groupTypeOrName == null) {
          final int position = matcher.start() + groupWholeFound.length() - groupWholeFoundTrimmed.length();
          this.detectedError = new JBBPTokenizerException("Detected atomic field definition without type", position);
          return;
        }

        String fieldType = groupTypeOrName;
        final String arrayLength = groupArrayLength;

        int position = matcher.start();

        final String fieldName;
        if (type == JBBPTokenType.COMMENT) {
          fieldName = matcher.group(0).trim().substring(2).trim();
          position += groupWholeFound.indexOf('/');
        }
        else {
          if (type == JBBPTokenType.STRUCT_START) {
            fieldName = fieldType;
            fieldType = null;
          }
          else {
            fieldName = groupName;
          }

          position += groupWholeFound.length() - groupWholeFound.trim().length();

          this.detectedError = checkFieldName(fieldName, position);

          if (this.detectedError != null) {
            return;
          }
        }

        JBBPFieldTypeParameterContainer parsedType = null;
        if (fieldType != null) {
          final Matcher typeMatcher = FIELD_TYPE_BREAK_PATTERN.matcher(fieldType);
          boolean wrongFormat = true;

          if (typeMatcher.find()) {
            final String groupTypeByteOrder = typeMatcher.group(1);
            final String groupTypeName = typeMatcher.group(2);
            final String groupTypeBitNumber = typeMatcher.group(3);

            wrongFormat = false;

            JBBPByteOrder byteOrder = null;
            if (groupTypeByteOrder != null) {
              if (">".equals(groupTypeByteOrder)) {
                byteOrder = JBBPByteOrder.BIG_ENDIAN;
              }
              else if ("<".equals(groupTypeByteOrder)) {
                byteOrder = JBBPByteOrder.LITTLE_ENDIAN;
              }
              else {
                throw new Error("Illegal byte order char, unexpected error, contact developer please [" + fieldType + ']');
              }
            }

            if (!wrongFormat) {
              parsedType = new JBBPFieldTypeParameterContainer(byteOrder, groupTypeName, groupTypeBitNumber);
            }
          }

          if (wrongFormat) {
            this.detectedError = new JBBPTokenizerException("Wrong format of type definition [" + fieldType + ']', position);
            return;
          }
        }
        else {
          parsedType = null;
        }

        this.nextItem = new JBBPToken(type, position, parsedType, arrayLength, fieldName);
        lastCharSubstingFound = matcher.end();
      }
    }
    else {
      if (this.lastCharSubstingFound < 0) {
        this.detectedError = new JBBPTokenizerException("Wrong format of whole string", 0);
      }
      else {
        final String restOfString = this.processingString.substring(this.lastCharSubstingFound);
        if (restOfString.trim().length() != 0) {
          throw new JBBPTokenizerException("Can't recognize a part of script [" + restOfString + ']', this.lastCharSubstingFound);
        }
      }
      this.nextItem = null;
    }
  }

  /**
   * Check a field name
   *
   * @param name the name to be checked.
   * @param position the token position in the string.
   * @return JBBPTokenizerException if the field name has wrong chars or
   * presented in disabled name set, null otherwise
   */
  private static JBBPTokenizerException checkFieldName(final String name, final int position) {
    if (name != null) {
      final String normalized = name.toLowerCase(Locale.ENGLISH);
      if (normalized.indexOf('.') >= 0) {
        return new JBBPTokenizerException("Field name must not contain '.' char", position);
      }
      if (disabledFieldNames.contains(normalized) || normalized.startsWith("$")) {
        return new JBBPTokenizerException("'" + name + "' can't be used as field name", position);
      }
    }
    return null;
  }

  public Iterator<JBBPToken> iterator() {
    return this;
  }

  public boolean hasNext() {
    return this.detectedError != null || nextItem != null;
  }

  public JBBPToken next() {
    if (this.detectedError != null) {
      final JBBPTokenizerException ex = this.detectedError;
      this.detectedError = null;
      throw ex;
    }
    final JBBPToken current = this.nextItem;
    if (current == null) {
      throw new NoSuchElementException("Parsing has been completed");
    }

    readNextItem();
    return current;
  }

  /**
   * The Operation is unsupported one.
   */
  public void remove() {
    throw new UnsupportedOperationException("Unsupported operation");
  }
}
