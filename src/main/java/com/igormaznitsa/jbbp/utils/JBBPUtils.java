/* 
 * Copyright 2014 Igor Maznitsa (http://www.igormaznitsa.com).
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
package com.igormaznitsa.jbbp.utils;

import com.igormaznitsa.jbbp.io.*;
import com.igormaznitsa.jbbp.model.JBBPAbstractField;
import java.io.*;
import java.lang.reflect.AccessibleObject;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Misc auxiliary methods to be used in the framework.
 *
 * @since 1.0
 */
public enum JBBPUtils {

  ;

  /**
   * Inside auxiliary class to make makeAccessible as a privileged action.
   * @since 1.1
   */
  private static final class PrivilegedProcessor implements PrivilegedAction<AccessibleObject> {

    private AccessibleObject theObject;

    public void setAccessibleObject(final AccessibleObject obj) {
      this.theObject = obj;
    }

    public AccessibleObject run() {
      final AccessibleObject objectToProcess = this.theObject;
      this.theObject = null;
      if (objectToProcess != null) {
        objectToProcess.setAccessible(true);
      }
      return objectToProcess;
    }
  }

  /**
   * Inside auxiliary queue for privileged processors to avoid mass creation of
   * processors.
   *
   * @since 1.1
   */
  private static final Queue<PrivilegedProcessor> processorsQueue = new ArrayBlockingQueue<PrivilegedProcessor>(32);

  /**
   * Make accessible an accessible object, AccessController.doPrivileged will be
   * called.
   *
   * @param obj an object to make accessible, it can be null.
   * @since 1.1
   * @see AccessController#doPrivileged(java.security.PrivilegedAction)
   */
  public static void makeAccessible(final AccessibleObject obj) {
    if (obj != null) {
      PrivilegedProcessor processor = processorsQueue.poll();
      if (processor == null) {
        processor = new PrivilegedProcessor();
      }
      processor.setAccessibleObject(obj);
      AccessController.doPrivileged(processor);
      processorsQueue.offer(processor);
    }
  }

  /**
   * Check that a string is a number.
   *
   * @param num a string to be checked, it can be null
   * @return true if the string represents a number, false if it is not number
   * or it is null
   */
  public static boolean isNumber(final String num) {
    if (num == null || num.length() == 0) {
      return false;
    }
    final boolean firstIsDigit = Character.isDigit(num.charAt(0));
    if (!firstIsDigit && num.charAt(0) != '-') {
      return false;
    }
    boolean dig = firstIsDigit;
    for (int i = 1; i < num.length(); i++) {
      if (!Character.isDigit(num.charAt(i))) {
        return false;
      }
      dig = true;
    }
    return dig;
  }

