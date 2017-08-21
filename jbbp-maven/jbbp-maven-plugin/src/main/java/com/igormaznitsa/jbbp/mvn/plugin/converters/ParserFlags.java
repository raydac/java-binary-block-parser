package com.igormaznitsa.jbbp.mvn.plugin.converters;

import com.igormaznitsa.jbbp.JBBPParser;

/**
 * Allowed parser flags.
 */
public enum ParserFlags {
    /**
     * Do not throw exception if input stream ended unexpectedly and not all
     * fields have been read.
     */
    SKIP_REMAINING_FIELDS_IF_EOF(JBBPParser.FLAG_SKIP_REMAINING_FIELDS_IF_EOF);

    private final int flag;

    ParserFlags(final int flag) {
        this.flag = flag;
    }

    /**
     * Get the flag value.
     *
     * @return the flag value as integer
     */
    public int getFlag() {
        return this.flag;
    }

}
