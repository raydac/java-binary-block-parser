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
import java.io.Serializable;

/**
 * The Class is the ancestor for all fields and arrays of fields.
 * @since 1.0
 */
public abstract class JBBPAbstractField implements Serializable {
  private static final long serialVersionUID = 8142829902016660630L;
  
  /**
   * The Field contains the field name info
   */
  protected final JBBPNamedFieldInfo fieldNameInfo;

  /**
   * The Constructor.
   * @param namedField the name field info for the field, it can be null. 
   */
  public JBBPAbstractField(final JBBPNamedFieldInfo namedField){
    this.fieldNameInfo = namedField;
  }
  
  /**
   * Get the field name info.
   * @return the field name info if it is presented, otherwise null
   */
  public JBBPNamedFieldInfo getNameInfo(){
    return this.fieldNameInfo;
  }
  
  /**
   * Get the field path.
   * @return the field path or null if the field doesn't contain any field name info
   */
  public String getFieldPath(){
    return this.fieldNameInfo == null ? null : this.fieldNameInfo.getFieldPath();
  }

  /**
   * Get the field name.
   *
   * @return the field name or null if the field doesn't contain any field name
   * info
   */
  public String getFieldName(){
    return this.fieldNameInfo == null ? null : this.fieldNameInfo.getFieldName();
  }

  /**
   * Get the field type in string representation.
   * 
   * @return the string representation of field type, like 'int', 'long', 'bool [123]'
   * @since 1.2.0
   */
  public abstract String getTypeAsString();
}
