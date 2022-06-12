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
 * Describes a long array.
 *
 * @since 1.0
 */
public final class JBBPFieldArrayUInt extends JBBPAbstractArrayField<JBBPFieldUInt>
    implements JBBPNumericArray {
  private static final long serialVersionUID = -2146953450724853264L;
  /**
   * Inside value storage.
   */
  private final int[] array;

  /**
   * The Constructor.
   *
   * @param name  a field name info, it can be null
   * @param array a value array, it must not be null
   */
  public JBBPFieldArrayUInt(final JBBPNamedFieldInfo name, final int[] array) {
    super(name);
    JBBPUtils.assertNotNull(array, "Array must not be null");
    this.array = array;
  }

  /**
   * get the value array
   *
   * @return the value array as a long array
   */
  public long[] getArray() {
    long[] result = new long[this.array.length];
    for (int i = 0; i < this.array.length; i++) {
      result[i] = (long) this.array[i] & 0xFFFFFFFFL;
    }
    return result;
  }

  /**
   * Get internal array of signed integers representing unsigned integers.
   *
   * @return the internal integer array, must not be null
   * @since 2.0.4
   */
  public int[] getInternalArray() {
    return this.array;
  }

  @Override
  public int size() {
    return this.array.length;
  }

  @Override
  public JBBPFieldUInt getElementAt(final int index) {
    final JBBPFieldUInt result =
        new JBBPFieldUInt(this.fieldNameInfo, (long) this.array[index] & 0xFFFFFFFFL);
    result.payload = this.payload;
    return result;
  }

  @Override
  public Object getValueArrayAsObject(final boolean reverseBits) {
    final long[] result = new long[this.array.length];
    for (int i = 0; i < result.length; i++) {
      result[i] =
          (reverseBits ? JBBPFieldInt.reverseBits(this.array[i]) : this.array[i]) & 0xFFFFFFFFL;
    }
    return result;
  }

  @Override
  public int getAsInt(final int index) {
    return this.array[index];
  }

  @Override
  public long getAsLong(final int index) {
    return (long) this.array[index] & 0xFFFFFFFFL;
  }

  @Override
  public boolean getAsBool(final int index) {
    return this.array[index] != 0L;
  }

  @Override
  public String getTypeAsString() {
    return "uint " + '[' + this.array.length + ']';
  }
}
