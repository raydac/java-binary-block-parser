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

package com.igormaznitsa.jbbp.plugin.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CommonUtilsTest {

  @Test
  public void testExtractClassName() {
    assertEquals("", CommonUtils.extractClassName(""));
    assertEquals("", CommonUtils.extractClassName("a.b.c."));
    assertEquals("hello", CommonUtils.extractClassName("hello"));
    assertEquals("hello", CommonUtils.extractClassName("a.b.c.hello"));
    assertEquals("hello", CommonUtils.extractClassName(".hello"));
  }

  @Test
  public void testExtractPackageName() {
    assertEquals("", CommonUtils.extractPackageName(""));
    assertEquals("", CommonUtils.extractPackageName("hello"));
    assertEquals("a", CommonUtils.extractPackageName("a.hello"));
    assertEquals("a.b.c", CommonUtils.extractPackageName("a.b.c.hello"));
    assertEquals("a.b.c", CommonUtils.extractPackageName("a.b.c."));
    assertEquals("", CommonUtils.extractPackageName(".hello"));
  }

}
