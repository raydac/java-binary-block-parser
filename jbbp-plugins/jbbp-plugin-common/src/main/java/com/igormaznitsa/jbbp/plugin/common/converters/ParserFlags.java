package com.igormaznitsa.jbbp.plugin.common.converters;

import com.igormaznitsa.jbbp.JBBPParser;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Allowed parser flags.
 *
 * @since 1.3.0
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

    public static int makeFromSet(@Nullable final Set<ParserFlags> set) {
        int result = 0;
        if (set!=null){
            for(final ParserFlags f : set) {
                result |= f.getFlag();
            }
        }
        return result;
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
