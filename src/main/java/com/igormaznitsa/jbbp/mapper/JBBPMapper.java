/*
 * Copyright 2014 Igor Maznitsa.
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
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.mapper.instantiators.JBBPClassInstantiator;
import com.igormaznitsa.jbbp.mapper.instantiators.JBBPClassInstantiatorFactory;
import com.igormaznitsa.jbbp.model.*;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The Class processes mapping of a parsed binary data to class fields. The
 * Class uses sun.misc.Unsafe which creates class instances without any
 * constructor call.
 *
 * @see sun.misc.Unsafe
 * @since 1.0
 */
public final class JBBPMapper {

  /**
   * The Special auxiliary object to generate class instances.
   */
  private static final JBBPClassInstantiator klazzInstantiator = JBBPClassInstantiatorFactory.getInstance().make();

  /**
   * Create a class instance, map binary data of a structure for its path to its
   * fields and return the instance.
   *
   * @param <T> the mapping class type
   * @param root a parsed structure to be used as the root, must not be null
   * @param structPath the path of a structure inside of the root to be mapped
   * to the class, must not be null
   * @param mappingClass the mapping class, must not be null and must have the
   * default constructor
   * @return the created and mapped instance of the mapping class
   * @throws JBBPMapperException for any error
   */
  public static <T> T map(final JBBPFieldStruct root, final String structPath, final Class<T> mappingClass) {
    return map(root, structPath, mappingClass, null);
  }

  /**
   * Create a class instance, map binary data of a structure for its path to its
   * fields and return the instance.
   *
   * @param <T> the mapping class type
   * @param root a parsed structure to be used as the root, must not be null
   * @param structPath the path of a structure inside of the root to be mapped
   * to the class, must not be null
   * @param mappingClass the mapping class, must not be null and must have the
   * default constructor
   * @param customFieldProcessor a custom field processor to provide custom
   * values, it can be null if there is not any mapping field desires the
   * processor
   * @return the created and mapped instance of the mapping class
   * @throws JBBPMapperException for any error
   */
  public static <T> T map(final JBBPFieldStruct root, final String structPath, final Class<T> mappingClass, final JBBPMapperCustomFieldProcessor customFieldProcessor) {
    JBBPUtils.assertNotNull(structPath, "Path must not be null");
    final JBBPFieldStruct struct = root.findFieldForPathAndType(structPath, JBBPFieldStruct.class);
    if (struct == null) {
      throw new JBBPMapperException("Can't find a structure field for its path [" + structPath + ']', null, mappingClass, null, null);
    }
    return map(struct, mappingClass, customFieldProcessor);
  }

  /**
   * Create a class instance, map binary data of a structure to the instance and
   * return it. It will create a class instance through a hack method and its
   * constructor will not be called, thus use the method carefully.
   *
   * @param <T> the mapping class type
   * @param root a parsed structure to be mapped to the class instance, must not
   * be null
   * @param mappingClass the class to be instantiated and mapped, must not be
   * null
   * @return the created and mapped instance of the class
   * @throws JBBPMapperException for any error
   */
  public static <T> T map(final JBBPFieldStruct root, final Class<T> mappingClass) {
    return mappingClass.cast(map(root, allocateMemoryForClass(root, mappingClass), null));
  }

  /**
   * Create a class instance, map binary data of a structure to the instance and
   * return it. It will create a class instance through a hack method and its
   * constructor will not be called, thus use the method carefully.
   *
   * @param <T> the mapping class type
   * @param root a parsed structure to be mapped to the class instance, must not
   * be null
   * @param mappingClass the class to be instantiated and mapped, must not be
   * null
   * @param customFieldProcessor a custom field processor to provide custom
   * values, it can be null if there is not any mapping field desires the
   * processor
   * @return the created and mapped instance of the class
   * @throws JBBPMapperException for any error
   */
  public static <T> T map(final JBBPFieldStruct root, final Class<T> mappingClass, final JBBPMapperCustomFieldProcessor customFieldProcessor) {
    return mappingClass.cast(map(root, allocateMemoryForClass(root, mappingClass), customFieldProcessor));
  }

