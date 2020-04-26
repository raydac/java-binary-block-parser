package com.igormaznitsa.jbbp.utils;

import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import java.lang.annotation.Annotation;

/**
 * Auxiliary class to replace Bin annotation field values.
 * <b>Not thread safe!</b>
 *
 * @since 2.0.2
 */
public final class BinAnnotationWrapper implements Bin {
  private Bin bin;
  private String name;
  private String path;
  private String customType;
  private String arraySizeExpr;
  private BinType type;
  private JBBPBitOrder bitOrder;
  private Boolean custom;
  private String paramExpr;
  private JBBPBitNumber bitNumber;
  private JBBPByteOrder byteOrder;
  private Integer order;
  private String comment;

  public BinAnnotationWrapper() {
  }

  public BinAnnotationWrapper setWrapped(final Bin bin) {
    this.bin = bin;
    return this;
  }

  public BinAnnotationWrapper setName(final String value) {
    this.name = value;
    return this;
  }

  @Override
  public String name() {
    assert this.bin != null;
    return this.name == null ? this.bin.name() : this.name;
  }

  public BinAnnotationWrapper setPath(final String value) {
    this.path = value;
    return this;
  }

  @Override
  public String path() {
    assert this.bin != null;
    return this.path == null ? this.bin.path() : this.path;
  }

  public BinAnnotationWrapper setCustomType(final String value) {
    this.customType = value;
    return this;
  }

  @Override
  public String customType() {
    assert this.bin != null;
    return this.customType == null ? this.bin.customType() : this.customType;
  }

  public BinAnnotationWrapper setArraySizeExpr(final String value) {
    this.arraySizeExpr = value;
    return this;
  }

  @Override
  public String arraySizeExpr() {
    assert this.bin != null;
    return this.arraySizeExpr == null ? this.bin.arraySizeExpr() : this.arraySizeExpr;
  }

  public BinAnnotationWrapper setType(final BinType value) {
    this.type = value;
    return this;
  }

  @Override
  public BinType type() {
    assert this.bin != null;
    return this.type == null ? this.bin.type() : this.type;
  }

  public BinAnnotationWrapper setBitOrder(final JBBPBitOrder value) {
    this.bitOrder = value;
    return this;
  }

  @Override
  public JBBPBitOrder bitOrder() {
    assert this.bin != null;
    return this.bitOrder == null ? this.bin.bitOrder() : this.bitOrder;
  }

  public BinAnnotationWrapper setCustom(final Boolean value) {
    this.custom = value;
    return this;
  }

  @Override
  public boolean custom() {
    assert this.bin != null;
    return this.custom == null ? this.bin.custom() : this.custom;
  }

  public BinAnnotationWrapper setParamExpr(final String value) {
    this.paramExpr = value;
    return this;
  }

  @Override
  public String paramExpr() {
    assert this.bin != null;
    return this.paramExpr == null ? this.bin.paramExpr() : this.paramExpr;
  }

  public BinAnnotationWrapper setBitNumber(final JBBPBitNumber value) {
    this.bitNumber = value;
    return this;
  }

  @Override
  public JBBPBitNumber bitNumber() {
    assert this.bin != null;
    return this.bitNumber == null ? this.bin.bitNumber() : this.bitNumber;
  }

  public BinAnnotationWrapper setByteOrder(final JBBPByteOrder value) {
    this.byteOrder = value;
    return this;
  }

  @Override
  public JBBPByteOrder byteOrder() {
    assert this.bin != null;
    return this.byteOrder == null ? this.bin.byteOrder() : this.byteOrder;
  }

  public BinAnnotationWrapper setOrder(final Integer value) {
    this.order = value;
    return this;
  }

  @Override
  public int order() {
    assert this.bin != null;
    return this.order == null ? this.bin.order() : this.order;
  }

  public BinAnnotationWrapper setComment(final String value) {
    this.comment = value;
    return this;
  }

  @Override
  public String comment() {
    assert this.bin != null;
    return this.comment == null ? this.bin.comment() : this.comment;
  }

  @Override
  public Class<? extends Annotation> annotationType() {
    return Bin.class;
  }
}
