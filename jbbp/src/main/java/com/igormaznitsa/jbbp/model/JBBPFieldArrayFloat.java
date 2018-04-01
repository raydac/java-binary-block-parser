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
 * Describes an array of floats.
 *
 * @since 1.4.0
 */
public final class JBBPFieldArrayFloat extends JBBPAbstractArrayField<JBBPFieldFloat> {
  private static final long serialVersionUID = 6839868800303265190L;
  /**
   * Inside storage.
   */
  private final float[] array;

  /**
   * The Constructor.
   *
   * @param name  the field name info, it can be null.
   * @param array the value array, it must not be null.
   */
  public JBBPFieldArrayFloat(final JBBPNamedFieldInfo name, final float[] array) {
    super(name);
    JBBPUtils.assertNotNull(array, "Array must not be null");
    this.array = array;
  }

  /**
   * Get values as an integer array.
   *
   * @return values as an integer array
   */
  public float[] getArray() {
    return this.array.clone();
  }

  @Override
  public int size() {
    return this.array.length;
  }

  @Override
  public JBBPFieldFloat getElementAt(final int index) {
    final JBBPFieldFloat result = new JBBPFieldFloat(this.fieldNameInfo, this.array[index]);
    result.payload = this.payload;
    return result;
  }

  @Override
  public int getAsInt(final int index) {
    return Math.round(this.array[index]);
  }

  public float getAsFloat(final int index) {
    return this.array[index];
  }

  @Override
  public long getAsLong(final int index) {
    return this.getAsInt(index);
  }

  @Override
  public boolean getAsBool(final int index) {
    return this.array[index] != 0;
  }

  @Override
  public Object getValueArrayAsObject(final boolean reverseBits) {
    final float[] result;
    if (reverseBits) {
      result = this.array.clone();
      for (int i = 0; i < result.length; i++) {
        result[i] = Float.intBitsToFloat((int) JBBPFieldInt.reverseBits(Float.floatToIntBits(result[i])));
      }
    } else {
      result = this.array.clone();
    }
    return result;
  }

  @Override
  public String getTypeAsString() {
    return JBBPFieldFloat.TYPE_NAME + " [" + this.array.length + ']';
  }
}
