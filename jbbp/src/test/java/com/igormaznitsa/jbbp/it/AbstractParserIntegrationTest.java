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

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.utils.JBBPUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

public abstract class AbstractParserIntegrationTest {

  public void assertFile(final String fileName, final String text) throws Exception {
    final InputStream in = this.getClass().getResourceAsStream(fileName);
    assertNotNull(in, "Can't find file [" + fileName + "]");
    Reader reader = null;
    String fileText;
    try {
      reader = new InputStreamReader(in, "UTF-8");
      final StringWriter wr = new StringWriter();

      while (true) {
        final int chr = reader.read();
        if (chr < 0) {
          break;
        }
        wr.write(chr);
      }
      wr.close();
      fileText = wr.toString();
    } finally {
      if (reader != null) {
        reader.close();
      }
    }

    assertEquals(fileText, text, "File content must be equals");
  }

  public InputStream getResourceAsInputStream(final String resourceName) throws Exception {
    final InputStream result = this.getClass().getResourceAsStream(resourceName);
    if (result == null) {
      throw new NullPointerException("Can't find resource '" + resourceName + '\'');
    }
    return result;
  }

  public void assertResource(final String resourceName, final byte[] content) throws Exception {
    final InputStream in = getResourceAsInputStream(resourceName);
    try {
      final byte[] fileContent = new JBBPBitInputStream(in).readByteArray(-1);
      assertArrayEquals(fileContent, content, "Content of '" + resourceName + "'");
    } finally {
      JBBPUtils.closeQuietly(in);
    }
  }
}
