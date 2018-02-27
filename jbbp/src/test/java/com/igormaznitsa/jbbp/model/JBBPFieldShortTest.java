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

public class JBBPFieldShortTest {

    @Test
    public void testNameField() {
        final JBBPFieldShort field = new JBBPFieldShort(new JBBPNamedFieldInfo("test.field", "field", 123), (short) 23456);
        final JBBPNamedFieldInfo namedField = field.getNameInfo();
        assertEquals("test.field", namedField.getFieldPath());
        assertEquals("field", namedField.getFieldName());
        assertEquals(123, namedField.getFieldOffsetInCompiledBlock());
    }

    @Test
    public void testgetAsBool_True() {
        assertTrue(new JBBPFieldShort(new JBBPNamedFieldInfo("test.field", "field", 123), (short) 32423).getAsBool());
    }

    @Test
    public void testgetAsBool_False() {
        assertFalse(new JBBPFieldShort(new JBBPNamedFieldInfo("test.field", "field", 123), (short) 0).getAsBool());
    }

    @Test
    public void testgetAsInt() {
        assertEquals(23432, new JBBPFieldShort(new JBBPNamedFieldInfo("test.field", "field", 123), (short) 23432).getAsInt());
        assertEquals(-23432, new JBBPFieldShort(new JBBPNamedFieldInfo("test.field", "field", 123), (short) -23432).getAsInt());
    }

    @Test
    public void testgetAsLong() {
        assertEquals(23432L, new JBBPFieldShort(new JBBPNamedFieldInfo("test.field", "field", 123), (short) 23432).getAsLong());
        assertEquals(-23432L, new JBBPFieldShort(new JBBPNamedFieldInfo("test.field", "field", 123), (short) -23432).getAsLong());
    }

    @Test
    public void testGetAsInvertedBitOrder() {
        assertEquals(0xFFFFFFFFFFFF8000L, new JBBPFieldShort(new JBBPNamedFieldInfo("test.field", "field", 123), (short) 0x1).getAsInvertedBitOrder());
        assertEquals(0x0L, new JBBPFieldShort(new JBBPNamedFieldInfo("test.field", "field", 123), (short) 0x0).getAsInvertedBitOrder());
        assertEquals(0x0000000000004080L, new JBBPFieldShort(new JBBPNamedFieldInfo("test.field", "field", 123), (short) 0x0102).getAsInvertedBitOrder());
    }

}
