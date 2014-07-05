/* 
 * Copyright 2014 Igor Maznitsa (http://www.igormaznitsa.com).
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

import static com.igormaznitsa.jbbp.compiler.JBBPCompiler.FLAG_ARRAY;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.exceptions.JBBPCompilationException;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.util.List;
import java.util.Locale;

public final class JBBPCompilerUtils {
  private JBBPCompilerUtils(){
    
  }
  
  public static int findIndexForName(final String name, final List<JBBPNamedFieldInfo> namedFields) {
    final String normalized = normalizeFieldName(name);
    for (int i = namedFields.size() - 1; i >= 0; i--) {
      final JBBPNamedFieldInfo f = namedFields.get(i);
      if (normalized.equals(f.getFieldPath())) {
        return i;
      }
    }
    return -1;
  }
  
  public static JBBPNamedFieldInfo findForName(final String name, final List<JBBPNamedFieldInfo> namedFields) {
    final String normalized = normalizeFieldName(name);
    for (int i = namedFields.size() - 1; i >= 0; i--) {
      final JBBPNamedFieldInfo f = namedFields.get(i);
      if (normalized.equals(f.getFieldPath())) {
        return f;
      }
    }
    return null;
  }

  public static String normalizeFieldName(final String name) {
    return name.trim().toLowerCase(Locale.ENGLISH);
  }

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
        final JBBPNamedFieldInfo structureEnd = JBBPCompilerUtils.findForName(fieldPath.toString(), namedFieldList);
        if ((compiledScript[structureEnd.getFieldOffsetInCompiledBlock()] & FLAG_ARRAY) != 0) {
          throw new JBBPCompilationException("Field from structure array can't be use as array size [" + fieldToCheck.getFieldPath() + ';' + structureEnd.getFieldPath() + ']');
        }
      }
    }
  }
  
}
