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

import java.util.Arrays;
import java.util.Collection;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class JBBPClassInstantiatorTest {
  
  @Parameterized.Parameters
  public static Collection<JBBPClassInstantiator[]> getParameters(){
    return Arrays.asList(new JBBPClassInstantiator[]{new JBBPUnsafeInstantiator()}, new JBBPClassInstantiator[]{new JBBPSafeInstantiator()});
  }
  
  private final JBBPClassInstantiator instantiator;
  
  public JBBPClassInstantiatorTest(final JBBPClassInstantiator val){
    this.instantiator = val;
  }
  
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
  public void testStaticClass() throws Exception{
    assertNotNull(instantiator.makeClassInstance(Static.class));
    assertNotNull(instantiator.makeClassInstance(StaticInnerOne.class));
    assertNotNull(instantiator.makeClassInstance(PrivateStaticInnerTwo.class));
  }
  
  @Test
  public void testNonStaticClass() throws Exception{
    assertNotNull(instantiator.makeClassInstance(NonStatic.class));
    assertNotNull(instantiator.makeClassInstance(NonStaticInnerOne.class));
    assertNotNull(instantiator.makeClassInstance(PrivateNonStaticInnerTwo.class));
  }
  
  @Test
  public void testInnerClass() throws Exception{
    class InnerTwo {
      int a;
    }
    class InnerOne {
      InnerTwo two;
    }
    class Inner {
      InnerOne one;
    }
    
    assertNotNull(instantiator.makeClassInstance(Inner.class));
    assertNotNull(instantiator.makeClassInstance(InnerOne.class));
    assertNotNull(instantiator.makeClassInstance(InnerTwo.class));
  }
  
  @Test
  public void testInnerClassHierarchy() throws Exception{
    class InnerOne {
      int a;
    }
    class InnerTwo extends InnerOne {
      int b;
    }
    class InnerThree extends InnerTwo{
      int c;
    }
    
    assertNotNull(instantiator.makeClassInstance(InnerOne.class));
    assertNotNull(instantiator.makeClassInstance(InnerTwo.class));
    assertNotNull(instantiator.makeClassInstance(InnerThree.class));
  }

  @Test
  public void testCreateLocalClass_OnlyNonDefaultConstructor() throws Exception {
    class NoDefaultConstructor {
      int i;
      NoDefaultConstructor(byte a, short m, char b, boolean c, int d, long e, float f, double g, String h, byte[] array) {
        i = d;
      }
    }

    assertNotNull(instantiator.makeClassInstance(NoDefaultConstructor.class));
  }

  @Test
  public void testCreateLocalClass_TwoCounsturctorsPlusDefaultConstructor() throws Exception {
    class WithDefaultConstructor {
      int i;
      WithDefaultConstructor(byte a, short m, char b, boolean c, int d, long e, float f, double g, String h, byte[] array) {
        i = d;
      }

      WithDefaultConstructor(int d) {
        i = d;
      }
      
      WithDefaultConstructor(){
        i = 0;
      }
    }

    assertNotNull(instantiator.makeClassInstance(WithDefaultConstructor.class));
  }

  @Test
  public void testCreateLocalClass_TwoCounsturctors() throws Exception {
    class NoDefaultConstructor {
      int i;
      NoDefaultConstructor(byte a, short m, char b, boolean c, int d, long e, float f, double g, String h, byte[] array) {
        i = d;
      }

      NoDefaultConstructor(int d) {
        i = d;
      }
    }

    assertNotNull(instantiator.makeClassInstance(NoDefaultConstructor.class));
  }

  
  private static final class StaticTwoConstructorsPlusDefaultConstructor {
    int i;
    private StaticTwoConstructorsPlusDefaultConstructor(byte a, short m, char b, boolean c, int d, long e, float f, double g, String h, byte[] array) {
      i = d;
    }
    private StaticTwoConstructorsPlusDefaultConstructor(int d) {
      i = d;
    }
    private StaticTwoConstructorsPlusDefaultConstructor() {
      i = 0;
    }
  }
  
  private static final class StaticTwoConstructors {
    int i;
    private StaticTwoConstructors(byte a, short m, char b, boolean c, int d, long e, float f, double g, String h, byte[] array) {
      i = d;
    }
    private StaticTwoConstructors(int d) {
      i = d;
    }
  }
  
  @Test
  public void testCreateStaticClass_TwoConstructorsPlusDefaultConstructor() throws Exception {
    assertNotNull(instantiator.makeClassInstance(StaticTwoConstructorsPlusDefaultConstructor.class));
  }

  @Test
  public void testCreateStaticClass_TwoConstructors() throws Exception {
    assertNotNull(instantiator.makeClassInstance(StaticTwoConstructors.class));
  }
}
