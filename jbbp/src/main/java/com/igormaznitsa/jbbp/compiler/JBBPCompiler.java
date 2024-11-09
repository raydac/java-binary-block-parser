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

package com.igormaznitsa.jbbp.compiler;

import com.igormaznitsa.jbbp.JBBPCustomFieldTypeProcessor;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPFieldTypeParameterContainer;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPToken;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPTokenizer;
import com.igormaznitsa.jbbp.compiler.varlen.JBBPEvaluatorFactory;
import com.igormaznitsa.jbbp.compiler.varlen.JBBPIntegerValueEvaluator;
import com.igormaznitsa.jbbp.exceptions.JBBPCompilationException;
import com.igormaznitsa.jbbp.exceptions.JBBPException;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.model.JBBPFieldDouble;
import com.igormaznitsa.jbbp.model.JBBPFieldFloat;
import com.igormaznitsa.jbbp.model.JBBPFieldString;
import com.igormaznitsa.jbbp.model.JBBPFieldUInt;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Class implements the compiler of a bin source script represented as a
 * text value into byte codes.
 *
 * @since 1.0
 */
public final class JBBPCompiler {

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
   * The Byte code shows that field should be processed by custom field type
   * processor.
   */
  public static final int CODE_CUSTOMTYPE = 0x0F;
  /**
   * The Byte-Code Flag shows that the field is a named one.
   */
  public static final int FLAG_NAMED = 0x10;
  /**
   * The Byte-Code Flag shows that the field is an array, but it must be omitted
   * for unlimited field arrays.
   */
  public static final int FLAG_ARRAY = 0x20;
  /**
   * The Byte-Code Flag shows that a multibyte field must be decoded as
   * Little-endian one.
   */
  public static final int FLAG_LITTLE_ENDIAN = 0x40;
  /**
   * The Flag shows that the byte code is wide and contains extra byte in the
   * next position of compiled block.
   */
  public static final int FLAG_WIDE = 0x80;

  /**
   * The flag (placed only in the second byte of wide codes) shows that the
   * field is an array which calculated size or unlimited and must be read till
   * the end of a stream.
   */
  public static final int EXT_FLAG_EXPRESSION_OR_WHOLESTREAM = 0x01;

  /**
   * The flag shows that the extra numeric value for field should be recognized
   * not as number but as expression.
   */
  public static final int EXT_FLAG_EXTRA_AS_EXPRESSION = 0x02;

  /**
   * The Flag shows that the type of data should be recognized differently.
   * as float (if int), as double (if long), as virtual value (if skip).
   *
   * @since 1.4.0
   */
  public static final int EXT_FLAG_EXTRA_DIFF_TYPE = 0x04;

  public static JBBPCompiledBlock compile(final String script) throws IOException {
    return compile(script, null);
  }

  private static void assertTokenNotArray(final String fieldType, final JBBPToken token) {
    if (token.getArraySizeAsString() != null) {
      final String fieldName = token.getFieldName() == null ? "<ANONYM>" : token.getFieldName();
      throw new JBBPCompilationException('\'' + fieldType + "' can't be array (" + fieldName + ')',
          token);
    }
  }

  private static void assertTokenNamed(final String fieldType, final JBBPToken token) {
    if (token.getFieldName() == null) {
      throw new JBBPCompilationException('\'' + fieldType + "' must be named", token);
    }
  }

  private static void assertTokenNotNamed(final String fieldType, final JBBPToken token) {
    if (token.getFieldName() != null) {
      throw new JBBPCompilationException(
          '\'' + fieldType + "' must not be named (" + token.getFieldName() + ')', token);
    }
  }

  private static void assertTokenHasExtraData(final String fieldType, final JBBPToken token) {
    if (token.getFieldTypeParameters().getExtraData() == null) {
      throw new JBBPCompilationException('\'' + fieldType + "' doesn't have extra value", token);
    }
  }

