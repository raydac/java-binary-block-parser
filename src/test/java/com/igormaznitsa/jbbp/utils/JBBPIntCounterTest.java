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

import org.junit.Test;
import static org.junit.Assert.*;

public class JBBPIntCounterTest {

  @Test
  public void testConstructors(){
    assertEquals(0, new JBBPIntCounter().get());
    assertEquals(999, new JBBPIntCounter(999).get());
  }
  
  @Test
  public void testGetAndIncrement(){
    final JBBPIntCounter counter = new JBBPIntCounter();
    assertEquals(0, counter.getAndIncrement());
    assertEquals(1, counter.getAndIncrement());
  }
  
  @Test
  public void testIncrementAndGet(){
    final JBBPIntCounter counter = new JBBPIntCounter();
    assertEquals(1, counter.incrementAndGet());
    assertEquals(2, counter.incrementAndGet());
  }
  
  @Test
  public void testGetAndDecrement(){
    final JBBPIntCounter counter = new JBBPIntCounter(5);
    assertEquals(5, counter.getAndDecrement());
    assertEquals(4, counter.getAndDecrement());
  }
  
  @Test
  public void testDecrementAndGet(){
    final JBBPIntCounter counter = new JBBPIntCounter(5);
    assertEquals(4, counter.decrementAndGet());
    assertEquals(3, counter.decrementAndGet());
  }
  
  @Test
  public void testGet(){
    final JBBPIntCounter counter = new JBBPIntCounter(5);
    assertEquals(5, counter.get());
  }
  
  @Test
  public void testSet(){
    final JBBPIntCounter counter = new JBBPIntCounter(5);
    assertEquals(5, counter.get());
    counter.set(345);
    assertEquals(345, counter.get());
  }
  
  @Test
  public void testIntValue(){
    final JBBPIntCounter counter = new JBBPIntCounter(5);
    assertEquals(5, counter.intValue());
  }
  
  @Test
  public void testLongValue(){
    final JBBPIntCounter counter = new JBBPIntCounter(5);
    assertEquals(5L, counter.longValue());
  }
  
  @Test
  public void testFloatValue(){
    final JBBPIntCounter counter = new JBBPIntCounter(5);
    assertTrue(Float.compare(5.0f, counter.floatValue())==0);
  }
  
  @Test
  public void testDoubleValue(){
    final JBBPIntCounter counter = new JBBPIntCounter(5);
    assertTrue(Double.compare(5.0d, counter.doubleValue())==0);
  }
  
  @Test
  public void testInc(){
    final JBBPIntCounter counter = new JBBPIntCounter(5);
    counter.inc();
    assertEquals(6,counter.get());
  }
  
  @Test
  public void testDec(){
    final JBBPIntCounter counter = new JBBPIntCounter(5);
    counter.dec();
    assertEquals(4,counter.get());
  }
  
  @Test
  public void testGetAndAdd(){
    final JBBPIntCounter counter = new JBBPIntCounter(5);
    assertEquals(5,counter.getAndAdd(3));
    assertEquals(8,counter.getAndAdd(0));
  }
  
}
