package com.igormaznitsa.jbbp.exceptions;

import com.igormaznitsa.jbbp.model.JBBPNumericField;

/**
 * Exception to indicate error during value conversion process.
 *
 * @since 2.0.4
 */
public class JBBPNumericFieldValueConversionException extends JBBPException {

  private final JBBPNumericField source;

  /**
   * Get source field.
   *
   * @return source field for exception, can be null
   */
  public JBBPNumericField getSource() {
    return this.source;
  }

  /**
   * Constructor to provide source field and message.
   *
   * @param source  source field. can be null
   * @param message message, can be null
   */
  public JBBPNumericFieldValueConversionException(final JBBPNumericField source,
                                                  final String message) {
    this(source, message, null);
  }

  /**
   * Constructor to provide source field, message and cause error.
   *
   * @param source  source field. can be null
   * @param message message, can be null
   * @param cause   cause error, can be null
   */
  public JBBPNumericFieldValueConversionException(final JBBPNumericField source,
                                                  final String message, final Throwable cause) {
    super(message, cause);
    this.source = source;
  }

  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder(getClass().getName());
    final String message = this.getLocalizedMessage();
    final JBBPNumericField source = this.getSource();

    result.append('(');
    result
        .append("message=").append(message).append(',')
        .append("source=").append(source);

    final Throwable cause = this.getCause();
    if (cause != null) {
      result.append(",cause=").append(cause.getClass().getName());
    }

    result.append(')');

    return result.toString();
  }
}
