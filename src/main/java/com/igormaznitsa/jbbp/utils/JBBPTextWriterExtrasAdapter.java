/*
 * Copyright 2014 Igor Maznitsa.
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

import java.io.IOException;

/**
 * Adapter for interface JBBPTextWriter.Extras.
 * @see com.igormaznitsa.jbbp.utils.JBBPTextWriter.Extras
 * @since 1.1
 */
public abstract class JBBPTextWriterExtrasAdapter implements JBBPTextWriter.Extras {

  public void onNewLine(final JBBPTextWriter context, final int lineNumber) throws IOException {
  }

  public void onBeforeFirstValue(final JBBPTextWriter context) throws IOException {
  }

  public void onClose(final JBBPTextWriter context) throws IOException {
  }

  public String doConvertByteToStr(final JBBPTextWriter context, final int value) throws IOException {
    return null;
  }

  public String doConvertShortToStr(final JBBPTextWriter context, final int value) throws IOException {
    return null;
  }

  public String doConvertIntToStr(final JBBPTextWriter context, final int value) throws IOException {
    return null;
  }

  public String doConvertLongToStr(final JBBPTextWriter context, final long value) throws IOException {
    return null;
  }

  public String doConvertObjToStr(final JBBPTextWriter context, final int id, final Object obj) throws IOException {
    return null;
  }

  public void onReachedMaxValueNumberForLine(final JBBPTextWriter context) throws IOException {
    
  }
}
