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

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.testaux.AbstractJBBPToJava6ConverterTest;
import com.igormaznitsa.jbbp.utils.TargetSources;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;

import static com.igormaznitsa.jbbp.TestUtils.getField;
import static org.junit.Assert.*;

public class JBBPToJBBPToJava6ConverterExpressionTest extends AbstractJBBPToJava6ConverterTest {

    private final JBBPBitInputStream UNLIMITED_STREAM = new JBBPBitInputStream(new InputStream() {
        @Override
        public int read() throws IOException {
            return RND.nextInt();
        }
    });


    private void assertExpression(final int etalonValue, final String expression) throws Exception {
        assertTrue("Etalon value must not be zero or negative one : " + etalonValue, etalonValue > 0);
        final Object obj = compileAndMakeInstance(String.format("byte [%s] data;", expression));

        callRead(obj, UNLIMITED_STREAM);

        final int detectedlength = getField(obj, "data", byte[].class).length;

        if (etalonValue != detectedlength) {
            System.err.println(JBBPParser.prepare(String.format("byte [%s] data;", expression)).convertToSrc(TargetSources.JAVA_1_6,PACKAGE_NAME+"."+CLASS_NAME).get(0).getResult().values().iterator().next());
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
        assertExpression(123 ^ 345 & 767, "123 ^ 345 & 767");
        assertExpression((123 | 883) ^ 345 & 767, "(123 | 883) ^ 345 & 767");
        assertExpression(123 & 345 | 234 ^ ~123 & 255, "123&345|234^~123&255");
        assertExpression(-123 & 345 | 234 ^ ~-123 & 255, "-123&345|234^~-123&255");
        assertExpression((-123 & (345 | (234 ^ ~-123))) & 1023, "(-123 & (345 | (234 ^ ~-123))) & 1023");
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
    public void testBrackets() throws Exception {
        assertExpression(3*(9/2), "3*(9/2)");
    }

    @Test
    public void testComplex() throws Exception {
        assertExpression(3 * 2 + 8 << 4 - 3, "3*2+8<<4-3");
        assertExpression(3 * 2 + 8 << 4 - 3 & 7, "3*2+8<<4-3&7");
        assertExpression(60|7-~17%1, "60|7-~17%1");
        assertExpression((11 * (8 - 7)) % 13 + (1234 >> 3 << 2) >>> 1 + (13 - 1) / 2 + ((11 + 22) * 33 / 44 % 55) - (123 & 345 | 234 ^ ~123) & 255, "(11 * (8 - 7)) % 13 + ( 1234>>3<<2)>>>1 + (13 - 1) / 2 + ((11 + 22) * 33 / 44 % 55) - (123 & 345 | 234 ^ ~123) & 255");
    }

    @Test
    public void testSynthesidExpression() throws Exception {
        final Random rnd = new Random(5111975);
        final String [] operatorsTwo = new String [] {"-","+","*","/","%",">>",">>>","<<","^","|","&"};
        final String [] operatorsOne = new String [] {"-","+","~"};

        int rightCounter = 0;

        for(int i=0;i<1000;i++){
            final StringBuilder buffer = new StringBuilder();
            if (rnd.nextInt(100)>60) {
                buffer.append(operatorsOne[rnd.nextInt(operatorsOne.length)]);
            }
            buffer.append(1+rnd.nextInt(100));

            buffer.append(operatorsTwo[rnd.nextInt(operatorsTwo.length)]);

            final int totalItems = rnd.nextInt(100)+1;
            int brakeCounter = 0;

            for(int j=0;j<totalItems;j++){
                if (rnd.nextInt(100)>80) {
                    buffer.append(operatorsOne[rnd.nextInt(operatorsOne.length)]);
                }
                buffer.append(1+rnd.nextInt(100));
                buffer.append(operatorsTwo[rnd.nextInt(operatorsTwo.length)]);

                if (rnd.nextInt(100)>80) {
                    buffer.append('(');
                    brakeCounter++;
                }
            }

            buffer.append(1+rnd.nextInt(100));

            while(brakeCounter>0){
                buffer.append(')');
                brakeCounter--;
            }

            String expression = buffer.toString().replace("--","-").replace("++","+");

            Object theInstance;
            final StringBuilder src = new StringBuilder();
            try {
                theInstance = compileAndMakeInstanceSrc("byte [" + expression + "] array;", " public static int makeExpressionResult(){ return " + expression + ";}",src);
            } catch(Exception ex){
                fail("Can't compile : "+expression);
                return;
            }

            try {
                final int etalon = (Integer) theInstance.getClass().getMethod("makeExpressionResult").invoke(null);
                if (etalon > 0 && etalon < 100000) {
                    System.out.println("Testing expression : " + expression);
                    assertEquals(src.toString(),etalon,getField(callRead(theInstance,new JBBPBitInputStream(UNLIMITED_STREAM)),"array", byte[].class).length);
                    rightCounter ++;
                }
            }catch (InvocationTargetException ex){
                if (!(ex.getCause() instanceof ArithmeticException)) {
                    ex.printStackTrace();
                    fail("Unexpected exception : "+ex.getCause());
                    return;
                }
            }
        }

        System.out.println("Totally generated right expressions : "+rightCounter);
    }

}
