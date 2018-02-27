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

import com.igormaznitsa.jbbp.JBBPNamedNumericFieldMap;
import com.igormaznitsa.jbbp.compiler.JBBPCompiledBlock;
import com.igormaznitsa.jbbp.compiler.JBBPCompilerUtils;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.compiler.conversion.ExpressionEvaluatorVisitor;
import com.igormaznitsa.jbbp.exceptions.JBBPCompilationException;
import com.igormaznitsa.jbbp.exceptions.JBBPEvalException;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.utils.JBBPIntCounter;
import com.igormaznitsa.jbbp.utils.JBBPUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Class implements an evaluator which can calculate an expression.
 *
 * @since 1.0
 */
public final class JBBPExpressionEvaluator implements JBBPIntegerValueEvaluator {

  private static final long serialVersionUID = -2951446352849455161L;

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
  private static final int[] PRIORITIES = new int[] {0, 1000, 1000, 1000, 500, 500, 500, 200, 200, 300, 300, 300, 50, 100, 150, 175, 175, 175};
  /**
   * Array of operator symbols mapped to their codes.
   */
  private static final String[] SYMBOLS = new String[] {"(", "", "", "", "~", "-", "+", "+", "-", "*", "/", "%", "|", "^", "&", "<<", ">>", ">>>"};
  /**
   * Array of first chars of operators to recognize a string as possible expression.
   */
  private static final char[] OPERATOR_FIRST_CHARS = new char[] {'(', '+', '-', '*', '/', '%', '|', '&', '^', '~', ')', '>', '<'};
  /**
   * The Pattern to parse an expression.
   */
  private static final Pattern PATTERN = Pattern.compile("([0-9]+)|([\\(\\)])|(<<|>>>|>>|[\\%\\*\\+\\-\\/\\&\\|\\^\\~])|([\\S][^\\<\\>\\s\\+\\%\\*\\-\\/\\(\\)\\&\\|\\^\\~]*)");
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
   * Max stack depth for the expression.
   *
   * @since 1.2.1
   */
  private final int maxStackDepth;

  /**
   * The Constructor. It makes compilation an expression into internal representation.
   *
   * @param expression   a source expression, must not be null
   * @param namedFields  a named field info list, must not be null
   * @param compiledData the current compiled data block of JBBP parent script for the expression, must not be null
   * @throws JBBPCompilationException if any problem in compilation
   */
  public JBBPExpressionEvaluator(final String expression, final List<JBBPNamedFieldInfo> namedFields, final byte[] compiledData) {
    this.expressionSource = expression;

    final Matcher matcher = PATTERN.matcher(expression);
    int lastFound = -1;

    final ByteArrayOutputStream compiledScript = new ByteArrayOutputStream(256);

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
        } else {
          extValue = false;
          nameIndex = JBBPCompilerUtils.findIndexForFieldPath(normalized, namedFields);
          if (nameIndex < 0) {
            throw new JBBPCompilationException("Unknown variable [" + variable + ']');
          }
          JBBPCompilerUtils.assertFieldIsNotArrayOrInArray(namedFields.get(nameIndex), namedFields, compiledData);
        }

