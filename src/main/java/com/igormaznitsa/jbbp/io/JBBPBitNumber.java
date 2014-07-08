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
package com.igormaznitsa.jbbp.io;

/**
 * Constants allowed bit numbers for bit operations.
 */
public enum JBBPBitNumber {
  BITS_1(1),
  BITS_2(2),
  BITS_3(3),
  BITS_4(4),
  BITS_5(5),
  BITS_6(6),
  BITS_7(7),
  BITS_8(8);

  /**
   * Number of bits.
   */
  private final int numberOfBits;
  
  private JBBPBitNumber(final int numberOfBits){
    this.numberOfBits = numberOfBits;
  }
  
  /**
   * Get the numeric value of the bit number.
   * @return the number of bits as integer
   */
  public int getBitNumber(){
    return this.numberOfBits;
  }
  
  /**
   * Decode a numeric value to a constant.
   * @param numberOfBits the numeric value to be decoded
   * @return decoded constant
   * @throws IllegalArgumentException if the value less than 1 or greater than 8
   */
  public static JBBPBitNumber decode(final int numberOfBits){
    if (numberOfBits <= 0 || numberOfBits>8) 
      throw new IllegalArgumentException("Unsupported bit number, allowed 1..8");
    return values()[numberOfBits-1];
  }
}
