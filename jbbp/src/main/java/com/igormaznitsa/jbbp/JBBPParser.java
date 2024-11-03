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

package com.igormaznitsa.jbbp;

import static com.igormaznitsa.jbbp.io.JBBPArraySizeLimiter.NO_LIMIT_FOR_ARRAY_SIZE;
import static com.igormaznitsa.jbbp.utils.JBBPUtils.ARRAY_FIELD_EMPTY;

import com.igormaznitsa.jbbp.compiler.JBBPCompiledBlock;
import com.igormaznitsa.jbbp.compiler.JBBPCompiler;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.compiler.conversion.JBBPToJavaConverter;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPFieldTypeParameterContainer;
import com.igormaznitsa.jbbp.compiler.varlen.JBBPIntegerValueEvaluator;
import com.igormaznitsa.jbbp.exceptions.JBBPParsingException;
import com.igormaznitsa.jbbp.exceptions.JBBPReachedArraySizeLimitException;
import com.igormaznitsa.jbbp.io.JBBPArraySizeLimiter;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
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
import com.igormaznitsa.jbbp.utils.JBBPIntCounter;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import com.igormaznitsa.jbbp.utils.TargetSources;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * the Main class allows a user to parse a binary stream or block for predefined
 * and precompiled script.
 *
 * @since 1.0
 */
@SuppressWarnings({"WeakerAccess"})
public final class JBBPParser {

  /**
   * Flag shows that if EOF and not whole packet has been read then remaining fields will be just ignored without exception.
   */
  public static final int FLAG_SKIP_REMAINING_FIELDS_IF_EOF = 1;
  /**
   * Flag to recognize negative expression results by zero.
   *
   * @since 1.4.0
   */
  public static final int FLAG_NEGATIVE_EXPRESSION_RESULT_AS_ZERO = 2;
  /**
   * Empty structure array
   */
  private static final JBBPFieldStruct[] EMPTY_STRUCT_ARRAY = new JBBPFieldStruct[0];
  /**
   * the Compiled block contains compiled script and extra information.
   */
  private final JBBPCompiledBlock compiledBlock;
  /**
   * The Bit order for stream operations.
   */
  private final JBBPBitOrder bitOrder;
  /**
   * Special flags for parsing process.
   */
  private final int flags;
  /**
   * Custom field type processor for the parser, it can be null.
   */
  private final JBBPCustomFieldTypeProcessor customFieldTypeProcessor;
  /**
   * The Variable contains the last parsing counter value.
   */
  private long finalStreamByteCounter;

  /**
   * Constructor.
   *
   * @param source                   the source script to parse binary blocks and streams, must
   *                                 not be null
   * @param bitOrder                 the bit order for bit reading operations, must not be null
   * @param customFieldTypeProcessor custom field type processor for the parser instance, it can be null
   * @param flags                    special flags for parsing process
   * @see #FLAG_SKIP_REMAINING_FIELDS_IF_EOF
   */
  private JBBPParser(final String source, final JBBPBitOrder bitOrder,
                     final JBBPCustomFieldTypeProcessor customFieldTypeProcessor, final int flags) {
    JBBPUtils.assertNotNull(source, "Script is null");
    JBBPUtils.assertNotNull(bitOrder, "Bit order is null");
    this.customFieldTypeProcessor = customFieldTypeProcessor;
    this.bitOrder = bitOrder;
    this.flags = flags;
    try {
      this.compiledBlock = JBBPCompiler.compile(source, customFieldTypeProcessor);
    } catch (IOException ex) {
      throw new RuntimeException("Can't compile script for unexpected IOException", ex);
    }
  }

  /**
   * Ensure that an array length is not a negative one.
   *
   * @param length the array length to be checked
   * @param name   the name information of a field, it can be null
   */
  private static void assertArrayLength(final int length, final JBBPNamedFieldInfo name) {
    if (length < 0) {
      throw new JBBPParsingException("Detected negative calculated array length for field '" +
          (name == null ? "<NO NAME>" : name.getFieldPath()) + "' [" + JBBPUtils.int2msg(length) +
          ']');
    }
  }

  /**
   * Prepare a parser for a script and a bit order.
   *
   * @param script   a text script contains field order and types reference, it must not be null
   * @param bitOrder the bit order for reading operations, it must not be null
   * @return the prepared parser for the script
   * @see JBBPBitOrder#LSB0
   * @see JBBPBitOrder#MSB0
   */
  public static JBBPParser prepare(final String script, final JBBPBitOrder bitOrder) {
    return new JBBPParser(script, bitOrder, null, 0);
  }

