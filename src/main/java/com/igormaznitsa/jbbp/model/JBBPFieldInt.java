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
import com.igormaznitsa.jbbp.utils.JBBPUtils;

/**
 * Describes an integer field.
 */
public final class JBBPFieldInt extends JBBPAbstractField implements JBBPNumericField{
  private static final long serialVersionUID = 5493764792042809716L;
  /**
   * Inside value storage.
   */
  private final int value;

  /**
   * The Constructor.
   * @param name a field name info, it can be null.
   * @param value the field value
   */
  public JBBPFieldInt(final JBBPNamedFieldInfo name, final int value) {
    super(name);
    this.value = value;
  }
  
  public int getAsInt() {
    return this.value;
  }

  public long getAsLong() {
    return this.getAsInt();
  }

  public boolean getAsBool() {
    return this.value != 0;
  }

  /**
   * Get the reversed bit representation of the value.
   *
   * @param value the value to be reversed
   * @return the reversed value
   */
  public static long reverseBits(final int value) {
    final int b0 = JBBPUtils.reverseBitsInByte((byte) value) & 0xFF;
    final int b1 = JBBPUtils.reverseBitsInByte((byte) (value >> 8)) & 0xFF;
    final int b2 = JBBPUtils.reverseBitsInByte((byte) (value >> 16)) & 0xFF;
    final int b3 = JBBPUtils.reverseBitsInByte((byte) (value >> 24)) & 0xFF;

    return (long) ((b0 << 24) | (b1 << 16) | (b2 << 8) | b3);
  }
  
  public long getAsInvertedBitOrder() {
    return reverseBits(this.value);
  }
}
