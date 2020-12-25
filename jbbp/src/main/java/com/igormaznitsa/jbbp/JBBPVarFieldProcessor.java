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
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.model.JBBPAbstractArrayField;
import com.igormaznitsa.jbbp.model.JBBPAbstractField;
import java.io.IOException;

/**
 * The Interface describes a class which can process VAR fields from a stream.
 *
 * @since 1.0
 */
public interface JBBPVarFieldProcessor {
  /**
   * Read a field array from a stream. The Method must read a field array from a stream and return the value with the provided field name info.
   *
   * @param inStream        the data source bit stream, it must not be null
   * @param arraySize       the array size, if it is negative one then whole stream must be read
   * @param fieldName       the field name info for the VAR field, it can be null for anonymous fields
   * @param extraValue      the extra value for the field, by default it is 0, it is the integer value after ':' char in the field type
   * @param byteOrder       the byte order for the field, it must not be null
   * @param numericFieldMap the numeric field map for the session, it must not be null, it can be used for access to already read values of another numeric fields.
   * @return a field array without nulls as values, it must not return null
   * @throws IOException it can be thrown for transport errors or another process exceptions
   */
  JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(JBBPBitInputStream inStream,
                                                                   int arraySize,
                                                                   JBBPNamedFieldInfo fieldName,
                                                                   int extraValue,
                                                                   JBBPByteOrder byteOrder,
                                                                   JBBPNamedNumericFieldMap numericFieldMap)
      throws IOException;

  /**
   * Read a field from a stream. The Method must read a field from a stream and return it with the provided name field info.
   *
   * @param inStream        the data source bit stream, it must not be null
   * @param fieldName       the field name info for the VAR field, it can be null for
   *                        anonymous fields
   * @param extraValue      the extra value for the field, by default it is 0, it is
   *                        the integer value after ':' char in the field type
   * @param byteOrder       the byte order for the field, it must not be null
   * @param numericFieldMap the numeric field map for the session, it must not
   *                        be null, it can be used for access to already read values of another
   *                        numeric fields.
   * @return a read field object, it must not return null
   * @throws IOException it should be thrown for transport errors
   */
  JBBPAbstractField readVarField(JBBPBitInputStream inStream, JBBPNamedFieldInfo fieldName,
                                 int extraValue, JBBPByteOrder byteOrder,
                                 JBBPNamedNumericFieldMap numericFieldMap) throws IOException;
}
