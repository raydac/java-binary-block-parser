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

/**
 * Text buffer to provide text manipulation operations to form Java style sources.
 *
 * @since 1.3.0
 */
public class JavaSrcTextBuffer {

    private final StringBuilder buffer;

    private int tabCounter = 0;

    /**
     * Constructor with default capacity.
     */
    public JavaSrcTextBuffer() {
        this.buffer = new StringBuilder();
    }

    /**
     * Constructor with capacity value.
     *
     * @param capacity capacity of the created buffer
     */
    public JavaSrcTextBuffer(final int capacity) {
        this.buffer = new StringBuilder(capacity);
    }

    /**
     * Increase indent
     *
     * @return this instance
     */
    public JavaSrcTextBuffer incIndent() {
        this.tabCounter++;
        return this;
    }

    /**
     * Decrease indent
     *
     * @return this instance
     */
    public JavaSrcTextBuffer decIndent() {
        if (this.tabCounter > 0) this.tabCounter--;
        return this;
    }

    /**
     * Print integer value
     *
     * @param value integer value
     * @return this instance
     */
    public JavaSrcTextBuffer print(final int value) {
        this.buffer.append(value);
        return this;
    }

    /**
     * Print boolean value
     *
     * @param value boolean value
     * @return this instance
     */
    public JavaSrcTextBuffer print(final boolean value) {
        this.buffer.append(value ? "true" : "false");
        return this;
    }

    /**
     * Foratted print.
     *
     * @param text format string
     * @param args arguments for formatted string
     * @return this instance
     * @see String#format(String, Object...)
     */
    public JavaSrcTextBuffer printf(final String text, final Object... args) {
        this.buffer.append(String.format(text, args));
        return this;
    }

    /**
     * Print string
     *
     * @param text string
     * @return this instance
     */
    public JavaSrcTextBuffer print(final String text) {
        this.buffer.append(text);
        return this;
    }

    /**
     * Clean buffer
     *
     * @return this instance
     */
    public JavaSrcTextBuffer clean() {
        this.buffer.setLength(0);
        this.buffer.trimToSize();
        return this;
    }

    /**
     * Print tab char
     *
     * @return this instance
     */
    public JavaSrcTextBuffer tab() {
        this.buffer.append('\t');
        return this;
    }

    /**
     * Print tabs for current indent number
     *
     * @return this instance
     */
    public JavaSrcTextBuffer indent() {
        for (int i = 0; i < this.tabCounter; i++) this.tab();
        return this;
    }

    /**
     * Print next line char
     *
     * @return this instance
     */
    public JavaSrcTextBuffer println() {
        this.buffer.append(String.format("%n"));
        return this;
    }

    /**
     * Check that the buffer is empty
     *
     * @return true if the buffer is empty, false otherwise
     */
    public boolean isEmpty() {
        return this.buffer.length() == 0;
    }

    /**
     * Print text and next line char in the end
     *
     * @param text the text to be printed
     * @return this instance
     */
    public JavaSrcTextBuffer println(final String text) {
        return this.print(text).println();
    }

    /**
     * Parse string to lines and print each line with current indent
     *
     * @param text the text to be printed
     * @return this instance
     */
    public JavaSrcTextBuffer printLinesWithIndent(final String text) {
        final String[] splitted = text.split("\n");

        for (final String aSplitted : splitted) {
            this.indent().println(aSplitted);
        }

        return this;
    }

    /**
     * Parse string to lines and print each line with '//' comment
     *
     * @param text text to be printed
     * @return this instance
     */
    public JavaSrcTextBuffer printCommentLinesWithIndent(final String text) {
        final String[] splitted = text.split("\n");

        for (final String aSplitted : splitted) {
            this.indent().print("// ").println(aSplitted);
        }

        return this;
    }

    /**
     * Print string as multiline java comment started with '/*'
     *
     * @param text text to be printed as multiline comment
     * @return this instance
     */
    public JavaSrcTextBuffer printCommentMultiLinesWithIndent(final String text) {
        final String[] splitted = text.split("\n");

        this.indent().println("/*");
        for (final String aSplitted : splitted) {
            this.indent().print(" * ").println(aSplitted);
        }
        this.indent().println(" */");

        return this;
    }

    /**
     * Print string as multiline java comment started with '/**'
     *
     * @param text text to be printed as multiline comment
     * @return this instance
     */
    public JavaSrcTextBuffer printJavaDocLinesWithIndent(final String text) {
        final String[] splitted = text.split("\n");

        this.indent().println("/**");
        for (final String aSplitted : splitted) {
            this.indent().print(" * ").println(aSplitted);
        }
        this.indent().println(" */");

        return this;
    }

    @Override
    public String toString() {
        return this.buffer.toString();
    }
}
