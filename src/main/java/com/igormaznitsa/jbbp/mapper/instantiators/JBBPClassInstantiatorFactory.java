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

import com.igormaznitsa.jbbp.utils.JBBPUtils;

/**
 * The Factory produces a class instantiator which is compatible with the
 * current platform.
 */
public final class JBBPClassInstantiatorFactory {

  /**
   * The System property to be used to get custom class instantiator name.
   */
  public static final String SYSTEM_PROPERTY_INSTANTIATOR_CLASS = "jbbp.mapper.instantiator";

  /**
   * The Factory instance.
   */
  private static final JBBPClassInstantiatorFactory instance = new JBBPClassInstantiatorFactory();

  /**
   * The Hidden constructor.
   */
  private JBBPClassInstantiatorFactory() {

  }

  /**
   * Get the factory instance.
   *
   * @return the factory instance, must not be null
   */
  public static JBBPClassInstantiatorFactory getInstance() {
    return instance;
  }

  /**
   * Make an instantiator automatically for the current platform.
   *
   * @return the class instantiator instance which is compatible with the
   * current platform
   * @see JBBPClassInstantiator
   */
  public JBBPClassInstantiator make() {
    return this.make(JBBPClassInstantiatorType.AUTO);
  }

  /**
   * Make an instantiator for defined type.
   *
   * @param type the type of needed instantiator, must not be null
   * @return the class instantiator instance which is compatible with the
   * current platform
   */
  public JBBPClassInstantiator make(final JBBPClassInstantiatorType type) {
    JBBPUtils.assertNotNull(type, "Type must not be null");

    String className = "com.igormaznitsa.jbbp.mapper.instantiators.JBBPSafeInstantiator";

    switch (type) {
      case AUTO: {
        final String customClassName = System.getProperty(SYSTEM_PROPERTY_INSTANTIATOR_CLASS);
        if (customClassName == null) {
          try {
            final Class<?> unsafeclazz = Class.forName("sun.misc.Unsafe");
            unsafeclazz.getDeclaredField("theUnsafe");
            className = "com.igormaznitsa.jbbp.mapper.instantiators.JBBPUnsafeInstantiator";
          }
          catch (ClassNotFoundException ex) {
            // do nothing
          }
          catch (NoSuchFieldException ex) {
            // do nothing
          }
          catch (SecurityException ex) {
            // do nothing
          }
        }
        else {
          className = customClassName;
        }
      }
      break;
      case SAFE: {
        className = "com.igormaznitsa.jbbp.mapper.instantiators.JBBPSafeInstantiator";
      }
      break;
      case UNSAFE: {
        className = "com.igormaznitsa.jbbp.mapper.instantiators.JBBPUnsafeInstantiator";
      }
      break;
      default:
        throw new Error("Unexpected type, contact developer! [" + type + ']');
    }

    try {
      final Class<?> klazz = Class.forName(className);
      return JBBPClassInstantiator.class.cast(klazz.newInstance());
    }
    catch (ClassNotFoundException ex) {
      throw new Error("Can't make instantiator because can't find class '" + className + "', may be the class is obfuscated or wrong defined", ex);
    }
    catch (IllegalAccessException ex) {
      throw new Error("Can't make instantiator from '" + className + "'for access exception ", ex);
    }
    catch (InstantiationException ex) {
      throw new Error("Can't make instantiator from '" + className + "'for inside exception", ex);
    }

  }
}
