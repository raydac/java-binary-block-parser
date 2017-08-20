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

import java.util.Map;
import java.util.Properties;

/**
 * Contains result of conversion of JBBPParser into source.
 *
 * @since 1.3.0
 */
public interface ResultSrcItem {
    /**
     * Get metadata generated during operation for the item, depends on converter.
     *
     * @return the metadata container as properties, must not be null
     */
    Properties getMetadata();

    /**
     * Get generated sources mapped by some key which defined by converter.
     *
     * @return map containing result of conversion, must not be null and can't be empty.
     */
    Map<String, String> getResult();
}
