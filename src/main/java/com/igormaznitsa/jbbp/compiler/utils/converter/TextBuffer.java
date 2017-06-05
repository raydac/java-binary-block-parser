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
package com.igormaznitsa.jbbp.compiler.utils.converter;

public class TextBuffer {

    private final StringBuilder buffer;


    public TextBuffer() {
        this.buffer = new StringBuilder();
    }

    public TextBuffer print(final int value) {
        this.buffer.append(value);
        return this;
    }

    public TextBuffer print(final boolean value) {
        this.buffer.append(value ? "true" : "false");
        return this;
    }

    public TextBuffer print(final String text) {
        this.buffer.append(text);
        return this;
    }

    public TextBuffer clean() {
        this.buffer.setLength(0);
        this.buffer.trimToSize();
        return this;
    }

    public TextBuffer tab() {
        this.buffer.append('\t');
        return this;
    }

    public TextBuffer println() {
        this.buffer.append('\n');
        return this;
    }

    public TextBuffer println(final String text) {
        return this.print(text).println();
    }

    public String toStringAndClean(final int spacesOnPrefix) {
        final StringBuilder locbuffer = new StringBuilder(spacesOnPrefix);
        for (int i = 0; i < spacesOnPrefix; i++) {
            locbuffer.append(' ');
        }
        final String prefix = locbuffer.toString();


        final boolean lastCharIsNL = this.buffer.length() > 0 ? this.buffer.charAt(this.buffer.length() - 1) == '\n' : false;
        final String[] splitted = this.buffer.toString().split("\n");

        locbuffer.setLength(0);

        for (int i = 0; i < splitted.length; i++) {
            if (i > 0) locbuffer.append('\n');
            locbuffer.append(prefix).append(splitted[i]);
        }
        if (lastCharIsNL) locbuffer.append('\n');

        return locbuffer.toString();
    }

    @Override
    public String toString() {
        return this.buffer.toString();
    }
}
