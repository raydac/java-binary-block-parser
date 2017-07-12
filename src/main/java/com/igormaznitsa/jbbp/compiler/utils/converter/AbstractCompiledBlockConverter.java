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
package com.igormaznitsa.jbbp.compiler.utils.converter;

import com.igormaznitsa.jbbp.JBBPNamedNumericFieldMap;
import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.compiler.JBBPCompiledBlock;
import com.igormaznitsa.jbbp.compiler.JBBPCompiler;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPFieldTypeParameterContainer;
import com.igormaznitsa.jbbp.compiler.varlen.JBBPIntegerValueEvaluator;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.utils.JBBPIntCounter;
import com.igormaznitsa.jbbp.utils.JBBPUtils;

public abstract class AbstractCompiledBlockConverter<T extends AbstractCompiledBlockConverter> {

    protected final JBBPCompiledBlock compiledBlock;
    protected final int parserFlags;

    public AbstractCompiledBlockConverter(final int parserFlags, final JBBPCompiledBlock notNullCompiledBlock) {
        this.parserFlags = parserFlags;
        this.compiledBlock = notNullCompiledBlock;
    }

    protected boolean isFlagSkipRemainingFieldsIfEOF(){
        return (this.parserFlags & JBBPParser.FLAG_SKIP_REMAINING_FIELDS_IF_EOF) != 0;
    }

    @SuppressWarnings("unchecked")
    public final T process() {
        this.onConvertStart();


        final byte[] compiledData = this.compiledBlock.getCompiledData();

        JBBPIntCounter positionAtCompiledBlock = new JBBPIntCounter(0);
        int positionAtNamedFieldList = 0;
        int positionAtVarLengthProcessors = 0;

        while (positionAtCompiledBlock.get() < compiledData.length) {

            final int theOffset = positionAtCompiledBlock.get();

            final int c = compiledData[positionAtCompiledBlock.getAndIncrement()] & 0xFF;
            final boolean wideCode = (c & JBBPCompiler.FLAG_WIDE) != 0;
            final int ec = wideCode ? compiledData[positionAtCompiledBlock.getAndIncrement()] & 0xFF : 0;
            final boolean extraFieldNumAsExpr = (ec & JBBPCompiler.EXT_FLAG_EXTRA_AS_EXPRESSION) != 0;
            final int code = (ec << 8) | c;

            final JBBPNamedFieldInfo name = (code & JBBPCompiler.FLAG_NAMED) == 0 ? null : this.compiledBlock.getNamedFields()[positionAtNamedFieldList++];
            final JBBPByteOrder byteOrder = (code & JBBPCompiler.FLAG_LITTLE_ENDIAN) == 0 ? JBBPByteOrder.BIG_ENDIAN : JBBPByteOrder.LITTLE_ENDIAN;

            final JBBPIntegerValueEvaluator extraFieldValueEvaluator;
            if (extraFieldNumAsExpr) {
                extraFieldValueEvaluator = this.compiledBlock.getArraySizeEvaluators()[positionAtVarLengthProcessors++];
            } else {
                extraFieldValueEvaluator = null;
            }

            final JBBPIntegerValueEvaluator arraySizeEvaluator;

            boolean readWholeStream = false;

            switch (code & (JBBPCompiler.FLAG_ARRAY | (JBBPCompiler.EXT_FLAG_EXPRESSION_OR_WHOLESTREAM << 8))) {
                case JBBPCompiler.FLAG_ARRAY: {
                    arraySizeEvaluator = new IntegerConstantValueEvaluator(JBBPUtils.unpackInt(compiledData, positionAtCompiledBlock));
                }
                break;
                case (JBBPCompiler.EXT_FLAG_EXPRESSION_OR_WHOLESTREAM << 8): {
                    arraySizeEvaluator = new IntegerConstantValueEvaluator(-1);
                    readWholeStream = true;
                }
                break;
                case JBBPCompiler.FLAG_ARRAY | (JBBPCompiler.EXT_FLAG_EXPRESSION_OR_WHOLESTREAM << 8): {
                    arraySizeEvaluator = this.compiledBlock.getArraySizeEvaluators()[positionAtVarLengthProcessors++];
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
                    onActionItem(theOffset, code, null);
                }
                break;
                case JBBPCompiler.CODE_SKIP:
                case JBBPCompiler.CODE_ALIGN: {
                    final JBBPIntegerValueEvaluator evaluator = extraFieldNumAsExpr ? extraFieldValueEvaluator : new IntegerConstantValueEvaluator(JBBPUtils.unpackInt(compiledData, positionAtCompiledBlock));
                    onActionItem(theOffset, theCode, evaluator);
                }
                break;

                case JBBPCompiler.CODE_BIT: {
                    final JBBPIntegerValueEvaluator numberOfBits = extraFieldNumAsExpr ? extraFieldValueEvaluator : new IntegerConstantValueEvaluator(JBBPUtils.unpackInt(compiledData, positionAtCompiledBlock));
                    onBitField(theOffset, name, numberOfBits, arraySizeEvaluator);
                }
                break;

                case JBBPCompiler.CODE_BOOL:
                case JBBPCompiler.CODE_BYTE:
                case JBBPCompiler.CODE_UBYTE:
                case JBBPCompiler.CODE_SHORT:
                case JBBPCompiler.CODE_USHORT:
                case JBBPCompiler.CODE_INT:
                case JBBPCompiler.CODE_LONG: {
                    onPrimitive(theOffset, theCode, name, byteOrder, arraySizeEvaluator);
                }
                break;

                case JBBPCompiler.CODE_STRUCT_START: {
                    onStructStart(theOffset, name, arraySizeEvaluator);
                }
                break;

                case JBBPCompiler.CODE_STRUCT_END: {
                    JBBPUtils.unpackInt(compiledData, positionAtCompiledBlock);
                    onStructEnd(theOffset, name);
                }
                break;

                case JBBPCompiler.CODE_VAR: {
                    final JBBPIntegerValueEvaluator extraDataValueEvaluator = extraFieldNumAsExpr ? extraFieldValueEvaluator : new IntegerConstantValueEvaluator(JBBPUtils.unpackInt(compiledData, positionAtCompiledBlock));
                    onVar(theOffset, name, byteOrder, readWholeStream, arraySizeEvaluator, extraDataValueEvaluator);
                }
                break;

                case JBBPCompiler.CODE_CUSTOMTYPE: {
                    final JBBPIntegerValueEvaluator extraDataValueEvaluator = extraFieldNumAsExpr ? extraFieldValueEvaluator : new IntegerConstantValueEvaluator(JBBPUtils.unpackInt(compiledData, positionAtCompiledBlock));
                    final JBBPFieldTypeParameterContainer fieldTypeInfo = this.compiledBlock.getCustomTypeFields()[JBBPUtils.unpackInt(compiledData, positionAtCompiledBlock)];
                    onCustom(theOffset, fieldTypeInfo, name, byteOrder, readWholeStream, arraySizeEvaluator, extraDataValueEvaluator);
                }
                break;
                default:
                    throw new Error("Unexpected code, contact developer!");
            }
        }

        this.onConvertEnd();

        return (T) this;
    }

