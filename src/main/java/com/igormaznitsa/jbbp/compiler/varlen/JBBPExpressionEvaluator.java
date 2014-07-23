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
import com.igormaznitsa.jbbp.exceptions.JBBPEvalException;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.utils.JBBPIntCounter;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * The Class implements an evaluator which can calculate an expression.
 */
public final class JBBPExpressionEvaluator implements JBBPIntegerValueEvaluator {
  private static final long serialVersionUID = -2951446352849455161L;

  /**
   * The Maximum stack depth for the expression.
   */
  private static final int MAX_STACK_DEPTH = 16;

  /**
   * Code for a left bracket.
   */
  private static final int PSEUDOCODE_LEFT_BRACKET = 0;
  /**
   * Code for a variable.
   */
  private static final int CODE_VAR = 1;
  /**
   * Code for a variable provided by an external provider.
   */
  private static final int CODE_EXTVAR = 2;
  /**
   * Code for a constant.
   */
  private static final int CODE_CONST = 3;
  /**
   * Code for unary bit NOT operator.
   */
  private static final int CODE_NOT = 4;
  /**
   * Code for unary '-' operator.
   */
  private static final int CODE_UNARYMINUS = 5;
  /**
   * Code for unary '+' operator.
   */
  private static final int CODE_UNARYPLUS = 6;
  /**
   * Code for ADD '+' operator.
   */
  private static final int CODE_ADD = 7;
  /**
   * Code for SUB '-' operator.
   */
  private static final int CODE_MINUS = 8;
  /**
   * Code for MUL '*' operator.
   */
  private static final int CODE_MUL = 9;
  /**
   * Code for DIV '/' operator.
   */
  private static final int CODE_DIV = 10;
  /**
   * Code for MOD '%' operator.
   */
  private static final int CODE_MOD = 11;
  /**
   * Code for bit OR '|' operator.
   */
  private static final int CODE_OR = 12;
  /**
   * Code for bit XOR '^' operator.
   */
  private static final int CODE_XOR = 13;
  /**
   * Code for bit AND '&' operator.
   */
  private static final int CODE_AND = 14;
  /**
   * Code for left shift '<<' operator.
   */
  private static final int CODE_LSHIFT = 15;
  /**
   * Code for right shift '>>' operator.
   */
  private static final int CODE_RSHIFT = 16;
  /**
   * Code for right sign shift '>>>' operator.
   */
  private static final int CODE_RSIGNSHIFT = 17;

  /**
   * Array of operator priorities for their codes.
   */
  private static final int[] PRIORITIES = new int[]{0, 1000, 1000, 1000, 500, 500, 500, 200, 200, 300, 300, 300, 50, 100, 150, 175, 175, 175};
  /**
   * Array of operator symbols mapped to their codes.
   */
  private static final String[] SYMBOLS = new String[]{"(", "", "", "", "~", "-", "+", "+", "-", "*", "/", "%", "|", "^", "&", "<<", ">>", ">>>"};

  /**
   * The Array contains byte code of compiled expression.
   */
  private final byte[] compiledExpression;
  /**
   * The String contains the script source for expression.
   */
  private final String expressionSource;
  /**
   * The Array contains external variable names which values should be provided externally.
   */
  private final String[] externalValueNames;

  /**
   * Array of first chars of operators to recognize a string as possible expression.
   */
  private static final char[] operatorFirstChars = new char[]{'(', '+', '-', '*', '/', '%', '|', '&', '^', '~', ')', '>', '<'};
  /**
   * The Pattern to parse an expression.
   */
  private static final Pattern pattern = Pattern.compile("([0-9]+)|([\\(\\)])|(<<|>>>|>>|[\\%\\*\\+\\-\\/\\&\\|\\^\\~])|([\\S][^\\<\\>\\s\\+\\%\\*\\-\\/\\(\\)\\&\\|\\^\\~]+)");

  /**
   * Check that a string represents a unary operator.
   * @param operator an operator to be checked, must not be null.
   * @throws JBBPCompilationException if the operator is not supported unary operator.
   */
  private void assertUnaryOperator(final String operator) {
    if (!("+".equals(operator) || "-".equals(operator) || "~".equals(operator))) {
      throw new JBBPCompilationException("Wrong unary operator '" + operator + "' [" + this.expressionSource + ']');
    }
  }

  /**
   * Encode code of an operator to code of similar unary operator.
   * @param code a code of operator.
   * @return code of an unary similar operator if it exists, the same code otherwise
   */
  private static int codeToUnary(final int code) {
    switch (code) {
      case CODE_MINUS:
        return CODE_UNARYMINUS;
      case CODE_ADD:
        return CODE_UNARYPLUS;
      default:
        return code;
    }
  }

