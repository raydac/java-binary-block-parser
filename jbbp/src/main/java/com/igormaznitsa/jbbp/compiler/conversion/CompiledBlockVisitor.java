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

package com.igormaznitsa.jbbp.compiler.conversion;

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.compiler.JBBPCompiledBlock;
import com.igormaznitsa.jbbp.compiler.JBBPCompiler;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPFieldTypeParameterContainer;
import com.igormaznitsa.jbbp.compiler.varlen.JBBPIntegerValueEvaluator;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.utils.JBBPIntCounter;
import com.igormaznitsa.jbbp.utils.JBBPUtils;

/**
 * Visitor implements Template pattern to visitSpecial all fields of compiled parser data block.
 *
 * @since 1.3.0
 */
public class CompiledBlockVisitor {

  /**
   * Compiled block to be processed, must not be null.
   */
  protected final JBBPCompiledBlock compiledBlock;

  /**
   * Parser flags to be used for the translation.
   */
  protected final int parserFlags;

  /**
   * The Constructor.
   *
   * @param parserFlags          parser flags
   * @param notNullCompiledBlock compiled parser data block, must not be null
   */
  public CompiledBlockVisitor(final int parserFlags, final JBBPCompiledBlock notNullCompiledBlock) {
    this.parserFlags = parserFlags;
    this.compiledBlock = notNullCompiledBlock;
  }

  /**
   * Auxiliary function to check that parser flag to skip remaining fields without exception is set.
   *
   * @return true if FLAG_SKIP_REMAINING_FIELDS_IF_EOF flag is set, false otherwise
   */
  protected boolean isFlagSkipRemainingFieldsIfEOF() {
    return (this.parserFlags & JBBPParser.FLAG_SKIP_REMAINING_FIELDS_IF_EOF) != 0;
  }

