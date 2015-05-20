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
package com.igormaznitsa.jbbp.compiler;

import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPTokenizer;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPToken;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPFieldTypeParameterContainer;
import com.igormaznitsa.jbbp.compiler.varlen.JBBPIntegerValueEvaluator;
import com.igormaznitsa.jbbp.compiler.varlen.JBBPEvaluatorFactory;
import com.igormaznitsa.jbbp.exceptions.*;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.*;
import java.util.*;

/**
 * The Class implements the compiler of a bin source script represented as a
 * text value into byte codes.
 *
 * @since 1.0
 */
public final class JBBPCompiler {

  /**
   * Inside auxiliary class to keep information about structures.
   */
  private static final class StructStackItem {

    /**
     * The Structure start offset.
     */
    private final int startStructureOffset;
    /**
     * The Start byte-code of the structure.
     */
    private final int code;
    /**
     * The Parsed Token.
     */
    private final JBBPToken token;

    /**
     * Named field counter value for the structure start.
     */
    private final int namedFieldCounter;

    /**
     * The Constructor.
     *
     * @param namedFieldCounter the named field counter value for the structure
     * start
     * @param startStructureOffset the offset of the start structure byte-code
     * instruction
     * @param code the start byte code
     * @param token the token
     */
    private StructStackItem(final int namedFieldCounter, final int startStructureOffset, final int code, final JBBPToken token) {
      this.namedFieldCounter = namedFieldCounter;
      this.startStructureOffset = startStructureOffset;
      this.code = code;
      this.token = token;
    }
  }

  /**
   * The Byte code of the 'ALIGN' command.
   */
  public static final int CODE_ALIGN = 0x01;
  /**
   * The Byte code of the 'BIT' command.
   */
  public static final int CODE_BIT = 0x02;
  /**
   * The Byte code of the 'BOOL' (boolean) command.
   */
  public static final int CODE_BOOL = 0x03;
  /**
   * The Byte code of the 'UBYTE' (unsigned byte) command.
   */
  public static final int CODE_UBYTE = 0x04;
  /**
   * The Byte code of the 'BYTE' command.
   */
  public static final int CODE_BYTE = 0x05;
  /**
   * p
   * The Byte code of the 'USHORT' (unsigned short) command.
   */
  public static final int CODE_USHORT = 0x06;
  /**
   * The Byte code of the 'SHORT' command.
   */
  public static final int CODE_SHORT = 0x07;
  /**
   * The Byte code of the 'INT' (integer) command.
   */
  public static final int CODE_INT = 0x08;
  /**
   * The Byte code of the 'LONG' command.
   */
  public static final int CODE_LONG = 0x09;

  /**
   * The Byte code of the 'STRUCTURE_START' command.
   */
  public static final int CODE_STRUCT_START = 0x0A;

  /**
   * The Byte code of the 'STRUCTURE_END' command.
   */
  public static final int CODE_STRUCT_END = 0x0B;

  /**
   * The Byte code of the SKIP command.
   */
  public static final int CODE_SKIP = 0x0C;

  /**
   * The Byte code of the VAR command. It describes a request to an external
   * processor to load values from a stream.
   */
  public static final int CODE_VAR = 0x0D;

  /**
   * The Byte code of the RESET COUNTER command. It resets the inside counter of
   * the input stream.
   */
  public static final int CODE_RESET_COUNTER = 0x0E;

  /**
   * The Byte code of the binary coded decimal (BCD, aka packed decimal) command.
   * @since 1.1.1
   */
  public static final int CODE_BCD = 0x0F;

  /**
   * The Byte-Code Flag shows that the field is a named one.
   */
  public static final int FLAG_NAMED = 0x10;
  /**
   * The Byte-Code Flag shows that the field is an array which size is defined
   * by an expression or the array is unsized and must be read till the end of a
   * stream.
   */
  public static final int FLAG_EXPRESSION_OR_WHOLESTREAM = 0x20;
  /**
   * The Byte-Code Flag shows that the field is an array but it must be omitted
   * for unlimited field arrays.
   */
  public static final int FLAG_ARRAY = 0x40;
  /**
   * The Byte-Code Flag shows that a multi-byte field must be decoded as
   * Little-endian one.
   */
  public static final int FLAG_LITTLE_ENDIAN = 0x80;

