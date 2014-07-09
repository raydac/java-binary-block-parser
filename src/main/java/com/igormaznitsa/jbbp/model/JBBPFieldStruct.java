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
package com.igormaznitsa.jbbp.model;

import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.exceptions.JBBPFinderException;
import com.igormaznitsa.jbbp.exceptions.JBBPTooManyFieldsFoundException;
import com.igormaznitsa.jbbp.model.finder.JBBPFieldFinder;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.util.List;

/**
 * Describes a structure.
 */
public final class JBBPFieldStruct extends JBBPAbstractField implements JBBPFieldFinder {

  /**
   * Structure fields.
   */
  private final JBBPAbstractField[] fields;

  /**
   * A Constructor.
   * @param name a field name info, it can be null
   * @param fields a field array, it must not be null
   */
  public JBBPFieldStruct(final JBBPNamedFieldInfo name, final JBBPAbstractField[] fields) {
    super(name);
    JBBPUtils.assertNotNull(fields, "Array of fields must not be null");
    this.fields = fields;
  }

  /**
   * A Constructor.
   * @param name a field name info, it can be null
   * @param fields a field list, it must not be null
   */
  public JBBPFieldStruct(final JBBPNamedFieldInfo name, final List<JBBPAbstractField> fields) {
    this(name, fields.toArray(new JBBPAbstractField[fields.size()]));
  }

  /**
   * Get the fields of the structure as an array.
   * @return the field array of the structure.
   */
  public JBBPAbstractField[] getArray() {
    return this.fields.clone();
  }

  public JBBPAbstractField findFieldForPath(final String fieldPath) {
    JBBPUtils.assertNotNull(fieldPath, "Path must not be null");
    final String[] parsedName = JBBPUtils.splitString(fieldPath, '.');

    JBBPAbstractField found = null;
    if (this.getNameInfo().getFieldName().equals(parsedName[0])) {
      found = this;
    }

    for (int i = 1; found != null && i < parsedName.length; i++) {
      if (found instanceof JBBPFieldStruct) {
        found = ((JBBPFieldStruct) found).findFieldForName(parsedName[i]);
      }
      else {
        throw new JBBPFinderException("Detected a field instead of a structure as one of nodes in the path '" + fieldPath + '\'', fieldPath, null);
      }
    }

    return found;
  }

  public JBBPAbstractField findFieldForName(final String name) {
    for (final JBBPAbstractField f : this.fields) {
      if (name.equals(f.getFieldName())) {
        return f;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public <T extends JBBPAbstractField> T findFieldForType(final Class<T> klazz) {
    T result = null;

    int counter = 0;

    for (final JBBPAbstractField f : this.fields) {
      if (f.getClass() == klazz) {
        if (result == null) {
          result = (T) f;
        }
        counter++;
      }
    }
    if (counter > 1) {
      throw new JBBPTooManyFieldsFoundException(counter, "Detected more than one field", null, klazz);
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  public <T extends JBBPAbstractField> T findFirstFieldForType(final Class<T> klazz) {
    for (final JBBPAbstractField f : this.fields) {
      if (f.getClass() == klazz) {
        return (T) f;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public <T extends JBBPAbstractField> T findLastFieldForType(final Class<T> klazz) {
    for (int i = this.fields.length - 1; i >= 0; i--) {
      final JBBPAbstractField f = this.fields[i];
      if (f.getClass() == klazz) {
        return (T) f;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public <T extends JBBPAbstractField> T findFieldForNameAndType(final String name, final Class<T> klazz) {
    for (final JBBPAbstractField f : this.fields) {
      if (f.getClass() == klazz && name.equals(f.getFieldName())) {
        return (T) f;
      }
    }
    return null;
  }

  public boolean nameExists(final String fieldName) {
    for (final JBBPAbstractField f : this.fields) {
      if (fieldName.equals(f.getFieldName())) {
        return true;
      }
    }
    return false;
  }

  public boolean pathExists(final String fieldPath) {
    for (final JBBPAbstractField f : this.fields) {
      if (fieldPath.equals(f.getFieldPath())) {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  public <T extends JBBPAbstractField> T findFieldForPathAndType(final String fieldPath, final Class<T> fieldType) {
    for (final JBBPAbstractField f : this.fields) {
      if (f.getClass() == fieldType && fieldPath.equals(f.getFieldPath())) {
        return (T) f;
      }
    }
    return null;
  }

}
