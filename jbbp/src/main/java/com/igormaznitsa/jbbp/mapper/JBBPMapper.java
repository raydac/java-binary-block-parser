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

package com.igormaznitsa.jbbp.mapper;

import com.igormaznitsa.jbbp.exceptions.JBBPMapperException;
import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.model.BitEntity;
import com.igormaznitsa.jbbp.model.JBBPAbstractField;
import com.igormaznitsa.jbbp.model.JBBPFieldStruct;
import com.igormaznitsa.jbbp.utils.Function;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import com.igormaznitsa.jbbp.utils.ReflectUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Class processes mapping of a parsed binary data to class fields.
 *
 * @since 1.0
 */
public final class JBBPMapper {

  public static final String MAKE_CLASS_INSTANCE_METHOD_NAME = "newInstance";
  /**
   * Flag to not throw exception if structure doesn't have value for a field.
   *
   * @since 1.1
   */
  public static final int FLAG_IGNORE_MISSING_VALUES = 1;
  private static final Map<Class<?>, List<MappedFieldRecord>> CACHED_FIELDS = new ConcurrentHashMap<>();

  /**
   * Create a class instance, map binary data of a structure for its path to its
   * fields and return the instance.
   *
   * @param <T>           the mapping class type
   * @param root          a parsed structure to be used as the root, must not be null
   * @param structPath    the path of a structure inside of the root to be mapped
   *                      to the class, must not be null
   * @param instance      object to be filled by values, must not be null
   * @param instantiators functions to produce class instance by request, must not be null
   * @return the created and mapped instance of the mapping class
   * @throws JBBPMapperException for any error
   * @since 2.0.0
   */
  @SafeVarargs
  public static <T> T map(final JBBPFieldStruct root, final String structPath, final T instance, final Function<Class<?>, Object>... instantiators) {
    return map(root, structPath, instance, null, instantiators);
  }

  /**
   * Create a class instance, map binary data of a structure for its path to its
   * fields and return the instance.
   *
   * @param <T>           the mapping class type
   * @param root          a parsed structure to be used as the root, must not be null
   * @param structPath    the path of a structure inside of the root to be mapped
   *                      to the class, must not be null
   * @param instance      object to be filled by values, must not be null
   * @param flags         special flags to tune mapping process
   * @param instantiators functions to produce class instance by request,
   *                      must not be null
   * @return the created and mapped instance of the mapping class
   * @throws JBBPMapperException for any error
   * @see #FLAG_IGNORE_MISSING_VALUES
   * @since 2.0.0
   */
  @SafeVarargs
  public static <T> T map(final JBBPFieldStruct root, final String structPath, final T instance, final int flags, final Function<Class<?>, Object>... instantiators) {
    return map(root, structPath, instance, null, flags, instantiators);
  }

  /**
   * Create a class instance, map binary data of a structure for its path to its
   * fields and return the instance.
   *
   * @param <T>                  the mapping class type
   * @param root                 a parsed structure to be used as the root, must not be null
   * @param structPath           the path of a structure inside of the root to be mapped
   *                             to the class, must not be null
   * @param instance             instance to be filled by values, must not be null
   * @param customFieldProcessor a custom field processor to provide custom
   *                             values, it can be null if there is not any mapping field desires the
   *                             processor
   * @param instantiators        functions produce class instance by request,
   *                             must not be null
   * @return the created and mapped instance of the mapping class
   * @throws JBBPMapperException for any error
   * @since 2.0.0
   */
  @SafeVarargs
  public static <T> T map(final JBBPFieldStruct root, final String structPath, final T instance, final JBBPMapperCustomFieldProcessor customFieldProcessor, final Function<Class<?>, Object>... instantiators) {
    return map(root, structPath, instance, customFieldProcessor, 0, instantiators);
  }

  /**
   * Create a class instance, map binary data of a structure for its path to its
   * fields and return the instance.
   *
   * @param <T>                  the mapping class type
   * @param root                 a parsed structure to be used as the root, must not be null
   * @param structPath           the path of a structure inside of the root to be mapped
   *                             to the class, must not be null
   * @param instance             object to be filled by values, must not be null
   * @param customFieldProcessor a custom field processor to provide custom
   *                             values, it can be null if there is not any mapping field desires the
   *                             processor
   * @param flags                special flags to tune mapping
   * @param instantiators        functions produce class instance by request,
   *                             must not be null
   * @return the created and mapped instance of the mapping class
   * @throws JBBPMapperException for any error
   * @see #FLAG_IGNORE_MISSING_VALUES
   * @since 2.0.0
   */
  @SafeVarargs
  public static <T> T map(final JBBPFieldStruct root, final String structPath, final T instance, final JBBPMapperCustomFieldProcessor customFieldProcessor, final int flags, final Function<Class<?>, Object>... instantiators) {
    JBBPUtils.assertNotNull(structPath, "Path must not be null");
    final JBBPFieldStruct struct = root.findFieldForPathAndType(structPath, JBBPFieldStruct.class);
    if (struct == null) {
      throw new JBBPMapperException("Can't find a structure field for its path [" + structPath + ']', null, instance.getClass(), null, null);
    }
    return map(struct, instance, customFieldProcessor, flags, instantiators);
  }

