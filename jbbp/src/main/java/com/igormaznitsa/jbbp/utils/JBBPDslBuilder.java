package com.igormaznitsa.jbbp.utils;

import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.mapper.BinType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Auxiliary builder to build DSL script through sequent method call.
 *
 * @since 1.4.0
 */
public class JBBPDslBuilder {

  /**
   * The List contains items added into builder.
   */
  protected final List<Item> items = new ArrayList<Item>();

  /**
   * The Variable contains current byte order for all next fields.
   */
  protected JBBPByteOrder byteOrder = JBBPByteOrder.BIG_ENDIAN;

  /**
   * Constructor is private one because can't be called directly.
   */
  protected JBBPDslBuilder() {
  }

  /**
   * Create new builder.
   *
   * @return the new instance of builder, must not be null
   */
  public static JBBPDslBuilder Begin() {
    return new JBBPDslBuilder();
  }

  /**
   * Auxiliary method to convert numeric value into array size expression.
   *
   * @param size size to be converted
   * @return string contains expression which can be used as array size. must not be null
   */
  protected String arraySizeToString(final int size) {
    return size < 0 ? "_" : Integer.toString(size);
  }

  /**
   * Add 'align' directive with default value (1 byte).
   *
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Align() {
    return this.Align(null);
  }

  /**
   * Add 'align' directive.
   *
   * @param alignBytes number of bytes to be aligned, must not be zero or negative one
   * @return the builder instance, must not be null
   * @throws IllegalArgumentException if value is not acceptable
   */
  public JBBPDslBuilder Align(final int alignBytes) {
    this.Align(Integer.toString(assertNotNegativeAndZero(alignBytes)));
    return this;
  }

  /**
   * Add 'align' directive with size calculated by expression.
   *
   * @param sizeExpression expression to be calculated to get align size, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Align(final String sizeExpression) {
    this.items.add(new ItemAlign(sizeExpression));
    return this;
  }

  /**
   * Add 'skip' directive with default value as one byte.
   *
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Skip() {
    return this.Skip(null);
  }

  /**
   * Add 'skip' directive with number of bytes to be skipped.
   *
   * @param bytesToSkip number of bytes to be skipped, must not be zero or negative one
   * @return the builder instance, must not be null
   * @throws IllegalArgumentException if value is not acceptable
   */
  public JBBPDslBuilder Skip(final int bytesToSkip) {
    this.Skip(Integer.toString(assertNotNegativeAndZero(bytesToSkip)));
    return this;
  }

