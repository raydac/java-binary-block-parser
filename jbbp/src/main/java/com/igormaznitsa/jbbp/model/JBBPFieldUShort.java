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

import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.utils.JBBPUtils;

/**
 * Describes a unsigned short value field.
 *
 * @since 1.0
 */
public final class JBBPFieldUShort extends JBBPAbstractField implements JBBPNumericField {
  private static final long serialVersionUID = 8028734964961601006L;
  /**
   * Inside value storage.
   */
  private final short value;

  /**
   * The Constructor.
   *
   * @param name  a field name info, it can be null
   * @param value the field value
   */
  public JBBPFieldUShort(final JBBPNamedFieldInfo name, final short value) {
    super(name);
    this.value = value;
  }

  /**
   * Get the reversed bit representation of the value.
   *
   * @param value the value to be reversed
   * @return the reversed value
   */
  public static long reverseBits(final short value) {
    final int b0 = JBBPUtils.reverseBitsInByte((byte) value) & 0xFF;
    final int b1 = JBBPUtils.reverseBitsInByte((byte) (value >> 8)) & 0xFF;

    return (long) ((b0 << 8) | b1) & 0xFFFFL;
  }

  @Override
  public double getAsDouble() {
    return (double) (this.value & 0xFFFF);
  }

  @Override
  public float getAsFloat() {
    return (float) (this.value & 0xFFFF);
  }

  @Override
  public int getAsInt() {
    return this.value & 0xFFFF;
  }

  @Override
  public long getAsLong() {
    return this.getAsInt();
  }

  @Override
  public boolean getAsBool() {
    return this.value != 0;
  }

  @Override
  public long getAsInvertedBitOrder() {
    return reverseBits(this.value);
  }

  @Override
  public String getTypeAsString() {
    return "ushort";
  }
}
