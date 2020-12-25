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

package com.igormaznitsa.jbbp.it;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;


import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;

public abstract class AbstractParserIntegrationTest {

  public static String normalizeEol(final String text) {
    return text
        .replace("\r\n", "\n")
        .replace("\n\r", "\n")
        .replace("\r", "");
  }

  public void assertFileContent(final String fileName, final String content) throws Exception {
    final String fileText;

    try (InputStream inStream = this.getResourceAsInputStream(fileName)) {
      fileText = IOUtils.toString(inStream, StandardCharsets.UTF_8);
    }

    assertEquals(normalizeEol(fileText), normalizeEol(content), "File content must be equals");
  }

  public InputStream getResourceAsInputStream(final String resourceName) {
    final InputStream result = this.getClass().getResourceAsStream(resourceName);
    if (result == null) {
      throw new NullPointerException("Can't find resource '" + resourceName + '\'');
    }
    return result;
  }

  public void assertResource(final String resourceName, final byte[] content) throws Exception {
    try (InputStream in = this.getResourceAsInputStream(resourceName)) {
      assertArrayEquals(new JBBPBitInputStream(in).readByteArray(-1), content,
          "Content of '" + resourceName + "'");
    }
  }
}
