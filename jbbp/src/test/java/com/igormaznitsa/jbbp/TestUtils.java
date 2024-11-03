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

package com.igormaznitsa.jbbp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Random;
import org.apache.commons.codec.digest.PureJavaCrc32;

/**
 * Different useful auxiliary test methods
 */
public enum TestUtils {
  ;

  /**
   * Delta to be used for double and float compare.
   */
  public static final float FLOAT_DELTA = Float.MIN_VALUE;

  /**
   * Read field value, also allows to provide dot-separated chain of fields
   *
   * @param <T>       expected type of value
   * @param instance  instance of object, must not be null
   * @param fieldName field name, can be single name or dot-separated one, must
   *                  not be null
   * @param klazz     expected value class, must not be null
   * @return value, can be null
   * @throws Exception it will be thrown if any error
   */
  public static <T> T getField(final Object instance, final String fieldName, final Class<T> klazz)
      throws Exception {
    final String[] fields = fieldName.split("\\.");
    Object result = instance;
    for (final String f : fields) {
      result = result.getClass().getField(f).get(result);
    }
    return klazz.cast(result);
  }

  /**
   * Read field value through getters
   *
   * @param <T>       expected type of value
   * @param instance  instance of object, must not be null
   * @param fieldName field name, can be single name or dot-separated one, must
   *                  not be null
   * @param klazz     expected value class, must not be null
   * @return value, can be null
   * @throws Exception it will be thrown if any error
   */
  public static <T> T getFieldThroughGetters(final Object instance, final String fieldName,
                                             final Class<T> klazz) throws Exception {
    final String[] fields = fieldName.split("\\.");
    Object result = instance;
    for (final String f : fields) {
      result = result.getClass().getMethod("get" + f.toUpperCase(Locale.ENGLISH)).invoke(result);
    }
    return klazz.cast(result);
  }

  /**
   * Check PNG chunk data.
   *
   * @param etalonName   etalon type name, must not be null
   * @param etalonLength etalon length in bytes
   * @param chunkType    chunk type
   * @param chunkLength  chunk data length
   * @param chunkCrc     chunk crc field value
   * @param chunkData    chunk data, must not be null
   */
  public static void assertPngChunk(final String etalonName, final int etalonLength,
                                    final int chunkType, final int chunkLength, final int chunkCrc,
                                    final byte[] chunkData) {
    final int chunkEtalonName =
        (etalonName.charAt(0) << 24) | (etalonName.charAt(1) << 16) | (etalonName.charAt(2) << 8) |
            etalonName.charAt(3);

    assertEquals(chunkEtalonName, chunkType, "Chunk must be " + etalonName);
    assertEquals(etalonLength, chunkLength, "Chunk length must be " + etalonLength);

    final PureJavaCrc32 crc32 = new PureJavaCrc32();
    crc32.update(etalonName.charAt(0));
    crc32.update(etalonName.charAt(1));
    crc32.update(etalonName.charAt(2));
    crc32.update(etalonName.charAt(3));

    if (etalonLength != 0) {
      assertEquals(etalonLength, chunkData.length,
          "Data array " + etalonName + " must be " + etalonLength);
      for (final byte b : chunkData) {
        crc32.update(b & 0xFF);
      }
    }

    final int crc = (int) crc32.getValue();
    assertEquals(crc, chunkCrc, "CRC32 for " + etalonName + " must be " + crc);
  }

  public static String wavInt2Str(final int value) {
    return new String(new char[] {(char) (value & 0xFF), (char) ((value >>> 8) & 0xFF),
        (char) ((value >>> 16) & 0xFF), (char) (value >>> 24)});
  }

  public static byte[] getRandomBytes(final int size) {
    final byte[] result = new byte[size];
    final Random random = new Random(System.nanoTime());
    random.nextBytes(result);
    return result;
  }

  public static byte[] makeStringArray(final JBBPByteOrder byteOrder, final String... text)
      throws IOException {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (final JBBPBitOutputStream stream = new JBBPBitOutputStream(out)) {
      stream.writeStringArray(text, byteOrder);
    }
    return out.toByteArray();
  }
}
