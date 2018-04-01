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

package com.igormaznitsa.jbbp.io;

import com.igormaznitsa.jbbp.exceptions.JBBPException;
import com.igormaznitsa.jbbp.exceptions.JBBPIllegalArgumentException;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import com.igormaznitsa.jbbp.model.JBBPFieldInt;
import com.igormaznitsa.jbbp.model.JBBPFieldLong;
import com.igormaznitsa.jbbp.model.JBBPFieldShort;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import com.igormaznitsa.jbbp.utils.ReflectUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract class to collect, order and process all fields in a mapped class.
 * since 1.1
 */
public abstract class AbstractMappedClassFieldObserver {

  /**
   * Inside cache to keep outOrder of fields for classes for data output. It is
   * lazy initialized field.
   */
  private static volatile Map<Class<?>, Field[]> cachedClasses;

  /**
   * Inside auxiliary method to read object field value.
   *
   * @param obj   an object which field is read
   * @param field a field to be read
   * @return a value from the field of the object
   * @throws JBBPException if the field can't be read
   * @since 1.1
   */
  private static Object readFieldValue(final Object obj, final Field field) {
    try {
      return field.get(obj);
    } catch (Exception ex) {
      throw new JBBPException("Can't get falue from field [" + field + ']', ex);
    }
  }

  /**
   * Check that a field defined as an array.
   *
   * @param field a field which is checked
   * @throws IllegalArgumentException if the field is not an array
   */
  private static void assertFieldArray(final Field field) {
    if (!field.getType().isArray()) {
      throw new IllegalArgumentException("Detected non-array field marked to be written as an array [" + field + ']');
    }
  }

  /**
   * Process an object.
   *
   * @param obj                  an object which is an instance of a mapped class, must not be null
   * @param field                a field where the object has been found, it can be null for first call
   * @param customFieldProcessor a processor for custom fields, it can be null
   */
  protected void processObject(final Object obj, Field field, final Object customFieldProcessor) {
    JBBPUtils.assertNotNull(obj, "Object must not be null");

    Field[] orderedFields = null;

    final Map<Class<?>, Field[]> fieldz;
    if (cachedClasses == null) {
      fieldz = new HashMap<Class<?>, Field[]>();
      cachedClasses = fieldz;
    } else {
      fieldz = cachedClasses;
      synchronized (fieldz) {
        orderedFields = fieldz.get(obj.getClass());
      }
    }

    if (orderedFields == null) {
      // find out the outOrder of fields and fields which should be serialized
      final List<Class<?>> listOfClassHierarchy = new ArrayList<Class<?>>();
      final List<OrderedField> fields = new ArrayList<OrderedField>();

      Class<?> current = obj.getClass();
      while (current != java.lang.Object.class) {
        listOfClassHierarchy.add(current);
        current = current.getSuperclass();
      }

      for (int i = listOfClassHierarchy.size() - 1; i >= 0; i--) {
        final Class<?> clazzToProcess = listOfClassHierarchy.get(i);
        final Bin clazzAnno = clazzToProcess.getAnnotation(Bin.class);

        for (Field f : clazzToProcess.getDeclaredFields()) {
          f = ReflectUtils.makeAccessible(f);

          final int modifiers = f.getModifiers();
          if (Modifier.isTransient(modifiers) || Modifier.isStatic(modifiers) || f.getName().indexOf('$') >= 0) {
            continue;
          }

          Bin fieldAnno = f.getAnnotation(Bin.class);
          fieldAnno = fieldAnno == null ? clazzAnno : fieldAnno;
          if (fieldAnno == null) {
            continue;
          }

          fields.add(new OrderedField(fieldAnno.outOrder(), f));
        }
      }

      Collections.sort(fields);

      orderedFields = new Field[fields.size()];
      for (int i = 0; i < fields.size(); i++) {
        orderedFields[i] = fields.get(i).field;
      }

      synchronized (fieldz) {
        fieldz.put(obj.getClass(), orderedFields);
      }
    }

    field = ReflectUtils.makeAccessible(field);

    final Bin clazzAnno = obj.getClass().getAnnotation(Bin.class);
    final Bin fieldAnno = field == null ? null : field.getAnnotation(Bin.class);

    this.onStructStart(obj, field, clazzAnno == null ? fieldAnno : clazzAnno);

    for (final Field f : orderedFields) {
      Bin binAnno = f.getAnnotation(Bin.class);
      if (binAnno == null) {
        binAnno = f.getDeclaringClass().getAnnotation(Bin.class);
        if (binAnno == null) {
          throw new JBBPIllegalArgumentException("Can't find any Bin annotation to use for " + f + " field");
        }
      }

      if (binAnno.custom() && customFieldProcessor == null) {
        throw new JBBPIllegalArgumentException("The Class '" + obj.getClass().getName() + "' contains the field '" + f.getName() + "\' which is a custom one, you must provide a JBBPCustomFieldWriter instance to save the field.");
      }

      processObjectField(obj, f, binAnno, customFieldProcessor);
    }

    this.onStructEnd(obj, field, clazzAnno == null ? fieldAnno : clazzAnno);
  }

