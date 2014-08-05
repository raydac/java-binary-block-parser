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
package com.igormaznitsa.jbbp.exceptions;

/**
 * The Exception can be thrown during parsing process.
 */
public class JBBPParsingException extends JBBPCompilationException {
  private static final long serialVersionUID = 3397335881291955769L;

  /**
   * A Constructor.
   * @param message the exception error message 
   */
  public JBBPParsingException(final String message) {
    this(message,null);
  }

  /**
   * A Constructor.
   * @param message the exception error message
   * @param cause the root cause for the exception
   */
  public JBBPParsingException(final String message, final Throwable cause) {
    super(message,null, cause);
  }

}
