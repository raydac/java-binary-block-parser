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

import com.igormaznitsa.jbbp.mapper.JBBPMapper;
import org.junit.jupiter.api.Assertions;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

public class JBBPMapperDefaultInstantiatorTest {

  @Test
  public void testStaticClass() throws Exception {
    assertNotNull(JBBPMapper.DEFAULT_INSTANTIATOR.apply(Static.class));
      Assertions.assertThrows(RuntimeException.class, ()->JBBPMapper.DEFAULT_INSTANTIATOR.apply(StaticInnerOne.class));
      Assertions.assertThrows(RuntimeException.class, () -> JBBPMapper.DEFAULT_INSTANTIATOR.apply(PrivateStaticInnerTwo.class));
  }

  @Test
  public void testNonStaticClass() throws Exception {
    assertNotNull(JBBPMapper.DEFAULT_INSTANTIATOR.apply(NonStatic.class));
      Assertions.assertThrows(RuntimeException.class, ()->JBBPMapper.DEFAULT_INSTANTIATOR.apply(NonStaticInnerOne.class));
      Assertions.assertThrows(RuntimeException.class, ()->JBBPMapper.DEFAULT_INSTANTIATOR.apply(PrivateNonStaticInnerTwo.class));
  }

  @Test
  public void testInnerClass() throws Exception {
    class InnerTwo {

      int a;
    }
    class InnerOne {

      InnerTwo two;
    }
    class Inner {

      InnerOne one;
    }

    assertNotNull(JBBPMapper.DEFAULT_INSTANTIATOR.apply(Inner.class));
    assertNotNull(JBBPMapper.DEFAULT_INSTANTIATOR.apply(InnerOne.class));
    assertNotNull(JBBPMapper.DEFAULT_INSTANTIATOR.apply(InnerTwo.class));
  }

  @Test
  public void testInnerClassHierarchy() throws Exception {
    class InnerOne {

      int a;
    }
    class InnerTwo extends InnerOne {

      int b;
    }
    class InnerThree extends InnerTwo {

      int c;
    }

    assertNotNull(JBBPMapper.DEFAULT_INSTANTIATOR.apply(InnerOne.class));
    assertNotNull(JBBPMapper.DEFAULT_INSTANTIATOR.apply(InnerTwo.class));
    assertNotNull(JBBPMapper.DEFAULT_INSTANTIATOR.apply(InnerThree.class));
  }

  @Test
  public void testCreateLocalClass_OnlyNonDefaultConstructor() throws Exception {
    class NoDefaultConstructor {

      int i;

      NoDefaultConstructor(byte a, short m, char b, boolean c, int d, long e, float f, double g, String h, byte[] array) {
        i = d;
      }
    }

    assertNotNull(JBBPMapper.DEFAULT_INSTANTIATOR.apply(NoDefaultConstructor.class));
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

      WithDefaultConstructor() {
        i = 0;
      }
    }

    assertNotNull(JBBPMapper.DEFAULT_INSTANTIATOR.apply(WithDefaultConstructor.class));
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

    assertNotNull(JBBPMapper.DEFAULT_INSTANTIATOR.apply(NoDefaultConstructor.class));
  }

  @Test
  public void testCreateStaticClass_TwoConstructorsPlusDefaultConstructor() throws Exception {
    assertNotNull(JBBPMapper.DEFAULT_INSTANTIATOR.apply(StaticTwoConstructorsPlusDefaultConstructor.class));
  }

  @Test
  public void testCreateStaticClass_TwoConstructors() throws Exception {
    assertNotNull(JBBPMapper.DEFAULT_INSTANTIATOR.apply(StaticTwoConstructors.class));
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

  class NonStaticInnerOne {

    PrivateStaticInnerTwo two;
  }

  private class PrivateNonStaticInnerTwo {

    int hello;
  }

  public class NonStatic {

    StaticInnerOne inner;
  }
}
