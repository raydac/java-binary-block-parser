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
package com.igormaznitsa.jbbp.mvn.plugin;

import static org.junit.Assert.*;
import org.junit.Test;

public class UtilsTest {

  @Test
  public void testExtractClassName() {
    assertEquals("", Utils.extractClassName(""));
    assertEquals("", Utils.extractClassName("a.b.c."));
    assertEquals("hello", Utils.extractClassName("hello"));
    assertEquals("hello", Utils.extractClassName("a.b.c.hello"));
    assertEquals("hello", Utils.extractClassName(".hello"));
  }

  @Test
  public void testExtractPackageName() {
    assertEquals("", Utils.extractPackageName(""));
    assertEquals("", Utils.extractPackageName("hello"));
    assertEquals("a", Utils.extractPackageName("a.hello"));
    assertEquals("a.b.c", Utils.extractPackageName("a.b.c.hello"));
    assertEquals("a.b.c", Utils.extractPackageName("a.b.c."));
    assertEquals("", Utils.extractPackageName(".hello"));
  }

}
