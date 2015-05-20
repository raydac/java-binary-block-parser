package com.igormaznitsa.jbbp.model;

import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.utils.JBBPUtils;

/**
 * Describes a packed binary coded decimal array.
 * @see <a href="http://en.wikipedia.org/wiki/Binary-coded_decimal#Packed_BCD">Packed BCD (Wikipedia)</a>
 *
 * @since 1.1.1
 */
public class JBBPFieldArrayPackedDecimal extends JBBPAbstractArrayField<JBBPFieldPackedDecimal> {
  private static final long serialVersionUID = 1L;
  /**
   * Inside value storage.
   */
  private final long [] array;

  /**
   * The Constructor.
   * @param name a field name info, it can be null
   * @param array a value array, it must not be null
   */
  public JBBPFieldArrayPackedDecimal(final JBBPNamedFieldInfo name, final long[] array) {
    super(name);
    JBBPUtils.assertNotNull(array, "Array must not be null");
    this.array = array;
  }

  /**
   * get the value array
   * @return the value array as a long array
   */
  public long [] getArray(){
    return this.array.clone();
  }

  @Override
  public int size() {
    return this.array.length;
  }

  @Override
  public JBBPFieldPackedDecimal getElementAt(int index) {
    return new JBBPFieldPackedDecimal(this.fieldNameInfo, this.array[index]);
  }

  @Override
  public int getAsInt(int index) {
    return (int)this.array[index];
  }

  @Override
  public long getAsLong(int index) {
    return this.array[index];
  }

  @Override
  public boolean getAsBool(int index) {
    return this.array[index]!=0L;
  }

  @Override
  public Object getValueArrayAsObject(boolean reverseBits) {
    final long[] result;
    if (reverseBits) {
      result = this.array.clone();
      for (int i = 0; i < result.length; i++) {
        result[i] = JBBPFieldLong.reverseBits(result[i]);
      }
    }
    else {
      result = this.array.clone();
    }
    return result;
  }
}
