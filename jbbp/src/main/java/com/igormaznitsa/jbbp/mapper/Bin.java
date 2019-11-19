/*
 * Copyright 2017 Igor Maznitsa.
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

import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.io.JBBPCustomFieldWriter;
import com.igormaznitsa.jbbp.io.JBBPOut;
import com.igormaznitsa.jbbp.utils.JBBPTextWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation describes a field in a class which can be mapped and loaded
 * from parsed a JBBP structure. Also it can be used for whole class but in the
 * case be careful and use default name and path values. The Class is not thread safe.
 *
 * <b></>Since 2.0.0 was removed prefix 'out' for fields which contained it</b>.
 *
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( {ElementType.FIELD, ElementType.TYPE})
@Inherited
public @interface Bin {

  /**
   * Name of a structure element to be mapped to the field.
   *
   * @return string name, if it is empty then field name will be used
   * as name
   */
  String name() default "";

  /**
   * Structure path inside to be mapped to the field.
   *
   * @return string path, if it is empty then the path is not used
   */
  String path() default "";

  /**
   * Custom type. It plays role only if {@link #type()} is UNDEFINED
   *
   * @return type of the field, if empty then undefined
   * @since 2.0.0
   */
  String customType() default "";

  /**
   * Expression to represent array size of the field.
   *
   * @return array size of the field, if empty then not defined
   * @since 2.0.0
   */
  String arraySizeExpr() default "";

  /**
   * Type of mapped parsed structure element.
   *
   * @return the mapped parsed structure element type
   * @see BinType
   */
  BinType type() default BinType.UNDEFINED;

  /**
   * Order of bits for the field.
   * <b>NB! The Value has strong effect! A Numeric field content will be inverted during mapping or saving of the field if the value is MSB0 (Java uses standard LSB0)</b>
   *
   * @return LSB0 or MSB0 outOrder, LSB0 by default
   * @see JBBPBitOrder
   */
  JBBPBitOrder bitOrder() default JBBPBitOrder.LSB0;

  /**
   * The Flag shows that the field must be processed by a defined externally
   * custom field processors during loading and saving.
   *
   * @return true if the mapping field must be processed externally, false
   * otherwise
   * @see JBBPMapperCustomFieldProcessor
   * @see JBBPCustomFieldWriter
   */
  boolean custom() default false;

  /**
   * Expression as extra part of type. It means take part in <b>type[:extra]</b>
   * @return extra value, if empty then undefined
   * @since 2.0.0
   */
  String paramExpr() default "";

  /**
   * The Value defines how many bytes are actual ones in the field, works for numeric field and arrays and allows make mapping to bit fields.
   *
   * @return the number of lower bits, by default 8 bits
   * @see JBBPTextWriter#Bin(java.lang.Object...)
   * @see JBBPOut#Bin(java.lang.Object)
   * @see JBBPOut#Bin(java.lang.Object, com.igormaznitsa.jbbp.io.JBBPCustomFieldWriter)
   * @see JBBPBitNumber
   * @since 2.0.0
   */
  JBBPBitNumber bitNumber() default JBBPBitNumber.BITS_8;

  /**
   * Byte order to be used for operations.
   *
   * @return order of bytes to be used
   * @see JBBPByteOrder
   * @since 2.0.0
   */
  JBBPByteOrder byteOrder() default JBBPByteOrder.BIG_ENDIAN;

  /**
   * The Value defines the field order to show relative position in data stream. If -1then it is undefined.
   *
   * @return the outOrder of the field as number (the mapping will make ascending sorting)
   * @see JBBPTextWriter#Bin(java.lang.Object...)
   * @see JBBPOut#Bin(java.lang.Object)
   * @see JBBPOut#Bin(java.lang.Object, com.igormaznitsa.jbbp.io.JBBPCustomFieldWriter)
   * @since 2.0.0
   */
  int order() default -1;

  /**
   * Just either description of the field or some remark.
   *
   * @return the comment as String
   * @see JBBPTextWriter#Bin(java.lang.Object...)
   * @since 1.1
   */
  String comment() default "";
}