  /**
   * Prepare a parser for a script with defined bit order and special flags.
   *
   * @param script   a text script contains field order and types reference, it
   *                 must not be null
   * @param bitOrder the bit order for reading operations, it must not be null
   * @param flags    special flags for parsing
   * @return the prepared parser for the script
   * @see JBBPBitOrder#LSB0
   * @see JBBPBitOrder#MSB0
   * @see #FLAG_SKIP_REMAINING_FIELDS_IF_EOF
   * @since 1.1
   */
  public static JBBPParser prepare(final String script, final JBBPBitOrder bitOrder,
                                   final int flags) {
    return new JBBPParser(script, bitOrder, null, flags);
  }

  /**
   * Prepare a parser for a script with defined bit order and special flags.
   *
   * @param script                   a text script contains field order and types reference, it
   *                                 must not be null
   * @param bitOrder                 the bit order for reading operations, it must not be null
   * @param customFieldTypeProcessor custom field type processor, it can be null
   * @param flags                    special flags for parsing
   * @return the prepared parser for the script
   * @see JBBPBitOrder#LSB0
   * @see JBBPBitOrder#MSB0
   * @see #FLAG_SKIP_REMAINING_FIELDS_IF_EOF
   * @since 1.1.1
   */
  public static JBBPParser prepare(final String script, final JBBPBitOrder bitOrder,
                                   final JBBPCustomFieldTypeProcessor customFieldTypeProcessor,
                                   final int flags) {
    return new JBBPParser(script, bitOrder, customFieldTypeProcessor, flags);
  }

  /**
   * Prepare a parser for a script with default bit order (LSB0) use.
   *
   * @param script a text script contains field order and types reference, it must not be null
   * @return the prepared parser for the script
   * @see JBBPBitOrder#LSB0
   */
  public static JBBPParser prepare(final String script) {
    return JBBPParser.prepare(script, JBBPBitOrder.LSB0);
  }

  /**
   * Prepare a parser for a script with default bit order (LSB0) use and with defined custom type field processor.
   *
   * @param script                   a text script contains field order and types reference, it
   *                                 must not be null
   * @param customFieldTypeProcessor custom field type processor, can be null
   * @return the prepared parser for the script
   * @see JBBPBitOrder#LSB0
   * @since 1.2.0
   */
  public static JBBPParser prepare(final String script,
                                   final JBBPCustomFieldTypeProcessor customFieldTypeProcessor) {
    return JBBPParser.prepare(script, JBBPBitOrder.LSB0, customFieldTypeProcessor, 0);
  }

  /**
   * Prepare a parser for a script with default bit order (LSB0) use and special flags
   *
   * @param script a text script contains field order and types reference, it
   *               must not be null
   * @param flags  special flags for parsing
   * @return the prepared parser for the script
   * @see JBBPBitOrder#LSB0
   * @see #FLAG_SKIP_REMAINING_FIELDS_IF_EOF
   * @since 1.1
   */
  public static JBBPParser prepare(final String script, final int flags) {
    return JBBPParser.prepare(script, JBBPBitOrder.LSB0, flags);
  }

