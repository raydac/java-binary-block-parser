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

/**
 * The Class describes a token from parsed script.
 * 
 */
public final class JBBPToken implements Serializable {
  private static final long serialVersionUID = 7864654729087070154L;

  /**
   * The Token type. It must not be null.
   */
  private final JBBPTokenType type;
  /**
   * The Field name value. It can be null.
   */
  private final String fieldName;
  
  /**
   * The Array size string value. It can be null if there is not any defined array size.
   */
  private final String arraySize;
  /**
   * Parsed field type part.
   */
  private final JBBPFieldTypeParameterContainer fieldTypeParameters;
  /**
   * The Token position in the script string, the first char is 0.
   */
  private final int position;

  /**
   * The Constructor
   * @param type the token type, it can't be null
   * @param position the token position in the script string
   * @param fieldTypeParameters the parsed field type, it can be null.
   * @param arrayLength the string value of array size, it can be null
   * @param fieldName the field name, it can be null
   */
  JBBPToken(final JBBPTokenType type, final int position, final JBBPFieldTypeParameterContainer fieldTypeParameters, final String arrayLength, final String fieldName) {
    JBBPUtils.assertNotNull(type, "Type must not be null");
    this.type = type;
    this.position = position;
    this.fieldTypeParameters = fieldTypeParameters;
    this.fieldName = fieldName;
    this.arraySize = arrayLength;
  }

  /**
   * Get the token position value in the string
   * @return the position value, the first char is 0. If the value is negative one then the position is undefined.
   */
  public int getPosition() {
    return this.position;
  }

  /**
   * Get the token type.
   * @return get the token type.
   * @see JBBPTokenType
   */
  public JBBPTokenType getType() {
    return this.type;
  }

  /**
   * Get the field name of the value represented by the token,
   * @return the field name as string or null if the field doesn't have defined field name.
   */
  public String getFieldName() {
    return this.fieldName;
  }

  /**
   * Get the field type parameters.
   * @return the field type parameters
   */
  public JBBPFieldTypeParameterContainer getFieldTypeParameters() {
    return this.fieldTypeParameters;
  }

  /**
   * Check that the token is array.
   * @return true if the token represents an array, false otherwise
   */
  public boolean isArray() {
    return this.arraySize != null;
  }

  /**
   * Check that the toke is commentaries.
   * @return true if the token represents commentaries, false otherwise
   */
  public boolean isComment() {
    return this.type == JBBPTokenType.COMMENT;
  }

  /**
   * Check that the array is not fixed size one.
   * @return true if the field represents a non-fixed size array
   */
  public boolean isVarArrayLength(){
    return isArray() && !JBBPUtils.isNumber(this.arraySize);
  }
  
  /**
   * Get the array size value in its raw form.
   * @return the array size value as string, if the field is not an array then null.
   */
  public String getArraySizeAsString() {
    return this.arraySize;
  }

  /**
   * Get numeric representation of the array size.
   * @return the parsed numeric representation of the array size value or null if it can't be parsed
   * @exception NullPointerException will be thrown if the array size value is null
   */
  public Integer getArraySizeAsInt() {
    if (this.arraySize == null) {
      throw new NullPointerException("Array size is not defined");
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
    if (this.fieldTypeParameters != null) {
      result.append(this.fieldTypeParameters).append(' ');
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
