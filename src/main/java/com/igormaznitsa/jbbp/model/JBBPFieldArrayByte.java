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
 * Describes a byte array.
 */
public final class JBBPFieldArrayByte extends JBBPAbstractArrayField<JBBPFieldByte>{
  private static final long serialVersionUID = -8100947416351943918L;
  /**
   * Inside storage of values.
   */
  private final byte [] array;
  
  /**
   * The Constructor.
   * @param name the field name info, it can be null.
   * @param array the values array, it must not be null
   */
  public JBBPFieldArrayByte(final JBBPNamedFieldInfo name, final byte [] array) {
    super(name);
    JBBPUtils.assertNotNull(array, "Array must not be null");
    this.array = array;
  }
  
  /**
   * Get the values of the array.
   * @return the values as a byte array
   */
  public byte[] getArray(){
    return this.array.clone();
  }

  @Override
  public int size() {
    return this.array.length;
  }

  @Override
  public JBBPFieldByte getElementAt(final int index) {
    return new JBBPFieldByte(this.fieldNameInfo, this.array[index]);
  }

  @Override
  public int getAsInt(final int index) {
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
    final byte[] result;
    if (reverseBits) {
      result = this.array.clone();
      for (int i = 0; i < result.length; i++) {
        result[i] = (byte) JBBPFieldByte.reverseBits(result[i]);
      }
    }
    else {
      result = this.array.clone();
    }
    return result;
  }
}
