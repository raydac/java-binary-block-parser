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
package com.igormaznitsa.jbbp.exceptions;

import com.igormaznitsa.jbbp.compiler.parser.JBBPToken;

public class JBBPCompilationException extends JBBPException {
  private static final long serialVersionUID = -7567503709641292590L;
  
  private final JBBPToken token;

  public JBBPCompilationException(final String text){
    this(text,null);
  }
  
  public JBBPCompilationException(final String text, final JBBPToken token){
    super(text);
    this.token = token;
  }
  
  public JBBPCompilationException(final String text, final JBBPToken token, final Throwable cause){
    super(text, cause);
    this.token = token;
  }
  
  public JBBPToken getToken(){
    return this.token;
  }
}
