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

    private int tabCounter = 0;


    public TextBuffer incIndent() {
        this.tabCounter++;
        return this;
    }

    public TextBuffer decIndent() {
        if (this.tabCounter > 0) this.tabCounter--;
        return this;
    }


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

    public TextBuffer prints(final String text, final Object... args) {
        this.buffer.append(' ').append(String.format(text, args)).append(' ');
        return this;
    }

    public TextBuffer print(final String text, final Object... args) {
        this.buffer.append(String.format(text, args));
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

    public TextBuffer indent() {
        for (int i = 0; i < this.tabCounter; i++) this.tab();
        return this;
    }

    public TextBuffer println() {
        this.buffer.append('\n');
        return this;
    }

    public boolean isEmpty(){
        return this.buffer.length() == 0;
    }

    public TextBuffer println(final String text) {
        return this.print(text).println();
    }

    public TextBuffer printLinesWithIndent(final String text) {
        final String[] splitted = text.split("\n");

        for (final String aSplitted : splitted) {
            this.indent().println(aSplitted);
        }

        return this;
    }

    @Override
    public String toString() {
        return this.buffer.toString();
    }
}
