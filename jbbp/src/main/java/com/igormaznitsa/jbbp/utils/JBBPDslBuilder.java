package com.igormaznitsa.jbbp.utils;

import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Auxiliary builder to build string JBBP script through sequent method call.
 * <b>NB! The Builder generates JBBP string script which can be compiled by parser!</b>
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
   * Value contains number of currently opened structures.
   */
  protected int openedStructCounter;

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

  protected static String assertExpressionChars(final String expression) {
    if (expression == null) {
      throw new NullPointerException("Expression is null");
    }

    if (expression.trim().length() == 0) {
      throw new IllegalArgumentException("Expression is empty");
    }

    if (expression.contains("//")) {
      throw new IllegalArgumentException("Comment is not allowed");
    }

    for (final char c : expression.toCharArray()) {
      switch (c) {
        case '\"':
        case ':':
        case ';':
        case '{':
        case '}':
        case '[':
        case ']':
          throw new IllegalArgumentException("Char is not allowed: " + c);
      }
    }


    return expression;
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
   * Get number of items added into internal item list.
   *
   * @return number of added items
   */
  public int size() {
    return this.items.size();
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
    this.items.add(new ItemAlign(assertExpressionChars(sizeExpression)));
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
    this.items.add(new ItemSkip(assertExpressionChars(sizeExpression)));
    return this;
  }

  /**
   * Add anonymous custom variable.
   *
   * @param type custom type, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Custom(final String type) {
    return this.Custom(type, null, null);
  }

  /**
   * Add named custom variable.
   *
   * @param type custom type, must not be null
   * @param name name of the field, can be null for anonymous
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Custom(final String type, final String name) {
    return this.Custom(type, name, null);
  }

  /**
   * Add named parametric custom variable.
   *
   * @param type  custom type, must not be null
   * @param name  name of the field, can be null for anonymous
   * @param param optional parameter for the field, can be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Custom(final String type, final String name, final String param) {
    final ItemCustom custom = new ItemCustom(type, name, this.byteOrder);
    custom.bitLenExpression = param == null ? null : assertExpressionChars(param);
    this.items.add(custom);
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
    this.openedStructCounter++;
    return this;
  }

  /**
   * Create anonymous structure array which size calculated by expression.
   *
   * @param sizeExpression expression to calculate array length, must not be null.
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder StructArray(final String sizeExpression) {
    return this.StructArray(null, assertExpressionChars(sizeExpression));
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
    item.sizeExpression = assertExpressionChars(sizeExpression);
    this.items.add(item);
    this.openedStructCounter++;
    return this;
  }

  /**
   * Create anonymous custom type array with fixed size.
   *
   * @param type custom type, must not be null
   * @param size size of the array, if less than zero then read till end of stream.
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder CustomArray(final String type, final int size) {
    return this.CustomArray(type, arraySizeToString(size));
  }

  /**
   * Create anonymous custom type array with fixed size.
   *
   * @param type custom type, must not be null
   * @param size expression to calculate size of the array, must not be null.
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder CustomArray(final String type, final String size) {
    return this.CustomArray(type, null, size, null);
  }

  /**
   * Create named custom type array with fixed size.
   *
   * @param type custom type, must not be null
   * @param name name of the array, can be null for anonymous one
   * @param size expression to calculate size of the array, must not be null.
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder CustomArray(final String type, final String name, final String size) {
    return this.CustomArray(type, name, size, null);
  }

  /**
   * Create named custom type array with fixed size.
   *
   * @param type custom type, must not be null
   * @param name name of the array, can be null for anonymous one
   * @param size size of he array, if less than zero then read till end of stream.
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder CustomArray(final String type, final String name, final int size) {
    return this.CustomArray(type, name, arraySizeToString(size), null);
  }

  /**
   * Create named custom type array which size calculated by expression.
   *
   * @param type  custom type, must not be null
   * @param name  name of the array, can be null for anonymous one
   * @param size  size of the array, if negative then read till the end of stream.
   * @param param optional parameter for the field, can be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder CustomArray(final String type, final String name, final int size, final String param) {
    return this.CustomArray(type, name, arraySizeToString(size), param);
  }

  /**
   * Create named custom type array which size calculated by expression.
   *
   * @param type           custom type, must not be null
   * @param name           name of the array, can be null for anonymous one
   * @param sizeExpression expression to calculate array length, must not be null.
   * @param param          optional parameter for the field, can be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder CustomArray(final String type, final String name, final String sizeExpression, final String param) {
    final ItemCustom item = new ItemCustom(type, name, this.byteOrder);
    item.array = true;
    item.bitLenExpression = param == null ? param : assertExpressionChars(param);
    item.sizeExpression = assertExpressionChars(sizeExpression);
    this.items.add(item);
    return this;
  }

  /**
   * Add directive to close currently opened structure or a structure array.
   *
   * @return the builder instance, must not be null
   * @throws IllegalStateException if there is not any opened struct
   */
  public JBBPDslBuilder CloseStruct() {
    return this.CloseStruct(false);
  }

  /**
   * Add directive to close currently opened structure or all opened structures.
   *
   * @param closeAllOpened flag to close all opened structures if true, false if close only last opened structure
   * @return the builder instance, must not be null
   * @throws IllegalStateException if there is not any opened struct
   */
  public JBBPDslBuilder CloseStruct(final boolean closeAllOpened) {
    if (this.openedStructCounter == 0) {
      throw new IllegalStateException("There is not any opened struct");
    }
    this.items.add(new ItemStructEnd(closeAllOpened));
    this.openedStructCounter = closeAllOpened ? 0 : this.openedStructCounter - 1;
    return this;
  }

  /**
   * Allows to check that there is an opened structure.
   *
   * @return true if there is any opened structure, false otherwise.
   */
  public boolean hasOpenedStructs() {
    return this.openedStructCounter > 0;
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
   *
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
   * Add named bit field which length calculated by expression.
   *
   * @param name             name of the field, if null then anonymous one
   * @param bitLenExpression expression to calculate number of bits, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder Bits(final String name, final String bitLenExpression) {
    final Item item = new Item(BinType.BIT, name, this.byteOrder);
    item.bitLenExpression = assertExpressionChars(bitLenExpression);
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
   * Add named fixed length bit array, size of one bit field is calculated by expression.
   *
   * @param name             name of the array, if null then anonymous one
   * @param bitLenExpression expression to calculate length of the bit field, must not be null
   * @param size             number of elements in array, if negative then till the end of stream
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder BitArray(final String name, final String bitLenExpression, final int size) {
    return this.BitArray(name, bitLenExpression, arraySizeToString(size));
  }

  /**
   * Add anonymous bit array with size calculated through expression.
   *
   * @param bits           length of the field, must not be null
   * @param sizeExpression expression to be used to calculate array size, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder BitArray(final JBBPBitNumber bits, final String sizeExpression) {
    return this.BitArray(null, bits, assertExpressionChars(sizeExpression));
  }

  /**
   * Add anonymous bit array with size calculated through expression.
   *
   * @param bitLenExpression expression to calculate length of the bit field, must not be null
   * @param sizeExpression   expression to be used to calculate array size, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder BitArray(final String bitLenExpression, final String sizeExpression) {
    return this.BitArray(null, bitLenExpression, assertExpressionChars(sizeExpression));
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
    item.sizeExpression = assertExpressionChars(sizeExpression);
    this.items.add(item);
    return this;
  }

  /**
   * Add named bit array where each bit length is calculated through expression.
   *
   * @param name             name of the array, if null then anonymous one
   * @param bitLenExpression expression to calculate length of the bit field, must not be null
   * @param sizeExpression   expression to be used to calculate array size, must not be null
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder BitArray(final String name, final String bitLenExpression, final String sizeExpression) {
    final Item item = new Item(BinType.BIT_ARRAY, name, this.byteOrder);
    item.bitLenExpression = assertExpressionChars(bitLenExpression);
    item.sizeExpression = assertExpressionChars(sizeExpression);
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
    return this.BoolArray(null, assertExpressionChars(sizeExpression));
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
    item.sizeExpression = assertExpressionChars(sizeExpression);
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
    return this.ByteArray(null, assertExpressionChars(sizeExpression));
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
    item.sizeExpression = assertExpressionChars(sizeExpression);
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
    return this.UByteArray(null, assertExpressionChars(sizeExpression));
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
    item.sizeExpression = assertExpressionChars(sizeExpression);
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
    return this.ShortArray(null, assertExpressionChars(sizeExpression));
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
    item.sizeExpression = assertExpressionChars(sizeExpression);
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
    return this.UShortArray(null, assertExpressionChars(sizeExpression));
  }

  /**
   * Add anonymous fixed size unsigned short array.
   *
   * @param size sizeof the array, if negative then read till the end of stream
   * @return the builder instance, must not be null
   */
  public JBBPDslBuilder UShortArray(final int size) {
    return this.UShortArray(null, arraySizeToString(size));
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
    item.sizeExpression = assertExpressionChars(sizeExpression);
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
    return this.IntArray(null, assertExpressionChars(sizeExpression));
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
    item.sizeExpression = assertExpressionChars(sizeExpression);
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
    return this.LongArray(null, assertExpressionChars(sizeExpression));
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
    item.sizeExpression = assertExpressionChars(sizeExpression);
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
    return this.FloatArray(null, assertExpressionChars(sizeExpression));
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
    item.sizeExpression = assertExpressionChars(sizeExpression);
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
    return this.DoubleArray(null, assertExpressionChars(sizeExpression));
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
    item.sizeExpression = assertExpressionChars(sizeExpression);
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
    return this.StringArray(null, assertExpressionChars(sizeExpression));
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
    item.sizeExpression = assertExpressionChars(sizeExpression);
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
   * @throws IllegalStateException if there is an unclosed struct
   */
  public String End() {
    return this.End(false);
  }

  /**
   * Build a formatted script.
   *
   * @param format if true then make some formatting of result, false if unformatted version allowed
   * @return script in formatted form, must not be null
   * @throws IllegalStateException if there is an unclosed struct
   */
  public String End(final boolean format) {
    if (this.openedStructCounter != 0) {
      throw new IllegalStateException("Detected unclosed structs: " + this.openedStructCounter);
    }

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
                if (structCounter > 0 && format) {
                  buffer.append('\n');
                }
              }
            } else {
              structCounter--;
              doTabs(format, buffer, structCounter).append('}');
            }
          } else if (item instanceof ItemCustom) {
            doTabs(format, buffer, structCounter).append(item.toString());
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

    return buffer.toString();
  }

  /**
   * Allows to collect all fields which can be used for scripting.
   *
   * @param annotatedClass class to be processed, must not be null
   * @return container which contains all found items
   */
  protected BinFieldContainer collectAnnotatedFields(final Class<?> annotatedClass) {
    final Bin defautBin = annotatedClass.getAnnotation(Bin.class);
    final BinFieldContainer result = new BinFieldContainer(annotatedClass, defautBin, null);

    final Class<?> parent = annotatedClass.getSuperclass();

    if (parent != null && parent != Object.class) {
      final BinFieldContainer parentFields = collectAnnotatedFields(parent);
      if (!parentFields.fields.isEmpty()) {
        result.addAllFromContainer(parentFields);
      }
    }

    for (final Field f : annotatedClass.getDeclaredFields()) {
      if ((f.getModifiers() & (Modifier.NATIVE | Modifier.STATIC | Modifier.FINAL | Modifier.PRIVATE | Modifier.TRANSIENT)) == 0) {
        final Bin binAnno = f.getAnnotation(Bin.class);
        if (binAnno != null || defautBin != null) {
          final Class<?> type = f.getType().isArray() ? f.getType().getComponentType() : f.getType();
          if (type.isPrimitive() || type == String.class) {
            final Bin foundBin = binAnno == null ? defautBin : binAnno;
            result.addField(foundBin, f);
          } else {
            final BinFieldContainer container = collectAnnotatedFields(type);
            if (!container.fields.isEmpty()) {
              if (binAnno != null) {
                container.bin = binAnno;
              }
              container.field = f;
              result.addContaner(container);
            }
          }
        }
      }
    }

    result.sort();

    if (!result.fields.isEmpty()) {
      result.addContaner(BinFieldContainer.END_STRUCT);
    }

    return result;
  }

  /**
   * Convert an annotated class into its JBBP DSL representation.
   *
   * @param annotatedClass class to be converted into JBBP script, must not be null
   * @return the builder instance, must not be null
   * @see com.igormaznitsa.jbbp.mapper.Bin
   */
  public JBBPDslBuilder AnnotatedClass(final Class<?> annotatedClass) {
    return addAnnotatedClass(annotatedClass, false);
  }

  /**
   * Add just fields of annotated class, outbound class will not be added as structure.
   *
   * @param annotatedClass class to be converted into JBBP script, must not be null
   * @return the builder instance, must not be null
   * @see com.igormaznitsa.jbbp.mapper.Bin
   */
  public JBBPDslBuilder AnnotatedClassFields(final Class<?> annotatedClass) {
    return addAnnotatedClass(annotatedClass, true);
  }

  protected JBBPDslBuilder addAnnotatedClass(final Class<?> annotatedClass, final boolean onlyFields) {
    final BinFieldContainer collected = collectAnnotatedFields(annotatedClass);

    final JBBPByteOrder old = this.byteOrder;
    this.byteOrder = JBBPByteOrder.BIG_ENDIAN;

    class Pair {
      final BinFieldContainer container;
      final Iterator<BinField> iter;

      Pair(final BinFieldContainer container) {
        this.container = container;
        this.iter = container.fields.iterator();
      }
    }

    if (onlyFields) {
      int indexOfLastEndStruct = -1;
      for (int i = collected.fields.size() - 1; i >= 0; i--) {
        if (collected.fields.get(i) == BinFieldContainer.END_STRUCT) {
          indexOfLastEndStruct = i;
          break;
        }
      }
      if (indexOfLastEndStruct >= 0) {
        collected.fields.remove(indexOfLastEndStruct);
      } else {
        throw new IllegalStateException("Can't find end of structure");
      }
    } else {
      this.Struct(collected.getName());
    }

    final List<Pair> stack = new ArrayList<Pair>();
    stack.add(new Pair(collected));

    while (!stack.isEmpty()) {
      final Pair pair = stack.remove(0);
      while (pair.iter.hasNext()) {
        final BinField field = pair.iter.next();
        if (field instanceof BinFieldContainer) {
          final BinFieldContainer conty = (BinFieldContainer) field;
          if (conty == BinFieldContainer.END_STRUCT) {
            this.CloseStruct();
          } else {
            if (field.isArray()) {
              this.StructArray(conty.getName(), conty.bin.extra());
            } else {
              this.Struct(conty.getName());
            }
            stack.add(0, pair);
            stack.add(0, new Pair(conty));
            break;
          }
        } else {
          final BinType type = field.findType();
          this.ByteOrder(pair.container.getByteOrder(field));
          switch (type) {
            case BIT_ARRAY: {
              this.BitArray(field.getName(), pair.container.getBitNumber(field), field.bin.extra());
            }
            break;
            case BIT: {
              this.Bits(field.getName(), pair.container.getBitNumber(field));
            }
            break;
            case BOOL: {
              this.Bool(field.getName());
            }
            break;
            case BOOL_ARRAY: {
              this.BoolArray(field.getName(), field.bin.extra());
            }
            break;
            case BYTE: {
              this.Byte(field.getName());
            }
            break;
            case BYTE_ARRAY: {
              this.ByteArray(field.getName(), field.bin.extra());
            }
            break;
            case UBYTE: {
              this.UByte(field.getName());
            }
            break;
            case UBYTE_ARRAY: {
              this.UByteArray(field.getName(), field.bin.extra());
            }
            break;
            case SHORT: {
              this.Short(field.getName());
            }
            break;
            case SHORT_ARRAY: {
              this.ShortArray(field.getName(), field.bin.extra());
            }
            break;
            case USHORT: {
              this.UShort(field.getName());
            }
            break;
            case USHORT_ARRAY: {
              this.UShortArray(field.getName(), field.bin.extra());
            }
            break;
            case INT: {
              this.Int(field.getName());
            }
            break;
            case INT_ARRAY: {
              this.IntArray(field.getName(), field.bin.extra());
            }
            break;
            case LONG: {
              this.Long(field.getName());
            }
            break;
            case LONG_ARRAY: {
              this.LongArray(field.getName(), field.bin.extra());
            }
            break;
            case FLOAT: {
              this.Float(field.getName());
            }
            break;
            case FLOAT_ARRAY: {
              this.FloatArray(field.getName(), field.bin.extra());
            }
            break;
            case DOUBLE: {
              this.Double(field.getName());
            }
            break;
            case DOUBLE_ARRAY: {
              this.DoubleArray(field.getName(), field.bin.extra());
            }
            break;
            case STRING: {
              this.String(field.getName());
            }
            break;
            case STRING_ARRAY: {
              this.StringArray(field.getName(), field.bin.extra());
            }
            break;
            default:
              throw new Error("Unexpected type:" + type);
          }
        }
      }
    }

    this.byteOrder = old;

    return this;
  }

  protected static class ItemCustom extends Item {
    final String customType;
    boolean array;

    ItemCustom(final String customType, final String name, final JBBPByteOrder byteOrder) {
      super(BinType.UNDEFINED, name, byteOrder);
      this.customType = customType;
    }
  }

  protected static class Item {
    final BinType type;
    final String name;
    final JBBPByteOrder byteOrder;
    String sizeExpression;
    JBBPBitNumber bitNumber;
    String bitLenExpression;

    Item(final BinType type, final String name, final JBBPByteOrder byteOrder) {
      this.type = type;
      this.name = name;
      this.byteOrder = byteOrder;
    }

    @Override
    public String toString() {
      String type;
      boolean isArray = false;
      boolean customType = this instanceof ItemCustom;

      if (customType) {
        type = ((ItemCustom) this).customType;
        isArray = ((ItemCustom) this).array;
      } else {
        type = this.type.name().toLowerCase(Locale.ENGLISH);
        isArray = type.endsWith("_array");

        if (isArray) {
          type = type.substring(0, type.indexOf('_'));
        }

        if (type.equals("string") || type.equals("float") || type.equals("double")) {
          type += 'j';
        }
      }

      final StringBuilder result = new StringBuilder();

      if (this.byteOrder == JBBPByteOrder.LITTLE_ENDIAN) {
        result.append('<');
      }

      result.append(type);

      if (customType) {
        if (this.bitLenExpression != null) {
          result.append(':').append(makeExpressionForExtraField(this.bitLenExpression));
        }
      } else if (this.type == BinType.BIT || this.type == BinType.BIT_ARRAY) {
        result.append(':');
        if (bitLenExpression == null) {
          if (this.bitNumber == null) {
            result.append('1');
          } else {
            result.append(this.bitNumber.getBitNumber());
          }
        } else {
          result.append(makeExpressionForExtraField(this.bitLenExpression));
        }
      }

      if (isArray) {
        result.append('[').append(this.sizeExpression).append(']');
      }

      if (this.name != null && this.name.length() != 0) {
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

  protected static class BinField implements Comparable<BinField> {

    Bin bin;
    Field field;

    BinField(final Bin bin, final Field field) {
      this.bin = bin;
      this.field = field;
    }

    boolean isArray() {
      return this.field == null ? false : this.field.getType().isArray();
    }

    BinType findType() {
      if (this.field == null) {
        return BinType.STRUCT;
      }
      return this.bin.type() == BinType.UNDEFINED ? BinType.findCompatible(this.field.getType()) : this.bin.type();
    }

    String getName() {
      if (this.field == null) {
        return null;
      }

      return this.bin == null ?
          this.field.getName() :
          this.bin.name().length() == 0 ?
              this.field.getName() :
              this.bin.name();
    }

    @Override
    public int compareTo(final BinField that) {
      final int thisOrder = this.bin == null ? 0 : this.bin.outOrder();
      final int thatOrder = that.bin == null ? 0 : that.bin.outOrder();

      return thisOrder == thatOrder ? 0 : (thisOrder < thatOrder ? -1 : 1);
    }
  }

  protected static class BinFieldContainer extends BinField {
    final List<BinField> fields = new ArrayList<BinField>();
    final Class<?> klazz;

    static BinFieldContainer END_STRUCT = new BinFieldContainer(null, null, null);

    BinFieldContainer(final Class<?> klazz, final Bin bin, final Field field) {
      super(bin, field);
      this.klazz = klazz;
    }

    void sort() {
      Collections.sort(this.fields);
    }

    void addAllFromContainer(final BinFieldContainer container) {
      this.fields.addAll(container.fields);
    }

    void addContaner(final BinFieldContainer container) {
      this.fields.add(container);
    }

    void addField(final Bin bin, final Field field) {
      this.fields.add(new BinField(bin, field));
    }

    JBBPByteOrder getByteOrder(final BinField field) {
      return field.bin.outByteOrder();
    }

    String getName() {
      final String name = super.getName();
      return name == null ? this.klazz.getSimpleName() : name;
    }

    JBBPBitNumber getBitNumber(final BinField field) {
      return field.bin.outBitNumber() == JBBPBitNumber.BITS_8 ? this.bin.outBitNumber() : field.bin.outBitNumber();
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
