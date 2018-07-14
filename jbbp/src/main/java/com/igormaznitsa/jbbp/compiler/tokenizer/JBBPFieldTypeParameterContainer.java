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

package com.igormaznitsa.jbbp.compiler.tokenizer;

import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.model.JBBPFieldDouble;
import com.igormaznitsa.jbbp.model.JBBPFieldFloat;
import com.igormaznitsa.jbbp.model.JBBPFieldString;

import java.io.Serializable;

/**
 * The Class is a container to keep parsed field type parameters.
 *
 * @since 1.0
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

  /**
   * The Constructor
   *
   * @param byteOrder the byte order for the field, must not be null
   * @param typeName  the type of the field, can be null
   * @param extraData the extra data placed after ':' char, can be null
   */
  public JBBPFieldTypeParameterContainer(final JBBPByteOrder byteOrder, final String typeName, final String extraData) {
    this.byteOrder = byteOrder;
    this.typeName = typeName;
    this.extraData = extraData;
  }

  /**
   * Get the byte order for the field. It can be null.
   *
   * @return the defined byte order for the field.
   */
  public JBBPByteOrder getByteOrder() {
    return this.byteOrder;
  }

  /**
   * Get the type name of the field.
   *
   * @return the type name as String.
   */
  public String getTypeName() {
    return this.typeName;
  }

  /**
   * Get the extra data for the field.
   *
   * @return the extra data as string or null.
   */
  public String getExtraData() {
    return this.extraData;
  }

  /**
   * Extract expression for extra data.
   *
   * @return extracted expression from extra data, null if it is not extra data
   */
  public String getExtraDataExpression() {
    String result = null;
    if (hasExpressionAsExtraData()) {
      result = this.extraData.substring(1, this.extraData.length() - 1);
    }
    return result;
  }

  /**
   * Check that the extra data is expression.
   *
   * @return true if the extra data is expression, false otherwise
   */
  public boolean hasExpressionAsExtraData() {
    return this.extraData != null && this.extraData.startsWith("(") && this.extraData.endsWith(")");
  }

  /**
   * Check that the type is a special one ('floatj', 'doublej', 'stringj' or 'value').
   *
   * @return true if the type is a special one
   * @see JBBPFieldFloat#TYPE_NAME
   * @see JBBPFieldDouble#TYPE_NAME
   * @see JBBPFieldString#TYPE_NAME
   * @since 1.4.0
   */
  public boolean isSpecialField() {
    return this.typeName.equals(JBBPFieldFloat.TYPE_NAME) || this.typeName.equals(JBBPFieldDouble.TYPE_NAME) || this.typeName.equals(JBBPFieldString.TYPE_NAME) || this.typeName.equals("val");
  }

  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder();

    if (byteOrder == JBBPByteOrder.LITTLE_ENDIAN) {
      result.append('<');
    }
    result.append(this.typeName);
    if (extraData != null) {
      int insertIndex = typeName.indexOf(' ');
      if (insertIndex < 0) {
        insertIndex = result.length();
      } else {
        insertIndex++;
      }
      result.insert(insertIndex, ':' + extraData);

    }

    return result.toString();
  }
}
