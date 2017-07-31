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
package com.igormaznitsa.jbbp.compiler.conversion;

import org.junit.Test;
import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.TestUtils;
import com.igormaznitsa.jbbp.testaux.AbstractJavaClassCompilerTest;
import static org.junit.Assert.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;


/**
 * Test reading writing with converted classes from parser.
 */
public class ParserToJavaClassConverterReadWriteTest extends AbstractJavaClassCompilerTest {

  private static final String PACKAGE_NAME = "com.igormaznitsa.test";
  private static final String CLASS_NAME = "TestClass";

  private Object make(final String script) throws Exception {
    final ClassLoader cloader = saveAndCompile(new JavaClassContent(PACKAGE_NAME + '.' + CLASS_NAME, JBBPParser.prepare(script).makeClassSrc(PACKAGE_NAME, CLASS_NAME)));
    return cloader.loadClass(PACKAGE_NAME+'.'+CLASS_NAME).newInstance();
  }
  
  private Object callRead(final Object instance, final byte [] array) throws Exception {
    instance.getClass().getMethod("read", JBBPBitInputStream.class).invoke(instance, new JBBPBitInputStream(new ByteArrayInputStream(array)));
    return instance;
  }
  
  private byte [] callWrite(final Object instance) throws Exception {
    final ByteArrayOutputStream bout = new ByteArrayOutputStream();
    instance.getClass().getMethod("write", JBBPBitOutputStream.class).invoke(instance, new JBBPBitOutputStream(bout));
    bout.close();
    return bout.toByteArray();
  }
  
  @Test
  public void testReadWite_ByteArray() throws Exception {
    final Object instance = make("byte [_] byteArray;");
    assertNull("by default must be null",TestUtils.getField(instance, "bytearray", byte[].class));
  
    final byte [] etalon = new byte[]{1,2,3,4,5,6,7,8,9,0,22,33,44,55,66};
    
    callRead(instance, etalon.clone());
    
    assertArrayEquals(etalon, TestUtils.getField(instance, "bytearray", byte[].class));
    assertArrayEquals(etalon, callWrite(instance));
  }
}