  /**
   * Compile a text script into its byte code representation/
   *
   * @param script                   a text script to be compiled, must not be null.
   * @param customTypeFieldProcessor processor to process custom type fields,
   *                                 can be null
   * @return a compiled block for the script.
   * @throws IOException   it will be thrown for an inside IO error.
   * @throws JBBPException it will be thrown for any logical or work exception
   *                       for the parser and compiler
   */
  public static JBBPCompiledBlock compile(final String script,
                                          final JBBPCustomFieldTypeProcessor customTypeFieldProcessor)
      throws IOException {
    JBBPUtils.assertNotNull(script, "Script must not be null");

    final JBBPCompiledBlock.Builder builder = JBBPCompiledBlock.prepare().setSource(script);

    final List<JBBPNamedFieldInfo> namedFields = new ArrayList<>();
    final List<JBBPFieldTypeParameterContainer> customTypeFields = new ArrayList<>();
    final List<JBBPIntegerValueEvaluator> varLengthEvaluators = new ArrayList<>();

    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    int offset = 0;

    final List<JBBPCompiler.StructStackItem> structureStack = new ArrayList<>();
    final JBBPTokenizer parser = new JBBPTokenizer(script, customTypeFieldProcessor);

    int fieldUnrestrictedArrayOffset = -1;

    boolean hasVarFields = false;

    for (final JBBPToken token : parser) {
      if (token.isComment()) {
        continue;
      }

      final int code = prepareCodeForToken(token, customTypeFieldProcessor);
      final int startFieldOffset = offset;

      final int extraCode = code >>> 8;

      out.write(code);
      offset++;

      if ((code & FLAG_WIDE) != 0) {
        out.write(extraCode);
        offset++;
      }

      StructStackItem currentClosedStructure = null;
      boolean writeExtraFieldNumberInCompiled = false;
      int extraFieldNumberAsInt = -1;
      int customTypeFieldIndex = -1;

      final boolean extraFieldNumericDataAsExpression =
          ((code >>> 8) & EXT_FLAG_EXTRA_AS_EXPRESSION) != 0;
      final boolean fieldTypeDiff = ((code >>> 8) & EXT_FLAG_EXTRA_DIFF_TYPE) != 0;

      switch (code & 0xF) {
        case CODE_BOOL:
        case CODE_BYTE:
        case CODE_UBYTE:
        case CODE_SHORT:
        case CODE_USHORT:
        case CODE_INT:
        case CODE_CUSTOMTYPE:
        case CODE_LONG: {
          if ((code & 0x0F) == CODE_CUSTOMTYPE) {
            if (extraFieldNumericDataAsExpression) {
              varLengthEvaluators.add(JBBPEvaluatorFactory.getInstance()
                  .make(token.getFieldTypeParameters().getExtraDataExpression(), namedFields,
                      out.toByteArray()));
            } else {
              final String extraDataAsStr = token.getFieldTypeParameters().getExtraData();
              if (extraDataAsStr == null) {
                extraFieldNumberAsInt = 0;
              } else {
                try {
                  extraFieldNumberAsInt = Integer.parseInt(extraDataAsStr);
                } catch (NumberFormatException ex) {
                  throw new JBBPCompilationException("Can't parse extra data, must be numeric",
                      token);
                }
              }
              writeExtraFieldNumberInCompiled = true;
            }
            if (customTypeFieldProcessor
                .isAllowed(token.getFieldTypeParameters(), token.getFieldName(),
                    extraFieldNumberAsInt, token.isArray())) {
              customTypeFieldIndex = customTypeFields.size();
              customTypeFields.add(token.getFieldTypeParameters());
            } else {
              throw new JBBPCompilationException("Illegal parameters for custom type field", token);
            }
          }
        }
        break;
        case CODE_SKIP: {
          if (fieldTypeDiff) {
            assertTokenNotArray("val", token);
            assertTokenNamed("val", token);
            assertTokenHasExtraData("val", token);
          } else {
            assertTokenNotArray("skip", token);
            assertTokenNotNamed("skip", token);
          }
          if (extraFieldNumericDataAsExpression) {
            varLengthEvaluators.add(JBBPEvaluatorFactory.getInstance()
                .make(token.getFieldTypeParameters().getExtraDataExpression(), namedFields,
                    out.toByteArray()));
          } else {
            final String extraNumberAsStr = token.getFieldTypeParameters().getExtraData();
            writeExtraFieldNumberInCompiled = true;
            if (extraNumberAsStr == null) {
              extraFieldNumberAsInt = 1;
            } else {
              try {
                extraFieldNumberAsInt = Integer.parseInt(extraNumberAsStr);
                if (!fieldTypeDiff) {
                  assertNonNegativeValue(extraFieldNumberAsInt, token);
                }
              } catch (final NumberFormatException ex) {
                extraFieldNumberAsInt = -1;
              }
            }
          }
        }
        break;
        case CODE_ALIGN: {
          assertTokenNotArray("align", token);
          assertTokenNotNamed("align", token);

          if (extraFieldNumericDataAsExpression) {
            varLengthEvaluators.add(JBBPEvaluatorFactory.getInstance()
                .make(token.getFieldTypeParameters().getExtraDataExpression(), namedFields,
                    out.toByteArray()));
          } else {
            final String extraNumberAsStr = token.getFieldTypeParameters().getExtraData();
            writeExtraFieldNumberInCompiled = true;
            if (extraNumberAsStr == null) {
              extraFieldNumberAsInt = 1;
            } else {
              try {
                extraFieldNumberAsInt = Integer.parseInt(extraNumberAsStr);
                assertNonNegativeValue(extraFieldNumberAsInt, token);
              } catch (NumberFormatException ex) {
                extraFieldNumberAsInt = -1;
              }
              if (extraFieldNumberAsInt <= 0) {
                throw new JBBPCompilationException("'align' size must be greater than zero [" +
                    token.getFieldTypeParameters().getExtraData() + ']', token);
              }
            }
          }
        }
        break;
        case CODE_BIT: {
          if (extraFieldNumericDataAsExpression) {
            varLengthEvaluators.add(JBBPEvaluatorFactory.getInstance()
                .make(token.getFieldTypeParameters().getExtraDataExpression(), namedFields,
                    out.toByteArray()));
          } else {
            final String extraFieldNumAsStr = token.getFieldTypeParameters().getExtraData();
            writeExtraFieldNumberInCompiled = true;
            if (extraFieldNumAsStr == null) {
              extraFieldNumberAsInt = 1;
            } else {
              try {
                extraFieldNumberAsInt = Integer.parseInt(extraFieldNumAsStr);
                assertNonNegativeValue(extraFieldNumberAsInt, token);
              } catch (NumberFormatException ex) {
                extraFieldNumberAsInt = -1;
              }
              if (extraFieldNumberAsInt < 1 || extraFieldNumberAsInt > 8) {
                throw new JBBPCompilationException(
                    "Bit-width must be 1..8 [" + token.getFieldTypeParameters().getExtraData() +
                        ']', token);
              }
            }
          }
        }
        break;
        case CODE_VAR: {
          hasVarFields = true;
          if (extraFieldNumericDataAsExpression) {
            varLengthEvaluators.add(JBBPEvaluatorFactory.getInstance()
                .make(token.getFieldTypeParameters().getExtraDataExpression(), namedFields,
                    out.toByteArray()));
          } else {
            final String extraFieldNumStr = token.getFieldTypeParameters().getExtraData();
            writeExtraFieldNumberInCompiled = true;
            if (extraFieldNumStr == null) {
              extraFieldNumberAsInt = 0;
            } else {
              try {
                extraFieldNumberAsInt = Integer.parseInt(extraFieldNumStr);
              } catch (NumberFormatException ex) {
                throw new JBBPCompilationException(
                    "Can't parse the extra value of a VAR field, must be integer [" +
                        token.getFieldTypeParameters().getExtraData() + ']', token);
              }
            }
          }
        }
        break;
        case CODE_RESET_COUNTER: {
          assertTokenNotArray("Reset counter", token);
          assertTokenNotNamed("Reset counter", token);
          assertTokenHasNotExtraData("Reset counter", token);
        }
        break;
        case CODE_STRUCT_START: {
          final boolean arrayReadTillEnd =
              (code & FLAG_ARRAY) != 0 && (extraCode & EXT_FLAG_EXPRESSION_OR_WHOLESTREAM) != 0 &&
                  "_".equals(token.getArraySizeAsString());
          structureStack.add(new StructStackItem(
              namedFields.size() + ((code & JBBPCompiler.FLAG_NAMED) == 0 ? 0 : 1),
              startFieldOffset, arrayReadTillEnd, code, token));
        }
        break;
        case CODE_STRUCT_END: {
          if (structureStack.isEmpty()) {
            throw new JBBPCompilationException("Detected structure close tag without opening one",
                token);
          } else {
            if (fieldUnrestrictedArrayOffset >= 0) {
              final StructStackItem startOfStruct = structureStack.get(structureStack.size() - 1);
              if (startOfStruct.arrayToReadTillEndOfStream &&
                  fieldUnrestrictedArrayOffset != startOfStruct.startStructureOffset) {
                throw new JBBPCompilationException(
                    "Detected unlimited array of structures but there is already presented one",
                    token);
              }
            }

            currentClosedStructure = structureStack.remove(structureStack.size() - 1);
            offset += writePackedInt(out, currentClosedStructure.startStructureOffset);
          }
        }
        break;
        default:
          throw new Error(
              "Detected unsupported compiled code, notify the developer please [" + code + ']');
      }

      if ((code & FLAG_ARRAY) == 0) {
        if (structureStack.isEmpty() && (code & 0x0F) != CODE_STRUCT_END &&
            fieldUnrestrictedArrayOffset >= 0) {
          throw new JBBPCompilationException("Detected field defined after unlimited array", token);
        }
      } else {
        if ((extraCode & EXT_FLAG_EXPRESSION_OR_WHOLESTREAM) != 0) {
          if ("_".equals(token.getArraySizeAsString())) {
            if (fieldUnrestrictedArrayOffset >= 0) {
              throw new JBBPCompilationException(
                  "Detected two or more unlimited arrays [" + script + ']', token);
            } else {
              fieldUnrestrictedArrayOffset = startFieldOffset;
            }
          } else {
            varLengthEvaluators.add(JBBPEvaluatorFactory.getInstance()
                .make(token.getArraySizeAsString(), namedFields, out.toByteArray()));
          }
        } else {
          final int fixedArraySize = token.getArraySizeAsInt();
          if (fixedArraySize <= 0) {
            throw new JBBPCompilationException(
                "Detected an array with negative or zero fixed length", token);
          }
          offset += writePackedInt(out, fixedArraySize);
        }
      }

      if (writeExtraFieldNumberInCompiled) {
        offset += writePackedInt(out, extraFieldNumberAsInt);
      }

      if (customTypeFieldIndex >= 0) {
        offset += writePackedInt(out, customTypeFieldIndex);
      }

      if ((code & FLAG_NAMED) != 0) {
        final String normalizedName = JBBPUtils.normalizeFieldNameOrPath(token.getFieldName());
        assertName(normalizedName, token);
        registerNamedField(normalizedName, structureStack.isEmpty() ? 0 :
                structureStack.get(structureStack.size() - 1).namedFieldCounter, startFieldOffset,
            namedFields, token);
      } else {
        if (currentClosedStructure != null && (currentClosedStructure.code & FLAG_NAMED) != 0) {
          // it is structure, process field names
          final String normalizedName =
              JBBPUtils.normalizeFieldNameOrPath(currentClosedStructure.token.getFieldName());
          for (int i = namedFields.size() - 1; i >= 0; i--) {
            final JBBPNamedFieldInfo f = namedFields.get(i);
            if (f.getFieldOffsetInCompiledBlock() <= currentClosedStructure.startStructureOffset) {
              break;
            }
            final String newFullName = normalizedName + '.' + f.getFieldPath();
            namedFields.set(i, new JBBPNamedFieldInfo(newFullName, f.getFieldName(),
                f.getFieldOffsetInCompiledBlock()));
          }
        }
      }
    }

    if (!structureStack.isEmpty()) {
      throw new JBBPCompilationException(
          "Detected non-closed " + structureStack.size() + " structure(s)");
    }

    final byte[] compiledBlock = out.toByteArray();

    if (fieldUnrestrictedArrayOffset >= 0) {
      compiledBlock[fieldUnrestrictedArrayOffset] =
          (byte) (compiledBlock[fieldUnrestrictedArrayOffset] & ~FLAG_ARRAY);
    }

    return builder
        .setNamedFieldData(namedFields)
        .setArraySizeEvaluators(varLengthEvaluators)
        .setCustomTypeFields(customTypeFields)
        .setCompiledData(compiledBlock)
        .setHasVarFields(hasVarFields)
        .build();
  }

