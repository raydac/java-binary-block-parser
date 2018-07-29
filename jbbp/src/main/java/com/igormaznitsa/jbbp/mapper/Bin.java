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
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( {ElementType.FIELD, ElementType.TYPE})
@Inherited
public @interface Bin {

  /**
   * Name of a structure element to be mapped to the field.
   *
   * @return string name, if it is empty then the name of a field will be used
   * as name
   */
  String name() default "";

  /**
   * Path inside structure to an element to be mapped to the field.
   *
   * @return string path, if it is empty then the path is not used
   */
  String path() default "";

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
   * The Field is used by custom field processor and as expression to calculate array length.
   *
   * @return the extra field as String
   * @see JBBPMapperCustomFieldProcessor
   */
  String extra() default "";

  /**
   * The Value defines how many bytes are actual ones in the field, works for numeric field and arrays and allows make mapping to bit fields.The Property
   * works only for save and logging.
   *
   * @return the number of lower bits, by default 8 bits
   * @see JBBPTextWriter#Bin(java.lang.Object...)
   * @see JBBPOut#Bin(java.lang.Object)
   * @see JBBPOut#Bin(java.lang.Object, com.igormaznitsa.jbbp.io.JBBPCustomFieldWriter)
   * @since 1.1
   */
  JBBPBitNumber outBitNumber() default JBBPBitNumber.BITS_8;

  /**
   * Byte order to be used for write of the value.
   *
   * @return order of bytes to be used for field value write
   * @since 1.4.0
   */
  JBBPByteOrder outByteOrder() default JBBPByteOrder.BIG_ENDIAN;

  /**
   * The Value defines the field order to sort fields of the class for save or logging.
   *
   * @return the outOrder of the field as number (the mapping will make ascending sorting)
   * @see JBBPTextWriter#Bin(java.lang.Object...)
   * @see JBBPOut#Bin(java.lang.Object)
   * @see JBBPOut#Bin(java.lang.Object, com.igormaznitsa.jbbp.io.JBBPCustomFieldWriter)
   * @since 1.1
   */
  int outOrder() default 0;

  /**
   * Just either description of the field or some remark.
   *
   * @return the comment as String
   * @see JBBPTextWriter#Bin(java.lang.Object...)
   * @since 1.1
   */
  String comment() default "";
}
