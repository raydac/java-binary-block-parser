/*
 * Copyright 2018 Igor Maznitsa.
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
 * Describes an array of strings.
 *
 * @since 1.4.0
 */
public final class JBBPFieldArrayString extends JBBPAbstractArrayField<JBBPFieldString> {
  private static final long serialVersionUID = -220078798710257313L;
  /**
   * Inside value storage.
   */
  private final String[] array;

  /**
   * The Constructor.
   *
   * @param name  a field name info, it can be null.
   * @param array a value array, it must not be null bt can contain null.
   */
  public JBBPFieldArrayString(final JBBPNamedFieldInfo name, final String[] array) {
    super(name);
    JBBPUtils.assertNotNull(array, "Array must not be null");
    this.array = array;
  }

  /**
   * Get the values as a short array.
   *
   * @return the values as a short array.
   */
  public String[] getArray() {
    return this.array.clone();
  }

  @Override
  public int size() {
    return this.array.length;
  }

  @Override
  public JBBPFieldString getElementAt(final int index) {
    final JBBPFieldString result = new JBBPFieldString(this.fieldNameInfo, this.array[index]);
    result.payload = this.payload;
    return result;
  }

  @Override
  public Object getValueArrayAsObject(final boolean reverseBits) {
    final String[] result = this.array.clone();
    if (reverseBits) {
      for (int i = 0; i < result.length; i++) {
        result[i] = JBBPFieldString.reverseBits(result[i]);
      }
    }
    return result;
  }

  @Override
  public String getTypeAsString() {
    return JBBPFieldString.TYPE_NAME + " [" + this.array.length + ']';
  }
}
