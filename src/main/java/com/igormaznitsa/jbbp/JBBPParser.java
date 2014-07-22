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
package com.igormaznitsa.jbbp;

import com.igormaznitsa.jbbp.compiler.*;
import com.igormaznitsa.jbbp.compiler.varlen.JBBPIntegerValueEvaluator;
import com.igormaznitsa.jbbp.exceptions.JBBPParsingException;
import com.igormaznitsa.jbbp.io.*;
import com.igormaznitsa.jbbp.model.*;
import com.igormaznitsa.jbbp.utils.*;
import java.io.*;
import java.util.*;

/**
 * the Main class allows a user to parse a binary stream or block for predefined
 * and precompiled script.
 */
public final class JBBPParser {

  /**
   * the Compiled block contains compiled script and extra information.
   */
  private final JBBPCompiledBlock compiledBlock;
  /**
   * The Bit order for stream operations.
   */
  private final JBBPBitOrder bitOrder;

  /**
   * Empty structure array
   */
  private static final JBBPFieldStruct[] EMPTY_STRUCT_ARRAY = new JBBPFieldStruct[0];

  /**
   * The Constructor.
   *
   * @param source the source script to parse binary blocks and streams, must
   * not be null
   * @param bitOrder the bit order for bit reading operations, must not be null
   */
  private JBBPParser(final String source, final JBBPBitOrder bitOrder) {
    JBBPUtils.assertNotNull(source, "Script is null");
    JBBPUtils.assertNotNull(bitOrder, "Bit order is null");
    this.bitOrder = bitOrder;
    try {
      this.compiledBlock = JBBPCompiler.compile(source);
    }
    catch (IOException ex) {
      throw new RuntimeException("Can't compile script for unexpected IO Exception", ex);
    }
  }

  /**
   * Ensure that an array length is not a negative one.
   *
   * @param length the array length to be checked
   * @param name the name information of a field, it can be null
   */
  private static void assertArrayLength(final int length, final JBBPNamedFieldInfo name) {
    if (length < 0) {
      throw new JBBPParsingException("Detected negative calculated array length for field '" + (name == null ? "<NONAMED>" : name.getFieldPath()) + "\' [" + JBBPUtils.int2msg(length) + ']');
    }
  }

