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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Different useful auxiliary test methods
 */
public enum TestUtils {
    ;

    /**
     * Inject new value into final field
     *
     * @param klazz     a class which field must be injected, must not be null
     * @param instance  the instance of the class, it can be null for static fields
     * @param fieldName the field name, must not be null
     * @param value     the value to be injected
     * @throws Exception it will be thrown for any error
     */
    public static void injectDeclaredFinalFieldValue(final Class<?> klazz, final Object instance, final String fieldName, final Object value) throws Exception {
        final Field field = klazz.getDeclaredField(fieldName);
        field.setAccessible(true);

        final Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(instance, value);
    }

    /**
     * Get a field value.
     *
     * @param klazz     a class which field will be read, must not be null
     * @param instance  an instance of the class, can be null for static fields
     * @param fieldName the field name, must not be null
     * @return the field value
     * @throws Exception it will be thrown for any error
     */
    public static Object getFieldValue(final Class<?> klazz, final Object instance, final String fieldName) throws Exception {
        final Field field = klazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(instance);
    }
}
