package com.igormaznitsa.jbbp.mvn.plugin.converters;

import com.igormaznitsa.jbbp.JBBPCustomFieldTypeProcessor;
import com.igormaznitsa.jbbp.mvn.plugin.GenerateSourcesMojo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;

public interface ScriptProcessor {
    void processScript(
            @Nonnull final GenerateSourcesMojo mojo,
            @Nonnull File jbbpScript,
            @Nullable String classCapComment,
            @Nullable String customText,
            @Nonnull String inEncoding,
            @Nonnull String outEncoding,
            @Nonnull JBBPCustomFieldTypeProcessor customFieldTypeProcessor
    ) throws IOException;

}
