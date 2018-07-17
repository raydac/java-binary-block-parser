package com.igormaznitsa.jbbp.plugin.common.converters;

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.compiler.conversion.JBBPToJava6Converter;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.plugin.common.utils.CommonUtils;
import com.igormaznitsa.meta.common.utils.Assertions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class Java16Converter implements JBBPScriptTranslator {
  @Override
  @Nonnull
  public Set<File> translate(@Nonnull final Parameters parameters, final boolean dryRun) throws IOException {
    final File scriptToProcess = Assertions.assertNotNull(parameters.getScriptFile());

    final String text = FileUtils.readFileToString(scriptToProcess, parameters.getEncodingIn());
    final String rawFileName = FilenameUtils.getBaseName(scriptToProcess.getName());
    final String className = CommonUtils.extractClassName(rawFileName);
    final String packageName = parameters.getPackageName() == null ? CommonUtils.extractPackageName(rawFileName) : parameters.getPackageName();

    final Set<File> resultFiles = Collections.singleton(CommonUtils.scriptFileToJavaFile(parameters.getOutputDir(), parameters.getPackageName(), parameters.getScriptFile()));
    if (!dryRun) {

      final File resultJavaFile = resultFiles.iterator().next();

      final JBBPParser parser = JBBPParser.prepare(text, JBBPBitOrder.LSB0, parameters.customFieldTypeProcessor, parameters.getParserFlags());

      final String[] implementsSorted = parameters.getClassImplements().toArray(new String[parameters.getClassImplements().size()]);
      Arrays.sort(implementsSorted);

      final JBBPToJava6Converter.Builder builder = JBBPToJava6Converter.makeBuilder(parser)
          .setMapSubClassesInterfaces(parameters.getSubClassInterfaces())
          .setMapSubClassesSuperclasses(parameters.getSubClassSuperclasses())
          .setMainClassName(className)
          .setHeadComment(parameters.getHeadComment())
          .setMainClassPackage(packageName)
          .setMainClassCustomText(parameters.getCustomText())
          .setAddGettersSetters(parameters.isAddGettersSetters())
          .setDoMainClassAbstract(parameters.isDoAbstract())
          .setMainClassImplements(implementsSorted)
          .setParserFlags(parameters.getParserFlags())
          .setSuperClass(parameters.superClass);

      if (parameters.isDoInternalClassesNonStatic()) {
        builder.doInternalClassesNonStatic();
      }

      if (parameters.isDisableGenerateFields()) {
        builder.disableGenerateFields();
      }
      
      FileUtils.write(resultJavaFile, builder.build().convert(), parameters.getEncodingOut());
    }
    return resultFiles;
  }
}


