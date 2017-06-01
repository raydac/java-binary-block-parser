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
package com.igormaznitsa.jbbp;

import com.igormaznitsa.jbbp.compiler.JBBPCompiledBlock;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.exceptions.JBBPEvalException;
import com.igormaznitsa.jbbp.exceptions.JBBPTooManyFieldsFoundException;
import com.igormaznitsa.jbbp.model.JBBPFieldByte;
import com.igormaznitsa.jbbp.model.JBBPFieldInt;
import com.igormaznitsa.jbbp.model.JBBPFieldLong;
import com.igormaznitsa.jbbp.model.JBBPNumericField;
import org.junit.Test;
import static org.junit.Assert.*;

public class JBBPNamedNumericFieldMapTest {
  
  @Test
  public void testConstructor_Default() {
    final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap();
    assertEquals(0,map.size());
    assertNull(map.getExternalValueProvider());
  }
  
  @Test
  public void testConstructor_ProviderIsNull() {
    final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap(null);
    assertEquals(0,map.size());
    assertNull(map.getExternalValueProvider());
  }
  
  @Test
  public void testConstructor_ProviderIsNotNull() {
    final JBBPExternalValueProvider provider = new JBBPExternalValueProvider() {

      public int provideArraySize(String fieldName, JBBPNamedNumericFieldMap numericFieldMap, JBBPCompiledBlock compiledBlock) {
        return 0;
      }
    };
    final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap(provider);
    assertEquals(0,map.size());
    assertSame(provider, map.getExternalValueProvider());
  }

  @Test
  public void testGetExternalValueProvider() {
    final JBBPExternalValueProvider provider = new JBBPExternalValueProvider() {

      public int provideArraySize(String fieldName, JBBPNamedNumericFieldMap numericFieldMap, JBBPCompiledBlock compiledBlock) {
        return 0;
      }
    };
    final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap(provider);
    assertSame(provider, map.getExternalValueProvider());

  
    final JBBPNamedNumericFieldMap map2 = new JBBPNamedNumericFieldMap(null);
    try{
      map2.getExternalFieldValue("test", JBBPCompiledBlock.prepare().setSource("").setCompiledData(new byte[]{0}).build(), null);
      fail("Must throw JBBPEvalException");
    }catch(JBBPEvalException ex){
    }
  }

  @Test
  public void testSize(){
    final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap();
    assertEquals(0,map.size());
    map.putField(new JBBPFieldInt(new JBBPNamedFieldInfo("test.test", "test", 0), 123));
    assertEquals(1,map.size());
  }
  
  @Test
  public void testIsEmpty(){
    final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap();
    assertTrue(map.isEmpty());
    map.putField(new JBBPFieldInt(new JBBPNamedFieldInfo("test.test", "test", 0), 123));
    assertFalse(map.isEmpty());
  }
  
  @Test
  public void testClear(){
    final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap();
    map.clear();
    assertTrue(map.isEmpty());
    map.putField(new JBBPFieldInt(new JBBPNamedFieldInfo("test.test", "test", 0), 123));
    assertFalse(map.isEmpty());
    map.clear();
    assertTrue(map.isEmpty());
  }
  
  @Test
  public void testFindFieldForName(){
    final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap();
    final JBBPNumericField field = new JBBPFieldInt(new JBBPNamedFieldInfo("test.test", "test", 0), 123);
    map.putField(field);
    assertSame(field, map.findFieldForName("tESt"));
    assertNull(map.findFieldForName("test1"));

    try {
      map.findFieldForName(null);
      fail("Must throw NPE");
    }
    catch (NullPointerException ex) {
    }
  }
  
