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

public enum JBBPNumberOfBits {
  BITS_1(1),
  BITS_2(2),
  BITS_3(3),
  BITS_4(4),
  BITS_5(5),
  BITS_6(6),
  BITS_7(7);
  
  private final int numberOfBits;
  
  private JBBPNumberOfBits(final int numberOfBits){
    this.numberOfBits = numberOfBits;
  }
  
  public int getNumberOfBits(){
    return this.numberOfBits;
  }
  
  public static JBBPNumberOfBits decode(final int numberOfBits){
    for(final JBBPNumberOfBits b : values()){
      if (b.numberOfBits == numberOfBits) return b;
    }
    throw new IllegalArgumentException("Unsupported bit number, allowed 1..7");
  }
}
