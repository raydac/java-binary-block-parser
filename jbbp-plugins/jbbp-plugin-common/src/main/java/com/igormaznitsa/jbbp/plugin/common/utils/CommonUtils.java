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

package com.igormaznitsa.jbbp.plugin.common.utils;

import org.apache.commons.io.FilenameUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;

/**
 * Misc auxiliary methods.
 *
 * @since 1.3.0
 */
public final class CommonUtils {
  private CommonUtils() {
  }

  /**
   * Get charset name. If name is null then default charset name provided.
   *
   * @param charsetName name of charset, can be null
   * @return charset name, must not be null
   * @throws IllegalArgumentException if charset name can't be recognized
   */
  @Nonnull
  public static String ensureEncodingName(@Nullable final String charsetName) {
    final Charset defaultCharset = Charset.defaultCharset();
    try {
      return (charsetName == null) ? defaultCharset.name() : Charset.forName(charsetName.trim()).name();
    } catch (IllegalCharsetNameException ex) {
      throw new IllegalArgumentException("Can't recognize charset for name '" + charsetName + '\'');
    }
  }

  /**
   * Extract class name from canonical Java class name
   *
   * @param canonicalJavaClassName canonical class name (like 'a.b.c.SomeClassName'), must not be null
   * @return extracted class name, must not be null but can be empty for case "a.b.c.d."
   */
  @Nonnull
  public static String extractClassName(@Nonnull final String canonicalJavaClassName) {
    final int lastDot = canonicalJavaClassName.lastIndexOf('.');
    if (lastDot < 0) {
      return canonicalJavaClassName.trim();
    }
    return canonicalJavaClassName.substring(lastDot + 1).trim();
  }

  /**
   * Extract package name from canonical Java class name
   *
   * @param fileNameWithoutExtension canonical class name (like 'a.b.c.SomeClassName'), must not be null
   * @return extracted package name, must not be null but can be empty
   */
  @Nonnull
  public static String extractPackageName(@Nonnull final String fileNameWithoutExtension) {
    final int lastDot = fileNameWithoutExtension.lastIndexOf('.');
    if (lastDot < 0) {
      return "";
    }
    return fileNameWithoutExtension.substring(0, lastDot).trim();
  }

  /**
   * Convert script file into path to Java class file.
   *
   * @param targetDir    the target dir for generated sources, it can be null
   * @param classPackage class package to override extracted one from script name, it can be null
   * @param scriptFile   the script file, must not be null
   * @return java source file for the script file
   */
  @Nonnull
  public static File scriptFileToJavaFile(@Nullable final File targetDir, @Nullable final String classPackage, @Nonnull final File scriptFile) {
    final String rawFileName = FilenameUtils.getBaseName(scriptFile.getName());
    final String className = CommonUtils.extractClassName(rawFileName);
    final String packageName = classPackage == null ? CommonUtils.extractPackageName(rawFileName) : classPackage;

    String fullClassName = packageName.isEmpty() ? className : packageName + '.' + className;
    fullClassName = fullClassName.replace('.', File.separatorChar) + ".java";

    return new File(targetDir, fullClassName);
  }
}
