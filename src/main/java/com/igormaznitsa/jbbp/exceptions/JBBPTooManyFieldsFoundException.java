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

import com.igormaznitsa.jbbp.model.JBBPAbstractField;

public class JBBPTooManyFieldsFoundException extends JBBPFinderException {
  private static final long serialVersionUID = -7805676497685397609L;

  private final int numberOfInstances;
  
  public JBBPTooManyFieldsFoundException(final int numberOfInstances, final String message, final String nameOrPath, final Class<? extends JBBPAbstractField> fieldType) {
    super(message, nameOrPath, fieldType);
    this.numberOfInstances = numberOfInstances;
  }

  public int getNumberOfFoundInstances(){
    return this.numberOfInstances;
  }
}