  /**
   * Inside auxiliary method to process a field of an object.
   *
   * @param obj                  the object which field under processing, must not be null
   * @param field                the field to be written, must not be null
   * @param annotation           the annotation to be used as data source about the field,
   *                             must not be null
   * @param customFieldProcessor an object which will be provided for processing
   *                             of custom fields, must not be null if object contains custom fields
   */
  protected void processObjectField(final Object obj, final Field field, final Bin annotation, final Object customFieldProcessor) {
    if (annotation.custom()) {
      this.onFieldCustom(obj, field, annotation, customFieldProcessor, readFieldValue(obj, field));
    } else {
      final Class<?> fieldType = field.getType();

      final BinType type;
      if (annotation.type() == BinType.UNDEFINED) {
        type = BinType.findCompatible(fieldType);
      } else {
        type = annotation.type();
      }

      final boolean reverseBits = annotation.bitOrder() == JBBPBitOrder.MSB0;

      switch (type) {
        case BIT: {
          final JBBPBitNumber bitNumber = annotation.outBitNumber();
          if (fieldType == boolean.class) {
            this.onFieldBits(obj, field, annotation, bitNumber, ((Boolean) readFieldValue(obj, field)) ? 0xFF : 0x00);
          } else {
            byte value = ((Number) readFieldValue(obj, field)).byteValue();
            if (reverseBits) {
              value = JBBPUtils.reverseBitsInByte(bitNumber, value);
            }
            this.onFieldBits(obj, field, annotation, bitNumber, value);
          }
        }
        break;
        case BOOL: {
          if (fieldType == boolean.class) {
            onFieldBool(obj, field, annotation, (Boolean) readFieldValue(obj, field));
          } else {
            onFieldBool(obj, field, annotation, ((Number) readFieldValue(obj, field)).longValue() != 0);
          }
        }
        break;
        case BYTE:
        case UBYTE: {
          byte value = ((Number) readFieldValue(obj, field)).byteValue();
          if (reverseBits) {
            value = JBBPUtils.reverseBitsInByte(value);
          }
          this.onFieldByte(obj, field, annotation, type == BinType.BYTE, value);
        }
        break;
        case SHORT:
        case USHORT: {
          short value;
          if (fieldType == char.class) {
            value = (short) ((Character) readFieldValue(obj, field)).charValue();
          } else {
            value = ((Number) readFieldValue(obj, field)).shortValue();
          }
          if (reverseBits) {
            value = (short) JBBPFieldShort.reverseBits(value);
          }
          this.onFieldShort(obj, field, annotation, type == BinType.SHORT, value);
        }
        break;
        case INT: {
          int value;
          value = ((Number) readFieldValue(obj, field)).intValue();
          if (reverseBits) {
            value = (int) JBBPFieldInt.reverseBits(value);
          }
          this.onFieldInt(obj, field, annotation, value);
        }
        break;
        case FLOAT: {
          float value;
          if (float.class == fieldType) {
            value = (Float) readFieldValue(obj, field);
          } else {
            value = ((Number) readFieldValue(obj, field)).floatValue();
          }
          if (reverseBits) {
            value = Float.intBitsToFloat((int) JBBPFieldInt.reverseBits(Float.floatToIntBits(value)));
          }
          this.onFieldFloat(obj, field, annotation, value);
        }
        break;
        case LONG: {
          long value = ((Number) readFieldValue(obj, field)).longValue();
          if (reverseBits) {
            value = JBBPFieldLong.reverseBits(value);
          }
          this.onFieldLong(obj, field, annotation, value);
        }
        break;
        case DOUBLE: {
          double value;
          if (float.class == fieldType) {
            value = (Float) readFieldValue(obj, field);
          } else if (double.class == fieldType) {
            value = (Double) readFieldValue(obj, field);
          } else {
            value = ((Number) readFieldValue(obj, field)).doubleValue();
          }

          if (reverseBits) {
            value = Double.longBitsToDouble(JBBPFieldLong.reverseBits(Double.doubleToLongBits(value)));
          }
          this.onFieldDouble(obj, field, annotation, value);
        }
        break;
        case STRUCT: {
          processObject(readFieldValue(obj, field), field, customFieldProcessor);
        }
        break;
        default: {
          final Object array = readFieldValue(obj, field);
          switch (type) {
            case BIT_ARRAY: {
              assertFieldArray(field);

              final int len = Array.getLength(array);
              this.onArrayStart(obj, field, annotation, len);

              final JBBPBitNumber bitNumber = annotation.outBitNumber();

              if (fieldType.getComponentType() == boolean.class) {
                for (int i = 0; i < len; i++) {
                  this.onFieldBits(obj, field, annotation, bitNumber, (Boolean) Array.get(array, i) ? 0xFF : 0x00);
                }
              } else {
                for (int i = 0; i < len; i++) {
                  byte value = ((Number) Array.get(array, i)).byteValue();
                  if (reverseBits) {
                    value = JBBPUtils.reverseBitsInByte(bitNumber, value);
                  }
                  this.onFieldBits(obj, field, annotation, bitNumber, value);
                }
              }

              this.onArrayEnd(obj, field, annotation);
            }
            break;
            case BOOL_ARRAY: {
              assertFieldArray(field);

              final int len = Array.getLength(array);
              this.onArrayStart(obj, field, annotation, len);

              for (int i = 0; i < len; i++) {
                this.onFieldBool(obj, field, annotation, (Boolean) Array.get(array, i));
              }

              this.onArrayEnd(obj, field, annotation);
            }
            break;
            case UBYTE_ARRAY:
            case BYTE_ARRAY: {
              final boolean signed = type == BinType.BYTE_ARRAY;

              if (fieldType == String.class) {
                final String strValue = (String) readFieldValue(obj, field);
                this.onArrayStart(obj, field, annotation, strValue.length());

                for (int i = 0; i < strValue.length(); i++) {
                  byte value = (byte) strValue.charAt(i);
                  if (reverseBits) {
                    value = JBBPUtils.reverseBitsInByte(value);
                  }
                  this.onFieldByte(obj, field, annotation, signed, value);
                }
              } else {
                assertFieldArray(field);
                final int len = Array.getLength(array);
                this.onArrayStart(obj, field, annotation, len);
                for (int i = 0; i < len; i++) {
                  byte value = ((Number) Array.get(array, i)).byteValue();
                  if (reverseBits) {
                    value = JBBPUtils.reverseBitsInByte(value);
                  }
                  this.onFieldByte(obj, field, annotation, signed, value);
                }
              }

              this.onArrayEnd(obj, field, annotation);
            }
            break;
            case SHORT_ARRAY:
            case USHORT_ARRAY: {
              final boolean signed = type == BinType.SHORT_ARRAY;

              if (fieldType == String.class) {
                final String str = (String) readFieldValue(obj, field);
                this.onArrayStart(obj, field, annotation, str.length());

                for (int i = 0; i < str.length(); i++) {
                  short value = (short) str.charAt(i);
                  if (reverseBits) {
                    value = (short) JBBPFieldShort.reverseBits(value);
                  }
                  this.onFieldShort(obj, field, annotation, signed, value);
                }
              } else {
                assertFieldArray(field);

                final int len = Array.getLength(array);
                this.onArrayStart(obj, field, annotation, len);

                if (fieldType.getComponentType() == char.class) {
                  for (int i = 0; i < len; i++) {
                    short value = (short) ((Character) Array.get(array, i)).charValue();
                    if (reverseBits) {
                      value = (short) JBBPFieldShort.reverseBits(value);
                    }
                    this.onFieldShort(obj, field, annotation, signed, value);
                  }
                } else {
                  for (int i = 0; i < len; i++) {
                    short value = ((Number) Array.get(array, i)).shortValue();
                    if (reverseBits) {
                      value = (short) JBBPFieldShort.reverseBits(value);
                    }
                    this.onFieldShort(obj, field, annotation, signed, value);
                  }
                }

                this.onArrayEnd(obj, field, annotation);
              }
            }
            break;
            case FLOAT_ARRAY: {
              assertFieldArray(field);
              final int len = Array.getLength(array);
              this.onArrayStart(obj, field, annotation, len);
              for (int i = 0; i < len; i++) {
                float value = Array.getFloat(array, i);
                if (reverseBits) {
                  value = Float.intBitsToFloat((int) JBBPFieldInt.reverseBits(Float.floatToIntBits(value)));
                }
                this.onFieldFloat(obj, field, annotation, value);
              }
              this.onArrayEnd(obj, field, annotation);
            }
            break;
            case INT_ARRAY: {
              assertFieldArray(field);
              final int len = Array.getLength(array);
              this.onArrayStart(obj, field, annotation, len);
              for (int i = 0; i < len; i++) {
                int value = ((Number) Array.get(array, i)).intValue();
                if (reverseBits) {
                  value = (int) JBBPFieldInt.reverseBits(value);
                }
                this.onFieldInt(obj, field, annotation, value);
              }

              this.onArrayEnd(obj, field, annotation);
            }
            break;
            case LONG_ARRAY: {
              assertFieldArray(field);
              final int len = Array.getLength(array);
              this.onArrayStart(obj, field, annotation, len);
              for (int i = 0; i < len; i++) {
                long value = ((Number) Array.get(array, i)).longValue();
                if (reverseBits) {
                  value = JBBPFieldLong.reverseBits(value);
                }
                this.onFieldLong(obj, field, annotation, value);
              }
              this.onArrayEnd(obj, field, annotation);
            }
            break;
            case DOUBLE_ARRAY: {
              assertFieldArray(field);
              final int len = Array.getLength(array);
              this.onArrayStart(obj, field, annotation, len);
              for (int i = 0; i < len; i++) {
                double value = ((Number) Array.get(array, i)).doubleValue();
                if (reverseBits) {
                  value = Double.longBitsToDouble(JBBPFieldLong.reverseBits(Double.doubleToLongBits(value)));
                }
                this.onFieldDouble(obj, field, annotation, value);
              }
              this.onArrayEnd(obj, field, annotation);
            }
            break;
            case STRUCT_ARRAY: {
              assertFieldArray(field);
              final int len = Array.getLength(array);
              this.onArrayStart(obj, field, annotation, len);
              for (int i = 0; i < len; i++) {
                this.processObject(Array.get(array, i), field, customFieldProcessor);
              }
              this.onArrayEnd(obj, field, annotation);
            }
            break;
            default: {
              throw new Error("Unexpected situation for field type, contact developer [" + type + ']');
            }
          }
        }
        break;
      }
    }
  }

