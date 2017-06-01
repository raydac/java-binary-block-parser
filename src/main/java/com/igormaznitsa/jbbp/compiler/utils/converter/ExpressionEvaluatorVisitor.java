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

import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;

public interface ExpressionEvaluatorVisitor {

    ExpressionEvaluatorVisitor begin();

    ExpressionEvaluatorVisitor visit(Special specialField);

    ExpressionEvaluatorVisitor visit(JBBPNamedFieldInfo nullableNameFieldInfo, String nullableExternalFieldName);

    ExpressionEvaluatorVisitor visit(Operator operator);

    ExpressionEvaluatorVisitor visit(int value);

    ExpressionEvaluatorVisitor end();

    enum Special {
        STREAM_COUNTER
    }

    enum Operator {
        ADD("+", 200), SUB("-", 200), MUL("*", 300), MOD("%", 300), NOT("~", 500), OR("|", 50), AND("&", 150), XOR("^", 100), DIV("/", 300), LSHIFT("<<", 175), RSHIFT(">>", 175), URSHIFT(">>>", 175), UNARY_PLUS("+", 500), UNARY_MINUS("-", 500);

        private final int priority;
        private final String text;

        private Operator(final String text, final int priority) {
            this.priority = priority;
            this.text = text;
        }

        public static Operator findForText(final String text) {
            Operator result = null;
            for (final Operator p : values()) {
                if (p.getText().equals(text)) {
                    result = p;
                    break;
                }
            }
            return result;
        }

        public String getText() {
            return this.text;
        }

        public int getPriority() {
            return this.priority;
        }
    }
}
