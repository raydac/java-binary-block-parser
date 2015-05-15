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

/**
 * Describes a unsigned byte array.
 * @since 1.0
 */
public final class JBBPFieldArrayUByte extends AbstractFieldByteArray<JBBPFieldUByte>{
  private static final long serialVersionUID = -2568935326782182401L;
  
  /**
   * The Constructor.
   * @param name a field name info, it can be null.
   * @param array a value array, it must not be null
   */
  public JBBPFieldArrayUByte(final JBBPNamedFieldInfo name, final byte[] array) {
    super(name,array);
  }

  /**
   * Get the value array as a byte array.
   * @return the value array as a byte array
   */
  public byte [] getArray(){
    return this.array.clone();
  }
  
  @Override
  public int size() {
    return this.array.length;
  }

  @Override
  public JBBPFieldUByte getElementAt(final int index) {
    return new JBBPFieldUByte(this.fieldNameInfo, this.array[index]);
  }

  @Override
  public int getAsInt(final int index) {
    return this.array[index] & 0xFF;
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
