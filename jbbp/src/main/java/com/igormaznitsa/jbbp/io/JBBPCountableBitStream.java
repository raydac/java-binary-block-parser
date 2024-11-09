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
 * The Interface describes a stream which can manipulate bits and count number of bytes.
 *
 * @since 1.0
 */
public interface JBBPCountableBitStream {

  /**
   * Get the number of bytes passed from the stream.
   *
   * @return the number of passed bytes
   */
  long getCounter();

  /**
   * Get the bit order for the stream.
   *
   * @return the bit order
   * @see JBBPBitOrder#LSB0
   * @see JBBPBitOrder#MSB0
   */
  JBBPBitOrder getBitOrder();

  /**
   * Reset the inside byte counter of the stream, inside bit buffer will be reset.
   */
  void resetCounter();

  /**
   * Get the inside stream a bit buffer.
   *
   * @return the value from inside the stream bit buffer
   */
  int getBitBuffer();

  /**
   * Get the number of bits cached in the bit buffer.
   *
   * @return the number of bits cached in the stream bit buffer
   */
  int getBufferedBitsNumber();
}
