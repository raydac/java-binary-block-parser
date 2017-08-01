/*
 * Copyright 2017 Igor Maznitsa.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.igormaznitsa.jbbp.testaux;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractJavaClassCompilerTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    public ClassLoader saveAndCompile(final JavaClassContent... klasses) throws IOException {
        final File folder = this.tempFolder.newFolder();

        final List<File> classFiles = new ArrayList<File>();

        for (final JavaClassContent c : klasses) {
            final File classFile = c.makeFile(folder);
            final File pack = classFile.getParentFile();
            if (!pack.isDirectory() && !pack.mkdirs()) {
                throw new IOException("Can't create folder : " + pack);
            }

            FileUtils.writeStringToFile(classFile, c.getText(), "UTF-8");
            classFiles.add(classFile);
        }

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        final StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        final Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(classFiles);

        if (!compiler.getTask(null, fileManager, null, null, null, compilationUnits).call()) {
            for (final Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                System.err.format("Error on line %d in %s%n", diagnostic.getLineNumber(), diagnostic.getSource());
            }
            
            for(final File f : classFiles){
              System.err.println("File '"+f.getName()+'\'');
              System.err.println("-------------------------------------------");
              System.err.println(FileUtils.readFileToString(f));
            }
            
            throw new IOException("Error during compilation");
        }

        final ClassLoader result = new URLClassLoader(new URL[]{folder.toURI().toURL()});
        return result;
        
    }

    public final static class JavaClassContent {
        private final String className;
        private final String classText;

        public JavaClassContent(final String className, final String classText) {
            this.className = className;
            this.classText = classText;
        }

        public File makeFile(final File folder) {
            return new File(folder, this.className.replace('.', '/') + ".java");
        }

        public String getText() {
            return this.classText;
        }
    }

}
