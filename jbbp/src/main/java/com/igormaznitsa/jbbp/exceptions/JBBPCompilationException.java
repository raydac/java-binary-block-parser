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

import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPToken;

/**
 * The Exception can be thrown during compilation phase and may provide the problematic token.
 *
 * @since 1.0
 */
public class JBBPCompilationException extends JBBPException {
  private static final long serialVersionUID = -7567503709641292590L;

  /**
   * The Problematic token, it can be null.
   */
  private final JBBPToken token;

  /**
   * A Constructor.
   *
   * @param text the text of message for the exception.
   */
  public JBBPCompilationException(final String text) {
    this(text, null);
  }

  /**
   * A Constructor.
   *
   * @param text  the text of message for the exception.
   * @param token the problematic token, it can be null.
   */
  public JBBPCompilationException(final String text, final JBBPToken token) {
    super(text);
    this.token = token;
  }

  /**
   * A Constructor.
   *
   * @param text  the text of message for the exception
   * @param token the problematic token for the exception, it can be null
   * @param cause the root cause for the exception
   */
  public JBBPCompilationException(final String text, final JBBPToken token, final Throwable cause) {
    super(text, cause);
    this.token = token;
  }

  /**
   * Get the problematic token.
   *
   * @return the problematic token or null if the token is undefined
   */
  public JBBPToken getToken() {
    return this.token;
  }
}