  /**
   * Compile a text script into its byte code representation/
   *
   * @param script a text script to be compiled, must not be null.
   * @return a compiled block for the script.
   * @throws IOException it will be thrown for an inside IO error.
   * @throws JBBPException it will be thrown for any logical or work exception
   * for the parser and compiler
   */
  public static JBBPCompiledBlock compile(final String script) throws IOException {
    JBBPUtils.assertNotNull(script, "Script must not be null");

    final JBBPCompiledBlock.Builder builder = JBBPCompiledBlock.prepare().setSource(script);

    final List<JBBPNamedFieldInfo> namedFields = new ArrayList<JBBPNamedFieldInfo>();
    final List<JBBPIntegerValueEvaluator> varLengthEvaluators = new ArrayList<JBBPIntegerValueEvaluator>();

    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    int offset = 0;

    final List<JBBPCompiler.StructStackItem> structureStack = new ArrayList<JBBPCompiler.StructStackItem>();
    final JBBPTokenizer parser = new JBBPTokenizer(script);

    int fieldUnrestrictedArrayOffset = -1;

    boolean hasVarFields = false;

    for (final JBBPToken token : parser) {
      if (token.isComment()) {
        continue;
      }

      final int code = prepareCodeForToken(token);
      final int startFieldOffset = offset;

      out.write(code);
      offset++;

      StructStackItem currentClosedStructure = null;
      boolean extraFieldPresented = false;
      int extraField = -1;

      // check that the field is not in the current structure which is a whole stream one
      if ((code & 0xF) != CODE_STRUCT_END && fieldUnrestrictedArrayOffset >= 0 && (structureStack.isEmpty() || structureStack.get(structureStack.size() - 1).startStructureOffset != fieldUnrestrictedArrayOffset)) {
        throw new JBBPCompilationException("Attempt to read field or structure after a full stream field", token);
      }

      switch (code & 0xF) {
        case CODE_BOOL:
        case CODE_BYTE:
        case CODE_UBYTE:
        case CODE_SHORT:
        case CODE_USHORT:
        case CODE_INT:
        case CODE_LONG: {
          // do nothing
        }
        break;
        case CODE_SKIP: {
          if (token.getArraySizeAsString() != null) {
            throw new JBBPCompilationException("A Skip field can't be array", token);
          }
          if (token.getFieldName() != null) {
            throw new JBBPCompilationException("A Skip field can't be named [" + token.getFieldName() + ']', token);
          }
          final String parsedSkipByteNumber = token.getFieldTypeParameters().getExtraData();
          extraFieldPresented = true;
          if (parsedSkipByteNumber == null) {
            extraField = 1;
          }
          else {
            try {
              extraField = Integer.parseInt(parsedSkipByteNumber);
              assertNonNegativeValue(extraField, token);
            }
            catch (NumberFormatException ex) {
              extraField = -1;
            }
          }
        }
        break;
        case CODE_ALIGN: {
          if (token.getArraySizeAsString() != null) {
            throw new JBBPCompilationException("An Align field can't be array", token);
          }
          if (token.getFieldName() != null) {
            throw new JBBPCompilationException("An Align field can't be named [" + token.getFieldName() + ']', token);
          }

          final String parsedAlignBytesNumber = token.getFieldTypeParameters().getExtraData();
          extraFieldPresented = true;
          if (parsedAlignBytesNumber == null) {
            extraField = 1;
          }
          else {
            try {
              extraField = Integer.parseInt(parsedAlignBytesNumber);
              assertNonNegativeValue(extraField, token);
            }
            catch (NumberFormatException ex) {
              extraField = -1;
            }
            if (extraField <= 0) {
              throw new JBBPCompilationException("Align byte number must be greater than zero [" + token.getFieldTypeParameters().getExtraData() + ']', token);
            }
          }
        }
        break;
        case CODE_BIT: {
          final String parsedBitNumber = token.getFieldTypeParameters().getExtraData();
          extraFieldPresented = true;
          if (parsedBitNumber == null) {
            extraField = 1;
          }
          else {
            try {
              extraField = Integer.parseInt(parsedBitNumber);
              assertNonNegativeValue(extraField, token);
            }
            catch (NumberFormatException ex) {
              extraField = -1;
            }
            if (extraField < 1 || extraField > 8) {
              throw new JBBPCompilationException("Wrong bit number, must be 1..8 [" + token.getFieldTypeParameters().getExtraData() + ']', token);
            }
          }
        }
        break;
        case CODE_BCD: {
          final String parsedNumBytes = token.getFieldTypeParameters().getExtraData();
          extraFieldPresented = true;
          if (parsedNumBytes == null) {
            extraField = 1;
          }
          else {
            try {
              extraField = Integer.parseInt(parsedNumBytes);
              assertNonNegativeValue(extraField, token);
            }
            catch (NumberFormatException ex) {
              extraField = -1;
            }
            if (extraField < 1 || extraField > 10) {
              throw new JBBPCompilationException("Wrong number of BCD bytes, must be 1..10 [" + token.getFieldTypeParameters().getExtraData() + ']', token);
            }
          }
        }
        break;
        case CODE_VAR: {
          hasVarFields = true;
          final String parsedExtraField = token.getFieldTypeParameters().getExtraData();
          extraFieldPresented = true;
          if (parsedExtraField == null) {
            extraField = 0;
          }
          else {
            try {
              extraField = Integer.parseInt(parsedExtraField);
            }
            catch (NumberFormatException ex) {
              throw new JBBPCompilationException("Can't parse the extra value of a VAR field, must be integer [" + token.getFieldTypeParameters().getExtraData() + ']', token);
            }
          }
        }
        break;
        case CODE_RESET_COUNTER: {
          if (token.getArraySizeAsString() != null) {
            throw new JBBPCompilationException("A Reset counter field can't be array", token);
          }
          if (token.getFieldName() != null) {
            throw new JBBPCompilationException("A Reset counter field can't be named [" + token.getFieldName() + ']', token);
          }
          if (token.getFieldTypeParameters().getExtraData() != null) {
            throw new JBBPCompilationException("A Reset counter field doesn't use extra value [" + token.getFieldName() + ']', token);
          }
        }
        break;
        case CODE_STRUCT_START: {
          structureStack.add(new StructStackItem(namedFields.size() + ((code & JBBPCompiler.FLAG_NAMED) == 0 ? 0 : 1), offset - 1, code, token));
        }
        break;
        case CODE_STRUCT_END: {
          if (structureStack.isEmpty()) {
            throw new JBBPCompilationException("Detected structure close tag without opening one", token);
          }
          else {
            currentClosedStructure = structureStack.remove(structureStack.size() - 1);
            offset += writePackedInt(out, currentClosedStructure.startStructureOffset);
          }
        }
        break;
        default:
          throw new Error("Detected unsupported compiled code, notify the developer please [" + code + ']');
      }

      if ((code & FLAG_ARRAY) != 0) {
        if ((code & FLAG_EXPRESSION_OR_WHOLESTREAM) != 0) {
          if ("_".equals(token.getArraySizeAsString())) {
            if (fieldUnrestrictedArrayOffset >= 0) {
              throw new JBBPCompilationException("Detected two or more unlimited arrays [" + script + ']', token);
            }
            else {
              fieldUnrestrictedArrayOffset = offset - 1;
            }
          }
          else {
            varLengthEvaluators.add(JBBPEvaluatorFactory.getInstance().make(token.getArraySizeAsString(), namedFields, out.toByteArray()));
          }
        }
        else {
          final int fixedArraySize = token.getArraySizeAsInt();
          if (fixedArraySize <= 0) {
            throw new JBBPCompilationException("Detected an array with negative or zero fixed length", token);
          }
          offset += writePackedInt(out, token.getArraySizeAsInt());
        }
      }

      if (extraFieldPresented) {
        offset += writePackedInt(out, extraField);
      }

      if ((code & FLAG_NAMED) != 0) {
        final String normalizedName = JBBPUtils.normalizeFieldNameOrPath(token.getFieldName());
        assertName(normalizedName, token);
        registerNamedField(normalizedName, structureStack.isEmpty() ? 0 : structureStack.get(structureStack.size() - 1).namedFieldCounter, startFieldOffset, namedFields, token);
      }
      else {
        if (currentClosedStructure != null && (currentClosedStructure.code & FLAG_NAMED) != 0) {
          // it is structure, process field names
          final String normalizedName = JBBPUtils.normalizeFieldNameOrPath(currentClosedStructure.token.getFieldName());
          for (int i = namedFields.size() - 1; i >= 0; i--) {
            final JBBPNamedFieldInfo f = namedFields.get(i);
            if (f.getFieldOffsetInCompiledBlock() <= currentClosedStructure.startStructureOffset) {
              break;
            }
            final String newFullName = normalizedName + '.' + f.getFieldPath();
            namedFields.set(i, new JBBPNamedFieldInfo(newFullName, f.getFieldName(), f.getFieldOffsetInCompiledBlock()));
          }
        }
      }
    }

    if (!structureStack.isEmpty()) {
      throw new JBBPCompilationException("Detected nonclosed " + structureStack.size() + " structure(s)");
    }

    final byte[] compiledBlock = out.toByteArray();

    if (fieldUnrestrictedArrayOffset >= 0) {
      compiledBlock[fieldUnrestrictedArrayOffset] = (byte) (compiledBlock[fieldUnrestrictedArrayOffset] & ~FLAG_ARRAY);
    }

    return builder
            .setNamedFieldData(namedFields)
            .setArraySizeEvaluators(varLengthEvaluators)
            .setCompiledData(compiledBlock)
            .setHasVarFields(hasVarFields)
            .build();
  }

