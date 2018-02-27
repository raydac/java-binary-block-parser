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

import com.igormaznitsa.jbbp.utils.JBBPSystemProperty;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class JBBPClassInstantiatorFactoryTest {

    @AfterEach
    public void afterTest() {
        System.clearProperty(JBBPSystemProperty.PROPERTY_INSTANTIATOR_CLASS.getPropertyName());
    }

    @Test
    public void testMake_Default() {
        assertEquals(JBBPUnsafeInstantiator.class, JBBPClassInstantiatorFactory.getInstance().make().getClass());
    }

    @Test
    public void testMake_WithArgument_NPEForNuill() {
        assertThrows(NullPointerException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                JBBPClassInstantiatorFactory.getInstance().make(null);
            }
        });
    }

    @Test
    public void testMake_WithArgument() {
        assertEquals(JBBPSafeInstantiator.class, JBBPClassInstantiatorFactory.getInstance().make(JBBPClassInstantiatorType.SAFE).getClass());
        assertEquals(JBBPUnsafeInstantiator.class, JBBPClassInstantiatorFactory.getInstance().make(JBBPClassInstantiatorType.UNSAFE).getClass());
        assertEquals(JBBPUnsafeInstantiator.class, JBBPClassInstantiatorFactory.getInstance().make(JBBPClassInstantiatorType.AUTO).getClass());

    }

    @Test
    public void testMake_CustomClass() {
        System.setProperty(JBBPSystemProperty.PROPERTY_INSTANTIATOR_CLASS.getPropertyName(), FakeInstantiator.class.getName());
        assertEquals(FakeInstantiator.class, JBBPClassInstantiatorFactory.getInstance().make(JBBPClassInstantiatorType.AUTO).getClass());
    }

    public static class FakeInstantiator implements JBBPClassInstantiator {
        @Override
        public <T> T makeClassInstance(Class<T> klazz) throws InstantiationException {
            return null;
        }
    }

}
