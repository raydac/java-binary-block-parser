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

import com.igormaznitsa.jbbp.JBBPCustomFieldTypeProcessor;
import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPFieldTypeParameterContainer;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.model.JBBPAbstractField;
import org.junit.Ignore;
import org.junit.Test;
import org.mdkt.compiler.*;

import java.io.IOException;

public class ParserToJavaClassTest {

    @Test
    public void testConvert_Primitives() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("bit;bool;byte;ubyte;short;ushort;int;<long;bool bool_field;int int_field;");
        final String converted = new ParserToJavaClass("com.igormaznitsa.test","TestClass", parser, null).process().getResult();

        System.out.println(converted);

        final Class<?> theClass = InMemoryJavaCompiler.compile("com.igormaznitsa.test.TestClass",converted);

        theClass.newInstance();
    }

    @Test
    public void testConvert_Arrays() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("bool[10];byte[20];ubyte[30];short[40];ushort[50];>int[60];<long[$$];");
        final String converted = new ParserToJavaClass("com.igormaznitsa.test","TestClass", parser, null).process().getResult();

        System.out.println(converted);

        final Class<?> theClass = InMemoryJavaCompiler.compile("com.igormaznitsa.test.TestClass",converted);

        theClass.newInstance();
    }

    @Test
    public void testConvert_Expression() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("int a;byte[10*(2+a)<<$$];");
        final String converted = new ParserToJavaClass("com.igormaznitsa.test","TestClass", parser, null).process().getResult();

        System.out.println(converted);

        final Class<?> theClass = InMemoryJavaCompiler.compile("com.igormaznitsa.test.TestClass",converted);

        theClass.newInstance();
    }

    @Test
    public void testConvert_Actions() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("skip:34;align:8;reset$$;");
        final String converted = new ParserToJavaClass("com.igormaznitsa.test","TestClass", parser, null).process().getResult();

        System.out.println(converted);

        final Class<?> theClass = InMemoryJavaCompiler.compile("com.igormaznitsa.test.TestClass",converted);

        theClass.newInstance();
    }

    @Ignore
    @Test
    public void testConvert_CustomField() throws Exception {
        final JBBPCustomFieldTypeProcessor proc = new JBBPCustomFieldTypeProcessor() {
            @Override
            public String[] getCustomFieldTypes() {
                return new String[]{"custom"};
            }

            @Override
            public boolean isAllowed(JBBPFieldTypeParameterContainer fieldType, String fieldName, int extraData, boolean isArray) {
                return true;
            }

            @Override
            public JBBPAbstractField readCustomFieldType(JBBPBitInputStream in, JBBPBitOrder bitOrder, int parserFlags, JBBPFieldTypeParameterContainer customTypeFieldInfo, JBBPNamedFieldInfo fieldName, int extraData, boolean readWholeStream, int arrayLength) throws IOException {
                return null;
            }
        };

        final JBBPParser parser = JBBPParser.prepare("custom:(3<<1) [123+334<<$val] test;",proc);
        final String converted = new ParserToJavaClass("com.igormaznitsa.test","TestClass", parser, null).process().getResult();

        System.out.println(converted);

        final Class<?> theClass = InMemoryJavaCompiler.compile("com.igormaznitsa.test.TestClass",converted);

        theClass.newInstance();
    }

}