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
 * Describes a unsigned short array.
 */
public final class JBBPFieldArrayUShort extends JBBPAbstractArrayField<JBBPFieldUShort>{
  private static final long serialVersionUID = -220078798710257343L;
  /**
   * Inside value storage.
   */
  private final short[] array;

  /**
   * The Constructor.
   * @param name a field name info, it can be null.
   * @param array a value array, it must not be null
   */
  public JBBPFieldArrayUShort(final JBBPNamedFieldInfo name, final short[] array) {
    super(name);
    JBBPUtils.assertNotNull(array, "Array must not be null");
    this.array = array;
  }

  /**
   * Get the values as a short array.
   * @return the values as a short array.
   */
  public short [] getArray() {
    return this.array.clone();
  }

  @Override
  public int size() {
    return this.array.length;
  }

  @Override
  public JBBPFieldUShort getElementAt(final int index) {
    return new JBBPFieldUShort(this.fieldNameInfo, this.array[index]);
  }

  @Override
  public int getAsInt(final int index) {
    return this.array[index] & 0xFFFF;
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
    final short[] result;
    if (reverseBits) {
      result = this.array.clone();
      for (int i = 0; i < result.length; i++) {
        result[i] = (short) JBBPFieldUShort.reverseBits(result[i]);
      }
    }
    else {
      result = this.array.clone();
    }
    return result;
  }
}
