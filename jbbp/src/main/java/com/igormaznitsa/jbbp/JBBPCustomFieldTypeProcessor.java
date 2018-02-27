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

package com.igormaznitsa.jbbp;

import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPFieldTypeParameterContainer;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.model.JBBPAbstractField;

import java.io.IOException;

/**
 * Allows to define and process own custom field types during parsing.
 *
 * @since 1.2.0
 */
public interface JBBPCustomFieldTypeProcessor {
  /**
   * Get custom types of fields supported by the custom type field processor.
   *
   * @return array of strings where every string is custom field type in lower-case.
   */
  String[] getCustomFieldTypes();

  /**
   * Called by compiler to check parameters for custom field.
   *
   * @param fieldType field type info, it must not be null
   * @param fieldName name of the field, it can be null for anonymous fields
   * @param extraData number placed as extra value for field, followed by ':' if not presented then zero, if it is expression then -1
   * @param isArray   flag shows that the field describes an array
   * @return true if such configuration allowed, false otherwise
   */
  boolean isAllowed(JBBPFieldTypeParameterContainer fieldType, String fieldName, int extraData, boolean isArray);

  /**
   * Read custom field from stream and return the data as a JBBP field.
   *
   * @param in                  the data source stream, must not be null
   * @param bitOrder            the bit order defined for parsing, must not be null
   * @param parserFlags         the flags defined for parsing
   * @param customTypeFieldInfo the current field type info, must not be null
   * @param fieldName           the field name info, it can be null if the field is anonymous one
   * @param extraData           extra numeric value for the field, followed by ':', if not presented then 0
   * @param readWholeStream     if true then the field is array which should contain parse data for whole stream till the end
   * @param arrayLength         -1 if it is not array else length of the array to be read.
   * @return parsed data as JBBP field, must not be null
   * @throws IOException it can be thrown for transport errors
   */
  JBBPAbstractField readCustomFieldType(JBBPBitInputStream in, JBBPBitOrder bitOrder, int parserFlags, JBBPFieldTypeParameterContainer customTypeFieldInfo, JBBPNamedFieldInfo fieldName, int extraData, boolean readWholeStream, int arrayLength) throws IOException;
}
