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
package com.igormaznitsa.jbbp.model;

import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class JBBPAbstractArrayField<T> extends JBBPAbstractField implements Iterable<T> {

  public JBBPAbstractArrayField(final JBBPNamedFieldInfo name) {
    super(name);
  }

  public abstract int size();

  public abstract T getElementAt(int index);

  public abstract int getAsInt(int index);

  public abstract long getAsLong(int index);

  public abstract boolean getAsBool(int index);

  public Iterator<T> iterator() {
    return new Iterator<T>() {
      private int index = 0;

      public boolean hasNext() {
        return this.index < size();
      }

      public T next() {
        if (this.index >= size()) {
          throw new NoSuchElementException(this.index + ">=" + size());
        }
        return getElementAt(this.index++);
      }

      public void remove() {
        throw new UnsupportedOperationException("Removing is unsupported here");
      }

    };
  }
}
