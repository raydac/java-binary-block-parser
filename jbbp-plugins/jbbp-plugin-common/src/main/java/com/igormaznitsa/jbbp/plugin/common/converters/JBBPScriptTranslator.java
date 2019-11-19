package com.igormaznitsa.jbbp.plugin.common.converters;

import com.igormaznitsa.jbbp.JBBPCustomFieldTypeProcessor;
import com.igormaznitsa.meta.common.utils.Assertions;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
  public final class Parameters {
    /**
     * Map of interface names to be implemented by subclasses.
     */
    @Nonnull
    private final Map<String, String> subClassInterfaces = new HashMap<String, String>();
    /**
     * Map of superclasses to be extended by subclasses.
     *
     * @since 1.4.0
     */
    @Nonnull
    private final Map<String, String> subClassSuperclasses = new HashMap<String, String>();

    /**
     * Flag to not make generated subclasses as static ones.
     *
     * @since 1.4.0
     */
    private boolean doInternalClassesNonStatic;
    /**
     * Set of interface names to be implemented by the main class.
     */
    @Nonnull
    private final Set<String> classImplements = new HashSet<String>();
    /**
     * Super class for main class.
     */
    @Nullable
    private String superClass = null;

    /**
     * Destination file name.
     *
     * @since 2.0.0
     */
    @Nullable
    private String destFileName = null;

    /**
     * Processor for custom types.
     */
    @Nullable
    JBBPCustomFieldTypeProcessor customFieldTypeProcessor = null;
    /**
     * Disable generate class fields.
     *
     * @since 1.4.0
     */
    private boolean disableGenerateFields = false;
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
     * Script text, it will be used only if {@link #scriptFile} is null
     *
     * @since 2.0.0
     */
    private String scriptText = null;
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
    /**
     * Turn on generate Bin annotations for fields.
     *
     * @since 2.0.0
     */
    private boolean addBinAnnotations;
    /**
     * Generate newInstance methods in classes.
     *
     * @since 2.0.0
     */
    private boolean addNewInstanceMethods;

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

    @Nullable
    public String getDestFileName() {
      return this.destFileName;
    }

    @Nonnull
    public Parameters setDestFileName(@Nullable final String name) {
      this.destFileName = name;
      return this;
    }

    public boolean isDisableGenerateFields() {
      return this.disableGenerateFields;
    }

    @Nonnull
    public Parameters setDisableGenerateFields(final boolean value) {
      this.disableGenerateFields = value;
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
    public Map<String, String> getSubClassSuperclasses() {
      return Collections.unmodifiableMap(this.subClassSuperclasses);
    }

    @Nonnull
    public Parameters setDoInternalClassesNonStatic(final boolean flag) {
      this.doInternalClassesNonStatic = flag;
      return this;
    }

    public boolean isDoInternalClassesNonStatic() {
      return this.doInternalClassesNonStatic;
    }

    public boolean isAddNewInstanceMethods() {
      return this.addNewInstanceMethods;
    }

    @Nonnull
    public Parameters setAddNewInstanceMethods(final boolean flag) {
      this.addNewInstanceMethods = flag;
      return this;
    }

    public boolean isAddBinAnnotations() {
      return this.addBinAnnotations;
    }

    @Nonnull
    public Parameters setAddBinAnnotations(final boolean flag) {
      this.addBinAnnotations = flag;
      return this;
    }

    @Nonnull
    public Parameters setSubClassSuperclasses(@Nonnull final Map<String, String> value) {
      this.subClassSuperclasses.clear();
      this.subClassSuperclasses.putAll(value);
      return this;
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

    @Nullable
    public File getScriptFile() {
      return this.scriptFile;
    }

    @Nonnull
    public Parameters setScriptFile(@Nullable final File file) {
      this.scriptFile = file;
      return this;
    }

    @Nullable
    public String getScriptText() {
      return this.scriptText;
    }

    @Nonnull
    public Parameters setScriptText(@Nonnull final String text) {
      this.scriptText = Assertions.assertNotNull(text);
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

    @Nullable
    public String getSuperClass() {
      return this.superClass;
    }
  }
}