  /**
   * Map a structure to a class instance.
   *
   * @param rootStructure a structure to be mapped, must not be null
   * @param mappingClassInstance a class instance to be destination for map
   * operations, must not be null
   * @param customFieldProcessor a custom field processor to provide custom
   * values, it can be null if there is not any mapping field desires the
   * processor
   * @return the processed class instance, the same which was the argument for
   * the method.
   * @throws JBBPMapperException for any error
   */
  public static Object map(final JBBPFieldStruct rootStructure, final Object mappingClassInstance, final JBBPMapperCustomFieldProcessor customFieldProcessor) {
    JBBPUtils.assertNotNull(rootStructure, "The Root structure must not be null");
    JBBPUtils.assertNotNull(mappingClassInstance, "The Mapping class instance must not be null");

    final Class<?> mappingClass = mappingClassInstance.getClass();

    final Bin defaultAnno = mappingClass.getAnnotation(Bin.class);

    // make chain of ancestors till java.lang.Object
    final List<Class<?>> listOfClassHierarchy = new ArrayList<Class<?>>();
    Class<?> current = mappingClassInstance.getClass();
    while (current != java.lang.Object.class) {
      listOfClassHierarchy.add(current);
      current = current.getSuperclass();
    }

    for (final Class<?> processingClazz : listOfClassHierarchy) {
      for (final Field mappingField : processingClazz.getDeclaredFields()) {
        if (Modifier.isTransient(mappingField.getModifiers())) {
          continue;
        }

        JBBPUtils.makeAccessible(mappingField);

        final Bin fieldAnno = mappingField.getAnnotation(Bin.class);
        final Bin mappedAnno;
        if ((fieldAnno == null && defaultAnno == null) || mappingField.getName().indexOf('$') >= 0) {
          continue;
        }
        mappedAnno = fieldAnno == null ? defaultAnno : fieldAnno;

        if (mappedAnno.custom()) {
          JBBPUtils.assertNotNull(customFieldProcessor, "There is a custom mapping field, in the case you must provide a custom mapping field processor");
          final Object value = customFieldProcessor.prepareObjectForMapping(rootStructure, mappedAnno, mappingField);
          setFieldValue(mappingClassInstance, mappingField, null, value);
        }
        else {
          final BinType fieldType;

          final JBBPBitNumber mappedBitNumber = mappedAnno.outBitNumber();

          if (mappedAnno.type() == BinType.UNDEFINED) {
            BinType thetype = BinType.findCompatible(mappingField.getType());
            if (thetype == null) {
              throw new JBBPMapperException("Can't find compatible type for a mapping field", rootStructure, mappingClass, mappingField, null);
            }
            else if (mappedBitNumber.getBitNumber()<8 && !(thetype == BinType.STRUCT || thetype == BinType.STRUCT_ARRAY)) {
              thetype = thetype.isArray() ? BinType.BIT_ARRAY : BinType.BIT;
            }
            fieldType = thetype;
          }
          else {
            fieldType = mappedAnno.type();
          }
          final boolean bitWideField = fieldType == BinType.BIT || fieldType == BinType.BIT_ARRAY;

          final String fieldName = mappedAnno.name().length() == 0 ? mappingField.getName() : mappedAnno.name();
          final String fieldPath = mappedAnno.path();

          final JBBPAbstractField binField;

          if (fieldPath.length() == 0) {
            binField = fieldName.length() == 0 ? rootStructure.findFieldForType(fieldType.getFieldClass()) : rootStructure.findFieldForNameAndType(fieldName, fieldType.getFieldClass());
          }
          else {
            binField = rootStructure.findFieldForPathAndType(fieldPath, fieldType.getFieldClass());
          }

          if (binField == null) {
            throw new JBBPMapperException("Can't find value to be mapped to a mapping field [" + mappingField + ']', null, mappingClass, mappingField, null);
          }

          if (bitWideField && mappedBitNumber!=JBBPBitNumber.BITS_8 && ((BitEntity) binField).getBitWidth() != mappedBitNumber) {
            throw new JBBPMapperException("Can't map value to a mapping field for different field bit width [" + mappedBitNumber + "!=" + ((BitEntity) binField).getBitWidth().getBitNumber() + ']', null, mappingClass, mappingField, null);
          }

          if (mappingField.getType().isArray()) {
            if (binField instanceof JBBPAbstractArrayField) {
              if (binField instanceof JBBPFieldArrayStruct) {
                // structure
                final JBBPFieldArrayStruct structArray = (JBBPFieldArrayStruct) binField;
                final Class<?> componentType = mappingField.getType().getComponentType();

                Object valueArray = getFieldValue(mappingClassInstance, mappingField);

                valueArray = valueArray == null ? Array.newInstance(componentType, structArray.size()) : valueArray;

                if (Array.getLength(valueArray) != structArray.size()) {
                  throw new JBBPMapperException("Can't map an array field for different expected size [" + Array.getLength(valueArray) + "!=" + structArray.size() + ']', binField, mappingClass, mappingField, null);
                }

                for (int i = 0; i < structArray.size(); i++) {
                  final Object curInstance = Array.get(valueArray, i);
                  if (curInstance == null) {
                    Array.set(valueArray, i, map(structArray.getElementAt(i), componentType, customFieldProcessor));
                  }
                  else {
                    Array.set(valueArray, i, map(structArray.getElementAt(i), curInstance, customFieldProcessor));
                  }
                }
                setFieldValue(mappingClassInstance, mappingField, binField, valueArray);
              }
              else {
                // primitive
                mapArrayField(mappingClassInstance, mappingField, (JBBPAbstractArrayField<?>) binField, mappedAnno.bitOrder() == JBBPBitOrder.MSB0);
              }
            }
            else {
              throw new JBBPMapperException("Can't map a non-array value to an array mapping field", binField, mappingClass, mappingField, null);
            }
          }
          else {
            if (binField instanceof JBBPNumericField) {
              mapNumericField(mappingClassInstance, mappingField, (JBBPNumericField) binField, mappedAnno.bitOrder() == JBBPBitOrder.MSB0);
            }
            else if (binField instanceof JBBPFieldStruct) {
              if (mappingField.getType().isPrimitive()) {
                throw new JBBPMapperException("Can't map a structure to a primitive mapping field", binField, mappingClass, mappingField, null);
              }
              else {
                final Object curValue = getFieldValue(mappingClassInstance, mappingField);
                if (curValue == null) {
                  setFieldValue(mappingClassInstance, mappingField, binField, map((JBBPFieldStruct) binField, mappingField.getType(), customFieldProcessor));
                }
                else {
                  setFieldValue(mappingClassInstance, mappingField, binField, map((JBBPFieldStruct) binField, curValue, customFieldProcessor));
                }
              }
            }
            else {
              boolean processed = false;
              if (mappingField.getType() == String.class && binField instanceof JBBPAbstractArrayField) {
                final String convertedValue = convertFieldValueToString((JBBPAbstractArrayField<?>) binField);
                if (convertedValue != null) {
                  setFieldValue(mappingClassInstance, mappingField, binField, convertedValue);
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
    return mappingClassInstance;
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
    }
    else if (field instanceof JBBPFieldArrayByte) {
      final JBBPFieldArrayByte array = (JBBPFieldArrayByte) field;
      result = new StringBuilder(array.size());
      for (final byte b : array.getArray()) {
        result.append((char) (b & 0xFF));
      }
    }
    else if (field instanceof JBBPFieldArrayUByte) {
      final JBBPFieldArrayUByte array = (JBBPFieldArrayUByte) field;
      result = new StringBuilder(array.size());
      for (final byte b : array.getArray()) {
        result.append((char) (b & 0xFF));
      }
    }
    else if (field instanceof JBBPFieldArrayShort) {
      final JBBPFieldArrayShort array = (JBBPFieldArrayShort) field;
      result = new StringBuilder(array.size());
      for (final short b : array.getArray()) {
        result.append((char) b);
      }
    }
    else if (field instanceof JBBPFieldArrayUShort) {
      final JBBPFieldArrayUShort array = (JBBPFieldArrayUShort) field;
      result = new StringBuilder(array.size());
      for (final short b : array.getArray()) {
        result.append((char) b);
      }
    }
    else {
      result = null;
    }
    return result == null ? null : result.toString();
  }

  /**
   * Set a value to a field of a class instance. Can't be used for static
   * fields!
   *
   * @param classInstance a class instance
   * @param classField a mapping class field which should be set by the value,
   * must not be null
   * @param binField a parsed bin field which value will be set, can be null
   * @param value a value to be set to the class field
   */
  private static void setFieldValue(final Object classInstance, final Field classField, final JBBPAbstractField binField, final Object value) {
    try {
      classField.set(classInstance, value);
    }
    catch (IllegalArgumentException ex) {
      throw new JBBPMapperException("Can't set value to a mapping field", binField, classInstance.getClass(), classField, ex);
    }
    catch (IllegalAccessException ex) {
      throw new JBBPMapperException("Can't get access to a mapping field", binField, classInstance.getClass(), classField, ex);
    }
  }

  /**
   * Get a value of a field from a class instance.
   *
   * @param classInstance a class instance object
   * @param classField a class field which value must be returned, must not be
   * null
   * @return the field value for the class instance
   */
  private static Object getFieldValue(final Object classInstance, final Field classField) {
    try {
      return classField.get(classInstance);
    }
    catch (IllegalArgumentException ex) {
      throw new JBBPMapperException("Can't set get value from a mapping field", null, classInstance.getClass(), classField, ex);
    }
    catch (IllegalAccessException ex) {
      throw new JBBPMapperException("Can't get access to a mapping field", null, classInstance.getClass(), classField, ex);
    }
  }

  /**
   * Map a parsed primitive numeric field to a primitive field in a mapping
   * class.
   *
   * @param mappingClassInstance the mapping class instance, must not be null
   * @param mappingField a mapping field to set the value, must not be null
   * @param numericField a parsed numeric field which value should be used, must
   * not be null
   * @param invertBitOrder flag shows that the parsed numeric field value must
   * be reversed in its bit before setting
   */
  private static void mapNumericField(final Object mappingClassInstance, final Field mappingField, final JBBPNumericField numericField, final boolean invertBitOrder) {
    final Class<?> fieldClass = mappingField.getType();
    try {
      if (fieldClass == byte.class) {
        mappingField.setByte(mappingClassInstance, (byte) (invertBitOrder ? numericField.getAsInvertedBitOrder() : numericField.getAsInt()));
      }
      else if (fieldClass == boolean.class) {
        mappingField.setBoolean(mappingClassInstance, numericField.getAsBool());
      }
      else if (fieldClass == char.class) {
        mappingField.setChar(mappingClassInstance, (char) (invertBitOrder ? numericField.getAsInvertedBitOrder() : numericField.getAsInt()));
      }
      else if (fieldClass == short.class) {
        mappingField.setShort(mappingClassInstance, (short) (invertBitOrder ? numericField.getAsInvertedBitOrder() : numericField.getAsInt()));
      }
      else if (fieldClass == int.class) {
        mappingField.setInt(mappingClassInstance, (int) (invertBitOrder ? numericField.getAsInvertedBitOrder() : numericField.getAsInt()));
      }
      else if (fieldClass == long.class) {
        mappingField.setLong(mappingClassInstance, (invertBitOrder ? numericField.getAsInvertedBitOrder() : numericField.getAsLong()));
      }
      else if (fieldClass == float.class) {
        mappingField.setFloat(mappingClassInstance, Float.intBitsToFloat(invertBitOrder ? (int) numericField.getAsInvertedBitOrder() : numericField.getAsInt()));
      }
      else if (fieldClass == double.class) {
        mappingField.setDouble(mappingClassInstance, Double.longBitsToDouble(invertBitOrder ? numericField.getAsInvertedBitOrder() : numericField.getAsLong()));
      }
      else {
        throw new JBBPMapperException("Unsupported mapping class field type to be mapped for binary parsed data", (JBBPAbstractField) numericField, mappingClassInstance.getClass(), mappingField, null);
      }
    }
    catch (IllegalAccessException ex) {
      throw new JBBPMapperException("Can't get access to a mapping field", (JBBPAbstractField) numericField, mappingClassInstance.getClass(), mappingField, ex);
    }
    catch (IllegalArgumentException ex) {
      throw new JBBPMapperException("Can't set argument to a mapping field", (JBBPAbstractField) numericField, mappingClassInstance.getClass(), mappingField, ex);
    }
  }

  /**
   * Map a parsed array to an array field in mapping class.
   *
   * @param mappingClassInstance a mapping class instance, must not be null
   * @param mappingField a field in the mapping class to be set, must not be
   * null
   * @param arrayField a binary parsed array field, must not be null
   * @param invertBitOrder flag shows that values of an array must be bit
   * reversed before set
   */
  private static void mapArrayField(final Object mappingClassInstance, final Field mappingField, final JBBPAbstractArrayField<?> arrayField, final boolean invertBitOrder) {
    try {
      if (arrayField instanceof JBBPFieldArrayUShort && mappingField.getType().getComponentType() == char.class) {
        final short[] shortarray = (short[]) arrayField.getValueArrayAsObject(invertBitOrder);
        final char[] chararray = new char[shortarray.length];
        for (int i = 0; i < shortarray.length; i++) {
          chararray[i] = (char) shortarray[i];
        }
        mappingField.set(mappingClassInstance, chararray);
      }
      else {
        mappingField.set(mappingClassInstance, arrayField.getValueArrayAsObject(invertBitOrder));
      }
    }
    catch (IllegalAccessException ex) {
      throw new JBBPMapperException("Can't get access to a mapping field", arrayField, mappingClassInstance.getClass(), mappingField, ex);
    }
    catch (IllegalArgumentException ex) {
      throw new JBBPMapperException("Can't set argument to a mapping field", arrayField, mappingClassInstance.getClass(), mappingField, ex);
    }
  }

  /**
   * Makes an instance of a class without call of its constructor, just allocate
   * memory
   *
   * @param <T> a class which instance is needed
   * @param root the structure to be mapped, it is needed as info for exception
   * @param klazz the class which instance is needed
   * @return an instance of the class without called constructor
   * @throws JBBPMapperException it will be thrown if it is impossible to make
   * an instance
   */
  private static <T> T allocateMemoryForClass(final JBBPFieldStruct root, final Class<T> klazz) {
    try {
      return klazzInstantiator.makeClassInstance(klazz);
    }
    catch (InstantiationException ex) {
      throw new JBBPMapperException("Can't make an instance of a class", root, klazz, null, ex);
    }
  }
}
