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

import com.igormaznitsa.jbbp.compiler.JBBPCompiledBlock;

/**
 * The Interface describes a provider which can provide numeric size for decoded arrays by their names.
 *
 * @since 1.0
 */
public interface JBBPExternalValueProvider {
    /**
     * Get an array size.
     *
     * @param fieldName       the field name of the array
     * @param numericFieldMap the numeric field map contains information about already read fields
     * @param compiledBlock   the compiled block for the script to provide extra information
     * @return the size of an array
     */
    int provideArraySize(final String fieldName, final JBBPNamedNumericFieldMap numericFieldMap, final JBBPCompiledBlock compiledBlock);
}
