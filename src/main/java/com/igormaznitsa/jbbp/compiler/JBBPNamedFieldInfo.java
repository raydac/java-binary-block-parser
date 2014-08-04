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

import com.igormaznitsa.jbbp.utils.JBBPUtils;

/**
 * The Class describes a named field info item. Such objects are presented
 * inside of compiled blocks only for fields which have names.
 */
public final class JBBPNamedFieldInfo {

  /**
   * The Field path.
   */
  private final String fieldPath;
  /**
   * The Field name.
   */
  private final String fieldName;
  /**
   * The Field byte-code offset in the compiled block.
   */
  private final int offsetInCompiledBlock;

  /**
   * The Constructor
   *
   * @param fieldPath the field path
   * @param fieldName the field name
   * @param offsetInCompiledBlock the offset in the compiled block for the field
   */
  public JBBPNamedFieldInfo(final String fieldPath, final String fieldName, final int offsetInCompiledBlock) {
    this.fieldPath = JBBPUtils.normalizeFieldNameOrPath(fieldPath);
    this.fieldName = JBBPUtils.normalizeFieldNameOrPath(fieldName);
    this.offsetInCompiledBlock = offsetInCompiledBlock;
  }

  /**
   * Get the field path.
   *
   * @return the field path as string
   */
  public String getFieldPath() {
    return this.fieldPath;
  }

  /**
   * Get the field name.
   *
   * @return the field name as string
   */
  public String getFieldName() {
    return this.fieldName;
  }

  /**
   * Get the field offset in the compiled byte-code block.
   *
   * @return the field offset in the byte-code block
   */
  public int getFieldOffsetInCompiledBlock() {
    return this.offsetInCompiledBlock;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == null) {
      return false;
    }
    if (this == obj) {
      return true;
    }

    boolean result = false;

    if (obj instanceof JBBPNamedFieldInfo) {
      final JBBPNamedFieldInfo that = (JBBPNamedFieldInfo) obj;
      result = this.fieldPath.equals(that.fieldPath) && this.offsetInCompiledBlock == that.offsetInCompiledBlock;
    }
    return result;
  }

  @Override
  public int hashCode() {
    return this.offsetInCompiledBlock;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "[fieldPath=" + this.fieldPath + ", fieldName=" + this.fieldName + ", offetInCompiledBlock=" + this.offsetInCompiledBlock + ']';
  }

}
