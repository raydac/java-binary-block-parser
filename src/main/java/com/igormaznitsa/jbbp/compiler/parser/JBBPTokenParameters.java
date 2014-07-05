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

public class JBBPTokenParameters implements Serializable {

  private static final long serialVersionUID = 1557492283811982431L;
  private final JBBPByteOrder byteOrder;
  private final String name;
  private final String extraField;

  public JBBPTokenParameters(final JBBPByteOrder byteOrder, final String name, final String extraField) {
    this.byteOrder = byteOrder;
    this.name = name;
    this.extraField = extraField;
  }

  public JBBPByteOrder getByteOrder() {
    return this.byteOrder;
  }

  public String getName() {
    return this.name;
  }

  public String getExtraField() {
    return this.extraField;
  }

  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder();

    if (byteOrder != null) {
      result.append(byteOrder == JBBPByteOrder.BIG_ENDIAN ? '>' : '<');
    }
    result.append(this.name);
    if (extraField != null) {
      int insertIndex = name.indexOf(' ');
      if (insertIndex < 0) {
        insertIndex = result.length();
      }
      else {
        insertIndex++;
      }
      result.insert(insertIndex, ':' + extraField);

    }

    return result.toString();
  }
}