  private static void assertTokenHasNotExtraData(final String fieldType,
                                                 final JBBPToken token) {
    if (token.getFieldTypeParameters().getExtraData() != null) {
      throw new JBBPCompilationException('\'' + fieldType + "' has extra value", token);
    }
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
      throw new JBBPCompilationException(
          "Detected unsupported negative value for a field must have only zero or a positive one",
          token);
    }
  }

  /**
   * The Method check that a field name supports business rules.
   *
   * @param name  the name to be checked
   * @param token the token contains the name
   * @throws JBBPCompilationException if the name doesn't support business rules
   */
  private static void assertName(final String name, final JBBPToken token) {
    if (name.indexOf('.') >= 0) {
      throw new JBBPCompilationException("Detected disallowed char '.' in name [" + name + ']',
          token);
    }
  }

  /**
   * Register a name field info item in a named field list.
   *
   * @param normalizedName normalized name of the named field
   * @param offset         the named field offset
   * @param namedFields    the named field info list for registration
   * @param token          the token for the field
   * @throws JBBPCompilationException if there is already a registered field for
   *                                  the path
   */
  private static void registerNamedField(final String normalizedName, final int structureBorder,
                                         final int offset,
                                         final List<JBBPNamedFieldInfo> namedFields,
                                         final JBBPToken token) {
    for (int i = namedFields.size() - 1; i >= structureBorder; i--) {
      final JBBPNamedFieldInfo info = namedFields.get(i);
      if (info.getFieldPath().equals(normalizedName)) {
        throw new JBBPCompilationException(
            "Duplicated named field detected [" + normalizedName + ']', token);
      }
    }
    namedFields.add(new JBBPNamedFieldInfo(normalizedName, normalizedName, offset));
  }

  /**
   * Write an integer value in packed form into an output stream.
   *
   * @param out   the output stream to be used to write the value into
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
   * @param token                    a token to be processed, must not be null
   * @param customTypeFieldProcessor custom type field processor for the parser,
   *                                 it can be null
   * @return the prepared byte code for the token
   */
  private static int prepareCodeForToken(final JBBPToken token,
                                         final JBBPCustomFieldTypeProcessor customTypeFieldProcessor) {
    int result = -1;
    switch (token.getType()) {
      case ATOM: {
        final JBBPFieldTypeParameterContainer descriptor = token.getFieldTypeParameters();

        result = descriptor.getByteOrder() == JBBPByteOrder.LITTLE_ENDIAN ? FLAG_LITTLE_ENDIAN : 0;

        final boolean hasExpressionAsExtraNumber = descriptor.hasExpressionAsExtraData();

        result |= token.getArraySizeAsString() == null ? 0 : (token.isVarArrayLength() ?
            FLAG_ARRAY | FLAG_WIDE | (EXT_FLAG_EXPRESSION_OR_WHOLESTREAM << 8) : FLAG_ARRAY);
        result |= hasExpressionAsExtraNumber ? FLAG_WIDE | (EXT_FLAG_EXTRA_AS_EXPRESSION << 8) : 0;
        result |= token.getFieldTypeParameters().isSpecialField() ?
            FLAG_WIDE | (EXT_FLAG_EXTRA_DIFF_TYPE << 8) : 0;
        result |= token.getFieldName() == null ? 0 : FLAG_NAMED;

        final String name = descriptor.getTypeName().toLowerCase(Locale.ENGLISH);
        switch (name) {
          case "skip":
          case "val":
            result |= CODE_SKIP;
            break;
          case "align":
            result |= CODE_ALIGN;
            break;
          case "bit":
            result |= CODE_BIT;
            break;
          case "var":
            result |= CODE_VAR;
            break;
          case "bool":
          case JBBPFieldString.TYPE_NAME:
            result |= CODE_BOOL;
            break;
          case "ubyte":
            result |= CODE_UBYTE;
            break;
          case "byte":
          case JBBPFieldUInt.TYPE_NAME:
            result |= CODE_BYTE;
            break;
          case "ushort":
            result |= CODE_USHORT;
            break;
          case "short":
            result |= CODE_SHORT;
            break;
          case "int":
          case JBBPFieldFloat.TYPE_NAME:
            result |= CODE_INT;
            break;
          case "long":
          case JBBPFieldDouble.TYPE_NAME:
            result |= CODE_LONG;
            break;
          case "reset$$":
            result |= CODE_RESET_COUNTER;
            break;
          default:
            boolean unsupportedType = true;
            if (customTypeFieldProcessor != null) {
              for (final String s : customTypeFieldProcessor.getCustomFieldTypes()) {
                if (name.equals(s)) {
                  result |= CODE_CUSTOMTYPE;
                  unsupportedType = false;
                  break;
                }
              }
            }
            if (unsupportedType) {
              throw new JBBPCompilationException(
                  "Unsupported type [" + descriptor.getTypeName() + ']', token);
            }
            break;
        }
      }
      break;
      case COMMENT: {
        // doesn't contain code
      }
      break;
      case STRUCT_START: {
        result = token.getArraySizeAsString() == null ? 0 : (token.isVarArrayLength() ?
            FLAG_ARRAY | FLAG_WIDE | (EXT_FLAG_EXPRESSION_OR_WHOLESTREAM << 8) : FLAG_ARRAY);
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
     * Flag shows that the structure is array which should be read till end of stream
     */
    private final boolean arrayToReadTillEndOfStream;

    /**
     * The Constructor.
     *
     * @param namedFieldCounter    the named field counter value for the structure
     *                             start
     * @param startStructureOffset the offset of the start structure byte-code
     *                             instruction
     * @param arrayToReadTillEnd   if true then it is an array to read till end
     * @param code                 the start byte code
     * @param token                the token
     */
    private StructStackItem(final int namedFieldCounter, final int startStructureOffset,
                            final boolean arrayToReadTillEnd, final int code,
                            final JBBPToken token) {
      this.namedFieldCounter = namedFieldCounter;
      this.arrayToReadTillEndOfStream = arrayToReadTillEnd;
      this.startStructureOffset = startStructureOffset;
      this.code = code;
      this.token = token;
    }
  }
}
