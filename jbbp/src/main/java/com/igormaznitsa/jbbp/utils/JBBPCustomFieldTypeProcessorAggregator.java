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

package com.igormaznitsa.jbbp.utils;

import static com.igormaznitsa.jbbp.utils.JBBPUtils.ARRAY_STRING_EMPTY;


import com.igormaznitsa.jbbp.JBBPCustomFieldTypeProcessor;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPFieldTypeParameterContainer;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.model.JBBPAbstractField;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The Aggregator allows to join several custom field type processors.
 *
 * @since 1.2.0
 */
public class JBBPCustomFieldTypeProcessorAggregator implements JBBPCustomFieldTypeProcessor {
  private final Map<String, JBBPCustomFieldTypeProcessor> customTypeMap;
  private final String[] types;

  /**
   * Constructor.
   *
   * @param processors processors which should be joined.
   */
  public JBBPCustomFieldTypeProcessorAggregator(final JBBPCustomFieldTypeProcessor... processors) {
    this.customTypeMap = new HashMap<>();
    for (final JBBPCustomFieldTypeProcessor p : processors) {
      for (final String s : p.getCustomFieldTypes()) {
        JBBPUtils.assertNotNull(s, "Type must not be null");
        if (this.customTypeMap.containsKey(s)) {
          throw new IllegalArgumentException("Detected duplicated field type [" + s + ']');
        }
        this.customTypeMap.put(s, p);
      }
    }
    this.types = this.customTypeMap.keySet().toArray(ARRAY_STRING_EMPTY);
  }

  @Override
  public String[] getCustomFieldTypes() {
    return this.types;
  }

  @Override
  public boolean isAllowed(final JBBPFieldTypeParameterContainer fieldType, final String fieldName,
                           final int extraData, final boolean isArray) {
    return this.customTypeMap.get(fieldType.getTypeName())
        .isAllowed(fieldType, fieldName, extraData, isArray);
  }

  @Override
  public JBBPAbstractField readCustomFieldType(JBBPBitInputStream in, JBBPBitOrder bitOrder,
                                               int parserFlags,
                                               JBBPFieldTypeParameterContainer fieldType,
                                               JBBPNamedFieldInfo fieldName, int extraData,
                                               boolean readWholeStream, int arrayLength)
      throws IOException {
    return this.customTypeMap.get(fieldType.getTypeName())
        .readCustomFieldType(in, bitOrder, parserFlags, fieldType, fieldName, extraData,
            readWholeStream, arrayLength);
  }
}
