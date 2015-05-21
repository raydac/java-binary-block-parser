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

import java.util.ArrayList;
import java.util.List;

/**
 * Describes an array of boolean values.
 * @since 1.0
 */
public final class JBBPFieldArrayBoolean extends JBBPAbstractArrayField<JBBPFieldBoolean>{
  private static final long serialVersionUID = -7896549257985728694L;
  /**
   * The Inside value storage.
   */
  private final boolean [] array;
  
  /**
   * The Constructor.
   * @param name the field name info, it can be null
   * @param array the value array, it must not be null
   */
  public JBBPFieldArrayBoolean(final JBBPNamedFieldInfo name, final boolean [] array) {
    super(name);
    JBBPUtils.assertNotNull(array, "Array must not be null");
    this.array = array;
  }
  /**
   * Get values of the array.
   * @return values as a boolean array
   */
  public boolean [] getArray(){
    return this.array.clone();
  }

  @Override
  protected String getKeyPrefix() {
    return "array_boolean";
  }

  @Override
  protected Object getValue() {
    List<Object> valueList = new ArrayList<Object>();
    for (boolean bool : getArray()) {
      valueList.add(bool);
    }
    return valueList;
  }

  @Override
  public int size() {
    return this.array.length;
  }

  @Override
  public JBBPFieldBoolean getElementAt(final int index) {
    return new JBBPFieldBoolean(this.fieldNameInfo, getAsBool(index));
  }

  @Override
  public int getAsInt(final int index) {
    return this.array[index] ? 1 : 0;
  }

  @Override
  public long getAsLong(final int index) {
    return this.getAsInt(index);
  }

  @Override
  public boolean getAsBool(final int index) {
    return this.array[index];
  }

  @Override
  public Object getValueArrayAsObject(final boolean reverseBits) {
    return this.array.clone ();
  }
}
