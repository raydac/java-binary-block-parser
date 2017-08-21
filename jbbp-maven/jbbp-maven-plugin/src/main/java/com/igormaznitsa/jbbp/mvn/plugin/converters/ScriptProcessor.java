package com.igormaznitsa.jbbp.mvn.plugin.converters;

import com.igormaznitsa.jbbp.JBBPCustomFieldTypeProcessor;
import com.igormaznitsa.jbbp.mvn.plugin.JBBPGenerateMojo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Set;

public interface ScriptProcessor {
    void processScript(
            @Nonnull final JBBPGenerateMojo mojo,
            @Nonnull File jbbpScript,
            @Nullable String classCapComment,
            @Nullable String customText,
            @Nonnull String inEncoding,
            @Nonnull String outEncoding,
            @Nonnull JBBPCustomFieldTypeProcessor customFieldTypeProcessor
    ) throws IOException;

    @Nonnull
    Set<File> makeTargetFiles(@Nullable File targetDir, @Nullable String classPackage, @Nonnull File jbbpScript);
}
