/*
 * Copyright 2017 Igor Maznitsa.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.igormaznitsa.jbbp.exceptions;

/**
 * The Exception can be thrown during parsing sources for tokens and allows to figure
 * out the position of the problematic token.
 *
 * @since 1.0
 */
public class JBBPTokenizerException extends JBBPCompilationException {
  private static final long serialVersionUID = -1132154077305893246L;

  /**
   * The Token position.
   */
  private final int position;

  private final String errorPart;

  /**
   * Constructor.
   *
   * @param message the exception message.
   * @param script  the script contains error, can be null
   * @param pos     the position of a problematic token inside sources.
   * @since 2.0.3
   */
  public JBBPTokenizerException(final String message, final String script, final int pos) {
    super(message);
    this.errorPart = script == null ? "" : extractErrorPartText(script, pos);
    this.position = pos;
  }

  /**
   * Auxiliary internal method to extract error part from script around specific position.
   *
   * @param script        the error script to be processed, must not be null
   * @param errorPosition the error position in the script
   * @return error part of the script as string, must not be null
   * @since 2.0.3
   */
  private static String extractErrorPartText(final String script, final int errorPosition) {
    if (script.length() == 0 || errorPosition >= script.length() || errorPosition < 0) {
      return "";
    }
    final int maxLengthWing = 16;
    final StringBuilder buffer = new StringBuilder();
    buffer.append(script.charAt(errorPosition));
    int errorPositionAtBuffer = 0;
    int leftPosition = errorPosition - 1;
    int rightPosition = errorPosition + 1;
    int leftNonSpaceCounter = 0;
    int rightNonSpaceCounter = 0;
    for (int i = 0; i < maxLengthWing; i++) {
      if (leftPosition >= 0) {
        final char chr = script.charAt(leftPosition);
        if (Character.isISOControl(chr)
            || (i > 2 && leftNonSpaceCounter > 0 && Character.isSpaceChar(chr))) {
          leftPosition = -1;
        } else {
          buffer.insert(0, chr);
          leftNonSpaceCounter += Character.isSpaceChar(chr) ? 1 : 0;
          errorPositionAtBuffer++;
          leftPosition--;
        }
      }
      if (rightPosition >= 0 && rightPosition < script.length()) {
        final char chr = script.charAt(rightPosition);
        if (Character.isISOControl(chr)
            || (i > 2 && rightNonSpaceCounter > 0 && Character.isSpaceChar(chr))) {
          rightPosition = -1;
        } else {
          buffer.append(chr);
          rightNonSpaceCounter += Character.isSpaceChar(chr) ? 1 : 0;
          rightPosition++;
        }
      }
    }
    final String errorMarkerLeft = " ->";
    final String errorMarkerRight = "<- ";
    buffer.insert(errorPositionAtBuffer + 1, errorMarkerRight);
    buffer.insert(errorPositionAtBuffer, errorMarkerLeft);
    errorPositionAtBuffer += errorMarkerLeft.length();

    if (Character.isISOControl(buffer.charAt(errorPositionAtBuffer))) {
      String hex = Integer.toHexString(buffer.charAt(errorPositionAtBuffer));
      hex = "\\u" + "0000".substring(hex.length()) + hex;

      buffer.delete(errorPositionAtBuffer, errorPositionAtBuffer + 1);
      buffer.insert(errorPositionAtBuffer, hex);
    }
    return buffer.toString().trim();
  }

  /**
   * Get error part of script where error position marked by !&gt;..&lt;!
   *
   * @return error part of the script in position, or empty if it was impossible to extract the part
   * @since 2.0.3
   */
  public String getErrorPart() {
    return this.errorPart;
  }

  /**
   * get the position in sources of the problematic token.
   *
   * @return the position or -1 if the position is unknown.
   */
  public int getPosition() {
    return this.position;
  }

  @Override
  public String toString() {
    return this.getLocalizedMessage() + " (pos=" + this.position + ", errorPart=\"" +
        this.errorPart + "\")";
  }
}
