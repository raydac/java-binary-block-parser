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
package com.igormaznitsa.jbbp.compiler;

import com.igormaznitsa.jbbp.compiler.varlen.JBBPIntegerValueEvaluator;
import com.igormaznitsa.jbbp.exceptions.JBBPException;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.util.*;

/**
 * The Class describes a data block contains compiled information for a bin
 * parser script.
 * @since 1.0
 */
public final class JBBPCompiledBlock {

  /**
   * The Array of named field info items.
   */
  private final JBBPNamedFieldInfo[] namedFieldData;
  /**
   * The Source which was used for compilation.
   */
  private final String source;
  /**
   * The Compiled script data.
   */
  private final byte[] compiledArray;
  /**
   * The Array of variable size array evaluators.
   */
  private final JBBPIntegerValueEvaluator[] arraySizeEvaluators;

  /**
   * The Flag shows that the compiled block contains var fields.
   */
  private final boolean hasVarFields;

  /**
   * Inside helper to build a compiled block
   */
  public final static class Builder {

    private String source;
    private final List<JBBPNamedFieldInfo> namedFields = new ArrayList<JBBPNamedFieldInfo>();
    private byte[] compiledData;
    private boolean hasVarFields;
    private final List<JBBPIntegerValueEvaluator> varLenProcessors = new ArrayList<JBBPIntegerValueEvaluator>();

    /**
     * Build a compiled block based on the data and check that all needed data
     * is provided.
     *
     * @return a compiled block
     * @throws NullPointerException if needed info is not provided
     */
    public JBBPCompiledBlock build() {
      JBBPUtils.assertNotNull(source, "Source is not defined");
      JBBPUtils.assertNotNull(compiledData, "Compiled data is not defined");

      return new JBBPCompiledBlock(this.source, this.namedFields.toArray(new JBBPNamedFieldInfo[this.namedFields.size()]), this.varLenProcessors.isEmpty() ? null : this.varLenProcessors.toArray(new JBBPIntegerValueEvaluator[this.varLenProcessors.size()]), this.compiledData, this.hasVarFields);
    }

    /**
     * Set the flag that the compiled block has var fields or array.
     *
     * @param flag the flag, true shows that the compiled block contains a var
     * file, false otherwise
     * @return this object
     */
    public Builder setHasVarFields(final boolean flag) {
      this.hasVarFields = flag;
      return this;
    }

    /**
     * Set the source.
     *
     * @param source the source for compiled block.
     * @return this object
     */
    public Builder setSource(final String source) {
      this.source = source;
      return this;
    }

    /**
     * Set the compiled data block.
     *
     * @param compiledData the compiled data block.
     * @return this object
     */
    public Builder setCompiledData(final byte[] compiledData) {
      this.compiledData = compiledData;
      return this;
    }

    /**
     * Set the named field info items.
     *
     * @param namedFields named field info item list
     * @return this object
     */
    public Builder setNamedFieldData(final List<JBBPNamedFieldInfo> namedFields) {
      this.namedFields.clear();
      if (namedFields != null) {
        this.namedFields.addAll(namedFields);
      }
      return this;
    }

    /**
     * Set the variable size array evaluators
     *
     * @param evaluators list of evaluators, it can be null
     * @return this object
     */
    public Builder setArraySizeEvaluators(final List<JBBPIntegerValueEvaluator> evaluators) {
      this.varLenProcessors.clear();
      if (evaluators != null) {
        this.varLenProcessors.addAll(evaluators);
      }
      return this;
    }
  }

  /**
   * The Method allows to create the new builder.
   *
   * @return the new builder
   */
  public static Builder prepare() {
    return new Builder();
  }

  /**
   * The Class
   *
   * @param source the source used for compilation, must not be null
   * @param namedFields named field info array
   * @param arraySizeEvaluators array size evaluator array
   * @param compiledData compiled data block
   * @param hasVarFields the flag shows that te block contains var fields
   */
  private JBBPCompiledBlock(final String source, final JBBPNamedFieldInfo[] namedFields, final JBBPIntegerValueEvaluator[] arraySizeEvaluators, final byte[] compiledData, final boolean hasVarFields) {
    this.source = source;
    this.namedFieldData = namedFields;
    this.hasVarFields = hasVarFields;
    this.compiledArray = compiledData;
    this.arraySizeEvaluators = arraySizeEvaluators;
  }

  /**
   * Get the source which was used for compilation
   *
   * @return the source as string
   */
  public String getSource() {
    return this.source;
  }

  /**
   * Check that the compiled block has var fields or arrays.
   *
   * @return true if the compiled block contains any VAR field or VAR array,
   * false otherwise
   */
  public boolean hasVarFields() {
    return this.hasVarFields;
  }

  /**
   * Check that the compiled block contains array fields with calculated size.
   *
   * @return true if calculated size arrays are presented, false otherwise
   */
  public boolean hasEvaluatedSizeArrays() {
    return this.arraySizeEvaluators != null;
  }

  /**
   * Get the compiled data block
   *
   * @return the compiled data block contains the byte code of the script
   */
  public byte[] getCompiledData() {
    return this.compiledArray;
  }

  /**
   * Get array contains the named field array
   *
   * @return the named field item array
   */
  public JBBPNamedFieldInfo[] getNamedFields() {
    return this.namedFieldData;
  }

  /**
   * Get the array size evaluators
   *
   * @return the array size evaluators, it can be null if there is not any
   * variable size array
   */
  public JBBPIntegerValueEvaluator[] getArraySizeEvaluators() {
    return this.arraySizeEvaluators;
  }

  /**
   * Find a field for its path.
   *
   * @param fieldPath a field path
   * @return a field to be found for the path, null otherwise
   */
  public JBBPNamedFieldInfo findFieldForPath(final String fieldPath) {
    
    JBBPNamedFieldInfo result = null;
    
    for (final JBBPNamedFieldInfo f : this.namedFieldData) {
      if (f.getFieldPath().equals(fieldPath)) {
        result = f;
        break;
      }
    }
    return result;
  }

  /**
   * Find offset of a field in the compiled block for its field path.
   *
   * @param fieldPath a field path, it must not be null
   * @return the offset as integer for the field path
   * @throws JBBPException if the field is not found
   */
  public int findFieldOffsetForPath(final String fieldPath) {
    for (final JBBPNamedFieldInfo f : this.namedFieldData) {
      if (f.getFieldPath().equals(fieldPath)) {
        return f.getFieldOffsetInCompiledBlock();
      }
    }
    throw new JBBPException("Unknown field path [" + fieldPath + ']');
  }
}
