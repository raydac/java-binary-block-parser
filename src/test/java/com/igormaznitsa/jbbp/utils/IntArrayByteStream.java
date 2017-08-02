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
package com.igormaznitsa.jbbp.utils;

import java.util.Arrays;

public class IntArrayByteStream {
  private int counter;
  private int [] array = new int [1024];
  
  public IntArrayByteStream(){
  }
  
  public void write(final int value) {
    if (this.array.length == this.counter) {
      this.array = Arrays.copyOf(this.array, this.array.length+1024);
    }
    this.array[this.counter++] = value;
  }
  
  public int [] toIntArray(){
    return Arrays.copyOf(this.array, this.counter);
  }
}
