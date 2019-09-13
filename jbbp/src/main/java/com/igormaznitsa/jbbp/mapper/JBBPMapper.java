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
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.model.BitEntity;
import com.igormaznitsa.jbbp.model.JBBPAbstractArrayField;
import com.igormaznitsa.jbbp.model.JBBPAbstractField;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayBit;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayByte;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayInt;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayLong;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayShort;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayStruct;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayUByte;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayUShort;
import com.igormaznitsa.jbbp.model.JBBPFieldInt;
import com.igormaznitsa.jbbp.model.JBBPFieldLong;
import com.igormaznitsa.jbbp.model.JBBPFieldString;
import com.igormaznitsa.jbbp.model.JBBPFieldStruct;
import com.igormaznitsa.jbbp.model.JBBPNumericField;
import com.igormaznitsa.jbbp.utils.Function;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import com.igormaznitsa.jbbp.utils.ReflectUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * The Class processes mapping of a parsed binary data to class fields.
 *
 * @since 1.0
 */
public final class JBBPMapper {

  public static final String MAKE_CLASS_INSTANCE_METHOD_NAME = "makeClassInstance";

  /**
   * Flag to not throw exception if structure doesn't have value for a field.
   *
   * @since 1.1
   */
  public static final int FLAG_IGNORE_MISSING_VALUES = 1;

  private static final Function<Class<?>, Object> STATIC_MAKE_CLASS_INSTANCE_INSTANTIATOR = (Class<?> aClass) -> {
    try {
      final Method method = aClass.getMethod(MAKE_CLASS_INSTANCE_METHOD_NAME, Class.class);
      if (Modifier.isStatic(method.getModifiers())) {
        return method.invoke(null, aClass);
      } else {
        return null;
      }
    } catch (IllegalAccessException ex) {
      throw new RuntimeException(String.format("Can't get access to static method %s(Class aClass) in %s", MAKE_CLASS_INSTANCE_METHOD_NAME, aClass), ex);
    } catch (InvocationTargetException ex) {
      throw new RuntimeException(String.format("Can't call static method %s(Class aClass) in %s", MAKE_CLASS_INSTANCE_METHOD_NAME, aClass), ex);
    } catch (NoSuchMethodException ex) {
      return null;
    }
  };

  private static final Function<Class<?>, Object> DEFAULT_CONSTRUCTOR_INSTANTIATOR = (Class<?> aClass) -> {
    try {
      if (!aClass.isLocalClass() || Modifier.isStatic(aClass.getModifiers())) {
        return aClass.getConstructor().newInstance();
      } else {
        return null;
      }
    } catch (NoSuchMethodException ex) {
      return null;
    } catch (InvocationTargetException ex) {
      throw new RuntimeException(String.format("Error during default constructor call, class %s", aClass), ex);
    } catch (IllegalAccessException ex) {
      throw new RuntimeException(String.format("Can't get access to default constructor , class %s", aClass), ex);
    } catch (InstantiationException ex) {
      throw new RuntimeException(String.format("Can't make instance of class %s", aClass), ex);
    }
  };

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

    final Class<?> mappingClass = instance.getClass();

    final Bin defaultAnno = mappingClass.getAnnotation(Bin.class);

    // make chain of ancestors till java.lang.Object
    final List<Class<?>> listOfClassHierarchy = new ArrayList<>();
    Class<?> current = instance.getClass();
    while (current != java.lang.Object.class) {
      listOfClassHierarchy.add(current);
      current = current.getSuperclass();
    }

