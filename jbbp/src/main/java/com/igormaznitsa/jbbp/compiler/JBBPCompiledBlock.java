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

package com.igormaznitsa.jbbp.compiler;

import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPFieldTypeParameterContainer;
import com.igormaznitsa.jbbp.compiler.varlen.JBBPIntegerValueEvaluator;
import com.igormaznitsa.jbbp.exceptions.JBBPException;
import com.igormaznitsa.jbbp.exceptions.JBBPIllegalArgumentException;
import com.igormaznitsa.jbbp.utils.JBBPUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class describes a data block contains compiled information for a bin
 * parser script.
 *
 * @since 1.0
 */
public final class JBBPCompiledBlock {

  private static final JBBPNamedFieldInfo[] ARRAY_FIELDINFO_EMPTY = new JBBPNamedFieldInfo[0];
  private static final JBBPIntegerValueEvaluator[] ARRAY_INTEVALUATOR_EMPTY = new JBBPIntegerValueEvaluator[0];
  private static final JBBPFieldTypeParameterContainer[] ARRAY_FIELDTYPE_EMPTY = new JBBPFieldTypeParameterContainer[0];

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
   * The Array of variable size array evaluators.
   */
  private final JBBPFieldTypeParameterContainer[] customTypeFields;

  /**
   * The Flag shows that the compiled block contains var fields.
   */
  private final boolean hasVarFields;

  /**
   * The Class
   *
   * @param source              the source used for compilation, must not be null
   * @param namedFields         named field info array
   * @param arraySizeEvaluators array size evaluator array
   * @param compiledData        compiled data block
   * @param hasVarFields        the flag shows that te block contains var fields
   */
  private JBBPCompiledBlock(final String source, final JBBPNamedFieldInfo[] namedFields, final JBBPIntegerValueEvaluator[] arraySizeEvaluators, final byte[] compiledData, final boolean hasVarFields, final JBBPFieldTypeParameterContainer[] customTypeFields) {
    this.source = source;
    this.namedFieldData = namedFields;
    this.hasVarFields = hasVarFields;
    this.compiledArray = compiledData;
    this.arraySizeEvaluators = arraySizeEvaluators;
    this.customTypeFields = customTypeFields;
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
   * Get array contains parameter info of detected custom type fields.
   *
   * @return the array contains the info for every registered custom type field.
   */
  public JBBPFieldTypeParameterContainer[] getCustomTypeFields() {
    return this.customTypeFields;
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
    throw new JBBPIllegalArgumentException("Unknown field path [" + fieldPath + ']');
  }

  /**
   * Inside helper to build a compiled block
   */
  public final static class Builder {

    private final List<JBBPNamedFieldInfo> namedFields = new ArrayList<>();
    private final List<JBBPIntegerValueEvaluator> varLenProcessors = new ArrayList<>();
    private final List<JBBPFieldTypeParameterContainer> customTypeFields = new ArrayList<>();
    private String source;
    private byte[] compiledData;
    private boolean hasVarFields;

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

      return new JBBPCompiledBlock(this.source, this.namedFields.toArray(ARRAY_FIELDINFO_EMPTY), this.varLenProcessors.isEmpty() ? null : this.varLenProcessors.toArray(ARRAY_INTEVALUATOR_EMPTY), this.compiledData, this.hasVarFields, this.customTypeFields.toArray(ARRAY_FIELDTYPE_EMPTY));
    }

    /**
     * Set the flag that the compiled block has var fields or array.
     *
     * @param flag the flag, true shows that the compiled block contains a var
     *             file, false otherwise
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

    /**
     * Set list of fields processed by custom type field processor.
     *
     * @param list list of field token info, it can be null
     * @return this object
     */
    public Builder setCustomTypeFields(final List<JBBPFieldTypeParameterContainer> list) {
      this.customTypeFields.clear();
      if (list != null) {
        this.customTypeFields.addAll(list);
      }
      return this;
    }
  }
}
