/* 
 * Copyright 2014 Igor Maznitsa (http://www.igormaznitsa.com).
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
package com.igormaznitsa.jbbp.compiler;

public final class JBBPNamedFieldInfo {
  private final String fieldPath;
  private final String fieldName;
  private final int offsetInCompiledBlock;
  
  public JBBPNamedFieldInfo(final String fieldPath, final String fieldName, final int offsetInCompiledBlock) {
    this.fieldPath = fieldPath;
    this.fieldName = fieldName;
    this.offsetInCompiledBlock = offsetInCompiledBlock;
  }

  public String getFieldPath() {
    return this.fieldPath;
  }

  public String getFieldName(){
    return this.fieldName;
  }
  
  public int getFieldOffsetInCompiledBlock() {
    return this.offsetInCompiledBlock;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == null) return false;
    if (this == obj) return true;
    if (obj instanceof JBBPNamedFieldInfo){
      final JBBPNamedFieldInfo that = (JBBPNamedFieldInfo)obj;
      return this.fieldPath.equals(that.fieldPath) && this.offsetInCompiledBlock == that.offsetInCompiledBlock;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.offsetInCompiledBlock;
  }
  
  @Override
  public String toString(){
    return this.getClass().getSimpleName()+"[fieldPath="+this.fieldPath+", fieldName="+this.fieldName+", offetInCompiledBlock="+this.offsetInCompiledBlock+']';
  }

}
