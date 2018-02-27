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
package com.igormaznitsa.jbbp.model;

import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class JBBPFieldIntTest {

    @Test
    public void testNameField() {
        final JBBPFieldInt field = new JBBPFieldInt(new JBBPNamedFieldInfo("test.field", "field", 123), 123456);
        final JBBPNamedFieldInfo namedField = field.getNameInfo();
        assertEquals("test.field", namedField.getFieldPath());
        assertEquals("field", namedField.getFieldName());
        assertEquals(123, namedField.getFieldOffsetInCompiledBlock());
    }

    @Test
    public void testgetAsBool_True() {
        assertTrue(new JBBPFieldInt(new JBBPNamedFieldInfo("test.field", "field", 123), 32423).getAsBool());
    }

    @Test
    public void testgetAsBool_False() {
        assertFalse(new JBBPFieldInt(new JBBPNamedFieldInfo("test.field", "field", 123), 0).getAsBool());
    }

    @Test
    public void testgetAsInt() {
        assertEquals(234324, new JBBPFieldInt(new JBBPNamedFieldInfo("test.field", "field", 123), 234324).getAsInt());
        assertEquals(-234324, new JBBPFieldInt(new JBBPNamedFieldInfo("test.field", "field", 123), -234324).getAsInt());
    }

    @Test
    public void testgetAsLong() {
        assertEquals(234324L, new JBBPFieldInt(new JBBPNamedFieldInfo("test.field", "field", 123), 234324).getAsLong());
        assertEquals(-234324L, new JBBPFieldInt(new JBBPNamedFieldInfo("test.field", "field", 123), -234324).getAsLong());
    }


    @Test
    public void testGetAsInvertedBitOrder() {
        assertEquals(0x0000000020C04080L, new JBBPFieldInt(new JBBPNamedFieldInfo("test.field", "field", 123), 0x01020304).getAsInvertedBitOrder());
        assertEquals(0x000000007FFFFFFFL, new JBBPFieldInt(new JBBPNamedFieldInfo("test.field", "field", 123), 0xFFFFFFFE).getAsInvertedBitOrder());
        assertEquals(0xFFFFFFFF80000000L, new JBBPFieldInt(new JBBPNamedFieldInfo("test.field", "field", 123), 0x00000001).getAsInvertedBitOrder());
    }


}
