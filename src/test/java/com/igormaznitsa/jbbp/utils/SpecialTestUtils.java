/* 
 * Copyright 2014 Igor Maznitsa (http://www.igormaznitsa.com).
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
package com.igormaznitsa.jbbp.utils;

public final class SpecialTestUtils {

  private SpecialTestUtils() {

  }

  public static byte[] copyOfRange(final byte[] array, final int startIndex, final int endIndex) {
    int len = endIndex - startIndex;
    if (len <= 0) {
      return new byte[0];
    }
    final byte[] result = new byte[len];
    System.arraycopy(array, startIndex, result, 0, len);
    return result;
  }
}