  /**
   * The Method checks a value for negative.
   *
   * @param value a value to be checked
   * @param token the tokens related to the value
   * @throws JBBPCompilationException if the value is a negative one
   */
  private static void assertNonNegativeValue(final int value, final JBBPToken token) {
    if (value < 0) {
      throw new JBBPCompilationException("Detected unsupported negative value for a field must have only zero or a positive one", token);
    }
  }

  /**
   * The Method check that a field name supports business rules.
   *
   * @param name the name to be checked
   * @param token the token contains the name
   * @throws JBBPCompilationException if the name doesn't support business rules
   */
  private static void assertName(final String name, final JBBPToken token) {
    if (name.indexOf('.') >= 0) {
      throw new JBBPCompilationException("Detected disallowed char '.' in name [" + name + ']', token);
    }
  }

  /**
   * Register a name field info item in a named field list.
   *
   * @param normalizedName normalized name of the named field
   * @param offset the named field offset
   * @param namedFields the named field info list for registration
   * @param token the token for the field
   * @throws JBBPCompilationException if there is already a registered field for
   * the path
   */
  private static void registerNamedField(final String normalizedName, final int structureBorder, final int offset, final List<JBBPNamedFieldInfo> namedFields, final JBBPToken token) {
    for (int i = namedFields.size() - 1; i >= structureBorder; i--) {
      final JBBPNamedFieldInfo info = namedFields.get(i);
      if (info.getFieldPath().equals(normalizedName)) {
        throw new JBBPCompilationException("Duplicated named field detected [" + normalizedName + ']', token);
      }
    }
    namedFields.add(new JBBPNamedFieldInfo(normalizedName, normalizedName, offset));
  }

