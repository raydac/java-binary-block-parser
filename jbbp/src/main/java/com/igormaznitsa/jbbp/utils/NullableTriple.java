package com.igormaznitsa.jbbp.utils;

/**
 * Auxiliary class to keep three values. Any value can be null.
 *
 * @param <A> first value type
 * @param <B> second value type
 * @param <C> third value type
 * @since 2.0.0
 */
public final class NullableTriple<A, B, C> {
  private final A a;
  private final B b;
  private final C c;

  public NullableTriple(A a, B b, C c) {
    this.a = a;
    this.b = b;
    this.c = c;
  }

  public A getA() {
    return this.a;
  }

  public B getB() {
    return this.b;
  }

  public C getC() {
    return this.c;
  }

}
