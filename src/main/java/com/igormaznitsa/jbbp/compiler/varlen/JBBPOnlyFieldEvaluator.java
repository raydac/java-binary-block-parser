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
package com.igormaznitsa.jbbp.compiler.varlen;

import com.igormaznitsa.jbbp.JBBPNamedNumericFieldMap;
import com.igormaznitsa.jbbp.compiler.JBBPCompiledBlock;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;

/**
 * Class implements an evaluator which works with only field.
 */
public final class JBBPOnlyFieldEvaluator implements JBBPLengthEvaluator {

  /**
   * The Index in named field area for the field which is used by the evaluator.
   */
  private final int namedFieldIndex;
  /**
   * An External field name which value will be requested by the evaluator. It
   * can be null.
   */
  private final String externalFieldName;

  /**
   * The Constructor.
   *
   * @param externalFieldName the external field name, it can be null.
   * @param namedFieldIndex the index of a named field in named field area.
   */
  public JBBPOnlyFieldEvaluator(final String externalFieldName, final int namedFieldIndex) {
    this.externalFieldName = externalFieldName;
    this.namedFieldIndex = namedFieldIndex;
  }

  public int eval(final JBBPBitInputStream inStream, final int currentCompiledBlockOffset, final JBBPCompiledBlock block, final JBBPNamedNumericFieldMap fieldMap) {
    final int result = externalFieldName == null
            ? fieldMap.get(block.getNamedFields()[this.namedFieldIndex]).getAsInt()
            : this.externalFieldName.equals("$")
            ? (int) inStream.getCounter()
            : fieldMap.getExternalFieldValue(this.externalFieldName, block);
    return result;
  }

  @Override
  public String toString() {
    return this.externalFieldName == null ? "NamedFieldIndex=" + this.namedFieldIndex : this.externalFieldName;
  }

}
