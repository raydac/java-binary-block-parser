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

package com.igormaznitsa.jbbp.mapper.instantiators;

/**
 * Type of class instantiator to provide new class instances for mapping.
 *
 * @since 1.0
 */
public enum JBBPClassInstantiatorType {
    /**
     * Auto recognition of the platform features and choice of appropriate one.
     */
    AUTO,
    /**
     * A Safe version which use standard Java approach but can be non-working in some cases.
     */
    SAFE,
    /**
     * A Version using sun.misc.Unsafe to allocate memory for objects without constructor calls.
     */
    UNSAFE
}