  /**
   * Notification about custom field.
   *
   * @param obj                  the object instance, must not be null
   * @param field                the custom field, must not be null
   * @param annotation           the annotation for the field, must not be null
   * @param customFieldProcessor processor for custom fields, must not be null
   * @param value                the value of the custom field
   */
  protected void onFieldCustom(final Object obj, final Field field, final Bin annotation, final Object customFieldProcessor, final Object value) {

  }

  /**
   * Notification about bit field.
   *
   * @param obj        the object instance, must not be null
   * @param field      the field, must not be null
   * @param annotation the annotation for field, must not be null
   * @param bitNumber  number of bits for the field, must not be null
   * @param value      the value of the field
   */
  protected void onFieldBits(final Object obj, final Field field, final Bin annotation, final JBBPBitNumber bitNumber, final int value) {

  }

  /**
   * Notification about boolean field.
   *
   * @param obj        the object instance, must not be null
   * @param field      the field, must not be null
   * @param annotation the annotation for field, must not be null
   * @param value      the value of the field
   */
  protected void onFieldBool(final Object obj, final Field field, final Bin annotation, final boolean value) {

  }

  /**
   * Notification about byte field.
   *
   * @param obj        the object instance, must not be null
   * @param field      the field, must not be null
   * @param annotation the annotation for field, must not be null
   * @param signed     flag shows that the field id signed
   * @param value      the value of the field
   */
  protected void onFieldByte(final Object obj, final Field field, final Bin annotation, final boolean signed, final int value) {

  }

