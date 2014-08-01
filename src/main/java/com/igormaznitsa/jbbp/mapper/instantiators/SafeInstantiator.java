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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/**
 * Class creates instances of classes through call of their default constructors, it works without any magic but through reflection thus inner class instances will be with null instead of instance of enclosing class.
 */
public final class SafeInstantiator implements ClassInstantiator {

  /**
   * Predefined array for empty method arguments.
   */
  private static final Class<?>[] EMPTTY_CLASS_ARRAY = new Class<?>[0];

  private static boolean isInnerClass(final Class<?> klazz) {
    return klazz.isMemberClass() && !Modifier.isStatic(klazz.getModifiers());
  }
  
  public <T> T makeClassInstance(final Class<T> klazz) throws InstantiationException {
    try {
      if (isInnerClass(klazz) || klazz.isLocalClass()) {
        final Class<?> declaringClass = klazz.getEnclosingClass();
        // find default constructor for inner class
        final Constructor<T> constructor = klazz.getDeclaredConstructor(declaringClass);
        constructor.setAccessible(true);
        return constructor.newInstance((Object) null);
      }
      else {
        // not inner or not member, must have default constructor
        final Constructor<T> constructor = klazz.getDeclaredConstructor(EMPTTY_CLASS_ARRAY);
        constructor.setAccessible(true);
        return constructor.newInstance((Object[])EMPTTY_CLASS_ARRAY);
      }
    }
    catch (NoSuchMethodException ex) {
      throw new InstantiationException("Can't find the default constructor for class '" + klazz.getName() + "\' ["+ex+']');
    }
    catch (SecurityException ex) {
      throw new InstantiationException("Can't get access to the default constructor for class '" + klazz.getName() + "\' ["+ex+']');
    }
    catch (IllegalArgumentException ex) {
      throw new InstantiationException("Can't make class '" + klazz.getName() + "\' ["+ex+']');
    }
    catch (InvocationTargetException ex) {
      throw new InstantiationException("Can't make class '" + klazz.getName() + "\' ["+ex+']');
    }
    catch (IllegalAccessException ex) {
      throw new InstantiationException("Can't make instance of class '" + klazz.getName() + "' for access exception ["+ex+']');
    }
  }

}
