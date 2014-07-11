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
import com.igormaznitsa.jbbp.compiler.varlen.JBBPLengthEvaluator;
import com.igormaznitsa.jbbp.exceptions.JBBPParsingException;
import com.igormaznitsa.jbbp.io.*;
import com.igormaznitsa.jbbp.model.*;
import com.igormaznitsa.jbbp.utils.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
  private List<JBBPAbstractField> parseStruct(final JBBPBitInputStream inStream, final AtomicInteger positionAtCompiledBlock, final JBBPNamedNumericFieldMap namedNumericFieldMap, final AtomicInteger positionAtNamedFieldList, final AtomicInteger positionAtVarLengthProcessors, final boolean skipStructureFields) throws IOException {
    final List<JBBPAbstractField> structureFields = skipStructureFields ? new ArrayList<JBBPAbstractField>() : null;
    final byte[] compiled = this.compiledBlock.getCompiledData();

    boolean endStructureNotMet = true;

    while (endStructureNotMet && positionAtCompiledBlock.get() < compiled.length) {
      final int instructionStartOffset = positionAtCompiledBlock.getAndIncrement();
      final int code = compiled[instructionStartOffset] & 0xFF;

      final JBBPNamedFieldInfo name;
      if ((code & JBBPCompiler.FLAG_NAMED) != 0) {
        name = compiledBlock.getNamedFields()[positionAtNamedFieldList.getAndIncrement()];
      }
      else {
        name = null;
      }

      final JBBPByteOrder byteOrder = (code & JBBPCompiler.FLAG_LITTLE_ENDIAN) == 0 ? JBBPByteOrder.BIG_ENDIAN : JBBPByteOrder.LITTLE_ENDIAN;
      final boolean resultNotIgnored = structureFields != null;

      final boolean wholeStreamArray;
      final int arrayLength;
      switch (code & (JBBPCompiler.FLAG_ARRAY | JBBPCompiler.FLAG_EXPRESSIONORWHOLE)) {
        case JBBPCompiler.FLAG_ARRAY: {
          arrayLength = JBBPUtils.unpackInt(compiled, positionAtCompiledBlock);
          wholeStreamArray = false;
        }
        break;
        case JBBPCompiler.FLAG_EXPRESSIONORWHOLE: {
          wholeStreamArray = true;
          arrayLength = 0;
        }
        break;
        case JBBPCompiler.FLAG_ARRAY | JBBPCompiler.FLAG_EXPRESSIONORWHOLE: {
          final JBBPLengthEvaluator evaluator = this.compiledBlock.getArraySizeEvaluators()[positionAtVarLengthProcessors.getAndIncrement()];
          arrayLength = evaluator.eval(inStream, instructionStartOffset, this.compiledBlock, namedNumericFieldMap);
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
          if (resultNotIgnored) {
            final int alignByteNumber = JBBPUtils.unpackInt(compiled, positionAtCompiledBlock);

            inStream.alignByte();

            if (alignByteNumber > 0) {
              while (inStream.getCounter() % alignByteNumber != 0) {
                final int skeptByte = inStream.read();
                if (skeptByte < 0) {
                  throw new EOFException("Can't align for " + alignByteNumber + " for EOFException");
                }
              }
            }
          }
        }
        break;
        case JBBPCompiler.CODE_SKIP: {
          if (resultNotIgnored) {
            final int skipByteNumber = JBBPUtils.unpackInt(compiled, positionAtCompiledBlock);
            if (skipByteNumber > 0) {

              final long skeptBytes = inStream.skip(skipByteNumber);

              if (skeptBytes != skipByteNumber) {
                throw new EOFException("Can't skip " + skipByteNumber + " byte(s), skept only " + skeptBytes);
              }
            }
          }
        }
        break;
        case JBBPCompiler.CODE_BIT: {
          if (resultNotIgnored) {
            final JBBPBitNumber bitNumber = JBBPBitNumber.decode(JBBPUtils.unpackInt(compiled, positionAtCompiledBlock));
            if (arrayLength < 0) {
              singleAtomicField = new JBBPFieldBit(name, inStream.readBits(bitNumber));
            }
            else {
              if (wholeStreamArray) {
                structureFields.add(new JBBPFieldArrayBit(name, inStream.readBitsArray(-1, bitNumber)));
              }
              else {
                structureFields.add(new JBBPFieldArrayBit(name, inStream.readBitsArray(arrayLength, bitNumber)));

              }
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
              if (wholeStreamArray) {
                structureFields.add(new JBBPFieldArrayBoolean(name, inStream.readBooleanArray(-1)));
              }
              else {
                structureFields.add(new JBBPFieldArrayBoolean(name, inStream.readBooleanArray(arrayLength)));
              }
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
              if (wholeStreamArray) {
                structureFields.add(new JBBPFieldArrayByte(name, inStream.readByteArray(-1)));
              }
              else {
                structureFields.add(new JBBPFieldArrayByte(name, inStream.readByteArray(arrayLength)));
              }
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
              if (wholeStreamArray) {
                structureFields.add(new JBBPFieldArrayUByte(name, inStream.readByteArray(-1)));
              }
              else {
                structureFields.add(new JBBPFieldArrayUByte(name, inStream.readByteArray(arrayLength)));
              }
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
              if (wholeStreamArray) {
                structureFields.add(new JBBPFieldArrayInt(name, inStream.readIntArray(-1, byteOrder)));
              }
              else {
                structureFields.add(new JBBPFieldArrayInt(name, inStream.readIntArray(arrayLength, byteOrder)));
              }
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
              if (wholeStreamArray) {
                structureFields.add(new JBBPFieldArrayLong(name, inStream.readLongArray(-1, byteOrder)));
              }
              else {
                structureFields.add(new JBBPFieldArrayLong(name, inStream.readLongArray(arrayLength, byteOrder)));
              }
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
              if (wholeStreamArray) {
                structureFields.add(new JBBPFieldArrayShort(name, inStream.readShortArray(-1, byteOrder)));
              }
              else {
                structureFields.add(new JBBPFieldArrayShort(name, inStream.readShortArray(arrayLength, byteOrder)));
              }
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
              if (wholeStreamArray) {
                structureFields.add(new JBBPFieldArrayUShort(name, inStream.readShortArray(-1, byteOrder)));
              }
              else {
                structureFields.add(new JBBPFieldArrayUShort(name, inStream.readShortArray(arrayLength, byteOrder)));
              }
            }
          }
        }
        break;
        case JBBPCompiler.CODE_STRUCT_START: {
          if (arrayLength < 0) {
            final List<JBBPAbstractField> structFields = parseStruct(inStream, positionAtCompiledBlock, namedNumericFieldMap, positionAtNamedFieldList, positionAtVarLengthProcessors, skipStructureFields);
            // offset
            JBBPUtils.unpackInt(compiled, positionAtCompiledBlock);
            if (structureFields != null && !structFields.isEmpty()) {
              structureFields.add(new JBBPFieldStruct(name, structFields.toArray(new JBBPAbstractField[structFields.size()])));
            }
          }
          else {
            final int nameFieldCurrent = positionAtNamedFieldList.get();
            final int varLenProcCurrent = positionAtVarLengthProcessors.get();

            final JBBPFieldStruct[] result;
            if (structureFields != null) {
              if (wholeStreamArray) {
                // read till the stream end
                final List<JBBPFieldStruct> list = new ArrayList<JBBPFieldStruct>();
                while (inStream.hasAvailableData()) {
                  positionAtNamedFieldList.set(nameFieldCurrent);
                  positionAtVarLengthProcessors.set(varLenProcCurrent);

                  final List<JBBPAbstractField> fieldsForStruct = parseStruct(inStream, positionAtCompiledBlock, namedNumericFieldMap, positionAtNamedFieldList, positionAtVarLengthProcessors, skipStructureFields);
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
                  parseStruct(inStream, positionAtCompiledBlock, namedNumericFieldMap, positionAtNamedFieldList, positionAtVarLengthProcessors, false);
                  JBBPUtils.unpackInt(compiled, positionAtCompiledBlock);
                }
                else {
                  result = new JBBPFieldStruct[arrayLength];
                  for (int i = 0; i < arrayLength; i++) {
                    positionAtNamedFieldList.set(nameFieldCurrent);

                    final List<JBBPAbstractField> fieldsForStruct = parseStruct(inStream, positionAtCompiledBlock, namedNumericFieldMap, positionAtNamedFieldList, positionAtVarLengthProcessors, skipStructureFields);
                    final int structStart = JBBPUtils.unpackInt(compiled, positionAtCompiledBlock);

                    result[i] = new JBBPFieldStruct(name, fieldsForStruct);

                    if (i < arrayLength - 1) {
                      // not the last
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
              parseStruct(inStream, positionAtCompiledBlock, namedNumericFieldMap, positionAtNamedFieldList, positionAtVarLengthProcessors, skipStructureFields);
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
    return this.parse(in, null);
  }

  /**
   * Parse am input stream with defined external value provider.
   *
   * @param in an input stream which content will be parsed, it must not be null
   * @param externalValueProvider an external value provider, it can be null but
   * only if the script doesn't have fields desired the provider
   * @return the parsed content as the root structure
   * @throws IOException it will be thrown for transport errors
   */
  public JBBPFieldStruct parse(final InputStream in, final JBBPExternalValueProvider externalValueProvider) throws IOException {
    final JBBPBitInputStream bitInStream = in instanceof JBBPBitInputStream ? (JBBPBitInputStream) in : new JBBPBitInputStream(in, bitOrder);

    final JBBPNamedNumericFieldMap fieldMap;
    if (this.compiledBlock.hasVarArrays()) {
      fieldMap = new JBBPNamedNumericFieldMap(externalValueProvider);
    }
    else {
      fieldMap = null;
    }

    return new JBBPFieldStruct(new JBBPNamedFieldInfo("", "", -1), parseStruct(bitInStream, new AtomicInteger(), fieldMap, new AtomicInteger(), new AtomicInteger(), true));
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
    return this.parse(new ByteArrayInputStream(array), null);
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
