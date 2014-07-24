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

package com.igormaznitsa.jbbp.exceptions;

import com.igormaznitsa.jbbp.model.JBBPAbstractField;
import java.lang.reflect.Field;

/**
 * The Exception describes an error during processing of mapping of bin fields to class fields.
 */
public class JBBPMapperException extends JBBPException {
  private static final long serialVersionUID = -5643926527601318948L;
  
  /**
   * The JBBP field which mapping generates the exception.
   */
  private final JBBPAbstractField binField;
  /**
   * The Class field which processing generates the exception.
   */
  private final transient Field mappingClassField;
  
  /**
   * The class which mapping generates the exception.
   */
  private final Class<?> mappingClass;
  
  /**
   * The Constructor.
   * @param message the text message describes the exception
   * @param binField the JBBP field which processing generates the exception
   * @param mappingClass the class which mapping generates the exception
   * @param mappingClassField the class field which mapping is wrong
   * @param cause the root cause for the exception, it can be null
   */
  public JBBPMapperException(final String message, final JBBPAbstractField binField, final Class<?> mappingClass, final Field mappingClassField, final Throwable cause){
    super(message, cause);
    this.binField = binField;
    this.mappingClassField = mappingClassField;
    this.mappingClass = mappingClass;
  }
  
  /**
   * Get the JBBP field related to the exception.
   * @return a JBBP field
   */
  public JBBPAbstractField getBinField(){
    return this.binField;
  }
  
  /**
   * Get a class which mapping generates the exception.
   * @return the class which mapping generates the exception, it can be null
   */
  public Class<?> getMappingClass(){
    return this.mappingClass;
  }
  
  /**
   * Get a class field related to the exception.
   * @return a class field which was processing as exception thrown, it can be null
   */
  public Field getMappingClassField(){
    return this.mappingClassField;
  }
 
  @Override
  public String toString(){
    return this.getMessage()+" [ "+this.getMappingClassField()+" -> "+this.getBinField()+']';
  }
  
}
