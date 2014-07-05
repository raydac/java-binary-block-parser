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
package com.igormaznitsa.jbbp.model.finder;

import com.igormaznitsa.jbbp.model.JBBPAbstractField;

public interface JBBPFieldFinder {
  <T extends JBBPAbstractField> T findFirstFieldForType(Class<T> fieldType);
  <T extends JBBPAbstractField> T findLastFieldForType(Class<T> fieldType);
  <T extends JBBPAbstractField> T findFieldForType(Class<T> fieldType);
  <T extends JBBPAbstractField> T findFieldForNameAndType(String fieldName, Class<T> fieldType);
  <T extends JBBPAbstractField> T findFieldForPathAndType(String fieldPath, Class<T> fieldType);
  JBBPAbstractField findFieldForName(String fieldName);
  JBBPAbstractField findFieldForPath(String fieldPath);
  boolean nameExists(String fieldName);
  boolean pathExists(String fieldPath);
}
