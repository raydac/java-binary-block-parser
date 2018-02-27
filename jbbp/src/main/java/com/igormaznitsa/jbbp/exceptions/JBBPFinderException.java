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

package com.igormaznitsa.jbbp.exceptions;

import com.igormaznitsa.jbbp.model.JBBPAbstractField;

/**
 * The Exception can be thrown during search field operations,
 *
 * @since 1.0
 */
public class JBBPFinderException extends JBBPException {
  private static final long serialVersionUID = 4499929218503483986L;

  /**
   * The Name or the path of a field to be searched. It may contain null.
   */
  private final String nameOrPath;

  /**
   * The Field type of a field to be searched. It may contain null.
   */
  private final Class<? extends JBBPAbstractField> fieldType;

  /**
   * The Constructor.
   *
   * @param message    the exception message
   * @param nameOrPath the name of the path for used for search process, it can be null
   * @param fieldType  the field type used for the search process, it can be null
   */
  public JBBPFinderException(final String message, final String nameOrPath, final Class<? extends JBBPAbstractField> fieldType) {
    super(message);
    this.nameOrPath = nameOrPath;
    this.fieldType = fieldType;
  }

  /**
   * Do not use the method, it will be removed in future. It just presented for back compatibility.
   *
   * @return the name or the path used for search, it can be null
   * @see #getNameOrPath()
   * @deprecated
   */
  @Deprecated
  public String getNamrOrPath() {
    return this.getNameOrPath();
  }

  /**
   * Get the name or the path used for search.
   *
   * @return the name or the path used for search, it can be null
   */
  public String getNameOrPath() {
    return this.nameOrPath;
  }

  /**
   * Get the field type used for search.
   *
   * @return the field type used for search, it can be null
   */
  public Class<? extends JBBPAbstractField> getFieldType() {
    return this.fieldType;
  }
}
