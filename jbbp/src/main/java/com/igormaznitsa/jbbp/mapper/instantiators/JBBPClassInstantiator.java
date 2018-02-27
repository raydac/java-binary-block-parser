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
 * Interface describes a memory allocator for Java classes.
 *
 * @since 1.0
 */
public interface JBBPClassInstantiator {
  /**
   * Allocate memory area for a class.
   *
   * @param <T>   the class type
   * @param klazz the class which should be instantiated, must not be null
   * @return an instance of the class
   * @throws InstantiationException it will be thrown if the class can;t be instantiated
   */
  <T> T makeClassInstance(Class<T> klazz) throws InstantiationException;
}
