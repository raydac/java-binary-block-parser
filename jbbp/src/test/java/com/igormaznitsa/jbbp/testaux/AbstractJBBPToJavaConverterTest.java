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
import com.igormaznitsa.jbbp.compiler.conversion.JBBPToJavaConverter;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;
import com.igormaznitsa.jbbp.utils.ReflectUtils;
import com.igormaznitsa.jbbp.utils.TargetSources;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractJBBPToJavaConverterTest {

  protected static final String PACKAGE_NAME = "com.igormaznitsa.test";
  protected static final String CLASS_NAME = "TestClass";
  protected static TemporaryFolder tempFolder = new TemporaryFolder();
  protected final Random testRandomGen = new Random(123456);

  protected boolean printGeneratedClassText = false;

  @BeforeAll
  public static void beforeAll() {
    tempFolder = new TemporaryFolder();
  }

  @AfterAll
  public static void afterAll() {
    if (tempFolder != null) {
      tempFolder.dispose();
    }
  }

  protected static Map<String, String> makeMap(final String... mapvalue) {
    final Map<String, String> result = new HashMap<>();
    int i = 0;
    while (i < mapvalue.length) {
      result.put(mapvalue[i++], mapvalue[i++]);
    }
    return result;
  }

  protected Object callRead(final Object instance, final byte[] array) throws Exception {
    try {
      return this.callRead(instance, new JBBPBitInputStream(new ByteArrayInputStream(array)));
    } catch (InvocationTargetException ex) {
      if (ex.getCause() != null) {
        throw (Exception) ex.getCause();
      } else {
        throw ex;
      }
    }
  }

  protected Object callRead(final Object instance, final JBBPBitInputStream inStream) throws Exception {
    try {
      instance.getClass().getMethod("read", JBBPBitInputStream.class).invoke(instance, inStream);
      return instance;
    } catch (InvocationTargetException ex) {
      if (ex.getTargetException() != null) {
        throw (Exception) ex.getTargetException();
      } else {
        throw ex;
      }
    }
  }

  protected byte[] callWrite(final Object instance) throws Exception {
    try {
      final ByteArrayOutputStream bout = new ByteArrayOutputStream();
      final JBBPBitOutputStream bitout = new JBBPBitOutputStream(bout);
      instance.getClass().getMethod("write", JBBPBitOutputStream.class).invoke(instance, bitout);
      bitout.close();
      return bout.toByteArray();
    } catch (InvocationTargetException ex) {
      if (ex.getCause() != null) {
        throw (Exception) ex.getCause();
      } else {
        throw ex;
      }
    }
  }

  protected void callWrite(final Object instance, final JBBPBitOutputStream outStream) throws Exception {
    instance.getClass().getMethod("write", JBBPBitOutputStream.class).invoke(instance, outStream);
  }

  protected Object compileAndMakeInstanceSrc(final String script, final String classCustomText, final StringBuilder srcBuffer) throws Exception {
    final String classBody = JBBPToJavaConverter.makeBuilder(JBBPParser.prepare(script)).setMainClassName(CLASS_NAME).setMainClassPackage(PACKAGE_NAME).setMainClassCustomText(classCustomText).build().convert();
    if (srcBuffer != null) {
      srcBuffer.append(classBody);
    }
    final ClassLoader cloader = saveAndCompile(new JavaClassContent(PACKAGE_NAME + '.' + CLASS_NAME, classBody));
    return ReflectUtils.newInstance(cloader.loadClass(PACKAGE_NAME + '.' + CLASS_NAME));
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
    final List<JavaClassContent> klazzes = new ArrayList<>(Arrays.asList(extraClasses));
    final JavaClassContent klazzContent = new JavaClassContent(PACKAGE_NAME + '.' + CLASS_NAME, JBBPParser.prepare(script, JBBPBitOrder.LSB0, customFieldProcessor, parserFlags).convertToSrc(TargetSources.JAVA_1_6, PACKAGE_NAME + "." + CLASS_NAME).get(0).getResult().values().iterator().next());
    if (this.printGeneratedClassText) {
      System.out.println(klazzContent.classText);
    }
    klazzes.add(0, klazzContent);
    final ClassLoader cloader = saveAndCompile(klazzes.toArray(new JavaClassContent[0]));
    return ReflectUtils.newInstance(cloader.loadClass(instanceClassName));
  }

  public ClassLoader saveAndCompile(final JavaClassContent... klasses) throws IOException {
    return this.saveAndCompile(null, klasses);
  }

  public ClassLoader saveAndCompile(final ClassLoader classLoader, final JavaClassContent... klasses) throws IOException {
    final File folder = tempFolder.newFolder();

    final List<File> classFiles = new ArrayList<>();

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
    final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    final StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
    final Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(classFiles);

    if (!compiler.getTask(null, fileManager, null, null, null, compilationUnits).call()) {
      for (final Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
        System.err.format("Error on line %d in %s%n", diagnostic.getLineNumber(), diagnostic.getSource());
      }

      for (final File f : classFiles) {
        System.err.println("File '" + f.getName() + '\'');
        System.err.println("-------------------------------------------");
        System.err.println(FileUtils.readFileToString(f, StandardCharsets.UTF_8));
      }

      throw new IOException("Error during compilation");
    }

    return classLoader == null ? new URLClassLoader(new URL[] {folder.toURI().toURL()}) : classLoader;
  }

  protected static class TemporaryFolder {

    private final File folder;
    private final AtomicBoolean disposed = new AtomicBoolean(false);

    public TemporaryFolder() {
      final String localTmpFolderPath = System.getProperty("jbbp.target.folder", null);
      if (localTmpFolderPath == null) {
        throw new Error("Temp folder is not defined among system properties");
      }
      final File localTmpFolderAsFile = new File(localTmpFolderPath);

      if (!localTmpFolderAsFile.isDirectory() && !localTmpFolderAsFile.mkdirs()) {
        throw new Error("Can't create main temp folder : " + localTmpFolderAsFile);
      }

      try {
        final Path path = Files.createTempDirectory(localTmpFolderAsFile.toPath(), "jbbp2j6");
        this.folder = path.toFile();
        this.folder.deleteOnExit();
      } catch (IOException ex) {
        throw new Error("Can't create tem directory", ex);
      }
    }

    public File newFolder() {
      if (this.disposed.get()) {
        throw new IllegalStateException("Already disposed");
      }
      try {
        final File result = Files.createTempDirectory(this.folder.toPath(), "_jbbp").toFile();
        result.deleteOnExit();
        return result;
      } catch (IOException ex) {
        throw new Error("Can't make new sub-folder in temp folder : " + this.folder.getAbsolutePath(), ex);
      }
    }

    public void dispose() {
      if (this.disposed.compareAndSet(false, true)) {
        try {
          FileUtils.deleteDirectory(this.folder);
        } catch (IOException ex) {
          throw new Error("Can't delete emp directory : " + this.folder.getAbsolutePath(), ex);
        }
      } else {
        throw new Error("Already disposed");
      }
    }
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