  /**
   * Inside method to parse a structure.
   *
   * @param inStream                      the input stream, must not be null
   * @param positionAtCompiledBlock       the current position in the compiled script
   *                                      block
   * @param varFieldProcessor             a processor to process var fields, it can be null
   *                                      but it will thrown NPE if a var field is met
   * @param namedNumericFieldMap          the named numeric field map
   * @param positionAtNamedFieldList      the current position at the named field
   *                                      list
   * @param positionAtVarLengthProcessors the current position at the variable
   *                                      array length processor list
   * @param arraySizeLimiter              limiter for only whole stream arrays, must not be null
   * @param skipStructureFields           the flag shows that content of fields must be
   *                                      skipped because the structure is skipped
   * @return list of read fields for the structure
   * @throws IOException                        it will be thrown for transport errors
   * @throws JBBPReachedArraySizeLimitException thrown if reached limit for a whole stream array
   */
  private List<JBBPAbstractField> parseStruct(final JBBPBitInputStream inStream,
                                              final JBBPIntCounter positionAtCompiledBlock,
                                              final JBBPVarFieldProcessor varFieldProcessor,
                                              final JBBPNamedNumericFieldMap namedNumericFieldMap,
                                              final JBBPIntCounter positionAtNamedFieldList,
                                              final JBBPIntCounter positionAtVarLengthProcessors,
                                              final JBBPArraySizeLimiter arraySizeLimiter,
                                              final boolean skipStructureFields)
      throws IOException {
    final List<JBBPAbstractField> structureFields = skipStructureFields ? null : new ArrayList<>();
    final byte[] compiled = this.compiledBlock.getCompiledData();

    boolean endStructureNotMet = true;

    while (endStructureNotMet && positionAtCompiledBlock.get() < compiled.length) {
      if (inStream.isArrayLimitDetected() ||
          (!inStream.hasAvailableData() && (flags & FLAG_SKIP_REMAINING_FIELDS_IF_EOF) != 0)) {
        // Break reading because the ignore flag for EOF has been set or reached limit for whole stream array read
        break;
      }

      final int c = compiled[positionAtCompiledBlock.getAndIncrement()] & 0xFF;
      final boolean wideCode = (c & JBBPCompiler.FLAG_WIDE) != 0;
      final int ec = wideCode ? compiled[positionAtCompiledBlock.getAndIncrement()] & 0xFF : 0;
      final boolean extraFieldNumAsExpr = (ec & JBBPCompiler.EXT_FLAG_EXTRA_AS_EXPRESSION) != 0;
      final int code = (ec << 8) | c;
      final boolean fieldTypeDiff = (ec & JBBPCompiler.EXT_FLAG_EXTRA_DIFF_TYPE) != 0;

      final JBBPNamedFieldInfo name = (code & JBBPCompiler.FLAG_NAMED) == 0 ? null :
          compiledBlock.getNamedFields()[positionAtNamedFieldList.getAndIncrement()];
      final JBBPByteOrder byteOrder =
          (code & JBBPCompiler.FLAG_LITTLE_ENDIAN) == 0 ? JBBPByteOrder.BIG_ENDIAN :
              JBBPByteOrder.LITTLE_ENDIAN;

      final boolean resultNotIgnored = !skipStructureFields;

      final int extraFieldNumExprResult;
      if (extraFieldNumAsExpr) {
        final JBBPIntegerValueEvaluator evaluator =
            this.compiledBlock.getArraySizeEvaluators()[positionAtVarLengthProcessors
                .getAndIncrement()];
        int resultOfExpression;
        if (resultNotIgnored) {
          resultOfExpression = evaluator
              .eval(inStream, positionAtCompiledBlock.get(), this.compiledBlock,
                  namedNumericFieldMap);
          if ((this.flags & FLAG_NEGATIVE_EXPRESSION_RESULT_AS_ZERO) != 0) {
            resultOfExpression = Math.max(resultOfExpression, 0);
          }
        } else {
          resultOfExpression = 0;
        }
        extraFieldNumExprResult = resultOfExpression;
      } else {
        extraFieldNumExprResult = 0;
      }

      final boolean wholeStreamArray;
      final int arrayLength;
      final int packedArraySizeOffset;
      switch (code &
          (JBBPCompiler.FLAG_ARRAY | (JBBPCompiler.EXT_FLAG_EXPRESSION_OR_WHOLESTREAM << 8))) {
        case JBBPCompiler.FLAG_ARRAY: {
          final int pos = positionAtCompiledBlock.get();
          arrayLength = JBBPUtils.unpackInt(compiled, positionAtCompiledBlock);
          packedArraySizeOffset = positionAtCompiledBlock.get() - pos;
          wholeStreamArray = false;
        }
        break;
        case (JBBPCompiler.EXT_FLAG_EXPRESSION_OR_WHOLESTREAM << 8): {
          wholeStreamArray = resultNotIgnored;
          packedArraySizeOffset = 0;
          arrayLength = 0;
        }
        break;
        case JBBPCompiler.FLAG_ARRAY | (JBBPCompiler.EXT_FLAG_EXPRESSION_OR_WHOLESTREAM << 8): {
          final JBBPIntegerValueEvaluator evaluator =
              this.compiledBlock.getArraySizeEvaluators()[positionAtVarLengthProcessors
                  .getAndIncrement()];
          int resultOfExpression;
          if (resultNotIgnored) {
            resultOfExpression = evaluator
                .eval(inStream, positionAtCompiledBlock.get(), this.compiledBlock,
                    namedNumericFieldMap);
            if ((this.flags & FLAG_NEGATIVE_EXPRESSION_RESULT_AS_ZERO) != 0) {
              resultOfExpression = Math.max(resultOfExpression, 0);
            }
          } else {
            resultOfExpression = 0;
          }
          arrayLength = resultOfExpression;
          packedArraySizeOffset = 0;
          assertArrayLength(arrayLength, name);
          wholeStreamArray = false;
        }
        break;
        default: {
          // it is not an array, just a single field
          packedArraySizeOffset = 0;
          wholeStreamArray = false;
          arrayLength = -1;
        }
        break;
      }

      JBBPAbstractField singleAtomicField = null;
      try {
        switch (code & 0xF) {
          case JBBPCompiler.CODE_RESET_COUNTER: {
            if (resultNotIgnored) {
              inStream.resetCounter();
            }
          }
          break;
          case JBBPCompiler.CODE_ALIGN: {
            final int alignValue = extraFieldNumAsExpr ? extraFieldNumExprResult :
                JBBPUtils.unpackInt(compiled, positionAtCompiledBlock);
            if (resultNotIgnored) {
              inStream.align(alignValue);
            }
          }
          break;
          case JBBPCompiler.CODE_SKIP: {
            final int skipByteNumber = extraFieldNumAsExpr ? extraFieldNumExprResult :
                JBBPUtils.unpackInt(compiled, positionAtCompiledBlock);
            if (resultNotIgnored) {
              if (fieldTypeDiff) {
                singleAtomicField = new JBBPFieldInt(name, skipByteNumber);
              } else {
                if (skipByteNumber > 0) {
                  final long skippedBytes = inStream.skip(skipByteNumber);
                  if (skippedBytes != skipByteNumber) {
                    throw new EOFException(
                        "Can't skip " + skipByteNumber + " byte(s), skipped only " + skippedBytes +
                            " byte(s)");
                  }
                }
              }
            }
          }
          break;
          case JBBPCompiler.CODE_BIT: {
            final int numberOfBits = extraFieldNumAsExpr ? extraFieldNumExprResult :
                JBBPUtils.unpackInt(compiled, positionAtCompiledBlock);
            if (resultNotIgnored) {
              final JBBPBitNumber bitNumber = JBBPBitNumber.decode(numberOfBits);
              if (arrayLength < 0) {
                final int read = inStream.readBitField(bitNumber);
                singleAtomicField = new JBBPFieldBit(name, read & 0xFF, bitNumber);
              } else {
                structureFields.add(new JBBPFieldArrayBit(name,
                    inStream.readBitsArray(wholeStreamArray ? -1 : arrayLength, bitNumber,
                        arraySizeLimiter),
                    bitNumber));
              }
            }
          }
          break;
          case JBBPCompiler.CODE_VAR: {
            final int extraField = extraFieldNumAsExpr ? extraFieldNumExprResult :
                JBBPUtils.unpackInt(compiled, positionAtCompiledBlock);
            if (resultNotIgnored) {
              if (arrayLength < 0) {
                singleAtomicField = varFieldProcessor
                    .readVarField(inStream, name, extraField, byteOrder, namedNumericFieldMap);
                JBBPUtils.assertNotNull(singleAtomicField,
                    "A Var processor must not return null as a result of a field reading");
                if (singleAtomicField instanceof JBBPAbstractArrayField) {
                  throw new JBBPParsingException(
                      "A Var field processor has returned an array value instead of a field value [" +
                          name + ':' + extraField + ']');
                }
                if (singleAtomicField.getNameInfo() != name) {
                  throw new JBBPParsingException(
                      "Detected wrong name for a read field , must be " + name + " but detected " +
                          singleAtomicField.getNameInfo() + ']');
                }
              } else {
                final JBBPAbstractArrayField<? extends JBBPAbstractField> array = varFieldProcessor
                    .readVarArray(inStream, wholeStreamArray ? -1 : arrayLength, name, extraField,
                        byteOrder, namedNumericFieldMap, arraySizeLimiter);
                JBBPUtils.assertNotNull(array,
                    "A Var processor must not return null as a result of an array field reading [" +
                        name + ':' + extraField + ']');
                if (array.getNameInfo() != name) {
                  throw new JBBPParsingException(
                      "Detected wrong name for a read field array, must be " + name +
                          " but detected " + array.getNameInfo() + ']');
                }
                structureFields.add(array);
              }
            }
          }
          break;
          case JBBPCompiler.CODE_CUSTOMTYPE: {
            final int extraData = extraFieldNumAsExpr ? extraFieldNumExprResult :
                JBBPUtils.unpackInt(compiled, positionAtCompiledBlock);
            if (resultNotIgnored) {
              final JBBPFieldTypeParameterContainer fieldTypeInfo =
                  this.compiledBlock.getCustomTypeFields()[JBBPUtils
                      .unpackInt(compiled, positionAtCompiledBlock)];
              final JBBPAbstractField field = this.customFieldTypeProcessor
                  .readCustomFieldType(inStream, this.bitOrder, this.flags, fieldTypeInfo, name,
                      extraData, wholeStreamArray, arrayLength, arraySizeLimiter);
              JBBPUtils.assertNotNull(field, "Must not return null as read result");

              if (arrayLength < 0) {
                singleAtomicField = field;
              } else {
                structureFields.add(field);
              }
            }
          }
          break;
          case JBBPCompiler.CODE_BYTE: {
            if (resultNotIgnored) {
              if (arrayLength < 0) {
                singleAtomicField = fieldTypeDiff ?
                    new JBBPFieldUInt(name, inStream.readInt(byteOrder) & 0xFFFFFFFFL) :
                    new JBBPFieldByte(name, (byte) inStream.readByte());
              } else {
                structureFields.add(fieldTypeDiff ?
                    new JBBPFieldArrayUInt(name,
                        inStream.readIntArray(wholeStreamArray ? -1 : arrayLength, byteOrder,
                            arraySizeLimiter)) :
                    new JBBPFieldArrayByte(name,
                        inStream.readByteArray(wholeStreamArray ? -1 : arrayLength, byteOrder,
                            arraySizeLimiter)));
              }
            }
          }
          break;
          case JBBPCompiler.CODE_UBYTE: {
            if (resultNotIgnored) {
              if (arrayLength < 0) {
                singleAtomicField = new JBBPFieldUByte(name, (byte) inStream.readByte());
              } else {
                structureFields.add(new JBBPFieldArrayUByte(name,
                    inStream.readByteArray(wholeStreamArray ? -1 : arrayLength, byteOrder,
                        arraySizeLimiter)));
              }
            }
          }
          break;
          case JBBPCompiler.CODE_BOOL: {
            if (resultNotIgnored) {
              if (arrayLength < 0) {
                singleAtomicField =
                    fieldTypeDiff ? new JBBPFieldString(name, inStream.readString(byteOrder)) :
                        new JBBPFieldBoolean(name, inStream.readBoolean());
              } else {
                structureFields.add(fieldTypeDiff ?
                    new JBBPFieldArrayString(name,
                        inStream.readStringArray(wholeStreamArray ? -1 : arrayLength, byteOrder,
                            arraySizeLimiter)) :
                    new JBBPFieldArrayBoolean(name,
                        inStream.readBoolArray(wholeStreamArray ? -1 : arrayLength,
                            arraySizeLimiter))
                );
              }
            }
          }
          break;
          case JBBPCompiler.CODE_INT: {
            if (resultNotIgnored) {
              if (arrayLength < 0) {
                singleAtomicField =
                    fieldTypeDiff ? new JBBPFieldFloat(name, inStream.readFloat(byteOrder)) :
                        new JBBPFieldInt(name, inStream.readInt(byteOrder));
              } else {
                structureFields.add(fieldTypeDiff ?
                    new JBBPFieldArrayFloat(name,
                        inStream.readFloatArray(wholeStreamArray ? -1 : arrayLength, byteOrder,
                            arraySizeLimiter)) :
                    new JBBPFieldArrayInt(name,
                        inStream.readIntArray(wholeStreamArray ? -1 : arrayLength, byteOrder,
                            arraySizeLimiter))
                );
              }
            }
          }
          break;
          case JBBPCompiler.CODE_LONG: {
            if (resultNotIgnored) {
              if (arrayLength < 0) {
                singleAtomicField =
                    fieldTypeDiff ? new JBBPFieldDouble(name, inStream.readDouble(byteOrder)) :
                        new JBBPFieldLong(name, inStream.readLong(byteOrder));
              } else {
                structureFields.add(fieldTypeDiff ?
                    new JBBPFieldArrayDouble(name,
                        inStream.readDoubleArray(wholeStreamArray ? -1 : arrayLength, byteOrder,
                            arraySizeLimiter)) :
                    new JBBPFieldArrayLong(name,
                        inStream.readLongArray(wholeStreamArray ? -1 : arrayLength, byteOrder,
                            arraySizeLimiter))
                );
              }
            }
          }
          break;
          case JBBPCompiler.CODE_SHORT: {
            if (resultNotIgnored) {
              if (arrayLength < 0) {
                final int value = inStream.readUnsignedShort(byteOrder);
                singleAtomicField = new JBBPFieldShort(name, (short) value);
              } else {
                structureFields.add(new JBBPFieldArrayShort(name,
                    inStream.readShortArray(wholeStreamArray ? -1 : arrayLength, byteOrder,
                        arraySizeLimiter)));
              }
            }
          }
          break;
          case JBBPCompiler.CODE_USHORT: {
            if (resultNotIgnored) {
              if (arrayLength < 0) {
                final int value = inStream.readUnsignedShort(byteOrder);
                singleAtomicField = new JBBPFieldUShort(name, (short) value);
              } else {
                structureFields.add(new JBBPFieldArrayUShort(name,
                    inStream.readShortArray(wholeStreamArray ? -1 : arrayLength, byteOrder,
                        arraySizeLimiter)));
              }
            }
          }
          break;
          case JBBPCompiler.CODE_STRUCT_START: {
            if (arrayLength < 0) {
              final List<JBBPAbstractField> structFields =
                  parseStruct(inStream, positionAtCompiledBlock, varFieldProcessor,
                      namedNumericFieldMap, positionAtNamedFieldList, positionAtVarLengthProcessors,
                      arraySizeLimiter, skipStructureFields);
              // skip offset
              JBBPUtils.unpackInt(compiled, positionAtCompiledBlock);
              if (resultNotIgnored) {
                structureFields
                    .add(new JBBPFieldStruct(name, structFields.toArray(ARRAY_FIELD_EMPTY)));
              }
            } else {
              final int nameFieldCurrent = positionAtNamedFieldList.get();
              final int varLenProcCurrent = positionAtVarLengthProcessors.get();

              final JBBPFieldStruct[] result;
              if (resultNotIgnored) {
                if (wholeStreamArray) {
                  // read till the stream end
                  final List<JBBPFieldStruct> list = new ArrayList<>();
                  while (inStream.hasAvailableData()) {
                    positionAtNamedFieldList.set(nameFieldCurrent);
                    positionAtVarLengthProcessors.set(varLenProcCurrent);

                    final List<JBBPAbstractField> fieldsForStruct =
                        parseStruct(inStream, positionAtCompiledBlock, varFieldProcessor,
                            namedNumericFieldMap, positionAtNamedFieldList,
                            positionAtVarLengthProcessors, arraySizeLimiter, skipStructureFields);
                    list.add(new JBBPFieldStruct(name, fieldsForStruct));

                    if (JBBPArraySizeLimiter.isBreakReadWholeStream(list.size(),
                        arraySizeLimiter)) {
                      break;
                    }

                    final int structStart = JBBPUtils.unpackInt(compiled, positionAtCompiledBlock);

                    if (inStream.hasAvailableData()) {
                      positionAtCompiledBlock.set(structStart + (wideCode ? 2 : 1));
                    }
                  }

                  result = list.isEmpty() ? EMPTY_STRUCT_ARRAY : list.toArray(EMPTY_STRUCT_ARRAY);
                } else {
                  // read number of items
                  if (arrayLength == 0) {
                    // skip the structure
                    result = EMPTY_STRUCT_ARRAY;
                    parseStruct(inStream, positionAtCompiledBlock, varFieldProcessor,
                        namedNumericFieldMap, positionAtNamedFieldList,
                        positionAtVarLengthProcessors, arraySizeLimiter, true);
                    JBBPUtils.unpackInt(compiled, positionAtCompiledBlock);
                  } else {
                    result = new JBBPFieldStruct[arrayLength];
                    for (int i = 0; i < arrayLength; i++) {

                      final List<JBBPAbstractField> fieldsForStruct =
                          parseStruct(inStream, positionAtCompiledBlock, varFieldProcessor,
                              namedNumericFieldMap, positionAtNamedFieldList,
                              positionAtVarLengthProcessors, arraySizeLimiter, skipStructureFields);
                      final int structBodyStart =
                          JBBPUtils.unpackInt(compiled, positionAtCompiledBlock);

                      result[i] = new JBBPFieldStruct(name, fieldsForStruct);

                      if (i < arrayLength - 1) {
                        // not the last
                        positionAtNamedFieldList.set(nameFieldCurrent);
                        positionAtVarLengthProcessors.set(varLenProcCurrent);
                        positionAtCompiledBlock
                            .set(structBodyStart + packedArraySizeOffset + (wideCode ? 2 : 1));
                      }
                    }
                  }
                }

                if (result != null) {
                  structureFields.add(new JBBPFieldArrayStruct(name, result));
                }
              } else {
                parseStruct(inStream, positionAtCompiledBlock, varFieldProcessor,
                    namedNumericFieldMap, positionAtNamedFieldList, positionAtVarLengthProcessors,
                    arraySizeLimiter, skipStructureFields);
                JBBPUtils.unpackInt(compiled, positionAtCompiledBlock);
              }
            }
          }
          break;
          case JBBPCompiler.CODE_STRUCT_END: {
            // we just left the method and the caller must process the structure offset start address for the structure
            endStructureNotMet = false;
          }
          break;
          default:
            throw new Error("Detected unexpected field type! Contact developer! [" + code + ']');
        }
      } catch (IOException ex) {
        if (name == null) {
          throw ex;
        } else {
          throw new JBBPParsingException(
              "Can't parse field '" + name.getFieldPath() + "' for IOException", ex);
        }
      }

      if (singleAtomicField != null) {
        structureFields.add(singleAtomicField);
        if (namedNumericFieldMap != null && singleAtomicField instanceof JBBPNumericField &&
            name != null) {
          namedNumericFieldMap.putField((JBBPNumericField) singleAtomicField);
        }
      }

    }

    return structureFields;
  }

