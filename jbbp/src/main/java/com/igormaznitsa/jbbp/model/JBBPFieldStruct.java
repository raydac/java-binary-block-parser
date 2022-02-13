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

package com.igormaznitsa.jbbp.model;

import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.exceptions.JBBPFinderException;
import com.igormaznitsa.jbbp.exceptions.JBBPTooManyFieldsFoundException;
import com.igormaznitsa.jbbp.mapper.BinFieldFilter;
import com.igormaznitsa.jbbp.mapper.JBBPMapper;
import com.igormaznitsa.jbbp.mapper.JBBPMapperCustomFieldProcessor;
import com.igormaznitsa.jbbp.model.finder.JBBPFieldFinder;
import com.igormaznitsa.jbbp.utils.Function;
import com.igormaznitsa.jbbp.utils.JBBPUtils;

import java.util.List;

import static com.igormaznitsa.jbbp.utils.JBBPUtils.ARRAY_FIELD_EMPTY;

/**
 * Describes a structure.
 *
 * @since 1.0
 */
public final class JBBPFieldStruct extends JBBPAbstractField implements JBBPFieldFinder {

  private static final long serialVersionUID = -5862961302818335702L;

  /**
   * Structure fields.
   */
  private final JBBPAbstractField[] fields;

  /**
   * A Constructor.
   *
   * @param name   a field name info, it can be null
   * @param fields a field array, it must not be null
   */
  public JBBPFieldStruct(final JBBPNamedFieldInfo name, final JBBPAbstractField[] fields) {
    super(name);
    JBBPUtils.assertNotNull(fields, "Array of fields must not be null");
    this.fields = fields;
  }

  /**
   * A Constructor.
   *
   * @param name   a field name info, it can be null
   * @param fields a field list, it must not be null
   */
  public JBBPFieldStruct(final JBBPNamedFieldInfo name, final List<JBBPAbstractField> fields) {
    this(name, fields.toArray(ARRAY_FIELD_EMPTY));
  }

  /**
   * Get the fields of the structure as an array.
   *
   * @return the field array of the structure.
   */
  public JBBPAbstractField[] getArray() {
    return this.fields.clone();
  }

  @Override
  public JBBPAbstractField findFieldForPath(final String fieldPath) {
    final String[] parsedName =
            JBBPUtils.splitString(JBBPUtils.normalizeFieldNameOrPath(fieldPath), '.');

    JBBPAbstractField found = this;
    final int firstIndex;
    if ("".equals(this.getFieldName())) {
      firstIndex = 0;
    } else if (parsedName[0].equals(this.getNameInfo().getFieldName())) {
      firstIndex = 1;
      found = this;
    } else {
      firstIndex = 0;
      found = null;
    }

    for (int i = firstIndex; found != null && i < parsedName.length; i++) {
      if (found instanceof JBBPFieldStruct) {
        found = ((JBBPFieldStruct) found).findFieldForName(parsedName[i]);
      } else {
        throw new JBBPFinderException(
                "Detected a field instead of a structure as one of nodes in the path '" + fieldPath +
                        '\'', fieldPath, null);
      }
    }

    return found;
  }

  @Override
  public JBBPAbstractField findFieldForName(final String name) {
    final String normalizedName = JBBPUtils.normalizeFieldNameOrPath(name);

    JBBPAbstractField result = null;

    for (final JBBPAbstractField f : this.fields) {
      if (normalizedName.equals(f.getFieldName())) {
        result = f;
        break;
      }
    }
    return result;
  }

  @Override
  public <T extends JBBPAbstractField> T findFieldForType(final Class<T> fieldType) {
    T result = null;

    int counter = 0;

    for (final JBBPAbstractField f : this.fields) {
      if (fieldType.isAssignableFrom(f.getClass())) {
        if (result == null) {
          result = fieldType.cast(f);
        }
        counter++;
      }
    }
    if (counter > 1) {
      throw new JBBPTooManyFieldsFoundException(counter, "Detected more than one field", null,
              fieldType);
    }
    return result;
  }