  /**
   * The Constructor. It makes compilation an expression into internal representation.
   * @param expression a source expression, must not be null
   * @param namedFields a named field info list, must not be null 
   * @param compiledData the current compiled data block of JBBP parent script for the expression, must not be null
   * @throws JBBPCompilationException if any problem in compilation
   */
  public JBBPExpressionEvaluator(final String expression, final List<JBBPNamedFieldInfo> namedFields, final byte[] compiledData) {
    this.expressionSource = expression;

    final Matcher matcher = pattern.matcher(expression);
    int lastFound = -1;

    final ByteArrayOutputStream compiedScript = new ByteArrayOutputStream(256);

    final List<Integer> operationStack = new ArrayList<Integer>();

    boolean prevoperator = false;

    int unaryOperatorCode = -1;
    boolean theFirstInTheSubExpression = true;

    int counterOperators = 0;
    int counterVarsAndConstants = 0;

    final List<String> externalValueNameList = new ArrayList<String>();

    while (matcher.find()) {
      if (lastFound >= 0) {
        // check for skipped substring
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

        final String normalized = JBBPUtils.normalizeFieldNameOrPath(variable);
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

          if (unaryOperatorCode > 0) {
            switch (unaryOperatorCode) {
              case CODE_ADD: {
                // do nothing
              }
              break;
              case CODE_MINUS:
              case CODE_UNARYMINUS: {
                compiedScript.write(CODE_UNARYMINUS);
              }
              break;
              case CODE_NOT: {
                compiedScript.write(CODE_NOT);
              }
              break;
              default:
                throw new Error("Unsupported unary operator [" + SYMBOLS[unaryOperatorCode] + ']');
            }
            unaryOperatorCode = -1;
          }

        }
        catch (IOException ex) {
          throw new Error("Unexpected IO exception", ex);
        }
        theFirstInTheSubExpression = false;
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
        else if ("&".equals(operator)) {
          code = CODE_AND;
        }
        else if ("|".equals(operator)) {
          code = CODE_OR;
        }
        else if ("^".equals(operator)) {
          code = CODE_XOR;
        }
        else if ("~".equals(operator)) {
          code = CODE_NOT;
        }
        else if ("<<".equals(operator)) {
          code = CODE_LSHIFT;
        }
        else if (">>".equals(operator)) {
          code = CODE_RSHIFT;
        }
        else if (">>>".equals(operator)) {
          code = CODE_RSIGNSHIFT;
        }
        else {
          throw new Error("Detected unsupported operator, connect developer for the error [" + operator + ']');
        }

        if (theFirstInTheSubExpression) {
          assertUnaryOperator(operator);
          unaryOperatorCode = codeToUnary(code);
        }
        else if (prevoperator) {
          if (unaryOperatorCode > 0) {
            assertUnaryOperator(operator);
            operationStack.add(unaryOperatorCode);
            unaryOperatorCode = codeToUnary(code);
          }
          else {
            assertUnaryOperator(operator);
            unaryOperatorCode = codeToUnary(code);
          }
        }
        else {
          unaryOperatorCode = -1;
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
        prevoperator = true;
        theFirstInTheSubExpression = false;
      }
      else if (bracket != null) {
        prevoperator = false;
        if ("(".equals(bracket)) {
          if (unaryOperatorCode > 0) {
            operationStack.add(unaryOperatorCode);
            unaryOperatorCode = -1;
          }
          operationStack.add(PSEUDOCODE_LEFT_BRACKET);
          theFirstInTheSubExpression = true;
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

          if (unaryOperatorCode >= 0) {
            switch (unaryOperatorCode) {
              case CODE_UNARYPLUS:
              case CODE_ADD: {
                // do nothing
              }
              break;
              case CODE_UNARYMINUS:
              case CODE_MINUS: {
                parsed = -parsed;
              }
              break;
              case CODE_NOT: {
                parsed = ~parsed;
              }
              break;
              default: {
                throw new Error("Unsupported unary operator [" + SYMBOLS[unaryOperatorCode] + ']');
              }
            }
          }

          unaryOperatorCode = -1;
          compiedScript.write(CODE_CONST);
          try {
            compiedScript.write(JBBPUtils.packInt(parsed));
          }
          catch (IOException ex) {
            throw new RuntimeException("Unexpected IO exception", ex);
          }
        }
        catch (NumberFormatException ex) {
          throw new JBBPCompilationException("Can't parse a numeric constant, only decimal integers are supported '" + number + "' [" + this.expressionSource + ']', null, ex);
        }
        theFirstInTheSubExpression = false;
      }
    }

    if (unaryOperatorCode > 0) {
      throw new JBBPCompilationException("Unary operator without argument '" + SYMBOLS[unaryOperatorCode] + "' [" + this.expressionSource + ']');
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

  /**
   * Check that the current stack depth is enough.
   * @param stackDepth the current stack depth
   * @throws JBBPCompilationException if the stack depth is not enough
   */
  private void assertStackDepth(final int stackDepth) {
    if (stackDepth >= MAX_STACK_DEPTH) {
      throw new JBBPCompilationException("Can't calculate expression, stack overflow [" + expressionSource + ']');
    }
  }

  /**
   * Evaluate the expression.
   * @param inStream the input stream of data, must not be null
   * @param currentCompiledBlockOffset the current offset inside the compiled JBBP script
   * @param compiledBlockData the compiled JBBP script, must not be null
   * @param fieldMap the named field info map, must not be null
   * @return calculated integer result of the expression
   * @throws JBBPEvalException if there is any problem during processing
   */
  public int eval(final JBBPBitInputStream inStream, final int currentCompiledBlockOffset, final JBBPCompiledBlock compiledBlockData, final JBBPNamedNumericFieldMap fieldMap) {
    final int[] stack = new int[MAX_STACK_DEPTH];
    int stackDepth = 0;

    final JBBPIntCounter counter = new JBBPIntCounter();

    while (counter.get() < this.compiledExpression.length) {
      final int code = this.compiledExpression[counter.getAndIncrement()];
      switch (code) {
        case CODE_EXTVAR:
        case CODE_VAR: {
          assertStackDepth(stackDepth);
          final int index = JBBPUtils.unpackInt(this.compiledExpression, counter);

          stack[stackDepth++] = code == CODE_EXTVAR
                  ? "$".equals(this.externalValueNames[index]) ? (int) inStream.getCounter() : fieldMap.getExternalFieldValue(this.externalValueNames[index], compiledBlockData, this)
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
            throw new JBBPEvalException("'+' needs two arguments [" + this.expressionSource + ']',this);
          }
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] += top;
        }
        break;
        case CODE_AND: {
          if (stackDepth < 2) {
            throw new JBBPEvalException("'&' needs two arguments [" + this.expressionSource + ']',this);
          }
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] &= top;
        }
        break;
        case CODE_OR: {
          if (stackDepth < 2) {
            throw new JBBPEvalException("'|' needs two arguments [" + this.expressionSource + ']',this);
          }
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] |= top;
        }
        break;
        case CODE_XOR: {
          if (stackDepth < 2) {
            throw new JBBPEvalException("'^' needs two arguments [" + this.expressionSource + ']',this);
          }
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] ^= top;
        }
        break;
        case CODE_MINUS: {
          if (stackDepth < 2) {
            throw new JBBPEvalException("'-' needs one or two arguments [" + this.expressionSource + ']',this);
          }
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] -= top;
        }
        break;
        case CODE_UNARYMINUS: {
          if (stackDepth < 1) {
            throw new JBBPEvalException("Unary operator '-' needs one argument [" + this.expressionSource + ']',this);
          }
          stack[stackDepth - 1] = -stack[stackDepth - 1];
        }
        break;
        case CODE_UNARYPLUS: {
          if (stackDepth < 1) {
            throw new JBBPEvalException("Unary operator '-' needs one argument [" + this.expressionSource + ']',this);
          }
        }
        break;
        case CODE_NOT: {
          if (stackDepth < 1) {
            throw new JBBPEvalException("Unary operator '~' needs one argument [" + this.expressionSource + ']',this);
          }
          stack[stackDepth - 1] = ~stack[stackDepth - 1];
        }
        break;
        case CODE_DIV: {
          if (stackDepth < 2) {
            throw new JBBPEvalException("'/' needs two arguments [" + this.expressionSource + ']',this);
          }
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] /= top;
        }
        break;
        case CODE_MUL: {
          if (stackDepth < 2) {
            throw new JBBPEvalException("'*' needs two arguments [" + this.expressionSource + ']',this);
          }
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] *= top;
        }
        break;
        case CODE_MOD: {
          if (stackDepth < 2) {
            throw new JBBPEvalException("'%' needs two arguments [" + this.expressionSource + ']',this);
          }
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] %= top;
        }
        break;
        case CODE_LSHIFT: {
          if (stackDepth < 2) {
            throw new JBBPEvalException("'<<' needs two arguments [" + this.expressionSource + ']',this);
          }
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] <<= top;
        }
        break;
        case CODE_RSHIFT: {
          if (stackDepth < 2) {
            throw new JBBPEvalException("'>>' needs two arguments [" + this.expressionSource + ']',this);
          }
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] >>= top;
        }
        break;
        case CODE_RSIGNSHIFT: {
          if (stackDepth < 2) {
            throw new JBBPEvalException("'>>>' needs two arguments [" + this.expressionSource + ']',this);
          }
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] >>>= top;
        }
        break;
        default:
          throw new Error("Detected unsupported operation, contact developer");
      }
    }

    if (stackDepth != 1) {
      throw new JBBPEvalException("Wrong expression [" + this.expressionSource + ']',this);
    }
    return stack[0];
  }

  /**
   * Check that a string has a char of operators.
   * @param str a string to be checked, must not be null
   * @return true if the string contains a char of an operator, false otherwise
   */
  public static boolean hasExpressionOperators(final String str) {
    for (final char chr : operatorFirstChars) {
      if (str.indexOf(chr) >= 0) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return this.expressionSource;
  }
}
