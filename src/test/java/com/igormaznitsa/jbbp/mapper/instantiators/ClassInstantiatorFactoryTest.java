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

package com.igormaznitsa.jbbp.mapper.instantiators;

import org.junit.*;
import static org.junit.Assert.*;

public class ClassInstantiatorFactoryTest {
  
  @After
  public void afterTest(){
    System.clearProperty(ClassInstantiatorFactory.SYSTEM_PROPERTY_INSTANTIATOR_CLASS);
  }
  
  public static class FakeInstantiator implements ClassInstantiator {
    public <T> T makeClassInstance(Class<T> klazz) throws InstantiationException {
      return null;
    }
  }
  
  @Test
  public void testMake_Default() {
    assertEquals(UnsafeInstantiator.class, ClassInstantiatorFactory.getInstance().make().getClass());
  }

  @Test(expected = NullPointerException.class)
  public void testMake_WithArgument_NPEForNuill() {
    ClassInstantiatorFactory.getInstance().make(null);
  }

  
  @Test
  public void testMake_WithArgument() {
    assertEquals(SafeInstantiator.class, ClassInstantiatorFactory.getInstance().make(ClassInstantiatorType.SAFE).getClass());
    assertEquals(UnsafeInstantiator.class, ClassInstantiatorFactory.getInstance().make(ClassInstantiatorType.UNSAFE).getClass());
    assertEquals(UnsafeInstantiator.class, ClassInstantiatorFactory.getInstance().make(ClassInstantiatorType.AUTO).getClass());
    
  }
  
  @Test
  public void testMake_CustomClass(){
    System.setProperty(ClassInstantiatorFactory.SYSTEM_PROPERTY_INSTANTIATOR_CLASS, FakeInstantiator.class.getName());
    assertEquals(FakeInstantiator.class, ClassInstantiatorFactory.getInstance().make(ClassInstantiatorType.AUTO).getClass());
  }
  
}
