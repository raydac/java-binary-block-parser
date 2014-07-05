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

public final class JBBPFieldBit extends JBBPAbstractField implements JBBPNumericField {

  private final int value;
  
  public JBBPFieldBit(final JBBPNamedFieldInfo name,final int value) {
    super(name);
    this.value = value;
  }
  
  public int getAsInt() {
    return this.value & 0xFF;
  }

  public long getAsLong() {
    return getAsInt();
  }

  public boolean getAsBool() {
    return this.value != 0;
  }
 
  
}
