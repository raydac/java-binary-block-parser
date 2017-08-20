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
import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.utils.JBBPUtils;

/**
 * Describes a bit field.
 *
 * @since 1.0
 */
public final class JBBPFieldBit extends JBBPAbstractField implements JBBPNumericField, BitEntity {
    private static final long serialVersionUID = 3113427734366331529L;
    /**
     * Inside value storage.
     */
    private final int value;

    /**
     * The Value shows how many bits are really contain the value in the byte.
     */
    private final JBBPBitNumber bitNumber;

    /**
     * The Constructor.
     *
     * @param name      a field name info, it can be null.
     * @param value     the field value
     * @param bitNumber number of valuable bits in the value, must not be null
     */
    public JBBPFieldBit(final JBBPNamedFieldInfo name, final int value, final JBBPBitNumber bitNumber) {
        super(name);
        JBBPUtils.assertNotNull(bitNumber, "Number of bits must not be null");
        this.bitNumber = bitNumber;
        this.value = value;
    }

    /**
     * Get the reversed bit representation of the value.
     *
     * @param value the value to be reversed
     * @param bits  number of bits to be reversed, must not be null
     * @return the reversed value
     */
    public static long reverseBits(final byte value, final JBBPBitNumber bits) {
        return JBBPUtils.reverseBitsInByte(value) >>> (8 - bits.getBitNumber()) & bits.getMask();
    }

    /**
     * Get number of valuable bits in the value. It plays informative role and doesn't play role during numeric value getting.
     *
     * @return the number of valuable bits in the value.
     */
    @Override
    public JBBPBitNumber getBitWidth() {
        return this.bitNumber;
    }

    @Override
    public int getAsInt() {
        return this.value & 0xFF;
    }

    @Override
    public long getAsLong() {
        return getAsInt();
    }

    @Override
    public boolean getAsBool() {
        return this.value != 0;
    }

    @Override
    public long getAsInvertedBitOrder() {
        return reverseBits((byte) this.value, this.bitNumber);
    }

    @Override
    public String getTypeAsString() {
        return "bit:" + this.bitNumber.getBitNumber();
    }
}