  /**
   * Parse an input stream.
   *
   * @param in an input stream which content should be parsed, it must not be
   *           null
   * @return the parsed content as the root structure
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPFieldStruct parse(final InputStream in) throws IOException {
    return this.parse(in, null, null);
  }

  /**
   * Parse am input stream with defined external value provider.
   *
   * @param in                    an input stream which content will be parsed, it must not be null
   * @param varFieldProcessor     a var field processor, it may be null if there is
   *                              not any var field in a script, otherwise NPE will be thrown during parsing
   * @param externalValueProvider an external value provider, it can be null but
   *                              only if the script doesn't have fields desired the provider
   * @return the parsed content as the root structure
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPFieldStruct parse(
      final InputStream in,
      final JBBPVarFieldProcessor varFieldProcessor,
      final JBBPExternalValueProvider externalValueProvider
  ) throws IOException {
    return this.parse(
        in,
        varFieldProcessor,
        externalValueProvider,
        NO_LIMIT_FOR_ARRAY_SIZE
    );
  }

  /**
   * Parse am input stream with defined external value provider.
   *
   * @param in                    an input stream which content will be parsed, it must not be null
   * @param varFieldProcessor     a var field processor, it may be null if there is
   *                              not any var field in a script, otherwise NPE will be thrown during parsing
   * @param externalValueProvider an external value provider, it can be null but
   *                              only if the script doesn't have fields desired the provider
   * @param arraySizeLimiter      limiter to read whole stream arrays, must not be null
   * @return the parsed content as the root structure
   * @throws IOException                        it will be thrown for transport errors
   * @throws JBBPReachedArraySizeLimitException thrown if reached limit for a whole stream array
   * @see JBBPArraySizeLimiter#NO_LIMIT_FOR_ARRAY_SIZE
   * @since 2.1.0
   */
  public JBBPFieldStruct parse(
      final InputStream in,
      final JBBPVarFieldProcessor varFieldProcessor,
      final JBBPExternalValueProvider externalValueProvider,
      final JBBPArraySizeLimiter arraySizeLimiter
  ) throws IOException {
    final JBBPBitInputStream bitInStream =
        in instanceof JBBPBitInputStream ? (JBBPBitInputStream) in :
            new JBBPBitInputStream(in, bitOrder);
    this.finalStreamByteCounter = bitInStream.getCounter();

    final JBBPNamedNumericFieldMap fieldMap;
    if (this.compiledBlock.hasEvaluatedSizeArrays() || this.compiledBlock.hasVarFields()) {
      fieldMap = new JBBPNamedNumericFieldMap(externalValueProvider);
    } else {
      fieldMap = null;
    }

    if (this.compiledBlock.hasVarFields()) {
      JBBPUtils.assertNotNull(varFieldProcessor,
          "The Script contains VAR fields, a var field processor must be provided");
    }
    try {
      return new JBBPFieldStruct(new JBBPNamedFieldInfo("", "", -1),
          parseStruct(bitInStream, new JBBPIntCounter(), varFieldProcessor, fieldMap,
              new JBBPIntCounter(), new JBBPIntCounter(), arraySizeLimiter, false));
    } finally {
      this.finalStreamByteCounter = bitInStream.getCounter();
    }
  }

