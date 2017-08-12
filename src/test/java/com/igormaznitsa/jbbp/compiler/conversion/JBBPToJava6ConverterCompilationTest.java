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

import com.igormaznitsa.jbbp.JBBPCustomFieldTypeProcessor;
import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPFieldTypeParameterContainer;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.model.JBBPAbstractField;
import com.igormaznitsa.jbbp.testaux.AbstractJavaClassCompilerTest;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JBBPToJava6ConverterCompilationTest extends AbstractJavaClassCompilerTest {

    private void assertCompilation(final String classSrc) throws Exception {
        System.out.println(classSrc);
        assertNotNull(saveAndCompile(new JavaClassContent(PACKAGE_NAME + '.' + CLASS_NAME, classSrc)));
    }

    @Test
    public void testExpression() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("bit:8 bitf; var somevar; bool bbb; long aaa; ubyte kkk; {{int lrn; {int [(lrn/aaa*1*(2*somevar-4)&$joomla)/(100%9>>bitf)&56|~kkk^78&bbb];}}}");
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, "some multiline text\nto be added into header", false));
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, "some multiline text\nto be added into header", true));
    }

    @Test
    public void testForceAbstract() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("byte a;");
        assertFalse(JBBPToJava6Converter.makeBuilder(parser).setClassName(CLASS_NAME).setForceAbstract(false).build().convert().contains("abstract"));
        assertTrue(JBBPToJava6Converter.makeBuilder(parser).setClassName(CLASS_NAME).setForceAbstract(true).build().convert().contains("abstract"));
    }

    @Test
    public void testCustomText() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("byte a;");
        assertTrue(JBBPToJava6Converter.makeBuilder(parser).setClassName(CLASS_NAME).setCustomText("public void test(){}").build().convert().contains("public void test(){}"));
    }

    @Test
    public void testSuperclass() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("byte a;");
        assertTrue(JBBPToJava6Converter.makeBuilder(parser).setClassName(CLASS_NAME).setSuperclass("com.igormaznitsa.Super").build().convert().contains("extends com.igormaznitsa.Super "));
    }

    @Test
    public void testInterfaces() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("byte a;");
        assertTrue(JBBPToJava6Converter.makeBuilder(parser).setClassName(CLASS_NAME).setInterfaces("com.igormaznitsa.InterfaceA","com.igormaznitsa.InterfaceB").build().convert().contains("implements com.igormaznitsa.InterfaceA,com.igormaznitsa.InterfaceB "));
    }

    @Test
    public void testClassPackage() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("byte a;");
        assertFalse(JBBPToJava6Converter.makeBuilder(parser).setClassName(CLASS_NAME).build().convert().contains("package "));
        assertFalse(JBBPToJava6Converter.makeBuilder(parser).setClassName(CLASS_NAME).setClassPackage("").build().convert().contains("package "));
        assertTrue(JBBPToJava6Converter.makeBuilder(parser).setClassName(CLASS_NAME).setClassPackage("hello.world").build().convert().contains("package hello.world;"));
    }

    @Test
    public void testGettersSetters() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("byte a;");
        String text = JBBPToJava6Converter.makeBuilder(parser).setClassName(CLASS_NAME).build().convert();
        assertTrue(text.contains("public byte a;"));
        assertFalse(text.contains("public void setA(byte value) {"));
        assertFalse(text.contains("public byte getA() {"));

        text = JBBPToJava6Converter.makeBuilder(parser).setDoGettersSetters(true).setClassName(CLASS_NAME).build().convert();

        assertFalse(text.contains("public byte a;"));
        assertTrue(text.contains("private byte a;"));
        assertTrue(text.contains("public void setA(byte value) {"));
        assertTrue(text.contains("public byte getA() {"));
    }

    @Test
    public void testZ80snap1() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("byte reg_a; byte reg_f; <short reg_bc; <short reg_hl; <short reg_pc; <short reg_sp; byte reg_ir; byte reg_r; "
                + "flags{ bit:1 reg_r_bit7; bit:3 bordercolor; bit:1 basic_samrom; bit:1 compressed; bit:2 nomeaning;}"
                + "<short reg_de; <short reg_bc_alt; <short reg_de_alt; <short reg_hl_alt; byte reg_a_alt; byte reg_f_alt; <short reg_iy; <short reg_ix; byte iff; byte iff2;"
                + "emulFlags{bit:2 interruptmode; bit:1 issue2emulation; bit:1 doubleintfreq; bit:2 videosync; bit:2 inputdevice;}"
                + "byte [_] data;");
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, false));
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, true));
    }

    @Test
    public void testSinglePrimitiveNamedFields() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("bit a;byte b;ubyte c;short d;ushort e;bool f;int g;long h;");
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, false));
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, true));
    }

    @Test
    public void testSinglePrimitiveAnonymousFields() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("bit;byte;ubyte;short;ushort;bool;int;long;");
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, false));
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, true));
    }

    @Test
    public void testSinglePrimitiveAnonymousArrayFields() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("bit[1];byte[2];ubyte[3];short[4];ushort[5];bool[6];int[7];long[8];");
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, false));
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, true));
    }

    @Test
    public void testActions() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("reset$$;skip:8;align:22;");
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, false));
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, true));
    }

    @Test
    public void testStruct() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("int;{byte;ubyte;{long;}}");
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, false));
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, true));
    }

    @Test
    public void testPrimitiveArrayInsideStructArray() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("ubyte len; {ubyte[len];} ubyte [_] rest;");
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, false));
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, true));
    }

    @Test
    public void testExternalValueInExpression() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("ubyte len; <int [len*2+$ex] hello;");
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, false));
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, true));
    }

    @Test
    public void testCustomType() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("some alpha; some:1 beta;", new JBBPCustomFieldTypeProcessor() {
            @Override
            public String[] getCustomFieldTypes() {
                return new String[]{"some"};
            }

            @Override
            public boolean isAllowed(JBBPFieldTypeParameterContainer fieldType, String fieldName, int extraData, boolean isArray) {
                return true;
            }

            @Override
            public JBBPAbstractField readCustomFieldType(JBBPBitInputStream in, JBBPBitOrder bitOrder, int parserFlags, JBBPFieldTypeParameterContainer customTypeFieldInfo, JBBPNamedFieldInfo fieldName, int extraData, boolean readWholeStream, int arrayLength) throws IOException {
                return null;
            }
        });

        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, false));
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, true));
    }

    @Test
    public void testVarType() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("var alpha; var [$$] beta;");
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, false));
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, true));
    }

    @Test
    public void testAllVariantsWithLinksToExternalStructures() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("bit:1 bit1; bit:2 bit2; bit:3 bit3; bit:4 bit4; bit:5 bit5; bit:6 bit6; bit:7 bit7; bit:8 bit8;"
                + "byte alpha; ubyte beta; short gamma; ushort delta; bool epsilon; int teta; long longField; var varField;"
                + "struct1 { byte someByte; struct2 {bit:3 [34*someByte<<1+$ext] data;} }");
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, false));
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, true));
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
        assertCompilation(pngParser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, false));
        assertCompilation(pngParser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, true));
    }

    @Test
    public void testPrimitiveFieldsInExpression() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("long lfield; int ifield; byte bfield; ggg {ubyte ubfield; short shfield;} ushort ushfield; bit:4 bitfield; byte [bfield*ggg.shfield<<bitfield-ggg.ubfield&ushfield%lfield/ifield] array;");
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, false));
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, true));
    }

    @Test
    public void testAllTypes() throws Exception {
        final JBBPParser parser = JBBPParser.prepare("custom alpha; custom [123] beta; {{ var [alpha*$extr] variarr; var fld;}}", new JBBPCustomFieldTypeProcessor() {
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
        });
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, false));
        assertCompilation(parser.makeJavaSources(PACKAGE_NAME, CLASS_NAME, true));
    }

}
