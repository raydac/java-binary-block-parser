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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JBBPClassInstantiatorTest {

  @ParameterizedTest
  @ArgumentsSource(InstantiatorProvider.class)
  public void testStaticClass(final JBBPClassInstantiator instantiator) throws Exception {
    assertNotNull(instantiator.makeClassInstance(Static.class));
    assertNotNull(instantiator.makeClassInstance(StaticInnerOne.class));
    assertNotNull(instantiator.makeClassInstance(PrivateStaticInnerTwo.class));
  }

  @ParameterizedTest
  @ArgumentsSource(InstantiatorProvider.class)
  public void testNonStaticClass(final JBBPClassInstantiator instantiator) throws Exception {
    assertNotNull(instantiator.makeClassInstance(NonStatic.class));
    assertNotNull(instantiator.makeClassInstance(NonStaticInnerOne.class));
    assertNotNull(instantiator.makeClassInstance(PrivateNonStaticInnerTwo.class));
  }

  @ParameterizedTest
  @ArgumentsSource(InstantiatorProvider.class)
  public void testInnerClass(final JBBPClassInstantiator instantiator) throws Exception {
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

  @ParameterizedTest
  @ArgumentsSource(InstantiatorProvider.class)
  public void testInnerClassHierarchy(final JBBPClassInstantiator instantiator) throws Exception {
    class InnerOne {

      int a;
    }
    class InnerTwo extends InnerOne {

      int b;
    }
    class InnerThree extends InnerTwo {

      int c;
    }

    assertNotNull(instantiator.makeClassInstance(InnerOne.class));
    assertNotNull(instantiator.makeClassInstance(InnerTwo.class));
    assertNotNull(instantiator.makeClassInstance(InnerThree.class));
  }

  @ParameterizedTest
  @ArgumentsSource(InstantiatorProvider.class)
  public void testCreateLocalClass_OnlyNonDefaultConstructor(final JBBPClassInstantiator instantiator) throws Exception {
    class NoDefaultConstructor {

      int i;

      NoDefaultConstructor(byte a, short m, char b, boolean c, int d, long e, float f, double g, String h, byte[] array) {
        i = d;
      }
    }

    assertNotNull(instantiator.makeClassInstance(NoDefaultConstructor.class));
  }

  @ParameterizedTest
  @ArgumentsSource(InstantiatorProvider.class)
  public void testCreateLocalClass_TwoCounsturctorsPlusDefaultConstructor(final JBBPClassInstantiator instantiator) throws Exception {
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

    assertNotNull(instantiator.makeClassInstance(WithDefaultConstructor.class));
  }

  @ParameterizedTest
  @ArgumentsSource(InstantiatorProvider.class)
  public void testCreateLocalClass_TwoCounsturctors(final JBBPClassInstantiator instantiator) throws Exception {
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

  @ParameterizedTest
  @ArgumentsSource(InstantiatorProvider.class)
  public void testCreateStaticClass_TwoConstructorsPlusDefaultConstructor(final JBBPClassInstantiator instantiator) throws Exception {
    assertNotNull(instantiator.makeClassInstance(StaticTwoConstructorsPlusDefaultConstructor.class));
  }

  @ParameterizedTest
  @ArgumentsSource(InstantiatorProvider.class)
  public void testCreateStaticClass_TwoConstructors(final JBBPClassInstantiator instantiator) throws Exception {
    assertNotNull(instantiator.makeClassInstance(StaticTwoConstructors.class));
  }

  final static class InstantiatorProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext ec) throws Exception {
      return Arrays.asList(
              () -> new Object[] {new JBBPUnsafeInstantiator()},
              (Arguments) () -> new Object[] {new JBBPSafeInstantiator()}).stream();
    }

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