  /**
   * The Main method of the class, it processes compiled block and make calls to template methods.
   *
   * @return the instance of the visitor, must not be null
   */
  public final CompiledBlockVisitor visit() {
    this.visitStart();

    final byte[] compiledData = this.compiledBlock.getCompiledData();

    JBBPIntCounter positionAtCompiledBlock = new JBBPIntCounter(0);
    int positionAtNamedFieldList = 0;
    int positionAtVarLengthProcessors = 0;

    while (positionAtCompiledBlock.get() < compiledData.length) {

      final int theOffset = positionAtCompiledBlock.get();

      final int c = compiledData[positionAtCompiledBlock.getAndIncrement()] & 0xFF;
      final boolean wideCode = (c & JBBPCompiler.FLAG_WIDE) != 0;
      final int ec = wideCode ? compiledData[positionAtCompiledBlock.getAndIncrement()] & 0xFF : 0;
      final boolean altFileType = (ec & JBBPCompiler.EXT_FLAG_EXTRA_DIFF_TYPE) != 0;
      final boolean extraFieldNumAsExpr = (ec & JBBPCompiler.EXT_FLAG_EXTRA_AS_EXPRESSION) != 0;
      final int code = (ec << 8) | c;

      final JBBPNamedFieldInfo name = (code & JBBPCompiler.FLAG_NAMED) == 0 ? null :
          this.compiledBlock.getNamedFields()[positionAtNamedFieldList++];
      final JBBPByteOrder byteOrder =
          (code & JBBPCompiler.FLAG_LITTLE_ENDIAN) == 0 ? JBBPByteOrder.BIG_ENDIAN :
              JBBPByteOrder.LITTLE_ENDIAN;

      final JBBPIntegerValueEvaluator extraFieldValueEvaluator;
      if (extraFieldNumAsExpr) {
        extraFieldValueEvaluator =
            this.compiledBlock.getArraySizeEvaluators()[positionAtVarLengthProcessors++];
      } else {
        extraFieldValueEvaluator = null;
      }

      final JBBPIntegerValueEvaluator arraySizeEvaluator;

      boolean readWholeStream = false;

      switch (code &
          (JBBPCompiler.FLAG_ARRAY | (JBBPCompiler.EXT_FLAG_EXPRESSION_OR_WHOLESTREAM << 8))) {
        case JBBPCompiler.FLAG_ARRAY: {
          arraySizeEvaluator = new IntConstValueEvaluator(
              JBBPUtils.unpackInt(compiledData, positionAtCompiledBlock));
        }
        break;
        case (JBBPCompiler.EXT_FLAG_EXPRESSION_OR_WHOLESTREAM << 8): {
          arraySizeEvaluator = new IntConstValueEvaluator(-1);
          readWholeStream = true;
        }
        break;
        case JBBPCompiler.FLAG_ARRAY | (JBBPCompiler.EXT_FLAG_EXPRESSION_OR_WHOLESTREAM << 8): {
          arraySizeEvaluator =
              this.compiledBlock.getArraySizeEvaluators()[positionAtVarLengthProcessors++];
        }
        break;
        default: {
          // it is not an array, just a single field
          arraySizeEvaluator = null;
        }
        break;
      }

      final int theCode = code & 0xF;

      switch (theCode) {
        case JBBPCompiler.CODE_RESET_COUNTER: {
          visitActionItem(theOffset, code, null);
        }
        break;
        case JBBPCompiler.CODE_SKIP:
        case JBBPCompiler.CODE_ALIGN: {
          final JBBPIntegerValueEvaluator evaluator =
              extraFieldNumAsExpr ? extraFieldValueEvaluator : new IntConstValueEvaluator(
                  JBBPUtils.unpackInt(compiledData, positionAtCompiledBlock));
          if (altFileType) {
            if (theCode == JBBPCompiler.CODE_SKIP) {
              visitValField(theOffset, byteOrder, name, evaluator);
            } else {
              throw new Error("Unexpected code:" + theCode);
            }
          } else {
            visitActionItem(theOffset, theCode, evaluator);
          }
        }
        break;

        case JBBPCompiler.CODE_BIT: {
          final JBBPIntegerValueEvaluator numberOfBits =
              extraFieldNumAsExpr ? extraFieldValueEvaluator : new IntConstValueEvaluator(
                  JBBPUtils.unpackInt(compiledData, positionAtCompiledBlock));
          visitBitField(theOffset, byteOrder, name, readWholeStream, numberOfBits,
              arraySizeEvaluator);
        }
        break;

        case JBBPCompiler.CODE_BOOL:
        case JBBPCompiler.CODE_BYTE:
        case JBBPCompiler.CODE_UBYTE:
        case JBBPCompiler.CODE_SHORT:
        case JBBPCompiler.CODE_USHORT:
        case JBBPCompiler.CODE_INT:
        case JBBPCompiler.CODE_LONG: {
          visitPrimitiveField(theOffset, theCode, name, byteOrder, readWholeStream, altFileType,
              arraySizeEvaluator);
        }
        break;

        case JBBPCompiler.CODE_STRUCT_START: {
          visitStructureStart(theOffset, byteOrder, readWholeStream, name, arraySizeEvaluator);
        }
        break;

        case JBBPCompiler.CODE_STRUCT_END: {
          JBBPUtils.unpackInt(compiledData, positionAtCompiledBlock);
          visitStructureEnd(theOffset, name);
        }
        break;

        case JBBPCompiler.CODE_VAR: {
          final JBBPIntegerValueEvaluator extraDataValueEvaluator =
              extraFieldNumAsExpr ? extraFieldValueEvaluator : new IntConstValueEvaluator(
                  JBBPUtils.unpackInt(compiledData, positionAtCompiledBlock));
          visitVarField(theOffset, name, byteOrder, readWholeStream, arraySizeEvaluator,
              extraDataValueEvaluator);
        }
        break;

        case JBBPCompiler.CODE_CUSTOMTYPE: {
          final JBBPIntegerValueEvaluator extraDataValueEvaluator =
              extraFieldNumAsExpr ? extraFieldValueEvaluator : new IntConstValueEvaluator(
                  JBBPUtils.unpackInt(compiledData, positionAtCompiledBlock));
          final JBBPFieldTypeParameterContainer fieldTypeInfo =
              this.compiledBlock.getCustomTypeFields()[JBBPUtils
                  .unpackInt(compiledData, positionAtCompiledBlock)];
          visitCustomField(theOffset, fieldTypeInfo, name, byteOrder, readWholeStream,
              arraySizeEvaluator, extraDataValueEvaluator);
        }
        break;
        default:
          throw new Error("Unexpected code, contact developer!");
      }
    }

    this.visitEnd();

    return this;
  }

  /**
   * Visit an action item (like skip, align or reset counter command)
   *
   * @param offsetInCompiledBlock offset in the compiled block
   * @param actionType            the action type
   * @param nullableArgument      argument for action, it can be null
   * @see JBBPCompiler#CODE_RESET_COUNTER
   * @see JBBPCompiler#CODE_ALIGN
   * @see JBBPCompiler#CODE_SKIP
   */
  public void visitActionItem(int offsetInCompiledBlock, int actionType,
                              JBBPIntegerValueEvaluator nullableArgument) {
  }

  /**
   * Visit field contains virtual field defined through VAL type.
   *
   * @param offsetInCompiledBlock offset in the compiled block
   * @param byteOrder             byteOrder
   * @param nameFieldInfo         name of the field, must not be null
   * @param expression            expression to calculate value
   * @since 1.4.0
   */
  public void visitValField(int offsetInCompiledBlock,
                            JBBPByteOrder byteOrder,
                            JBBPNamedFieldInfo nameFieldInfo,
                            JBBPIntegerValueEvaluator expression) {
  }

