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
import com.igormaznitsa.jbbp.mapper.BinFieldFilter;
import com.igormaznitsa.jbbp.mapper.BinType;
import com.igormaznitsa.jbbp.mapper.JBBPMapper;
import com.igormaznitsa.jbbp.mapper.MappedFieldRecord;
import com.igormaznitsa.jbbp.model.JBBPFieldInt;
import com.igormaznitsa.jbbp.model.JBBPFieldLong;
import com.igormaznitsa.jbbp.model.JBBPFieldShort;
import com.igormaznitsa.jbbp.model.JBBPFieldString;
import com.igormaznitsa.jbbp.model.JBBPFieldUInt;
import com.igormaznitsa.jbbp.utils.BinAnnotationWrapper;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Abstract class to collect, order and process all fields in a mapped class.
 * since 1.1
 */
public abstract class AbstractMappedClassFieldObserver {

  /**
   * Inside auxiliary method to read object field value.
   *
   * @param obj    an object which field is read
   * @param record field record, must not be null
   * @return a value from the field of the object
   * @throws JBBPException if the field can't be read
   * @since 2.0
   */
  private static Object readFieldValue(final Object obj, final MappedFieldRecord record) {
    try {
      if (record.getter == null) {
        return record.mappingField.get(obj);
      } else {
        return record.getter.invoke(obj);
      }
    } catch (Exception ex) {
      throw new JBBPException("Can't get value from field [" + record + ']', ex);
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
      throw new IllegalArgumentException(
              "Detected non-array field marked to be written as an array [" + field + ']');
    }
  }

  /**
   * Process an object. It works only with classes and fields marked by Bin annotations. <b>It doesn't process classes and fields marked by DslBinCustom annotations.</b>
   *
   * @param obj                  an object which is an instance of a mapped class, must not be null
   * @param field                a field where the object has been found, it can be null for first call
   * @param binAnnotationWrapper wrapper to replace Bin annotation values for processing fields, can be null to be ignored
   * @param customFieldProcessor a processor for custom fields, it can be null
   * @see Bin
   * @since 2.0.2
   */
  protected void processObject(
          final Object obj,
          final Field field,
          final BinAnnotationWrapper binAnnotationWrapper,
          final Object customFieldProcessor
  ) {
    this.processObject(obj, field, binAnnotationWrapper, null, customFieldProcessor);
  }

  /**
   * Process an object. It works only with classes and fields marked by Bin annotations. <b>It doesn't process classes and fields marked by DslBinCustom annotations.</b>
   *
   * @param obj                  an object which is an instance of a mapped class, must not be null
   * @param field                a field where the object has been found, it can be null for first call
   * @param binAnnotationWrapper wrapper to replace Bin annotation values for processing fields, can be null to be ignored
   * @param binFieldFilter       filter for mapped fields, alows to exclude some of them, can be null
   * @param customFieldProcessor a processor for custom fields, it can be null
   * @see Bin
   * @since 2.0.4
   */
  protected void processObject(
          final Object obj,
          final Field field,
          final BinAnnotationWrapper binAnnotationWrapper,
          final BinFieldFilter binFieldFilter,
          final Object customFieldProcessor
  ) {
    JBBPUtils.assertNotNull(obj, "Object must not be null");

    final List<MappedFieldRecord> orderedFields = JBBPMapper.findAffectedFields(obj, binFieldFilter);

    final Bin clazzAnno = obj.getClass().getAnnotation(Bin.class);
    final Bin fieldAnno = field == null ? null : field.getAnnotation(Bin.class);

    final Bin binAnno = clazzAnno == null ? fieldAnno : clazzAnno;

    if (binFieldFilter == null || binFieldFilter.isAllowed(binAnno, field)) {
      this.onStructStart(obj, field, binAnno);

      for (final MappedFieldRecord rec : orderedFields) {
        final Bin annotation = binAnnotationWrapper == null ? rec.binAnnotation :
                binAnnotationWrapper.setWrapped(rec.binAnnotation);

        if (binFieldFilter == null || binFieldFilter.isAllowed(annotation, rec.mappingField)) {
          if (annotation.custom() && customFieldProcessor == null) {
            throw new JBBPIllegalArgumentException(
                    "Class '" + obj.getClass().getName() + "' contains field '" +
                            rec.mappingField.getName() +
                            "' which is custom one, you must provide JBBPCustomFieldWriter instance to save it.");
          }
          processObjectField(obj, rec, annotation, customFieldProcessor, binFieldFilter);
        }
      }

      this.onStructEnd(obj, field, binAnno);
    }
  }

  /**
   * Inside auxiliary method to process a field of an object.
   *
   * @param obj                  the object which field under processing, must not be null
   * @param fieldRecord          internal record about the field, must not be null
   * @param annotation           the annotation to be used as data source about the field,
   *                             must not be null
   * @param customFieldProcessor an object which will be provided for processing
   *                             of custom fields, must not be null if object contains custom fields
   * @since 2.0.4
   */
  protected void processObjectField(
          final Object obj,
          final MappedFieldRecord fieldRecord,
          final Bin annotation,
          final Object customFieldProcessor
  ) {
    this.processObjectField(obj, fieldRecord, annotation, customFieldProcessor, null);
  }

