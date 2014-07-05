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

public final class JBBPFieldArrayLong extends JBBPAbstractArrayField<JBBPFieldLong>{
  private final long [] array;

  public JBBPFieldArrayLong(final JBBPNamedFieldInfo name,final long[] array) {
    super(name);
    this.array = array;
  }
  
  public long [] getArray(){
    return this.array.clone();
  }

  @Override
  public int size() {
    return this.array.length;
  }

  @Override
  public JBBPFieldLong getElementAt(final int index) {
    return new JBBPFieldLong(this.namedField, this.array[index]);
  }

  @Override
  public int getAsInt(final int index) {
    return (int)this.array[index];
  }

  @Override
  public long getAsLong(final int index) {
    return this.array[index];
  }

  @Override
  public boolean getAsBool(final int index) {
    return this.array[index]!=0L;
  }
  
  
}
