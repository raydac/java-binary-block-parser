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
package com.igormaznitsa.jbbp;

import com.igormaznitsa.jbbp.compiler.JBBPCompiledBlock;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.compiler.varlen.JBBPIntegerValueEvaluator;
import com.igormaznitsa.jbbp.exceptions.*;
import com.igormaznitsa.jbbp.model.*;
import com.igormaznitsa.jbbp.model.finder.JBBPFieldFinder;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.util.*;

/**
 * Implements a storage to keep named fields. it is not a thread-safe one
 */
public final class JBBPNamedNumericFieldMap implements JBBPFieldFinder {

  /**
   * Inside map to get numeric fields for their name field descriptors.
   */
  private final Map<JBBPNamedFieldInfo, JBBPNumericField> fieldMap;
  /**
   * Defined external value provider, it can be null.
   */
  private final JBBPExternalValueProvider externalValueProvider;

  /**
   * Empty constructor which makes a map with null provider.
   */
  public JBBPNamedNumericFieldMap() {
    this(null);
  }

  /**
   * A Constructor.
   *
   * @param externalValueProvider an external value provider, it can be null
   */
  public JBBPNamedNumericFieldMap(final JBBPExternalValueProvider externalValueProvider) {
    this.fieldMap = new LinkedHashMap<JBBPNamedFieldInfo, JBBPNumericField>();
    this.externalValueProvider = externalValueProvider;
  }

  /**
   * Get the external value provider.
   *
   * @return the external value provider or null if it is undefined
   */
  public JBBPExternalValueProvider getExternalValueProvider() {
    return this.externalValueProvider;
  }

  /**
   * Get a numeric field for its field name info.
   *
   * @param namedField a field name info, it must not be null
   * @return the found field or null if it is not found
   */
  public JBBPNumericField get(final JBBPNamedFieldInfo namedField) {
    return this.fieldMap.get(namedField);
  }

  /**
   * Put a numeric field into map.
   *
   * @param field a field to be added into map or replace already exists one, it
   * must not be null
   * @throws NullPointerException if the field is null or if it is an anonymous
   * field
   */
  public void putField(final JBBPNumericField field) {
    JBBPUtils.assertNotNull(field, "Field must not be null");
    final JBBPNamedFieldInfo fieldName = field.getNameInfo();
    JBBPUtils.assertNotNull(fieldName, "Field name info must not be null");
    this.fieldMap.put(fieldName, field);
  }

  /**
   * Remove a field for its field name info descriptor.
   *
   * @param nameInfo the field name info, it must not be null
   * @return removed numeric field or null if there was not any field for the info
   */
  public JBBPNumericField remove(final JBBPNamedFieldInfo nameInfo) {
    JBBPUtils.assertNotNull(nameInfo, "Name info must not be null");
    return this.fieldMap.remove(nameInfo);
  }

  /**
   * Find a registered field for its field offset in compiled script.
   *
   * @param offset the field offset
   * @return found field or null if there is not any found for the offset
   */
  public JBBPNumericField findForFieldOffset(final int offset) {
    JBBPNumericField result = null;
    for (final Map.Entry<JBBPNamedFieldInfo, JBBPNumericField> f : fieldMap.entrySet()) {
      if (f.getKey().getFieldOffsetInCompiledBlock() == offset) {
        result = f.getValue();
        break;
      }
    }
    return result;
  }

  public <T extends JBBPAbstractField> T findFirstFieldForType(final Class<T> fieldType) {
    JBBPUtils.assertNotNull(fieldType, "Type must not be null");
    T result = null;
    for (final JBBPNumericField f : fieldMap.values()) {
      if (fieldType.isAssignableFrom(f.getClass())) {
        result = fieldType.cast(f);
        break;
      }
    }
    return result;
  }

  public <T extends JBBPAbstractField> T findLastFieldForType(final Class<T> fieldType) {
    JBBPUtils.assertNotNull(fieldType, "Type must not be null");
    T result = null;
    for (final JBBPNumericField f : fieldMap.values()) {
      if (fieldType == f.getClass()) {
        result = fieldType.cast(f);
      }
    }
    return result;
  }

  public <T extends JBBPAbstractField> T findFieldForType(final Class<T> fieldType) {
    JBBPUtils.assertNotNull(fieldType, "Type must not be null");
    int count = 0;
    T result = null;
    for (final JBBPNumericField f : fieldMap.values()) {
      if (fieldType == f.getClass()) {
        result = fieldType.cast(f);
        count++;
      }
    }
    if (count > 1) {
      throw new JBBPTooManyFieldsFoundException(count, "Too many fields detected", null, fieldType);
    }
    return result;
  }

