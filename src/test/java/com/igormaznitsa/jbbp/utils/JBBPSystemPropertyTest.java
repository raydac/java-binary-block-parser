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

package com.igormaznitsa.jbbp.utils;

import org.junit.*;
import static org.junit.Assert.*;

public class JBBPSystemPropertyTest {
  
  @Before
  public void before(){
    for(final JBBPSystemProperty p : JBBPSystemProperty.values()){
      System.clearProperty(p.getPropertyName());
    }
  }
  
  @Test
  public void testSet() {
    assertNull(System.getProperty(JBBPSystemProperty.PROPERTY_INSTANTIATOR_CLASS.getPropertyName()));
    JBBPSystemProperty.PROPERTY_INSTANTIATOR_CLASS.set("hello_world");
    assertEquals("hello_world",System.getProperty(JBBPSystemProperty.PROPERTY_INSTANTIATOR_CLASS.getPropertyName()));
  }
  
  @Test
  public void testRemove(){
    System.setProperty(JBBPSystemProperty.PROPERTY_INSTANTIATOR_CLASS.getPropertyName(),"1234");
    JBBPSystemProperty.PROPERTY_INSTANTIATOR_CLASS.remove();
    assertNull(System.getProperty(JBBPSystemProperty.PROPERTY_INSTANTIATOR_CLASS.getPropertyName()));
  }
  
  @Test
  public void testGetAsString_Default() {
    assertEquals("12345",JBBPSystemProperty.PROPERTY_INSTANTIATOR_CLASS.getAsString("12345"));
  }
  
  @Test
  public void testGetAsString_DefinedValue() {
    System.setProperty(JBBPSystemProperty.PROPERTY_INSTANTIATOR_CLASS.getPropertyName(), "5678");
    assertEquals("5678",JBBPSystemProperty.PROPERTY_INSTANTIATOR_CLASS.getAsString("12345"));
  }
  
  @Test
  public void testGetAsInteger_Default() {
    assertEquals(12345,JBBPSystemProperty.PROPERTY_INSTANTIATOR_CLASS.getAsInteger(12345));
  }
  
  @Test
  public void testGetAsInteger_DefinedValue() {
    System.setProperty(JBBPSystemProperty.PROPERTY_INSTANTIATOR_CLASS.getPropertyName(), "5678");
    assertEquals(5678,JBBPSystemProperty.PROPERTY_INSTANTIATOR_CLASS.getAsInteger(12345));
  }
  
  @Test(expected = Error.class)
  public void testGetAsInteger_ErrorForNonIntegerValue() {
    System.setProperty(JBBPSystemProperty.PROPERTY_INSTANTIATOR_CLASS.getPropertyName(), "abcd");
    JBBPSystemProperty.PROPERTY_INSTANTIATOR_CLASS.getAsInteger(12345);
  }
  
}