  /**
   * Pack an integer value as a byte array.
   *
   * @param value a code to be packed
   * @return a byte array contains the packed code
   */
  public static byte[] packInt(final int value) {
    if ((value & 0xFFFFFF80) == 0) {
      return new byte[]{(byte) value};
    }
    else if ((value & 0xFFFF0000) == 0) {
      return new byte[]{(byte) 0x80, (byte) (value >>> 8), (byte) value};
    }
    else {
      return new byte[]{(byte) 0x81, (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value};
    }
  }

  /**
   * Pack an integer value and save that into a byte array since defined
   * position.
   *
   * @param array a byte array where to write the packed data, it must not be
   * null
   * @param position the position of the first byte of the packed value, it must
   * not be null
   * @param value the value to be packed
   * @return number of bytes written into the array, the position will be
   * increased
   */
  public static int packInt(final byte[] array, final JBBPIntCounter position, final int value) {
    if ((value & 0xFFFFFF80) == 0) {
      array[position.getAndIncrement()] = (byte) value;
      return 1;
    }
    else if ((value & 0xFFFF0000) == 0) {
      array[position.getAndIncrement()] = (byte) 0x80;
      array[position.getAndIncrement()] = (byte) (value >>> 8);
      array[position.getAndIncrement()] = (byte) value;
      return 3;
    }
    array[position.getAndIncrement()] = (byte) 0x81;
    array[position.getAndIncrement()] = (byte) (value >>> 24);
    array[position.getAndIncrement()] = (byte) (value >>> 16);
    array[position.getAndIncrement()] = (byte) (value >>> 8);
    array[position.getAndIncrement()] = (byte) value;
    return 5;
  }

  /**
   * Unpack an integer value from defined position in a byte array.
   *
   * @param array the source byte array
   * @param position the position of the first byte of packed value
   * @return the unpacked value, the position will be increased
   */
  public static int unpackInt(final byte[] array, final JBBPIntCounter position) {
    final int code = array[position.getAndIncrement()] & 0xFF;
    if (code < 0x80) {
      return code;
    }

    final int result;
    switch (code) {
      case 0x80: {
        result = ((array[position.getAndIncrement()] & 0xFF) << 8) | (array[position.getAndIncrement()] & 0xFF);
      }
      break;
      case 0x81: {
        result = ((array[position.getAndIncrement()] & 0xFF) << 24)
                | ((array[position.getAndIncrement()] & 0xFF) << 16)
                | ((array[position.getAndIncrement()] & 0xFF) << 8)
                | (array[position.getAndIncrement()] & 0xFF);
      }
      break;
      default:
        throw new IllegalArgumentException("Unsupported packed integer prefix [0x" + Integer.toHexString(code).toUpperCase(Locale.ENGLISH) + ']');
    }
    return result;
  }

  /**
   * A Byte array into its hex string representation
   *
   * @param array an array to be converted
   * @return a string of hex representations of values from the array
   */
  public static String array2hex(final byte[] array) {
    return byteArray2String(array, "0x", ", ", true, 16);
  }

  /**
   * A Byte array into its bin string representation
   *
   * @param array an array to be converted
   * @return a string of bin representations of values from the array
   */
  public static String array2bin(final byte[] array) {
    return byteArray2String(array, "0b", ", ", true, 2);
  }

  /**
   * A Byte array into its octal string representation
   *
   * @param array an array to be converted
   * @return a string of octal representations of values from the array
   */
  public static String array2oct(final byte[] array) {
    return byteArray2String(array, "0o", ", ", true, 8);
  }

  /**
   * Convert a byte array into string representation
   *
   * @param array the array to be converted, it must not be null
   * @param prefix the prefix for each converted value, it can be null
   * @param delimiter the delimeter for string representations
   * @param brackets if true then place the result into square brackets
   * @param radix the base for conversion
   * @return the string representation of the byte array
   */
  public static String byteArray2String(final byte[] array, final String prefix, final String delimiter, final boolean brackets, final int radix) {
    if (array == null) {
      return null;
    }

    final int maxlen = Integer.toString(0xFF, radix).length();
    final String zero = "00000000";

    final String normDelim = delimiter == null ? " " : delimiter;
    final String normPrefix = prefix == null ? "" : prefix;

    final StringBuilder result = new StringBuilder(array.length * 4);

    if (brackets) {
      result.append('[');
    }

    boolean nofirst = false;

    for (final byte b : array) {
      if (nofirst) {
        result.append(normDelim);
      }
      else {
        nofirst = true;
      }

      result.append(normPrefix);

      final String v = Integer.toString(b & 0xFF, radix);
      if (v.length() < maxlen) {
        result.append(zero.substring(0, maxlen - v.length()));
      }
      result.append(v.toUpperCase(Locale.ENGLISH));
    }

    if (brackets) {
      result.append(']');
    }

    return result.toString();
  }

  /**
   * Reverse bits in a byte.
   *
   * @param value a byte value which bits must be reversed.
   * @return the reversed version of the byte
   * @since 1.1
   */
  public static byte reverseBitsInByte(final byte value) {
    final int v = value & 0xFF;
    return (byte) (((v * 0x0802 & 0x22110) | (v * 0x8020 & 0x88440)) * 0x10101 >> 16);
  }

  /**
   * Reverse lower part of a byte defined by bits number constant.
   *
   * @param bitNumber number of lowest bits to be reversed, must not be null
   * @param value a byte to be processed
   * @return value contains reversed number of lowest bits of the byte
   */
  public static byte reverseBitsInByte(final JBBPBitNumber bitNumber, final byte value) {
    final byte reversed = reverseBitsInByte(value);
    return (byte) ((reversed >>> (8 - bitNumber.getBitNumber())) & bitNumber.getMask());
  }

  /**
   * Convert a byte array into string binary representation with LSB0 order.
   *
   * @param values a byte array to be converted
   * @return the string representation of the array
   */
  public static String bin2str(final byte[] values) {
    return bin2str(values, JBBPBitOrder.LSB0, false);
  }

  /**
   * Convert a byte array into string binary representation with LSB0 order and
   * possibility to separate bytes.
   *
   * @param values a byte array to be converted
   * @param separateBytes if true then bytes will be separated by spaces
   * @return the string representation of the array
   */
  public static String bin2str(final byte[] values, final boolean separateBytes) {
    return bin2str(values, JBBPBitOrder.LSB0, separateBytes);
  }

  /**
   * Convert a byte array into string binary representation with defined bit
   * order and possibility to separate bytes.
   *
   * @param values a byte array to be converted
   * @param bitOrder the bit order for byte decoding
   * @param separateBytes if true then bytes will be separated by spaces
   * @return the string representation of the array
   */
  public static String bin2str(final byte[] values, final JBBPBitOrder bitOrder, final boolean separateBytes) {
    if (values == null) {
      return null;
    }

    final StringBuilder result = new StringBuilder(values.length * (separateBytes ? 9 : 8));

    boolean nofirst = false;
    for (final byte b : values) {
      if (separateBytes) {
        if (nofirst) {
          result.append(' ');
        }
        else {
          nofirst = true;
        }
      }

      int a = b;

      if (bitOrder == JBBPBitOrder.MSB0) {
        for (int i = 0; i < 8; i++) {
          result.append((a & 0x1) == 0 ? '0' : '1');
          a >>= 1;
        }
      }
      else {
        for (int i = 0; i < 8; i++) {
          result.append((a & 0x80) == 0 ? '0' : '1');
          a <<= 1;
        }
      }
    }

    return result.toString();
  }

  /**
   * Convert array of JBBP fields into a list.
   *
   * @param fields an array of fields, must not be null
   * @return a list of JBBP fields
   */
  public static List<JBBPAbstractField> fieldsAsList(final JBBPAbstractField... fields) {
    final List<JBBPAbstractField> result = new ArrayList<JBBPAbstractField>();
    for (final JBBPAbstractField f : fields) {
      result.add(f);
    }
    return result;
  }

  /**
   * Convert string representation of binary data into byte array with LSB0 bit
   * order.
   *
   * @param values a string represents binary data
   * @return a byte array generated from the decoded string, empty array for
   * null string
   */
  public static byte[] str2bin(final String values) {
    return str2bin(values, JBBPBitOrder.LSB0);
  }

  /**
   * Convert string representation of binary data into byte array/
   *
   * @param values a string represents binary data
   * @param bitOrder the bit order to be used for operation
   * @return a byte array generated from the decoded string, empty array for
   * null string
   */
  public static byte[] str2bin(final String values, final JBBPBitOrder bitOrder) {
    if (values == null) {
      return new byte[0];
    }

    int buff = 0;
    int cnt = 0;

    final ByteArrayOutputStream buffer = new ByteArrayOutputStream((values.length() + 7) >> 3);

    final boolean msb0 = bitOrder == JBBPBitOrder.MSB0;

    for (final char v : values.toCharArray()) {
      switch (v) {
        case '_':
        case ' ':
          continue;
        case '0':
        case 'X':
        case 'x':
        case 'Z':
        case 'z': {
          if (msb0) {
            buff >>= 1;
          }
          else {
            buff <<= 1;
          }
        }
        break;
        case '1': {
          if (msb0) {
            buff = (buff >> 1) | 0x80;
          }
          else {
            buff = (buff << 1) | 1;
          }
        }
        break;
        default:
          throw new IllegalArgumentException("Detected unsupported char '" + v + ']');
      }
      cnt++;
      if (cnt == 8) {
        buffer.write(buff);
        cnt = 0;
        buff = 0;
      }
    }
    if (cnt > 0) {
      buffer.write(msb0 ? buff >>> (8 - cnt) : buff);
    }
    return buffer.toByteArray();
  }

  /**
   * Split a string for a char used as the delimeter.
   *
   * @param str a string to be split
   * @param splitChar a char to be used as delimeter
   * @return array contains split string parts without delimeter chars
   */
  public static String[] splitString(final String str, final char splitChar) {
    final int length = str.length();
    final StringBuilder bulder = new StringBuilder(Math.max(8, length));

    int counter = 1;
    for (int i = 0; i < length; i++) {
      if (str.charAt(i) == splitChar) {
        counter++;
      }
    }

    final String[] result = new String[counter];

    int position = 0;
    for (int i = 0; i < length; i++) {
      final char chr = str.charAt(i);
      if (chr == splitChar) {
        result[position++] = bulder.toString();
        bulder.setLength(0);
      }
      else {
        bulder.append(chr);
      }
    }
    if (position < result.length) {
      result[position] = bulder.toString();
    }

    return result;
  }

  /**
   * Check that an object is null and throw NullPointerException in the case.
   *
   * @param object an object to be checked
   * @param message message to be used as the exception message
   * @throws NullPointerException it will be thrown if the object is null
   */
  public static void assertNotNull(final Object object, final String message) {
    if (object == null) {
      throw new NullPointerException(message == null ? "Object is null" : message);
    }
  }

  /**
   * Convert an integer number into human readable hexadecimal format.
   *
   * @param number a number to be converted
   * @return a string with human readable hexadecimal number representation
   */
  public static String int2msg(final int number) {
    return number + " (0x" + Long.toHexString((long) number & 0xFFFFFFFFL).toUpperCase(Locale.ENGLISH) + ')';
  }

  /**
   * Normalize field name or path.
   *
   * @param nameOrPath a field name or a path to be normalized, must not be null
   * @return the normalized version of the name or path
   */
  public static String normalizeFieldNameOrPath(final String nameOrPath) {
    assertNotNull(nameOrPath, "Name of path must not be null");
    return nameOrPath.trim().toLowerCase(Locale.ENGLISH);
  }

  /**
   * Quiet closing of a closeable object.
   *
   * @param closeable a closeable object, can be null
   */
  public static void closeQuietly(final Closeable closeable) {
    try {
      if (closeable != null) {
        closeable.close();
      }
    }
    catch (IOException ex) {
      // Keep silence
    }
  }

  /**
   * Convert chars of a string into a byte array contains the unicode codes.
   *
   * @param byteOrder the byte order for the operation, must not be null
   * @param str the string which chars should be written, must not be null
   * @return the byte array contains unicodes of the string written as byte
   * pairs
   * @since 1.1
   */
  public static byte[] str2UnicodeByteArray(final JBBPByteOrder byteOrder, final String str) {
    final byte[] result = new byte[str.length() << 1];
    int index = 0;
    for (int i = 0; i < str.length(); i++) {
      final int val = str.charAt(i);
      switch (byteOrder) {
        case BIG_ENDIAN: {
          result[index++] = (byte) (val >> 8);
          result[index++] = (byte) val;
        }
        break;
        case LITTLE_ENDIAN: {
          result[index++] = (byte) val;
          result[index++] = (byte) (val >> 8);
        }
        break;
        default:
          throw new Error("Unexpected byte order [" + byteOrder + ']');
      }
    }
    return result;
  }

  /**
   * Reverse order of bytes in a byte array.
   *
   * @param nullableArrayToBeInverted a byte array which order must be reversed,
   * it can be null
   * @return the same array instance but with reversed byte order, null if the
   * source array is null
   * @since 1.1
   */
  public static byte[] reverseArray(final byte[] nullableArrayToBeInverted) {
    if (nullableArrayToBeInverted != null && nullableArrayToBeInverted.length > 0) {
      int indexStart = 0;
      int indexEnd = nullableArrayToBeInverted.length - 1;
      while (indexStart < indexEnd) {
        final byte a = nullableArrayToBeInverted[indexStart];
        nullableArrayToBeInverted[indexStart] = nullableArrayToBeInverted[indexEnd];
        nullableArrayToBeInverted[indexEnd] = a;
        indexStart++;
        indexEnd--;
      }
    }
    return nullableArrayToBeInverted;
  }

  /**
   * Split an integer value to bytes and returns as a byte array.
   *
   * @param value a value to be split
   * @param valueInLittleEndian the flag shows that the integer is presented in
   * the little endian form
   * @param buffer a buffer array to be used as a storage, if the array is null
   * or its length is less than 4 then new array will be created
   * @return the same array filled by parts of the integer value or new array if
   * the provided buffer is null or has not enough size
   * @since 1.1
   */
  public static byte[] splitInteger(final int value, final boolean valueInLittleEndian, final byte[] buffer) {
    final byte[] result;
    if (buffer == null || buffer.length < 4) {
      result = new byte[4];
    }
    else {
      result = buffer;
    }
    int tmpvalue = value;
    if (valueInLittleEndian) {
      for (int i = 0; i < 4; i++) {
        result[i] = (byte) tmpvalue;
        tmpvalue >>>= 8;
      }
    }
    else {
      for (int i = 3; i >= 0; i--) {
        result[i] = (byte) tmpvalue;
        tmpvalue >>>= 8;
      }
    }
    return result;
  }

  /**
   * Split a long value to its bytes and returns the parts as an array.
   *
   * @param value the value to be split
   * @param valueInLittleEndian the flag shows that the long is presented in the
   * little endian for
   * @param buffer a buffer array to be used as a storage, if the array is null
   * or its length is less than 8 then new array will be created
   * @return the same array filled by parts of the integer value or new array if
   * the provided buffer is null or has not enough size
   * @since 1.1
   */
  public static byte[] splitLong(final long value, final boolean valueInLittleEndian, final byte[] buffer) {
    final byte[] result;
    if (buffer == null || buffer.length < 8) {
      result = new byte[8];
    }
    else {
      result = buffer;
    }
    long tmpvalue = value;
    if (valueInLittleEndian) {
      for (int i = 0; i < 8; i++) {
        result[i] = (byte) tmpvalue;
        tmpvalue >>>= 8;
      }
    }
    else {
      for (int i = 7; i >= 0; i--) {
        result[i] = (byte) tmpvalue;
        tmpvalue >>>= 8;
      }
    }
    return result;
  }

  /**
   * Concatenate byte arrays into one byte array sequentially.
   *
   * @param arrays arrays to be concatenated
   * @return the result byte array contains concatenated source arrays
   * @since 1.1
   */
  public static byte[] concat(final byte[]... arrays) {
    int len = 0;
    for (final byte[] arr : arrays) {
      len += arr.length;
    }

    final byte[] result = new byte[len];
    int pos = 0;
    for (final byte[] arr : arrays) {
      System.arraycopy(arr, 0, result, pos, arr.length);
      pos += arr.length;
    }
    return result;
  }

  /**
   * Revert order for defined number of bytes in a value.
   *
   * @param value the value which bytes should be reordered
   * @param numOfLowerBytesToInvert number of lower bytes to be reverted in
   * their order, must be 1..8
   * @return new value which has reverted order for defined number of lower
   * bytes
   * @since 1.1
   */
  public static long reverseByteOrder(long value, int numOfLowerBytesToInvert) {
    if (numOfLowerBytesToInvert < 1 || numOfLowerBytesToInvert > 8) {
      throw new IllegalArgumentException("Wrong number of bytes [" + numOfLowerBytesToInvert + ']');
    }

    long result = 0;

    int offsetInResult = (numOfLowerBytesToInvert - 1) * 8;

    while (numOfLowerBytesToInvert-- > 0) {
      final long thebyte = value & 0xFF;
      value >>>= 8;
      result |= (thebyte << offsetInResult);
      offsetInResult -= 8;
    }

    return result;
  }

  /**
   * Convert unsigned long value into string representation with defined radix
   * base.
   *
   * @param ulongValue value to be converted in string
   * @param radix radix base to be used for conversion, must be 2..36
   * @param charBuffer char buffer to be used for conversion operations, should
   * be not less than 64 char length, if length is less than 64 or null then new
   * one will be created
   * @return converted value as upper case string
   * @throws IllegalArgumentException for wrong radix base
   * @since 1.1
   */
  public static String ulong2str(final long ulongValue, final int radix, final char[] charBuffer) {
    if (radix < 2 || radix > 36) {
      throw new IllegalArgumentException("Illegal radix [" + radix + ']');
    }

    if (ulongValue == 0) {
      return "0";
    }
    else {
      long cur = ulongValue;
      final String result;
      if (cur > 0) {
        result = Long.toString(cur, radix).toUpperCase(Locale.ENGLISH);
      }
      else {
        final char[] buffer = charBuffer == null || charBuffer.length < 64 ? new char[64] : charBuffer;
        int pos = buffer.length;
        long topPart = cur >>> 32;
        long bottomPart = (cur & 0xFFFFFFFFL) + ((topPart % radix) << 32);
        topPart /= radix;
        while ((bottomPart | topPart) > 0) {
          final int val = (int) (bottomPart % radix);
          buffer[--pos] = (char) (val < 10 ? '0' + val : 'A' + val - 10);
          bottomPart = (bottomPart / radix) + ((topPart % radix) << 32);
          topPart /= radix;
        }
        result = new String(buffer, pos, buffer.length - pos);
      }
      return result;
    }
  }

  /**
   * Extend text by chars to needed length.
   *
   * @param text text to be extended, must not be null.
   * @param neededLen needed length for text
   * @param ch char to be used for extending
   * @param mode 0 to extend left, 1 to extend right, otherwise extends both
   * sides
   * @return text extended by chars up to needed length, or non-changed if the
   * text has equals or greater length.
   * @since 1.1
   */
  public static String ensureMinTextLength(final String text, final int neededLen, final char ch, final int mode) {
    final int number = neededLen - text.length();
    if (number <= 0) {
      return text;
    }

    final StringBuilder result = new StringBuilder(neededLen);
    switch (mode) {
      case 0: {
        for (int i = 0; i < number; i++) {
          result.append(ch);
        }
        result.append(text);
      }
      break;
      case 1: {
        result.append(text);
        for (int i = 0; i < number; i++) {
          result.append(ch);
        }
      }
      break;
      default: {
        int leftField = number / 2;
        int rightField = number - leftField;
        while (leftField-- > 0) {
          result.append(ch);
        }
        result.append(text);
        while (rightField-- > 0) {
          result.append(ch);
        }
      }
    }
    return result.toString();
  }

  /**
   * Remove leading zeros from string.
   *
   * @param str the string to be trimmed
   * @return the result string without left extra zeros, or null if argument is
   * null
   * @since 1.1
   */
  public static String removeLeadingZeros(final String str) {
    String result = str;
    if (str != null && str.length() != 0) {
      int startIndex = 0;
      while (startIndex < str.length() - 1) {
        final char ch = str.charAt(startIndex);
        if (ch != '0') {
          break;
        }
        startIndex++;
      }
      if (startIndex > 0) {
        result = str.substring(startIndex);
      }
    }
    return result;
  }

  /**
   * Remove trailing zeros from string.
   *
   * @param str the string to be trimmed
   * @return the result string without left extra zeros, or null if argument is
   * null
   * @since 1.1
   */
  public static String removeTrailingZeros(final String str) {
    String result = str;
    if (str != null && str.length() != 0) {
      int endIndex = str.length();
      while (endIndex > 1) {
        final char ch = str.charAt(endIndex - 1);
        if (ch != '0') {
          break;
        }
        endIndex--;
      }
      if (endIndex < str.length()) {
        result = str.substring(0, endIndex);
      }
    }
    return result;
  }

  /**
   * Check that a byte array starts with some byte values.
   *
   * @param array array to be checked, must not be null
   * @param str a byte string which will be checked as the start sequence of the
   * array, must not be null
   * @return true if the string is the start sequence of the array, false
   * otherwise
   * @throws NullPointerException if any argument is null
   * @since 1.1
   */
  public static boolean arrayStartsWith(final byte[] array, final byte[] str) {
    boolean result = false;
    if (array.length >= str.length) {
      result = true;
      int index = str.length;
      while (--index >= 0) {
        if (array[index] != str[index]) {
          result = false;
          break;
        }
      }
    }
    return result;
  }

  /**
   * Check that a byte array ends with some byte values.
   *
   * @param array array to be checked, must not be null
   * @param str a byte string which will be checked as the end sequence of the
   * array, must not be null
   * @return true if the string is the end sequence of the array, false
   * otherwise
   * @throws NullPointerException if any argument is null
   * @since 1.1
   */
  public static boolean arrayEndsWith(final byte[] array, final byte[] str) {
    boolean result = false;
    if (array.length >= str.length) {
      result = true;
      int index = str.length;
      int arrindex = array.length;
      while (--index >= 0) {
        if (array[--arrindex] != str[index]) {
          result = false;
          break;
        }
      }
    }
    return result;
  }

  /**
   * Make mask for value.
   *
   * @param value a value for which we need to make mask.
   * @return generated mask to represent the value
   * @since 1.1
   */
  public static int makeMask(final int value) {
    if (value == 0) {
      return 0;
    }
    if ((value & 0x80000000) != 0) {
      return 0xFFFFFFFF;
    }
    int msk = 1;
    do{
      msk <<= 1;
    }while(msk <= value);
    return msk - 1;
  }

}