  /**
   * Notification about short field.
   *
   * @param obj        the object instance, must not be null
   * @param field      the field, must not be null
   * @param annotation the annotation for field, must not be null
   * @param signed     flag shows that the field id signed
   * @param value      the value of the field
   */
  protected void onFieldShort(final Object obj, final Field field, final Bin annotation, final boolean signed, final int value) {

  }

  /**
   * Notification about integer field.
   *
   * @param obj        the object instance, must not be null
   * @param field      the field, must not be null
   * @param annotation the annotation for field, must not be null
   * @param value      the value of the field
   */
  protected void onFieldInt(final Object obj, final Field field, final Bin annotation, final int value) {

  }

  /**
   * Notification about float field.
   *
   * @param obj        the object instance, must not be null
   * @param field      the field, must not be null
   * @param annotation the annotation for field, must not be null
   * @param value      the value of the field
   * @since 1.4.0
   */
  protected void onFieldFloat(final Object obj, final Field field, final Bin annotation, final float value) {

  }

  /**
   * Notification about double field.
   *
   * @param obj        the object instance, must not be null
   * @param field      the field, must not be null
   * @param annotation the annotation for field, must not be null
   * @param value      the value of the field
   * @since 1.4.0
   */
  protected void onFieldDouble(final Object obj, final Field field, final Bin annotation, final double value) {

  }

