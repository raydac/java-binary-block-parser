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
import java.lang.reflect.*;

/**
 * The Class instantiate a class through sun.misc.unsafe without any call of
 * class constructors. To be more safe during porting to another platforms, all
 * work with sun,misc,unsafe organized through reflection to not have static
 * links the the class.
 * @since 1.0
 */
public final class JBBPUnsafeInstantiator implements JBBPClassInstantiator {

  /**
   * The sun,misc.Unsafe object.
   */
  private static final Object SUN_MISC_UNSAFE;
  /**
   * The sun,misc.Unsafe.allocateInstance method.
   */
  private static final Method ALLOCATE_INSTANCE_METHOD;

  static {
    try {
      final Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
      final Field singleoneInstanceField = unsafeClass.getDeclaredField("theUnsafe");
      JBBPUtils.makeAccessible(singleoneInstanceField);
      SUN_MISC_UNSAFE = singleoneInstanceField.get(null);
      ALLOCATE_INSTANCE_METHOD = unsafeClass.getMethod("allocateInstance", Class.class);
      JBBPUtils.makeAccessible(ALLOCATE_INSTANCE_METHOD);
    }
    catch (ClassNotFoundException e) {
      throw new Error("Can't find 'sun.misc.Unsafe' class", e);
    }
    catch (IllegalAccessException e) {
      throw new Error("Can't get sun.misc.Unsafe for illegal access", e);
    }
    catch (IllegalArgumentException e) {
      throw new Error("Can't get sun.misc.Unsafe for wrong argument", e);
    }
    catch (NoSuchFieldException e) {
      throw new Error("Can't get sun.misc.Unsafe because it doesn't exist", e);
    }
    catch (SecurityException e) {
      throw new Error("Can't get sun.misc.Unsafe for security exception", e);
    }
    catch (NoSuchMethodException e) {
      throw new Error("Can't get the 'allocateInstance' method in sun.misc.Unsafe", e);
    }
  }

  public <T> T makeClassInstance(final Class<T> klazz) throws InstantiationException {
    JBBPUtils.assertNotNull(klazz, "Class must not be null");
    try {
      return klazz.cast(ALLOCATE_INSTANCE_METHOD.invoke(SUN_MISC_UNSAFE, klazz));
    }
    catch (InvocationTargetException ex) {
      final Throwable cause = ex.getTargetException();
      if (cause instanceof InstantiationException) {
        throw (InstantiationException) cause;
      }
      else {
        throw new InstantiationException("Can't instantiate class for exception [" + ex + ']');
      }
    }
    catch (IllegalAccessException ex) {
      throw new InstantiationException("Can't instantiate class for exception [" + ex + ']');
    }
    catch (IllegalArgumentException ex) {
      throw new InstantiationException("Can't instantiate class for exception [" + ex + ']');
    }
  }

}
