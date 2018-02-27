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

import com.igormaznitsa.jbbp.exceptions.JBBPException;
import com.igormaznitsa.jbbp.mapper.Bin;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Auxiliary adapter for interface JBBPTextWriter.Extra.
 *
 * @see com.igormaznitsa.jbbp.utils.JBBPTextWriter.Extra
 * @since 1.1
 */
public abstract class JBBPTextWriterExtraAdapter implements JBBPTextWriter.Extra {

  @Override
  public void onNewLine(final JBBPTextWriter context, final int lineNumber) throws IOException {
  }

  @Override
  public void onBeforeFirstValue(final JBBPTextWriter context) throws IOException {
  }

  @Override
  public void onClose(final JBBPTextWriter context) throws IOException {
  }

  @Override
  public String doConvertByteToStr(final JBBPTextWriter context, final int value) throws IOException {
    return null;
  }

  @Override
  public String doConvertShortToStr(final JBBPTextWriter context, final int value) throws IOException {
    return null;
  }

  @Override
  public String doConvertFloatToStr(JBBPTextWriter context, float value) throws IOException {
    return null;
  }

  @Override
  public String doConvertDoubleToStr(JBBPTextWriter context, double value) throws IOException {
    return null;
  }

  @Override
  public String doConvertIntToStr(final JBBPTextWriter context, final int value) throws IOException {
    return null;
  }

  @Override
  public String doConvertLongToStr(final JBBPTextWriter context, final long value) throws IOException {
    return null;
  }

  @Override
  public String doConvertObjToStr(final JBBPTextWriter context, final int id, final Object obj) throws IOException {
    return null;
  }

  @Override
  public String doConvertCustomField(final JBBPTextWriter context, final Object obj, final Field field, final Bin annotation) throws IOException {
    return null;
  }

  @Override
  public void onReachedMaxValueNumberForLine(final JBBPTextWriter context) throws IOException {

  }

  /**
   * Auxiliary method to extract field value.
   *
   * @param instance object instance, can be null
   * @param field    the filed which value should be extracted, must not be null
   * @return the field value
   */
  public Object extractFieldValue(final Object instance, final Field field) {
    JBBPUtils.assertNotNull(field, "Field must not be null");
    try {
      return ReflectUtils.makeAccessible(field).get(instance);
    } catch (Exception ex) {
      throw new JBBPException("Can't extract value from field for exception", ex);
    }
  }
}
