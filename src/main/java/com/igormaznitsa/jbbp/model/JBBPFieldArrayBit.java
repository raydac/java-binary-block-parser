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
 * Describes an array contains bit fields.
 */
public final class JBBPFieldArrayBit extends JBBPAbstractArrayField<JBBPFieldBit> {

  /**
   * Bit values.
   */
  private final byte [] array;

  /**
   * The Constructor.
   * @param name the field name info, it can be null.
   * @param array the byte array contains values, it must not be null
   */
  public JBBPFieldArrayBit(final JBBPNamedFieldInfo name,final byte[] array) {
    super(name);
    JBBPUtils.assertNotNull(array, "Array must not be null");
    this.array = array;
  }
  
  /**
   * Get values as a byte array.
   * @return the value array
   */
  public byte [] getArray(){
    return this.array.clone();
  }
  
  @Override
  public int size() {
    return this.array.length;
  }

  @Override
  public JBBPFieldBit getElementAt(final int index) {
    return new JBBPFieldBit(this.fieldNameInfo, this.getAsInt(index));
  }

  @Override
  public int getAsInt(final int index) {
    return this.array[index] & 0xFF;
  }

  @Override
  public long getAsLong(final int index) {
    return this.getAsInt(index);
  }

  @Override
  public boolean getAsBool(final int index) {
    return this.array[index]!=0;
  }

  
}
