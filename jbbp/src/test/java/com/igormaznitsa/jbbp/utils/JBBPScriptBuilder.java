package com.igormaznitsa.jbbp.utils;

import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.mapper.BinType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class JBBPScriptBuilder {

  private class Item {
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
  }

  private class ItemComment extends Item {
    ItemComment(final String text) {
      super(BinType.UNDEFINED, text, JBBPByteOrder.BIG_ENDIAN);
    }
  }

  private class ItemAlign extends Item {
    ItemAlign(final String sizeExpression) {
      super(BinType.UNDEFINED, null, JBBPByteOrder.BIG_ENDIAN);
      this.sizeExpression = sizeExpression;
    }
  }

  private class ItemStructEnd extends Item {
    ItemStructEnd() {
      super(BinType.UNDEFINED, null, JBBPByteOrder.BIG_ENDIAN);
    }
  }

  private class ItemSkip extends Item {
    ItemSkip(final String sizeExpression) {
      super(BinType.UNDEFINED, null, JBBPByteOrder.BIG_ENDIAN);
      this.sizeExpression = sizeExpression;
    }
  }

  private final List<Item> items = new ArrayList<Item>();

  private JBBPByteOrder byteOrder = JBBPByteOrder.BIG_ENDIAN;

  private JBBPScriptBuilder() {
  }

  public static JBBPScriptBuilder Begin() {
    return new JBBPScriptBuilder();
  }

  public JBBPScriptBuilder Align() {
    return this.Align(null);
  }

  public JBBPScriptBuilder Align(final String sizeExpression) {
    this.items.add(new ItemAlign(sizeExpression));
    return this;
  }

  public JBBPScriptBuilder Skip() {
    return this.Skip(null);
  }

  public JBBPScriptBuilder Skip(final String sizeExpression) {
    this.items.add(new ItemSkip(sizeExpression));
    return this;
  }

  public JBBPScriptBuilder Struct() {
    return this.Struct(null);
  }

  public JBBPScriptBuilder Struct(final String name) {
    final Item item = new Item(BinType.STRUCT, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  public JBBPScriptBuilder StructArray(final String sizeExpression) {
    return this.Struct(null);
  }

  public JBBPScriptBuilder StructArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.STRUCT_ARRAY, name, this.byteOrder);
    item.sizeExpression = sizeExpression;
    this.items.add(item);
    return this;
  }

  public JBBPScriptBuilder EndStruct() {
    this.items.add(new ItemStructEnd());
    return this;
  }

  public JBBPScriptBuilder Bit(final JBBPBitNumber bits) {
    return this.Bit(null, bits);
  }

  public JBBPScriptBuilder Bit(final String name, final JBBPBitNumber bits) {
    final Item item = new Item(BinType.BIT, name, this.byteOrder);
    item.bitNumber = bits;
    this.items.add(item);
    return this;
  }

  public JBBPScriptBuilder BitArray(final JBBPBitNumber bits, final String sizeExpression) {
    return this.BitArray(null, bits, sizeExpression);
  }

  public JBBPScriptBuilder BitArray(final String name, final JBBPBitNumber bits, final String sizeExpression) {
    final Item item = new Item(BinType.BIT_ARRAY, name, this.byteOrder);
    item.bitNumber = bits;
    item.sizeExpression = sizeExpression;
    this.items.add(item);
    return this;
  }

  public JBBPScriptBuilder BoolArray(final String sizeExpression) {
    return this.BoolArray(null, sizeExpression);
  }

  public JBBPScriptBuilder BoolArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.BOOL_ARRAY, name, this.byteOrder);
    item.sizeExpression = sizeExpression;
    this.items.add(item);
    return this;
  }

  public JBBPScriptBuilder Bool() {
    return this.Bool(null);
  }

  public JBBPScriptBuilder Bool(final String name) {
    final Item item = new Item(BinType.BOOL, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  public JBBPScriptBuilder Byte() {
    return this.Byte(null);
  }

  public JBBPScriptBuilder Byte(final String name) {
    final Item item = new Item(BinType.BYTE, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  public JBBPScriptBuilder ByteArray(final String sizeExpression) {
    return this.ByteArray(null, sizeExpression);
  }

  public JBBPScriptBuilder ByteArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.BYTE_ARRAY, name, this.byteOrder);
    item.sizeExpression = sizeExpression;
    this.items.add(item);
    return this;
  }

  public JBBPScriptBuilder UByte() {
    return this.UByte(null);
  }

  public JBBPScriptBuilder UByte(final String name) {
    final Item item = new Item(BinType.UBYTE, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  public JBBPScriptBuilder UByteArray(final String sizeExpression) {
    return this.UByteArray(null, sizeExpression);
  }

  public JBBPScriptBuilder UByteArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.UBYTE_ARRAY, name, this.byteOrder);
    item.sizeExpression = sizeExpression;
    this.items.add(item);
    return this;
  }

  public JBBPScriptBuilder Short() {
    return this.Short(null);
  }

  public JBBPScriptBuilder Short(final String name) {
    final Item item = new Item(BinType.SHORT, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  public JBBPScriptBuilder ShortArray(final String sizeExpression) {
    return this.ShortArray(null, sizeExpression);
  }

  public JBBPScriptBuilder ShortArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.SHORT_ARRAY, name, this.byteOrder);
    item.sizeExpression = sizeExpression;
    this.items.add(item);
    return this;
  }

  public JBBPScriptBuilder UShort() {
    return this.UShort(null);
  }

  public JBBPScriptBuilder UShort(final String name) {
    final Item item = new Item(BinType.USHORT, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  public JBBPScriptBuilder UShortArray(final String sizeExpression) {
    return this.UShortArray(null,sizeExpression);
  }

  public JBBPScriptBuilder UShortArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.USHORT_ARRAY, name, this.byteOrder);
    item.sizeExpression = sizeExpression;
    this.items.add(item);
    return this;
  }

  public JBBPScriptBuilder Int() {
    return this.Int(null);
  }

  public JBBPScriptBuilder Int(final String name) {
    final Item item = new Item(BinType.INT, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  public JBBPScriptBuilder IntArray(final String sizeExpression) {
    return this.IntArray(null, sizeExpression);
  }

  public JBBPScriptBuilder IntArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.INT_ARRAY, name, this.byteOrder);
    item.sizeExpression = sizeExpression;
    this.items.add(item);
    return this;
  }

  public JBBPScriptBuilder Long() {
    return this.Long(null);
  }

  public JBBPScriptBuilder Long(final String name) {
    final Item item = new Item(BinType.LONG, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  public JBBPScriptBuilder LongArray(final String sizeExpression) {
    return this.LongArray(null, sizeExpression);
  }

  public JBBPScriptBuilder LongArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.LONG_ARRAY, name, this.byteOrder);
    item.sizeExpression = sizeExpression;
    this.items.add(item);
    return this;
  }

  public JBBPScriptBuilder Float() {
    return this.Float(null);
  }

  public JBBPScriptBuilder Float(final String name) {
    final Item item = new Item(BinType.FLOAT, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  public JBBPScriptBuilder FloatArray(final String sizeExpression) {
    return this.FloatArray(null, sizeExpression);
  }

  public JBBPScriptBuilder FloatArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.FLOAT_ARRAY, name, this.byteOrder);
    item.sizeExpression = sizeExpression;
    this.items.add(item);
    return this;
  }

  public JBBPScriptBuilder Double() {
    return this.Double(null);
  }

  public JBBPScriptBuilder Double(final String name) {
    final Item item = new Item(BinType.DOUBLE, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  public JBBPScriptBuilder Comment(final String text) {
    this.items.add(new ItemComment(text == null ? "" : text));
    return this;
  }

  public JBBPScriptBuilder DoubleArray(final String sizeExpression) {
    return this.DoubleArray(null, sizeExpression);
  }

  public JBBPScriptBuilder DoubleArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.DOUBLE_ARRAY, name, this.byteOrder);
    item.sizeExpression = sizeExpression;
    this.items.add(item);
    return this;
  }

  public JBBPScriptBuilder String() {
    return this.String(null);
  }

  public JBBPScriptBuilder String(final String name) {
    final Item item = new Item(BinType.STRING, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  public JBBPScriptBuilder StringArray(final String sizeExpression) {
    return this.String(null);
  }

  public JBBPScriptBuilder StringArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.STRING_ARRAY, name, this.byteOrder);
    item.sizeExpression = sizeExpression;
    this.items.add(item);
    return this;
  }

  public JBBPScriptBuilder ByteOrder(final JBBPByteOrder order) {
    this.byteOrder = order == null ? JBBPByteOrder.BIG_ENDIAN : order;
    return this;
  }

  private static String makeExpresionForExtraField(final String expression) {
    try {
      Long.parseLong(expression);
      return expression;
    } catch (NumberFormatException ex) {
      return '(' + expression + ')';
    }
  }

  private static String makeForItem(final Item item) {
    String type = item.type.name().toLowerCase(Locale.ENGLISH);
    final boolean isarray = type.endsWith("_array");
    if (isarray) {
      type = type.substring(0, type.indexOf('_'));
    }
    if (type.equals("string") || type.equals("float") || type.equals("double")) {
      type += 'j';
    }

    final StringBuilder result = new StringBuilder();

    if (item.byteOrder == JBBPByteOrder.LITTLE_ENDIAN) {
      result.append('<');
    }

    result.append(type);

    if (item.type == BinType.BIT || item.type == BinType.BIT_ARRAY) {
      if (item.bitNumber != null) {
        result.append(':').append(item.bitNumber.getBitNumber());
      } else if (item.sizeExpression != null) {
        result.append(':').append(makeExpresionForExtraField(item.sizeExpression));
      }
    }

    if (isarray) {
      result.append('[').append(item.sizeExpression).append(']').append(' ');
    }

    if (item.name != null && !item.name.isEmpty()) {
      result.append(' ').append(item.name);
    }

    result.append(';');

    return result.toString();
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
            if (structCounter == 0) {
              throw new IllegalStateException("End of undefined structure");
            }
            structCounter--;
            buffer.append('}');
          } else if (item instanceof ItemAlign) {
            buffer.append("align").append(item.sizeExpression == null ? "" : ':' + makeExpresionForExtraField(item.sizeExpression));
            buffer.append(';');
          } else if (item instanceof ItemSkip) {
            buffer.append("skip").append(item.sizeExpression == null ? "" : ':' + makeExpresionForExtraField(item.sizeExpression));
            buffer.append(';');
          } else if (item instanceof ItemComment) {
            buffer.append("// ").append(item.name.replace("\n", " ")).append('\n');
          } else {
            throw new IllegalArgumentException("Detected unexpected item : " + item.getClass().getName());
          }
        }
        break;
        default: {
          buffer.append(makeForItem(item));
        }
        break;
      }
    }

    if (structCounter != 0) {
      throw new IllegalStateException("Detected unclosed structures : " + structCounter);
    }

    return buffer.toString();
  }

}
