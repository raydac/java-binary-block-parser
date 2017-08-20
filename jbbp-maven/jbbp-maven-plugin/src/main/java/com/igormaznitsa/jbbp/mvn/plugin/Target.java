package com.igormaznitsa.jbbp.mvn.plugin;

import com.igormaznitsa.jbbp.mvn.plugin.converters.Java16Converter;
import com.igormaznitsa.jbbp.mvn.plugin.converters.ScriptProcessor;

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
