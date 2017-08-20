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
import com.igormaznitsa.jbbp.utils.JBBPUtils;

/**
 * Describes a long value field.
 *
 * @since 1.0
 */
public final class JBBPFieldLong extends JBBPAbstractField implements JBBPNumericField {
    private static final long serialVersionUID = -7006473435241171461L;
    /**
     * Inside value storage.
     */
    private final long value;

    /**
     * The COnstructor.
     *
     * @param name  a field name info, it can be null
     * @param value the field value
     */
    public JBBPFieldLong(final JBBPNamedFieldInfo name, final long value) {
        super(name);
        this.value = value;
    }

    /**
     * Get the reversed bit representation of the value.
     *
     * @param value the value to be reversed
     * @return the reversed value
     */
    public static long reverseBits(final long value) {
        final long b0 = JBBPUtils.reverseBitsInByte((byte) value) & 0xFFL;
        final long b1 = JBBPUtils.reverseBitsInByte((byte) (value >> 8)) & 0xFFL;
        final long b2 = JBBPUtils.reverseBitsInByte((byte) (value >> 16)) & 0xFFL;
        final long b3 = JBBPUtils.reverseBitsInByte((byte) (value >> 24)) & 0xFFL;
        final long b4 = JBBPUtils.reverseBitsInByte((byte) (value >> 32)) & 0xFFL;
        final long b5 = JBBPUtils.reverseBitsInByte((byte) (value >> 40)) & 0xFFL;
        final long b6 = JBBPUtils.reverseBitsInByte((byte) (value >> 48)) & 0xFFL;
        final long b7 = JBBPUtils.reverseBitsInByte((byte) (value >> 56)) & 0xFFL;

        return (b0 << 56) | (b1 << 48) | (b2 << 40) | (b3 << 32) | (b4 << 24) | (b5 << 16) | (b6 << 8) | b7;
    }

    @Override
    public int getAsInt() {
        return (int) this.value;
    }

    @Override
    public long getAsLong() {
        return this.value;
    }

    @Override
    public boolean getAsBool() {
        return this.value != 0;
    }

    @Override
    public long getAsInvertedBitOrder() {
        return reverseBits(this.value);
    }

    @Override
    public String getTypeAsString() {
        return "long";
    }

}
