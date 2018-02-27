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
 * The Exception can be thrown when detected incompatible or unusable parameter.
 *
 * @since 1.1.1
 */
public class JBBPIllegalArgumentException extends JBBPIOException {
  private static final long serialVersionUID = 2811626713945893782L;

  public JBBPIllegalArgumentException(String message) {
    super(message);
  }

  public JBBPIllegalArgumentException(String message, Throwable cause) {
    super(message, cause);
  }

}
