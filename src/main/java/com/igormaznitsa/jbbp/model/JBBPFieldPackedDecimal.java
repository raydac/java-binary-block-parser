package com.igormaznitsa.jbbp.model;

import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.utils.JBBPUtils;

/**
 * Describes a packed binary coded decimal (BCD) field.
 * @see <a href="http://en.wikipedia.org/wiki/Binary-coded_decimal#Packed_BCD">Packed BCD (Wikipedia)</a>
 *
 * @since 1.1.1
 */
public class JBBPFieldPackedDecimal extends JBBPAbstractField implements JBBPNumericField {
  private static final long serialVersionUID = 1L;

  /**
   * Inside value storage.
   */
  private final long value;

  /**
   * The COnstructor.
   * @param name a field name info, it can be null
   * @param value the field value
   */
  public JBBPFieldPackedDecimal(final JBBPNamedFieldInfo name, final long value) {
    super(name);
    this.value = value;
  }

  public long getValue() {
    return value;
  }

  public int getAsInt() {
    return (int)this.value;
  }

  public long getAsLong() {
    return this.value;
  }

  public boolean getAsBool() {
    return this.value != 0;
  }

  /**
   * Get the reversed bit representation of the value.
   *
   * @param value the value to be reversed
   * @return the reversed value
   */
  public static long reverseBits(final long value) {
    return JBBPFieldLong.reverseBits(value);
  }

  public long getAsInvertedBitOrder() {
    return reverseBits(this.value);
  }
}
