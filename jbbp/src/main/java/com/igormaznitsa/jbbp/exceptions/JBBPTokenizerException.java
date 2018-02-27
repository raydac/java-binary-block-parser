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

/**
 * The Exception can be thrown during parsing sources for tokens and allows to figure out the position of the problematic token.
 *
 * @since 1.0
 */
public class JBBPTokenizerException extends JBBPCompilationException {
  private static final long serialVersionUID = -1132154077305894146L;

  /**
   * The Token position.
   */
  private final int position;

  /**
   * The Constructor.
   *
   * @param message the exception message.
   * @param pos     the position of a problematic token inside sources.
   */
  public JBBPTokenizerException(final String message, final int pos) {
    super(message);
    this.position = pos;
  }

  /**
   * get the position in sources of the problematic token.
   *
   * @return the position or -1 if the position is unknown.
   */
  public int getPosition() {
    return this.position;
  }
}
