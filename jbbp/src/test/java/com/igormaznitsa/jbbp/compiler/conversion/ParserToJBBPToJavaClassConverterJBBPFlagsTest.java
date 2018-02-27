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
import com.igormaznitsa.jbbp.testaux.AbstractJBBPToJava6ConverterTest;

import java.io.EOFException;

import static com.igormaznitsa.jbbp.TestUtils.getField;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Test parser flags for converted sources.
 */
public class ParserToJBBPToJavaClassConverterJBBPFlagsTest extends AbstractJBBPToJava6ConverterTest {

    @Test
    public void testFlag_SkipRemainingFieldsIfEOF() throws Exception {
        Object instance = compileAndMakeInstance("byte a; byte b;", 0);
        callRead(instance, new byte[]{1, 2});
        try {
            callRead(instance, new byte[]{1});
            fail("Must throw EOF");
        } catch (EOFException ex) {
        }

        instance = compileAndMakeInstance("byte a; byte b;", JBBPParser.FLAG_SKIP_REMAINING_FIELDS_IF_EOF);
        try {
            callRead(instance, new byte[]{11});
            assertEquals(11, getField(instance, "a", Byte.class).intValue());
            assertEquals(0, getField(instance, "b", Byte.class).intValue());
        } catch (EOFException ex) {
            fail("Must not throw EOF");
        }
    }

}