        try {
          compiledScript.write(extValue ? CODE_EXTVAR : CODE_VAR);
          compiledScript.write(JBBPUtils.packInt(nameIndex));

          if (unaryOperatorCode > 0) {
            switch (unaryOperatorCode) {
              case CODE_ADD: {
                // do nothing
              }
              break;
              case CODE_MINUS:
              case CODE_UNARYMINUS: {
                compiledScript.write(CODE_UNARYMINUS);
              }
              break;
              case CODE_NOT: {
                compiledScript.write(CODE_NOT);
              }
              break;
              default:
                throw new Error("Unsupported unary operator [" + SYMBOLS[unaryOperatorCode] + ']');
            }
            unaryOperatorCode = -1;
          }

        } catch (IOException ex) {
          throw new Error("Unexpected IO exception", ex);
        }
        theFirstInTheSubExpression = false;
      } else if (operator != null) {
        counterOperators++;

        final int code;
        if ("+".equals(operator)) {
          code = CODE_ADD;
        } else if ("-".equals(operator)) {
          code = CODE_MINUS;
        } else if ("*".equals(operator)) {
          code = CODE_MUL;
        } else if ("%".equals(operator)) {
          code = CODE_MOD;
        } else if ("/".equals(operator)) {
          code = CODE_DIV;
        } else if ("&".equals(operator)) {
          code = CODE_AND;
        } else if ("|".equals(operator)) {
          code = CODE_OR;
        } else if ("^".equals(operator)) {
          code = CODE_XOR;
        } else if ("~".equals(operator)) {
          code = CODE_NOT;
        } else if ("<<".equals(operator)) {
          code = CODE_LSHIFT;
        } else if (">>".equals(operator)) {
          code = CODE_RSHIFT;
        } else if (">>>".equals(operator)) {
          code = CODE_RSIGNSHIFT;
        } else {
          throw new Error("Detected unsupported operator, contact developer [" + operator + ']');
        }

        if (theFirstInTheSubExpression) {
          assertUnaryOperator(operator);
          unaryOperatorCode = codeToUnary(code);
        } else if (prevoperator) {
          if (unaryOperatorCode > 0) {
            assertUnaryOperator(operator);
            operationStack.add(unaryOperatorCode);
            unaryOperatorCode = codeToUnary(code);
          } else {
            assertUnaryOperator(operator);
            unaryOperatorCode = codeToUnary(code);
          }
        } else {
          unaryOperatorCode = -1;
          final int currentPriority = PRIORITIES[code];
          while (!operationStack.isEmpty()) {
            final int top = operationStack.get(operationStack.size() - 1);
            if (PRIORITIES[top] >= currentPriority) {
              operationStack.remove(operationStack.size() - 1);
              compiledScript.write(top);
            } else {
              break;
            }
          }
          operationStack.add(code);
        }
        prevoperator = true;
        theFirstInTheSubExpression = false;
      } else if (bracket != null) {
        prevoperator = false;
        if ("(".equals(bracket)) {
          if (unaryOperatorCode > 0) {
            operationStack.add(unaryOperatorCode);
            unaryOperatorCode = -1;
          }
          operationStack.add(PSEUDOCODE_LEFT_BRACKET);
          theFirstInTheSubExpression = true;
        } else if (")".equals(bracket)) {
          boolean metLeftPart = false;
          while (!operationStack.isEmpty()) {
            final int top = operationStack.remove(operationStack.size() - 1);
            if (top != PSEUDOCODE_LEFT_BRACKET) {
              compiledScript.write(top);
            } else {
              metLeftPart = true;
              break;
            }
          }
          if (!metLeftPart) {
            throw new JBBPCompilationException("Detected unclosed bracket [" + this.expressionSource + ']');
          }
        } else {
          throw new Error("Detected unsupported bracket, connect developer for the error [" + bracket + ']');
        }
      } else if (number != null) {
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
          compiledScript.write(CODE_CONST);
          try {
            compiledScript.write(JBBPUtils.packInt(parsed));
          } catch (IOException ex) {
            throw new RuntimeException("Unexpected IO exception", ex);
          }
        } catch (NumberFormatException ex) {
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
      } else if (counterVarsAndConstants > 1) {
        throw new JBBPCompilationException("No operators [" + this.expressionSource + ']');
      }
    }

    while (!operationStack.isEmpty()) {
      final int top = operationStack.remove(operationStack.size() - 1);
      if (top == PSEUDOCODE_LEFT_BRACKET) {
        throw new JBBPCompilationException("Detected unclosed bracket [" + this.expressionSource + ']');
      }
      compiledScript.write(top);
    }

    if (lastFound < 0) {
      throw new JBBPCompilationException("Can't extract expression [" + this.expressionSource + ']');
    }

    this.compiledExpression = compiledScript.toByteArray();
    this.externalValueNames = externalValueNameList.isEmpty() ? null : externalValueNameList.toArray(new String[externalValueNameList.size()]);

    this.maxStackDepth = calculateMaxStackDepth();
  }

  /**
   * Encode code of an operator to code of similar unary operator.
   *
   * @param code a code of operator.
   * @return code of an unary similar operator if it exists, the same code otherwise
   */
  private static int codeToUnary(final int code) {
    final int result;

    switch (code) {
      case CODE_MINUS:
        result = CODE_UNARYMINUS;
        break;
      case CODE_ADD:
        result = CODE_UNARYPLUS;
        break;
      default:
        result = code;
        break;
    }
    return result;
  }

  private static String code2operator(final int code) {
    final String result;
    switch (code) {
      case CODE_AND:
        result = "&";
        break;
      case CODE_UNARYPLUS:
      case CODE_ADD:
        result = "+";
        break;
      case CODE_OR:
        result = "|";
        break;
      case CODE_DIV:
        result = "/";
        break;
      case CODE_MUL:
        result = "*";
        break;
      case CODE_MOD:
        result = "%";
        break;
      case CODE_LSHIFT:
        result = "<<";
        break;
      case CODE_RSHIFT:
        result = ">>";
        break;
      case CODE_RSIGNSHIFT:
        result = ">>>";
        break;
      case CODE_UNARYMINUS:
      case CODE_MINUS:
        result = "-";
        break;
      case CODE_XOR:
        result = "^";
        break;
      default:
        result = "CODE:" + code;
        break;
    }
    return result;
  }

  /**
   * Check that a string has a char of operators.
   *
   * @param str a string to be checked, must not be null
   * @return true if the string contains a char of an operator, false otherwise
   */
  public static boolean hasExpressionOperators(final String str) {

    boolean result = false;

    for (final char chr : OPERATOR_FIRST_CHARS) {
      if (str.indexOf(chr) >= 0) {
        result = true;
        break;
      }
    }
    return result;
  }

  /**
   * Check that a string represents a unary operator.
   *
   * @param operator an operator to be checked, must not be null.
   * @throws JBBPCompilationException if the operator is not supported unary operator.
   */
  private void assertUnaryOperator(final String operator) {
    if (!("+".equals(operator) || "-".equals(operator) || "~".equals(operator))) {
      throw new JBBPCompilationException("Wrong unary operator '" + operator + "' [" + this.expressionSource + ']');
    }
  }

  private int calculateMaxStackDepth() {
    int stackMaxPosition = 0;
    int stackPosition = 0;

    final JBBPIntCounter counter = new JBBPIntCounter();

    while (counter.get() < this.compiledExpression.length) {
      final int code = this.compiledExpression[counter.getAndIncrement()];
      switch (code) {
        case CODE_EXTVAR:
        case CODE_VAR:
        case CODE_CONST: {
          JBBPUtils.unpackInt(this.compiledExpression, counter);
          stackPosition++;
          stackMaxPosition = Math.max(stackPosition, stackMaxPosition);
        }
        break;
        case CODE_AND:
        case CODE_ADD:
        case CODE_OR:
        case CODE_DIV:
        case CODE_MUL:
        case CODE_MOD:
        case CODE_LSHIFT:
        case CODE_RSHIFT:
        case CODE_RSIGNSHIFT:
        case CODE_MINUS:
        case CODE_XOR: {
          if (stackPosition < 2) {
            throw new JBBPEvalException("Operator '" + code2operator(code) + "' needs two operands", this);
          }
          // decrease for one position
          stackPosition--;
        }
        break;
        case CODE_UNARYMINUS:
        case CODE_UNARYPLUS:
        case CODE_NOT: {
          // stack not changed
          if (stackPosition < 1) {
            throw new JBBPEvalException("Operator '" + code2operator(code) + "' needs operand", this);
          }
        }
        break;
        default:
          throw new Error("Detected unsupported operation, contact developer");
      }
    }

    if (stackPosition != 1 || stackPosition > stackMaxPosition) {
      throw new JBBPEvalException("Wrong expression [" + this.expressionSource + "] (" + stackPosition + ':' + stackMaxPosition + ')', this);
    }
    return stackMaxPosition;
  }

  /**
   * Get the max stack depth needed for the expression.
   *
   * @return max stack depth for expression
   * @since 1.2.1
   */
  public int getMaxStackDepth() {
    return this.maxStackDepth;
  }

  /**
   * Evaluate the expression.
   *
   * @param inStream                   the input stream of data, must not be null
   * @param currentCompiledBlockOffset the current offset inside the compiled JBBP script
   * @param compiledBlockData          the compiled JBBP script, must not be null
   * @param fieldMap                   the named field info map, must not be null
   * @return calculated integer result of the expression
   * @throws JBBPEvalException if there is any problem during processing
   */
  @Override
  public int eval(final JBBPBitInputStream inStream, final int currentCompiledBlockOffset, final JBBPCompiledBlock compiledBlockData, final JBBPNamedNumericFieldMap fieldMap) {
    final int[] stack = new int[this.maxStackDepth];

    int stackDepth = 0;

    final JBBPIntCounter counter = new JBBPIntCounter();

    while (counter.get() < this.compiledExpression.length) {
      final int code = this.compiledExpression[counter.getAndIncrement()];
      switch (code) {
        case CODE_EXTVAR:
        case CODE_VAR: {
          final int index = JBBPUtils.unpackInt(this.compiledExpression, counter);

          stack[stackDepth++] = code == CODE_EXTVAR
              ? "$".equals(this.externalValueNames[index]) ? (int) inStream.getCounter() : fieldMap.getExternalFieldValue(this.externalValueNames[index], compiledBlockData, this)
              : fieldMap.get(compiledBlockData.getNamedFields()[index]).getAsInt();
        }
        break;
        case CODE_CONST: {
          stack[stackDepth++] = JBBPUtils.unpackInt(this.compiledExpression, counter);
        }
        break;
        case CODE_ADD: {
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] += top;
        }
        break;
        case CODE_AND: {
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] &= top;
        }
        break;
        case CODE_OR: {
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] |= top;
        }
        break;
        case CODE_XOR: {
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] ^= top;
        }
        break;
        case CODE_MINUS: {
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] -= top;
        }
        break;
        case CODE_UNARYMINUS: {
          stack[stackDepth - 1] = -stack[stackDepth - 1];
        }
        break;
        case CODE_UNARYPLUS: {
          // do nothing
        }
        break;
        case CODE_NOT: {
          stack[stackDepth - 1] = ~stack[stackDepth - 1];
        }
        break;
        case CODE_DIV: {
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] /= top;
        }
        break;
        case CODE_MUL: {
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] *= top;
        }
        break;
        case CODE_MOD: {
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] %= top;
        }
        break;
        case CODE_LSHIFT: {
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] <<= top;
        }
        break;
        case CODE_RSHIFT: {
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] >>= top;
        }
        break;
        case CODE_RSIGNSHIFT: {
          final int top = stack[--stackDepth];
          stack[stackDepth - 1] >>>= top;
        }
        break;
        default:
          throw new Error("Detected unsupported operation, contact developer");
      }
    }

    return stack[0];
  }

  @Override
  public void visitItems(final JBBPCompiledBlock block, final int currentCompiledBlockOffset, final ExpressionEvaluatorVisitor visitor) {
    visitor.visitStart();

    final JBBPIntCounter counter = new JBBPIntCounter();

    while (counter.get() < this.compiledExpression.length) {
      final int code = this.compiledExpression[counter.getAndIncrement()];
      switch (code) {
        case CODE_EXTVAR:
        case CODE_VAR: {
          final int index = JBBPUtils.unpackInt(this.compiledExpression, counter);

          if (code == CODE_EXTVAR) {
            if ("$".equals(this.externalValueNames[index])) {
              visitor.visitSpecial(ExpressionEvaluatorVisitor.Special.STREAM_COUNTER);
            } else {
              visitor.visitField(null, this.externalValueNames[index]);
            }
          } else {
            visitor.visitField(block.getNamedFields()[index], null);
          }
        }
        break;
        case CODE_CONST:
          visitor.visitConstant(JBBPUtils.unpackInt(this.compiledExpression, counter));
          break;
        case CODE_ADD:
          visitor.visitOperator(ExpressionEvaluatorVisitor.Operator.ADD);
          break;
        case CODE_AND:
          visitor.visitOperator(ExpressionEvaluatorVisitor.Operator.AND);
          break;
        case CODE_OR:
          visitor.visitOperator(ExpressionEvaluatorVisitor.Operator.OR);
          break;
        case CODE_XOR:
          visitor.visitOperator(ExpressionEvaluatorVisitor.Operator.XOR);
          break;
        case CODE_MINUS:
          visitor.visitOperator(ExpressionEvaluatorVisitor.Operator.SUB);
          break;
        case CODE_UNARYMINUS:
          visitor.visitOperator(ExpressionEvaluatorVisitor.Operator.UNARY_MINUS);
          break;
        case CODE_UNARYPLUS:
          visitor.visitOperator(ExpressionEvaluatorVisitor.Operator.UNARY_PLUS);
          break;
        case CODE_NOT:
          visitor.visitOperator(ExpressionEvaluatorVisitor.Operator.NOT);
          break;
        case CODE_DIV:
          visitor.visitOperator(ExpressionEvaluatorVisitor.Operator.DIV);
          break;
        case CODE_MUL:
          visitor.visitOperator(ExpressionEvaluatorVisitor.Operator.MUL);
          break;
        case CODE_MOD:
          visitor.visitOperator(ExpressionEvaluatorVisitor.Operator.MOD);
          break;
        case CODE_LSHIFT:
          visitor.visitOperator(ExpressionEvaluatorVisitor.Operator.LSHIFT);
          break;
        case CODE_RSHIFT:
          visitor.visitOperator(ExpressionEvaluatorVisitor.Operator.RSHIFT);
          break;
        case CODE_RSIGNSHIFT:
          visitor.visitOperator(ExpressionEvaluatorVisitor.Operator.URSHIFT);
          break;
        default:
          throw new Error("Detected unsupported operation, contact developer");
      }
    }
    visitor.visitEnd();
  }

  @Override
  public String toString() {
    return this.expressionSource;
  }
}
