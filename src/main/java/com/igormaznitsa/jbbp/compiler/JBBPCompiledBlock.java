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

import com.igormaznitsa.jbbp.compiler.varlen.JBBPLengthEvaluator;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.util.*;

public class JBBPCompiledBlock {
  
  private final JBBPNamedFieldInfo[] namedFieldData;
  private final String source;
  private final byte [] compiledArray;
  private final JBBPLengthEvaluator[] arraySizeEvaluators;
  
  public static class Builder {
    private String source;
    private final List<JBBPNamedFieldInfo> namedFields = new ArrayList<JBBPNamedFieldInfo>();
    private byte [] compiledData;
    private final List<JBBPLengthEvaluator> varLenProcessors = new ArrayList<JBBPLengthEvaluator>();
    
    public JBBPCompiledBlock build(){
      JBBPUtils.assertNotNull(source, "Source is not defined");
      JBBPUtils.assertNotNull(compiledData, "Compiled data is not defined");
    
      return new JBBPCompiledBlock(this.source, this.namedFields.toArray(new JBBPNamedFieldInfo[this.namedFields.size()]), this.varLenProcessors.isEmpty() ? null : this.varLenProcessors.toArray(new JBBPLengthEvaluator[this.varLenProcessors.size()]) , this.compiledData);
    }
    
    public Builder setSource(final String source){
      this.source = source;
      return this;
    }
    
    public Builder setCompiledData(final byte [] compiledData){
      this.compiledData = compiledData;
      return this;
    }
    
    public Builder setNamedFieldData(final List<JBBPNamedFieldInfo> namedFields){
      this.namedFields.clear();
      if (namedFields!=null){
        this.namedFields.addAll(namedFields);
      }
      return this;
    }
    
    public Builder setVarLengthProcessors(final List<JBBPLengthEvaluator> varLenProcessors){
      this.varLenProcessors.clear();
      if (varLenProcessors!=null){
        this.varLenProcessors.addAll(varLenProcessors);
      }
      return this;
    }
  } 
  
  public static Builder prepare(){
    return new Builder();
  }
  
  private JBBPCompiledBlock(final String source, final JBBPNamedFieldInfo[] namedFields, final JBBPLengthEvaluator[] arraySizeEvaluators, final byte [] compiledData){
    this.source = source;
    this.namedFieldData = namedFields;
    this.compiledArray = compiledData;
    this.arraySizeEvaluators = arraySizeEvaluators;
  }

  public String getSource(){
    return this.source;
  }
  
  public boolean hasVarArrays(){
    return this.arraySizeEvaluators!=null;
  }
  
  public byte [] getCompiledData(){
    return this.compiledArray;
  }

  public JBBPNamedFieldInfo[] getNamedFields(){
    return this.namedFieldData;
  }
  
  public JBBPLengthEvaluator[] getVarLengthProcessorList(){
    return this.arraySizeEvaluators;
  }
  
  public boolean exists(final String fieldPath) {
    return findFieldForPath(fieldPath)!=null;
  }
  
  public JBBPNamedFieldInfo findFieldForPath(final String fieldPath){
    for(final JBBPNamedFieldInfo f : this.namedFieldData){
      if (f.getFieldPath().equals(fieldPath)) return f;
    }
    return null;
  }
  
  public int findFieldOffsetForPath(final String fieldPath){
    for (final JBBPNamedFieldInfo f : this.namedFieldData) {
      if (f.getFieldPath().equals(fieldPath)) {
        return f.getFieldOffsetInCompiledBlock();
      }
    }
    throw new IllegalArgumentException("Unknown field path ["+fieldPath+']');
  }
}
