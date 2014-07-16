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
package com.igormaznitsa.jbbp.model;

import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;

/**
 * The Interface describes a field which content can be represented as a numeric value.
 */
public interface JBBPNumericField {
  /**
   * Get the field value as integer.
   * @return the field value as integer
   */
  int getAsInt();
 
  /**
   * Get the field value as long
   * @return the field value as long
   */
  long getAsLong();
  
  /**
   * Get the field value as boolean, usually if the value is 0 then false, true otherwise.
   * @return the field value as boolean
   */
  boolean getAsBool();
  
  /**
   * Get field name info for the field.
   * @return the field name info, it can be null for anonymous fields
   */
  JBBPNamedFieldInfo getNameInfo();
  
  /**
   * Get the value in inverted bit order.
   * @return the value in inverted bit order
   */
  long getAsInvertedBitOrder();
}
