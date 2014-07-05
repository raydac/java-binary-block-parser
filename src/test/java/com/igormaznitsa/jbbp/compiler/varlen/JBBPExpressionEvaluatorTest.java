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
package com.igormaznitsa.jbbp.compiler.varlen;

import com.igormaznitsa.jbbp.JBBPExternalValueProvider;
import com.igormaznitsa.jbbp.JBBPNamedNumericFieldMap;
import com.igormaznitsa.jbbp.compiler.*;
import com.igormaznitsa.jbbp.exceptions.JBBPCompilationException;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.model.JBBPFieldInt;
import java.io.ByteArrayInputStream;
import java.util.*;
import static org.junit.Assert.*;
import org.junit.Test;

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
  public void testExpression_Error_OnlyUnaryOperator() {
    new JBBPExpressionEvaluator("+", null, null);
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

  @Test
  public void testExpression_Constant() {
    JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("5678", null, null);
    assertEquals(5678, expr.eval(null, 0, null, null));
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

    JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("2*hello*6/4+3*2-11%3", list, compiled);
    final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap();
    map.putField(new JBBPFieldInt(info, 8));
    assertEquals(2 * 8 * 6 / 4 + 3 * 2 - 11 % 3, expr.eval(null, 0, JBBPCompiledBlock.prepare().setCompiledData(compiled).setNamedFieldData(list).setSource("no source").build(), map));
  }

  @Test
  public void testExpression_NegativeConstant() {
    JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("-5678", null, null);
    assertEquals(-5678, expr.eval(null, 0, null, null));
  }

  @Test
  public void testExpression_UnaryMinusWithSingleConstantInBrackets() {
    JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("-(5678)", null, null);
    assertEquals(-5678, expr.eval(null, 0, null, null));
  }

  @Test
  public void testExpression_UnaryOperatorsInEasyExpression() {
    JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("-6*-7*3*+6*-32", null, null);
    assertEquals(-6 * -7 * 3 * +6 * -32, expr.eval(null, 0, null, null));
  }

  @Test
  public void testExpression_UnaryMinusInExpression() {
    JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("8*  - 5678", null, null);
    assertEquals(8 * -5678, expr.eval(null, 0, null, null));
  }

  @Test
  public void testExpression_UnaryMinusInExpressionAsFirstArgument() {
    JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("-5678 * 8", null, null);
    assertEquals(-5678 * 8, expr.eval(null, 0, null, null));
  }

  @Test
  public void testExpression_WithoutVariablesAndBrackets() {
    JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("10+2*42/3-8%2", null, null);
    assertEquals(10 + 2 * 42 / 3 - 8 % 2, expr.eval(null, 0, null, null));
  }

  @Test
  public void testExpression_WithoutVariablesAndWithBrackets() {
    JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("(10+2)*42/(3-8)%2", null, null);
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
    assertEquals(567, expr.eval(null, 0, null, null));
  }

  @Test
  public void testExpression_Mul() {
    JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("5623*567", null, null);
    assertEquals(5623 * 567, expr.eval(null, 0, null, null));
  }

  @Test
  public void testExpression_Div() {
    JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("5623/567", null, null);
    assertEquals(5623 / 567, expr.eval(null, 0, null, null));
  }

  @Test
  public void testExpression_Mod() {
    JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("5623%567", null, null);
    assertEquals(5623 % 567, expr.eval(null,0, null, null));
  }

  @Test
  public void testExpression_Sub() {
    JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("5623-567", null, null);
    assertEquals(5623 - 567, expr.eval(null,0, null, null));
  }

  @Test
  public void testExpression_Add() {
    JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("5623+567", null, null);
    assertEquals(5623 + 567, expr.eval(null,0, null, null));
  }

  @Test
  public void testExpression_CheckExternalField() {
    final int value = 1234; 
    final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap(new JBBPExternalValueProvider() {

      public int provideArraySize(final String fieldName, final JBBPNamedNumericFieldMap numericFieldMap, final JBBPCompiledBlock compiledBlock) {
        if (fieldName.equals("value")) return value;
        assertNotNull(numericFieldMap);
        assertNotNull(compiledBlock);
        fail("Unexpected request for value ["+fieldName+']');
        return -1;
      }
    });
   
    final List<JBBPNamedFieldInfo> list = new ArrayList<JBBPNamedFieldInfo>();
    
    final byte [] compiled = new byte[]{0}; 
    final JBBPCompiledBlock compiledBlock = JBBPCompiledBlock.prepare().setCompiledData(compiled).setSource("none").setNamedFieldData(list).build();
    
    JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("123*($value-45/3)", list, compiled);
    assertEquals(123 * (value - 45 / 3), expr.eval(null, 0, compiledBlock, map));
  }

  @Test
  public void testExpression_CheckExternalFieldAndStreamOffset() throws Exception {
    final int value = 1234; 
    final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap(new JBBPExternalValueProvider() {

      public int provideArraySize(final String fieldName, final JBBPNamedNumericFieldMap numericFieldMap, final JBBPCompiledBlock compiledBlock) {
        if (fieldName.equals("value")) return value;
        assertNotNull(numericFieldMap);
        assertNotNull(compiledBlock);
        fail("Unexpected request for value ["+fieldName+']');
        return -1;
      }
    });
   
    final List<JBBPNamedFieldInfo> list = new ArrayList<JBBPNamedFieldInfo>();
    
    final byte [] compiled = new byte[]{0}; 
    final JBBPCompiledBlock compiledBlock = JBBPCompiledBlock.prepare().setCompiledData(compiled).setSource("none").setNamedFieldData(list).build();
    
    JBBPExpressionEvaluator expr = new JBBPExpressionEvaluator("123*($value-45/3)*$$", list, compiled);
    
    final JBBPBitInputStream inStream = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5}));
    inStream.read();
    inStream.read();
    inStream.read();
    
    
    assertEquals(123 * (value - 45 / 3)*3, expr.eval(inStream, 0, compiledBlock, map));
  }

  
}