  /**
   * Write an integer value in packed form into an output stream.
   *
   * @param out the output stream to be used to write the value into
   * @param value the value to be written into the output stream
   * @return the length of packed data in bytes
   * @throws IOException it will be thrown for any IO problems
   */
  private static int writePackedInt(final OutputStream out, final int value) throws IOException {
    final byte[] packedInt = JBBPUtils.packInt(value);
    out.write(packedInt);
    return packedInt.length;
  }

  /**
   * The Method prepares a byte-code for a token field type and modifiers.
   *
   * @param token a token to be processed, must not be null
   * @return the prepared byte code for the token
   */
  private static int prepareCodeForToken(final JBBPToken token) {
    int result = -1;
    switch (token.getType()) {
      case ATOM: {
        final JBBPFieldTypeParameterContainer descriptor = token.getFieldTypeParameters();

        result = descriptor.getByteOrder() == JBBPByteOrder.LITTLE_ENDIAN ? FLAG_LITTLE_ENDIAN : 0;
        result |= token.getArraySizeAsString() == null ? 0 : (token.isVarArrayLength() ? FLAG_ARRAY | FLAG_EXPRESSION_OR_WHOLESTREAM : FLAG_ARRAY);
        result |= token.getFieldName() == null ? 0 : FLAG_NAMED;

        final String name = descriptor.getTypeName().toLowerCase(Locale.ENGLISH);
        if ("skip".equals(name)) {
          result |= CODE_SKIP;
        }
        else if ("align".equals(name)) {
          result |= CODE_ALIGN;
        }
        else if ("bit".equals(name)) {
          result |= CODE_BIT;
        }
        else if ("var".equals(name)) {
          result |= CODE_VAR;
        }
        else if ("bool".equals(name)) {
          result |= CODE_BOOL;
        }
        else if ("ubyte".equals(name)) {
          result |= CODE_UBYTE;
        }
        else if ("byte".equals(name)) {
          result |= CODE_BYTE;
        }
        else if ("ushort".equals(name)) {
          result |= CODE_USHORT;
        }
        else if ("short".equals(name)) {
          result |= CODE_SHORT;
        }
        else if ("int".equals(name)) {
          result |= CODE_INT;
        }
        else if ("long".equals(name)) {
          result |= CODE_LONG;
        }
        else if ("bcd".equals(name)) {
          result |= CODE_BCD;
        }
        else if ("reset$$".equals(name)) {
          result |= CODE_RESET_COUNTER;
        }
        else {
          throw new JBBPCompilationException("Unsupported type [" + descriptor.getTypeName() + ']', token);
        }
      }
      break;
      case COMMENT: {
        // doesn't contain code
      }
      break;
      case STRUCT_START: {
        result = token.getArraySizeAsString() == null ? 0 : (token.isVarArrayLength() ? FLAG_ARRAY | FLAG_EXPRESSION_OR_WHOLESTREAM : FLAG_ARRAY);
        result |= token.getFieldName() == null ? 0 : FLAG_NAMED;
        result |= CODE_STRUCT_START;
      }
      break;
      case STRUCT_END: {
        result = CODE_STRUCT_END;
      }
      break;
      default:
        throw new Error("Unsupported type detected, contact developer! [" + token.getType() + ']');
    }
    return result;
  }
}