  /**
   * Add 'skip' directive with number of bytes to be skipped, their number calculated through expression.
   *
   * @param sizeExpression expression to be used to calculate number of bytes, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Skip(final String sizeExpression) {
    this.items.add(new ItemSkip(sizeExpression));
    return this;
  }

  /**
   * Create new anonymous struct.
   *
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Struct() {
    return this.Struct(null);
  }

  /**
   * Create new named struct.
   *
   * @param name name of structure, it can be null for anonymous one
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Struct(final String name) {
    final Item item = new Item(BinType.STRUCT, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  /**
   * Create anonymous structure array which size calculated by expression.
   *
   * @param sizeExpression expression to calculate array length, must not be null.
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder StructArray(final String sizeExpression) {
    return this.StructArray(null, sizeExpression);
  }

  /**
   * Create anonymous fixed size structure array.
   *
   * @param size fixed size of the array, if negative one then read all steam until the end
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder StructArray(final int size) {
    return this.StructArray(null, size);
  }

  /**
   * Create named fixed length structure array.
   *
   * @param name name of the structure array, can be null for anonymous one
   * @param size fixed size of the array, if negative one then read all steam until the end
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder StructArray(final String name, final int size) {
    return this.StructArray(name, arraySizeToString(size));
  }

  /**
   * Create named structure array which size calculated by expression.
   *
   * @param name           name of the structure array, can be null for anonymous one
   * @param sizeExpression expression to calculate array length, must not be null.
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder StructArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.STRUCT_ARRAY, name, this.byteOrder);
    item.sizeExpression = sizeExpression;
    this.items.add(item);
    return this;
  }

  /**
   * Add directive to end currently opened structure or a structure array.
   *
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder EndStruct() {
    this.items.add(new ItemStructEnd(false));
    return this;
  }

  /**
   * Add directive to end all currently opened structures or structure arrays if they presented.
   *
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder EndAllStructs() {
    this.items.add(new ItemStructEnd(true));
    return this;
  }

  /**
   * Add anonymous single bit field.
   *
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Bit() {
    return this.Bits(null, JBBPBitNumber.BITS_1);
  }

  /**
   * Add anonymous fixed sized bit field.
   *
   * @param bits, number of bits 1..7
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Bits(final int bits) {
    return this.Bits(null, JBBPBitNumber.decode(bits));
  }

  /**
   * Add anonymous fixed sized bit field.
   *
   * @param bits, number of bits
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Bits(final JBBPBitNumber bits) {
    return this.Bits(null, bits);
  }

  /**
   * Add named fixed length bit field.
   *
   * @param name name of the field, if null then anonymous one
   * @param bits number of bits as length of the field, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Bits(final String name, final JBBPBitNumber bits) {
    final Item item = new Item(BinType.BIT, name, this.byteOrder);
    item.bitNumber = bits;
    this.items.add(item);
    return this;
  }

  /**
   * Add anonymous fixed length bit array.
   * @param bits length of the field, must not be null
   * @param size number of elements in array, if negative then till the end of stream
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder BitArray(final JBBPBitNumber bits, final int size) {
    return this.BitArray(null, bits, arraySizeToString(size));
  }

  /**
   * Add named fixed length bit array.
   * @param name name of the array, if null then anonymous one
   * @param bits length of the field, must not be null
   * @param size number of elements in array, if negative then till the end of stream
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder BitArray(final String name, final JBBPBitNumber bits, final int size) {
    return this.BitArray(name, bits, arraySizeToString(size));
  }

  /**
   * Add anonymous bit array with size calculated through expression.
   * @param bits length of the field, must not be null
   * @param sizeExpression expression to be used to calculate array size, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder BitArray(final JBBPBitNumber bits, final String sizeExpression) {
    return this.BitArray(null, bits, sizeExpression);
  }

  /**
   * Add named bit array with size calculated through expression.
   * @param name name of the array, if null then anonymous one
   * @param bits length of the field, must not be null
   * @param sizeExpression expression to be used to calculate array size, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder BitArray(final String name, final JBBPBitNumber bits, final String sizeExpression) {
    final Item item = new Item(BinType.BIT_ARRAY, name, this.byteOrder);
    item.bitNumber = bits;
    item.sizeExpression = sizeExpression;
    this.items.add(item);
    return this;
  }

  public JBBPDslBuilder BoolArray(final String sizeExpression) {
    return this.BoolArray(null, sizeExpression);
  }

  public JBBPDslBuilder BoolArray(final String name, final int size) {
    return this.BoolArray(name, arraySizeToString(size));
  }

  public JBBPDslBuilder BoolArray(final int size) {
    return this.BoolArray(null, arraySizeToString(size));
  }

  public JBBPDslBuilder BoolArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.BOOL_ARRAY, name, this.byteOrder);
    item.sizeExpression = sizeExpression;
    this.items.add(item);
    return this;
  }

  public JBBPDslBuilder Bool() {
    return this.Bool(null);
  }

  public JBBPDslBuilder Bool(final String name) {
    final Item item = new Item(BinType.BOOL, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  public JBBPDslBuilder Byte() {
    return this.Byte(null);
  }

  public JBBPDslBuilder Byte(final String name) {
    final Item item = new Item(BinType.BYTE, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  public JBBPDslBuilder ByteArray(final String sizeExpression) {
    return this.ByteArray(null, sizeExpression);
  }

  public JBBPDslBuilder ByteArray(final int size) {
    return this.ByteArray(null, arraySizeToString(size));
  }

  public JBBPDslBuilder ByteArray(final String name, final int size) {
    return this.ByteArray(name, arraySizeToString(size));
  }

  public JBBPDslBuilder ByteArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.BYTE_ARRAY, name, this.byteOrder);
    item.sizeExpression = sizeExpression;
    this.items.add(item);
    return this;
  }

  public JBBPDslBuilder UByte() {
    return this.UByte(null);
  }

  public JBBPDslBuilder UByte(final String name) {
    final Item item = new Item(BinType.UBYTE, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  public JBBPDslBuilder UByteArray(final String sizeExpression) {
    return this.UByteArray(null, sizeExpression);
  }

  public JBBPDslBuilder UByteArray(final int size) {
    return this.UByteArray(null, arraySizeToString(size));
  }

  public JBBPDslBuilder UByteArray(final String name, final int size) {
    return this.UByteArray(name, arraySizeToString(size));
  }

  public JBBPDslBuilder UByteArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.UBYTE_ARRAY, name, this.byteOrder);
    item.sizeExpression = sizeExpression;
    this.items.add(item);
    return this;
  }

  public JBBPDslBuilder Short() {
    return this.Short(null);
  }

  public JBBPDslBuilder Short(final String name) {
    final Item item = new Item(BinType.SHORT, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  public JBBPDslBuilder ShortArray(final String sizeExpression) {
    return this.ShortArray(null, sizeExpression);
  }

  public JBBPDslBuilder ShortArray(final int size) {
    return this.ShortArray(null, arraySizeToString(size));
  }

  public JBBPDslBuilder ShortArray(final String name, final int size) {
    return this.ShortArray(name, arraySizeToString(size));
  }

  public JBBPDslBuilder ShortArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.SHORT_ARRAY, name, this.byteOrder);
    item.sizeExpression = sizeExpression;
    this.items.add(item);
    return this;
  }

  public JBBPDslBuilder UShort() {
    return this.UShort(null);
  }

  public JBBPDslBuilder UShort(final String name) {
    final Item item = new Item(BinType.USHORT, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  public JBBPDslBuilder UShortArray(final String sizeExpression) {
    return this.UShortArray(null, sizeExpression);
  }

  public JBBPDslBuilder UShortArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.USHORT_ARRAY, name, this.byteOrder);
    item.sizeExpression = sizeExpression;
    this.items.add(item);
    return this;
  }

  public JBBPDslBuilder Int() {
    return this.Int(null);
  }

  public JBBPDslBuilder Int(final String name) {
    final Item item = new Item(BinType.INT, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  public JBBPDslBuilder IntArray(final String sizeExpression) {
    return this.IntArray(null, sizeExpression);
  }

  public JBBPDslBuilder IntArray(final int size) {
    return this.IntArray(null, arraySizeToString(size));
  }

  public JBBPDslBuilder IntArray(final String name, final int size) {
    return this.IntArray(name, arraySizeToString(size));
  }

  public JBBPDslBuilder IntArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.INT_ARRAY, name, this.byteOrder);
    item.sizeExpression = sizeExpression;
    this.items.add(item);
    return this;
  }

  public JBBPDslBuilder Long() {
    return this.Long(null);
  }

  public JBBPDslBuilder Long(final String name) {
    final Item item = new Item(BinType.LONG, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  public JBBPDslBuilder LongArray(final String sizeExpression) {
    return this.LongArray(null, sizeExpression);
  }

  public JBBPDslBuilder LongArray(final int size) {
    return this.LongArray(null, arraySizeToString(size));
  }

  public JBBPDslBuilder LongArray(final String name, final int size) {
    return this.LongArray(name, arraySizeToString(size));
  }

  public JBBPDslBuilder LongArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.LONG_ARRAY, name, this.byteOrder);
    item.sizeExpression = sizeExpression;
    this.items.add(item);
    return this;
  }

  public JBBPDslBuilder Float() {
    return this.Float(null);
  }

  public JBBPDslBuilder Float(final String name) {
    final Item item = new Item(BinType.FLOAT, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  public JBBPDslBuilder FloatArray(final String sizeExpression) {
    return this.FloatArray(null, sizeExpression);
  }

  public JBBPDslBuilder FloatArray(final int size) {
    return this.FloatArray(null, arraySizeToString(size));
  }

  public JBBPDslBuilder FloatArray(final String name, final int size) {
    return this.FloatArray(name, arraySizeToString(size));
  }

  public JBBPDslBuilder FloatArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.FLOAT_ARRAY, name, this.byteOrder);
    item.sizeExpression = sizeExpression;
    this.items.add(item);
    return this;
  }

  public JBBPDslBuilder Double() {
    return this.Double(null);
  }

  public JBBPDslBuilder Double(final String name) {
    final Item item = new Item(BinType.DOUBLE, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  public JBBPDslBuilder Comment(final String text) {
    this.items.add(new ItemComment(text == null ? "" : text));
    return this;
  }

  public JBBPDslBuilder DoubleArray(final String sizeExpression) {
    return this.DoubleArray(null, sizeExpression);
  }

  public JBBPDslBuilder DoubleArray(final int size) {
    return this.DoubleArray(null, arraySizeToString(size));
  }

  public JBBPDslBuilder DoubleArray(final String name, final int size) {
    return this.DoubleArray(name, arraySizeToString(size));
  }

  public JBBPDslBuilder DoubleArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.DOUBLE_ARRAY, name, this.byteOrder);
    item.sizeExpression = sizeExpression;
    this.items.add(item);
    return this;
  }

  public JBBPDslBuilder String() {
    return this.String(null);
  }

  public JBBPDslBuilder String(final String name) {
    final Item item = new Item(BinType.STRING, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  public JBBPDslBuilder StringArray(final String sizeExpression) {
    return this.StringArray(null, sizeExpression);
  }

  public JBBPDslBuilder StringArray(final int size) {
    return this.StringArray(null, arraySizeToString(size));
  }

  public JBBPDslBuilder StringArray(final String name, final int size) {
    return this.StringArray(name, arraySizeToString(size));
  }

  public JBBPDslBuilder StringArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.STRING_ARRAY, name, this.byteOrder);
    item.sizeExpression = sizeExpression;
    this.items.add(item);
    return this;
  }

  public JBBPDslBuilder ByteOrder(final JBBPByteOrder order) {
    this.byteOrder = order == null ? JBBPByteOrder.BIG_ENDIAN : order;
    return this;
  }

  protected int assertNotNegativeAndZero(final int value) {
    if (value == 0) {
      throw new IllegalArgumentException("must not be 0");
    }
    if (value < 0) {
      throw new IllegalArgumentException("must not be negative");
    }
    return value;
  }

  public String build() {
    final StringBuilder buffer = new StringBuilder(128);

    int structCounter = 0;

    for (final Item item : this.items) {
      switch (item.type) {
        case STRUCT: {
          structCounter++;
          buffer.append(item.name == null ? "" : item.name).append('{');
        }
        break;
        case STRUCT_ARRAY: {
          structCounter++;
          buffer.append(item.name == null ? "" : item.name).append('[').append(item.sizeExpression).append(']').append('{');
        }
        break;
        case UNDEFINED: {
          if (item instanceof ItemStructEnd) {
            final ItemStructEnd structEnd = (ItemStructEnd) item;
            if (structEnd.endAll) {
              while (structCounter > 0) {
                structCounter--;
                buffer.append('}');
              }
            } else {
              if (structCounter == 0) {
                throw new IllegalStateException("Unexpected structure close");
              }
              structCounter--;
              buffer.append('}');
            }
          } else if (item instanceof ItemAlign) {
            buffer.append("align").append(item.sizeExpression == null ? "" : ':' + item.makeExpressionForExtraField(item.sizeExpression));
            buffer.append(';');
          } else if (item instanceof ItemSkip) {
            buffer.append("skip").append(item.sizeExpression == null ? "" : ':' + item.makeExpressionForExtraField(item.sizeExpression));
            buffer.append(';');
          } else if (item instanceof ItemComment) {
            buffer.append("// ").append(item.name.replace("\n", " ")).append('\n');
          } else {
            throw new IllegalArgumentException("Unexpected item : " + item.getClass().getName());
          }
        }
        break;
        default: {
          buffer.append(item.toString());
        }
        break;
      }
    }

    if (structCounter != 0) {
      throw new IllegalStateException("Detected unclosed structures : " + structCounter);
    }

    return buffer.toString();
  }

  protected static class Item {
    final BinType type;
    final String name;
    final JBBPByteOrder byteOrder;
    String sizeExpression;
    JBBPBitNumber bitNumber;

    Item(final BinType type, final String name, final JBBPByteOrder byteOrder) {
      this.type = type;
      this.name = name;
      this.byteOrder = byteOrder;
    }

    @Override
    public String toString() {
      String type = this.type.name().toLowerCase(Locale.ENGLISH);
      final boolean isArray = type.endsWith("_array");

      if (isArray) {
        type = type.substring(0, type.indexOf('_'));
      }

      if (type.equals("string") || type.equals("float") || type.equals("double")) {
        type += 'j';
      }

      final StringBuilder result = new StringBuilder();

      if (this.byteOrder == JBBPByteOrder.LITTLE_ENDIAN) {
        result.append('<');
      }

      result.append(type);

      if (this.type == BinType.BIT || this.type == BinType.BIT_ARRAY) {
        if (this.bitNumber != null) {
          result.append(':').append(this.bitNumber.getBitNumber());
        } else if (this.sizeExpression != null) {
          result.append(':').append(makeExpressionForExtraField(this.sizeExpression));
        }
      }

      if (isArray) {
        result.append('[').append(this.sizeExpression).append(']').append(' ');
      }

      if (this.name != null && !this.name.isEmpty()) {
        result.append(' ').append(this.name);
      }

      result.append(';');

      return result.toString();
    }

    /**
     * Auxiliary method to prepare expression which can be placed as extra data in fields.
     *
     * @param expression expression to be prepared, must not be null
     * @return prepared string, must not be null
     */
    protected String makeExpressionForExtraField(final String expression) {
      try {
        Long.parseLong(expression);
        return expression;
      } catch (NumberFormatException ex) {
        return '(' + expression + ')';
      }
    }
  }

  protected static class ItemComment extends Item {
    ItemComment(final String text) {
      super(BinType.UNDEFINED, text, JBBPByteOrder.BIG_ENDIAN);
    }
  }

  protected static class ItemAlign extends Item {
    ItemAlign(final String sizeExpression) {
      super(BinType.UNDEFINED, null, JBBPByteOrder.BIG_ENDIAN);
      this.sizeExpression = sizeExpression;
    }
  }

  protected static class ItemStructEnd extends Item {
    private final boolean endAll;

    ItemStructEnd(final boolean endAll) {
      super(BinType.UNDEFINED, null, JBBPByteOrder.BIG_ENDIAN);
      this.endAll = endAll;
    }
  }

  protected static class ItemSkip extends Item {
    ItemSkip(final String sizeExpression) {
      super(BinType.UNDEFINED, null, JBBPByteOrder.BIG_ENDIAN);
      this.sizeExpression = sizeExpression;
    }
  }

}
