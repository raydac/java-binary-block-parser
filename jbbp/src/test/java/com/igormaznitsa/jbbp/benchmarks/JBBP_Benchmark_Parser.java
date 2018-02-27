package com.igormaznitsa.jbbp.benchmarks;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

/**
 * Generated from JBBP script by internal JBBP Class Source Generator
 */
public class JBBP_Benchmark_Parser {

  /**
   * The Constant contains parser flags
   *
   * @see JBBPParser#FLAG_SKIP_REMAINING_FIELDS_IF_EOF
   */
  protected static final int _ParserFlags_ = 0;
  public char val;
  protected _ASTRUCT1234[] _astruct1234;

  public JBBP_Benchmark_Parser() {
  }

  public JBBP_Benchmark_Parser read(final JBBPBitInputStream In) throws IOException {
    this.val = (char) (In.readByte() & 0xFF);
    if (this._astruct1234 == null || this._astruct1234.length != (((int) this.val >> 1) * ((int) this.val + 3))) {
      this._astruct1234 = new _ASTRUCT1234[(((int) this.val >> 1) * ((int) this.val + 3))];
      for (int I = 0; I < (((int) this.val >> 1) * ((int) this.val + 3)); I++) {
        this._astruct1234[I] = new _ASTRUCT1234(this);
      }
    }
    for (int I = 0; I < (((int) this.val >> 1) * ((int) this.val + 3)); I++) {
      this._astruct1234[I].read(In);
    }
    return this;
  }

  public JBBP_Benchmark_Parser write(final JBBPBitOutputStream Out) throws IOException {
    Out.write(this.val);
    for (int I = 0; I < (((int) this.val >> 1) * ((int) this.val + 3)); I++) {
      this._astruct1234[I].write(Out);
    }
    return this;
  }

  public static class _ASTRUCT1234 {

    private final JBBP_Benchmark_Parser _Root_;
    public byte a;
    public byte b;
    public byte c;

    public _ASTRUCT1234(JBBP_Benchmark_Parser root) {
      _Root_ = root;
    }

    public _ASTRUCT1234 read(final JBBPBitInputStream In) throws IOException {
      this.a = In.readBitField(JBBPBitNumber.BITS_3);
      this.b = In.readBitField(JBBPBitNumber.BITS_3);
      this.c = In.readBitField(JBBPBitNumber.BITS_2);
      In.skip(1);
      return this;
    }

    public _ASTRUCT1234 write(final JBBPBitOutputStream Out) throws IOException {
      Out.writeBits(this.a, JBBPBitNumber.BITS_3);
      Out.writeBits(this.b, JBBPBitNumber.BITS_3);
      Out.writeBits(this.c, JBBPBitNumber.BITS_2);
      for (int I = 0; I < 1; I++) {
        Out.write(0);
      }
      return this;
    }
  }
}