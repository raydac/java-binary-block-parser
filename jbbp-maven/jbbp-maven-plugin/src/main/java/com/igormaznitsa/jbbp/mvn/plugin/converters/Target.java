package com.igormaznitsa.jbbp.mvn.plugin.converters;

import javax.annotation.Nonnull;

public enum Target {
    JAVA_1_6(new Java16Converter());

    private final ScriptProcessor scriptProcessor;

    Target(@Nonnull final ScriptProcessor processor) {
        this.scriptProcessor = processor;
    }

    @Nonnull
    public ScriptProcessor getScriptProcessor() {
        return this.scriptProcessor;
    }
}
