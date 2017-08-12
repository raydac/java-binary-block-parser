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

import com.igormaznitsa.jbbp.JBBPNamedNumericFieldMap;
import com.igormaznitsa.jbbp.compiler.JBBPCompiledBlock;
import com.igormaznitsa.jbbp.compiler.varlen.JBBPIntegerValueEvaluator;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;

/**
 * Auxiliary class to process expression with only integer constant.
 *
 * @since 1.3.0
 */
final class IntConstValueEvaluator implements JBBPIntegerValueEvaluator {

    private static final long serialVersionUID = 4640385518512384490L;

    /**
     * The Field contains integer constant.
     */
    private final int value;

    IntConstValueEvaluator(final int value) {
        this.value = value;
    }

    @Override
    public int eval(final JBBPBitInputStream inStream, final int currentCompiledBlockOffset, final JBBPCompiledBlock block, final JBBPNamedNumericFieldMap fieldMap) {
        return this.value;
    }

    @Override
    public void visitItems(JBBPCompiledBlock block, int currentCompiledBlockOffset, ExpressionEvaluatorVisitor visitor) {
        visitor.visitStart();
        visitor.visitConstant(this.value);
        visitor.visitEnd();
    }

}
