package com.igormaznitsa.jbbp.utils;

import com.igormaznitsa.jbbp.io.JBBPByteOrder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation allows to mark a field as custom data type for DSL builder.
 * @since 1.4.1
 * @see com.igormaznitsa.jbbp.utils.JBBPDslBuilder
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( {ElementType.FIELD, ElementType.TYPE})
@Inherited
public @interface DslBinCustom {
  /**
   * Name of the field.
   * @return name of the field, if empty then undefined
   */
  String name() default "";

  /**
   * Name of the custom type.
   * @return type of the field, if empty then undefined
   */
  String type() default "";

  /**
   * Byte order for the field.
   * @return byte order of data represented by the field
   */
  JBBPByteOrder byteOrder() default JBBPByteOrder.BIG_ENDIAN;

  /**
   * Expression to be placed in extra part of type. It means type[:extra]
   * @return extra value, if empty then undefined
   */
  String extraExpression() default "";

  /**
   * Expression to represent array size of the field.
   * @return array size of the field, if empty then not defined
   */
  String arraySizeExpression() default "";

  /**
   * Order of the field.
   * @return order of the field.
   */
  int order() default 0;

  /**
   * Field allows provide some comment.
   * @return comment for the annotation
   */
  String comment() default "";
}