  @Test
  public void testRemove(){
    final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap();
    final JBBPNumericField field1 = new JBBPFieldInt(new JBBPNamedFieldInfo("test.test", "test", 0), 123);
    final JBBPNumericField field2 = new JBBPFieldInt(new JBBPNamedFieldInfo("test.test2", "test2", 0), 123);
    final JBBPNumericField field3 = new JBBPFieldByte(new JBBPNamedFieldInfo("test.test3", "test3", 0), (byte)123);
    
    map.putField(field1);
    map.putField(field2);
    map.putField(field3);
    
    assertEquals(3, map.size());
    
    map.remove(field2.getNameInfo());
    
    assertEquals(2, map.size());
    assertSame(field1, map.findFieldForName("test"));
    assertSame(field3, map.findFieldForName("test3"));
    assertNull(map.findFieldForName("test2"));
    
    try{
      map.remove(null);
      fail("Must throw NPE");
    }catch(NullPointerException ex){
      
    }
  }
  
  @Test
  public void testFindFieldForType(){
    final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap();
    final JBBPNumericField field1 = new JBBPFieldInt(new JBBPNamedFieldInfo("test.test", "test", 0), 123);
    final JBBPNumericField field2 = new JBBPFieldInt(new JBBPNamedFieldInfo("test.test2", "test2", 0), 123);
    final JBBPNumericField field3 = new JBBPFieldByte(new JBBPNamedFieldInfo("test.test3", "test3", 0), (byte)123);
    
    map.putField(field1);
    map.putField(field2);
    map.putField(field3);
    
    assertSame(field3, map.findFieldForType(JBBPFieldByte.class));
    assertNull(map.findFieldForType(JBBPFieldLong.class));

    try{
      map.findFieldForType(JBBPFieldInt.class);
      fail("Must throw exception JBBPTooManyFieldsFoundException");
    }catch(JBBPTooManyFieldsFoundException ex){
      assertEquals(2,ex.getNumberOfFoundInstances());
    }
    
    try {
      map.findFieldForType(null);
      fail("Must throw NPE");
    }
    catch (NullPointerException ex) {
    }
  }
  
  @Test
  public void testFindLastFieldForType(){
    final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap();
    final JBBPNumericField field1 = new JBBPFieldInt(new JBBPNamedFieldInfo("test.test", "test", 0), 123);
    final JBBPNumericField field2 = new JBBPFieldInt(new JBBPNamedFieldInfo("test.test2", "test2", 0), 123);
    final JBBPNumericField field3 = new JBBPFieldByte(new JBBPNamedFieldInfo("test.test3", "test3", 0), (byte)123);
    
    map.putField(field1);
    map.putField(field2);
    map.putField(field3);
    
    assertSame(field2, map.findLastFieldForType(JBBPFieldInt.class));
    assertNull(map.findLastFieldForType(JBBPFieldLong.class));

    try {
      map.findLastFieldForType(null);
      fail("Must throw NPE");
    }
    catch (NullPointerException ex) {
    }
  }
  
  @Test
  public void testFindFirstFieldForType(){
    final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap();
    final JBBPNumericField field1 = new JBBPFieldInt(new JBBPNamedFieldInfo("test.test", "test", 0), 123);
    final JBBPNumericField field2 = new JBBPFieldInt(new JBBPNamedFieldInfo("test.test2", "test2", 0), 123);
    final JBBPNumericField field3 = new JBBPFieldByte(new JBBPNamedFieldInfo("test.test3", "test3", 0), (byte)123);
    
    map.putField(field1);
    map.putField(field2);
    map.putField(field3);
    
    assertSame(field1, map.findFirstFieldForType(JBBPFieldInt.class));
    assertNull(map.findFirstFieldForType(JBBPFieldLong.class));

    try {
      map.findFirstFieldForType(null);
      fail("Must throw NPE");
    }
    catch (NullPointerException ex) {
    }
  }
  
