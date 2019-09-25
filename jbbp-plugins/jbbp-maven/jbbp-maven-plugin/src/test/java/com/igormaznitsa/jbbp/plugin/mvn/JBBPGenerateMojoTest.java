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

package com.igormaznitsa.jbbp.plugin.mvn;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;


import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JBBPGenerateMojoTest extends AbstractMojoTestCase {

  private JBBPGenerateMojo findMojo(final String pomName, final String goal) throws Exception {
    final File pomFile = new File(this.getClass().getResource(pomName).toURI());
    final MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
    final ProjectBuildingRequest buildingRequest = executionRequest.getProjectBuildingRequest();
    final ProjectBuilder projectBuilder = this.lookup(ProjectBuilder.class);
    final MavenProject project = projectBuilder.build(pomFile, buildingRequest).getProject();
    return (JBBPGenerateMojo) this.lookupConfiguredMojo(project, goal);
  }

  @BeforeEach
  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @AfterEach
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  private String[] set2array(final Set<String> set) {
    final String[] arr = set.toArray(new String[set.size()]);
    Arrays.sort(arr);
    return arr;
  }

  @Test
  public void testConfig() throws Exception {
    final JBBPGenerateMojo mojo = findMojo("mojoConfig.xml", "generate");
    assertNotNull(mojo);

    assertTrue(mojo.getGenerateTestSources());
    assertTrue(mojo.getSkip());
    assertTrue(mojo.getVerbose());
    assertEquals("/some/custom/file", mojo.getCustomTextFile().getPath());
    assertEquals("public void test(){}", mojo.getCustomText());
    assertEquals("uber.package", mojo.getPackageName());
    assertEquals("/some/cap/file", mojo.getHeadCommentFile().getPath());
    assertEquals("some cap text", mojo.getHeadComment());
    assertEquals("/some/source", mojo.getSource().getPath());
    assertEquals("/some/output", mojo.getOutput().getPath());
    assertEquals("IN-8", mojo.getInputEncoding());
    assertEquals("OUT-8", mojo.getOutputEncoding());
    assertEquals("com.igormaznitsa.Super", mojo.getSuperClass());
    assertEquals("SOME_TARGET", mojo.getTarget());
    assertTrue(mojo.isDisableGenerateFields());
    assertTrue(mojo.isDoAbstract());
    assertTrue(mojo.isAddToSourceFolders());
    assertTrue(mojo.isAddToTestSourceFolders());
    assertTrue(mojo.getAddGettersSetters());
    assertArrayEquals(new String[] {"abc", "def"}, set2array(mojo.getCustomTypes()));
    assertArrayEquals(new String[] {"com.igormaznitsa.InterfaceA", "com.igormaznitsa.InterfaceB"}, set2array(mojo.getInterfaces()));
    assertArrayEquals(new String[] {"path1/**/*.jbbp", "path2/**/*.jbbp"}, set2array(mojo.getIncludes()));
    assertArrayEquals(new String[] {"path3/**/*.jbbp", "path4/**/*.jbbp"}, set2array(mojo.getExcludes()));

    assertTrue(mojo.isDoInnerClassesNonStatic());
    assertTrue(mojo.isDisableGenerateFields());

    final Map<String, String> mapStructToInterfaces = mojo.getMapStructToInterfaces();
    assertEquals(2, mapStructToInterfaces.size());
    assertEquals("com.test.C", mapStructToInterfaces.get("a.b.c"));
    assertEquals("com.test.D", mapStructToInterfaces.get("a.b.d"));

    final Map<String, String> mapStructToSuperclasses = mojo.getMapStructToSuperclasses();
    assertEquals(2, mapStructToSuperclasses.size());
    assertEquals("com.test.CC", mapStructToSuperclasses.get("a.b.c"));
    assertEquals("com.test.DD", mapStructToSuperclasses.get("a.b.d"));
  }

}
