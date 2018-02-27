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
package com.igormaznitsa.jbbp;

import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPFieldTypeParameterContainer;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.model.*;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class JBBPCustomFieldTypeProcessorTest {

    @Test
    public void testFieldsWithCustomNames() throws Exception {
        final AtomicInteger callCounter = new AtomicInteger();

        final JBBPCustomFieldTypeProcessor testProcessor = new JBBPCustomFieldTypeProcessor() {
            private final String[] types = new String[]{"some1", "some2", "some3"};

            @Override
            public String[] getCustomFieldTypes() {
                return this.types;
            }

            @Override
            public boolean isAllowed(final JBBPFieldTypeParameterContainer fieldType, final String fieldName, final int extraData, final boolean isArray) {
                callCounter.incrementAndGet();

                assertNotNull(fieldType);
                final String type = fieldType.getTypeName();
                assertTrue(type.equals("some1") || type.equals("some2") || type.equals("some3"));

                if (fieldName.equals("b")) {
                    assertEquals("some1", type);
                }
                if (fieldName.equals("c")) {
                    assertEquals("some2", type);
                }
                if (fieldName.equals("e")) {
                    assertEquals("some3", type);
                }

                if (type.equals("some3")) {
                    assertTrue(isArray);
                } else {
                    assertFalse(isArray);
                }

                if (type.equals("some2")) {
                    assertEquals(345, extraData);
                } else {
                    assertEquals(0, extraData);
                }

                return true;
            }

            @Override
            public JBBPAbstractField readCustomFieldType(final JBBPBitInputStream in, final JBBPBitOrder bitOrder, final int parserFlags, final JBBPFieldTypeParameterContainer customFieldTypeInfo, final JBBPNamedFieldInfo fieldName, final int extraData, final boolean readWholeStream, final int arrayLength) throws IOException {
                final String type = customFieldTypeInfo.getTypeName();

                assertEquals(JBBPBitOrder.LSB0, bitOrder);

                assertEquals(JBBPParser.FLAG_SKIP_REMAINING_FIELDS_IF_EOF, parserFlags);
                assertEquals(type.equals("some1") ? JBBPByteOrder.LITTLE_ENDIAN : JBBPByteOrder.BIG_ENDIAN, customFieldTypeInfo.getByteOrder());

                if (type.equals("some1")) {
                    assertEquals(0, extraData);
                    assertEquals("b", fieldName.getFieldName());
                    assertFalse(readWholeStream);
                    assertEquals(-1, arrayLength);
                    return new JBBPFieldByte(fieldName, (byte) in.readByte());
                } else if (type.equals("some2")) {
                    assertEquals(345, extraData);
                    assertEquals("c", fieldName.getFieldName());
                    assertFalse(readWholeStream);
                    assertEquals(-1, arrayLength);
                    return new JBBPFieldShort(fieldName, (short) in.readUnsignedShort(customFieldTypeInfo.getByteOrder()));
                } else if (type.equals("some3")) {
                    assertEquals(0, extraData);
                    assertEquals("e", fieldName.getFieldName());
                    assertFalse(readWholeStream);
                    assertEquals(5, arrayLength);
                    return new JBBPFieldArrayByte(fieldName, in.readByteArray(arrayLength));
                } else {
                    fail("Unexpected " + type);
                    return null;
                }
            }
        };

        final JBBPParser parser = JBBPParser.prepare("int a; <some1 b; some2:345 c; long d; some3 [5] e;", JBBPBitOrder.LSB0, testProcessor, JBBPParser.FLAG_SKIP_REMAINING_FIELDS_IF_EOF);
        assertEquals(3, callCounter.get());

        final JBBPFieldStruct parsed = parser.parse(new byte[]{
                (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
                (byte) 0xAB,
                (byte) 0xCD, (byte) 0xDE,
                (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08,
                (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD, (byte) 0xEE,
                (byte) 0xFF, (byte) 0xFF
        });

        assertEquals(0x12345678, parsed.findFieldForNameAndType("a", JBBPFieldInt.class).getAsInt());
        assertEquals((byte) 0xAB, parsed.findFieldForNameAndType("b", JBBPFieldByte.class).getAsInt());
        assertEquals((short) 0xCDDE, parsed.findFieldForNameAndType("c", JBBPFieldShort.class).getAsInt());
        assertEquals(0x0102030405060708L, parsed.findFieldForNameAndType("d", JBBPFieldLong.class).getAsLong());
        assertArrayEquals(new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD, (byte) 0xEE,}, parsed.findFieldForNameAndType("e", JBBPFieldArrayByte.class).getArray());
    }

    @Test
    public void testFieldsWithCustomNames_ExpressionInExtraFieldValue() throws Exception {
        final AtomicInteger callCounter = new AtomicInteger();

        final JBBPCustomFieldTypeProcessor testProcessor = new JBBPCustomFieldTypeProcessor() {
            private final String[] types = new String[]{"some1", "some2", "some3"};

            @Override
            public String[] getCustomFieldTypes() {
                return this.types;
            }

            @Override
            public boolean isAllowed(final JBBPFieldTypeParameterContainer fieldType, final String fieldName, final int extraData, final boolean isArray) {
                callCounter.incrementAndGet();

                assertNotNull(fieldType);
                final String type = fieldType.getTypeName();
                assertTrue(type.equals("some1") || type.equals("some2") || type.equals("some3"));

                if (fieldName.equals("b")) {
                    assertEquals("some1", type);
                }
                if (fieldName.equals("c")) {
                    assertEquals("some2", type);
                }
                if (fieldName.equals("e")) {
                    assertEquals("some3", type);
                }

                if (type.equals("some3")) {
                    assertTrue(isArray);
                } else {
                    assertFalse(isArray);
                }

                assertEquals(-1, extraData);

                return true;
            }

            @Override
            public JBBPAbstractField readCustomFieldType(final JBBPBitInputStream in, final JBBPBitOrder bitOrder, final int parserFlags, final JBBPFieldTypeParameterContainer customFieldTypeInfo, final JBBPNamedFieldInfo fieldName, final int extraData, final boolean readWholeStream, final int arrayLength) throws IOException {
                final String type = customFieldTypeInfo.getTypeName();

                if (type.equals("some1")) {
                    assertEquals(0x12345678 * 2, extraData);
                    return new JBBPFieldByte(fieldName, (byte) in.readByte());
                }
                if (type.equals("some2")) {
                    assertEquals(0x12345678 * 3, extraData);
                    return new JBBPFieldShort(fieldName, (short) in.readUnsignedShort(customFieldTypeInfo.getByteOrder()));
                }
                if (type.equals("some3")) {
                    assertEquals(0x12345678 * 4, extraData);
                    return new JBBPFieldArrayByte(fieldName, in.readByteArray(arrayLength));
                }

                fail("Unexpected field " + type);
                return null;
            }
        };

        final JBBPParser parser = JBBPParser.prepare("int a; some1:(a*2) b; some2:(a*3) c; long d; some3:(a*4) [5] e;", JBBPBitOrder.LSB0, testProcessor, JBBPParser.FLAG_SKIP_REMAINING_FIELDS_IF_EOF);
        assertEquals(3, callCounter.get());

        final JBBPFieldStruct parsed = parser.parse(new byte[]{
                (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
                (byte) 0xAB,
                (byte) 0xCD, (byte) 0xDE,
                (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08,
                (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD, (byte) 0xEE,
                (byte) 0xFF, (byte) 0xFF
        });

        assertEquals(0x12345678, parsed.findFieldForNameAndType("a", JBBPFieldInt.class).getAsInt());
        assertEquals((byte) 0xAB, parsed.findFieldForNameAndType("b", JBBPFieldByte.class).getAsInt());
        assertEquals((short) 0xCDDE, parsed.findFieldForNameAndType("c", JBBPFieldShort.class).getAsInt());
        assertEquals(0x0102030405060708L, parsed.findFieldForNameAndType("d", JBBPFieldLong.class).getAsLong());
        assertArrayEquals(new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD, (byte) 0xEE,}, parsed.findFieldForNameAndType("e", JBBPFieldArrayByte.class).getArray());
    }

}
