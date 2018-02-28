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

package com.igormaznitsa.mvn.test.jbbp;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;


public class WholeStreamByteArrayTest {

  @Test
  public void testRead_DefaultBitOrder() throws IOException {
    final WholeStreamByteArray struct = new WholeStreamByteArray().read(new JBBPBitInputStream(new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10})));
    assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, struct.array);
  }


  @Test
  public void testWrite_DefaultBitOrder() throws IOException {
    final WholeStreamByteArray struct = new WholeStreamByteArray();
    struct.array = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final JBBPBitOutputStream bitOut = new JBBPBitOutputStream(out);
    struct.write(bitOut);
    bitOut.close();
    assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, out.toByteArray());
  }

}