  /**
   * Visit a primitive data field
   *
   * @param offsetInCompiledBlock  offset in the compiled block
   * @param primitiveType          the primitive type
   * @param nullableNameFieldInfo  field info, null if the field is anonymous one
   * @param byteOrder              byte order for the field, must not be null
   * @param readWholeStreamAsArray if true then it is array with unknown size till the stream end
   * @param altFieldType           flag shows that field type is alternative one, INT should be recognized as FLOAT and LONG as DOUBLE and BOOL as STRING
   * @param nullableArraySize      array size if the field is array, null if the field is not array or variable length array
   * @see JBBPCompiler#CODE_BYTE
   * @see JBBPCompiler#CODE_UBYTE
   * @see JBBPCompiler#CODE_SHORT
   * @see JBBPCompiler#CODE_USHORT
   * @see JBBPCompiler#CODE_BOOL
   * @see JBBPCompiler#CODE_INT
   * @see JBBPCompiler#CODE_LONG
   * @see JBBPCompiler#CODE_SKIP
   */
  public void visitPrimitiveField(int offsetInCompiledBlock,
                                  int primitiveType,
                                  JBBPNamedFieldInfo nullableNameFieldInfo,
                                  JBBPByteOrder byteOrder,
                                  boolean readWholeStreamAsArray,
                                  boolean altFieldType,
                                  JBBPIntegerValueEvaluator nullableArraySize) {
  }

  /**
   * Visit a variable field (which is defined with var data type)
   *
   * @param offsetInCompiledBlock offset in the compiled block
   * @param nullableNameFieldInfo field info, null if the field is anonymous one
   * @param byteOrder             byte order for the field, must not be null
   * @param readWholeStream       true if whole stream should be read as array of var type, false otherwise
   * @param nullableArraySize     if not null then evaluator of array size to be read from stream
   * @param extraDataValue        if not null then extra data evaluator for the var field
   */
  public void visitVarField(int offsetInCompiledBlock,
                            JBBPNamedFieldInfo nullableNameFieldInfo,
                            JBBPByteOrder byteOrder,
                            boolean readWholeStream,
                            JBBPIntegerValueEvaluator nullableArraySize,
                            JBBPIntegerValueEvaluator extraDataValue) {
  }

  /**
   * Visit a custom type field.
   *
   * @param offsetInCompiledBlock offset in the compiled block
   * @param notNullFieldType      field type info, must not be null
   * @param nullableNameFieldInfo field info, null if the field is anonymous one
   * @param byteOrder             byte order for the field, must not be null
   * @param readWholeStream       true if whole stream should be read as array of var type, false otherwise
   * @param nullableArraySize     if not null then evaluator of array size to be read from stream
   * @param extraDataValue        if not null then extra data evaluator for the var field
   */
  public void visitCustomField(int offsetInCompiledBlock,
                               JBBPFieldTypeParameterContainer notNullFieldType,
                               JBBPNamedFieldInfo nullableNameFieldInfo,
                               JBBPByteOrder byteOrder,
                               boolean readWholeStream,
                               JBBPIntegerValueEvaluator nullableArraySize,
                               JBBPIntegerValueEvaluator extraDataValue) {
  }

  /**
   * Visit a custom type field.
   *
   * @param offsetInCompiledBlock offset in the compiled block
   * @param byteOrder             byte order for the field, must not be null
   * @param nullableNameFieldInfo field info, null if the field is anonymous one
   * @param readWholeStream       true if whole stream should be read as array of var type, false otherwise
   * @param notNullFieldSize      evaluator to calculate size of the field, must not be null
   * @param nullableArraySize     if not null then evaluator of array size to be read from stream
   */
  public void visitBitField(int offsetInCompiledBlock,
                            JBBPByteOrder byteOrder,
                            JBBPNamedFieldInfo nullableNameFieldInfo,
                            boolean readWholeStream,
                            JBBPIntegerValueEvaluator notNullFieldSize,
                            JBBPIntegerValueEvaluator nullableArraySize) {
  }

  /**
   * Visit a structure field.
   *
   * @param offsetInCompiledBlock offset in the compiled block
   * @param byteOrder             byte order for the field, must not be null
   * @param readWholeStream       true if whole stream should be read as array of var type, false otherwise
   * @param nullableNameFieldInfo field info, null if the field is anonymous one
   * @param nullableArraySize     if not null then evaluator of array size to be read from stream
   */
  public void visitStructureStart(int offsetInCompiledBlock,
                                  JBBPByteOrder byteOrder,
                                  boolean readWholeStream,
                                  JBBPNamedFieldInfo nullableNameFieldInfo,
                                  JBBPIntegerValueEvaluator nullableArraySize) {
  }

  /**
   * End visit of a structure
   *
   * @param offsetInCompiledBlock offset in the compiled block
   * @param nullableNameFieldInfo field info, null if the field is anonymous one
   */
  public void visitStructureEnd(int offsetInCompiledBlock,
                                JBBPNamedFieldInfo nullableNameFieldInfo) {
  }

  /**
   * Called before visit of each item.
   */
  public void visitStart() {
  }

  /**
   * Called after visit each item.
   */
  public void visitEnd() {
  }

}
