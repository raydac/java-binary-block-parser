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

package com.igormaznitsa.jbbp.io;

/**
 * Constants for a bit order of reading operations.
 *
 * @since 1.0
 */
public enum JBBPBitOrder {
  /**
   * Most Significant Bit First means that the most significant bit will arrive first, the 7th bit will be read as the first one.
   * Read data wille be presented in reverse format for Java because Java is LSB0.
   */
  MSB0,
  /**
   * Least Significant Bit First means that the least significant bit will arrive first, the 0th bit will be read as the first one.
   * It is default order for Java.
   */
  LSB0,
  /**
   * Most Significant Bit First means that the most significant bit will arrive first, the 7th bit will be read as the first one.
   * <b>In opposite to MSB0 it doesn't make reverse for data during read and write.</b>
   *
   * @since 3.0.1
   */
  MSB0_DIRECT
}
