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
 * Inside abstract class to keep common operations for byte array based entities.
 *
 * @param <T> type of array item.
 * @since 1.1.1
 */
abstract class AbstractFieldByteArray<T extends JBBPAbstractField> extends JBBPAbstractArrayField<T> {
    private static final long serialVersionUID = -884448637983315507L;

    protected final byte[] array;

    public AbstractFieldByteArray(final JBBPNamedFieldInfo name, final byte[] array) {
        super(name);
        JBBPUtils.assertNotNull(array, "Array must not be null");
        this.array = array;
    }

    @Override
    public boolean getAsBool(final int index) {
        return this.array[index] != 0;
    }

    @Override
    public long getAsLong(final int index) {
        return this.getAsInt(index);
    }

}
