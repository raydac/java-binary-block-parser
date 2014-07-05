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
package com.igormaznitsa.jbbp.compiler.parser;

import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.Serializable;

public class JBBPToken implements Serializable {
  private static final long serialVersionUID = 7864654729087070154L;

  private final JBBPTokenType type;
  private final String fieldName;
  private final String arraySize;
  private final JBBPTokenParameters fieldType;
  private final int position;

  public JBBPToken(final JBBPTokenType type, final int position, final JBBPTokenParameters fieldType, final String arrayLength, final String fieldName) {
    this.type = type;
    this.position = position;
    this.fieldType = fieldType;
    this.fieldName = fieldName;
    this.arraySize = arrayLength;
  }

  public int getPosition() {
    return this.position;
  }

  public JBBPTokenType getType() {
    return this.type;
  }

  public String getFieldName() {
    return this.fieldName;
  }

  public JBBPTokenParameters getFieldType() {
    return this.fieldType;
  }

  public boolean isArray() {
    return this.arraySize != null;
  }

  public boolean isComment() {
    return this.type == JBBPTokenType.COMMENT;
  }

  public boolean isVarArrayLength(){
    return !JBBPUtils.isNumber(this.arraySize);
  }
  
  public String getSizeAsString() {
    return this.arraySize;
  }

  public Integer getSizeAsInt() {
    if (this.arraySize == null) {
      throw new NumberFormatException("Array size is not defined");
    }
    try{
      return Integer.valueOf(this.arraySize.trim());
    }catch(NumberFormatException ex){
      return null;
    }
  }

  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.append(this.type.name()).append(' ');
    if (this.fieldType != null) {
      result.append(this.fieldType).append(' ');
    }
    if (this.arraySize != null) {
      result.append('[').append(this.arraySize).append("] ");
    }
    if (this.fieldName != null) {
      result.append(this.fieldName);
    }

    if (this.type != JBBPTokenType.COMMENT) {
      result.append(';');
    }

    return result.toString();
  }
}
