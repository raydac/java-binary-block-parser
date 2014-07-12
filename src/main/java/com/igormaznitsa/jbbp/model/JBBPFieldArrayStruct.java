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
 * Describes a structure array. It doesn't support operations to get an array value as a numeric one.
 */
public final class JBBPFieldArrayStruct extends JBBPAbstractArrayField<JBBPFieldStruct> {
  /**
   * Inside value storage.
   */
  private final JBBPFieldStruct [] structs;
  
  /**
   * The Constructor.
   * @param name a field name info, it can be null
   * @param array a value array, it must not be null
   */
  public JBBPFieldArrayStruct(final JBBPNamedFieldInfo name, final JBBPFieldStruct [] array) {
    super(name);
    JBBPUtils.assertNotNull(array, "Array must not be null");
    this.structs = array;
  }

  /**
   * Get the value array.
   * @return the value array as a structure array
   */
  public JBBPFieldStruct [] getArray(){
    return this.structs.clone();
  }
  
  @Override
  public int size() {
    return this.structs.length;
  }

  @Override
  public JBBPFieldStruct getElementAt(final int index) {
    return this.structs[index];
  }

  @Override
  public int getAsInt(final int index) {
    throw new UnsupportedOperationException("Structure can't be mapped to integer");
  }

  @Override
  public long getAsLong(final int index) {
    throw new UnsupportedOperationException("Structure can't be mapped to long");
  }

  @Override
  public boolean getAsBool(final int index) {
    throw new UnsupportedOperationException("Structure can't be mapped to boolean");
  }

  
}
