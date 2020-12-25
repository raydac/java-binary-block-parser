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

package com.igormaznitsa.jbbp.model;

import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * The Class is the ancestor for all field which represent arrays.
 *
 * @param <T> type of field which can be contained in the array
 * @since 1.0
 */
public abstract class JBBPAbstractArrayField<T extends JBBPAbstractField> extends JBBPAbstractField
    implements Iterable<T> {
  private static final long serialVersionUID = -9007994400543951290L;

  /**
   * The Constructor.
   *
   * @param name the name descriptor for the array field, it can be null.
   */
  public JBBPAbstractArrayField(final JBBPNamedFieldInfo name) {
    super(name);
  }

  /**
   * Get number of elements in the array.
   *
   * @return the array size
   */
  public abstract int size();

  /**
   * Get element from the array for its index.
   *
   * @param index the array index
   * @return the array element for its index
   */
  public abstract T getElementAt(int index);

  /**
   * Get the value array as an object.
   *
   * @param reverseBits reverse bit order in values
   * @return the value array as an object
   */
  public abstract Object getValueArrayAsObject(boolean reverseBits);

  /**
   * Generates an iterator to allow the array processing in loops.
   *
   * @return an iterator for the array
   */
  @SuppressWarnings("NullableProblems")
  @Override
  public Iterator<T> iterator() {
    return new Iterator<T>() {
      private int index = 0;

      @Override
      public boolean hasNext() {
        return this.index < size();
      }

      @Override
      public T next() {
        if (this.index >= size()) {
          throw new NoSuchElementException(this.index + ">=" + size());
        }
        return getElementAt(this.index++);
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("Removing is unsupported here");
      }

    };
  }
}
