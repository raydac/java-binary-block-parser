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

import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import java.io.Serializable;

/**
 * The Class is a container to keep parsed field type parameters.
 */
public final class JBBPFieldTypeParameterContainer implements Serializable {

  private static final long serialVersionUID = 1557492283811982431L;
  
  /**
   * The Byte order for the field.
   */
  private final JBBPByteOrder byteOrder;
  /**
   * The field type.
   */
  private final String typeName;
  /**
   * Extra data for the field (for instance number of bits for a bit field).
   */
  private final String extraData;

  public JBBPFieldTypeParameterContainer(final JBBPByteOrder byteOrder, final String typeName, final String extraData) {
    this.byteOrder = byteOrder;
    this.typeName = typeName;
    this.extraData = extraData;
  }

  /**
   * Get the byte order for the field. It can be null.
   * @return the defined byte order for the field.
   */
  public JBBPByteOrder getByteOrder() {
    return this.byteOrder;
  }

  /**
   * Get the type name of the field.
   * @return the type name as String.
   */
  public String getTypeName() {
    return this.typeName;
  }

  /**
   * Get the extra data for the field.
   * @return the extra data as string or null.
   */
  public String getExtraData() {
    return this.extraData;
  }

  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder();

    if (byteOrder != null) {
      result.append(byteOrder == JBBPByteOrder.BIG_ENDIAN ? '>' : '<');
    }
    result.append(this.typeName);
    if (extraData != null) {
      int insertIndex = typeName.indexOf(' ');
      if (insertIndex < 0) {
        insertIndex = result.length();
      }
      else {
        insertIndex++;
      }
      result.insert(insertIndex, ':' + extraData);

    }

    return result.toString();
  }
}
