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

package com.igormaznitsa.jbbp.mapper;

import com.igormaznitsa.jbbp.model.JBBPFieldStruct;

import java.lang.reflect.Field;

/**
 * The Interface describes a processor which will be called during mapping of fields of a parsed binary structure to a mapping class instance, if they marked for custom processing.
 *
 * @since 1.0
 */
public interface JBBPMapperCustomFieldProcessor {
  /**
   * Prepare an object to be set to a custom mapping field.
   *
   * @param parsedBlock the structure root of parsed binary packet, must not be null
   * @param annotation  the annotation for mapping field, must not be null
   * @param field       the mapping field in a mapping class, must not be null
   * @return an object which will be set to the field in a mapping class instance, it can be null for non-primitive fields
   */
  Object prepareObjectForMapping(final JBBPFieldStruct parsedBlock, final Bin annotation, final Field field);
}
