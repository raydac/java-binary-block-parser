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

import com.igormaznitsa.jbbp.JBBPNamedNumericFieldMap;
import com.igormaznitsa.jbbp.compiler.*;
import com.igormaznitsa.jbbp.compiler.utils.JBBPCompilerUtils;
import com.igormaznitsa.jbbp.exceptions.JBBPCompilationException;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.utils.JBBPPositionCounter;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.*;
import java.util.*;
import java.util.regex.*;

public class JBBPExpressionEvaluator implements JBBPLengthEvaluator {

  private static final int MAX_STACK_DEPTH = 16;

  private static final int PSEUDOCODE_LEFT_BRACKET = 0;
  private static final int CODE_VAR = 1;
  private static final int CODE_EXTVAR = 2;
  private static final int CODE_CONST = 3;
  private static final int CODE_ADD = 4;
  private static final int CODE_MINUS = 5;
  private static final int CODE_MUL = 6;
  private static final int CODE_DIV = 7;
  private static final int CODE_MOD = 8;
  private static final int CODE_UNARYMINUS = 9;

  private static final int[] PRIORITIES = new int[]{0, 1000, 1000, 1000, 2, 2, 3, 3, 3, 4};

  private final byte[] compiledExpression;
  private final String expressionSource;
  private final String[] externalValueNames;

  private static final char[] supportedOperators = new char[]{'(', '+', '-', '*', '/', '%'};
  private static final Pattern pattern = Pattern.compile("([0-9]+)|([\\(\\)])|([\\%\\*\\+\\-\\/])|([\\S][^\\s\\+\\%\\*\\-\\/\\(\\)]+)");

