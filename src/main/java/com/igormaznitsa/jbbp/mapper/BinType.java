/*
 * Copyright 2014 Igor Maznitsa.
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
package com.igormaznitsa.jbbp.mapper;

import com.igormaznitsa.jbbp.model.*;

/**
 * The Enum contains all supported bin types for mapper.
 * @see BinMapper
 */
public enum BinType {
  /**
   * Undefined type, the mapper will be looking for compatible parsed field with the mapping field.
   */
  UNDEFINED(null),
  /**
   * A Mapping field will be mapped to a parsed bit field.
   */
  BIT(JBBPFieldBit.class),
  /**
   * A Mapping field will be mapped to a parsed boolean field.
   */
  BOOL(JBBPFieldBoolean.class),
  /**
   * A Mapping field will be mapped to a parsed byte field.
   */
  BYTE(JBBPFieldByte.class),
  /**
   * A Mapping field will be mapped to a parsed unsigned byte field.
   */
  UBYTE(JBBPFieldUByte.class),
  /**
   * A Mapping field will be mapped to a parsed short field.
   */
  SHORT(JBBPFieldShort.class),
  /**
   * A Mapping field will be mapped to a parsed unsigned short field.
   */
  USHORT(JBBPFieldUShort.class),
  /**
   * A Mapping field will be mapped to a parsed integer field.
   */  
  INT(JBBPFieldInt.class),
  /**
   * A Mapping field will be mapped to a parsed long field.
   */
  LONG(JBBPFieldLong.class),
  /**
   * A Mapping field will be mapped to a parsed bit array field.
   */
  BIT_ARRAY(JBBPFieldArrayBit.class),
  /**
   * A Mapping field will be mapped to a parsed boolean array field.
   */
  BOOL_ARRAY(JBBPFieldArrayBoolean.class),
  /**
   * A Mapping field will be mapped to a parsed byte array field.
   */  
  BYTE_ARRAY(JBBPFieldArrayByte.class),
  /**
   * A Mapping field will be mapped to a parsed unsigned byte array field.
   */
  UBYTE_ARRAY(JBBPFieldArrayUByte.class),
  /**
   * A Mapping field will be mapped to a parsed short array field.
   */
  SHORT_ARRAY(JBBPFieldArrayShort.class),
  /**
   * A Mapping field will be mapped to a parsed unsigned short array field.
   */
  USHORT_ARRAY(JBBPFieldArrayUShort.class),
  /**
   * A Mapping field will be mapped to a parsed integer array field.
   */
  INT_ARRAY(JBBPFieldArrayInt.class),
  /**
   * A Mapping field will be mapped to a parsed long array field.
   */
  LONG_ARRAY(JBBPFieldArrayLong.class),
  /**
   * A Mapping field will be mapped to a parsed structure field.
   */
  STRUCT(JBBPFieldStruct.class),
  /**
   * A Mapping field will be mapped to a parsed structure array field.
   */
  STRUCT_ARRAY(JBBPFieldArrayStruct.class);

  /**
   * The field class for the value.
   */
  private final Class<? extends JBBPAbstractField> fieldClass;

  /**
   * The Field class for the value.
   * @param fieldClass 
   */
  private BinType(final Class<? extends JBBPAbstractField> fieldClass) {
    this.fieldClass = fieldClass;
  }

  /**
   * Get The Field class.
   * @return the field class for the value, it can be null
   */
  public Class<? extends JBBPAbstractField> getFieldClass() {
    return this.fieldClass;
  }

  /**
   * Find compatible type for a class.
   * @param fieldClazz the field class to find compatible type, must not be null
   * @return found compatible field type or null if not found
   */
  public static BinType findCompatible(final Class<?> fieldClazz) {
    if (fieldClazz.isArray()) {
      final Class<?> type = fieldClazz.getComponentType();
      if (type.isPrimitive()){
        if (type == byte.class){
          return BYTE_ARRAY;
        }else if (type == char.class){
          return USHORT_ARRAY;
        }else if (type == boolean.class){
          return BOOL_ARRAY;
        }else if (type == short.class){
          return SHORT_ARRAY;
        }else if (type == int.class){
          return INT_ARRAY;
        }else if (type == long.class){
          return LONG_ARRAY;
        }else{
          return null;
        }
      }else{
        return STRUCT_ARRAY;
      }
    }
    else if (fieldClazz.isPrimitive()) {
      if (fieldClazz == byte.class) {
        return BYTE;
      }
      else if (fieldClazz == char.class) {
        return USHORT;
      }
      else if (fieldClazz == boolean.class) {
        return BOOL;
      }
      else if (fieldClazz == short.class) {
        return SHORT;
      }
      else if (fieldClazz == int.class) {
        return INT;
      }
      else if (fieldClazz == long.class) {
        return LONG;
      }
      else {
        return null;
      }
    }
    else {
      return STRUCT;
    }
  }
}
