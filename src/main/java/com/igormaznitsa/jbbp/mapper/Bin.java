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

import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation describes a field in a class which can be mapped and loaded
 * from parsed a JBBP structure. Also it can be used for whole class but in the
 * case be careful and use default name and path values.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
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
   *
   * @return LSB0 or MSB0 order, LSB0 by default
   * @see JBBPBitOrder
   */
  JBBPBitOrder bitOrder() default JBBPBitOrder.LSB0;

  /**
   * The Flag shows that the field must be processed by a defined externally
   * custom field processor.
   *
   * @return true if the mapping field must be processed externally, false
   * otherwise
   * @see JBBPMapperCustomFieldProcessor
   */
  boolean custom() default false;

  /**
   * The Filed contains some extra text info which can be used by a custom field
   * processor, the engine doesn't use the field.
   *
   * @return the extra field as String
   * @see JBBPMapperCustomFieldProcessor
   */
  String extra() default "";
}
