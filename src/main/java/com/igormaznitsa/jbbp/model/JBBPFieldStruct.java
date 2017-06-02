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
import com.igormaznitsa.jbbp.mapper.JBBPMapper;
import com.igormaznitsa.jbbp.mapper.JBBPMapperCustomFieldProcessor;
import com.igormaznitsa.jbbp.model.finder.JBBPFieldFinder;
import com.igormaznitsa.jbbp.utils.JBBPUtils;

import java.util.List;

/**
 * Describes a structure.
 *
 * @since 1.0
 */
public final class JBBPFieldStruct extends JBBPAbstractField implements JBBPFieldFinder {

    private static final long serialVersionUID = -5862961302858335702L;

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
        this(name, fields.toArray(new JBBPAbstractField[fields.size()]));
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
        final String[] parsedName = JBBPUtils.splitString(JBBPUtils.normalizeFieldNameOrPath(fieldPath), '.');

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
                throw new JBBPFinderException("Detected a field instead of a structure as one of nodes in the path '" + fieldPath + '\'', fieldPath, null);
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
            throw new JBBPTooManyFieldsFoundException(counter, "Detected more than one field", null, fieldType);
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
    public <T extends JBBPAbstractField> T findFieldForNameAndType(final String fieldName, final Class<T> fieldType) {
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
    @SuppressWarnings("unchecked")
    public <T extends JBBPAbstractField> T findFieldForPathAndType(final String fieldPath, final Class<T> fieldType) {
        final JBBPAbstractField field = this.findFieldForPath(fieldPath);

        T result = null;

        if (field != null && fieldType.isAssignableFrom(field.getClass())) {
            result = fieldType.cast(field);
        }
        return result;
    }

    /**
     * Map the structure fields to a class fields.
     *
     * @param <T>          a class type
     * @param mappingClass a mapping class to be mapped by the structure fields,
     *                     must not be null and must have the default constructor
     * @return a mapped instance of the class, must not be null
     */
    public <T> T mapTo(final Class<T> mappingClass) {
        return mapTo(mappingClass, null);
    }

    /**
     * Map the structure fields to a class fields.
     *
     * @param <T>          a class type
     * @param mappingClass a mapping class to be mapped by the structure fields,
     *                     must not be null and must have the default constructor
     * @param flags        special flags to tune mapping
     * @return a mapped instance of the class, must not be null
     */
    public <T> T mapTo(final Class<T> mappingClass, final int flags) {
        return mapTo(mappingClass, null, flags);
    }

    /**
     * Map the structure fields to a class fields.
     *
     * @param <T>                  a class type
     * @param mappingClass         a mapping class to be mapped by the structure fields,
     *                             must not be null and must have the default constructor
     * @param customFieldProcessor a custom field processor to provide values for
     *                             custom mapping fields, it can be null if there is not any custom field
     * @return a mapped instance of the class, must not be null
     */
    public <T> T mapTo(final Class<T> mappingClass, final JBBPMapperCustomFieldProcessor customFieldProcessor) {
        return JBBPMapper.map(this, mappingClass, customFieldProcessor);
    }

    /**
     * Map the structure fields to a class fields.
     *
     * @param <T>                  a class type
     * @param mappingClass         a mapping class to be mapped by the structure fields,
     *                             must not be null and must have the default constructor
     * @param customFieldProcessor a custom field processor to provide values for
     *                             custom mapping fields, it can be null if there is not any custom field
     * @param flags                special flags to tune mapping process
     * @return a mapped instance of the class, must not be null
     */
    public <T> T mapTo(final Class<T> mappingClass, final JBBPMapperCustomFieldProcessor customFieldProcessor, final int flags) {
        return JBBPMapper.map(this, mappingClass, customFieldProcessor, flags);
    }

    /**
     * Find a structure by its path and map the structure fields to a class
     * fields.
     *
     * @param <T>          a class type
     * @param path         the path to the structure to be mapped, must not be null
     * @param mappingClass a mapping class to be mapped by the structure fields,
     *                     must not be null and must have the default constructor
     * @return a mapped instance of the class, must not be null
     */
    public <T> T mapTo(final String path, final Class<T> mappingClass) {
        return JBBPMapper.map(this, path, mappingClass);
    }

    /**
     * Find a structure by its path and map the structure fields to a class
     * fields.
     *
     * @param <T>          a class type
     * @param path         the path to the structure to be mapped, must not be null
     * @param mappingClass a mapping class to be mapped by the structure fields,
     *                     must not be null and must have the default constructor
     * @param flags        special flags to tune mapping process
     * @return a mapped instance of the class, must not be null
     * @see JBBPMapper#FLAG_IGNORE_MISSING_VALUES
     * @since 1.1
     */
    public <T> T mapTo(final String path, final Class<T> mappingClass, final int flags) {
        return JBBPMapper.map(this, path, mappingClass, flags);
    }

    /**
     * Find a structure by its path and map the structure fields to a class
     * fields.
     *
     * @param <T>                  a class type
     * @param path                 the path to the structure to be mapped, must not be null
     * @param mappingClass         a mapping class to be mapped by the structure fields,
     *                             must not be null and must have the default constructor
     * @param customFieldProcessor a custom field processor to provide values for custom mapping fields, it can be null if there is not any custom field
     * @return a mapped instance of the class, must not be null
     */
    public <T> T mapTo(final String path, final Class<T> mappingClass, final JBBPMapperCustomFieldProcessor customFieldProcessor) {
        return JBBPMapper.map(this, path, mappingClass, customFieldProcessor);
    }

    /**
     * Find a structure by its path and map the structure fields to a class
     * fields.
     *
     * @param <T>                  a class type
     * @param path                 the path to the structure to be mapped, must not be null
     * @param mappingClass         a mapping class to be mapped by the structure fields,
     *                             must not be null and must have the default constructor
     * @param customFieldProcessor a custom field processor to provide values for custom mapping fields, it can be null if there is not any custom field
     * @param flags                special flags to tune mapping process
     * @return a mapped instance of the class, must not be null
     * @see JBBPMapper#FLAG_IGNORE_MISSING_VALUES
     * @since 1.1
     */
    public <T> T mapTo(final String path, final Class<T> mappingClass, final JBBPMapperCustomFieldProcessor customFieldProcessor, final int flags) {
        return JBBPMapper.map(this, path, mappingClass, customFieldProcessor, flags);
    }

    /**
     * Map the structure fields to object fields.
     *
     * @param objectToMap an object to map fields of the structure, must not be
     *                    null
     * @return the same object from the arguments but with filled fields by values
     * of the structure
     */
    public Object mapTo(final Object objectToMap) {
        return this.mapTo(objectToMap, null);
    }

    /**
     * Map the structure fields to object fields.
     *
     * @param objectToMap an object to map fields of the structure, must not be
     *                    null
     * @param flags       special flags to tune mapping process
     * @return the same object from the arguments but with filled fields by values
     * of the structure
     * @see JBBPMapper#FLAG_IGNORE_MISSING_VALUES
     * @since 1.1
     */
    public Object mapTo(final Object objectToMap, final int flags) {
        return this.mapTo(objectToMap, null, flags);
    }

    /**
     * Map the structure fields to object fields.
     *
     * @param objectToMap          an object to map fields of the structure, must not be
     *                             null
     * @param customFieldProcessor a custom field processor to provide values for
     *                             custom mapping fields, it can be null if there is not any custom field
     * @return the same object from the arguments but with filled fields by values
     * of the structure
     */
    public Object mapTo(final Object objectToMap, final JBBPMapperCustomFieldProcessor customFieldProcessor) {
        return JBBPMapper.map(this, objectToMap, customFieldProcessor);
    }

    /**
     * Map the structure fields to object fields.
     *
     * @param objectToMap          an object to map fields of the structure, must not be
     *                             null
     * @param customFieldProcessor a custom field processor to provide values for
     *                             custom mapping fields, it can be null if there is not any custom field
     * @param flags                special flags to tune mapping process
     * @return the same object from the arguments but with filled fields by values
     * of the structure
     * @see JBBPMapper#FLAG_IGNORE_MISSING_VALUES
     * @since 1.1
     */
    public Object mapTo(final Object objectToMap, final JBBPMapperCustomFieldProcessor customFieldProcessor, final int flags) {
        return JBBPMapper.map(this, objectToMap, customFieldProcessor, flags);
    }

    @Override
    public String getTypeAsString() {
        return "{}";
    }
}
