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

/**
 * The Enum contains all system properties which are used by the JBBP framework.
 */
public enum JBBPSystemProperty {
  /**
   * The Property allows to redefine the default depth of the stack for expression evaluators.
   */
  PROPERTY_EXPRESSION_STACK_DEPTH("jbbp.expr.stack.depth"),
  /**
   * The Property allows to define which will be work as a class instantiator for the JBBP mapper.
   */
  PROPERTY_INSTANTIATOR_CLASS("jbbp.mapper.instantiator"),
  
  /**
   * The Property allows to define the initial size for array buffer to read whole stream.
   */
  PROPERTY_INPUT_INITIAL_ARRAY_BUFFER_SIZE("jbbp.input.initial.array.buffer");

  /**
   * The name of the property.
   */
  private final String propertyName;
  
  /**
   * The Constructor.
   * @param propertyName the property name
   */
  private JBBPSystemProperty(final String propertyName){
    this.propertyName = propertyName;
  }
  
  /**
   * Set a value to the property.
   * @param value the value to be set to the property, must not be null.
   */
  public void set(final String value){
    System.setProperty(this.propertyName, value);
  }
  
  /**
   * Remove the property.
   */
  public void remove(){
    System.clearProperty(this.propertyName);
  }
  
  /**
   * Get the property value as string.
   * @param defaultValue the default value which will be returned if there is not defined value for the property.
   * @return the value as string or the default value if it is not defined
   */
  public String getAsString(final String defaultValue){
    final String value = System.getProperty(this.propertyName);
    return value == null ? defaultValue : value;
  }
  
  /**
   * Get the property value as integer.
   *
   * @param defaultValue the default value which will be returned if there is
   * not defined value for the property.
   * @return the value as integer or the default value if it is not defined
   * @throws Error if the value can't be recognized as integer
   */
  public int getAsInteger(final int defaultValue){
    final String value = System.getProperty(this.propertyName);
    
    int result = defaultValue;
    
    if (value != null){
      try{
        result = Integer.parseInt(value);
      }catch(NumberFormatException ex){
        throw new Error("Can't get the system property '"+this.propertyName+"' as integer value, may be wrong format ["+value+']',ex);
      }
    }
    return result;
  }
  
  /**
   * Get the property name.
   * @return the property name
   */
  public String getPropertyName(){
    return this.propertyName;
  }
  
}