    for (final Class<?> processingClazz : listOfClassHierarchy) {
      for (Field mappingField : processingClazz.getDeclaredFields()) {
        final int modifiers = mappingField.getModifiers();
        if (Modifier.isTransient(modifiers) || Modifier.isStatic(modifiers)) {
          continue;
        }

        mappingField = ReflectUtils.makeAccessible(mappingField);

        final Bin fieldAnno = mappingField.getAnnotation(Bin.class);
        final Bin mappedAnno;
        if ((fieldAnno == null && defaultAnno == null) || mappingField.getName().indexOf('$') >= 0) {
          continue;
        }
        mappedAnno = fieldAnno == null ? defaultAnno : fieldAnno;

        if (mappedAnno.custom()) {
          JBBPUtils.assertNotNull(customFieldProcessor, "There is a custom mapping field, in the case you must provide a custom mapping field processor");
          final Object value = customFieldProcessor.prepareObjectForMapping(rootStructure, mappedAnno, mappingField);
          setFieldValue(instance, mappingField, null, value);
        } else {
          final BinType fieldType;

          final JBBPBitNumber mappedBitNumber = mappedAnno.outBitNumber();

          if (mappedAnno.type() == BinType.UNDEFINED) {
            BinType thetype = BinType.findCompatible(mappingField.getType());
            if (thetype == null) {
              throw new JBBPMapperException("Can't find compatible type for a mapping field", rootStructure, mappingClass, mappingField, null);
            } else if (mappedBitNumber.getBitNumber() < 8 && !(thetype == BinType.STRUCT || thetype == BinType.STRUCT_ARRAY)) {
              thetype = thetype.isArray() ? BinType.BIT_ARRAY : BinType.BIT;
            }
            fieldType = thetype;
          } else {
            fieldType = mappedAnno.type();
          }
          final boolean bitWideField = fieldType == BinType.BIT || fieldType == BinType.BIT_ARRAY;

          final String fieldName = mappedAnno.name().length() == 0 ? mappingField.getName() : mappedAnno.name();
          final String fieldPath = mappedAnno.path();

          final JBBPAbstractField binField;

          if (fieldPath.length() == 0) {
            binField = fieldName.length() == 0 ? rootStructure.findFieldForType(fieldType.getFieldClass()) : rootStructure.findFieldForNameAndType(fieldName, fieldType.getFieldClass());
          } else {
            binField = rootStructure.findFieldForPathAndType(fieldPath, fieldType.getFieldClass());
          }

          if (binField == null) {
            if ((flags & FLAG_IGNORE_MISSING_VALUES) != 0) {
              continue;
            }
            throw new JBBPMapperException("Can't find value to be mapped to a mapping field [" + mappingField + ']', null, mappingClass, mappingField, null);
          }

          if (bitWideField && mappedBitNumber != JBBPBitNumber.BITS_8 && ((BitEntity) binField).getBitWidth() != mappedBitNumber) {
            throw new JBBPMapperException("Can't map value to a mapping field for different field bit width [" + mappedBitNumber + "!=" + ((BitEntity) binField).getBitWidth().getBitNumber() + ']', null, mappingClass, mappingField, null);
          }

          if (mappingField.getType().isArray()) {
            if (binField instanceof JBBPAbstractArrayField) {
              if (binField instanceof JBBPFieldArrayStruct) {
                // structure
                final JBBPFieldArrayStruct structArray = (JBBPFieldArrayStruct) binField;
                final Class<?> componentType = mappingField.getType().getComponentType();

                Object valueArray = getFieldValue(instance, mappingField);

                valueArray = valueArray == null ? Array.newInstance(componentType, structArray.size()) : valueArray;

                if (Array.getLength(valueArray) != structArray.size()) {
                  throw new JBBPMapperException("Can't map an array field for different expected size [" + Array.getLength(valueArray) + "!=" + structArray.size() + ']', binField, mappingClass, mappingField, null);
                }

                for (int i = 0; i < structArray.size(); i++) {
                  final Object curInstance = Array.get(valueArray, i);
                  if (curInstance == null) {
                    Array.set(valueArray, i, map(structArray.getElementAt(i), tryMakeInstance(componentType, binField, instance, mappingField, instantiators), customFieldProcessor, instantiators));
                  } else {
                    Array.set(valueArray, i, map(structArray.getElementAt(i), curInstance, customFieldProcessor));
                  }
                }
                setFieldValue(instance, mappingField, binField, valueArray);
              } else {
                // primitive
                mapArrayField(instance, mappingField, (JBBPAbstractArrayField<?>) binField, mappedAnno.bitOrder() == JBBPBitOrder.MSB0);
              }
            } else {
              throw new JBBPMapperException("Can't map a non-array value to an array mapping field", binField, mappingClass, mappingField, null);
            }
          } else {
            if (binField instanceof JBBPNumericField) {
              mapNumericField(instance, mappingField, (JBBPNumericField) binField, mappedAnno.bitOrder() == JBBPBitOrder.MSB0);
            } else if (binField instanceof JBBPFieldString) {
              if (mappingField.getType().isPrimitive()) {
                throw new JBBPMapperException("Can't map a string to a primitive mapping field", binField, mappingClass, mappingField, null);
              } else {
                setFieldValue(instance, mappingField, binField, ((JBBPFieldString) binField).getAsString());
              }
            } else if (binField instanceof JBBPFieldStruct) {
              if (mappingField.getType().isPrimitive()) {
                throw new JBBPMapperException("Can't map a structure to a primitive mapping field", binField, mappingClass, mappingField, null);
              } else {
                final Object curValue = getFieldValue(instance, mappingField);
                if (curValue == null) {
                  setFieldValue(instance, mappingField, binField, map((JBBPFieldStruct) binField, tryMakeInstance(mappingField.getType(), binField, instance, mappingField, instantiators), customFieldProcessor));
                } else {
                  setFieldValue(instance, mappingField, binField, map((JBBPFieldStruct) binField, curValue, customFieldProcessor));
                }
              }
            } else {
              boolean processed = false;
              if (mappingField.getType() == String.class && binField instanceof JBBPAbstractArrayField) {
                final String convertedValue = convertFieldValueToString((JBBPAbstractArrayField<?>) binField);
                if (convertedValue != null) {
                  setFieldValue(instance, mappingField, binField, convertedValue);
                  processed = true;
                }
              }
              if (!processed) {
                throw new JBBPMapperException("Can't map a field for its value incompatibility", binField, mappingClass, mappingField, null);
              }
            }
          }
        }
      }
    }
    return instance;
  }

  private static <T> T tryMakeInstance(
      final Class<T> type,
      final JBBPAbstractField binField,
      final Object mappingObject,
      final Field mappingField,
      final Function<Class<?>, Object>[] instantiators
  ) {
    T result = null;
    for (final Function<Class<?>, Object> instantiator : instantiators) {
      result = type.cast(instantiator.apply(type));
      if (result != null) {
        break;
      }
    }

    if (result == null) {
      Exception detectedException = null;
      try {
        result = type.cast(mappingObject.getClass().getMethod(MAKE_CLASS_INSTANCE_METHOD_NAME, Class.class).invoke(mappingObject, type));
      } catch (NoSuchMethodException ex) {
        // do nothing
      } catch (IllegalAccessException ex) {
        // WARNING! Don't replace by multicatch for Android compatibility!
        detectedException = ex;
      } catch (InvocationTargetException ex) {
        detectedException = ex;
      }

      if (detectedException != null) {
        throw new RuntimeException(String.format("Error during %s() call: %s", MAKE_CLASS_INSTANCE_METHOD_NAME, mappingObject.getClass()), detectedException);
      }

      if (result == null) {
        result = type.cast(STATIC_MAKE_CLASS_INSTANCE_INSTANTIATOR.apply(type));
        if (result == null) {
          result = type.cast(DEFAULT_CONSTRUCTOR_INSTANTIATOR.apply(type));
        }
      }

      if (result == null) {
        throw new JBBPMapperException(String.format("Can't create instance of %s", type), binField, mappingObject.getClass(), mappingField, null);
      }
    }
    return result;
  }

  /**
   * Convert an array field into its string representation.
   *
   * @param field an array field to be converted, must not be null
   * @return the string representation of the array or null if the field can't
   * be converted
   */
  private static String convertFieldValueToString(final JBBPAbstractArrayField<?> field) {
    final StringBuilder result;
    if (field instanceof JBBPFieldArrayBit) {
      final JBBPFieldArrayBit array = (JBBPFieldArrayBit) field;
      result = new StringBuilder(array.size());
      for (final byte b : array.getArray()) {
        result.append((char) (b & 0xFF));
      }
    } else if (field instanceof JBBPFieldArrayByte) {
      final JBBPFieldArrayByte array = (JBBPFieldArrayByte) field;
      result = new StringBuilder(array.size());
      for (final byte b : array.getArray()) {
        result.append((char) (b & 0xFF));
      }
    } else if (field instanceof JBBPFieldArrayUByte) {
      final JBBPFieldArrayUByte array = (JBBPFieldArrayUByte) field;
      result = new StringBuilder(array.size());
      for (final byte b : array.getArray()) {
        result.append((char) (b & 0xFF));
      }
    } else if (field instanceof JBBPFieldArrayShort) {
      final JBBPFieldArrayShort array = (JBBPFieldArrayShort) field;
      result = new StringBuilder(array.size());
      for (final short b : array.getArray()) {
        result.append((char) b);
      }
    } else if (field instanceof JBBPFieldArrayUShort) {
      final JBBPFieldArrayUShort array = (JBBPFieldArrayUShort) field;
      result = new StringBuilder(array.size());
      for (final short b : array.getArray()) {
        result.append((char) b);
      }
    } else {
      result = null;
    }
    return result == null ? null : result.toString();
  }

  /**
   * Set a value to a field of a class instance. Can't be used for static
   * fields!
   *
   * @param classInstance a class instance
   * @param classField    a mapping class field which should be set by the value,
   *                      must not be null
   * @param binField      a parsed bin field which value will be set, can be null
   * @param value         a value to be set to the class field
   */
  private static void setFieldValue(final Object classInstance, final Field classField, final JBBPAbstractField binField, final Object value) {
    try {
      classField.set(classInstance, value);
    } catch (IllegalArgumentException ex) {
      throw new JBBPMapperException("Can't set value to a mapping field", binField, classInstance.getClass(), classField, ex);
    } catch (IllegalAccessException ex) {
      throw new JBBPMapperException("Can't get access to a mapping field", binField, classInstance.getClass(), classField, ex);
    }
  }

  /**
   * Get a value of a field from a class instance.
   *
   * @param classInstance a class instance object
   * @param classField    a class field which value must be returned, must not be
   *                      null
   * @return the field value for the class instance
   */
  private static Object getFieldValue(final Object classInstance, final Field classField) {
    try {
      return classField.get(classInstance);
    } catch (IllegalArgumentException ex) {
      throw new JBBPMapperException("Can't set get value from a mapping field", null, classInstance.getClass(), classField, ex);
    } catch (IllegalAccessException ex) {
      throw new JBBPMapperException("Can't get access to a mapping field", null, classInstance.getClass(), classField, ex);
    }
  }

  /**
   * Map a parsed primitive numeric field to a primitive field in a mapping
   * class.
   *
   * @param mappingClassInstance the mapping class instance, must not be null
   * @param mappingField         a mapping field to set the value, must not be null
   * @param numericField         a parsed numeric field which value should be used, must
   *                             not be null
   * @param invertBitOrder       flag shows that the parsed numeric field value must
   *                             be reversed in its bit before setting
   */
  private static void mapNumericField(final Object mappingClassInstance, final Field mappingField, final JBBPNumericField numericField, final boolean invertBitOrder) {
    final Class<?> fieldClass = mappingField.getType();
    try {
      if (fieldClass == byte.class) {
        mappingField.setByte(mappingClassInstance, (byte) (invertBitOrder ? numericField.getAsInvertedBitOrder() : numericField.getAsInt()));
      } else if (fieldClass == boolean.class) {
        mappingField.setBoolean(mappingClassInstance, numericField.getAsBool());
      } else if (fieldClass == char.class) {
        mappingField.setChar(mappingClassInstance, (char) (invertBitOrder ? numericField.getAsInvertedBitOrder() : numericField.getAsInt()));
      } else if (fieldClass == short.class) {
        mappingField.setShort(mappingClassInstance, (short) (invertBitOrder ? numericField.getAsInvertedBitOrder() : numericField.getAsInt()));
      } else if (fieldClass == int.class) {
        mappingField.setInt(mappingClassInstance, (int) (invertBitOrder ? numericField.getAsInvertedBitOrder() : numericField.getAsInt()));
      } else if (fieldClass == long.class) {
        mappingField.setLong(mappingClassInstance, (invertBitOrder ? numericField.getAsInvertedBitOrder() : numericField.getAsLong()));
      } else if (fieldClass == float.class) {
        if (numericField instanceof JBBPFieldInt) {
          mappingField.setFloat(mappingClassInstance, invertBitOrder ? Float.intBitsToFloat((int) numericField.getAsInvertedBitOrder()) : Float.intBitsToFloat(numericField.getAsInt()));
        } else {
          mappingField.setFloat(mappingClassInstance, invertBitOrder ? Float.intBitsToFloat((int) numericField.getAsInvertedBitOrder()) : numericField.getAsFloat());
        }
      } else if (fieldClass == double.class) {
        if (numericField instanceof JBBPFieldLong) {
          mappingField.setDouble(mappingClassInstance, invertBitOrder ? Double.longBitsToDouble(numericField.getAsInvertedBitOrder()) : Double.longBitsToDouble(numericField.getAsLong()));
        } else {
          mappingField.setDouble(mappingClassInstance, invertBitOrder ? Double.longBitsToDouble(numericField.getAsInvertedBitOrder()) : numericField.getAsDouble());
        }
      } else {
        throw new JBBPMapperException("Unsupported mapping class field type to be mapped for binary parsed data", (JBBPAbstractField) numericField, mappingClassInstance.getClass(), mappingField, null);
      }
    } catch (IllegalAccessException ex) {
      throw new JBBPMapperException("Can't get access to a mapping field", (JBBPAbstractField) numericField, mappingClassInstance.getClass(), mappingField, ex);
    } catch (IllegalArgumentException ex) {
      throw new JBBPMapperException("Can't set argument to a mapping field", (JBBPAbstractField) numericField, mappingClassInstance.getClass(), mappingField, ex);
    }
  }

  /**
   * Map a parsed array to an array field in mapping class.
   *
   * @param mappingClassInstance a mapping class instance, must not be null
   * @param mappingField         a field in the mapping class to be set, must not be
   *                             null
   * @param arrayField           a binary parsed array field, must not be null
   * @param invertBitOrder       flag shows that values of an array must be bit
   *                             reversed before set
   */
  private static void mapArrayField(final Object mappingClassInstance, final Field mappingField, final JBBPAbstractArrayField<?> arrayField, final boolean invertBitOrder) {
    try {
      if (arrayField instanceof JBBPFieldArrayLong && mappingField.getType().getComponentType() == double.class) {
        final long[] longarray = (long[]) arrayField.getValueArrayAsObject(invertBitOrder);
        final double[] doublearray = new double[longarray.length];
        for (int i = 0; i < longarray.length; i++) {
          doublearray[i] = Double.longBitsToDouble(longarray[i]);
        }
        mappingField.set(mappingClassInstance, doublearray);
      } else if (arrayField instanceof JBBPFieldArrayInt && mappingField.getType().getComponentType() == float.class) {
        final int[] intarray = (int[]) arrayField.getValueArrayAsObject(invertBitOrder);
        final float[] floatarray = new float[intarray.length];
        for (int i = 0; i < intarray.length; i++) {
          floatarray[i] = Float.intBitsToFloat(intarray[i]);
        }
        mappingField.set(mappingClassInstance, floatarray);
      } else if (arrayField instanceof JBBPFieldArrayUShort && mappingField.getType().getComponentType() == char.class) {
        final short[] shortarray = (short[]) arrayField.getValueArrayAsObject(invertBitOrder);
        final char[] chararray = new char[shortarray.length];
        for (int i = 0; i < shortarray.length; i++) {
          chararray[i] = (char) shortarray[i];
        }
        mappingField.set(mappingClassInstance, chararray);
      } else {
        mappingField.set(mappingClassInstance, arrayField.getValueArrayAsObject(invertBitOrder));
      }
    } catch (IllegalAccessException ex) {
      throw new JBBPMapperException("Can't get access to a mapping field", arrayField, mappingClassInstance.getClass(), mappingField, ex);
    } catch (IllegalArgumentException ex) {
      throw new JBBPMapperException("Can't set argument to a mapping field", arrayField, mappingClassInstance.getClass(), mappingField, ex);
    }
  }

}