  /**
   * Inside method to parse a structure.
   *
   * @param inStream the input stream, must not be null
   * @param positionAtCompiledBlock the current position in the compiled script
   * block
   * @param varFieldProcessor a processor to process var fields, it can be null
   * but it will thrown NPE if a var field is met
   * @param namedNumericFieldMap the named numeric field map
   * @param positionAtNamedFieldList the current position at the named field
   * list
   * @param positionAtVarLengthProcessors the current position at the variable
   * array length processor list
   * @param skipStructureFields the flag shows that content of fields must be
   * skipped because the structure is skipped
   * @return list of read fields for the structure
   * @throws IOException it will be thrown for transport errors
   */
  private List<JBBPAbstractField> parseStruct(final JBBPBitInputStream inStream, final JBBPIntCounter positionAtCompiledBlock, final JBBPVarFieldProcessor varFieldProcessor, final JBBPNamedNumericFieldMap namedNumericFieldMap, final JBBPIntCounter positionAtNamedFieldList, final JBBPIntCounter positionAtVarLengthProcessors, final boolean skipStructureFields) throws IOException {
    final List<JBBPAbstractField> structureFields = skipStructureFields ? null : new ArrayList<JBBPAbstractField>();
    final byte[] compiled = this.compiledBlock.getCompiledData();

    boolean endStructureNotMet = true;

    while (endStructureNotMet && positionAtCompiledBlock.get() < compiled.length) {
      final int code = compiled[positionAtCompiledBlock.getAndIncrement()] & 0xFF;

      final JBBPNamedFieldInfo name = (code & JBBPCompiler.FLAG_NAMED) == 0 ? null : compiledBlock.getNamedFields()[positionAtNamedFieldList.getAndIncrement()];
      final JBBPByteOrder byteOrder = (code & JBBPCompiler.FLAG_LITTLE_ENDIAN) == 0 ? JBBPByteOrder.BIG_ENDIAN : JBBPByteOrder.LITTLE_ENDIAN;

      final boolean resultNotIgnored = !skipStructureFields;

      final boolean wholeStreamArray;
      final int arrayLength;
      switch (code & (JBBPCompiler.FLAG_ARRAY | JBBPCompiler.FLAG_EXPRESSION_OR_WHOLESTREAM)) {
        case JBBPCompiler.FLAG_ARRAY: {
          arrayLength = JBBPUtils.unpackInt(compiled, positionAtCompiledBlock);
          wholeStreamArray = false;
        }
        break;
        case JBBPCompiler.FLAG_EXPRESSION_OR_WHOLESTREAM: {
          wholeStreamArray = resultNotIgnored;
          arrayLength = 0;
        }
        break;
        case JBBPCompiler.FLAG_ARRAY | JBBPCompiler.FLAG_EXPRESSION_OR_WHOLESTREAM: {
          final JBBPIntegerValueEvaluator evaluator = this.compiledBlock.getArraySizeEvaluators()[positionAtVarLengthProcessors.getAndIncrement()];
          arrayLength = resultNotIgnored ? evaluator.eval(inStream, positionAtCompiledBlock.get(), this.compiledBlock, namedNumericFieldMap) : 0;
          assertArrayLength(arrayLength, name);
          wholeStreamArray = false;
        }
        break;
        default: {
          // it is not an array, just a single field
          wholeStreamArray = false;
          arrayLength = -1;
        }
        break;
      }

      JBBPAbstractField singleAtomicField = null;

      switch (code & 0xF) {
        case JBBPCompiler.CODE_ALIGN: {
          final int alignValue = JBBPUtils.unpackInt(compiled, positionAtCompiledBlock);
          if (resultNotIgnored) {
            inStream.align(alignValue);
          }
        }
        break;
        case JBBPCompiler.CODE_SKIP: {
          final int skipByteNumber = JBBPUtils.unpackInt(compiled, positionAtCompiledBlock);
          if (resultNotIgnored) {
            if (skipByteNumber > 0) {
              final long skippedBytes = inStream.skip(skipByteNumber);
              if (skippedBytes != skipByteNumber) {
                throw new EOFException("Can't skip " + skipByteNumber + " byte(s), skipped only " + skippedBytes + " byte(s)");
              }
            }
          }
        }
        break;
        case JBBPCompiler.CODE_BIT: {
          final int numberOfBits = JBBPUtils.unpackInt(compiled, positionAtCompiledBlock);
          if (resultNotIgnored) {
            final JBBPBitNumber bitNumber = JBBPBitNumber.decode(numberOfBits);
            if (arrayLength < 0) {
              final int read = inStream.readBits(bitNumber);
              if (read < 0) {
                throw new EOFException("Can't read bits from stream [" + bitNumber + ']');
              }
              singleAtomicField = new JBBPFieldBit(name, read, bitNumber);
            }
            else {
              structureFields.add(new JBBPFieldArrayBit(name, inStream.readBitsArray(wholeStreamArray ? -1 : arrayLength, bitNumber), bitNumber));
            }
          }
        }
        break;
        case JBBPCompiler.CODE_VAR: {
          final int extraField = JBBPUtils.unpackInt(compiled, positionAtCompiledBlock);
          if (resultNotIgnored) {
            if (arrayLength < 0) {
              singleAtomicField = varFieldProcessor.readVarField(inStream, name, extraField, byteOrder, namedNumericFieldMap);
              JBBPUtils.assertNotNull(singleAtomicField, "A Var processor must not return null as a result of a field reading");
              if (singleAtomicField instanceof JBBPAbstractArrayField) {
                throw new JBBPParsingException("A Var field processor has returned an array value instead of a field value [" + name + ':' + extraField + ']');
              }
              if (singleAtomicField.getNameInfo() != name) {
                throw new JBBPParsingException("Detected wrong name for a read field , must be " + name + " but detected " + singleAtomicField.getNameInfo() + ']');
              }
            }
            else {
              final JBBPAbstractArrayField<? extends JBBPAbstractField> array = varFieldProcessor.readVarArray(inStream, wholeStreamArray ? -1 : arrayLength, name, extraField, byteOrder, namedNumericFieldMap);
              JBBPUtils.assertNotNull(array, "A Var processor must not return null as a result of an array field reading [" + name + ':' + extraField + ']');
              if (array.getNameInfo() != name) {
                throw new JBBPParsingException("Detected wrong name for a read field array, must be " + name + " but detected " + array.getNameInfo() + ']');
              }
              structureFields.add(array);
            }
          }
        }
        break;
        case JBBPCompiler.CODE_BOOL: {
          if (resultNotIgnored) {
            if (arrayLength < 0) {
              singleAtomicField = new JBBPFieldBoolean(name, inStream.readBoolean());
            }
            else {
              structureFields.add(new JBBPFieldArrayBoolean(name, inStream.readBoolArray(wholeStreamArray ? -1 : arrayLength)));
            }
          }
        }
        break;
        case JBBPCompiler.CODE_BYTE: {
          if (resultNotIgnored) {
            if (arrayLength < 0) {
              singleAtomicField = new JBBPFieldByte(name, (byte) inStream.readByte());
            }
            else {
              structureFields.add(new JBBPFieldArrayByte(name, inStream.readByteArray(wholeStreamArray ? -1 : arrayLength)));
            }
          }
        }
        break;
        case JBBPCompiler.CODE_UBYTE: {
          if (resultNotIgnored) {
            if (arrayLength < 0) {
              singleAtomicField = new JBBPFieldUByte(name, (byte) inStream.readByte());
            }
            else {
              structureFields.add(new JBBPFieldArrayUByte(name, inStream.readByteArray(wholeStreamArray ? -1 : arrayLength)));
            }
          }
        }
        break;
        case JBBPCompiler.CODE_INT: {
          if (resultNotIgnored) {
            if (arrayLength < 0) {
              final int value = inStream.readInt(byteOrder);
              singleAtomicField = new JBBPFieldInt(name, value);
            }
            else {
              structureFields.add(new JBBPFieldArrayInt(name, inStream.readIntArray(wholeStreamArray ? -1 : arrayLength, byteOrder)));
            }
          }
        }
        break;
        case JBBPCompiler.CODE_LONG: {
          if (resultNotIgnored) {
            if (arrayLength < 0) {
              final long value = inStream.readLong(byteOrder);
              singleAtomicField = new JBBPFieldLong(name, value);
            }
            else {
              structureFields.add(new JBBPFieldArrayLong(name, inStream.readLongArray(wholeStreamArray ? -1 : arrayLength, byteOrder)));
            }
          }
        }
        break;
        case JBBPCompiler.CODE_SHORT: {
          if (resultNotIgnored) {
            if (arrayLength < 0) {
              final int value = inStream.readUnsignedShort(byteOrder);
              singleAtomicField = new JBBPFieldShort(name, (short) value);
            }
            else {
              structureFields.add(new JBBPFieldArrayShort(name, inStream.readShortArray(wholeStreamArray ? -1 : arrayLength, byteOrder)));
            }
          }
        }
        break;
        case JBBPCompiler.CODE_USHORT: {
          if (resultNotIgnored) {
            if (arrayLength < 0) {
              final int value = inStream.readUnsignedShort(byteOrder);
              singleAtomicField = new JBBPFieldUShort(name, (short) value);
            }
            else {
              structureFields.add(new JBBPFieldArrayUShort(name, inStream.readShortArray(wholeStreamArray ? -1 : arrayLength, byteOrder)));
            }
          }
        }
        break;
        case JBBPCompiler.CODE_STRUCT_START: {
          if (arrayLength < 0) {
            final List<JBBPAbstractField> structFields = parseStruct(inStream, positionAtCompiledBlock, varFieldProcessor, namedNumericFieldMap, positionAtNamedFieldList, positionAtVarLengthProcessors, skipStructureFields);
            // skip offset
            JBBPUtils.unpackInt(compiled, positionAtCompiledBlock);
            if (resultNotIgnored) {
              structureFields.add(new JBBPFieldStruct(name, structFields.toArray(new JBBPAbstractField[structFields.size()])));
            }
          }
          else {
            final int nameFieldCurrent = positionAtNamedFieldList.get();
            final int varLenProcCurrent = positionAtVarLengthProcessors.get();

            final JBBPFieldStruct[] result;
            if (resultNotIgnored) {
              if (wholeStreamArray) {
                // read till the stream end
                final List<JBBPFieldStruct> list = new ArrayList<JBBPFieldStruct>();
                while (inStream.hasAvailableData()) {
                  positionAtNamedFieldList.set(nameFieldCurrent);
                  positionAtVarLengthProcessors.set(varLenProcCurrent);

                  final List<JBBPAbstractField> fieldsForStruct = parseStruct(inStream, positionAtCompiledBlock, varFieldProcessor, namedNumericFieldMap, positionAtNamedFieldList, positionAtVarLengthProcessors, skipStructureFields);
                  list.add(new JBBPFieldStruct(name, fieldsForStruct));

                  final int structStart = JBBPUtils.unpackInt(compiled, positionAtCompiledBlock);

                  if (inStream.hasAvailableData()) {
                    positionAtCompiledBlock.set(structStart + 1);
                  }
                }

                result = list.isEmpty() ? EMPTY_STRUCT_ARRAY : list.toArray(new JBBPFieldStruct[list.size()]);
              }
              else {
                // read number of items
                if (arrayLength == 0) {
                  // skip the structure
                  result = EMPTY_STRUCT_ARRAY;
                  parseStruct(inStream, positionAtCompiledBlock, varFieldProcessor, namedNumericFieldMap, positionAtNamedFieldList, positionAtVarLengthProcessors, true);
                  JBBPUtils.unpackInt(compiled, positionAtCompiledBlock);
                }
                else {
                  result = new JBBPFieldStruct[arrayLength];
                  for (int i = 0; i < arrayLength; i++) {

                    final List<JBBPAbstractField> fieldsForStruct = parseStruct(inStream, positionAtCompiledBlock, varFieldProcessor, namedNumericFieldMap, positionAtNamedFieldList, positionAtVarLengthProcessors, skipStructureFields);
                    final int structStart = JBBPUtils.unpackInt(compiled, positionAtCompiledBlock);

                    result[i] = new JBBPFieldStruct(name, fieldsForStruct);

                    if (i < arrayLength - 1) {
                      // not the last
                      positionAtNamedFieldList.set(nameFieldCurrent);
                      positionAtVarLengthProcessors.set(varLenProcCurrent);
                      positionAtCompiledBlock.set(structStart + 1);
                    }
                  }
                }
              }

              if (result != null) {
                structureFields.add(new JBBPFieldArrayStruct(name, result));
              }
            }
            else {
              parseStruct(inStream, positionAtCompiledBlock, varFieldProcessor, namedNumericFieldMap, positionAtNamedFieldList, positionAtVarLengthProcessors, skipStructureFields);
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

      if (singleAtomicField != null) {
        structureFields.add(singleAtomicField);
        if (namedNumericFieldMap != null && singleAtomicField instanceof JBBPNumericField && name != null) {
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
   * null
   * @return the parsed content as the root structure
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPFieldStruct parse(final InputStream in) throws IOException {
    return this.parse(in, null, null);
  }

  /**
   * Parse am input stream with defined external value provider.
   *
   * @param in an input stream which content will be parsed, it must not be null
   * @param varFieldProcessor a var field processor, it may be null if there is
   * not any var field in a script, otherwise NPE will be thrown during parsing
   * @param externalValueProvider an external value provider, it can be null but
   * only if the script doesn't have fields desired the provider
   * @return the parsed content as the root structure
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPFieldStruct parse(final InputStream in, final JBBPVarFieldProcessor varFieldProcessor, final JBBPExternalValueProvider externalValueProvider) throws IOException {
    final JBBPBitInputStream bitInStream = in instanceof JBBPBitInputStream ? (JBBPBitInputStream) in : new JBBPBitInputStream(in, bitOrder);

    final JBBPNamedNumericFieldMap fieldMap;
    if (this.compiledBlock.hasEvaluatedSizeArrays() || this.compiledBlock.hasVarFields()) {
      fieldMap = new JBBPNamedNumericFieldMap(externalValueProvider);
    }
    else {
      fieldMap = null;
    }

    if (this.compiledBlock.hasVarFields()) {
      JBBPUtils.assertNotNull(varFieldProcessor, "The Script contains VAR fields, a var field processor must be provided");
    }

    return new JBBPFieldStruct(new JBBPNamedFieldInfo("", "", -1), parseStruct(bitInStream, new JBBPIntCounter(), varFieldProcessor, fieldMap, new JBBPIntCounter(), new JBBPIntCounter(), false));
  }

  /**
   * Parse a byte array content.
   *
   * @param array a byte array which content should be parsed, it must not be
   * null
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
   * @param array a byte array which content should be parsed, it must not be
   * null
   * @param varFieldProcessor a var field processor, it may be null if there is
   * not any var field in a script, otherwise NPE will be thrown during parsing
   * @param externalValueProvider an external value provider, it can be null but
   * only if the script doesn't have fields desired the provider
   * @return the parsed content as the root structure
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPFieldStruct parse(final byte[] array, final JBBPVarFieldProcessor varFieldProcessor, final JBBPExternalValueProvider externalValueProvider) throws IOException {
    JBBPUtils.assertNotNull(array, "Array must not be null");
    return this.parse(new ByteArrayInputStream(array), varFieldProcessor, externalValueProvider);
  }

  /**
   * Prepare a parser for a script and a bit order.
   *
   * @param script a text script contains field order and types reference, it
   * must not be null
   * @param bitOrder the bit order for reading operations, it must not be null
   * @return the prepared parser for the script
   * @see JBBPBitOrder#LSB0
   * @see JBBPBitOrder#MSB0
   */
  public static JBBPParser prepare(final String script, final JBBPBitOrder bitOrder) {
    return new JBBPParser(script, bitOrder);
  }

  /**
   * Prepare a parser for a script with usage of the default bit order LSB0.
   *
   * @param script a text script contains field order and types reference, it
   * must not be null
   * @return the prepared parser for the script
   * @see JBBPBitOrder#LSB0
   */
  public static JBBPParser prepare(final String script) {
    return JBBPParser.prepare(script, JBBPBitOrder.LSB0);
  }
}
