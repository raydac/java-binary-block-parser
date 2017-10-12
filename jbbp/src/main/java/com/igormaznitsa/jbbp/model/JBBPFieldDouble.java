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
 * Describes a double value field.
 *
 * @since 1.3.1
 */
public final class JBBPFieldDouble extends JBBPAbstractField implements JBBPNumericField {
    private static final long serialVersionUID = -7106473415348171461L;

    public static final String TYPE_NAME = "doublej";

    /**
     * Inside value storage.
     */
    private final double value;

    /**
     * The Constructor.
     *
     * @param name  a field name info, it can be null
     * @param value the field value
     */
    public JBBPFieldDouble(final JBBPNamedFieldInfo name, final double value) {
        super(name);
        this.value = value;
    }

    @Override
    public double getAsDouble() {
        return this.value;
    }

    @Override
    public float getAsFloat() {
        return (float)this.value;
    }

    @Override
    public int getAsInt() {
        return (int) Math.round(this.value);
    }

    @Override
    public long getAsLong() {
        return Math.round(this.value);
    }

    @Override
    public boolean getAsBool() {
        return this.value != 0;
    }

    @Override
    public long getAsInvertedBitOrder() {
        return JBBPFieldLong.reverseBits(Double.doubleToLongBits(this.value));
    }

    @Override
    public String getTypeAsString() {
        return TYPE_NAME;
    }

}
