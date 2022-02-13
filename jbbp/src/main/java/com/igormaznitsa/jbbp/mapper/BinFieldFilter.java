/*
 * Copyright 2022 Igor Maznitsa.
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

import java.lang.reflect.Field;

/**
 * Filter allows cheeking Bin annotation and fields during mapping or writing operations.
 *
 * @see com.igormaznitsa.jbbp.io.JBBPOut
 * @see JBBPMapper#map
 * @see com.igormaznitsa.jbbp.io.JBBPOut#Bin
 * @since 2.0.4
 */
public interface BinFieldFilter {
  /**
   * Check annotation and field that they allowed.
   *
   * @param annotation bin annotation, must not be null
   * @param field      marked field, can be null
   * @return true if field allowed, false otherwise
   */
  boolean isAllowed(Bin annotation, Field field);
}