  @Override
  public <T extends JBBPAbstractField> T findFirstFieldForType(final Class<T> fieldType) {
    T result = null;

    for (final JBBPAbstractField f : this.fields) {
      if (fieldType.isAssignableFrom(f.getClass())) {
        result = fieldType.cast(f);
        break;
      }
    }
    return result;
  }

  @Override
  public <T extends JBBPAbstractField> T findLastFieldForType(final Class<T> fieldType) {
    T result = null;

    for (int i = this.fields.length - 1; i >= 0; i--) {
      final JBBPAbstractField f = this.fields[i];
      if (fieldType.isAssignableFrom(f.getClass())) {
        result = fieldType.cast(f);
        break;
      }
    }
    return result;
  }

  @Override
  public <T extends JBBPAbstractField> T findFieldForNameAndType(final String fieldName,
                                                                 final Class<T> fieldType) {
    final String normalizedName = JBBPUtils.normalizeFieldNameOrPath(fieldName);

    T result = null;

    for (final JBBPAbstractField f : this.fields) {
      if (fieldType.isAssignableFrom(f.getClass()) && normalizedName.equals(f.getFieldName())) {
        result = fieldType.cast(f);
        break;
      }
    }
    return result;
  }

  @Override
  public boolean nameExists(final String fieldName) {
    final String normalizedName = JBBPUtils.normalizeFieldNameOrPath(fieldName);

    boolean result = false;

    for (final JBBPAbstractField f : this.fields) {
      if (normalizedName.equals(f.getFieldName())) {
        result = true;
        break;
      }
    }
    return result;
  }

  @Override
  public boolean pathExists(final String fieldPath) {
    final String normalizedPath = JBBPUtils.normalizeFieldNameOrPath(fieldPath);

    boolean result = false;

    for (final JBBPAbstractField f : this.fields) {
      if (normalizedPath.equals(f.getFieldPath())) {
        result = true;
        break;
      }
    }
    return result;
  }

  @Override
  public <T extends JBBPAbstractField> T findFieldForPathAndType(final String fieldPath,
                                                                 final Class<T> fieldType) {
    final JBBPAbstractField field = this.findFieldForPath(fieldPath);

    T result = null;

    if (field != null && fieldType.isAssignableFrom(field.getClass())) {
      result = fieldType.cast(field);
    }
    return result;
  }

  /**
   * Find a structure by its path and map the structure fields to a class
   * fields.
   *
   * @param <T>           a class type
   * @param path          the path to the structure to be mapped, must not be null
   * @param instance      object instance to be filled by values, must not be null
   * @param instantiators array of functions which can instantiate object of required class, must not be null
   * @return a mapped instance of the class, must not be null
   * @since 2.0.0
   */
  @SafeVarargs
  public final <T> T mapTo(final String path, final T instance,
                           final Function<Class<?>, Object>... instantiators) {
    return JBBPMapper.map(this, path, instance, instantiators);
  }

  /**
   * Find a structure by its path and map the structure fields to a class
   * fields.
   *
   * @param <T>           a class type
   * @param path          the path to the structure to be mapped, must not be null
   * @param instance      object instance to be filled by values, must not be null
   * @param flags         special flags to tune mapping process
   * @param instantiators array of functions which can instantiate object of required class, must not be null
   * @return a mapped instance of the class, must not be null
   * @see JBBPMapper#FLAG_IGNORE_MISSING_VALUES
   * @since 2.0.0
   */
  @SafeVarargs
  public final <T> T mapTo(final String path, final T instance, final int flags,
                           final Function<Class<?>, Object>... instantiators) {
    return JBBPMapper.map(this, path, instance, flags, instantiators);
  }

