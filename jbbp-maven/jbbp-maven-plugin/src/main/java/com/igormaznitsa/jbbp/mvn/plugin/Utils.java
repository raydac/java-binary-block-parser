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
package com.igormaznitsa.jbbp.mvn.plugin;

import javax.annotation.Nonnull;

/**
 * Auxiliary methods.
 */
public final class Utils {
    private Utils() {
    }

    /**
     * Extract class name from canonical Java class name
     *
     * @param canonicalJavaClassName canonical class name (like 'a.b.c.SomeClassName'), must not be null
     * @return extracted class name, must not be null but can be empty for case "a.b.c.d."
     */
    @Nonnull
    public static String extractClassName(@Nonnull final String canonicalJavaClassName) {
        final int lastDot = canonicalJavaClassName.lastIndexOf('.');
        if (lastDot < 0) return canonicalJavaClassName.trim();
        return canonicalJavaClassName.substring(lastDot + 1).trim();
    }

    /**
     * Extract package name from canonical Java class name
     *
     * @param fileNameWithoutExtension canonical class name (like 'a.b.c.SomeClassName'), must not be null
     * @return extracted package name, must not be null but can be empty
     */
    @Nonnull
    public static String extractPackageName(@Nonnull final String fileNameWithoutExtension) {
        final int lastDot = fileNameWithoutExtension.lastIndexOf('.');
        if (lastDot < 0) return "";
        return fileNameWithoutExtension.substring(0, lastDot).trim();
    }
}
