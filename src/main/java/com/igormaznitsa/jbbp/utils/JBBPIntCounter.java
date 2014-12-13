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
 * The Class implements a simple thread unsafe integer counter.
 * @since 1.0
 */
public final class JBBPIntCounter extends Number {
  private static final long serialVersionUID = -4457089530231871404L;
  
  /**
   * The Variable contains the counter value.
   */
  private int counter;
  
  /**
   * The Constructor to initialize the counter by 0.
   */
  public JBBPIntCounter(){
    this(0);
  }
  
  /**
   * The Constructor to initialize the counter by an integer value.
   * @param startValue the start integer value for the counter
   */
  public JBBPIntCounter(final int startValue){
    this.counter = startValue;
  }

  /**
   * Set the value for the counter.
   * @param value the new value for the counter
   */
  public void set(final int value){
    this.counter = value;
  }
  
  /**
   * Increase the counter.
   */
  public void inc(){
    this.counter++;
  }
  
  /**
   * Decrease the counter.
   */
  public void dec(){
    this.counter--;
  }
  
  /**
   * Get the counter value and increment the counter after result return.
   * @return the value before increment.
   */
  public int getAndIncrement(){
    return this.counter++;
  }
  
  /**
   * Increment the counter and get the incremented result.
   * @return the value after increment
   */
  public int incrementAndGet(){
    return ++this.counter;
  }
  
  /**
   * Get the counter value and decrement the counter after result return.
   * @return the value before decrement
   */
  public int getAndDecrement(){
    return this.counter--;
  }
  
  /**
   * Decrement the counter and return the decremented value.
   * @return the decremented value
   */
  public int decrementAndGet(){
    return --this.counter;
  }
  
  /**
   * Get the counter value.
   * @return the current counter value
   */
  public int get(){
    return this.counter;
  }
  
  /**
   * Get the current counter value and add a value after the return.
   * @param value a value to be added to the counter
   * @return the counter before addition
   */
  public int getAndAdd(final int value){
    final int result = this.counter;
    this.counter += value;
    return result;
  }
  
  @Override
  public int intValue() {
    return this.counter;
  }

  @Override
  public long longValue() {
    return (long) this.counter;
  }

  @Override
  public float floatValue() {
    return (float) this.counter;
  }

  @Override
  public double doubleValue() {
    return (double) this.counter;
  }
  
}
