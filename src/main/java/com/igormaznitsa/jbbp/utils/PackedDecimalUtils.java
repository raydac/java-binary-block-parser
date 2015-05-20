package com.igormaznitsa.jbbp.utils;

import com.igormaznitsa.jbbp.exceptions.JBBPOutException;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.io.JBBPPackedDecimalType;

import java.io.IOException;

/**
 * Collections of methods for handling packed decimal (BCD).  All methods are thread-safe.
 */
public class PackedDecimalUtils {

  public long readValueFromPackedDecimal(final byte[] bytes, final JBBPPackedDecimalType type) {

    // read the numeric digits
    StringBuilder digitStr = new StringBuilder();
    for (int i = 0; i < bytes.length * 2; i++) {

      byte currentByte = bytes[i / 2];

      // even values of i use high nibble, odd values use low nibble
      byte digit = (i % 2 == 0) ? (byte)((currentByte & 0xff) >>> 4) : (byte)(currentByte & 0x0f);
      if (digit < 10) {
        digitStr.append(digit);
      }
    }

    // read the sign
    if (type.equals(JBBPPackedDecimalType.SIGNED)) {
      byte sign = (byte)(bytes[bytes.length-1] & 0x0f);
      if (sign == 0x0b || sign == 0x0d) {
        digitStr.insert(0, '-');
      }
    }

    return Long.parseLong(digitStr.toString());
  }

  public byte[] writeValueToPackedDecimal(final int length, final long value, final JBBPPackedDecimalType type) throws IOException {

    // validate sign
    boolean signed = type.equals(JBBPPackedDecimalType.SIGNED);
    if (value < 0 && !signed) {
      throw new JBBPOutException("Packed decimal set to UNSIGNED, but value is negative: " + value);
    }

    // validate length (sign requires one nibble)
    String valueStr = Long.toString(value).replaceFirst("-", "");
    int digits = valueStr.length();
    if ((signed && digits > ((length*2) - 1)) || (!signed && digits > (length*2))) {
      throw new JBBPOutException("Number of digits in value exceeds packed binary capacity using " + length + " bytes");
    }

    // create array with one byte per digit, plus optional sign
    byte[] temp = new byte[length*2]; // elements get initialized to 0
    int startIdx = signed ? ((length*2) - 1) - digits : (length*2) - digits;
    for (int i = startIdx; i < (startIdx + digits); i++) {
      temp[i] = Byte.parseByte(Character.toString(valueStr.charAt(i-startIdx)));
    }
    if (signed) {
      if (value < 0)
        temp[temp.length-1] = 0xD;
      else
        temp[temp.length-1] = 0xC;
    }

    // pack into nibbles
    byte[] outArray = new byte[length];
    for (int i = 0; i < temp.length; i+=2) {
      outArray[i/2] = (byte)((temp[i] << 4) | temp[i+1]);
    }
    return outArray;
  }
}