  /**
   * Inside auxiliary method to process a field of an object.
   *
   * @param obj                  the object which field under processing, must not be null
   * @param fieldRecord          internal record about the field, must not be null
   * @param annotation           the annotation to be used as data source about the field,
   *                             must not be null
   * @param customFieldProcessor an object which will be provided for processing
   *                             of custom fields, must not be null if object contains custom fields
   * @param binFieldFilter       filter allows to exclude some fields from process, can be null
   * @since 2.0.4
   */
  protected void processObjectField(
          final Object obj,
          final MappedFieldRecord fieldRecord,
          final Bin annotation,
          final Object customFieldProcessor,
          final BinFieldFilter binFieldFilter
  ) {
    final Field field = fieldRecord.mappingField;

    if (annotation.custom()) {
      this.onFieldCustom(obj, field, annotation, customFieldProcessor,
              readFieldValue(obj, fieldRecord));
    } else {
      final Class<?> fieldType = field.getType();
      final BinAnnotationWrapper wrapper =
              annotation instanceof BinAnnotationWrapper ? (BinAnnotationWrapper) annotation : null;

      final BinType type;
      if (annotation.type() == BinType.UNDEFINED) {
        type = BinType.findCompatible(fieldType);
      } else {
        type = annotation.type();
      }

      final boolean reverseBits = annotation.bitOrder() == JBBPBitOrder.MSB0;

      switch (type) {
        case BIT: {
          final JBBPBitNumber bitNumber = annotation.bitNumber();
          if (fieldType == boolean.class) {
            this.onFieldBits(obj, field, annotation, bitNumber,
                    ((Boolean) readFieldValue(obj, fieldRecord)) ? 0xFF : 0x00);
          } else {
            byte value = ((Number) readFieldValue(obj, fieldRecord)).byteValue();
            if (reverseBits) {
              value = JBBPUtils.reverseBitsInByte(bitNumber, value);
            }
            this.onFieldBits(obj, field, annotation, bitNumber, value);
          }
        }
        break;
        case BOOL: {
          if (fieldType == boolean.class) {
            onFieldBool(obj, field, annotation, (Boolean) readFieldValue(obj, fieldRecord));
          } else {
            onFieldBool(obj, field, annotation,
                    ((Number) readFieldValue(obj, fieldRecord)).longValue() != 0);
          }
        }
        break;
        case BYTE:
        case UBYTE: {
          byte value = ((Number) readFieldValue(obj, fieldRecord)).byteValue();
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
            value = (short) ((Character) readFieldValue(obj, fieldRecord)).charValue();
          } else {
            value = ((Number) readFieldValue(obj, fieldRecord)).shortValue();
          }
          if (reverseBits) {
            value = (short) JBBPFieldShort.reverseBits(value);
          }
          this.onFieldShort(obj, field, annotation, type == BinType.SHORT, value);
        }
        break;
        case INT: {
          int value;
          value = ((Number) readFieldValue(obj, fieldRecord)).intValue();
          if (reverseBits) {
            value = (int) JBBPFieldInt.reverseBits(value);
          }
          this.onFieldInt(obj, field, annotation, value);
        }
        break;
        case UINT: {
          long value;
          value = ((Number) readFieldValue(obj, fieldRecord)).longValue();
          if (reverseBits) {
            value = (int) JBBPFieldUInt.reverseBits(value);
          }
          this.onFieldUInt(obj, field, annotation, (int) value);
        }
        break;
        case FLOAT: {
          float value;
          if (float.class == fieldType) {
            value = (Float) readFieldValue(obj, fieldRecord);
          } else {
            value = ((Number) readFieldValue(obj, fieldRecord)).floatValue();
          }
          if (reverseBits) {
            value =
                Float.intBitsToFloat((int) JBBPFieldInt.reverseBits(Float.floatToIntBits(value)));
          }
          this.onFieldFloat(obj, field, annotation, value);
        }
        break;
        case STRING: {
          String value;
          final Object valueAsObject = readFieldValue(obj, fieldRecord);
          if (valueAsObject != null) {
            value = String.valueOf(valueAsObject);
            if (reverseBits) {
              value = JBBPFieldString.reverseBits(value);
            }
          } else {
            value = null;
          }
          this.onFieldString(obj, field, annotation, value);
        }
        break;
        case LONG: {
          long value = ((Number) readFieldValue(obj, fieldRecord)).longValue();
          if (reverseBits) {
            value = JBBPFieldLong.reverseBits(value);
          }
          this.onFieldLong(obj, field, annotation, value);
        }
        break;
        case DOUBLE: {
          double value;
          if (float.class == fieldType) {
            value = (Float) readFieldValue(obj, fieldRecord);
          } else if (double.class == fieldType) {
            value = (Double) readFieldValue(obj, fieldRecord);
          } else {
            value = ((Number) readFieldValue(obj, fieldRecord)).doubleValue();
          }

          if (reverseBits) {
            value =
                    Double.longBitsToDouble(JBBPFieldLong.reverseBits(Double.doubleToLongBits(value)));
          }
          this.onFieldDouble(obj, field, annotation, value);
        }
        break;
        case STRUCT: {
          processObject(readFieldValue(obj, fieldRecord), field, wrapper, binFieldFilter, customFieldProcessor);
        }
        break;
        default: {
          final Object array = readFieldValue(obj, fieldRecord);
          switch (type) {
            case BIT_ARRAY: {
              assertFieldArray(field);

              final int len = Array.getLength(array);
              this.onArrayStart(obj, field, annotation, len);

              final JBBPBitNumber bitNumber = annotation.bitNumber();

              if (fieldType.getComponentType() == boolean.class) {
                for (int i = 0; i < len; i++) {
                  this.onFieldBits(obj, field, annotation, bitNumber,
                          (Boolean) Array.get(array, i) ? 0xFF : 0x00);
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
                final String strValue = (String) readFieldValue(obj, fieldRecord);
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
                final String str = (String) readFieldValue(obj, fieldRecord);
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
                  value = Float
                      .intBitsToFloat((int) JBBPFieldInt.reverseBits(Float.floatToIntBits(value)));
                }
                this.onFieldFloat(obj, field, annotation, value);
              }
              this.onArrayEnd(obj, field, annotation);
            }
            break;
            case UINT_ARRAY: {
              assertFieldArray(field);
              final int len = Array.getLength(array);
              this.onArrayStart(obj, field, annotation, len);
              for (int i = 0; i < len; i++) {
                long value = ((Number) Array.get(array, i)).longValue();
                if (reverseBits) {
                  value = JBBPFieldUInt.reverseBits(value);
                }
                this.onFieldUInt(obj, field, annotation, (int) value);
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
            case STRING_ARRAY: {
              assertFieldArray(field);
              final int len = Array.getLength(array);
              this.onArrayStart(obj, field, annotation, len);
              for (int i = 0; i < len; i++) {
                final Object value = Array.get(array, i);
                String nullableStrValue = value == null ? null : String.valueOf(value);
                if (nullableStrValue != null && reverseBits) {
                  nullableStrValue = JBBPFieldString.reverseBits(nullableStrValue);
                }
                this.onFieldString(obj, field, annotation, nullableStrValue);
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
                  value = Double
                          .longBitsToDouble(JBBPFieldLong.reverseBits(Double.doubleToLongBits(value)));
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
                this.processObject(Array.get(array, i), field, wrapper, binFieldFilter, customFieldProcessor);
              }
              this.onArrayEnd(obj, field, annotation);
            }
            break;
            default: {
              throw new Error(
                      "Unexpected situation for field type, contact developer [" + type + ']');
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
  protected void onFieldCustom(final Object obj, final Field field, final Bin annotation,
                               final Object customFieldProcessor, final Object value) {

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
  protected void onFieldBits(final Object obj, final Field field, final Bin annotation,
                             final JBBPBitNumber bitNumber, final int value) {

  }

  /**
   * Notification about boolean field.
   *
   * @param obj        the object instance, must not be null
   * @param field      the field, must not be null
   * @param annotation the annotation for field, must not be null
   * @param value      the value of the field
   */
  protected void onFieldBool(final Object obj, final Field field, final Bin annotation,
                             final boolean value) {

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
  protected void onFieldByte(final Object obj, final Field field, final Bin annotation,
                             final boolean signed, final int value) {

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
  protected void onFieldShort(final Object obj, final Field field, final Bin annotation,
                              final boolean signed, final int value) {

  }

  /**
   * Notification about integer field.
   *
   * @param obj        the object instance, must not be null
   * @param field      the field, must not be null
   * @param annotation the annotation for field, must not be null
   * @param value      the value of the field
   */
  protected void onFieldInt(final Object obj, final Field field, final Bin annotation,
                            final int value) {

  }

  /**
   * Notification about unsigned integer field.
   *
   * @param obj        the object instance, must not be null
   * @param field      the field, must not be null
   * @param annotation the annotation for field, must not be null
   * @param value      the value of the field
   * @since 2.0.4
   */
  protected void onFieldUInt(final Object obj, final Field field, final Bin annotation,
                             final int value) {

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
  protected void onFieldFloat(final Object obj, final Field field, final Bin annotation,
                              final float value) {

  }

  /**
   * Notification about string field.
   *
   * @param obj        the object instance, must not be null
   * @param field      the field, must not be null
   * @param annotation the annotation for field, must not be null
   * @param value      the value of the field
   * @since 1.4.0
   */
  protected void onFieldString(final Object obj, final Field field, final Bin annotation,
                               final String value) {

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
  protected void onFieldDouble(final Object obj, final Field field, final Bin annotation,
                               final double value) {

  }

  /**
   * Notification about long field.
   *
   * @param obj        the object instance, must not be null
   * @param field      the field, must not be null
   * @param annotation the annotation for field, must not be null
   * @param value      the value of the field
   */
  protected void onFieldLong(final Object obj, final Field field, final Bin annotation,
                             final long value) {

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
  protected void onArrayStart(final Object obj, final Field field, final Bin annotation,
                              final int length) {

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

}
