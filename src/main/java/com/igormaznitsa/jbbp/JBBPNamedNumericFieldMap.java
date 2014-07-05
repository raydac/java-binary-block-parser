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
import com.igormaznitsa.jbbp.exceptions.JBBPException;
import com.igormaznitsa.jbbp.exceptions.JBBPTooManyFieldsFoundException;
import com.igormaznitsa.jbbp.model.*;
import com.igormaznitsa.jbbp.model.finder.JBBPFieldFinder;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.util.*;

public final class JBBPNamedNumericFieldMap implements JBBPFieldFinder {

  private final Map<JBBPNamedFieldInfo, JBBPNumericField> fieldMap;
  private final JBBPExternalValueProvider externalValueProvider;

  public JBBPNamedNumericFieldMap() {
    this(null);
  }

  public JBBPNamedNumericFieldMap(final JBBPExternalValueProvider externalValueProvider) {
    this.fieldMap = new LinkedHashMap<JBBPNamedFieldInfo, JBBPNumericField>();
    this.externalValueProvider = externalValueProvider;
  }

  public JBBPExternalValueProvider getExternalValueProvider() {
    return this.externalValueProvider;
  }

  public JBBPNumericField get(final JBBPNamedFieldInfo namedField) {
    return this.fieldMap.get(namedField);
  }

  public void putField(final JBBPAbstractField field) {
    JBBPUtils.assertNotNull(field, "Field must not be null");
    final JBBPNamedFieldInfo fieldName = field.getNameInfo();
    JBBPUtils.assertNotNull(fieldName, "Field name info must not be null");
    if (field instanceof JBBPNumericField) {
      this.fieldMap.put(fieldName, (JBBPNumericField) field);

    }
  }

  public JBBPNumericField remove(final JBBPNamedFieldInfo nameInfo) {
    JBBPUtils.assertNotNull(nameInfo, "Name info must not be null");
    return this.fieldMap.remove(nameInfo);
  }

  public JBBPNumericField findForFieldOffset(final int offset) {
    for (final Map.Entry<JBBPNamedFieldInfo, JBBPNumericField> f : fieldMap.entrySet()) {
      if (f.getKey().getFieldOffsetInCompiledBlock() == offset) {
        return f.getValue();
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public <T extends JBBPAbstractField> T findFirstFieldForType(final Class<T> fieldType) {
    JBBPUtils.assertNotNull(fieldType, "Type must not be null");
    for (final JBBPNumericField f : fieldMap.values()) {
      if (fieldType == f.getClass()) {
        return (T) f;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public <T extends JBBPAbstractField> T findLastFieldForType(final Class<T> fieldType) {
    JBBPUtils.assertNotNull(fieldType, "Type must not be null");
    T result = null;
    for (final JBBPNumericField f : fieldMap.values()) {
      if (fieldType == f.getClass()) {
        result = (T) f;
      }
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  public <T extends JBBPAbstractField> T findFieldForType(final Class<T> fieldType) {
    JBBPUtils.assertNotNull(fieldType, "Type must not be null");
    int count = 0;
    T result = null;
    for (final JBBPNumericField f : fieldMap.values()) {
      if (fieldType == f.getClass()) {
        result = (T) f;
        count++;
      }
    }
    if (count > 1) {
      throw new JBBPTooManyFieldsFoundException(count, "Too many fields detected", null, fieldType);
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  public <T extends JBBPAbstractField> T findFieldForNameAndType(final String fieldName, final Class<T> fieldType) {
    JBBPUtils.assertNotNull(fieldName, "Name must not be null");
    JBBPUtils.assertNotNull(fieldType, "Field type must not be null");
    for (final Map.Entry<JBBPNamedFieldInfo, JBBPNumericField> f : fieldMap.entrySet()) {
      if (fieldName.equals(f.getKey().getFieldName()) && fieldType == f.getValue().getClass()) {
        return (T) f.getValue();
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public <T extends JBBPAbstractField> T findFieldForPathAndType(final String fieldPath, final Class<T> fieldType) {
    JBBPUtils.assertNotNull(fieldPath, "Path must not be null");
    JBBPUtils.assertNotNull(fieldType, "Field type must not be null");
    for (final Map.Entry<JBBPNamedFieldInfo, JBBPNumericField> f : fieldMap.entrySet()) {
      if (fieldPath.equals(f.getKey().getFieldPath()) && fieldType == f.getValue().getClass()) {
        return (T) f.getValue();
      }
    }
    return null;
  }

  public JBBPAbstractField findFieldForName(final String fieldName) {
    for (final Map.Entry<JBBPNamedFieldInfo, JBBPNumericField> f : fieldMap.entrySet()) {
      if (fieldName.equals(f.getKey().getFieldName())) {
        return (JBBPAbstractField) f.getValue();
      }
    }
    return null;
  }

  public JBBPAbstractField findFieldForPath(final String fieldPath) {
    for (final Map.Entry<JBBPNamedFieldInfo, JBBPNumericField> f : fieldMap.entrySet()) {
      if (fieldPath.equals(f.getKey().getFieldPath())) {
        return (JBBPAbstractField) f.getValue();
      }
    }
    return null;
  }

  public boolean nameExists(final String fieldName) {
    for (final JBBPNamedFieldInfo f : fieldMap.keySet()) {
      if (fieldName.equals(f.getFieldName())) {
        return true;
      }
    }
    return false;
  }

  public boolean pathExists(final String fieldPath) {
    for (final JBBPNamedFieldInfo f : fieldMap.keySet()) {
      if (fieldPath.equals(f.getFieldPath())) {
        return true;
      }
    }
    return false;
  }

  public void clear() {
    this.fieldMap.clear();
  }

  public boolean isEmpty() {
    return this.fieldMap.isEmpty();
  }

  public int size() {
    return this.fieldMap.size();
  }

  public int getExternalFieldValue(final String externalFieldName, final JBBPCompiledBlock compiledBlock) {
    if (this.externalValueProvider == null) {
      throw new JBBPException("Request for '" + externalFieldName + "' but there is not any value provider");
    }
    else {
      return this.externalValueProvider.provideArraySize(externalFieldName, this, compiledBlock);
    }
  }
}
