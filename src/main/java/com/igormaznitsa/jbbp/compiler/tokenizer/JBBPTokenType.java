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
package com.igormaznitsa.jbbp.compiler.tokenizer;

/**
 * The Enumeration represents allowed token types to be met in binary block parser script.
 *
 * @since 1.0
 */
public enum JBBPTokenType {
    /**
     * A Commentaries.
     */
    COMMENT,
    /**
     * A Structure opening token.
     */
    STRUCT_START,
    /**
     * A Regular field or field array.
     */
    ATOM,
    /**
     * A Structure closing token.
     */
    STRUCT_END
}