  public JBBPExpressionEvaluator(final String expression, final List<JBBPNamedFieldInfo> namedFields, final byte[] compiledData) {
    this.expressionSource = expression;

    final Matcher matcher = pattern.matcher(expression);
    int lastFound = -1;

    final ByteArrayOutputStream compiedScript = new ByteArrayOutputStream(256);

    final List<Integer> operationStack = new ArrayList<Integer>();

    boolean prevoperator = false;

    String unaryOperator = null;
    boolean first = true;

    int counterOperators = 0;
    int counterVarsAndConstants = 0;

    final List<String> externalValueNameList = new ArrayList<String>();

    while (matcher.find()) {
      if (lastFound >= 0) {
        // check for skept substring
        final String substr = expression.substring(lastFound, matcher.start());
        if (substr.trim().length() != 0) {
          throw new JBBPCompilationException("Can't recognize part of expression '" + substr + "' [" + expression + ']');
        }
      }

      lastFound = matcher.end();

      final String number = matcher.group(1);
      final String bracket = matcher.group(2);
      final String operator = matcher.group(3);
      final String variable = matcher.group(4);

      if (variable != null) {
        prevoperator = false;
        counterVarsAndConstants++;

        final String normalized = JBBPCompilerUtils.normalizeFieldNameOrPath(variable);
        final int nameIndex;
        final boolean extValue;
        if (normalized.startsWith("$")) {
          extValue = true;
          nameIndex = externalValueNameList.size();
          externalValueNameList.add(normalized.substring(1));
        }
        else {
          extValue = false;
          nameIndex = JBBPCompilerUtils.findIndexForFieldPath(normalized, namedFields);
          if (nameIndex < 0) {
            throw new JBBPCompilationException("Unknown variable [" + variable + ']');
          }
          JBBPCompilerUtils.assertFieldIsNotArrayOrInArray(namedFields.get(nameIndex), namedFields, compiledData);
        }

        try {
          compiedScript.write(extValue ? CODE_EXTVAR : CODE_VAR);
          compiedScript.write(JBBPUtils.packInt(nameIndex));

          if (unaryOperator != null) {
            if ("+".equals(unaryOperator)) {
              // do nothing
            }
            else if ("-".equals(unaryOperator)) {
              compiedScript.write(CODE_UNARYMINUS);
            }
            else {
              throw new JBBPCompilationException("Unsupported unary operator [" + unaryOperator + ']');
            }
          }

        }
        catch (IOException ex) {
          throw new RuntimeException("UNexpected IO exception", ex);
        }
      }
      else if (operator != null) {
        counterOperators++;

        final int code;
        if ("+".equals(operator)) {
          code = CODE_ADD;
        }
        else if ("-".equals(operator)) {
          code = CODE_MINUS;
        }
        else if ("*".equals(operator)) {
          code = CODE_MUL;
        }
        else if ("%".equals(operator)) {
          code = CODE_MOD;
        }
        else if ("/".equals(operator)) {
          code = CODE_DIV;
        }
        else {
          throw new Error("Detected unsupported operator, connect developer for the error [" + operator + ']');
        }

        if (operationStack.isEmpty()) {
          if (first) {
            if (!"+".equals(operator) && !"-".equals(operator)) {
              throw new JBBPCompilationException("Wrong unary operator '" + operator + "' [" + this.expressionSource + ']');
            }
            unaryOperator = operator;
          }
          else {
            operationStack.add(code);
          }
        }
        else {
          if (prevoperator) {
            if (unaryOperator != null) {
              throw new JBBPCompilationException("Operator without argument '" + unaryOperator + "' [" + this.expressionSource + ']');
            }
            if (!"+".equals(operator) && !"-".equals(operator)) {
              throw new JBBPCompilationException("Wrong unary operator '" + operator + "' [" + this.expressionSource + ']');
            }
            unaryOperator = operator;
          }
          else {
            unaryOperator = null;
            final int currentPriority = PRIORITIES[code];
            while (!operationStack.isEmpty()) {
              final int top = operationStack.get(operationStack.size() - 1);
              if (PRIORITIES[top] >= currentPriority) {
                operationStack.remove(operationStack.size() - 1);
                compiedScript.write(top);
              }
              else {
                break;
              }
            }
            operationStack.add(code);
          }
        }
        prevoperator = true;
      }
      else if (bracket != null) {
        prevoperator = false;
        if ("(".equals(bracket)) {
          operationStack.add(PSEUDOCODE_LEFT_BRACKET);
        }
        else if (")".equals(bracket)) {
          boolean metLeftPart = false;
          while (!operationStack.isEmpty()) {
            final int top = operationStack.remove(operationStack.size() - 1);
            if (top != PSEUDOCODE_LEFT_BRACKET) {
              compiedScript.write(top);
            }
            else {
              metLeftPart = true;
              break;
            }
          }
          if (!metLeftPart) {
            throw new JBBPCompilationException("Detected unclosed bracket [" + this.expressionSource + ']');
          }
        }
        else {
          throw new Error("Detected unsupported bracket, connect developer for the error [" + bracket + ']');
        }
      }
      else if (number != null) {
        counterVarsAndConstants++;

        prevoperator = false;
        try {
          int parsed = Integer.parseInt(number);

          if (unaryOperator != null) {
            if ("+".equals(unaryOperator)) {
              // do nothing
            }
            else if ("-".equals(unaryOperator)) {
              parsed = -parsed;
            }
            else {
              throw new JBBPCompilationException("Unsupported unary operator [" + unaryOperator + ']');
            }
          }

          unaryOperator = null;
          compiedScript.write(CODE_CONST);
          try {
            compiedScript.write(JBBPUtils.packInt(parsed));
          }
          catch (IOException ex) {
            throw new RuntimeException("Unexpected IO exception", ex);
          }
        }
        catch (NumberFormatException ex) {
          throw new JBBPCompilationException("Can't parse number '" + number + "' [" + this.expressionSource + ']', null, ex);
        }
      }
      first = false;
    }

    if (unaryOperator != null) {
      throw new JBBPCompilationException("Unary operator without argument '" + unaryOperator + "' [" + this.expressionSource + ']');
    }

    if (counterOperators == 0) {
      if (counterVarsAndConstants == 0) {
        throw new JBBPCompilationException("Empty expression [" + this.expressionSource + ']');
      }
      else if (counterVarsAndConstants > 1) {
        throw new JBBPCompilationException("No operators [" + this.expressionSource + ']');
      }
    }

    while (!operationStack.isEmpty()) {
      final int top = operationStack.remove(operationStack.size() - 1);
      if (top == PSEUDOCODE_LEFT_BRACKET) {
        throw new JBBPCompilationException("Detected unclosed bracket [" + this.expressionSource + ']');
      }
      compiedScript.write(top);
    }

    if (lastFound < 0) {
      throw new JBBPCompilationException("Can't extract expression [" + this.expressionSource + ']');
    }

    this.compiledExpression = compiedScript.toByteArray();
    this.externalValueNames = externalValueNameList.isEmpty() ? null : externalValueNameList.toArray(new String[externalValueNameList.size()]);
  }

