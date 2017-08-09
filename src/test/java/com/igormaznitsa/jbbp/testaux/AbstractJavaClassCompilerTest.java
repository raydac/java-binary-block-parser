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

import com.igormaznitsa.jbbp.JBBPCustomFieldTypeProcessor;
import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import javax.tools.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public abstract class AbstractJavaClassCompilerTest {

    protected static final String PACKAGE_NAME = "com.igormaznitsa.test";
    protected static final String CLASS_NAME = "TestClass";
    protected final Random RND = new Random(123456);

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    protected Object callRead(final Object instance, final byte[] array) throws Exception {
        try {
            return this.callRead(instance, new JBBPBitInputStream(new ByteArrayInputStream(array)));
        } catch (Exception ex) {
            if (ex instanceof InvocationTargetException) {
                if (ex.getCause() != null) throw (Exception) ex.getCause();
            }
            throw ex;
        }
    }

    protected Object callRead(final Object instance, final JBBPBitInputStream inStream) throws Exception {
        try {
            instance.getClass().getMethod("read", JBBPBitInputStream.class).invoke(instance, inStream);
            return instance;
        } catch (Exception ex) {
            if (ex instanceof InvocationTargetException) {
                if (ex.getCause() != null) throw (Exception) ex.getCause();
            }
            throw ex;
        }
    }

    protected byte[] callWrite(final Object instance) throws Exception {
        try {
            final ByteArrayOutputStream bout = new ByteArrayOutputStream();
            final JBBPBitOutputStream bitout = new JBBPBitOutputStream(bout);
            instance.getClass().getMethod("write", JBBPBitOutputStream.class).invoke(instance, bitout);
            bitout.close();
            return bout.toByteArray();
        } catch (Exception ex) {
            if (ex instanceof InvocationTargetException) {
                if (ex.getCause() != null) throw (Exception) ex.getCause();
            }
            throw ex;
        }
    }

    protected void callWrite(final Object instance, final JBBPBitOutputStream outStream) throws Exception {
        instance.getClass().getMethod("write", JBBPBitOutputStream.class).invoke(instance, outStream);
    }

    protected Object compileAndMakeInstance(final String script) throws Exception {
        return this.compileAndMakeInstance(PACKAGE_NAME + '.' + CLASS_NAME, script, null);
    }

    protected Object compileAndMakeInstance(final String script, final int parserFlags) throws Exception {
        return this.compileAndMakeInstance(PACKAGE_NAME + '.' + CLASS_NAME, script, parserFlags, null);
    }

    protected Object compileAndMakeInstance(final String instanceClassName, final String script, final JBBPCustomFieldTypeProcessor customFieldProcessor, final JavaClassContent... extraClasses) throws Exception {
        return this.compileAndMakeInstance(instanceClassName, script, 0, customFieldProcessor, extraClasses);
    }

    protected Object compileAndMakeInstance(final String instanceClassName, final String script, final int parserFlags, final JBBPCustomFieldTypeProcessor customFieldProcessor, final JavaClassContent... extraClasses) throws Exception {
        final List<JavaClassContent> klazzes = new ArrayList<JavaClassContent>(Arrays.asList(extraClasses));
        klazzes.add(0, new JavaClassContent(PACKAGE_NAME + '.' + CLASS_NAME, JBBPParser.prepare(script, JBBPBitOrder.LSB0, customFieldProcessor, parserFlags).makeClassSrc(PACKAGE_NAME, CLASS_NAME)));
        final ClassLoader cloader = saveAndCompile(klazzes.toArray(new JavaClassContent[klazzes.size()]));
        return cloader.loadClass(instanceClassName).newInstance();
    }

    public ClassLoader saveAndCompile(final JavaClassContent... klasses) throws IOException {
        return this.saveAndCompile(null, klasses);
    }

    public ClassLoader saveAndCompile(final ClassLoader classLoader, final JavaClassContent... klasses) throws IOException {
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

            for (final File f : classFiles) {
                System.err.println("File '" + f.getName() + '\'');
                System.err.println("-------------------------------------------");
                System.err.println(FileUtils.readFileToString(f));
            }

            throw new IOException("Error during compilation");
        }

        final ClassLoader result = classLoader == null ? new URLClassLoader(new URL[]{folder.toURI().toURL()}) : classLoader;
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
