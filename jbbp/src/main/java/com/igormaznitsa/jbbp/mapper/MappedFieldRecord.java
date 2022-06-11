package com.igormaznitsa.jbbp.mapper;

import static com.igormaznitsa.jbbp.mapper.JBBPMapper.MAKE_CLASS_INSTANCE_METHOD_NAME;

import com.igormaznitsa.jbbp.exceptions.JBBPMapperException;
import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.model.JBBPAbstractArrayField;
import com.igormaznitsa.jbbp.model.JBBPAbstractField;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayBit;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayByte;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayInt;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayLong;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayShort;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayStruct;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayUByte;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayUInt;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayUShort;
import com.igormaznitsa.jbbp.model.JBBPFieldInt;
import com.igormaznitsa.jbbp.model.JBBPFieldLong;
import com.igormaznitsa.jbbp.model.JBBPFieldString;
import com.igormaznitsa.jbbp.model.JBBPFieldStruct;
import com.igormaznitsa.jbbp.model.JBBPNumericField;
import com.igormaznitsa.jbbp.utils.Function;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class MappedFieldRecord implements Comparable<MappedFieldRecord> {
  private static final Function<Class<?>, Object> STATIC_MAKE_CLASS_INSTANCE_INSTANTIATOR =
      (Class<?> klazz) -> {
        Class<?> currentClass = klazz;
        Object result = null;
        boolean find;
        do {
          try {
            final Method method =
                currentClass.getMethod(MAKE_CLASS_INSTANCE_METHOD_NAME, Class.class);
            if (Modifier.isStatic(method.getModifiers())) {
              result = method.invoke(null, klazz);
            }
          } catch (IllegalAccessException ex) {
            throw new RuntimeException(String
                .format("Can't get access to static method %s(%ss) in %s",
                    MAKE_CLASS_INSTANCE_METHOD_NAME, klazz, currentClass), ex);
          } catch (InvocationTargetException ex) {
            throw new RuntimeException(String
                .format("Can't call static method %s(%s) in %s", MAKE_CLASS_INSTANCE_METHOD_NAME,
                    klazz, currentClass), ex);
          } catch (NoSuchMethodException ex) {
            // do nothing!
          }
          if (result == null) {
            if (currentClass.isLocalClass()) {
              currentClass = currentClass.getEnclosingClass();
              find = currentClass != null;
            } else {
              find = false;
            }
          } else {
            find = false;
          }
        } while (find);
        return result;
      };
  private static final Function<Class<?>, Object> DEFAULT_CONSTRUCTOR_INSTANTIATOR =
      (Class<?> aClass) -> {
        try {
          if (!aClass.isLocalClass() || Modifier.isStatic(aClass.getModifiers())) {
            return aClass.getConstructor().newInstance();
          } else {
            return null;
          }
        } catch (NoSuchMethodException ex) {
          return null;
        } catch (InvocationTargetException ex) {
          throw new RuntimeException(
              String.format("Error during default constructor call, class %s", aClass), ex);
        } catch (IllegalAccessException ex) {
          throw new RuntimeException(
              String.format("Can't get access to default constructor , class %s", aClass), ex);
        } catch (InstantiationException ex) {
          throw new RuntimeException(String.format("Can't make instance of class %s", aClass), ex);
        }
      };
  private static final FieldProcessor PROC_ARRAYS =
          (record, rootStructure, instance, customFieldProcessor, binField, flags, binFieldFilter, instantiators) -> {

            if (binField instanceof JBBPAbstractArrayField) {
              if (binField instanceof JBBPFieldArrayStruct) {
                // structure
                final JBBPFieldArrayStruct structArray = (JBBPFieldArrayStruct) binField;
                final Class<?> componentType = record.mappingField.getType().getComponentType();

                Object valueArray = getFieldValue(instance, record.getter, record.mappingField);

                valueArray = valueArray == null ? Array.newInstance(componentType, structArray.size()) :
                valueArray;

            if (Array.getLength(valueArray) != structArray.size()) {
              throw new JBBPMapperException(
                  "Can't map an array field for different expected size [" +
                      Array.getLength(valueArray) + "!=" + structArray.size() + ']', binField,
                  record.mappingClass, record.mappingField, null);
            }

            for (int i = 0; i < structArray.size(); i++) {
              final Object curInstance = Array.get(valueArray, i);
              if (curInstance == null) {
                Array.set(valueArray, i, JBBPMapper.map(structArray.getElementAt(i),
                        tryMakeInstance(componentType, binField, instance, record.mappingField,
                                instantiators), customFieldProcessor, 0, binFieldFilter, instantiators));
              } else {
                Array.set(valueArray, i,
                        JBBPMapper.map(structArray.getElementAt(i), curInstance, customFieldProcessor, 0, binFieldFilter));
              }
            }
            setFieldValue(instance, record.setter, record.mappingField, binField, valueArray);
          } else {
            // primitive
            mapArrayField(instance, record.setter, record.mappingField,
                (JBBPAbstractArrayField<?>) binField,
                record.binAnnotation.bitOrder() == JBBPBitOrder.MSB0);
          }
        } else {
          throw new JBBPMapperException("Can't map a non-array value to an array mapping field",
              binField, record.mappingClass, record.mappingField, null);
        }
      };
  private static final FieldProcessor PROC_NUM =
          (record, rootStructure, instance, customFieldProcessor, binField, flags, binFieldFilter, instantiators) -> {
            if (binField instanceof JBBPNumericField) {
              mapNumericField(instance, record.setter, record.mappingField, (JBBPNumericField) binField,
                      record.binAnnotation.bitOrder() == JBBPBitOrder.MSB0);
            } else if (binField instanceof JBBPFieldString) {
              if (record.mappingField.getType().isPrimitive()) {
                throw new JBBPMapperException("Can't map string to a primitive mapping field", binField,
                        record.mappingClass, record.mappingField, null);
              } else {
                setFieldValue(instance, record.setter, record.mappingField, binField,
                        ((JBBPFieldString) binField).getAsString());
          }
        } else if (binField instanceof JBBPFieldStruct) {
          if (record.mappingField.getType().isPrimitive()) {
            throw new JBBPMapperException("Can't map structure to a primitive mapping field",
                binField, record.mappingClass, record.mappingField, null);
          } else {
            final Object curValue = getFieldValue(instance, record.getter, record.mappingField);
            if (curValue == null) {
              if (record.instanceMaker == null) {
                setFieldValue(instance, record.setter, record.mappingField, binField, JBBPMapper
                        .map((JBBPFieldStruct) binField,
                                tryMakeInstance(record.mappingField.getType(), binField, instance,
                                        record.mappingField, instantiators), customFieldProcessor, 0, binFieldFilter));
              } else {
                try {
                  JBBPMapper.map((JBBPFieldStruct) binField, record.instanceMaker.invoke(instance));
                } catch (Exception ex) {
                  throw new JBBPMapperException(
                      "Can't map field which member generatet by instance", binField,
                      record.mappingClass, record.mappingField, ex);
                }
              }
            } else {
              setFieldValue(instance, record.setter, record.mappingField, binField,
                  JBBPMapper.map((JBBPFieldStruct) binField, curValue, customFieldProcessor));
            }
          }
        } else {
          boolean processed = false;
          if (record.mappingField.getType() == String.class &&
              binField instanceof JBBPAbstractArrayField) {
            final String convertedValue =
                convertFieldValueToString((JBBPAbstractArrayField<?>) binField);
            if (convertedValue != null) {
              setFieldValue(instance, record.setter, record.mappingField, binField, convertedValue);
              processed = true;
            }
          }
          if (!processed) {
            throw new JBBPMapperException("Can't map a field for its value incompatibility",
                binField, record.mappingClass, record.mappingField, null);
          }
        }
      };

  public final Field mappingField;
  public final Class<?> mappingClass;
  public final Method setter;
  public final Method getter;
  public final Method instanceMaker;
  public final Bin binAnnotation;
  public final boolean bitWideField;
  public final String fieldName;
  public final String fieldPath;
  public final JBBPBitNumber mappedBitNumber;
  public final BinType fieldType;
  public final FieldProcessor proc;

  MappedFieldRecord(final Field mappingField,
                    final Method instanceMaker,
                    final Method setter,
                    final Method getter,
                    final Class<?> mappingClass,
                    final Bin binAnnotation) {
    this.instanceMaker = instanceMaker;
    this.setter = setter;
    this.getter = getter;

    this.mappingField = mappingField;
    this.mappingClass = mappingClass;
    this.binAnnotation = binAnnotation;

    this.mappedBitNumber = binAnnotation.bitNumber();

    if (binAnnotation.type() == BinType.UNDEFINED) {
      BinType thetype = BinType.findCompatible(mappingField.getType());
      if (thetype == null) {
        throw new IllegalStateException("Can't find compatible mapped type for field");
      } else if (this.mappedBitNumber.getBitNumber() < 8 &&
              !(thetype == BinType.STRUCT || thetype == BinType.STRUCT_ARRAY)) {
        thetype = thetype.isArray() ? BinType.BIT_ARRAY : BinType.BIT;
      }
      this.fieldType = thetype;
    } else {
      this.fieldType = binAnnotation.type();
    }
    this.bitWideField = this.fieldType == BinType.BIT || fieldType == BinType.BIT_ARRAY;

    this.fieldName =
            binAnnotation.name().length() == 0 ? mappingField.getName() : binAnnotation.name();
    this.fieldPath = binAnnotation.path();

    if (this.mappingField.getType().isArray()) {
      this.proc = PROC_ARRAYS;
    } else {
      this.proc = PROC_NUM;
    }
  }

  /**
   * Map a parsed array to an array field in mapping class.
   *
   * @param mappingClassInstance a mapping class instance, must not be null
   * @param setter               detected setter for the field, can be null
   * @param mappingField         a field in the mapping class to be set, must not be
   *                             null
   * @param arrayField           a binary parsed array field, must not be null
   * @param invertBitOrder       flag shows that values of an array must be bit
   *                             reversed before set
   */
  private static void mapArrayField(final Object mappingClassInstance, final Method setter,
                                    final Field mappingField,
                                    final JBBPAbstractArrayField<?> arrayField,
                                    final boolean invertBitOrder) {
    try {
      final Object value;
      if (arrayField instanceof JBBPFieldArrayLong &&
          mappingField.getType().getComponentType() == double.class) {
        final long[] longArray = (long[]) arrayField.getValueArrayAsObject(invertBitOrder);
        final double[] doubleArray = new double[longArray.length];
        for (int i = 0; i < longArray.length; i++) {
          doubleArray[i] = Double.longBitsToDouble(longArray[i]);
        }
        value = doubleArray;
      } else if (arrayField instanceof JBBPFieldArrayUInt &&
          mappingField.getType().getComponentType() == double.class) {
        final long[] longArray = (long[]) arrayField.getValueArrayAsObject(invertBitOrder);
        final double[] doubleArray = new double[longArray.length];
        for (int i = 0; i < longArray.length; i++) {
          doubleArray[i] = Double.longBitsToDouble(longArray[i]);
        }
        value = doubleArray;
      } else if (arrayField instanceof JBBPFieldArrayInt &&
          mappingField.getType().getComponentType() == float.class) {
        final int[] intArray = (int[]) arrayField.getValueArrayAsObject(invertBitOrder);
        final float[] floatArray = new float[intArray.length];
        for (int i = 0; i < intArray.length; i++) {
          floatArray[i] = Float.intBitsToFloat(intArray[i]);
        }
        value = floatArray;
      } else if (arrayField instanceof JBBPFieldArrayUInt &&
          mappingField.getType().getComponentType() == float.class) {
        final long[] longArray = (long[]) arrayField.getValueArrayAsObject(invertBitOrder);
        final float[] floatArray = new float[longArray.length];
        for (int i = 0; i < longArray.length; i++) {
          floatArray[i] = Float.intBitsToFloat((int) longArray[i]);
        }
        value = floatArray;
      } else if (arrayField instanceof JBBPFieldArrayUInt &&
          mappingField.getType().getComponentType() == int.class) {
        final long[] longArray = (long[]) arrayField.getValueArrayAsObject(invertBitOrder);
        final int[] intArray = new int[longArray.length];
        for (int i = 0; i < longArray.length; i++) {
          intArray[i] = (int) longArray[i];
        }
        value = intArray;
      } else if (arrayField instanceof JBBPFieldArrayUShort &&
          mappingField.getType().getComponentType() == char.class) {
        final short[] shortarray = (short[]) arrayField.getValueArrayAsObject(invertBitOrder);
        final char[] chararray = new char[shortarray.length];
        for (int i = 0; i < shortarray.length; i++) {
          chararray[i] = (char) shortarray[i];
        }
        value = chararray;
      } else {
        value = arrayField.getValueArrayAsObject(invertBitOrder);
      }
      if (setter == null) {
        mappingField.set(mappingClassInstance, value);
      } else {
        setter.invoke(mappingClassInstance, value);
      }
    } catch (IllegalAccessException ex) {
      throw new JBBPMapperException("Can't get access to a mapping field", arrayField,
              mappingClassInstance.getClass(), mappingField, ex);
    } catch (IllegalArgumentException ex) {
      throw new JBBPMapperException("Can't set argument to a mapping field", arrayField,
              mappingClassInstance.getClass(), mappingField, ex);
    } catch (InvocationTargetException ex) {
      throw new JBBPMapperException("Can't set argument to field through setter", arrayField,
              mappingClassInstance.getClass(), mappingField, ex);
    }
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
   * Map a parsed primitive numeric field to a primitive field in a mapping
   * class.
   *
   * @param mappingClassInstance the mapping class instance, must not be null
   * @param setter               detected setter for field, can be null
   * @param mappingField         a mapping field to set the value, must not be null
   * @param numericField         a parsed numeric field which value should be used, must
   *                             not be null
   * @param invertBitOrder       flag shows that the parsed numeric field value must
   *                             be reversed in its bit before setting
   */
  private static void mapNumericField(final Object mappingClassInstance, final Method setter,
                                      final Field mappingField, final JBBPNumericField numericField,
                                      final boolean invertBitOrder) {
    final Class<?> fieldClass = mappingField.getType();
    try {
      if (fieldClass == byte.class) {
        final byte value = (byte) (invertBitOrder ? numericField.getAsInvertedBitOrder() :
                numericField.getAsInt());
        if (setter == null) {
          mappingField.setByte(mappingClassInstance, value);
        } else {
          setter.invoke(mappingClassInstance, value);
        }
      } else if (fieldClass == boolean.class) {
        if (setter == null) {
          mappingField.setBoolean(mappingClassInstance, numericField.getAsBool());
        } else {
          setter.invoke(mappingClassInstance, numericField.getAsBool());
        }
      } else if (fieldClass == char.class) {
        final char value = (char) (invertBitOrder ? numericField.getAsInvertedBitOrder() :
                numericField.getAsInt());
        if (setter == null) {
          mappingField.setChar(mappingClassInstance, value);
        } else {
          setter.invoke(mappingClassInstance, value);
        }
      } else if (fieldClass == short.class) {
        final short value = (short) (invertBitOrder ? numericField.getAsInvertedBitOrder() :
                numericField.getAsInt());
        if (setter == null) {
          mappingField.setShort(mappingClassInstance, value);
        } else {
          setter.invoke(mappingClassInstance, value);
        }
      } else if (fieldClass == int.class) {
        final int value =
                (int) (invertBitOrder ? numericField.getAsInvertedBitOrder() : numericField.getAsInt());
        if (setter == null) {
          mappingField.setInt(mappingClassInstance, value);
        } else {
          setter.invoke(mappingClassInstance, value);
        }
      } else if (fieldClass == long.class) {
        final long value =
                (invertBitOrder ? numericField.getAsInvertedBitOrder() : numericField.getAsLong());
        if (setter == null) {
          mappingField.setLong(mappingClassInstance, value);
        } else {
          setter.invoke(mappingClassInstance, value);
        }
      } else if (fieldClass == float.class) {
        final float value;
        if (numericField instanceof JBBPFieldInt) {
          value =
                  invertBitOrder ? Float.intBitsToFloat((int) numericField.getAsInvertedBitOrder()) :
                          Float.intBitsToFloat(numericField.getAsInt());
        } else {
          value =
                  invertBitOrder ? Float.intBitsToFloat((int) numericField.getAsInvertedBitOrder()) :
                          numericField.getAsFloat();
        }
        if (setter == null) {
          mappingField.setFloat(mappingClassInstance, value);
        } else {
          setter.invoke(mappingClassInstance, value);
        }
      } else if (fieldClass == double.class) {
        final double value;
        if (numericField instanceof JBBPFieldLong) {
          value = invertBitOrder ? Double.longBitsToDouble(numericField.getAsInvertedBitOrder()) :
                  Double.longBitsToDouble(numericField.getAsLong());
        } else {
          value = invertBitOrder ? Double.longBitsToDouble(numericField.getAsInvertedBitOrder()) :
                  numericField.getAsDouble();
        }
        if (setter == null) {
          mappingField.setDouble(mappingClassInstance, value);
        } else {
          setter.invoke(mappingClassInstance, value);
        }
      } else {
        throw new JBBPMapperException(
                "Unsupported mapping class field type to be mapped for binary parsed data",
                (JBBPAbstractField) numericField, mappingClassInstance.getClass(), mappingField, null);
      }
    } catch (IllegalAccessException ex) {
      throw new JBBPMapperException("Can't get access to a mapping field",
              (JBBPAbstractField) numericField, mappingClassInstance.getClass(), mappingField, ex);
    } catch (IllegalArgumentException ex) {
      throw new JBBPMapperException("Can't set argument to a mapping field",
              (JBBPAbstractField) numericField, mappingClassInstance.getClass(), mappingField, ex);
    } catch (InvocationTargetException ex) {
      throw new JBBPMapperException("Can't set argument to a mapping field through setter",
              (JBBPAbstractField) numericField, mappingClassInstance.getClass(), mappingField, ex);
    }
  }

  /**
   * Get a value of a field from a class instance.
   *
   * @param classInstance a class instance object
   * @param getter        method to get field value, can be null
   * @param classField    a class field which value must be returned, must not be
   *                      null
   * @return the field value for the class instance
   */
  private static Object getFieldValue(final Object classInstance, final Method getter,
                                      final Field classField) {
    try {
      if (getter == null) {
        return classField.get(classInstance);
      } else {
        return getter.invoke(classInstance);
      }
    } catch (IllegalArgumentException ex) {
      throw new JBBPMapperException("Can't set get value from a mapping field", null,
              classInstance.getClass(), classField, ex);
    } catch (IllegalAccessException ex) {
      throw new JBBPMapperException("Can't get access to a mapping field", null,
              classInstance.getClass(), classField, ex);
    } catch (InvocationTargetException ex) {
      throw new JBBPMapperException("Can't get field value through getter", null,
              classInstance.getClass(), classField, ex);
    }
  }

  /**
   * Set a value to a field of a class instance. Can't be used for static
   * fields!
   *
   * @param classInstance a class instance
   * @param setter        setter for the field, can be null
   * @param classField    a mapping class field which should be set by the value,
   *                      must not be null
   * @param binField      a parsed bin field which value will be set, can be null
   * @param value         a value to be set to the class field
   */
  static void setFieldValue(final Object classInstance, final Method setter, final Field classField,
                            final JBBPAbstractField binField, final Object value) {
    try {
      if (setter == null) {
        classField.set(classInstance, value);
      } else {
        setter.invoke(classInstance, value);
      }
    } catch (IllegalArgumentException ex) {
      throw new JBBPMapperException("Can't set value to a mapping field", binField,
              classInstance.getClass(), classField, ex);
    } catch (IllegalAccessException ex) {
      throw new JBBPMapperException("Can't get access to a mapping field", binField,
              classInstance.getClass(), classField, ex);
    } catch (InvocationTargetException ex) {
      throw new JBBPMapperException("Can't set field value through setter", binField,
              classInstance.getClass(), classField, ex);
    }
  }

  @SuppressWarnings("TryWithIdenticalCatches")
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
        final Method method =
                mappingObject.getClass().getMethod(MAKE_CLASS_INSTANCE_METHOD_NAME, Class.class);
        if (!Modifier.isStatic(method.getModifiers())) {
          result = type.cast(
                  mappingObject.getClass().getMethod(MAKE_CLASS_INSTANCE_METHOD_NAME, Class.class)
                          .invoke(mappingObject, type));
        }
      } catch (NoSuchMethodException ex) {
        // do nothing
      } catch (IllegalAccessException ex) {
        // WARNING! Don't replace by multicatch for Android compatibility!
        detectedException = ex;
      } catch (InvocationTargetException ex) {
        detectedException = ex;
      }

      if (detectedException != null) {
        throw new RuntimeException(String
                .format("Error during %s(%s) call", MAKE_CLASS_INSTANCE_METHOD_NAME,
                        mappingObject.getClass()), detectedException);
      }

      if (result == null) {
        result = type.cast(STATIC_MAKE_CLASS_INSTANCE_INSTANTIATOR.apply(type));
        if (result == null) {
          result = type.cast(DEFAULT_CONSTRUCTOR_INSTANTIATOR.apply(type));
        }
      }

      if (result == null) {
        throw new JBBPMapperException(String.format("Can't create instance of %s", type), binField,
                mappingObject.getClass(), mappingField, null);
      }
    }
    return result;
  }

  @Override
  public int compareTo(final MappedFieldRecord o) {
    final int thisOrder = this.binAnnotation.order();
    final int thatOrder = o.binAnnotation.order();

    final int result;
    if (thisOrder == thatOrder) {
      result = this.mappingField.getName().compareTo(o.mappingField.getName());
    } else {
      result = thisOrder < thatOrder ? -1 : 1;
    }
    return result;
  }

  interface FieldProcessor {
    @SuppressWarnings("unchecked")
    void apply(
            MappedFieldRecord record,
            JBBPFieldStruct rootStructure,
            Object instance,
            JBBPMapperCustomFieldProcessor customFieldProcessor,
            JBBPAbstractField binField,
            int flags,
            BinFieldFilter binFieldFilter,
            Function<Class<?>, Object>... instantiators
    );
  }
}
