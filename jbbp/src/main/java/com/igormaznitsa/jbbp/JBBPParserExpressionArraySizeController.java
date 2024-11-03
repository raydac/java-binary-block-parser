package com.igormaznitsa.jbbp;

import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.compiler.varlen.JBBPIntegerValueEvaluator;

/**
 * Controller to get value for every array field which size calculated by expression.
 *
 * @see JBBPParser#setExpressionArraySizeController(JBBPParserExpressionArraySizeController)
 * @see JBBPParser#getExpressionArraySizeController()
 * @since 2.1.0
 */
@FunctionalInterface
public interface JBBPParserExpressionArraySizeController {
  /**
   * Called for every calculation of an array size by expression.
   *
   * @param parser              source parser, must not be null
   * @param expressionEvaluator expression evaluator used for calculation, must not be null
   * @param fieldInfo           target field info, must not be null
   * @param calculatedArraySize calculated array size
   * @return array size which can be same as provided size or changed
   */
  int onCalculatedArraySize(JBBPParser parser, JBBPIntegerValueEvaluator expressionEvaluator,
                            JBBPNamedFieldInfo fieldInfo, int calculatedArraySize);
}