  public <T extends JBBPAbstractField> T findFieldForNameAndType(final String fieldName, final Class<T> fieldType) {
    final String normalizedName = JBBPUtils.normalizeFieldNameOrPath(fieldName);
    JBBPUtils.assertNotNull(fieldType, "Field type must not be null");

    T result = null;

    for (final Map.Entry<JBBPNamedFieldInfo, JBBPNumericField> f : fieldMap.entrySet()) {
      if (normalizedName.equals(f.getKey().getFieldName()) && fieldType.isAssignableFrom(f.getValue().getClass())) {
        result = fieldType.cast(f.getValue());
        break;
      }
    }
    return result;
  }

  public <T extends JBBPAbstractField> T findFieldForPathAndType(final String fieldPath, final Class<T> fieldType) {
    final String normalizedPath = JBBPUtils.normalizeFieldNameOrPath(fieldPath);
    JBBPUtils.assertNotNull(fieldType, "Field type must not be null");

    T result = null;

    for (final Map.Entry<JBBPNamedFieldInfo, JBBPNumericField> f : fieldMap.entrySet()) {
      if (normalizedPath.equals(f.getKey().getFieldPath()) && fieldType.isAssignableFrom(f.getValue().getClass())) {
        result = fieldType.cast(f.getValue());
        break;
      }
    }
    return result;
  }

  public JBBPAbstractField findFieldForName(final String fieldName) {
    final String normalizedName = JBBPUtils.normalizeFieldNameOrPath(fieldName);

    JBBPAbstractField result = null;

    for (final Map.Entry<JBBPNamedFieldInfo, JBBPNumericField> f : fieldMap.entrySet()) {
      if (normalizedName.equals(f.getKey().getFieldName())) {
        result = (JBBPAbstractField) f.getValue();
        break;
      }
    }
    return result;
  }

  public JBBPAbstractField findFieldForPath(final String fieldPath) {
    final String normalizedPath = JBBPUtils.normalizeFieldNameOrPath(fieldPath);

    JBBPAbstractField result = null;

    for (final Map.Entry<JBBPNamedFieldInfo, JBBPNumericField> f : fieldMap.entrySet()) {
      if (normalizedPath.equals(f.getKey().getFieldPath())) {
        result = (JBBPAbstractField) f.getValue();
        break;
      }
    }
    return result;
  }

  public boolean nameExists(final String fieldName) {
    final String normalizedName = JBBPUtils.normalizeFieldNameOrPath(fieldName);

    boolean result = false;

    for (final JBBPNamedFieldInfo f : fieldMap.keySet()) {
      if (normalizedName.equals(f.getFieldName())) {
        result = true;
        break;
      }
    }
    return result;
  }

  public boolean pathExists(final String fieldPath) {
    final String normalizedPath = JBBPUtils.normalizeFieldNameOrPath(fieldPath);

    boolean result = false;

    for (final JBBPNamedFieldInfo f : fieldMap.keySet()) {
      if (normalizedPath.equals(f.getFieldPath())) {
        result = true;
        break;
      }
    }
    return result;
  }

  /**
   * Clear the map.
   */
  public void clear() {
    this.fieldMap.clear();
  }

  /**
   * Check that the map is empty.
   *
   * @return true if the map is empty, false otherwise
   */
  public boolean isEmpty() {
    return this.fieldMap.isEmpty();
  }

  /**
   * Get number of registered fields in the map.
   *
   * @return number of registered fields as integer
   */
  public int size() {
    return this.fieldMap.size();
  }

  /**
   * Ask the registered external value provider for a field value.
   *
   * @param externalFieldName the name of a field, it must not be null
   * @param compiledBlock the compiled block, it must not be null
   * @param evaluator an evaluator which is calling the method, it can be null
   * @return integer value for the field
   * @throws JBBPException if there is not any external value provider
   */
  public int getExternalFieldValue(final String externalFieldName, final JBBPCompiledBlock compiledBlock, final JBBPIntegerValueEvaluator evaluator) {
    final String normalizedName = JBBPUtils.normalizeFieldNameOrPath(externalFieldName);
    if (this.externalValueProvider == null) {
      throw new JBBPEvalException("Request for '" + externalFieldName + "' but there is not any value provider", evaluator);
    }
    else {
      return this.externalValueProvider.provideArraySize(normalizedName, this, compiledBlock);
    }
  }
}