  /**
   * Find a structure by its path and map the structure fields to a class
   * fields.
   *
   * @param <T>                  a class type
   * @param path                 the path to the structure to be mapped, must not be null
   * @param instance             object instance to be filled by values, must not be null
   * @param customFieldProcessor a custom field processor to provide values for custom mapping fields, it can be null if there is not any custom field
   * @param instantiators        array of functions which can instantiate object of required class, must not be null
   * @return a mapped instance of the class, must not be null
   * @since 2.0.0
   */
  @SafeVarargs
  public final <T> T mapTo(final String path, final T instance,
                           final JBBPMapperCustomFieldProcessor customFieldProcessor,
                           final Function<Class<?>, Object>... instantiators) {
    return JBBPMapper.map(this, path, instance, customFieldProcessor, instantiators);
  }

  /**
   * Find a structure by its path and map the structure fields to a class
   * fields.
   *
   * @param <T>                  a class type
   * @param path                 the path to the structure to be mapped, must not be null
   * @param instance             object instance to be filled by values, must not be null
   * @param customFieldProcessor a custom field processor to provide values for custom mapping fields, it can be null if there is not any custom field
   * @param flags                special flags to tune mapping process
   * @param instantiators        array of functions which can instantiate object of required class, must not be null
   * @return a mapped instance of the class, must not be null
   * @see JBBPMapper#FLAG_IGNORE_MISSING_VALUES
   * @since 2.0.0
   */
  @SafeVarargs
  public final <T> T mapTo(final String path, final T instance,
                           final JBBPMapperCustomFieldProcessor customFieldProcessor,
                           final int flags, final Function<Class<?>, Object>... instantiators) {
    return JBBPMapper.map(this, path, instance, customFieldProcessor, flags, instantiators);
  }

  /**
   * Map the structure fields to object fields.
   *
   * @param <T>           expected result type
   * @param objectToMap   an object to map fields of the structure, must not be
   *                      null
   * @param instantiators array of functions which can instantiate object of required class, must not be null
   * @return the same object from the arguments but with filled fields by values
   * of the structure
   */
  @SafeVarargs
  public final <T> T mapTo(final T objectToMap, final Function<Class<?>, Object>... instantiators) {
    return this.mapTo(objectToMap, (JBBPMapperCustomFieldProcessor) null, instantiators);
  }

  /**
   * Map the structure fields to object fields.
   *
   * @param <T>            expected result type
   * @param objectToMap    an object to map fields of the structure, must not be
   *                       null
   * @param binFieldFilter filter allows to exclude some fields from process, can be null
   * @param instantiators  array of functions which can instantiate object of required class, must not be null
   * @return the same object from the arguments but with filled fields by values
   * of the structure
   * @since 2.0.4
   */
  @SafeVarargs
  public final <T> T mapTo(final T objectToMap, final BinFieldFilter binFieldFilter, final Function<Class<?>, Object>... instantiators) {
    return this.mapTo(objectToMap, null, binFieldFilter, instantiators);
  }

  /**
   * Map the structure fields to object fields.
   *
   * @param <T>           expected result type
   * @param instance      object instance to be filled by values, must not be null
   * @param flags         special flags to tune mapping process
   * @param instantiators array of functions which can instantiate object of required class, must not be null
   * @return the same object from the arguments but with filled fields by values
   * of the structure
   * @see JBBPMapper#FLAG_IGNORE_MISSING_VALUES
   * @since 2.0.0
   */
  @SafeVarargs
  public final <T> T mapTo(final T instance, final int flags,
                           final Function<Class<?>, Object>... instantiators) {
    return this.mapTo(instance, (JBBPMapperCustomFieldProcessor) null, flags, instantiators);
  }

  /**
   * Map the structure fields to object fields.
   *
   * @param <T>            expected result type
   * @param instance       object instance to be filled by values, must not be null
   * @param flags          special flags to tune mapping process
   * @param binFieldFilter filter to exclude some fields from process, can be null
   * @param instantiators  array of functions which can instantiate object of required class, must not be null
   * @return the same object from the arguments but with filled fields by values
   * of the structure
   * @see JBBPMapper#FLAG_IGNORE_MISSING_VALUES
   * @since 2.0.4
   */
  @SafeVarargs
  public final <T> T mapTo(final T instance, final int flags,
                           final BinFieldFilter binFieldFilter,
                           final Function<Class<?>, Object>... instantiators) {
    return this.mapTo(instance, null, flags, binFieldFilter, instantiators);
  }

