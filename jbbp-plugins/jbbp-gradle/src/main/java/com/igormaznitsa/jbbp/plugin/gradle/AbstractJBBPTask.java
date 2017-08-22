package com.igormaznitsa.jbbp.plugin.gradle;

import com.igormaznitsa.jbbp.plugin.common.converters.Target;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.tasks.*;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractJBBPTask extends SourceTask {

    @Input
    @Optional
    protected Target target = Target.JAVA_1_6;

    @OutputDirectory
    @Optional
    protected File output = new File(getProject().getBuildDir(), "generated-src");

    public AbstractJBBPTask() {
        this.source.add(getProject().fileTree("src/jbbp"));
        this.include("**/*.jbbp");
    }

    public File getOutput() {
        return this.output;
    }

    public void setOutput(final File file) {
        this.output = file;
    }

    public Target getTarget() {
        return this.target;
    }

    public void setTarget(final Target value) {
        this.target = value;
    }

    @TaskAction
    public final void doAction() {
        doTaskAction();
    }

    protected Set<File> findScripts() {
        final Set<File> result = new HashSet<File>();

        this.getSource().visit(new FileVisitor() {
            @Override
            public void visitDir(final FileVisitDetails fileVisitDetails) {

            }

            @Override
            public void visitFile(final FileVisitDetails fileVisitDetails) {
                result.add(fileVisitDetails.getFile());
            }
        });
        return result;
    }

    protected abstract void doTaskAction();

}
