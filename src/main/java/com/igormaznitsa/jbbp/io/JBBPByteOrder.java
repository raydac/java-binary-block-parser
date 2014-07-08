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
package com.igormaznitsa.jbbp.io;

/**
 * Constants define byte order for multi-byte values to be read or written into streams.
 */
public enum JBBPByteOrder {
  /**
   * The Big-Endian order. Big-endian systems store the most significant byte of
   * a word in the smallest address and the least significant byte is stored in
   * the largest address. The Default order for Java and Network.
   */
  BIG_ENDIAN,
  /**
   * The Little-Endian order. Little-endian systems store the
   * least significant byte in the smallest address.
   */
  LITTLE_ENDIAN
}
