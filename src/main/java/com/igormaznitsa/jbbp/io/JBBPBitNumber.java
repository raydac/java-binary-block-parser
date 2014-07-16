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
  BITS_1(1,0x01),
  BITS_2(2,0x03),
  BITS_3(3,0x07),
  BITS_4(4,0x0F),
  BITS_5(5,0x1F),
  BITS_6(6,0x3F),
  BITS_7(7,0x7F),
  BITS_8(8,0xFF);

  /**
   * Number of bits.
   */
  private final int numberOfBits;
  /**
   * Mask for the number of bits.
   */
  private final int mask;
  
  private JBBPBitNumber(final int numberOfBits, final int mask){
    this.numberOfBits = numberOfBits;
    this.mask = mask;
  }

  /**
   * Get the mask for the number of bits.
   * @return the mask for the number of bits
   */
  public int getMask(){
    return this.mask;
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
