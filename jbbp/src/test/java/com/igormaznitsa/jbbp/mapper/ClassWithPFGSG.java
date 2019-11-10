package com.igormaznitsa.jbbp.mapper;

@Bin
public class ClassWithPFGSG {
  @Bin(order = 1)
  private byte a;
  @Bin(order = 2)
  private byte b;
  @Bin(order = 3)
  private Internal i;

  public Internal getI() {
    return this.i;
  }

  public byte getA() {
    return this.a;
  }

  public void setA(final byte a) {
    this.a = a;
  }

  public byte getB() {
    return this.b;
  }

  public void setB(final byte b) {
    this.b = b;
  }

  public Internal makeI() {
    this.i = new Internal();
    return this.i;
  }

  @Bin
  public class Internal {
    @Bin(order = 1)
    private byte c;
    @Bin(order = 2)
    private InternalInternal ii;

    public byte getC() {
      return this.c;
    }

    public void setC(byte c) {
      this.c = c;
    }

    public InternalInternal getIi() {
      return this.ii;
    }

    public InternalInternal makeIi() {
      this.ii = new InternalInternal();
      return ii;
    }

    @Bin
    public class InternalInternal {
      @Bin(order = 1)
      private byte d;

      public byte getD() {
        return this.d;
      }

      public void setD(final byte d) {
        this.d = d;
      }
    }
  }

}
