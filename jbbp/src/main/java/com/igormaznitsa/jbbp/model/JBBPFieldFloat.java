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

/**
 * Describes a float field.
 *
 * @since 1.3.1
 */
public final class JBBPFieldFloat extends JBBPAbstractField implements JBBPNumericField {
  public static final String TYPE_NAME = "floatj";
  private static final long serialVersionUID = 5493764792942829316L;
  /**
   * Inside value storage.
   */
  private final float value;

  /**
   * The Constructor.
   *
   * @param name  a field name info, it can be null.
   * @param value the field value
   */
  public JBBPFieldFloat(final JBBPNamedFieldInfo name, final float value) {
    super(name);
    this.value = value;
  }

  @Override
  public float getAsFloat() {
    return this.value;
  }

  @Override
  public double getAsDouble() {
    return (double) this.value;
  }

  @Override
  public int getAsInt() {
    return Math.round(this.value);
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
    return JBBPFieldInt.reverseBits(Float.floatToIntBits(this.value));
  }

  @Override
  public String getTypeAsString() {
    return TYPE_NAME;
  }

}
