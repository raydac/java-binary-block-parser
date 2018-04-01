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
 * Describes a double array.
 *
 * @since 1.4.0
 */
public final class JBBPFieldArrayDouble extends JBBPAbstractArrayField<JBBPFieldDouble> implements JBBPNumericArray {
  private static final long serialVersionUID = -2146959311724853264L;
  /**
   * Inside value storage.
   */
  private final double[] array;

  /**
   * The Constructor.
   *
   * @param name  a field name info, it can be null
   * @param array a value array, it must not be null
   */
  public JBBPFieldArrayDouble(final JBBPNamedFieldInfo name, final double[] array) {
    super(name);
    JBBPUtils.assertNotNull(array, "Array must not be null");
    this.array = array;
  }

  /**
   * get the value array
   *
   * @return the value array as a long array
   */
  public double[] getArray() {
    return this.array.clone();
  }

  @Override
  public int size() {
    return this.array.length;
  }

  @Override
  public JBBPFieldDouble getElementAt(final int index) {
    final JBBPFieldDouble result = new JBBPFieldDouble(this.fieldNameInfo, this.array[index]);
    result.payload = this.payload;
    return result;
  }

  @Override
  public int getAsInt(final int index) {
    return (int) Math.round(this.array[index]);
  }

  @Override
  public long getAsLong(final int index) {
    return Math.round(this.array[index]);
  }

  public double getAsDouble(final int index) {
    return this.array[index];
  }

  @Override
  public boolean getAsBool(final int index) {
    return Double.compare(this.array[index], 0.0d) != 0;
  }

  @Override
  public Object getValueArrayAsObject(final boolean reverseBits) {
    final double[] result;
    if (reverseBits) {
      result = this.array.clone();
      for (int i = 0; i < result.length; i++) {
        result[i] = Double.longBitsToDouble(JBBPFieldLong.reverseBits(Double.doubleToLongBits(result[i])));
      }
    } else {
      result = this.array.clone();
    }
    return result;
  }

  @Override
  public String getTypeAsString() {
    return JBBPFieldDouble.TYPE_NAME + " [" + this.array.length + ']';
  }
}
