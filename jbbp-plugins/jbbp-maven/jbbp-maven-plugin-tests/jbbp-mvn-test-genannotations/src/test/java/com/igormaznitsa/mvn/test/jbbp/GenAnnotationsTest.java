/*
 * Copyright 2019 Igor Maznitsa.
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

package com.igormaznitsa.mvn.test.jbbp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;


import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class GenAnnotationsTest {

  @Test
  void testReadWrite() throws IOException {
    final byte[] testData = new byte[] {4, (byte) 0x12, (byte) 0x34, 3, 5, 6, 7};

    final GenAnnotations result =
        new GenAnnotations().read(new JBBPBitInputStream(new ByteArrayInputStream(testData)));
    assertEquals(4, result.getLEN());
    assertEquals(3, result.getSOME1().getSOME2().getFIELD().length);

    final String script = "ubyte len;"
        + "some1 {"
        + " bit:4 [len] someField;"
        + " ubyte len;"
        + " some2 {"
        + "   byte [len] field;"
        + " }"
        + "}";

    final GenAnnotations instance =
        JBBPParser.prepare(script).parse(testData).mapTo(new GenAnnotations());
    assertEquals(result.getLEN(), instance.getLEN());
    assertEquals(result.getSOME1().getLEN(), instance.getSOME1().getLEN());
    assertArrayEquals(result.getSOME1().getSOMEFIELD(), instance.getSOME1().getSOMEFIELD());
    assertArrayEquals(result.getSOME1().getSOME2().getFIELD(),
        instance.getSOME1().getSOME2().getFIELD());
  }
}
