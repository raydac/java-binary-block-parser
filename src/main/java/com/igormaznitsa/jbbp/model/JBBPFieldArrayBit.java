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
import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.utils.JBBPUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes an array contains bit fields.
 * @since 1.0
 */
public final class JBBPFieldArrayBit extends JBBPAbstractArrayField<JBBPFieldBit> implements BitEntity {
  private static final long serialVersionUID = -4589044511663149591L;

  /**
   * Number of value bits in values of the array.
   */
  private final JBBPBitNumber bitNumber;

  /**
   * Bit values.
   */
  private final byte[] array;

  /**
   * The Constructor.
   *
   * @param name the field name info, it can be null.
   * @param array the byte array contains values, it must not be null
   * @param bitNumber number of valuable bits in values of the array, it must
   * not be null
   */
  public JBBPFieldArrayBit(final JBBPNamedFieldInfo name, final byte[] array, final JBBPBitNumber bitNumber) {
    super(name);
    JBBPUtils.assertNotNull(array, "Array must not be null");
    JBBPUtils.assertNotNull(bitNumber, "Bit number must not be null");
    this.array = array;
    this.bitNumber = bitNumber;
  }

  /**
   * Get values as a byte array.
   *
   * @return the value array
   */
  public byte[] getArray() {
    return this.array.clone();
  }

  @Override
  protected String getKeyPrefix() {
    return "array_bit";
  }

  @Override
  protected Object getValue() {
    List<Object> valueList = new ArrayList<Object>();
    for (byte b : getArray()) {
      valueList.add((int)b);
    }
    return valueList;
  }

  /**
   * Get the valuable bit number of values in the array.
   * @return the valuable bit number, must not be null
   */
  public JBBPBitNumber getBitWidth() {
    return this.bitNumber;
  }
  
  @Override
  public int size() {
    return this.array.length;
  }

  @Override
  public JBBPFieldBit getElementAt(final int index) {
    return new JBBPFieldBit(this.fieldNameInfo, this.getAsInt(index), this.bitNumber);
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
    return this.array[index] != 0;
  }

  @Override
  public Object getValueArrayAsObject(final boolean reverseBits) {
    final byte [] result;
    if (reverseBits){
      result = this.array.clone();
      for(int i =0; i<result.length; i++){
        result[i] = (byte)JBBPFieldBit.reverseBits(result[i], this.bitNumber);
      }
    }else{
      result = this.array.clone();
    }
    return result;
  }
}
