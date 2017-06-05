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
        ADD("+", 2, 200),
        SUB("-", 2, 200),
        MUL("*", 2, 300),
        MOD("%", 2, 300),
        NOT("~", 1, 500),
        OR("|", 2, 50),
        AND("&", 2, 150),
        XOR("^", 2, 100),
        DIV("/", 2, 300),
        LSHIFT("<<", 2, 175),
        RSHIFT(">>", 2, 175),
        URSHIFT(">>>", 2, 175),
        UNARY_PLUS("+", 1, 500),
        UNARY_MINUS("-", 1, 500);

        private final int priority;
        private final int args;
        private final String text;

        private Operator(final String text, final int args, final int priority) {
            this.args = args;
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

        public int getArgsNumber() {
            return this.args;
        }

        public String getText() {
            return this.text;
        }

        public int getPriority() {
            return this.priority;
        }
    }
}
