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

package com.igormaznitsa.jbbp.exceptions;

import com.igormaznitsa.jbbp.compiler.varlen.JBBPIntegerValueEvaluator;

/**
 * The Exception is thrown if any errors during execution of an array length evaluator.
 *
 * @since 1.0
 */
public class JBBPEvalException extends JBBPException {
  private static final long serialVersionUID = -8580688001091915787L;

  /**
   * The Evaluator which is the cause of the exception.
   */
  private final JBBPIntegerValueEvaluator evaluator;

  /**
   * A Constructor.
   *
   * @param message   a message, can be null
   * @param evaluator a cause evaluator, can be null
   */
  public JBBPEvalException(final String message, final JBBPIntegerValueEvaluator evaluator) {
    this(message, evaluator, null);
  }

  /**
   * A Constructor.
   *
   * @param message   a message, can be null
   * @param evaluator a cause evaluator, can be null
   * @param cause     a cause exception, can be null
   */
  public JBBPEvalException(final String message, final JBBPIntegerValueEvaluator evaluator,
                           Throwable cause) {
    super(message, cause);
    this.evaluator = evaluator;
  }

  /**
   * get the cause evaluator.
   *
   * @return the cause evaluator for the exception, can be null
   */
  public JBBPIntegerValueEvaluator getEvaluator() {
    return this.evaluator;
  }

}