  private void assertStackDepth(final int stackDepth) {
    if (stackDepth >= MAX_STACK_DEPTH) {
      throw new JBBPCompilationException("Can't calculate expression, stack overflow [" + expressionSource + ']');
    }
  }

  public int eval(final JBBPBitInputStream inStream, final int currentCompiledBlockOffset, final JBBPCompiledBlock compiledBlockData, final JBBPNamedNumericFieldMap fieldMap) {
    final int[] stack = new int[MAX_STACK_DEPTH];
    int stackDepth = 0;

    final JBBPPositionCounter counter = new JBBPPositionCounter(0);

    while (counter.get() < this.compiledExpression.length) {
      final int code = this.compiledExpression[counter.getAndIncrease()];
      switch (code) {
        case CODE_EXTVAR:
        case CODE_VAR: {
          assertStackDepth(stackDepth);
          final int index = JBBPUtils.unpackInt(this.compiledExpression, counter);

          stack[stackDepth++] = code == CODE_EXTVAR
                  ? "$".equals(this.externalValueNames[index]) ? (int) inStream.getCounter() : fieldMap.getExternalFieldValue(this.externalValueNames[index], compiledBlockData)
                  : fieldMap.get(compiledBlockData.getNamedFields()[index]).getAsInt();
        }
        break;
        case CODE_CONST: {
          assertStackDepth(stackDepth);

          stack[stackDepth++] = JBBPUtils.unpackInt(this.compiledExpression, counter);
        }
        break;
        case CODE_ADD: {
          if (stackDepth < 2) {
            throw new JBBPCompilationException("'+' needs two arguments [" + this.expressionSource + ']');
          }
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] += top;
        }
        break;
        case CODE_MINUS: {
          if (stackDepth < 2) {
            throw new JBBPCompilationException("'-' needs one or two arguments [" + this.expressionSource + ']');
          }
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] -= top;
        }
        break;
        case CODE_UNARYMINUS: {
          if (stackDepth < 1) {
            throw new JBBPCompilationException("Unary operator '-' needs one argument [" + this.expressionSource + ']');
          }
          stack[stackDepth - 1] = -stack[stackDepth - 1];
        }
        break;
        case CODE_DIV: {
          if (stackDepth < 2) {
            throw new JBBPCompilationException("'/' needs two arguments [" + this.expressionSource + ']');
          }
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] /= top;
        }
        break;
        case CODE_MUL: {
          if (stackDepth < 2) {
            throw new JBBPCompilationException("'*' needs two arguments [" + this.expressionSource + ']');
          }
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] *= top;
        }
        break;
        case CODE_MOD: {
          if (stackDepth < 2) {
            throw new JBBPCompilationException("'%' needs two arguments [" + this.expressionSource + ']');
          }
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] %= top;
        }
        break;
        default:
          throw new Error("Detected unsupported operation, contact developer");
      }
    }

    if (stackDepth != 1) {
      throw new JBBPCompilationException("Wrong expression [" + this.expressionSource + ']');
    }
    return stack[0];
  }

  public static boolean hasExpressionOperators(final String str) {
    for (final char chr : supportedOperators) {
      if (str.indexOf(chr) >= 0) {
        return true;
      }
    }
    return false;
  }
}
