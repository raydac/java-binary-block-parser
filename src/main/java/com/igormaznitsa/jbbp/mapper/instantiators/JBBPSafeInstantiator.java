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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/**
 * Class creates instances of classes through call of their default
 * constructors, it works without any magic but through reflection thus inner
 * class instances will be with null instead of instance of enclosing class.
 *
 * @since 1.0
 */
public final class JBBPSafeInstantiator implements JBBPClassInstantiator {

    /**
     * Predefined empty object array.
     */
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    /**
     * Check that a class is an inner one.
     *
     * @param klazz a class to be checked, must not be null
     * @return true if the class is inner one, false otherwise
     */
    private static boolean isInnerClass(final Class<?> klazz) {
        return klazz.isMemberClass() && !Modifier.isStatic(klazz.getModifiers());
    }

    /**
     * Make stub arguments for a constructor.
     *
     * @param constructorParamTypes types of constructor arguments, must not be
     *                              null
     * @return generated Object array contains stub values for the constructor,
     * must not be null
     */
    private static Object[] makeStubForConstructor(final Class<?>[] constructorParamTypes) {
        if (constructorParamTypes.length == 0) {
            return EMPTY_OBJECT_ARRAY;
        }
        final Object[] result = new Object[constructorParamTypes.length];
        for (int i = 0; i < constructorParamTypes.length; i++) {
            final Class<?> arg = constructorParamTypes[i];
            final Object obj;
            if (arg.isArray()) {
                obj = null;
            } else {
                if (arg == byte.class) {
                    obj = (byte) 0;
                } else if (arg == char.class) {
                    obj = 'a';
                } else if (arg == short.class) {
                    obj = (short) 0;
                } else if (arg == boolean.class) {
                    obj = Boolean.FALSE;
                } else if (arg == int.class) {
                    obj = 0;
                } else if (arg == long.class) {
                    obj = 0L;
                } else if (arg == double.class) {
                    obj = 0.0d;
                } else if (arg == float.class) {
                    obj = 0.0f;
                } else {
                    obj = null;
                }
            }
            result[i] = obj;
        }
        return result;
    }

    /**
     * Find a constructor for an inner class.
     *
     * @param klazz          a class to find a constructor, must not be null
     * @param declaringClass the declaring class for the class, must not be null
     * @return found constructor to be used to make an instance
     */
    private static Constructor<?> findConstructorForInnerClass(final Class<?> klazz, final Class<?> declaringClass) {
        final Constructor<?>[] constructors = klazz.getDeclaredConstructors();
        if (constructors.length == 1) {
            return constructors[0];
        }
        for (final Constructor<?> c : constructors) {
            final Class<?>[] params = c.getParameterTypes();
            if (params.length == 1 && params[0] == declaringClass) {
                return c;
            }
        }
        return constructors[0];
    }

    /**
     * Find a constructor for a static class.
     *
     * @param klazz a class to find a constructor, must not be null
     * @return found constructor to be used to make an instance
     */
    private static Constructor<?> findConstructorForStaticClass(final Class<?> klazz) {
        final Constructor<?>[] constructors = klazz.getDeclaredConstructors();
        if (constructors.length == 1) {
            return constructors[0];
        }
        for (final Constructor<?> c : constructors) {
            final Class<?>[] params = c.getParameterTypes();
            if (params.length == 0) {
                return c;
            }
        }
        return constructors[0];
    }

    public <T> T makeClassInstance(final Class<T> klazz) throws InstantiationException {
        try {
            if (isInnerClass(klazz) || klazz.isLocalClass()) {
                final Class<?> declaringClass = klazz.getEnclosingClass();
                final Constructor<?> constructor = findConstructorForInnerClass(klazz, declaringClass);
                constructor.setAccessible(true);
                return klazz.cast(constructor.newInstance(makeStubForConstructor(constructor.getParameterTypes())));
            } else {
                final Constructor<?> constructor = findConstructorForStaticClass(klazz);
                constructor.setAccessible(true);
                return klazz.cast(constructor.newInstance(makeStubForConstructor(constructor.getParameterTypes())));
            }
        } catch (SecurityException ex) {
            throw new InstantiationException("Can't get access to the default constructor for class '" + klazz.getName() + "\' [" + ex + ']');
        } catch (IllegalArgumentException ex) {
            throw new InstantiationException("Can't make class '" + klazz.getName() + "\' [" + ex + ']');
        } catch (InvocationTargetException ex) {
            throw new InstantiationException("Can't make class '" + klazz.getName() + "\' [" + ex + ']');
        } catch (IllegalAccessException ex) {
            throw new InstantiationException("Can't make instance of class '" + klazz.getName() + "' for access exception [" + ex + ']');
        }
    }

}
