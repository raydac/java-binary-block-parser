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

package com.igormaznitsa.jbbp.model;

/**
 * The Interface describes a field which content can be represented as a numeric value.
 *
 * @since 1.0
 */
public interface JBBPNumericField extends JBBPInvertableBitOrder, JBBPNamedField {
  /**
   * Get the field value as integer.
   *
   * @return the field value as integer
   * @throws IllegalStateException can be thrown if impossible to represent saved value as int
   */
  int getAsInt();

  /**
   * Get the field value as double.
   *
   * @return the field value as double
   * @throws IllegalStateException can be thrown if impossible to represent saved value as double
   * @since 1.4.0
   */
  double getAsDouble();

  /**
   * Get the field value as float.
   *
   * @return the field value as float
   * @throws IllegalStateException can be thrown if impossible to represent saved value as float
   * @since 1.4.0
   */
  float getAsFloat();

  /**
   * Get the field value as long
   *
   * @return the field value as long
   * @throws IllegalStateException can be thrown if impossible to represent saved value as long
   */
  long getAsLong();

  /**
   * Get the field value as boolean, usually if the value is 0 then false, true otherwise.
   *
   * @return the field value as boolean
   * @throws IllegalStateException can be thrown if impossible to represent saved value as boolean
   */
  boolean getAsBool();

}
