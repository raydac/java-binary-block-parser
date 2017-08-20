package com.igormaznitsa.jbbp.mvn.plugin.converters;

import com.igormaznitsa.jbbp.JBBPCustomFieldTypeProcessor;
import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.compiler.conversion.JBBPToJava6Converter;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.mvn.plugin.GenerateSourcesMojo;
import com.igormaznitsa.jbbp.mvn.plugin.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Java16Converter  implements ScriptProcessor {
    @Override
    public void processScript(
            @Nonnull final GenerateSourcesMojo mojo,
            @Nonnull File jbbpScript,
            @Nullable String classCapComment,
            @Nullable String customText,
            @Nonnull String inEncoding,
            @Nonnull String outEncoding,
            @Nonnull JBBPCustomFieldTypeProcessor customFieldTypeProcessor
    ) throws IOException {
        final int pflags = mojo.makeParserFlags();

        final String text = FileUtils.readFileToString(jbbpScript, inEncoding);
        final String rawFileName = FilenameUtils.getBaseName(jbbpScript.getName());
        final String className = Utils.extractClassName(rawFileName);
        final String packageName = mojo.getCommonPackage() == null ? Utils.extractPackageName(rawFileName) : mojo.getCommonPackage();

        final File resultFile = new File(mojo.getOutputDirectory(), packageName.replace('.', File.separatorChar) + File.separatorChar + className + ".java");
        final JBBPParser parser = JBBPParser.prepare(text, JBBPBitOrder.LSB0, customFieldTypeProcessor, pflags);

        final String[] interfacesSorted = mojo.getInterfaces().toArray(new String[mojo.getInterfaces().size()]);
        Arrays.sort(interfacesSorted);

        final JBBPToJava6Converter converter = JBBPToJava6Converter.makeBuilder(parser)
                .setStructInterfaceMap(mojo.getMapStructToInterfaces())
                .setClassName(className)
                .setClassHeadComments(classCapComment)
                .setClassPackage(packageName)
                .setCustomText(customText)
                .setDoGettersSetters(mojo.getDoGettersSetters())
                .setForceAbstract(mojo.getForceAbstract())
                .setInterfaces(interfacesSorted)
                .setParserFlags(pflags)
                .setSuperclass(mojo.getSuperClass()).build();

        FileUtils.write(resultFile, converter.convert(), outEncoding);

        mojo.logInfo(String.format("JBBP script '%s' has been translated into '%s' file, package '%s'", jbbpScript.getName(), resultFile.getName(), packageName), false);

    }
}


