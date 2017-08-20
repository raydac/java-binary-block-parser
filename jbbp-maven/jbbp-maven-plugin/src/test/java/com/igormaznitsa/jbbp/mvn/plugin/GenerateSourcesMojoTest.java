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
package com.igormaznitsa.jbbp.mvn.plugin;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;

public class GenerateSourcesMojoTest extends AbstractMojoTestCase {

    private GenerateSourcesMojo findMojo(final String pomName, final String goal) throws Exception {
        final File pomFile = new File(this.getClass().getResource(pomName).toURI());
        final MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
        final ProjectBuildingRequest buildingRequest = executionRequest.getProjectBuildingRequest();
        final ProjectBuilder projectBuilder = this.lookup(ProjectBuilder.class);
        final MavenProject project = projectBuilder.build(pomFile, buildingRequest).getProject();
        return (GenerateSourcesMojo) this.lookupConfiguredMojo(project, goal);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

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
        final GenerateSourcesMojo mojo = findMojo("mojoConfig.xml", "jbbp");
        assertNotNull(mojo);

        assertTrue(mojo.getGenerateTestSources());
        assertTrue(mojo.getSkip());
        assertTrue(mojo.getVerbose());
        assertEquals("/some/custom/file", mojo.getCustomTextFile().getPath());
        assertEquals("public void test(){}", mojo.getCustomText());
        assertEquals("uber.package", mojo.getCommonPackage());
        assertEquals("/some/cap/file", mojo.getCapCommentFile().getPath());
        assertEquals("some cap text", mojo.getCapCommentText());
        assertEquals("/some/source", mojo.getSourceDirectory().getPath());
        assertEquals("/some/output", mojo.getOutputDirectory().getPath());
        assertEquals("IN-8", mojo.getInputEncoding());
        assertEquals("OUT-8", mojo.getOutputEncoding());
        assertEquals("com.igormaznitsa.Super", mojo.getSuperClass());
        assertTrue(mojo.getForceAbstract());
        assertTrue(mojo.getDoGettersSetters());
        assertArrayEquals(new String[]{"abc", "def"}, set2array(mojo.getCustomTypes()));
        assertArrayEquals(new String[]{"com.igormaznitsa.InterfaceA", "com.igormaznitsa.InterfaceB"}, set2array(mojo.getInterfaces()));
        assertArrayEquals(new String[]{"path1/**/*.jbbp", "path2/**/*.jbbp"}, set2array(mojo.getIncludes()));
        assertArrayEquals(new String[]{"path3/**/*.jbbp", "path4/**/*.jbbp"}, set2array(mojo.getExcludes()));

        final Map<String, String> mapStructToInterfaces = mojo.getMapStructToInterfaces();
        assertEquals(2, mapStructToInterfaces.size());
        assertEquals("com.test.C", mapStructToInterfaces.get("a.b.c"));
        assertEquals("com.test.D", mapStructToInterfaces.get("a.b.d"));
    }

}
