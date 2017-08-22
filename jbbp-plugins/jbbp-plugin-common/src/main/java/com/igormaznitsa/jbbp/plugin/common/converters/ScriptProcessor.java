package com.igormaznitsa.jbbp.plugin.common.converters;

import com.igormaznitsa.jbbp.JBBPCustomFieldTypeProcessor;
import com.igormaznitsa.meta.common.utils.Assertions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public interface ScriptProcessor {
    @Nonnull
    Set<File> processScript(@Nonnull Parameters parameters) throws IOException;

    @Nonnull
    Set<File> makeTargetFiles(@Nullable File targetDir, @Nullable String classPackage, @Nonnull File jbbpScript);

    final class Parameters {
        @Nonnull
        protected final Map<String, String> mapClassInterfaces = new HashMap<String, String>();
        @Nonnull
        protected final Set<String> classInterfaces = new HashSet<String>();
        protected boolean forceAbstract = false;
        protected boolean doGettersSetters = false;
        @Nullable
        protected File scriptFile = null;
        @Nullable
        protected String classPackageName = null;
        @Nullable
        protected String classCapComment = null;
        @Nullable
        protected String customText;
        @Nonnull
        protected String inEncoding = "UTF-8";
        @Nonnull
        protected String outEncoding = "UTF-8";
        @Nullable
        protected JBBPCustomFieldTypeProcessor customFieldTypeProcessor = null;
        @Nullable
        protected File outputDir = null;
        protected int parserFlags;
        @Nullable
        protected String superClass = null;

        @Nonnull
        public Parameters setSuperClass(@Nullable final String value) {
            this.superClass = value;
            return this;
        }

        @Nonnull
        public Parameters setDoGettersSetters(final boolean value) {
            this.doGettersSetters = value;
            return this;
        }

        @Nonnull
        public Parameters setClassInterfaces(@Nonnull final Set<String> value) {
            this.classInterfaces.clear();
            this.classInterfaces.addAll(value);
            return this;
        }

        @Nonnull
        public Parameters setMapClassInterfaces(@Nonnull final Map<String, String> value) {
            this.mapClassInterfaces.clear();
            this.mapClassInterfaces.putAll(value);
            return this;
        }

        @Nonnull
        public Parameters setParserFlags(final int value) {
            this.parserFlags = value;
            return this;
        }

        @Nonnull
        public Parameters setForceAbstract(final boolean value) {
            this.forceAbstract = value;
            return this;
        }

        @Nonnull
        public Parameters setOutputDir(@Nonnull final File dir) {
            this.outputDir = Assertions.assertNotNull(dir);
            return this;
        }

        @Nonnull
        public Parameters setScriptFile(@Nonnull final File file) {
            this.scriptFile = Assertions.assertNotNull(file);
            return this;
        }

        @Nonnull
        public Parameters setClassCapComment(@Nullable final String text) {
            this.classCapComment = text;
            return this;
        }

        @Nonnull
        public Parameters setCustomText(@Nullable final String text) {
            this.customText = text;
            return this;
        }

        @Nonnull
        public Parameters setEncodingIn(@Nullable final String text) {
            this.inEncoding = Assertions.assertNotNull(text);
            return this;
        }

        @Nonnull
        public Parameters setEncodingOut(@Nullable final String text) {
            this.outEncoding = Assertions.assertNotNull(text);
            return this;
        }

        @Nonnull
        public Parameters setCustomFieldTypeProcessor(@Nullable final JBBPCustomFieldTypeProcessor customProcessor) {
            this.customFieldTypeProcessor = customProcessor;
            return this;
        }

        @Nonnull
        public Parameters assertAllOk() {
            if (this.scriptFile == null) throw new NullPointerException("Script file is null");
            if (this.outputDir == null) throw new NullPointerException("Output directory is null");
            return this;
        }
    }
}
