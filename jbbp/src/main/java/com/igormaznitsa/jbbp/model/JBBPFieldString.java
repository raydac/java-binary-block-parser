/*
 * Copyright 2018 Igor Maznitsa.
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

package com.igormaznitsa.jbbp.model;

import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;

/**
 * Describes a java string object.
 *
 * @since 1.4.0
 */
public final class JBBPFieldString extends JBBPAbstractField {

  private static final long serialVersionUID = -2861961302858335702L;
  public static final String TYPE_NAME = "stringj";

  private final String str;

  /**
   * A Constructor.
   *
   * @param name   a field name info, it can be null
   * @param nullableValue a value, it can be null
   */
  public JBBPFieldString(final JBBPNamedFieldInfo name, final String nullableValue) {
    super(name);
    this.str = nullableValue;
  }

  /**
   * Get the saved value.
   * @return the value as String, it can be null
   */
  public String getAsString() {
    return this.str;
  }

  /**
   * Get the reversed bit representation of the value.
   *
   * @param value the value to be reversed, can be null
   * @return the reversed value
   */
  public static String reverseBits(final String value) {
    String result = null;
    if (value != null) {
      final char[] chars = value.toCharArray();

      for(int i=0; i<chars.length; i++) {
        chars [i] = (char)JBBPFieldUShort.reverseBits((short)chars[i]);
      }

      result = String.valueOf(chars);
    }
    return result;
  }

  @Override
  public String getTypeAsString() {
    return TYPE_NAME;
  }
}
