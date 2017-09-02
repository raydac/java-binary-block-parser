package com.igormaznitsa.jbbp.plugin.gradle;

import com.igormaznitsa.jbbp.plugin.common.converters.ParserFlags;
import com.igormaznitsa.jbbp.plugin.common.converters.Target;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileTree;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * JBBP extension settings.
 *
 * @since 1.3.0
 */
public class JBBPExtension {
    /**
     * The Registered name of the extension.
     */
    public static final String EXT_NAME = "jbbp";

    /**
     * Target of translation.
     */
    public Target target = Target.JAVA_1_6;

    /**
     * Flag to generate getters and setters in result class, all fields will be protected ones.
     */
    public boolean addGettersSetters = false;

    /**
     * Make the result class as abstract one even if it doesn't have abstract methods.
     */
    public boolean doAbstract = false;

    /**
     * Interfaces to be implemented by the generated class.
     */
    public Set<String> interfaces = new HashSet<String>();

    /**
     * Set of parser flags.
     */
    public Set<ParserFlags> parserFlags = new HashSet<ParserFlags>();

    /**
     * Allowed custom types for JBBP scripts.
     */
    public Set<String> customTypes = new HashSet<String>();

    /**
     * Interface names mapped to generated sub-classes, mapped interface will be used in getters instead of the sub-class type.
     */
    public Map<String, String> mapSubClassInterfaces = new HashMap<String, String>();

    /**
     * Encoding for text input.
     */
    public String inEncoding = "UTF-8";

    /**
     * Encoding for text output.
     */
    public String outEncoding = "UTF-8";

    /**
     * Text to be placed in the head of generated class as comments.
     */
    public String headComment = null;

    /**
     * Text file which content will be added into head of generated class as comments.
     */
    public File headCommentFile = null;

    /**
     * Text to be inserted into body of generated class.
     */
    public String customText = null;

    /**
     * Text file which content will be injected into body of generated class.
     */
    public File customTextFile = null;

    /**
     * Super-class for the generated class.
     */
    public String superClass = null;

    /**
     * Package name to override the package name extracted from JBBP script name.
     */
    public String packageName = null;

    /**
     * Output folder for result classes.
     */
    public File output;

    /**
     * Source folder to find JBBP scripts.
     */
    public ConfigurableFileTree source;

    public JBBPExtension(@Nonnull final Project project) {
        this.source = project.fileTree("src/jbbp");
    }

    public void prepare(@Nonnull final Project project) {
        if (this.source == null) {
            this.source = project.fileTree("src/jbbp");
        }
        if (this.source.getIncludes().isEmpty()) {
            this.source.include("**/*.jbbp");
        }
        if (this.output == null) {
            this.output = new File(project.getBuildDir(), "generated-jbbp-dir");
        }
    }

}
