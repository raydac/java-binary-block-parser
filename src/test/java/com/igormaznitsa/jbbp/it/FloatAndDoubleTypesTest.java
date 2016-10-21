/*
 * Copyright 2016 Igor Maznitsa.
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
package com.igormaznitsa.jbbp.it;

import static org.junit.Assert.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import org.junit.Test;
import com.igormaznitsa.jbbp.JBBPCustomFieldTypeProcessor;
import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPFieldTypeParameterContainer;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.model.JBBPAbstractArrayField;
import com.igormaznitsa.jbbp.model.JBBPAbstractField;
import com.igormaznitsa.jbbp.model.JBBPFieldInt;
import com.igormaznitsa.jbbp.model.JBBPFieldLong;
import com.igormaznitsa.jbbp.model.JBBPNumericField;
import com.igormaznitsa.jbbp.utils.JBBPTextWriter;
import com.igormaznitsa.jbbp.utils.JBBPUtils;

public strictfp class FloatAndDoubleTypesTest {

  public static final class JBBPFieldFloat extends JBBPAbstractField implements JBBPNumericField {

    private static final long serialVersionUID = 9022682441939193636L;

    private final int value;

    public JBBPFieldFloat(final JBBPNamedFieldInfo name, final int value) {
      super(name);
      this.value = value;
    }

    public float getAsFloat() {
      return Float.intBitsToFloat(this.value);
    }

    public int getAsInt() {
      return this.value;
    }

    public long getAsLong() {
      return this.getAsInt();
    }

    public boolean getAsBool() {
      return this.value != 0;
    }

    public static long reverseBits(final int value) {
      final int b0 = JBBPUtils.reverseBitsInByte((byte) value) & 0xFF;
      final int b1 = JBBPUtils.reverseBitsInByte((byte) (value >> 8)) & 0xFF;
      final int b2 = JBBPUtils.reverseBitsInByte((byte) (value >> 16)) & 0xFF;
      final int b3 = JBBPUtils.reverseBitsInByte((byte) (value >> 24)) & 0xFF;

      return (long) ((b0 << 24) | (b1 << 16) | (b2 << 8) | b3);
    }

    public long getAsInvertedBitOrder() {
      return reverseBits(this.value);
    }

    @Override
    public String getTypeAsString() {
      return "float";
    }

  }

  public static final class JBBPFieldArrayFloat extends JBBPAbstractArrayField<JBBPFieldFloat> {

    private static final long serialVersionUID = 9088456752092618676L;

    private final int[] array;

    public JBBPFieldArrayFloat(final JBBPNamedFieldInfo name, final int[] array) {
      super(name);
      JBBPUtils.assertNotNull(array, "Array must not be null");
      this.array = array;
    }

    public float[] getArray() {
      final float[] result = new float[this.array.length];
      for (int i = 0; i < result.length; i++) {
        result[i] = Float.intBitsToFloat(this.array[i]);
      }
      return result;
    }

    @Override
    public int size() {
      return this.array.length;
    }

    @Override
    public JBBPFieldFloat getElementAt(final int index) {
      final JBBPFieldFloat result = new JBBPFieldFloat(this.fieldNameInfo, this.array[index]);
      result.setPayload(this.payload);
      return result;
    }

    @Override
    public int getAsInt(final int index) {
      return this.array[index];
    }

    @Override
    public long getAsLong(final int index) {
      return this.getAsInt(index);
    }

    @Override
    public boolean getAsBool(final int index) {
      return this.array[index] != 0;
    }

    public float getAsFloat(final int index) {
      return Float.intBitsToFloat(this.array[index]);
    }

    @Override
    public Object getValueArrayAsObject(final boolean reverseBits) {
      final float[] result = new float[this.array.length];
      if (reverseBits) {
        for (int i = 0; i < result.length; i++) {
          result[i] = Float.intBitsToFloat((int) JBBPFieldInt.reverseBits(this.array[i]));
        }
      }
      else {
        for (int i = 0; i < result.length; i++) {
          result[i] = Float.intBitsToFloat(this.array[i]);
        }
      }
      return result;
    }

    @Override
    public String getTypeAsString() {
      return "float " + '[' + this.array.length + ']';
    }
  }

  public static final class JBBPFieldDouble extends JBBPAbstractField implements JBBPNumericField {

    private static final long serialVersionUID = 8571285179176757539L;

    private final long value;

    public JBBPFieldDouble(final JBBPNamedFieldInfo name, final long value) {
      super(name);
      this.value = value;
    }

    public double getAsDouble() {
      return Double.longBitsToDouble(this.value);
    }

    public int getAsInt() {
      return (int) this.value;
    }

    public long getAsLong() {
      return this.value;
    }

    public boolean getAsBool() {
      return this.value != 0;
    }

    public static long reverseBits(final long value) {
      final long b0 = JBBPUtils.reverseBitsInByte((byte) value) & 0xFFL;
      final long b1 = JBBPUtils.reverseBitsInByte((byte) (value >> 8)) & 0xFFL;
      final long b2 = JBBPUtils.reverseBitsInByte((byte) (value >> 16)) & 0xFFL;
      final long b3 = JBBPUtils.reverseBitsInByte((byte) (value >> 24)) & 0xFFL;
      final long b4 = JBBPUtils.reverseBitsInByte((byte) (value >> 32)) & 0xFFL;
      final long b5 = JBBPUtils.reverseBitsInByte((byte) (value >> 40)) & 0xFFL;
      final long b6 = JBBPUtils.reverseBitsInByte((byte) (value >> 48)) & 0xFFL;
      final long b7 = JBBPUtils.reverseBitsInByte((byte) (value >> 56)) & 0xFFL;

      return (b0 << 56) | (b1 << 48) | (b2 << 40) | (b3 << 32) | (b4 << 24) | (b5 << 16) | (b6 << 8) | b7;
    }

    public long getAsInvertedBitOrder() {
      return reverseBits(this.value);
    }

    @Override
    public String getTypeAsString() {
      return "double";
    }

  }

  public static final class JBBPFieldArrayDouble extends JBBPAbstractArrayField<JBBPFieldDouble> {

    private static final long serialVersionUID = 5143347696236941029L;

    private final long[] array;

    public JBBPFieldArrayDouble(final JBBPNamedFieldInfo name, final long[] array) {
      super(name);
      JBBPUtils.assertNotNull(array, "Array must not be null");
      this.array = array;
    }

    public double[] getArray() {
      final double[] result = new double[this.array.length];
      for (int i = 0; i < result.length; i++) {
        result[i] = Double.longBitsToDouble(this.array[i]);
      }
      return result;
    }

    @Override
    public int size() {
      return this.array.length;
    }

    @Override
    public JBBPFieldDouble getElementAt(final int index) {
      final JBBPFieldDouble result = new JBBPFieldDouble(this.fieldNameInfo, this.array[index]);
      result.setPayload(this.payload);
      return result;
    }

    public double getAsDouble(final int index) {
      return Double.longBitsToDouble(this.array[index]);
    }

    @Override
    public int getAsInt(final int index) {
      return (int) this.array[index];
    }

    @Override
    public long getAsLong(final int index) {
      return this.array[index];
    }

    @Override
    public boolean getAsBool(final int index) {
      return this.array[index] != 0L;
    }

    @Override
    public Object getValueArrayAsObject(final boolean reverseBits) {
      final double[] result = new double[this.array.length];
      if (reverseBits) {
        for (int i = 0; i < result.length; i++) {
          result[i] = Double.longBitsToDouble(JBBPFieldLong.reverseBits(this.array[i]));
        }
      }
      else {
        for (int i = 0; i < result.length; i++) {
          result[i] = Double.longBitsToDouble(this.array[i]);
        }
      }
      return result;
    }

    @Override
    public String getTypeAsString() {
      return "double " + '[' + this.array.length + ']';
    }
  }

  public static final class JBBPFloatAndDoubleTypeProcessor implements JBBPCustomFieldTypeProcessor {

    private static final String[] TYPES = new String[]{"float", "double"};

    public String[] getCustomFieldTypes() {
      return TYPES;
    }

    public boolean isAllowed(final JBBPFieldTypeParameterContainer fieldType, final String fieldName, final int extraData, final boolean isArray) {
      return true;
    }

    public JBBPAbstractField readCustomFieldType(final JBBPBitInputStream in, final JBBPBitOrder bitOrder, final int parserFlags, final JBBPFieldTypeParameterContainer customTypeFieldInfo, final JBBPNamedFieldInfo fieldName, final int extraData, final boolean readWholeStream, final int arrayLength) throws IOException {
      final JBBPAbstractField result;

      final boolean needsBitReversing = in.getBitOrder() != bitOrder;

      if (customTypeFieldInfo.getTypeName().equals("float")) {
        if (readWholeStream) {
          final int[] array = in.readIntArray(-1, customTypeFieldInfo.getByteOrder());
          if (needsBitReversing) {
            for (int i = 0; i < array.length; i++) {
              array[i] = (int) JBBPFieldInt.reverseBits(array[i]);
            }
          }
          result = new JBBPFieldArrayFloat(fieldName, array);
        }
        else if (arrayLength >= 0) {
          final int[] array = in.readIntArray(arrayLength, customTypeFieldInfo.getByteOrder());
          if (needsBitReversing) {
            for (int i = 0; i < array.length; i++) {
              array[i] = (int) JBBPFieldInt.reverseBits(array[i]);
            }
          }
          result = new JBBPFieldArrayFloat(fieldName, array);
        }
        else {
          final int value = in.readInt(customTypeFieldInfo.getByteOrder());
          result = new JBBPFieldFloat(fieldName, needsBitReversing ? (int) JBBPFieldInt.reverseBits(value) : value);
        }
      }
      else {
        if (readWholeStream) {
          final long[] array = in.readLongArray(-1, customTypeFieldInfo.getByteOrder());
          if (needsBitReversing) {
            for (int i = 0; i < array.length; i++) {
              array[i] = JBBPFieldLong.reverseBits(array[i]);
            }
          }
          result = new JBBPFieldArrayDouble(fieldName, array);
        }
        else if (arrayLength >= 0) {
          final long[] array = in.readLongArray(arrayLength, customTypeFieldInfo.getByteOrder());
          if (needsBitReversing) {
            for (int i = 0; i < array.length; i++) {
              array[i] = JBBPFieldLong.reverseBits(array[i]);
            }
          }
          result = new JBBPFieldArrayDouble(fieldName, array);
        }
        else {
          final long value = in.readLong(customTypeFieldInfo.getByteOrder());
          result = new JBBPFieldDouble(fieldName, needsBitReversing ? JBBPFieldLong.reverseBits(value) : value);
        }
      }
      return result;
    }

  }

  private static byte[] floatToBytes(final float value, final boolean reverse) {
    final int valueAsInt = Float.floatToIntBits(value);

    int intvaue = reverse ? (int) JBBPFieldInt.reverseBits(valueAsInt) : valueAsInt;

    final byte[] result = new byte[4];

    for (int i = 0; i < 4; i++) {
      result[i] = (byte) (intvaue >>> 24);
      intvaue <<= 8;
    }
    return result;
  }

  private static byte[] doubleToBytes(final double value, final boolean reverse) {
    final long valueAsLong = Double.doubleToLongBits(value);

    long buffer = reverse ? JBBPFieldLong.reverseBits(valueAsLong) : valueAsLong;

    final byte[] result = new byte[8];

    for (int i = 0; i < 8; i++) {
      result[i] = (byte) (buffer >>> 56);
      buffer <<= 8;
    }
    return result;
  }

  private static byte[] floatsAsArray(final boolean reverse, final float... value) {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    for (final float f : value) {
      buffer.write(floatToBytes(f, reverse), 0, 4);
    }
    return buffer.toByteArray();
  }

  private static byte[] doublesAsArray(final boolean reverse, final double... value) {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    for (final double f : value) {
      buffer.write(doubleToBytes(f, reverse), 0, 8);
    }
    return buffer.toByteArray();
  }

  @Test
  public void testParseSingleFloat() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("float a;", new JBBPFloatAndDoubleTypeProcessor());
    final float etalon = 1.2345f;
    final JBBPFieldFloat value = parser.parse(floatToBytes(etalon, false)).findFieldForType(JBBPFieldFloat.class);
    assertEquals(etalon, value.getAsFloat(), 0.0f);
  }

  @Test
  public void testParseSingleFloat_Reversed() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("<float a;", JBBPBitOrder.MSB0, new JBBPFloatAndDoubleTypeProcessor(), 0);
    final float etalon = 1.2345f;

    final byte[] arrayparse = floatToBytes(etalon, true);
    final JBBPFieldFloat value = parser.parse(arrayparse).findFieldForType(JBBPFieldFloat.class);

    assertEquals(etalon, value.getAsFloat(), 0.0f);
  }

  @Test
  public void testParseFloatArray_WholeSream() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("float [_] a;", new JBBPFloatAndDoubleTypeProcessor());
    final float[] etalon = new float[]{1.2345f, -734.334f};
    final JBBPFieldArrayFloat value = parser.parse(floatsAsArray(false, etalon)).findFieldForType(JBBPFieldArrayFloat.class);
    assertArrayEquals(etalon, value.getArray(), 0.0f);
  }

  @Test
  public void testParseFloatArray_WholeSream_Reversed() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("<float [_] a;", JBBPBitOrder.MSB0, new JBBPFloatAndDoubleTypeProcessor(), 0);
    final float[] etalon = new float[]{1.2345f, -734.334f};
    final JBBPFieldArrayFloat value = parser.parse(floatsAsArray(true, etalon)).findFieldForType(JBBPFieldArrayFloat.class);
    assertArrayEquals(etalon, value.getArray(), 0.0f);
  }

  @Test
  public void testParseFloatArray_FirstTwoElements() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("float [2] a;", new JBBPFloatAndDoubleTypeProcessor());
    final float[] etalon = new float[]{1.2345f, -734.334f};
    final JBBPFieldArrayFloat value = parser.parse(floatsAsArray(false, Arrays.copyOf(etalon, 334))).findFieldForType(JBBPFieldArrayFloat.class);
    assertArrayEquals(etalon, value.getArray(), 0.0f);
  }

  @Test
  public void testParseFloatArray_FirstTwoElements_Reversed() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("<float [2] a;", JBBPBitOrder.MSB0, new JBBPFloatAndDoubleTypeProcessor(), 0);
    final float[] etalon = new float[]{1.2345f, -734.334f};
    final JBBPFieldArrayFloat value = parser.parse(floatsAsArray(true, Arrays.copyOf(etalon, 334))).findFieldForType(JBBPFieldArrayFloat.class);
    assertArrayEquals(etalon, value.getArray(), 0.0f);
  }

  @Test
  public void testParseSingleDouble() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("double a;", new JBBPFloatAndDoubleTypeProcessor());
    final double etalon = 1.234524324324d;
    final JBBPFieldDouble value = parser.parse(doubleToBytes(etalon, false)).findFieldForType(JBBPFieldDouble.class);
    assertEquals(etalon, value.getAsDouble(), 0.0d);
  }

  @Test
  public void testParseSingleDouble_Reversed() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("<double a;", JBBPBitOrder.MSB0, new JBBPFloatAndDoubleTypeProcessor(), 0);
    final double etalon = 1.2345287364324d;

    final byte[] arrayparse = doubleToBytes(etalon, true);
    final JBBPFieldDouble value = parser.parse(arrayparse).findFieldForType(JBBPFieldDouble.class);

    assertEquals(etalon, value.getAsDouble(), 0.0d);
  }

  @Test
  public void testParseDoubleArray_WholeSream() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("double [_] a;", new JBBPFloatAndDoubleTypeProcessor());
    final double[] etalon = new double[]{1.2432324345d, -23432734.334d};
    final JBBPFieldArrayDouble value = parser.parse(doublesAsArray(false, etalon)).findFieldForType(JBBPFieldArrayDouble.class);
    assertArrayEquals(etalon, value.getArray(), 0.0d);
  }

  @Test
  public void testParseDoubleArray_WholeSream_Reversed() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("<double [_] a;", JBBPBitOrder.MSB0, new JBBPFloatAndDoubleTypeProcessor(), 0);
    final double[] etalon = new double[]{1.2345324234324f, -234324734.334f};
    final JBBPFieldArrayDouble value = parser.parse(doublesAsArray(true, etalon)).findFieldForType(JBBPFieldArrayDouble.class);
    assertArrayEquals(etalon, value.getArray(), 0.0d);
  }

  @Test
  public void testParseDoubleArray_FirstTwoElements() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("double [2] a;", new JBBPFloatAndDoubleTypeProcessor());
    final double[] etalon = new double[]{1.234523424234d, -232432734.234324334d};
    final JBBPFieldArrayDouble value = parser.parse(doublesAsArray(false, Arrays.copyOf(etalon, 334))).findFieldForType(JBBPFieldArrayDouble.class);
    assertArrayEquals(etalon, value.getArray(), 0.0d);
  }

  @Test
  public void testParseDoubleArray_FirstTwoElements_Reversed() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("<double [2] a;", JBBPBitOrder.MSB0, new JBBPFloatAndDoubleTypeProcessor(), 0);
    final double[] etalon = new double[]{1.2342432345d, -32432324734.32432334d};
    final JBBPFieldArrayDouble value = parser.parse(doublesAsArray(true, Arrays.copyOf(etalon, 334))).findFieldForType(JBBPFieldArrayDouble.class);
    assertArrayEquals(etalon, value.getArray(), 0.0d);
  }

}
