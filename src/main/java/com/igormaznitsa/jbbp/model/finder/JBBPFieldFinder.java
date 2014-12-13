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

/**
 * The Interface describes a class which can provide search for its inside field storage.
 * @since 1.0
 */
public interface JBBPFieldFinder {
  /**
   * Find the first met field for defined class. Field should be exactly instance of the class, not a successor.
   * @param <T> the class of the field
   * @param fieldType the field class for search
   * @return found field or null
   */
  <T extends JBBPAbstractField> T findFirstFieldForType(Class<T> fieldType);
  /**
   * Find the last met field for defined class. Field should be exactly
   * instance of the class, not a successor.
   *
   * @param <T> the class of the field
   * @param fieldType the field class for search, must not be null
   * @return found field or null
   */
  <T extends JBBPAbstractField> T findLastFieldForType(Class<T> fieldType);

  /**
   * Find unique field for defined class. Field should be exactly
   * instance of the class and the field must be only one or not be presented else an exception will be thrown.
   *
   * @param <T> the class of the field
   * @param fieldType the field class for search, must not be null
   * @return found field or null
   */
  <T extends JBBPAbstractField> T findFieldForType(Class<T> fieldType);
  
  /**
   * Find a field for its name and type pair.
   *
   * @param <T> the class of the field
   * @param fieldName the field name for search, must not be null
   * @param fieldType the field class for search, must not be null
   * @return found field or null
   */
  <T extends JBBPAbstractField> T findFieldForNameAndType(String fieldName, Class<T> fieldType);
  
  /**
   * Find a field for its path and type pair.
   *
   * @param <T> the class of the field
   * @param fieldPath the field path for search, must not be null
   * @param fieldType the field class for search, must not be null
   * @return found field or null
   */
  <T extends JBBPAbstractField> T findFieldForPathAndType(String fieldPath, Class<T> fieldType);
  
  /**
   * Find a field for its name.
   * @param fieldName the field name for search, it must not be null
   * @return found field or null
   */
  JBBPAbstractField findFieldForName(String fieldName);

  /**
   * Find a field for its path.
   *
   * @param fieldPath the field path for search, it must not be null
   * @return found field or null
   */
  JBBPAbstractField findFieldForPath(String fieldPath);
  
  /**
   * Check that a field exists for a name
   * @param fieldName a field name to check, it must not be null 
   * @return true if the field exists, false otherwise
   */
  boolean nameExists(String fieldName);
  
  /**
   * Check that a file exists for path
   * @param fieldPath a field path to check, it must not be null
   * @return true if the field exists, false otherwise
   */
  boolean pathExists(String fieldPath);
}
