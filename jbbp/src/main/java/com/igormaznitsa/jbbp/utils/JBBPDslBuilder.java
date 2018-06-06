package com.igormaznitsa.jbbp.utils;

import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.mapper.BinType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Auxiliary builder to build string JBBP script through sequent method call.
 * <b>NB! The Builder generaes JBBP string script which can be compiled by parser!</b>
 *
 * @see com.igormaznitsa.jbbp.JBBPParser
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

  protected static String assertStringNotNull(final String str) {
    if (str == null) {
      throw new NullPointerException("String is null");
    }
    return str;
  }

  protected static int assertNotNegativeAndZero(final int value) {
    if (value == 0) {
      throw new IllegalArgumentException("must not be 0");
    }
    if (value < 0) {
      throw new IllegalArgumentException("must not be negative");
    }
    return value;
  }

  protected static StringBuilder doTabs(final boolean enable, final StringBuilder buffer, int tabs) {
    if (enable) {
      while (tabs > 0) {
        buffer.append('\t');
        tabs--;
      }
    }
    return buffer;
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
    return this.Align(1);
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
    this.items.add(new ItemAlign(assertStringNotNull(sizeExpression)));
    return this;
  }

  /**
   * Add 'skip' directive with default value as one byte.
   *
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Skip() {
    return this.Skip(1);
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
    this.items.add(new ItemSkip(assertStringNotNull(sizeExpression)));
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
    return this.StructArray(null, assertStringNotNull(sizeExpression));
  }

  /**
   * Create anonymous fixed size structure array.
   *
   * @param size fixed size of the array, if negative one then read all steam until the end
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder StructArray(final int size) {
    return this.StructArray(null, arraySizeToString(size));
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
    item.sizeExpression = assertStringNotNull(sizeExpression);
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
   * Add named single bit field.
   * @param name name of the field, can be null for anonymous one
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Bit(final String name) {
    return this.Bits(name, JBBPBitNumber.BITS_1);
  }

  /**
   * Add anonymous fixed size bit field.
   *
   * @param bits, number of bits 1..7
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Bits(final int bits) {
    return this.Bits(null, JBBPBitNumber.decode(bits));
  }

  /**
   * Add anonymous fixed size bit field.
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
   *
   * @param bits length of the field, must not be null
   * @param size number of elements in array, if negative then till the end of stream
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder BitArray(final JBBPBitNumber bits, final int size) {
    return this.BitArray(null, bits, arraySizeToString(size));
  }

  /**
   * Add named fixed length bit array.
   *
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
   *
   * @param bits           length of the field, must not be null
   * @param sizeExpression expression to be used to calculate array size, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder BitArray(final JBBPBitNumber bits, final String sizeExpression) {
    return this.BitArray(null, bits, assertStringNotNull(sizeExpression));
  }

  /**
   * Add named bit array with size calculated through expression.
   *
   * @param name           name of the array, if null then anonymous one
   * @param bits           length of the field, must not be null
   * @param sizeExpression expression to be used to calculate array size, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder BitArray(final String name, final JBBPBitNumber bits, final String sizeExpression) {
    final Item item = new Item(BinType.BIT_ARRAY, name, this.byteOrder);
    item.bitNumber = bits;
    item.sizeExpression = assertStringNotNull(sizeExpression);
    this.items.add(item);
    return this;
  }

  /**
   * Add anonymous boolean array with size calculated through expression.
   *
   * @param sizeExpression expression to be used to calculate array size, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder BoolArray(final String sizeExpression) {
    return this.BoolArray(null, assertStringNotNull(sizeExpression));
  }

  /**
   * Add named fixed size boolean array.
   *
   * @param name name of the array, it can be null for anonymous one
   * @param size number of elements in array, if negative then till the end of stream
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder BoolArray(final String name, final int size) {
    return this.BoolArray(name, arraySizeToString(size));
  }

  /**
   * Add anonymous fixed size boolean array.
   *
   * @param size number of elements in array, if negative then till the end of stream
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder BoolArray(final int size) {
    return this.BoolArray(null, arraySizeToString(size));
  }

  /**
   * Add named boolean array which length calculated through expression.
   *
   * @param name           name of the array, it can be null for anonymous one
   * @param sizeExpression expression to calculate number of elements, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder BoolArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.BOOL_ARRAY, name, this.byteOrder);
    item.sizeExpression = assertStringNotNull(sizeExpression);
    this.items.add(item);
    return this;
  }

  /**
   * Add an anonymous boolean field.
   *
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Bool() {
    return this.Bool(null);
  }

  /**
   * Add named boolean field.
   *
   * @param name name of the field, can be null for anonymous one
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Bool(final String name) {
    final Item item = new Item(BinType.BOOL, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  /**
   * Add anonymous signed byte field.
   *
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Byte() {
    return this.Byte(null);
  }

  /**
   * Add named signed byte field.
   *
   * @param name name of the field, can be null for anonymous one
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Byte(final String name) {
    final Item item = new Item(BinType.BYTE, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  /**
   * Add anonymous byte array which size calculated through expression.
   *
   * @param sizeExpression expression to be used to calculate array length, must not be null or empty.
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder ByteArray(final String sizeExpression) {
    return this.ByteArray(null, assertStringNotNull(sizeExpression));
  }

  /**
   * Add anonymous fixed size byte array.
   *
   * @param size size of the array, if negative then read stream till the end.
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder ByteArray(final int size) {
    return this.ByteArray(null, arraySizeToString(size));
  }

  /**
   * Add named fixed size byte array.
   *
   * @param name name of the array, it can be null fo anonymous fields
   * @param size size of the array, if negative then read stream till the end.
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder ByteArray(final String name, final int size) {
    return this.ByteArray(name, arraySizeToString(size));
  }

  /**
   * Add named byte array which size calculated through expression.
   *
   * @param name           name of the array, it can be null for anonymous fields
   * @param sizeExpression expression to be used to calculate array length, must not be null or empty.
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder ByteArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.BYTE_ARRAY, name, this.byteOrder);
    item.sizeExpression = assertStringNotNull(sizeExpression);
    this.items.add(item);
    return this;
  }

  /**
   * Add anonymous unsigned byte field.
   *
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder UByte() {
    return this.UByte(null);
  }

  /**
   * Add named unsigned byte field.
   *
   * @param name name of the field, can be null for anonymous one
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder UByte(final String name) {
    final Item item = new Item(BinType.UBYTE, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  /**
   * Added anonymous unsigned byte array which size calculated through expression.
   *
   * @param sizeExpression expression to be used to calculate size, must not be null or empty
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder UByteArray(final String sizeExpression) {
    return this.UByteArray(null, assertStringNotNull(sizeExpression));
  }

  /**
   * Add anonymous fixed size unsigned byte array.
   *
   * @param size size of the array, if negative then read stream till the end.
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder UByteArray(final int size) {
    return this.UByteArray(null, arraySizeToString(size));
  }

  /**
   * Add named fixed size unsigned byte array.
   *
   * @param name name of the field, it can be null for anonymous one
   * @param size size of the array, if negative then read stream till the end.
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder UByteArray(final String name, final int size) {
    return this.UByteArray(name, arraySizeToString(size));
  }

  /**
   * Add named unsigned byte array which size calculated through expression.
   *
   * @param name           name of the field, it can be null for anonymous one
   * @param sizeExpression expression to calculate array size, must ot be null or empty.
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder UByteArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.UBYTE_ARRAY, name, this.byteOrder);
    item.sizeExpression = assertStringNotNull(sizeExpression);
    this.items.add(item);
    return this;
  }

  /**
   * Added anonymous signed short field.
   *
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Short() {
    return this.Short(null);
  }

  /**
   * Add named signed short field.
   *
   * @param name name of the field, can be null for anonymous one
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Short(final String name) {
    final Item item = new Item(BinType.SHORT, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  /**
   * Add anonymous signed short array which size calculated through expression.
   *
   * @param sizeExpression expression to be used for calculation, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder ShortArray(final String sizeExpression) {
    return this.ShortArray(null, assertStringNotNull(sizeExpression));
  }

  /**
   * Add anonymous fixed size signed short array.
   *
   * @param size size of the array, if negative then stream will be read till end
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder ShortArray(final int size) {
    return this.ShortArray(null, arraySizeToString(size));
  }

  /**
   * Add named fixed size signed short array.
   *
   * @param name name of the field, if null then anonymous
   * @param size size of the array, if negative then stream will be read till end
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder ShortArray(final String name, final int size) {
    return this.ShortArray(name, arraySizeToString(size));
  }

  /**
   * Add named fixed signed short array which size calculated through expression.
   *
   * @param name           name of the field, if null then anonymous
   * @param sizeExpression expression to be used to calculate size, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder ShortArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.SHORT_ARRAY, name, this.byteOrder);
    item.sizeExpression = assertStringNotNull(sizeExpression);
    this.items.add(item);
    return this;
  }

  /**
   * Add anonymous unsigned short field.
   *
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder UShort() {
    return this.UShort(null);
  }

  /**
   * Add named unsigned short field.
   *
   * @param name name of the field, can be null for anonymous
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder UShort(final String name) {
    final Item item = new Item(BinType.USHORT, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  /**
   * Add anonymous unsigned short array which size calculated through expression.
   *
   * @param sizeExpression expression to be used for calculation, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder UShortArray(final String sizeExpression) {
    return this.UShortArray(null, assertStringNotNull(sizeExpression));
  }

  /**
   * Add named fixed unsigned short array which size calculated through expression.
   *
   * @param name           name of the field, if null then anonymous
   * @param sizeExpression expression to be used to calculate size, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder UShortArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.USHORT_ARRAY, name, this.byteOrder);
    item.sizeExpression = assertStringNotNull(sizeExpression);
    this.items.add(item);
    return this;
  }

  /**
   * Add named fixed size unsigned short array.
   *
   * @param name name of the field, if null then anonymous
   * @param size size of the array, if negative then stream will be read till end
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder UShortArray(final String name, final int size) {
    return this.UShortArray(name, arraySizeToString(size));
  }

  /**
   * Add anonymous integer field.
   *
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Int() {
    return this.Int(null);
  }

  /**
   * Add named integer field.
   *
   * @param name name of the field, can be null for anonymous
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Int(final String name) {
    final Item item = new Item(BinType.INT, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  /**
   * Add anonymous integer array with size calculated through expression.
   *
   * @param sizeExpression expression to be used to calculate size, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder IntArray(final String sizeExpression) {
    return this.IntArray(null, assertStringNotNull(sizeExpression));
  }

  /**
   * Add anonymous fixed size integer array.
   *
   * @param size size of the array, if negative then read stream till the end
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder IntArray(final int size) {
    return this.IntArray(null, arraySizeToString(size));
  }

  /**
   * Add named fixed size integer array.
   *
   * @param name name of field, it can be null for anonymous
   * @param size size of the array, if negative then read stream till the end
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder IntArray(final String name, final int size) {
    return this.IntArray(name, arraySizeToString(size));
  }

  /**
   * Add named integer array with size calculated through expression.
   *
   * @param name           name of field, can be nul for anonymous
   * @param sizeExpression expression to be used to calculate size, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder IntArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.INT_ARRAY, name, this.byteOrder);
    item.sizeExpression = assertStringNotNull(sizeExpression);
    this.items.add(item);
    return this;
  }

  /**
   * Add anonymous long field.
   *
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Long() {
    return this.Long(null);
  }

  /**
   * Add named long field.
   *
   * @param name name of the field, can be null for anonymous
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Long(final String name) {
    final Item item = new Item(BinType.LONG, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  /**
   * Add anonymous long array which size calculated through expression.
   *
   * @param sizeExpression expression to be used to calculate size, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder LongArray(final String sizeExpression) {
    return this.LongArray(null, assertStringNotNull(sizeExpression));
  }

  /**
   * Add anonymous fixed size long array.
   *
   * @param size size of array, if negative then read till stream end
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder LongArray(final int size) {
    return this.LongArray(null, arraySizeToString(size));
  }

  /**
   * Add named fixed size long array field.
   *
   * @param name name of field, can be null for anonymous
   * @param size size of array, if negative then read till stream end
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder LongArray(final String name, final int size) {
    return this.LongArray(name, arraySizeToString(size));
  }

  /**
   * Add named long array which size calculated through expression.
   *
   * @param name           name of the field, can be null for anonymous
   * @param sizeExpression expression to be used to calculate size, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder LongArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.LONG_ARRAY, name, this.byteOrder);
    item.sizeExpression = assertStringNotNull(sizeExpression);
    this.items.add(item);
    return this;
  }

  /**
   * Add anonymous float field.
   *
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Float() {
    return this.Float(null);
  }

  /**
   * Add named float field
   *
   * @param name name of the field, can be null for anonymous
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Float(final String name) {
    final Item item = new Item(BinType.FLOAT, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  /**
   * Add anonymous float array which size calculated through expression.
   *
   * @param sizeExpression expression to be used to calculate size, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder FloatArray(final String sizeExpression) {
    return this.FloatArray(null, assertStringNotNull(sizeExpression));
  }

  /**
   * Add anonymous fixed size float array.
   *
   * @param size size of array, if negative then read till stream end
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder FloatArray(final int size) {
    return this.FloatArray(null, arraySizeToString(size));
  }

  /**
   * Add named fixed size float array.
   *
   * @param name name of field, null for anonymous
   * @param size size of array, read till stream end if negative
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder FloatArray(final String name, final int size) {
    return this.FloatArray(name, arraySizeToString(size));
  }

  /**
   * Add named float array which size calculated through expression.
   *
   * @param name           name of the field, can be null for anonymous
   * @param sizeExpression expression to be used to calculate size, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder FloatArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.FLOAT_ARRAY, name, this.byteOrder);
    item.sizeExpression = assertStringNotNull(sizeExpression);
    this.items.add(item);
    return this;
  }

  /**
   * Add anonymous double field.
   *
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Double() {
    return this.Double(null);
  }

  /**
   * Add named double field.
   *
   * @param name name of the field, can be null for anonymous
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Double(final String name) {
    final Item item = new Item(BinType.DOUBLE, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  /**
   * Add comment.
   *
   * @param text text of comment, can be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Comment(final String text) {
    this.items.add(new ItemComment(text == null ? "" : text));
    return this;
  }

  /**
   * Add anonymous double array field which size calculated trough expression.
   *
   * @param sizeExpression expression to be used to calculate array size, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder DoubleArray(final String sizeExpression) {
    return this.DoubleArray(null, assertStringNotNull(sizeExpression));
  }

  /**
   * Add anonymous fixed size double array field.
   *
   * @param size size of the array, if negative then read till end of stream
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder DoubleArray(final int size) {
    return this.DoubleArray(null, arraySizeToString(size));
  }

  /**
   * Add named fixed size double array field.
   *
   * @param name ame of the field, can be null for anonymous
   * @param size size of the array, if negative then read till end of stream
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder DoubleArray(final String name, final int size) {
    return this.DoubleArray(name, arraySizeToString(size));
  }

  /**
   * Add named double array field which size calculated trough expression.
   *
   * @param name           name of the field, can be null for anonymous
   * @param sizeExpression expression to be used to calculate array size, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder DoubleArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.DOUBLE_ARRAY, name, this.byteOrder);
    item.sizeExpression = assertStringNotNull(sizeExpression);
    this.items.add(item);
    return this;
  }

  /**
   * Add anonymous string field.
   *
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder String() {
    return this.String(null);
  }

  /**
   * Add named string field.
   *
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder String(final String name) {
    final Item item = new Item(BinType.STRING, name, this.byteOrder);
    this.items.add(item);
    return this;
  }

  /**
   * Add anonymous string array which size calculated through expression.
   *
   * @param sizeExpression expression to calculate size, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder StringArray(final String sizeExpression) {
    return this.StringArray(null, assertStringNotNull(sizeExpression));
  }

  /**
   * Add anonymous fixed size string array.
   *
   * @param size size of array, if negative then read till stream end
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder StringArray(final int size) {
    return this.StringArray(null, arraySizeToString(size));
  }

  /**
   * Add named fixed size string array.
   *
   * @param name name of field, can be null for anonymous
   * @param size size of array, if negative then read till stream end
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder StringArray(final String name, final int size) {
    return this.StringArray(name, arraySizeToString(size));
  }

  /**
   * Add named string array which size calculated through expression.
   *
   * @param name           name of field, can be null for anonymous
   * @param sizeExpression expression to calculate size, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder StringArray(final String name, final String sizeExpression) {
    final Item item = new Item(BinType.STRING_ARRAY, name, this.byteOrder);
    item.sizeExpression = assertStringNotNull(sizeExpression);
    this.items.add(item);
    return this;
  }

  /**
   * Set byte order for next fields
   *
   * @param order order, if null then BIG_ENDIAN
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder ByteOrder(final JBBPByteOrder order) {
    this.byteOrder = order == null ? JBBPByteOrder.BIG_ENDIAN : order;
    return this;
  }

  /**
   * Build non-formatted script.
   *
   * @return script in non-formatted form, must not be null
   */
  public String build() {
    return this.build(false);
  }

  /**
   * Build a formatted script.
   *
   * @return script in formatted form, must not be null
   */
  public String build(final boolean format) {
    final StringBuilder buffer = new StringBuilder(128);

    int structCounter = 0;

    for (final Item item : this.items) {
      switch (item.type) {
        case STRUCT: {
          doTabs(format, buffer, structCounter).append(item.name == null ? "" : item.name).append('{');
          structCounter++;
        }
        break;
        case STRUCT_ARRAY: {
          doTabs(format, buffer, structCounter).append(item.name == null ? "" : item.name).append('[').append(item.sizeExpression).append(']').append('{');
          structCounter++;
        }
        break;
        case UNDEFINED: {
          if (item instanceof ItemStructEnd) {
            final ItemStructEnd structEnd = (ItemStructEnd) item;
            if (structEnd.endAll) {
              while (structCounter > 0) {
                structCounter--;
                doTabs(format, buffer, structCounter).append('}');
              }
            } else {
              if (structCounter == 0) {
                throw new IllegalStateException("Unexpected structure close");
              }
              structCounter--;
              doTabs(format, buffer, structCounter).append('}');
            }
          } else if (item instanceof ItemAlign) {
            doTabs(format, buffer, structCounter).append("align").append(item.sizeExpression == null ? "" : ':' + item.makeExpressionForExtraField(item.sizeExpression)).append(';');
          } else if (item instanceof ItemSkip) {
            doTabs(format, buffer, structCounter).append("skip").append(item.sizeExpression == null ? "" : ':' + item.makeExpressionForExtraField(item.sizeExpression)).append(';');
          } else if (item instanceof ItemComment) {
            doTabs(format, buffer, structCounter).append("// ").append(item.name.replace("\n", " "));
          } else {
            throw new IllegalArgumentException("Unexpected item : " + item.getClass().getName());
          }
        }
        break;
        default: {
          doTabs(format, buffer, structCounter).append(item.toString());
        }
        break;
      }
      if (format || item instanceof ItemComment) {
        buffer.append('\n');
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
        result.append('[').append(this.sizeExpression).append(']');
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
