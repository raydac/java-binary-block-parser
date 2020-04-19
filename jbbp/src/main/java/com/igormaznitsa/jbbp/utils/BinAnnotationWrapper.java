package com.igormaznitsa.jbbp.utils;

import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import java.lang.annotation.Annotation;

/**
 * Auxiliary class to replace Bin annotation fields during internal processing
 *
 * @since 2.0.
 */
public class BinAnnotationWrapper implements Bin {
  private final Bin bin;
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

  public BinAnnotationWrapper(final Bin bin) {
    this.bin = bin;
  }

  public BinAnnotationWrapper setName(final String value) {
    this.name = value;
    return this;
  }

  @Override
  public String name() {
    return this.name == null ? this.bin.name() : this.name;
  }

  public BinAnnotationWrapper setPath(final String value) {
    this.path = value;
    return this;
  }

  @Override
  public String path() {
    return this.path == null ? this.bin.path() : this.path;
  }

  public BinAnnotationWrapper setCustomType(final String value) {
    this.customType = value;
    return this;
  }

  @Override
  public String customType() {
    return this.customType == null ? this.bin.customType() : this.customType;
  }

  public BinAnnotationWrapper setArraySizeExpr(final String value) {
    this.arraySizeExpr = value;
    return this;
  }

  @Override
  public String arraySizeExpr() {
    return this.arraySizeExpr == null ? this.bin.arraySizeExpr() : this.arraySizeExpr;
  }

  public BinAnnotationWrapper setType(final BinType value) {
    this.type = value;
    return this;
  }

  @Override
  public BinType type() {
    return this.type == null ? this.bin.type() : this.type;
  }

  public BinAnnotationWrapper setBitOrder(final JBBPBitOrder value) {
    this.bitOrder = value;
    return this;
  }

  @Override
  public JBBPBitOrder bitOrder() {
    return this.bitOrder == null ? this.bin.bitOrder() : this.bitOrder;
  }

  public BinAnnotationWrapper setCustom(final Boolean value) {
    this.custom = value;
    return this;
  }

  @Override
  public boolean custom() {
    return this.custom == null ? this.bin.custom() : this.custom;
  }

  public BinAnnotationWrapper setParamExpr(final String value) {
    this.paramExpr = value;
    return this;
  }

  @Override
  public String paramExpr() {
    return this.paramExpr == null ? this.bin.paramExpr() : this.paramExpr;
  }

  public BinAnnotationWrapper setBitNumber(final JBBPBitNumber value) {
    this.bitNumber = value;
    return this;
  }

  @Override
  public JBBPBitNumber bitNumber() {
    return this.bitNumber == null ? this.bin.bitNumber() : this.bitNumber;
  }

  public BinAnnotationWrapper setByteOrder(final JBBPByteOrder value) {
    this.byteOrder = value;
    return this;
  }

  @Override
  public JBBPByteOrder byteOrder() {
    return this.byteOrder == null ? this.bin.byteOrder() : this.byteOrder;
  }

  public BinAnnotationWrapper setOrder(final Integer value) {
    this.order = value;
    return this;
  }

  @Override
  public int order() {
    return this.order == null ? this.bin.order() : this.order;
  }

  public BinAnnotationWrapper setComment(final String value) {
    this.comment = value;
    return this;
  }

  @Override
  public String comment() {
    return this.comment == null ? this.bin.comment() : this.comment;
  }

  @Override
  public Class<? extends Annotation> annotationType() {
    return this.bin.getClass();
  }
}