  /**
   * Map the structure fields to object fields.
   *
   * @param <T>                  expected result type
   * @param instance             an object to map fields of the structure, must not be
   *                             null
   * @param customFieldProcessor a custom field processor to provide values for
   *                             custom mapping fields, it can be null if there is not any custom field
   * @param instantiators        array of functions which can instantiate object of required class, must not be null
   * @return the same object from the arguments but with filled fields by values
   * of the structure
   */
  @SafeVarargs
  public final <T> T mapTo(final T instance,
                           final JBBPMapperCustomFieldProcessor customFieldProcessor,
                           final Function<Class<?>, Object>... instantiators) {
    return JBBPMapper.map(this, instance, customFieldProcessor, instantiators);
  }

  /**
   * Map the structure fields to object fields.
   *
   * @param <T>                  expected result type
   * @param instance             an object to map fields of the structure, must not be
   *                             null
   * @param customFieldProcessor a custom field processor to provide values for
   *                             custom mapping fields, it can be null if there is not any custom field
   * @param binFieldFilter       filter to exclude some fields, can be null
   * @param instantiators        array of functions which can instantiate object of required class, must not be null
   * @return the same object from the arguments but with filled fields by values
   * of the structure
   * @since 2.0.4
   */
  @SafeVarargs
  public final <T> T mapTo(final T instance,
                           final JBBPMapperCustomFieldProcessor customFieldProcessor,
                           final BinFieldFilter binFieldFilter,
                           final Function<Class<?>, Object>... instantiators) {
    return JBBPMapper.map(this,
            instance,
            customFieldProcessor,
            0,
            binFieldFilter,
            instantiators);
  }

  /**
   * Map the structure fields to object fields.
   *
   * @param <T>                  expected result type
   * @param objectToMap          an object to map fields of the structure, must not be
   *                             null
   * @param customFieldProcessor a custom field processor to provide values for
   *                             custom mapping fields, it can be null if there is not any custom field
   * @param flags                special flags to tune mapping process
   * @param instantiators        array of functions which can instantiate object of required class, must not be null
   * @return the same object from the arguments but with filled fields by values
   * of the structure
   * @see JBBPMapper#FLAG_IGNORE_MISSING_VALUES
   * @since 1.1
   */
  @SafeVarargs
  public final <T> T mapTo(final T objectToMap,
                           final JBBPMapperCustomFieldProcessor customFieldProcessor,
                           final int flags, final Function<Class<?>, Object>... instantiators) {
    return JBBPMapper.map(this, objectToMap, customFieldProcessor, flags, instantiators);
  }

  /**
   * Map the structure fields to object fields.
   *
   * @param <T>                  expected result type
   * @param objectToMap          an object to map fields of the structure, must not be
   *                             null
   * @param customFieldProcessor a custom field processor to provide values for
   *                             custom mapping fields, it can be null if there is not any custom field
   * @param flags                special flags to tune mapping process
   * @param binFieldFilter       filter to exclude some fields, can be null
   * @param instantiators        array of functions which can instantiate object of required class, must not be null
   * @return the same object from the arguments but with filled fields by values
   * of the structure
   * @see JBBPMapper#FLAG_IGNORE_MISSING_VALUES
   * @since 2.0.4
   */
  @SafeVarargs
  public final <T> T mapTo(final T objectToMap,
                           final JBBPMapperCustomFieldProcessor customFieldProcessor,
                           final int flags, final BinFieldFilter binFieldFilter, final Function<Class<?>, Object>... instantiators) {
    return JBBPMapper.map(this, objectToMap, customFieldProcessor, flags, binFieldFilter, instantiators);
  }

  @Override
  public String getTypeAsString() {
    return "{}";
  }
}
