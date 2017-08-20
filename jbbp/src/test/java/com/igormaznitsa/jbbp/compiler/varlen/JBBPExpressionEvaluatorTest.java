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
package com.igormaznitsa.jbbp.compiler.varlen;

import com.igormaznitsa.jbbp.JBBPExternalValueProvider;
import com.igormaznitsa.jbbp.JBBPNamedNumericFieldMap;
import com.igormaznitsa.jbbp.compiler.JBBPCompiledBlock;
import com.igormaznitsa.jbbp.compiler.JBBPCompiler;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.exceptions.JBBPCompilationException;
import com.igormaznitsa.jbbp.exceptions.JBBPEvalException;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.model.JBBPFieldInt;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class JBBPExpressionEvaluatorTest {

    @Test(expected = JBBPCompilationException.class)
    public void testExpression_Error_ConstantsWithoutOperators() {
        new JBBPExpressionEvaluator(" 1   334", null, null);
    }

    @Test(expected = JBBPCompilationException.class)
    public void testExpression_Error_OnlyBrackets() {
        new JBBPExpressionEvaluator(" ( ( ( ( ) )) )  ", null, null);
    }

    @Test(expected = JBBPCompilationException.class)
    public void testExpression_Error_NonClosedBracket() {
        new JBBPExpressionEvaluator("(34+45  ", null, null);
    }

    @Test(expected = JBBPCompilationException.class)
    public void testExpression_Error_NonOpenedBracket() {
        new JBBPExpressionEvaluator("34+45)  ", null, null);
    }

    @Test(expected = JBBPCompilationException.class)
    public void testExpression_Error_Spaces() {
        new JBBPExpressionEvaluator("  ", null, null);
    }

    @Test(expected = JBBPCompilationException.class)
    public void testExpression_Error_Empty() {
        new JBBPExpressionEvaluator("", null, null);
    }

    @Test(expected = JBBPCompilationException.class)
    public void testExpression_Error_OnlyUnaryPlus() {
        new JBBPExpressionEvaluator("+", null, null);
    }

    @Test(expected = JBBPCompilationException.class)
    public void testExpression_Error_OnlyUnaryMinus() {
        new JBBPExpressionEvaluator("-", null, null);
    }

    @Test(expected = JBBPCompilationException.class)
    public void testExpression_Error_OnlyNot() {
        new JBBPExpressionEvaluator("~", null, null);
    }

    @Test(expected = JBBPCompilationException.class)
    public void testExpression_Error_WrongFirstUnaryOperator() {
        new JBBPExpressionEvaluator("%345", null, null);
    }

    @Test(expected = JBBPCompilationException.class)
    public void testExpression_Error_WrongUnaryOperator() {
        new JBBPExpressionEvaluator("222%%345", null, null);
    }

    @Test(expected = JBBPCompilationException.class)
    public void testExpression_Error_OnlyNonUnaryOperator() {
        new JBBPExpressionEvaluator("*", null, null);
    }

    @Test(expected = JBBPEvalException.class)
    public void testExpression_Error_MulWithoutSecondArgument() {
        new JBBPExpressionEvaluator("123*", null, null).eval(null, 0, null, null);
    }

    @Test(expected = JBBPEvalException.class)
    public void testExpression_Error_SubWithoutSecondArgument() {
        new JBBPExpressionEvaluator("123-", null, null).eval(null, 0, null, null);
    }

    @Test(expected = JBBPEvalException.class)
    public void testExpression_Error_AddWithoutSecondArgument() {
        new JBBPExpressionEvaluator("123+", null, null).eval(null, 0, null, null);
    }

    @Test(expected = JBBPEvalException.class)
    public void testExpression_Error_ModWithoutSecondArgument() {
        new JBBPExpressionEvaluator("123%", null, null).eval(null, 0, null, null);
    }

    @Test(expected = JBBPEvalException.class)
    public void testExpression_Error_DivWithoutSecondArgument() {
        new JBBPExpressionEvaluator("123/", null, null).eval(null, 0, null, null);
    }

    @Test(expected = JBBPEvalException.class)
    public void testExpression_Error_AndWithoutSecondArgument() {
        new JBBPExpressionEvaluator("123&", null, null).eval(null, 0, null, null);
    }

    @Test(expected = JBBPEvalException.class)
    public void testExpression_Error_OrWithoutSecondArgument() {
        new JBBPExpressionEvaluator("123|", null, null).eval(null, 0, null, null);
    }

    @Test(expected = JBBPEvalException.class)
    public void testExpression_Error_XorWithoutSecondArgument() {
        new JBBPExpressionEvaluator("123^", null, null).eval(null, 0, null, null);
    }

    @Test
    public void testExpression_Constant() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("5678", null, null);
        assertEquals(5678, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_ErrorForTooBigConstant() {
        try {
            new JBBPExpressionEvaluator("5678892374982347", null, null);
            fail("Must throw compilation exception");
        } catch (JBBPCompilationException ex) {
            assertTrue(ex.getCause() instanceof NumberFormatException);
        }
    }

    @Test
    public void testExpression_Variable() {
        final JBBPNamedFieldInfo info = new JBBPNamedFieldInfo("hello", "hello", 0);
        final byte[] compiled = new byte[]{JBBPCompiler.CODE_INT | JBBPCompiler.FLAG_NAMED};
        final List<JBBPNamedFieldInfo> list = Collections.singletonList(info);

        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("hello", list, compiled);
        final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap();
        map.putField(new JBBPFieldInt(info, 1234));
        assertEquals(1234, expr.eval(null, 0, JBBPCompiledBlock.prepare().setCompiledData(compiled).setNamedFieldData(list).setSource("no source").build(), map));
    }

    @Test
    public void testExpression_ExpressionWithVariable() {
        final JBBPNamedFieldInfo info = new JBBPNamedFieldInfo("hello", "hello", 0);
        final byte[] compiled = new byte[]{JBBPCompiler.CODE_INT | JBBPCompiler.FLAG_NAMED};
        final List<JBBPNamedFieldInfo> list = Collections.singletonList(info);

        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("2*hello*6/4+3*2-11%3&hello-~hello", list, compiled);
        final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap();
        map.putField(new JBBPFieldInt(info, 8));
        assertEquals(2 * 8 * 6 / 4 + 3 * 2 - 11 % 3 & 8 - ~8, expr.eval(null, 0, JBBPCompiledBlock.prepare().setCompiledData(compiled).setNamedFieldData(list).setSource("no source").build(), map));
    }

    @Test
    public void testExpression_MustNotThrowStackOverfow() throws Exception {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("1+(2+(3+(4+(5+(6+(7+(8+(9+(10+(11+(12+(13+(14+(15+(16+(17+(18+(19+(20*2)))))))))))))))))))", null, null);
        assertEquals(21, expr.getMaxStackDepth());
        assertEquals(1 + (2 + (3 + (4 + (5 + (6 + (7 + (8 + (9 + (10 + (11 + (12 + (13 + (14 + (15 + (16 + (17 + (18 + (19 + (20 * 2))))))))))))))))))), expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_NegativeConstant() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("-5678", null, null);
        assertEquals(1, expr.getMaxStackDepth());
        assertEquals(-5678, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_UnaryMinusWithSingleConstantInBrackets() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("-(5678)", null, null);
        assertEquals(1, expr.getMaxStackDepth());
        assertEquals(-5678, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_UnaryNotWithSingleConstantInBrackets() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("~(5678)", null, null);
        assertEquals(1, expr.getMaxStackDepth());
        assertEquals(~5678, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_UnaryOperatorsInEasyExpression() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("-6*-7*3*+6*-32", null, null);
        assertEquals(2, expr.getMaxStackDepth());
        assertEquals(-6 * -7 * 3 * +6 * -32, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_UnaryMinusInExpression() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("8*  - 5678", null, null);
        assertEquals(2, expr.getMaxStackDepth());
        assertEquals(8 * -5678, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_UnaryMinusInExpressionAsFirstArgument() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("-5678 * 8", null, null);
        assertEquals(2, expr.getMaxStackDepth());
        assertEquals(-5678 * 8, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_WithoutVariablesAndBrackets() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("10+2*42/3-8%2", null, null);
        assertEquals(3, expr.getMaxStackDepth());
        assertEquals(10 + 2 * 42 / 3 - 8 % 2, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_WithoutVariablesAndWithBrackets() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("(10+2)*42/(3-8)%2", null, null);
        assertEquals(3, expr.getMaxStackDepth());
        assertEquals((10 + 2) * 42 / (3 - 8) % 2, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_unaryMinus() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("-567", null, null);
        assertEquals(-567, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_unaryPlus() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("+567", null, null);
        assertEquals(1, expr.getMaxStackDepth());
        assertEquals(567, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_Mul() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("5623*567", null, null);
        assertEquals(2, expr.getMaxStackDepth());
        assertEquals(5623 * 567, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_SingleCharNamedVar_Mul() {
        final JBBPNamedFieldInfo varA = new JBBPNamedFieldInfo("a", "a", 0);
        final byte[] compiled = new byte[]{JBBPCompiler.CODE_INT | JBBPCompiler.FLAG_NAMED};
        final List<JBBPNamedFieldInfo> list = Collections.singletonList(varA);

        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("a*2", list, compiled);
        assertEquals(2, expr.getMaxStackDepth());
        final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap();
        map.putField(new JBBPFieldInt(varA, 123));
        assertEquals(123 * 2, expr.eval(null, 0, JBBPCompiledBlock.prepare().setCompiledData(compiled).setNamedFieldData(list).setSource("no source").build(), map));
    }

    @Test
    public void testExpression_Div() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("5623/567", null, null);
        assertEquals(2, expr.getMaxStackDepth());
        assertEquals(5623 / 567, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_Mod() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("5623%567", null, null);
        assertEquals(2, expr.getMaxStackDepth());
        assertEquals(5623 % 567, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_Sub() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("5623-567", null, null);
        assertEquals(2, expr.getMaxStackDepth());
        assertEquals(5623 - 567, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_Add() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("5623+567", null, null);
        assertEquals(2, expr.getMaxStackDepth());
        assertEquals(5623 + 567, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_And() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("5623&567", null, null);
        assertEquals(2, expr.getMaxStackDepth());
        assertEquals(5623 & 567, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_Or() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("5623|567", null, null);
        assertEquals(2, expr.getMaxStackDepth());
        assertEquals(5623 | 567, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_Xor() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("5623^567", null, null);
        assertEquals(2, expr.getMaxStackDepth());
        assertEquals(5623 ^ 567, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_Not() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("~567", null, null);
        assertEquals(1, expr.getMaxStackDepth());
        assertEquals(~567, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_TestUnaryTwoOperators() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("-~567", null, null);
        assertEquals(1, expr.getMaxStackDepth());
        assertEquals(-~567, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_TestUnaryThreeOperators() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("~-~567", null, null);
        assertEquals(1, expr.getMaxStackDepth());
        assertEquals(~-~567, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_TestUnaryTwoOperatorsInExpression() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("~-567-~567", null, null);
        assertEquals(2, expr.getMaxStackDepth());
        assertEquals(~-567 - ~567, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_TestUnaryThreeOperatorsInExpression() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("-~-567-~-567", null, null);
        assertEquals(2, expr.getMaxStackDepth());
        assertEquals(-~-567 - ~-567, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_TestComplexUnaryWithConstant() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("-~-~~567", null, null);
        assertEquals(1, expr.getMaxStackDepth());
        assertEquals(-~-~~567, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_TestComplexUnaryWithConstantAndBrackets() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("-(~-(~~567))", null, null);
        assertEquals(1, expr.getMaxStackDepth());
        assertEquals(-(~-(~~567)), expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_TestComplexUnaryWithExpressionAndBrackets() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("-(~+-(~+~567*-(+34)))", null, null);
        assertEquals(2, expr.getMaxStackDepth());
        assertEquals(-(~+-(~+~567 * -(+34))), expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_MultipleUnaryPlusOutsideBracketsAndOneInBrackets() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("++++++(+54)", null, null);
        assertEquals(1, expr.getMaxStackDepth());
        assertEquals(+(+54), expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_ConstantAndMultipleUnaryPlusOutsideBracketsAndOneInBrackets() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("112++++++(+54)", null, null);
        assertEquals(2, expr.getMaxStackDepth());
        assertEquals(112 + (+54), expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_TestComplexUnaryInConstantExpression() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("445*-~-~+~567", null, null);
        assertEquals(2, expr.getMaxStackDepth());
        assertEquals(445 * -~-~+~567, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_ComplexLogicalWithConstants() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("~23*-1234&~123/(34+89)|3232%56^~2234", null, null);
        assertEquals(4, expr.getMaxStackDepth());
        assertEquals((~23 * -1234 & ~123 / (34 + 89)) | (3232 % 56 ^ ~2234), expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_LeftShift() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("1234<<3", null, null);
        assertEquals(2, expr.getMaxStackDepth());
        assertEquals(1234 << 3, expr.eval(null, 0, null, null));
    }

    @Test(expected = JBBPEvalException.class)
    public void testExpression_LeftShift_ErrorWithoutSecondArgument() {
        new JBBPExpressionEvaluator("1234<<", null, null).eval(null, 0, null, null);
    }

    @Test
    public void testExpression_RightShift() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("1234>>3", null, null);
        assertEquals(2, expr.getMaxStackDepth());
        assertEquals(1234 >> 3, expr.eval(null, 0, null, null));
    }

    @Test(expected = JBBPEvalException.class)
    public void testExpression_RightShift_ErrorWithoutSecondArgument() {
        new JBBPExpressionEvaluator("1234 >>", null, null).eval(null, 0, null, null);
    }

    @Test
    public void testExpression_RightSignShift() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("-1>>>3", null, null);
        assertEquals(2, expr.getMaxStackDepth());
        assertEquals(-1 >>> 3, expr.eval(null, 0, null, null));
    }

    @Test
    public void testExpression_RightSignShiftWithInversion() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("-1>>>~3&7", null, null);
        assertEquals(2, expr.getMaxStackDepth());
        assertEquals(-1 >>> ~3 & 7, expr.eval(null, 0, null, null));
    }

    @Test(expected = JBBPEvalException.class)
    public void testExpression_RightSignShift_ErrorWithoutSecondArgument() {
        new JBBPExpressionEvaluator("1234 >>>", null, null).eval(null, 0, null, null);
    }

    @Test
    public void testExpression_ReverseByte() {
        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("((($v*2050&139536)|($v*32800&558144))*65793>>16)&255", null, null);
        assertEquals(3, expr.getMaxStackDepth());
        assertEquals(JBBPUtils.reverseBitsInByte((byte) 123) & 0xFF, expr.eval(null, 0, null, new JBBPNamedNumericFieldMap(new JBBPExternalValueProvider() {

            @Override
            public int provideArraySize(final String fieldName, final JBBPNamedNumericFieldMap numericFieldMap, final JBBPCompiledBlock compiledBlock) {
                if ("v".equals(fieldName)) {
                    return 123;
                } else {
                    fail("Unexpected field [" + fieldName + ']');
                    return 0;
                }
            }
        })));
    }

    @Test
    public void testExpression_CheckExternalField() {
        final int value = 1234;
        final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap(new JBBPExternalValueProvider() {

            @Override
            public int provideArraySize(final String fieldName, final JBBPNamedNumericFieldMap numericFieldMap, final JBBPCompiledBlock compiledBlock) {
                if (fieldName.equals("value")) {
                    return value;
                }
                assertNotNull(numericFieldMap);
                assertNotNull(compiledBlock);
                fail("Unexpected request for value [" + fieldName + ']');
                return -1;
            }
        });

        final List<JBBPNamedFieldInfo> list = new ArrayList<JBBPNamedFieldInfo>();

        final byte[] compiled = new byte[]{0};
        final JBBPCompiledBlock compiledBlock = JBBPCompiledBlock.prepare().setCompiledData(compiled).setSource("none").setNamedFieldData(list).build();

        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("123*($value-45/3)", list, compiled);
        assertEquals(4, expr.getMaxStackDepth());
        assertEquals(123 * (value - 45 / 3), expr.eval(null, 0, compiledBlock, map));
    }

    @Test
    public void testExpression_CheckExternalFieldAndStreamOffset() throws Exception {
        final int value = 1234;
        final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap(new JBBPExternalValueProvider() {

            @Override
            public int provideArraySize(final String fieldName, final JBBPNamedNumericFieldMap numericFieldMap, final JBBPCompiledBlock compiledBlock) {
                if (fieldName.equals("value")) {
                    return value;
                }
                assertNotNull(numericFieldMap);
                assertNotNull(compiledBlock);
                fail("Unexpected request for value [" + fieldName + ']');
                return -1;
            }
        });

        final List<JBBPNamedFieldInfo> list = new ArrayList<JBBPNamedFieldInfo>();

        final byte[] compiled = new byte[]{0};
        final JBBPCompiledBlock compiledBlock = JBBPCompiledBlock.prepare().setCompiledData(compiled).setSource("none").setNamedFieldData(list).build();

        JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("123*($value-45/3)*$$", list, compiled);
        assertEquals(4, expr.getMaxStackDepth());

        final JBBPBitInputStream inStream = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5}));
        inStream.read();
        inStream.read();
        inStream.read();

        assertEquals(123 * (value - 45 / 3) * 3, expr.eval(inStream, 0, compiledBlock, map));
    }

}
