package com.igormaznitsa.jbbp.plugin.common.converters;

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.compiler.conversion.JBBPToJava6Converter;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.plugin.common.utils.CommonUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class Java16Converter implements ScriptProcessor {
    @Override
    @Nonnull
    public Set<File> processScript(@Nonnull final ScriptProcessor.Parameters parameters) throws IOException {

        final String text = FileUtils.readFileToString(parameters.scriptFile, parameters.inEncoding);
        final String rawFileName = FilenameUtils.getBaseName(parameters.scriptFile.getName());
        final String className = CommonUtils.extractClassName(rawFileName);
        final String packageName = parameters.classPackageName == null ? CommonUtils.extractPackageName(rawFileName) : parameters.classPackageName;

        final Set<File> resultFiles = makeTargetFiles(parameters.outputDir, parameters.classPackageName, parameters.scriptFile);

        final File resultJavaFile = resultFiles.iterator().next();

        final JBBPParser parser = JBBPParser.prepare(text, JBBPBitOrder.LSB0, parameters.customFieldTypeProcessor, parameters.parserFlags);

        final String[] interfacesSorted = parameters.classInterfaces.toArray(new String[parameters.classInterfaces.size()]);
        Arrays.sort(interfacesSorted);

        final JBBPToJava6Converter converter = JBBPToJava6Converter.makeBuilder(parser)
                .setStructInterfaceMap(parameters.mapClassInterfaces)
                .setClassName(className)
                .setClassHeadComments(parameters.classCapComment)
                .setClassPackage(packageName)
                .setCustomText(parameters.customText)
                .setDoGettersSetters(parameters.doGettersSetters)
                .setForceAbstract(parameters.forceAbstract)
                .setInterfaces(interfacesSorted)
                .setParserFlags(parameters.parserFlags)
                .setSuperclass(parameters.superClass).build();

        FileUtils.write(resultJavaFile, converter.convert(), parameters.outEncoding);

        return resultFiles;
    }

    @Override
    @Nonnull
    public Set<File> makeTargetFiles(@Nullable File targetDir, @Nullable String classPackage, @Nonnull File jbbpScript) {
        return Collections.singleton(CommonUtils.scriptFileToJavaFile(targetDir, classPackage, jbbpScript));
    }
}


