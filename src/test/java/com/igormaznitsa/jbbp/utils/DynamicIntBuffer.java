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

/**
 * Buffer to accumulate integer values. <b>NB!</b> It is being used in tests.
 */
public class DynamicIntBuffer {
    protected static final int INITIAL_CAPACITY_INCREASE = 128;

    protected int counter;
    protected int[] array;
    protected int capacityIncreaseStep;

    /**
     * Constructor with default buffer capacity.
     */
    public DynamicIntBuffer() {
        this(INITIAL_CAPACITY_INCREASE);
    }

    /**
     * Constructor with capacity.
     *
     * @param capacity capacity of the inside buffer, must be >=0
     */
    public DynamicIntBuffer(final int capacity) {
        this.array = new int[capacity];
        this.capacityIncreaseStep = INITIAL_CAPACITY_INCREASE;
    }

    /**
     * Write an integer value into the inside buffer.
     *
     * @param value value to be written
     */
    public void write(final int value) {
        if (this.array.length == this.counter) {
            if (this.capacityIncreaseStep < 0x1000) {
                this.capacityIncreaseStep <<= 2;
            } else if (this.capacityIncreaseStep > 0x10000) {
                this.capacityIncreaseStep += this.capacityIncreaseStep / 3;
            } else {
                this.capacityIncreaseStep <<= 1;
            }

            int newCapacity = this.array.length + this.capacityIncreaseStep;
            if (newCapacity < 0) newCapacity = Integer.MAX_VALUE;

            this.array = Arrays.copyOf(this.array, newCapacity);
        }
        this.array[this.counter++] = value;
    }

    /**
     * Get current length of written data.
     *
     * @return the length of currently written data.
     */
    public int length() {
        return this.counter;
    }

    /**
     * Reset the buffer data.
     *
     * @param capacity the capacity of the buffer
     */
    public void reset(final int capacity) {
        this.counter = 0;
        this.capacityIncreaseStep = INITIAL_CAPACITY_INCREASE;
        this.array = this.array.length == capacity ? this.array : new int[capacity];
    }

    /**
     * Convert current saved data into int array.
     *
     * @return int array from saved data, must not be null
     */
    public int[] toIntArray() {
        return Arrays.copyOf(this.array, this.counter);
    }
}
