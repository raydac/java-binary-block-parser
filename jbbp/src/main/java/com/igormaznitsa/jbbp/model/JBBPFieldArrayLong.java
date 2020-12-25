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
public final class JBBPFieldArrayLong extends JBBPAbstractArrayField<JBBPFieldLong>
    implements JBBPNumericArray {
  private static final long serialVersionUID = -2146959300724853264L;
  /**
   * Inside value storage.
   */
  private final long[] array;

  /**
   * The Constructor.
   *
   * @param name  a field name info, it can be null
   * @param array a value array, it must not be null
   */
  public JBBPFieldArrayLong(final JBBPNamedFieldInfo name, final long[] array) {
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
    return this.array.clone();
  }

  @Override
  public int size() {
    return this.array.length;
  }

  @Override
  public JBBPFieldLong getElementAt(final int index) {
    final JBBPFieldLong result = new JBBPFieldLong(this.fieldNameInfo, this.array[index]);
    result.payload = this.payload;
    return result;
  }

  @Override
  public int getAsInt(final int index) {
    return (int) this.array[index];
  }

  @Override
  public long getAsLong(final int index) {
    return this.array[index];
  }

  @Override
  public boolean getAsBool(final int index) {
    return this.array[index] != 0L;
  }

  @Override
  public Object getValueArrayAsObject(final boolean reverseBits) {
    final long[] result;
    if (reverseBits) {
      result = this.array.clone();
      for (int i = 0; i < result.length; i++) {
        result[i] = JBBPFieldLong.reverseBits(result[i]);
      }
    } else {
      result = this.array.clone();
    }
    return result;
  }

  @Override
  public String getTypeAsString() {
    return "long " + '[' + this.array.length + ']';
  }
}
