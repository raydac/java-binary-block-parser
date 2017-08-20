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
package com.igormaznitsa.jbbp.io;

import com.igormaznitsa.jbbp.mapper.Bin;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * The Interface describes an object which can save data for custom fields in mapped classes.
 *
 * @see Bin
 * @see JBBPOut
 * @since 1.1
 */
public interface JBBPCustomFieldWriter {
    /**
     * To write the field data into the bit stream.
     *
     * @param context             the JBBPOut context, must not be null
     * @param outStream           the current context output stream, must not be null
     * @param instanceToSave      the mapped class instance, must not be null
     * @param instanceCustomField the field of the mapped class which data must be saved into the stream, must not be null
     * @param fieldAnnotation     the field Bin annotation which can be used as extra data source about the field, must not be null
     * @param value               the value found in the field, can be null
     * @throws IOException it will be thrown if it is impossible to process field data and save them into the stream
     */
    void writeCustomField(final JBBPOut context, final JBBPBitOutputStream outStream, final Object instanceToSave, final Field instanceCustomField, final Bin fieldAnnotation, final Object value) throws IOException;
}
