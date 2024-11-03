package com.igormaznitsa.jbbp.plugin.gradle;

import static com.igormaznitsa.jbbp.utils.JBBPUtils.ARRAY_STRING_EMPTY;

import com.igormaznitsa.jbbp.JBBPCustomFieldTypeProcessor;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPFieldTypeParameterContainer;
import com.igormaznitsa.jbbp.io.JBBPArraySizeLimiter;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.model.JBBPAbstractField;
import com.igormaznitsa.jbbp.plugin.common.converters.JBBPScriptTranslator;
import com.igormaznitsa.jbbp.plugin.common.converters.ParserFlags;
import com.igormaznitsa.jbbp.plugin.common.converters.Target;
import com.igormaznitsa.jbbp.plugin.common.utils.CommonUtils;
import com.igormaznitsa.meta.annotation.MustNotContainNull;
import com.igormaznitsa.meta.common.utils.Assertions;
import com.igormaznitsa.meta.common.utils.GetUtils;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.gradle.api.GradleException;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.SourceSet;

/**
 * Task to translate found JBBP scripts in source files.
 *
 * @since 1.3.0
 */
public class JBBPGenerateTask extends AbstractJBBPTask {

  /**
   * Flag to register the output folder in Java source folders at the end of process.
   */
  @Input
  protected boolean addSource = true;

  public boolean isAddSource() {
    return this.addSource;
  }

  @Override
  protected void doTaskAction(@Nonnull final JBBPExtension ext) {
    final Target target = GetUtils.ensureNonNull(ext.target, Target.JAVA);

    final Set<String> normalizedCustomTypeNames = new HashSet<String>();
    if (ext.customTypes != null) {
      for (final String s : ext.customTypes) {
        final String trimmed = s.trim();
        final String normalized = trimmed.toLowerCase(Locale.ENGLISH);
        if (!normalized.equals(trimmed)) {
          getLogger().warn(String
              .format("Custom type name '%s' in JBBP normal form is '%s' ", trimmed, normalized));
        }
        normalizedCustomTypeNames.add(normalized);
      }
      getLogger().debug("Defined normalized custom types : " + normalizedCustomTypeNames);
    }
    final String[] customTypesArray = normalizedCustomTypeNames.toArray(ARRAY_STRING_EMPTY);

    final JBBPCustomFieldTypeProcessor customFieldProcessor = new JBBPCustomFieldTypeProcessor() {
      @Override
      @Nonnull
      @MustNotContainNull
      public String[] getCustomFieldTypes() {
        return customTypesArray;
      }

      @Override
      public boolean isAllowed(@Nonnull final JBBPFieldTypeParameterContainer fieldType,
                               @Nullable final String fieldName, final int extraData,
                               final boolean isArray) {
        final boolean result = normalizedCustomTypeNames.contains(fieldType.getTypeName());
        if (!result) {
          getLogger().warn("Detected not allowed custom type name : " + fieldType.getTypeName());
        }
        return result;
      }

      @Override
      @Nonnull
      public JBBPAbstractField readCustomFieldType(@Nonnull final JBBPBitInputStream in,
                                                   @Nonnull final JBBPBitOrder bitOrder,
                                                   final int parserFlags, @Nonnull
                                                   final JBBPFieldTypeParameterContainer customTypeFieldInfo,
                                                   @Nullable final JBBPNamedFieldInfo fieldName,
                                                   final int extraData,
                                                   final boolean readWholeStream,
                                                   final int arrayLength,
                                                   final JBBPArraySizeLimiter arraySizeLimiter)
          throws IOException {
        throw new Error("Must not be called");
      }
    };


    final JBBPScriptTranslator.Parameters parameters = new JBBPScriptTranslator.Parameters();

    parameters
        .setParserFlags(ParserFlags.makeFromSet(ext.parserFlags))
        .setPackageName(ext.packageName)
        .setOutputDir(ext.output)
        .setHeadComment(getTextOrFileContent(ext, ext.headComment, ext.headCommentFile))
        .setCustomText(getTextOrFileContent(ext, ext.customText, ext.customTextFile))
        .setEncodingIn(CommonUtils.ensureEncodingName(ext.inEncoding))
        .setEncodingOut(CommonUtils.ensureEncodingName(ext.outEncoding))
        .setCustomFieldTypeProcessor(customFieldProcessor)
        .setSuperClass(ext.superClass)
        .setAddNewInstanceMethods(ext.addNewInstanceMethods)
        .setAddBinAnnotations(ext.addBinAnnotations)
        .setClassImplements(ext.interfaces)
        .setSubClassInterfaces(ext.mapSubClassInterfaces)
        .setSubClassSuperclasses(ext.mapSubClassSuperclasses)
        .setDoInternalClassesNonStatic(ext.doInternalClassesNonStatic)
        .setAddGettersSetters(ext.addGettersSetters)
        .setDoAbstract(ext.doAbstract)
        .setDisableGenerateFields(ext.disableGenerateFields);


    for (final File aScript : findScripts(ext)) {
      parameters.setScriptFile(aScript).assertAllOk();
      getLogger().info("Detected JBBP script file : " + aScript);
      try {
        final Set<File> files = target.getTranslator().translate(parameters, false);
        getLogger().debug("Converted " + aScript + " into " + files);
        for (final File f : files) {
          getLogger().info(String
              .format("JBBP script '%s' has been converted into '%s'", aScript.getName(),
                  f.getName()));
        }
      } catch (IOException ex) {
        throw new GradleException(
            "Error during JBBP script translation : " + aScript.getAbsolutePath(), ex);
      }
    }


    if (this.addSource) {
      getLogger().debug("Registering path to java sources : " +
          Assertions.assertNotNull("Output must not be null", ext.output));
      if (getProject().getPlugins().hasPlugin(JavaPlugin.class)) {
        final JavaPluginConvention javaPluginConvention =
            getProject().getConvention().getPlugin(JavaPluginConvention.class);
        final SourceSet main =
            javaPluginConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        main.getJava().srcDir(ext.output);
        getLogger().info("Source folder has been added into Java  task : " + ext.output);
      } else {
        getLogger().info("Java plugin not found");
      }
    } else {
      getLogger().info("Source folder registration has been disabled");
    }
  }
}