  /**
   * Notification about long field.
   *
   * @param obj        the object instance, must not be null
   * @param field      the field, must not be null
   * @param annotation the annotation for field, must not be null
   * @param value      the value of the field
   */
  protected void onFieldLong(final Object obj, final Field field, final Bin annotation, final long value) {

  }

  /**
   * Notification of start of "structure" field.
   *
   * @param obj        the object instance, must not be null
   * @param field      the field, must not be null
   * @param annotation the annotation for field, must not be null
   */
  protected void onStructStart(final Object obj, final Field field, final Bin annotation) {

  }

  /**
   * Notification of end of "structure" field.
   *
   * @param obj        the object instance, must not be null
   * @param field      the field, must not be null
   * @param annotation the annotation for field, must not be null
   */
  protected void onStructEnd(final Object obj, final Field field, final Bin annotation) {

  }

  /**
   * Notification of start of "array" field.
   *
   * @param obj        the object instance, must not be null
   * @param field      the field, must not be null
   * @param annotation the annotation for field, must not be null
   * @param length     the length of the array
   */
  protected void onArrayStart(final Object obj, final Field field, final Bin annotation, final int length) {

  }

  /**
   * Notification of end of "array" field.
   *
   * @param obj        the object instance, must not be null
   * @param field      the field, must not be null
   * @param annotation the annotation for the field, must not be null
   */
  protected void onArrayEnd(final Object obj, final Field field, final Bin annotation) {

  }

  /**
   * Inside JBBPOut.Bin command creates cached list of fields of a saved class,
   * the method allows to reset the inside cache.
   */
  public void resetInsideClassCache() {
    final Map<Class<?>, Field[]> fieldz = cachedClasses;
    if (fieldz != null) {
      synchronized (fieldz) {
        fieldz.clear();
      }
    }
  }

  /**
   * An Auxiliary class to be used for class field ordering in save operations.
   */
  private static final class OrderedField implements Comparable<OrderedField> {

    final int order;
    final Field field;

    OrderedField(final int order, final Field field) {
      this.order = order;
      this.field = field;
    }

    @Override
    public boolean equals(final Object obj) {
      return obj != null && (obj == this || (obj instanceof OrderedField && this.field.equals(((OrderedField) obj).field)));
    }

    @Override
    public int hashCode() {
      return this.order;
    }

    @Override
    public int compareTo(final OrderedField o) {
      final int result;
      if (this.order == o.order) {
        result = this.field.getName().compareTo(o.field.getName());
      } else {
        result = this.order < o.order ? -1 : 1;
      }
      return result;
    }
  }

}
