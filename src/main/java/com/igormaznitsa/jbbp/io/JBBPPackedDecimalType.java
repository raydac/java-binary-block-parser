package com.igormaznitsa.jbbp.io;

/**
 * Constants for signed and unsigned packed decimal.
 */
public enum JBBPPackedDecimalType {
  /**
   * Signed with a nibble indicating the sign.  Sign values are according to the following table:
   * <table>
   *   <tr>
   *     <th align="center">Value</th>
   *     <th align="center">Sign</th>
   *     <th align="center">Notes</th>
   *   </tr>
   *   <tr>
   *     <td align="center">0xA (1010)</td>
   *     <td align="center">+</td>
   *     <td align="center">&nbsp;</td>
   *   </tr>
   *   <tr>
   *     <td align="center">0xB (1011)</td>
   *     <td align="center">-</td>
   *     <td align="center">&nbsp;</td>
   *   </tr>
   *   <tr>
   *     <td align="center">0xC (1100)</td>
   *     <td align="center">+</td>
   *     <td align="center">Preferred</td>
   *   </tr>
   *   <tr>
   *     <td align="center">0xD (1101)</td>
   *     <td align="center">-</td>
   *     <td align="center">Preferred</td>
   *   </tr>
   *   <tr>
   *     <td align="center">0xE (1110)</td>
   *     <td align="center">+</td>
   *     <td align="center">&nbsp;</td>
   *   </tr>
   *   <tr>
   *     <td align="center">0xF (1111)</td>
   *     <td align="center">+</td>
   *     <td align="center">Unsigned</td>
   *   </tr>
   * </table>
   */
  SIGNED,
  /**
   * Unsigned.  Numbers with an odd number of digits can be encoded either with
   * a leading zero nibble, leading zeroes, or with the last nibble set to 0xA or
   * higher (0xF is standard), which will be ignored.  For example, the number 123
   * could be encoded either as:
   * <pre>
   *   0001 0010 0011 1111
   * </pre>
   * or as:
   * <pre>
   *   0000 0001 0010 0011
   * </pre>
   */
  UNSIGNED
}
