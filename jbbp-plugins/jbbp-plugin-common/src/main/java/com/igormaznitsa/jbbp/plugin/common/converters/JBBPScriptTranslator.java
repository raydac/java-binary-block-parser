package com.igormaznitsa.jbbp.plugin.common.converters;

import com.igormaznitsa.jbbp.JBBPCustomFieldTypeProcessor;
import com.igormaznitsa.meta.common.utils.Assertions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Interface for auxiliary class to process found JBBP script and translate them into set of files.
 *
 * @since 1.3.0
 */
public interface JBBPScriptTranslator {
  /**
   * Do translation.
   *
   * @param parameters parameter block, must not be null
   * @param dryRun     if true then only set of files will be returned without any creation.
   * @return set of result files created during operation.
   * @throws IOException if any transport error
   */
  @Nonnull
  Set<File> translate(@Nonnull Parameters parameters, boolean dryRun) throws IOException;

  /**
   * Class to be used as parameter container.
   *
   * @since 1.3.0
   */
  final class Parameters {
    /**
     * Map of interface names to be implemented by subclasses.
     */
    @Nonnull
    private final Map<String, String> subClassInterfaces = new HashMap<String, String>();
    /**
     * Set of interface names to be implemented by the main class.
     */
    @Nonnull
    private final Set<String> classImplements = new HashSet<String>();
    /**
     * Super class for main class.
     */
    @Nullable
    protected String superClass = null;
    /**
     * Processor for custom types.
     */
    @Nullable
    JBBPCustomFieldTypeProcessor customFieldTypeProcessor = null;
    /**
     * Flag to force abstract main class.
     */
    private boolean doAbstract = false;
    /**
     * Flag to generated getters and setters instead of direct access.
     */
    private boolean addGettersSetters = false;
    /**
     * Script file.
     */
    @Nullable
    private File scriptFile = null;
    /**
     * Package name for main class.
     */
    @Nullable
    private String packageName = null;
    /**
     * Comment to be placed in the start of generated class.
     */
    @Nullable
    private String headComment = null;
    /**
     * Text to be injected into class body.
     */
    @Nullable
    private String customText;
    /**
     * Encoding for input text data.
     */
    @Nonnull
    private String inEncoding = "UTF-8";
    /**
     * Encoding for output text data.
     */
    @Nonnull
    private String outEncoding = "UTF-8";
    /**
     * Output directory for result files.
     */
    @Nullable
    private File outputDir = null;
    /**
     * Parser flags.
     */
    private int parserFlags;

    @Nullable
    public String getPackageName() {
      return this.packageName;
    }

    @Nonnull
    public Parameters setPackageName(@Nullable final String value) {
      this.packageName = value;
      return this;
    }

    @Nonnull
    public Parameters setSuperClass(@Nullable final String value) {
      this.superClass = value;
      return this;
    }

    public boolean isAddGettersSetters() {
      return this.addGettersSetters;
    }

    @Nonnull
    public Parameters setAddGettersSetters(final boolean value) {
      this.addGettersSetters = value;
      return this;
    }

    @Nonnull
    public Set<String> getClassImplements() {
      return Collections.unmodifiableSet(this.classImplements);
    }

    @Nonnull
    public Parameters setClassImplements(@Nonnull final Set<String> value) {
      this.classImplements.clear();
      this.classImplements.addAll(value);
      return this;
    }

    @Nonnull
    public Map<String, String> getSubClassInterfaces() {
      return Collections.unmodifiableMap(this.subClassInterfaces);
    }

    @Nonnull
    public Parameters setSubClassInterfaces(@Nonnull final Map<String, String> value) {
      this.subClassInterfaces.clear();
      this.subClassInterfaces.putAll(value);
      return this;
    }

    public int getParserFlags() {
      return this.parserFlags;
    }

    @Nonnull
    public Parameters setParserFlags(final int value) {
      this.parserFlags = value;
      return this;
    }

    public boolean isDoAbstract() {
      return this.doAbstract;
    }

    @Nonnull
    public Parameters setDoAbstract(final boolean value) {
      this.doAbstract = value;
      return this;
    }

    @Nullable
    public File getOutputDir() {
      return this.outputDir;
    }

    @Nonnull
    public Parameters setOutputDir(@Nullable final File dir) {
      this.outputDir = dir;
      return this;
    }

    @Nonnull
    public File getScriptFile() {
      return Assertions.assertNotNull(this.scriptFile);
    }

    @Nonnull
    public Parameters setScriptFile(@Nonnull final File file) {
      this.scriptFile = Assertions.assertNotNull(file);
      return this;
    }

    @Nullable
    public String getHeadComment() {
      return this.headComment;
    }

    @Nonnull
    public Parameters setHeadComment(@Nullable final String text) {
      this.headComment = text;
      return this;
    }

    @Nullable
    public String getCustomText() {
      return this.customText;
    }

    @Nonnull
    public Parameters setCustomText(@Nullable final String text) {
      this.customText = text;
      return this;
    }

    @Nonnull
    public String getEncodingIn() {
      return this.inEncoding;
    }

    @Nonnull
    public Parameters setEncodingIn(@Nullable final String text) {
      this.inEncoding = Assertions.assertNotNull(text);
      return this;
    }

    @Nonnull
    public String getEncodingOut() {
      return this.outEncoding;
    }

    @Nonnull
    public Parameters setEncodingOut(@Nullable final String text) {
      this.outEncoding = Assertions.assertNotNull(text);
      return this;
    }

    @Nullable
    public JBBPCustomFieldTypeProcessor getCustomFieldTypeProcessor() {
      return this.customFieldTypeProcessor;
    }

    @Nonnull
    public Parameters setCustomFieldTypeProcessor(@Nullable final JBBPCustomFieldTypeProcessor customProcessor) {
      this.customFieldTypeProcessor = customProcessor;
      return this;
    }

    @Nonnull
    public Parameters assertAllOk() {
      if (this.scriptFile == null) {
        throw new NullPointerException("Script file is null");
      }
      return this;
    }
  }
}