  @Test
  public void testFindForFieldOffset(){
    final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap();
    final JBBPNumericField field1 = new JBBPFieldInt(new JBBPNamedFieldInfo("test.test", "test", 1), 123);
    final JBBPNumericField field2 = new JBBPFieldInt(new JBBPNamedFieldInfo("test.test2", "test2", 2222), 123);
    final JBBPNumericField field3 = new JBBPFieldByte(new JBBPNamedFieldInfo("test.test3", "test3", 3000), (byte)123);
    
    map.putField(field1);
    map.putField(field2);
    map.putField(field3);
    
    assertSame(field1, map.findForFieldOffset(1));
    assertSame(field2, map.findForFieldOffset(2222));
    assertSame(field3, map.findForFieldOffset(3000));
    assertNull(map.findForFieldOffset(123));
  }
  
  @Test
  public void testFindFieldForPath(){
    final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap();
    final JBBPNumericField field = new JBBPFieldInt(new JBBPNamedFieldInfo("test.test", "test", 0), 123);
    map.putField(field);
    assertSame(field, map.findFieldForPath("test.test"));
    assertNull(map.findFieldForPath("test.test1"));
    assertNull(map.findFieldForPath("test"));

    try {
      map.findFieldForPath(null);
      fail("Must throw NPE");
    }
    catch (NullPointerException ex) {
    }
  }
  
  @Test
  public void testFindFieldForPathAndType(){
    final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap();
    final JBBPNumericField field = new JBBPFieldInt(new JBBPNamedFieldInfo("test.test", "test", 0), 123);
    map.putField(field);
    assertSame(field, map.findFieldForPathAndType("TesT.tESt",JBBPFieldInt.class));
    assertNull(map.findFieldForPathAndType("test.test",JBBPFieldByte.class));
    assertNull(map.findFieldForPathAndType("test.test1",JBBPFieldInt.class));

    try{
      map.findFieldForPathAndType(null, JBBPFieldInt.class);
      fail("Must throw NPE");
    }catch(NullPointerException ex){
    }

    try{
      map.findFieldForPathAndType("test.test", null);
      fail("Must throw NPE");
    }catch(NullPointerException ex){
    }
  }
  
  @Test
  public void testFindFieldForNameAndType(){
    final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap();
    final JBBPNumericField field = new JBBPFieldInt(new JBBPNamedFieldInfo("test.test", "test", 0), 123);
    map.putField(field);
    assertSame(field, map.findFieldForNameAndType("tESt",JBBPFieldInt.class));
    assertNull(map.findFieldForNameAndType("test",JBBPFieldByte.class));
    assertNull(map.findFieldForNameAndType("test1",JBBPFieldInt.class));

    try{
      map.findFieldForNameAndType(null, JBBPFieldInt.class);
      fail("Must throw NPE");
    }catch(NullPointerException ex){
    }

    try{
      map.findFieldForNameAndType("test.test", null);
      fail("Must throw NPE");
    }catch(NullPointerException ex){
    }
  }
  
  @Test
  public void testPathExists(){
    final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap();
    final JBBPNumericField field = new JBBPFieldInt(new JBBPNamedFieldInfo("test.test", "test", 0), 123);
    map.putField(field);
    assertTrue(map.pathExists("tESt.teSt"));
    assertFalse(map.pathExists("test.test1"));
    assertFalse(map.pathExists("test"));
    
    try{
      map.pathExists(null);
      fail("Must throw NPE");
    }catch(NullPointerException ex){
      
    }
  }
  
  @Test
  public void testNameExists(){
    final JBBPNamedNumericFieldMap map = new JBBPNamedNumericFieldMap();
    final JBBPNumericField field = new JBBPFieldInt(new JBBPNamedFieldInfo("test.test", "test", 0), 123);
    map.putField(field);
    assertTrue(map.nameExists("tESt"));
    assertFalse(map.nameExists("test1"));
    assertFalse(map.nameExists("test.test"));
    
    try{
      map.nameExists(null);
      fail("Must throw NPE");
    }catch(NullPointerException ex){
      
    }
  }
  
}