    public void onActionItem(int offsetInCompiledBlock, int actionType, JBBPIntegerValueEvaluator nullableArgument) {
    }

    public void onPrimitive(int offsetInCompiledBlock, int primitiveType, JBBPNamedFieldInfo nullableNameFieldInfo, JBBPByteOrder byteOrder, JBBPIntegerValueEvaluator nullableArraySize) {
    }

    public void onVar(int offsetInCompiledBlock, JBBPNamedFieldInfo nullableNameFieldInfo, JBBPByteOrder byteOrder, boolean readWholeStreamIntoArray, JBBPIntegerValueEvaluator nullableArraySize,  JBBPIntegerValueEvaluator extraDataValueEvaluator) {
    }

    public void onCustom(int offsetInCompiledBlock, JBBPFieldTypeParameterContainer notNullfieldType, JBBPNamedFieldInfo nullableNameFieldInfo, JBBPByteOrder byteOrder, boolean readWholeStream, JBBPIntegerValueEvaluator nullableArraySizeEvaluator, JBBPIntegerValueEvaluator extraDataValueEvaluator) {
    }

    public void onBitField(int offsetInCompiledBlock, JBBPNamedFieldInfo nullableNameFieldInfo, JBBPIntegerValueEvaluator notNullFieldSize, JBBPIntegerValueEvaluator nullableArraySize) {
    }

    public void onStructStart(int offsetInCompiledBlock, JBBPNamedFieldInfo nullableNameFieldInfo, JBBPIntegerValueEvaluator nullableArraySize) {
    }

    public void onStructEnd(int offsetInCompiledBlock, JBBPNamedFieldInfo nullableNameFieldInfo) {
    }

    public void onConvertStart() {
    }

    public void onConvertEnd() {
    }

    private static final class IntegerConstantValueEvaluator implements JBBPIntegerValueEvaluator {

        private static final long serialVersionUID = 4640385518512384490L;

        private final int value;

        public IntegerConstantValueEvaluator(final int value) {
            this.value = value;
        }

        @Override
        public int eval(final JBBPBitInputStream inStream, final int currentCompiledBlockOffset, final JBBPCompiledBlock block, final JBBPNamedNumericFieldMap fieldMap) {
            return this.value;
        }

        @Override
        public void visit(JBBPCompiledBlock block, int currentCompiledBlockOffset, ExpressionEvaluatorVisitor visitor) {
            visitor.begin();
            visitor.visit(this.value);
            visitor.end();
        }

    }
}
