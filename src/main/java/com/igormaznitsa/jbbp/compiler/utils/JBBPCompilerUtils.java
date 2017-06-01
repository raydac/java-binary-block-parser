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
package com.igormaznitsa.jbbp.compiler.utils;

import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.exceptions.JBBPCompilationException;
import com.igormaznitsa.jbbp.utils.JBBPUtils;

import java.util.List;

import static com.igormaznitsa.jbbp.compiler.JBBPCompiler.FLAG_ARRAY;

/**
 * Class contains specific common auxiliary methods for parser and compiler classes.
 *
 * @since 1.0
 */
public enum JBBPCompilerUtils {
    ;

    /**
     * Find a named field info index in a list for its path.
     *
     * @param fieldPath   a field path, it must not be null.
     * @param namedFields a list contains named field info items.
     * @return the index of a field for the path if found one, -1 otherwise
     */
    public static int findIndexForFieldPath(final String fieldPath, final List<JBBPNamedFieldInfo> namedFields) {
        final String normalized = JBBPUtils.normalizeFieldNameOrPath(fieldPath);
        int result = -1;
        for (int i = namedFields.size() - 1; i >= 0; i--) {
            final JBBPNamedFieldInfo f = namedFields.get(i);
            if (normalized.equals(f.getFieldPath())) {
                result = i;
                break;
            }
        }
        return result;
    }

    /**
     * Find a named field info for its path inside a named field list.
     *
     * @param fieldPath   a field path, must not be null.
     * @param namedFields a named field list.
     * @return found item for the path, null otherwise
     */
    public static JBBPNamedFieldInfo findForFieldPath(final String fieldPath, final List<JBBPNamedFieldInfo> namedFields) {
        final String normalized = JBBPUtils.normalizeFieldNameOrPath(fieldPath);
        JBBPNamedFieldInfo result = null;
        for (int i = namedFields.size() - 1; i >= 0; i--) {
            final JBBPNamedFieldInfo f = namedFields.get(i);
            if (normalized.equals(f.getFieldPath())) {
                result = f;
                break;
            }
        }
        return result;
    }

    /**
     * Check a field in a compiled list defined by its named field info, that the field is not an array and it is not inside a structure array.
     *
     * @param fieldToCheck   a named field info to be checked, must not be null
     * @param namedFieldList a named field info list, must not be null.
     * @param compiledScript a compiled script body
     */
    public static void assertFieldIsNotArrayOrInArray(final JBBPNamedFieldInfo fieldToCheck, final List<JBBPNamedFieldInfo> namedFieldList, final byte[] compiledScript) {
        // check that the field is not array
        if ((compiledScript[fieldToCheck.getFieldOffsetInCompiledBlock()] & FLAG_ARRAY) != 0) {
            throw new JBBPCompilationException("An Array field can't be used as array size [" + fieldToCheck.getFieldPath() + ']');
        }
        if (fieldToCheck.getFieldPath().indexOf('.') >= 0) {
            // the field in structure, check that the structure is not an array or not in an array
            final String[] splittedFieldPath = JBBPUtils.splitString(fieldToCheck.getFieldPath(), '.');
            final StringBuilder fieldPath = new StringBuilder();
            // process till the field name because we have already checked the field
            for (int i = 0; i < splittedFieldPath.length - 1; i++) {
                if (fieldPath.length() != 0) {
                    fieldPath.append('.');
                }
                fieldPath.append(splittedFieldPath[i]);
                final JBBPNamedFieldInfo structureEnd = JBBPCompilerUtils.findForFieldPath(fieldPath.toString(), namedFieldList);
                if ((compiledScript[structureEnd.getFieldOffsetInCompiledBlock()] & FLAG_ARRAY) != 0) {
                    throw new JBBPCompilationException("Field from structure array can't be use as array size [" + fieldToCheck.getFieldPath() + ';' + structureEnd.getFieldPath() + ']');
                }
            }
        }
    }

}
