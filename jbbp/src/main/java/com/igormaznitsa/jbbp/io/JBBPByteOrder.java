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

package com.igormaznitsa.jbbp.io;

/**
 * Byte order for multi-byte numeric values and for <b>raw byte/ubyte array chunks</b> in the DSL.
 * <p>
 * In scripts, {@code >} selects {@link #BIG_ENDIAN} and may be omitted (default). {@code <}
 * selects {@link #LITTLE_ENDIAN}. For {@link JBBPBitInputStream#readByteArray(int, JBBPByteOrder)}
 * and {@link JBBPBitOutputStream#writeBytes(byte[], int, JBBPByteOrder)}, the chunk is treated as
 * one byte sequence: big-endian keeps stream index order; little-endian reverses that sequence
 * so the logical value matches usual LE layout (least significant byte at the lowest index).
 *
 * @since 1.0
 */
public enum JBBPByteOrder {
  /**
   * Big-endian: most significant byte first. Default in the DSL when no {@code >}/{@code <} prefix
   * is written; explicit {@code >} is equivalent.
   */
  BIG_ENDIAN,
  /**
   * Little-endian: for multi-byte scalars, least significant byte is read/written first. For
   * {@code byte[]}/{@code ubyte[]} array fields, the read or written chunk is reversed end-to-end
   * so index {@code 0} holds the least significant byte of the sequence.
   */
  LITTLE_ENDIAN
}