  /**
   * Create a class instance, map binary data of a structure to the instance and
   * return it. It will create a class instance through a hack method and its
   * constructor will not be called, thus use the method carefully.
   *
   * @param <T>           the mapping class type
   * @param root          a parsed structure to be mapped to the class instance, must not
   *                      be null
   * @param instance      object instance to be filled by values, must not be null
   * @param instantiators functions produce class instance by request,
   *                      must not be null
   * @return the created and mapped instance of the class
   * @throws JBBPMapperException for any error
   * @since 2.0.0
   */
  @SafeVarargs
  public static <T> T map(final JBBPFieldStruct root, final T instance, final Function<Class<?>, Object>... instantiators) {
    return map(root, instance, null, instantiators);
  }

  /**
   * Create a class instance, map binary data of a structure to the instance and
   * return it. It will create a class instance through a hack method and its
   * constructor will not be called, thus use the method carefully.
   *
   * @param <T>           the mapping class type
   * @param root          a parsed structure to be mapped to the class instance, must not
   *                      be null
   * @param instance      the class to be instantiated and mapped, must not be
   *                      null
   * @param flags         special flags to tune mapping process
   * @param instantiators functions produce class instance by request,
   *                      must not be null
   * @return the created and mapped instance of the class
   * @throws JBBPMapperException for any error
   * @see #FLAG_IGNORE_MISSING_VALUES
   * @since 2.0.0
   */
  @SafeVarargs
  public static <T> T map(final JBBPFieldStruct root, final T instance, final int flags, final Function<Class<?>, Object>... instantiators) {
    return map(root, instance, null, flags, instantiators);
  }

