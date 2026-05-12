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
                    JBBPMapper.map(structArray.getElementAt(i), curInstance, customFieldProcessor,
                        0, binFieldFilter));
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
          if (isPrimitiveOrWrapperNumericField(record.mappingField.getType())) {
            throw new JBBPMapperException("Can't map string to a primitive mapping field", binField,
                record.mappingClass, record.mappingField, null);
          } else {
            setFieldValue(instance, record.setter, record.mappingField, binField,
                ((JBBPFieldString) binField).getAsString());
          }
        } else if (binField instanceof JBBPFieldStruct) {
          if (isPrimitiveOrWrapperNumericField(record.mappingField.getType())) {
            throw new JBBPMapperException("Can't map structure to a primitive mapping field",
                binField, record.mappingClass, record.mappingField, null);
          } else {
            final Object curValue = getFieldValue(instance, record.getter, record.mappingField);
            if (curValue == null) {
              if (record.instanceMaker == null) {
                setFieldValue(instance, record.setter, record.mappingField, binField, JBBPMapper
                    .map((JBBPFieldStruct) binField,
                        tryMakeInstance(record.mappingField.getType(), binField, instance,
                            record.mappingField, instantiators), customFieldProcessor, 0,
                        binFieldFilter));
              } else {
                try {
                  JBBPMapper.map((JBBPFieldStruct) binField, record.instanceMaker.invoke(instance));
                } catch (Exception ex) {
                  throw new JBBPMapperException(
                      "Can't map field which member generated by instance", binField,
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
      BinType compatibleBinType = BinType.findCompatible(mappingField.getType());
      if (compatibleBinType == null) {
        throw new IllegalStateException("Can't find compatible mapped type for field");
      } else if (this.mappedBitNumber.getBitNumber() < 8 &&
          !(compatibleBinType == BinType.STRUCT || compatibleBinType == BinType.STRUCT_ARRAY)) {
        compatibleBinType = compatibleBinType.isArray() ? BinType.BIT_ARRAY : BinType.BIT;
      }
      this.fieldType = compatibleBinType;
    } else {
      this.fieldType = binAnnotation.type();
    }
    this.bitWideField = this.fieldType == BinType.BIT || fieldType == BinType.BIT_ARRAY;

    this.fieldName =
        binAnnotation.name().isEmpty() ? mappingField.getName() : binAnnotation.name();
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
      final Class<?> componentType = mappingField.getType().getComponentType();
      final Class<?> wrapperPrim = wrapperToPrimitive(componentType);
      final Class<?> logicalComponent = wrapperPrim != null ? wrapperPrim : componentType;
      final Object value;
      if (!logicalComponent.isPrimitive()) {
        value = arrayField.getValueArrayAsObject(invertBitOrder);
      } else {
        final Object ieeeMapped = mapPrimitiveArrayAsIeeeBits(logicalComponent, arrayField,
            invertBitOrder);
        final Object primitiveArray;
        if (ieeeMapped != null) {
          primitiveArray = ieeeMapped;
        } else {
          final Object rawArray = arrayField.getValueArrayAsObject(invertBitOrder);
          final Class<?> rawComponentType = rawArray.getClass().getComponentType();
          if (rawComponentType == logicalComponent) {
            primitiveArray = rawArray;
          } else if (arrayField instanceof JBBPFieldArrayUByte && rawComponentType == byte.class) {
            primitiveArray = widenUnsignedByteArrayToPrimitiveComponent((byte[]) rawArray,
                logicalComponent);
          } else {
            primitiveArray = coercePrimitiveArray(rawArray, rawComponentType, logicalComponent);
          }
        }
        value = wrapperPrim != null ? boxWrapperComponentArray(componentType, primitiveArray)
            : primitiveArray;
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
   * Same IEEE-754 bit reinterpretation as {@link #mapNumericField} for {@code int}/{@code long}
   * fields mapped to {@code float}/{@code double}.
   *
   * @return mapped array, or {@code null} when generic primitive coercion should be used
   */
  private static Object mapPrimitiveArrayAsIeeeBits(final Class<?> componentType,
                                                    final JBBPAbstractArrayField<?> arrayField,
                                                    final boolean invertBitOrder) {
    if (arrayField instanceof JBBPFieldArrayLong && componentType == double.class) {
      final long[] longArray = (long[]) arrayField.getValueArrayAsObject(invertBitOrder);
      final double[] result = new double[longArray.length];
      for (int i = 0; i < longArray.length; i++) {
        result[i] = Double.longBitsToDouble(longArray[i]);
      }
      return result;
    }
    if (arrayField instanceof JBBPFieldArrayUInt && componentType == double.class) {
      final long[] longArray = (long[]) arrayField.getValueArrayAsObject(invertBitOrder);
      final double[] result = new double[longArray.length];
      for (int i = 0; i < longArray.length; i++) {
        result[i] = Double.longBitsToDouble(longArray[i]);
      }
      return result;
    }
    if (arrayField instanceof JBBPFieldArrayInt && componentType == float.class) {
      final int[] intArray = (int[]) arrayField.getValueArrayAsObject(invertBitOrder);
      final float[] result = new float[intArray.length];
      for (int i = 0; i < intArray.length; i++) {
        result[i] = Float.intBitsToFloat(intArray[i]);
      }
      return result;
    }
    if (arrayField instanceof JBBPFieldArrayUInt && componentType == float.class) {
      final long[] longArray = (long[]) arrayField.getValueArrayAsObject(invertBitOrder);
      final float[] result = new float[longArray.length];
      for (int i = 0; i < longArray.length; i++) {
        result[i] = Float.intBitsToFloat((int) longArray[i]);
      }
      return result;
    }
    return null;
  }

  /**
   * Widen each unsigned byte (0..255) to {@code dstCt}. Used for {@link JBBPFieldArrayUByte} only;
   * signed {@link JBBPFieldArrayByte} still uses {@link #coercePrimitiveArray} for compatibility.
   */
  private static Object widenUnsignedByteArrayToPrimitiveComponent(final byte[] raw,
                                                                   final Class<?> dstCt) {
    final int n = raw.length;
    if (dstCt == boolean.class) {
      final boolean[] out = new boolean[n];
      for (int i = 0; i < n; i++) {
        out[i] = (raw[i] & 0xFF) != 0;
      }
      return out;
    }
    if (dstCt == char.class) {
      final char[] out = new char[n];
      for (int i = 0; i < n; i++) {
        out[i] = (char) (raw[i] & 0xFF);
      }
      return out;
    }
    if (dstCt == short.class) {
      final short[] out = new short[n];
      for (int i = 0; i < n; i++) {
        out[i] = (short) (raw[i] & 0xFF);
      }
      return out;
    }
    if (dstCt == int.class) {
      final int[] out = new int[n];
      for (int i = 0; i < n; i++) {
        out[i] = raw[i] & 0xFF;
      }
      return out;
    }
    if (dstCt == long.class) {
      final long[] out = new long[n];
      for (int i = 0; i < n; i++) {
        out[i] = raw[i] & 0xFFL;
      }
      return out;
    }
    if (dstCt == float.class) {
      final float[] out = new float[n];
      for (int i = 0; i < n; i++) {
        out[i] = raw[i] & 0xFF;
      }
      return out;
    }
    if (dstCt == double.class) {
      final double[] out = new double[n];
      for (int i = 0; i < n; i++) {
        out[i] = raw[i] & 0xFF;
      }
      return out;
    }
    throw new IllegalStateException("Unsupported unsigned byte widen target: " + dstCt);
  }

  /**
   * Cast each element from {@code srcCt} to {@code dstCt} using Java primitive conversion rules.
   */
  private static Object coercePrimitiveArray(final Object sourceArray, final Class<?> srcCt,
                                             final Class<?> dstCt) {
    final int length = Array.getLength(sourceArray);
    final Object destArray = Array.newInstance(dstCt, length);
    for (int i = 0; i < length; i++) {
      coercePrimitiveElement(sourceArray, srcCt, i, destArray, dstCt, i);
    }
    return destArray;
  }

  private static void coercePrimitiveElement(final Object src, final Class<?> srcCt, final int si,
                                             final Object dst, final Class<?> dstCt, final int di) {
    if (dstCt == boolean.class) {
      Array.setBoolean(dst, di, readAsBool(src, srcCt, si));
      return;
    }
    if (srcCt == boolean.class) {
      putNumericFromBool(dst, dstCt, di, Array.getBoolean(src, si));
      return;
    }
    final boolean srcFp = srcCt == float.class || srcCt == double.class;
    final boolean dstFp = dstCt == float.class || dstCt == double.class;
    if (srcFp || dstFp) {
      final double d = readAsDouble(src, srcCt, si);
      if (dstCt == float.class) {
        Array.setFloat(dst, di, (float) d);
      } else if (dstCt == double.class) {
        Array.setDouble(dst, di, d);
      } else {
        putIntegralFromDouble(dst, dstCt, di, d);
      }
      return;
    }
    putIntegralFromLong(dst, dstCt, di, readIntegralAsLong(src, srcCt, si));
  }

  private static boolean readAsBool(final Object src, final Class<?> srcCt, final int i) {
    if (srcCt == boolean.class) {
      return Array.getBoolean(src, i);
    }
    if (srcCt == float.class) {
      return Array.getFloat(src, i) != 0.0f;
    }
    if (srcCt == double.class) {
      return Array.getDouble(src, i) != 0.0d;
    }
    return readIntegralAsLong(src, srcCt, i) != 0L;
  }

  private static void putNumericFromBool(final Object dst, final Class<?> dstCt, final int di,
                                         final boolean value) {
    final long lv = value ? 1L : 0L;
    if (dstCt == float.class) {
      Array.setFloat(dst, di, (float) lv);
    } else if (dstCt == double.class) {
      Array.setDouble(dst, di, (double) lv);
    } else {
      putIntegralFromLong(dst, dstCt, di, lv);
    }
  }

  private static double readAsDouble(final Object src, final Class<?> srcCt, final int i) {
    if (srcCt == float.class) {
      return Array.getFloat(src, i);
    }
    if (srcCt == double.class) {
      return Array.getDouble(src, i);
    }
    return readIntegralAsLong(src, srcCt, i);
  }

  private static long readIntegralAsLong(final Object src, final Class<?> srcCt, final int i) {
    if (srcCt == byte.class) {
      return Array.getByte(src, i);
    }
    if (srcCt == short.class) {
      return Array.getShort(src, i);
    }
    if (srcCt == char.class) {
      return Array.getChar(src, i);
    }
    if (srcCt == int.class) {
      return Array.getInt(src, i);
    }
    if (srcCt == long.class) {
      return Array.getLong(src, i);
    }
    throw new IllegalStateException("Unsupported primitive component type: " + srcCt);
  }

  private static void putIntegralFromLong(final Object dst, final Class<?> dstCt, final int di,
                                          final long value) {
    if (dstCt == byte.class) {
      Array.setByte(dst, di, (byte) value);
    } else if (dstCt == short.class) {
      Array.setShort(dst, di, (short) value);
    } else if (dstCt == char.class) {
      Array.setChar(dst, di, (char) value);
    } else if (dstCt == int.class) {
      Array.setInt(dst, di, (int) value);
    } else if (dstCt == long.class) {
      Array.setLong(dst, di, value);
    } else {
      throw new IllegalStateException("Unsupported integral target component type: " + dstCt);
    }
  }

  private static void putIntegralFromDouble(final Object dst, final Class<?> dstCt, final int di,
                                            final double value) {
    if (dstCt == byte.class) {
      Array.setByte(dst, di, (byte) value);
    } else if (dstCt == short.class) {
      Array.setShort(dst, di, (short) value);
    } else if (dstCt == char.class) {
      Array.setChar(dst, di, (char) value);
    } else if (dstCt == int.class) {
      Array.setInt(dst, di, (int) value);
    } else if (dstCt == long.class) {
      Array.setLong(dst, di, (long) value);
    } else {
      throw new IllegalStateException("Unsupported integral target component type: " + dstCt);
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

  private static Class<?> wrapperToPrimitive(final Class<?> clazz) {
    if (clazz == Boolean.class) {
      return boolean.class;
    }
    if (clazz == Byte.class) {
      return byte.class;
    }
    if (clazz == Character.class) {
      return char.class;
    }
    if (clazz == Short.class) {
      return short.class;
    }
    if (clazz == Integer.class) {
      return int.class;
    }
    if (clazz == Long.class) {
      return long.class;
    }
    if (clazz == Float.class) {
      return float.class;
    }
    if (clazz == Double.class) {
      return double.class;
    }
    return null;
  }

  private static boolean isPrimitiveOrWrapperNumericField(final Class<?> clazz) {
    return clazz.isPrimitive() || wrapperToPrimitive(clazz) != null;
  }

  private static Object boxWrapperComponentArray(final Class<?> wrapperComponentType,
                                                 final Object primitiveArray) {
    final int length = Array.getLength(primitiveArray);
    final Object boxedArray = Array.newInstance(wrapperComponentType, length);
    for (int i = 0; i < length; i++) {
      Array.set(boxedArray, i, Array.get(primitiveArray, i));
    }
    return boxedArray;
  }

  private static void putMappedByte(final Object instance, final Field mappingField,
                                    final Method setter, final Class<?> declaredType,
                                    final byte value)
      throws IllegalAccessException, InvocationTargetException {
    if (setter != null) {
      setter.invoke(instance, value);
      return;
    }
    if (declaredType == byte.class) {
      mappingField.setByte(instance, value);
    } else {
      mappingField.set(instance, Byte.valueOf(value));
    }
  }

  private static void putMappedBoolean(final Object instance, final Field mappingField,
                                       final Method setter, final Class<?> declaredType,
                                       final boolean value)
      throws IllegalAccessException, InvocationTargetException {
    if (setter != null) {
      setter.invoke(instance, value);
      return;
    }
    if (declaredType == boolean.class) {
      mappingField.setBoolean(instance, value);
    } else {
      mappingField.set(instance, Boolean.valueOf(value));
    }
  }

  private static void putMappedChar(final Object instance, final Field mappingField,
                                    final Method setter, final Class<?> declaredType,
                                    final char value)
      throws IllegalAccessException, InvocationTargetException {
    if (setter != null) {
      setter.invoke(instance, value);
      return;
    }
    if (declaredType == char.class) {
      mappingField.setChar(instance, value);
    } else {
      mappingField.set(instance, Character.valueOf(value));
    }
  }

  private static void putMappedShort(final Object instance, final Field mappingField,
                                     final Method setter, final Class<?> declaredType,
                                     final short value)
      throws IllegalAccessException, InvocationTargetException {
    if (setter != null) {
      setter.invoke(instance, value);
      return;
    }
    if (declaredType == short.class) {
      mappingField.setShort(instance, value);
    } else {
      mappingField.set(instance, Short.valueOf(value));
    }
  }

  private static void putMappedInt(final Object instance, final Field mappingField,
                                   final Method setter, final Class<?> declaredType,
                                   final int value)
      throws IllegalAccessException, InvocationTargetException {
    if (setter != null) {
      setter.invoke(instance, value);
      return;
    }
    if (declaredType == int.class) {
      mappingField.setInt(instance, value);
    } else {
      mappingField.set(instance, Integer.valueOf(value));
    }
  }

  private static void putMappedLong(final Object instance, final Field mappingField,
                                    final Method setter, final Class<?> declaredType,
                                    final long value)
      throws IllegalAccessException, InvocationTargetException {
    if (setter != null) {
      setter.invoke(instance, value);
      return;
    }
    if (declaredType == long.class) {
      mappingField.setLong(instance, value);
    } else {
      mappingField.set(instance, Long.valueOf(value));
    }
  }

  private static void putMappedFloat(final Object instance, final Field mappingField,
                                     final Method setter, final Class<?> declaredType,
                                     final float value)
      throws IllegalAccessException, InvocationTargetException {
    if (setter != null) {
      setter.invoke(instance, value);
      return;
    }
    if (declaredType == float.class) {
      mappingField.setFloat(instance, value);
    } else {
      mappingField.set(instance, Float.valueOf(value));
    }
  }

  private static void putMappedDouble(final Object instance, final Field mappingField,
                                      final Method setter, final Class<?> declaredType,
                                      final double value)
      throws IllegalAccessException, InvocationTargetException {
    if (setter != null) {
      setter.invoke(instance, value);
      return;
    }
    if (declaredType == double.class) {
      mappingField.setDouble(instance, value);
    } else {
      mappingField.set(instance, Double.valueOf(value));
    }
  }

  /**
   * Map a parsed primitive numeric field to a primitive or boxed primitive field in a mapping
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
    final Class<?> declaredType = mappingField.getType();
    final Class<?> wrapperPrim = wrapperToPrimitive(declaredType);
    final Class<?> key = wrapperPrim != null ? wrapperPrim : declaredType;
    try {
      if (key == byte.class) {
        final byte value = (byte) (invertBitOrder ? numericField.getAsInvertedBitOrder() :
            numericField.getAsInt());
        putMappedByte(mappingClassInstance, mappingField, setter, declaredType, value);
      } else if (key == boolean.class) {
        putMappedBoolean(mappingClassInstance, mappingField, setter, declaredType,
            numericField.getAsBool());
      } else if (key == char.class) {
        final char value = (char) (invertBitOrder ? numericField.getAsInvertedBitOrder() :
            numericField.getAsInt());
        putMappedChar(mappingClassInstance, mappingField, setter, declaredType, value);
      } else if (key == short.class) {
        final short value = (short) (invertBitOrder ? numericField.getAsInvertedBitOrder() :
            numericField.getAsInt());
        putMappedShort(mappingClassInstance, mappingField, setter, declaredType, value);
      } else if (key == int.class) {
        final int value =
            (int) (invertBitOrder ? numericField.getAsInvertedBitOrder() : numericField.getAsInt());
        putMappedInt(mappingClassInstance, mappingField, setter, declaredType, value);
      } else if (key == long.class) {
        final long value =
            (invertBitOrder ? numericField.getAsInvertedBitOrder() : numericField.getAsLong());
        putMappedLong(mappingClassInstance, mappingField, setter, declaredType, value);
      } else if (key == float.class) {
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
        putMappedFloat(mappingClassInstance, mappingField, setter, declaredType, value);
      } else if (key == double.class) {
        final double value;
        if (numericField instanceof JBBPFieldLong) {
          value = invertBitOrder ? Double.longBitsToDouble(numericField.getAsInvertedBitOrder()) :
              Double.longBitsToDouble(numericField.getAsLong());
        } else {
          value = invertBitOrder ? Double.longBitsToDouble(numericField.getAsInvertedBitOrder()) :
              numericField.getAsDouble();
        }
        putMappedDouble(mappingClassInstance, mappingField, setter, declaredType, value);
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
        // WARNING! Don't replace by multi-catch for Android compatibility!
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

  public interface FieldProcessor {
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
