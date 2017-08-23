package com.igormaznitsa.jbbp.plugin.gradle;

import com.igormaznitsa.jbbp.plugin.common.converters.ParserFlags;
import com.igormaznitsa.jbbp.plugin.common.converters.Target;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JBBPExtension {
    public static final String NAME = "jbbp";

    public Target target = Target.JAVA_1_6;
    public boolean addGettersSetters = false;
    public boolean doAbstract = false;
    public Set<String> interfaces = new HashSet<String>();
    public Set<ParserFlags> parserFlags = new HashSet<ParserFlags>();
    public Set<String> customTypes = new HashSet<String>();
    public Map<String, String> mapSubClassInterfaces = new HashMap<String, String>();
    public String inEncoding = "UTF-8";
    public String outEncoding = "UTF-8";
    public File headCommentFile = null;
    public String  headComment = null;
    public File customTextFile = null;
    public String customText = null;
    public String superClass = null;
    public String packageName = null;
    public File output;
    public ConfigurableFileTree source;

    public JBBPExtension(@Nonnull final Project project) {
        this.source = project.fileTree("src/jbbp");
        this.source.include("**/*.jbbp");
        this.output = new File(project.getBuildDir(),"generated-jbbp-dir");
    }

}
