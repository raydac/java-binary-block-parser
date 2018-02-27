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

package com.igormaznitsa.jbbp.exceptions;

import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.model.JBBPAbstractField;
import com.igormaznitsa.jbbp.model.JBBPFieldByte;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class JBBPMapperExceptionTest {
    private final JBBPAbstractField field = new JBBPFieldByte(new JBBPNamedFieldInfo("test.test", "test", 0), (byte) 1);
    private final Exception cause = new Exception();
    private final String message = "Message";

    @Test
    public void testConstructorAndGetters() throws Exception {
        final Class<?> clazz = Integer.class;
        final Field clazzField = Integer.class.getDeclaredFields()[0];

        assertNotNull(clazzField);

        final JBBPMapperException ex = new JBBPMapperException(this.message, this.field, clazz, clazzField, this.cause);
        assertSame(this.message, ex.getMessage());
        assertSame(this.field, ex.getField());
        assertSame(clazz, ex.getMappingClass());
        assertSame(clazzField, ex.getMappingClassField());
    }

    @Test
    public void testToString() throws Exception {
        final Class<?> clazz = Integer.class;
        final Field clazzField = Integer.class.getDeclaredFields()[0];
        assertNotNull(clazzField);
        final JBBPMapperException ex = new JBBPMapperException(this.message, this.field, clazz, clazzField, this.cause);
        assertTrue(ex.toString().contains(clazzField.toString()));
    }

}
