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
import com.igormaznitsa.jbbp.exceptions.JBBPNumericFieldValueConversionException;
import java.util.Locale;

/**
 * Describes a unsigned integer (32 bit) field.
 *
 * @since 2.0.4
 */
public final strictfp class JBBPFieldUInt extends JBBPAbstractField implements JBBPNumericField {
  public static final String TYPE_NAME = "uint";
  private static final long serialVersionUID = 354342324375674L;
  /**
   * Internal field keeps the value.
   */
  private final int value;

  /**
   * The Constructor.
   *
   * @param name  a field name info, it can be null.
   * @param value the field value
   */
  public JBBPFieldUInt(final JBBPNamedFieldInfo name, final long value) {
    super(name);
    this.value = (int) value;
  }

  @Override
  public int getAsInt() {
    if (this.value >= 0) {
      return this.value;
    } else {
      throw new JBBPNumericFieldValueConversionException(this, "UINT 0x" +
          (Long.toHexString(this.value & 0xFFFFFFFFL).toUpperCase(
              Locale.ENGLISH)) + " can't be represented as INT");
    }
  }

  @Override
  public double getAsDouble() {
    return (double) this.getAsLong();
  }

  @Override
  public float getAsFloat() {
    return (float) this.getAsLong();
  }

  @Override
  public long getAsLong() {
    return ((long) this.value & 0xFFFFFFFFL);
  }

  @Override
  public boolean getAsBool() {
    return this.value != 0;
  }

  @Override
  public long getAsInvertedBitOrder() {
    return reverseBits(this.value & 0xFFFFFFFFL);
  }

  public static long reverseBits(final long value) {
    return JBBPFieldInt.reverseBits((int) value) & 0xFFFFFFFFL;
  }

  @Override
  public String getTypeAsString() {
    return TYPE_NAME;
  }

}
