package com.igormaznitsa.jbbp.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.language.base.internal.tasks.SimpleStaleClassCleaner;
import org.gradle.language.base.internal.tasks.StaleClassCleaner;

import java.io.File;

public class JBBPTask extends DefaultTask {

    @TaskAction
    public void execute(){
        this.getInputs()
    }

}
