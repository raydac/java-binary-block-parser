package com.igormaznitsa.jbbp.exceptions;

/**
 * Thrown when errors occur while using {@link com.igormaznitsa.jbbp.io.JBBPOut}
 * to create binary block byte arrays.
 */
public class JBBPOutException extends JBBPException {

  public JBBPOutException(String message) {
    super(message);
  }

  public JBBPOutException(String message, Throwable cause) {
    super(message, cause);
  }
}
