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
import java.util.Map;

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
   * Get key in order to store in a {@link Map}.  If the field is named, the name is
   * returned as the key, otherwise a key is generated based on the field type and
   * the index within the enclosing array of fields.
   *
   * @param index the index of the field within the enclosing array of fields
   * @return the key
   */
  protected String getKey(int index) {

    if (getNameInfo() != null) {
      return getNameInfo().getFieldName();
    } else {
      return getKeyPrefix() + "_" + index;
    }
  }

  /**
   * Returns the key prefix used for generated keys.
   *
   * @return the key prefix
   */
  protected abstract String getKeyPrefix();

  /**
   * Returns an object representation of the field and its children.  The intent
   * of this method is to produce a hierarchy consisting only of Java primitive
   * Objects (e.g., Boolean, Long, Integer, String) and collection (e.g., Map,
   * List) classes that can be converted to other representations, such as JSON
   * or XML.  The contract for the return Object is as follows:
   *
   * <ul>
   *   <li>{@link java.lang.Boolean} - for <code>bool</code> fields</li>
   *   <li>extending from {@link java.lang.Number} - for numeric fields (i.e.,
   *   <code>bit, byte, ubyte, short, ushort, int, long</code>, or other numeric
   *   custom fields)</li>
   *   <li>{@link java.lang.String} - for custom fields that read strings</li>
   *   <li>{@link java.util.List} - for arrays of primitive types</li>
   *   <li>{@link java.util.Map} - for structs that contain other structs and/or
   *   fields</li>
   * </ul>
   *
   * New custom field types must adhere to this contract.
   *
   * @return the value object
   */
  protected abstract Object getValue();

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
}
