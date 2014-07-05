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
package com.igormaznitsa.jbbp.compiler.varlen;

import com.igormaznitsa.jbbp.JBBPExternalValueProvider;
import com.igormaznitsa.jbbp.JBBPNamedNumericFieldMap;
import com.igormaznitsa.jbbp.compiler.JBBPCompiledBlock;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.model.JBBPFieldInt;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class JBBPOnlyFieldEvaluatorTest {
  
  @Test
  public void testNumericValue() {
    final int value = 1234;
    final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap();
    final JBBPNamedFieldInfo nameInfo = new JBBPNamedFieldInfo("value", "value", value);
    
    map.putField(new JBBPFieldInt(nameInfo,value));

    final List<JBBPNamedFieldInfo> list = new ArrayList<JBBPNamedFieldInfo>();
    list.add(nameInfo);
    
    final byte[] compiled = new byte[]{0};
    final JBBPCompiledBlock compiledBlock = JBBPCompiledBlock.prepare().setCompiledData(compiled).setSource("none").setNamedFieldData(list).build();

    JBBPOnlyFieldEvaluator expr = new JBBPOnlyFieldEvaluator(null, 0);
    assertEquals(value, expr.eval(null,0, compiledBlock, map));
  }
  
  @Test
  public void testExternalValue() {
    final int value = 1234;
    final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap(new JBBPExternalValueProvider() {

      public int provideArraySize(final String fieldName, final JBBPNamedNumericFieldMap numericFieldMap, final JBBPCompiledBlock compiledBlock) {
        if (fieldName.equals("value")) {
          return value;
        }
        assertNotNull(numericFieldMap);
        assertNotNull(compiledBlock);
        fail("Unexpected request for value [" + fieldName + ']');
        return -1;
      }
    });

    final List<JBBPNamedFieldInfo> list = new ArrayList<JBBPNamedFieldInfo>();

    final byte[] compiled = new byte[]{0};
    final JBBPCompiledBlock compiledBlock = JBBPCompiledBlock.prepare().setCompiledData(compiled).setSource("none").setNamedFieldData(list).build();

    JBBPOnlyFieldEvaluator expr = new JBBPOnlyFieldEvaluator("value", -1);
    assertEquals(value, expr.eval(null,0, compiledBlock, map));
  }
  
  @Test
  public void testExternalValueNamedAsFirstCharDollar() {
    final int value = 1234;
    final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap(new JBBPExternalValueProvider() {

      public int provideArraySize(final String fieldName, final JBBPNamedNumericFieldMap numericFieldMap, final JBBPCompiledBlock compiledBlock) {
        if (fieldName.equals("$value")) {
          return value;
        }
        assertNotNull(numericFieldMap);
        assertNotNull(compiledBlock);
        fail("Unexpected request for value [" + fieldName + ']');
        return -1;
      }
    });

    final List<JBBPNamedFieldInfo> list = new ArrayList<JBBPNamedFieldInfo>();

    final byte[] compiled = new byte[]{0};
    final JBBPCompiledBlock compiledBlock = JBBPCompiledBlock.prepare().setCompiledData(compiled).setSource("none").setNamedFieldData(list).build();

    JBBPOnlyFieldEvaluator expr = new JBBPOnlyFieldEvaluator("$value", -1);
    assertEquals(value, expr.eval(null,0, compiledBlock, map));
  }
  
  @Test
  public void testCounterOfStreamAsParameter() throws Exception {
    final List<JBBPNamedFieldInfo> list = new ArrayList<JBBPNamedFieldInfo>();

    final byte[] compiled = new byte[]{0};
    final JBBPCompiledBlock compiledBlock = JBBPCompiledBlock.prepare().setCompiledData(compiled).setSource("none").setNamedFieldData(list).build();

    JBBPOnlyFieldEvaluator expr = new JBBPOnlyFieldEvaluator("$", -1);
    
    final JBBPBitInputStream inStream = new JBBPBitInputStream(new ByteArrayInputStream(new byte[]{1,2,3,4,5}));
    inStream.read();
    inStream.read();
    inStream.read();
    
    assertEquals(3, expr.eval(inStream,0, compiledBlock, null));
  }
  
}