  /**
   * Get the parse flags.
   *
   * @return the parser flags
   * @see #FLAG_SKIP_REMAINING_FIELDS_IF_EOF
   */
  public int getFlags() {
    return this.flags;
  }

  /**
   * Parse a byte array content.
   *
   * @param array a byte array which content should be parsed, it must not be
   *              null
   * @return the parsed content as the root structure
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPFieldStruct parse(final byte[] array) throws IOException {
    JBBPUtils.assertNotNull(array, "Array must not be null");
    return this.parse(new ByteArrayInputStream(array), null, null);
  }

  /**
   * Parse a byte array content.
   *
   * @param array                 a byte array which content should be parsed, it must not be
   *                              null
   * @param varFieldProcessor     a var field processor, it may be null if there is
   *                              not any var field in a script, otherwise NPE will be thrown during parsing
   * @param externalValueProvider an external value provider, it can be null but
   *                              only if the script doesn't have fields desired the provider
   * @return the parsed content as the root structure
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPFieldStruct parse(final byte[] array, final JBBPVarFieldProcessor varFieldProcessor,
                               final JBBPExternalValueProvider externalValueProvider)
      throws IOException {
    JBBPUtils.assertNotNull(array, "Array must not be null");
    return this.parse(new ByteArrayInputStream(array), varFieldProcessor, externalValueProvider);
  }

  /**
   * Get the final input stream byte counter value for the last parsing
   * operation. It is loaded just after exception or parsing completion. NB: It
   * is appropriate one only if the parsing didn't make any counter reset
   * operation.
   *
   * @return the last parsing byte counter value
   */
  public long getFinalStreamByteCounter() {
    return this.finalStreamByteCounter;
  }

