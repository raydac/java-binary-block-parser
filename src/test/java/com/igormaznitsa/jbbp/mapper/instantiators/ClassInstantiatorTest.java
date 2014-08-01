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

import org.junit.Test;
import static org.junit.Assert.*;

public class ClassInstantiatorTest {
  
  static class StaticInnerOne {
    PrivateStaticInnerTwo two;
  }
  
  private static class PrivateStaticInnerTwo {
    int hello;
  }
  
  public static class Static {
    StaticInnerOne inner;
  }

  class NonStaticInnerOne {
    PrivateStaticInnerTwo two;
  }
  
  private class PrivateNonStaticInnerTwo {
    int hello;
  }
  
  public class NonStatic {
    StaticInnerOne inner;
  }

  
  @Test
  public void testSafeInstantiator_StaticClass() throws Exception{
    final ClassInstantiator instantiator = new SafeInstantiator();
    
    assertNotNull(instantiator.makeClassInstance(Static.class));
    assertNotNull(instantiator.makeClassInstance(StaticInnerOne.class));
    assertNotNull(instantiator.makeClassInstance(PrivateStaticInnerTwo.class));
  }
  
  @Test
  public void testSafeInstantiator_NonStaticClass() throws Exception{
    final ClassInstantiator instantiator = new SafeInstantiator();

    assertNotNull(instantiator.makeClassInstance(NonStatic.class));
    assertNotNull(instantiator.makeClassInstance(NonStaticInnerOne.class));
    assertNotNull(instantiator.makeClassInstance(PrivateNonStaticInnerTwo.class));
  }
  
  @Test
  public void testSafeInstantiator_InnerClass() throws Exception{
    class InnerTwo {
      int a;
    }
    class InnerOne {
      InnerTwo two;
    }
    class Inner {
      InnerOne one;
    }
    
    final ClassInstantiator instantiator = new SafeInstantiator();

    assertNotNull(instantiator.makeClassInstance(Inner.class));
    assertNotNull(instantiator.makeClassInstance(InnerOne.class));
    assertNotNull(instantiator.makeClassInstance(InnerTwo.class));
  }
  
  @Test
  public void testSafeInstantiator_InnerClassHierarchy() throws Exception{
    class InnerOne {
      int a;
    }
    class InnerTwo extends InnerOne {
      int b;
    }
    class InnerThree extends InnerTwo{
      int c;
    }
    
    final ClassInstantiator instantiator = new SafeInstantiator();

    assertNotNull(instantiator.makeClassInstance(InnerOne.class));
    assertNotNull(instantiator.makeClassInstance(InnerTwo.class));
    assertNotNull(instantiator.makeClassInstance(InnerThree.class));
  }
  
  @Test
  public void testUnsafeInstantiator_StaticClass() throws Exception{
    final ClassInstantiator instantiator = new UnsafeInstantiator();

    assertNotNull(instantiator.makeClassInstance(Static.class));
    assertNotNull(instantiator.makeClassInstance(StaticInnerOne.class));
    assertNotNull(instantiator.makeClassInstance(PrivateStaticInnerTwo.class));
  }
  
  @Test
  public void testUnsafeInstantiator_NonStaticClass() throws Exception {
    final ClassInstantiator instantiator = new UnsafeInstantiator();

    assertNotNull(instantiator.makeClassInstance(NonStatic.class));
    assertNotNull(instantiator.makeClassInstance(NonStaticInnerOne.class));
    assertNotNull(instantiator.makeClassInstance(PrivateNonStaticInnerTwo.class));
  }

  @Test
  public void testUnsafeInstantiator_InnerClass() throws Exception {
    class InnerTwo {

      int a;
    }
    class InnerOne {

      InnerTwo two;
    }
    class Inner {

      InnerOne one;
    }

    final ClassInstantiator instantiator = new UnsafeInstantiator();

    assertNotNull(instantiator.makeClassInstance(Inner.class));
    assertNotNull(instantiator.makeClassInstance(InnerOne.class));
    assertNotNull(instantiator.makeClassInstance(InnerTwo.class));
  }

  @Test
  public void testUnsafeInstantiator_InnerClassHierarchy() throws Exception {
    class InnerOne {

      int a;
    }
    class InnerTwo extends InnerOne {

      int b;
    }
    class InnerThree extends InnerTwo {

      int c;
    }

    final ClassInstantiator instantiator = new UnsafeInstantiator();

    assertNotNull(instantiator.makeClassInstance(InnerOne.class));
    assertNotNull(instantiator.makeClassInstance(InnerTwo.class));
    assertNotNull(instantiator.makeClassInstance(InnerThree.class));
  }

}
