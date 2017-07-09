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

package com.igormaznitsa.jbbp.compiler.utils.converter;

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.testaux.AbstractJavaClassCompilerTest;
import org.junit.Ignore;
import org.junit.Test;

public class ConverterToJavaClassSrcTest extends AbstractJavaClassCompilerTest {

    private static final String PACKAGE_NAME = "com.igormaznitsa.test";
    private static final String CLASS_NAME = "TestClass";

    @Test
    public void testSinglePrimitiveNamedFields() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("bit a;byte b;ubyte c;short d;ushort e;bool f;int g;long h;");
        final String classSrc = parser.makeClassSrc(PACKAGE_NAME, CLASS_NAME);
        System.out.println(classSrc);
        final ClassLoader cloader = saveAndCompile(new JavaClassContent(PACKAGE_NAME + '.' + CLASS_NAME, classSrc));
    }

    @Test
    public void testSinglePrimitiveAnonymousFields() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("bit;byte;ubyte;short;ushort;bool;int;long;");
        final String classSrc = parser.makeClassSrc(PACKAGE_NAME, CLASS_NAME);
        System.out.println(classSrc);
        final ClassLoader cloader = saveAndCompile(new JavaClassContent(PACKAGE_NAME + '.' + CLASS_NAME, classSrc));
    }

    @Test
    public void testSinglePrimitiveAnonymousArrayFields() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("bit[1];byte[2];ubyte[3];short[4];ushort[5];bool[6];int[7];long[8];");
        final String classSrc = parser.makeClassSrc(PACKAGE_NAME, CLASS_NAME);
        System.out.println(classSrc);
        final ClassLoader cloader = saveAndCompile(new JavaClassContent(PACKAGE_NAME + '.' + CLASS_NAME, classSrc));
    }

    @Test
    public void testActions() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("reset$$;skip:8;align:22;");
        final String classSrc = parser.makeClassSrc(PACKAGE_NAME, CLASS_NAME);
        System.out.println(classSrc);
        final ClassLoader cloader = saveAndCompile(new JavaClassContent(PACKAGE_NAME + '.' + CLASS_NAME, classSrc));
    }

    @Test
    public void testStruct() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("int;{byte;ubyte;{long;}}");
        final String classSrc = parser.makeClassSrc(PACKAGE_NAME, CLASS_NAME);
        System.out.println(classSrc);
        final ClassLoader cloader = saveAndCompile(new JavaClassContent(PACKAGE_NAME + '.' + CLASS_NAME, classSrc));
    }

    @Test
    public void testPngParsing() throws Exception {
        final JBBPParser pngParser = JBBPParser.prepare(
                "long header;"
                        + "// chunks\n"
                        + "chunk [_]{"
                        + "   int length; "
                        + "   int type; "
                        + "   byte[length] data; "
                        + "   int crc;"
                        + "}"
        );
        final String classSrc = pngParser.makeClassSrc(PACKAGE_NAME, CLASS_NAME);
        System.out.println(classSrc);
        final ClassLoader cloader = saveAndCompile(new JavaClassContent(PACKAGE_NAME + '.' + CLASS_NAME, classSrc));
    }


}