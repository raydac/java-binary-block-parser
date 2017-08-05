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
package com.igormaznitsa.jbbp.compiler.conversion;

import static org.junit.Assert.*;
import java.io.IOException;
import java.io.InputStream;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.testaux.AbstractJavaClassCompilerTest;
import static com.igormaznitsa.jbbp.TestUtils.*;
import org.junit.Test;
import com.igormaznitsa.jbbp.JBBPParser;

public class ParserToJavaClassConverterExpressionTest extends AbstractJavaClassCompilerTest {

  private void assertExpression(final int etalonValue, final String expression) throws Exception {
    assertTrue("Etalon value must not be zero or egative one : " + etalonValue, etalonValue > 0);
    final Object obj = compileAndMakeInstance(String.format("byte [%s] data;", expression));

    final JBBPBitInputStream in = new JBBPBitInputStream(new InputStream() {
      @Override
      public int read() throws IOException {
        return RND.nextInt();
      }
    });

    callRead(obj, in);

    final int detectedlength = getField(obj, "data", byte[].class).length;

    if (etalonValue != detectedlength) {
      System.err.println(JBBPParser.prepare(String.format("byte [%s] data;", expression)).makeClassSrc(PACKAGE_NAME, CLASS_NAME));
      fail(etalonValue + "!=" + detectedlength);
    }
  }

  @Test
  public void testArithmeticOps() throws Exception {
    assertExpression(11 * (+8 - 7) % 13 + (-13 - 1) / 2, "11*(+8-7)%13+(-13-1)/2");
    assertExpression(11 + 22 * 33 / 44 % 55, "11 + 22 * 33 / 44 % 55");
    assertExpression((3 * (5 * 7)) / 11, "(3 * (5 * 7)) / 11");
  }

  @Test
  public void testBitOps() throws Exception {
    assertExpression(123 & 345 | 234 ^ ~123 & 255, "123&345|234^~123&255");
    assertExpression(-123 & 345 | 234 ^ ~-123 & 255, "-123&345|234^~-123&255");
  }

  @Test
  public void testShifts() throws Exception {
    assertExpression(1234 >> 3 << 2 >>> 1, "1234>>3<<2>>>1");
    assertExpression((123456 >> (3 << 2)) >>> 1, "(123456>>(3<<2))>>>1");
    assertExpression(56 << (3 << 2), "56 << (3 << 2)");
    assertExpression(56 << 3 << 2, "56 << 3 << 2");
    assertExpression(123456 >> (3 << 2), "123456>>(3<<2)");
  }

  @Test
  public void testComplex() throws Exception {
    assertExpression((11 * (8 - 7)) % 13 + (1234 >> 3 << 2) >>> 1 + (13 - 1) / 2 + ((11 + 22) * 33 / 44 % 55) - (123 & 345 | 234 ^ ~123) & 255, "(11 * (8 - 7)) % 13 + ( 1234>>3<<2)>>>1 + (13 - 1) / 2 + ((11 + 22) * 33 / 44 % 55) - (123 & 345 | 234 ^ ~123) & 255");
  }

}
