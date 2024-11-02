package com.igormaznitsa.jbbp.exceptions;

/**
 * Exception thrown if reached limit of items for whole stream array.
 *
 * @since 2.1.0
 */
public class JBBPReachedArraySizeLimitException extends JBBPIOException {

  private final int readSize;
  private final int limitSize;

  public JBBPReachedArraySizeLimitException(final String message, final int readSize,
                                            final int limitSize) {
    super(message);
    this.readSize = readSize;
    this.limitSize = limitSize;
  }

  public int getReadSize() {
    return this.readSize;
  }

  public int getLimitSize() {
    return this.limitSize;
  }

  @Override
  public String toString() {
    return JBBPReachedArraySizeLimitException.class.getSimpleName() + '{' +
        "readSize=" + this.readSize +
        ", limitSize=" + this.limitSize +
        '}';
  }
}
