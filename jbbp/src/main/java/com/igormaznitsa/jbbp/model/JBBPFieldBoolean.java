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

/**
 * Describes a boolean field.
 *
 * @since 1.0
 */
public final class JBBPFieldBoolean extends JBBPAbstractField implements JBBPNumericField {
    private static final long serialVersionUID = 4165558936928450699L;
    /**
     * Inside value storage.
     */
    private final boolean value;

    /**
     * The Constructor.
     *
     * @param name  a field name info, it can be null.
     * @param value the field value
     */
    public JBBPFieldBoolean(final JBBPNamedFieldInfo name, final boolean value) {
        super(name);
        this.value = value;
    }

    @Override
    public double getAsDouble() {
        return value ? 1.0d : 0.0d;
    }

    @Override
    public float getAsFloat() {
        return value ? 1.0f : 0.0f;
    }

    /**
     * Get the reversed bit representation of the value. But for boolean it doesn't work and made for compatibility
     *
     * @param value the value to be reversed
     * @return the reversed value
     */
    public static long reverseBits(final boolean value) {
        return value ? 1 : 0;
    }

    @Override
    public int getAsInt() {
        return this.value ? 1 : 0;
    }

    @Override
    public long getAsLong() {
        return this.getAsInt();
    }

    @Override
    public boolean getAsBool() {
        return this.value;
    }

    @Override
    public long getAsInvertedBitOrder() {
        return reverseBits(this.value);
    }

    @Override
    public String getTypeAsString() {
        return "bool";
    }
}