  /**
   * Map a structure to a class instance.
   *
   * @param <T>                  the mapping class type
   * @param rootStructure        a structure to be mapped, must not be null
   * @param instance             a class instance to be destination for map
   *                             operations, must not be null
   * @param customFieldProcessor a custom field processor to provide custom
   *                             values, it can be null if there is not any mapping field desires the
   *                             processor
   * @param instantiators        functions to produce class instance by request,
   *                             must not be null
   * @return the processed class instance, the same which was the argument for
   * the method.
   * @throws JBBPMapperException for any error
   */
  @SafeVarargs
  public static <T> T map(final JBBPFieldStruct rootStructure, final T instance, final JBBPMapperCustomFieldProcessor customFieldProcessor, final Function<Class<?>, Object>... instantiators) {
    return map(rootStructure, instance, customFieldProcessor, 0, instantiators);
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  private static void processFieldOfMappedClass(
      final MappedFieldRecord record,
      final JBBPFieldStruct rootStructure,
      final Object instance,
      final JBBPMapperCustomFieldProcessor customFieldProcessor,
      final int flags,
      final Function<Class<?>, Object>... instantiators
  ) {
    if (record.binAnnotation.custom()) {
      JBBPUtils.assertNotNull(customFieldProcessor, "There is a custom mapping field, in the case you must provide a custom mapping field processor");
      final Object value = customFieldProcessor.prepareObjectForMapping(rootStructure, record.binAnnotation, record.mappingField);
      MappedFieldRecord.setFieldValue(instance, record.setter, record.mappingField, null, value);
    } else {
      final JBBPAbstractField binField;

      if (record.fieldPath.length() == 0) {
        binField = record.fieldName.length() == 0 ? rootStructure.findFieldForType(record.fieldType.getFieldClass()) : rootStructure.findFieldForNameAndType(record.fieldName, record.fieldType.getFieldClass());
      } else {
        binField = rootStructure.findFieldForPathAndType(record.fieldPath, record.fieldType.getFieldClass());
      }

      if (binField == null) {
        if ((flags & FLAG_IGNORE_MISSING_VALUES) != 0) {
          return;
        }
        throw new JBBPMapperException("Can't find value for mapping field [" + record.mappingField + ']', null, record.mappingClass, record.mappingField, null);
      }

      if (record.bitWideField && record.mappedBitNumber != JBBPBitNumber.BITS_8 && ((BitEntity) binField).getBitWidth() != record.mappedBitNumber) {
        throw new JBBPMapperException("Can't map mapping field because wrong field bitness [" + record.mappedBitNumber + "!=" + ((BitEntity) binField).getBitWidth().getBitNumber() + ']', null, record.mappingClass, record.mappingField, null);
      }
      record.proc.apply(record, rootStructure, instance, customFieldProcessor, binField, flags, instantiators);
    }
  }

  /**
   * Map a structure to a class instance.
   *
   * @param <T>                  the mapping class type
   * @param rootStructure        a structure to be mapped, must not be null
   * @param instance             a class instance to be destination for map
   *                             operations, must not be null
   * @param customFieldProcessor a custom field processor to provide custom
   *                             values, it can be null if there is not any mapping field desires the
   *                             processor
   * @param flags                special flags for mapping process
   * @param instantiators        functions to produce class instance by request, must
   *                             not be null
   * @return the processed class instance, the same which was the argument for
   * the method.
   * @throws JBBPMapperException for any error
   * @see #FLAG_IGNORE_MISSING_VALUES
   * @since 1.1
   */
  @SafeVarargs
  @SuppressWarnings("varargs")
  public static <T> T map(final JBBPFieldStruct rootStructure, final T instance, final JBBPMapperCustomFieldProcessor customFieldProcessor, final int flags, final Function<Class<?>, Object>... instantiators) {
    JBBPUtils.assertNotNull(rootStructure, "The Root structure must not be null");
    JBBPUtils.assertNotNull(instance, "The Mapping class instance must not be null");

    // Don't use forEach() for Android compatibility!
    for (final MappedFieldRecord record : findAffectedFields(instance)) {
      processFieldOfMappedClass(
          record,
          rootStructure,
          instance,
          customFieldProcessor,
          flags,
          instantiators
      );
    }
    return instance;
  }

  /**
   * Get current number of classes which fields are cached in internal field cache.
   *
   * @return number of classes
   * @since 2.0.0
   */
  public static int getFieldCacheSize() {
    return CACHED_FIELDS.size();
  }

  /**
   * Clear internal class field cache.
   *
   * @since 2.0.0
   */
  public static void clearFieldCache() {
    CACHED_FIELDS.clear();
  }

  public static List<MappedFieldRecord> findAffectedFields(final Object instance) {
    final Class<?> mappingClass = instance.getClass();

    List<MappedFieldRecord> result = CACHED_FIELDS.get(mappingClass);
    if (result == null) {
      result = new ArrayList<>();

      final Bin defaultAnno = mappingClass.getAnnotation(Bin.class);

      // make chain of ancestors till java.lang.Object
      final List<Class<?>> listOfClassHierarchy = new ArrayList<>();
      Class<?> current = instance.getClass();
      while (current != null) {
        final String packageName = current.getPackage().getName();
        if (packageName.startsWith("java.")
            || packageName.startsWith("javax.")
            || packageName.startsWith("android.")
        ) {
          break;
        }
        listOfClassHierarchy.add(current);
        current = current.getSuperclass();
      }

      for (final Class<?> processingClazz : listOfClassHierarchy) {
        for (Field mappingField : processingClazz.getDeclaredFields()) {
          final int fieldModifiers = mappingField.getModifiers();

          final Bin fieldAnno = mappingField.getAnnotation(Bin.class);
          final Bin mappedAnno;
          if ((fieldAnno == null && defaultAnno == null) || mappingField.getName().indexOf('$') >= 0) {
            continue;
          }
          mappedAnno = fieldAnno == null ? defaultAnno : fieldAnno;

          if (fieldAnno == null) {
            if (Modifier.isTransient(fieldModifiers)
                || Modifier.isStatic(fieldModifiers)
                || Modifier.isPrivate(fieldModifiers)
                || Modifier.isFinal(fieldModifiers)) {
              continue;
            }
          } else {
            final String disallowedModifier;
            if (Modifier.isStatic(fieldModifiers)) {
              disallowedModifier = "STATIC";
            } else if (Modifier.isFinal(fieldModifiers)) {
              disallowedModifier = "FINAL";
            } else if (Modifier.isPrivate(fieldModifiers)) {
              disallowedModifier = "PRIVATE";
            } else {
              disallowedModifier = null;
            }
            if (disallowedModifier != null) {
              throw new JBBPMapperException("Detected @Bin marked " + disallowedModifier + " field", null, processingClazz, mappingField, null);
            }
          }

          if (!ReflectUtils.isPotentiallyAccessibleField(mappingField)) {
            mappingField = ReflectUtils.makeAccessible(mappingField);
          }

          try {
            result.add(new MappedFieldRecord(mappingField, null, null, mappingClass, mappedAnno));
          } catch (IllegalStateException ex) {
            throw new JBBPMapperException(ex.getMessage(), null, mappingClass, mappingField, ex);
          }
        }
      }

      Collections.sort(result);

      CACHED_FIELDS.put(mappingClass, result);
    }

    return result;
  }

}