  /**
   * Get compiled block containing compiled information for the parser.
   *
   * @return compiled block, must not be null
   * @since 1.3.0
   */
  public JBBPCompiledBlock getCompiledBlock() {
    return this.compiledBlock;
  }

  /**
   * Convert the prepared parser into sources. It doesn't provide way to define different flag for conversion, it uses default flags for converters and provided for short fast way.
   *
   * @param target target to generate sources, must not be null
   * @param name   name of result, depends on target, must not be null, for instance class name (example 'com.test.jbbp.Parser')
   * @return list of source items generated during operation, must not be null and must not be empty
   * @throws IllegalArgumentException if target is unsupported
   * @see JBBPToJavaConverter
   * @see JBBPToJavaConverter.Builder
   * @since 1.3.0
   */
  public List<ResultSrcItem> convertToSrc(final TargetSources target, final String name) {
    JBBPUtils.assertNotNull(name, "Name must not be null");

    if (target == TargetSources.JAVA) {
      final Properties metadata = new Properties();
      metadata.setProperty("script", this.compiledBlock.getSource());
      metadata.setProperty("name", name);
      metadata.setProperty("target", target.name());
      metadata.setProperty("converter", JBBPToJavaConverter.class.getCanonicalName());

      final int nameStart = name.lastIndexOf('.');
      final String packageName;
      final String className;
      if (nameStart < 0) {
        packageName = "";
        className = name;
      } else {
        packageName = name.substring(0, nameStart);
        className = name.substring(nameStart + 1);
      }

      final String resultSources =
          JBBPToJavaConverter.makeBuilder(this).setMainClassPackage(packageName)
              .setMainClassName(className).build().convert();
      final Map<String, String> resultMap =
          Collections.singletonMap(name.replace('.', '/') + ".java", resultSources);

      return Collections.singletonList(new ResultSrcItem() {
        @Override
        public Properties getMetadata() {
          return metadata;
        }

        @Override
        public Map<String, String> getResult() {
          return resultMap;
        }
      });
    }
    throw new IllegalArgumentException("Unsupported target : " + target);
  }
}
