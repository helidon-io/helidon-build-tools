/*
 * Copyright (c) 2018, 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.build.maven.sitegen.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;

/**
 * Maven plugin helper.
 */
@SuppressWarnings({"NewClassNamingConvention", "unused", "UnconstructableJUnitTestCase"})
public final class MavenPluginHelper extends AbstractMojoTestCase {

    /**
     * Create a new instance.
     */
    public MavenPluginHelper() {
        try {
            this.setUp();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static class LazyHolder {
        static final MavenPluginHelper INSTANCE = new MavenPluginHelper();
    }

    /**
     * Get a mojo.
     *
     * @param pom      pom
     * @param dir      dir
     * @param execName exec name
     * @return MavenProject
     * @throws IOException if an IO error occurs
     */
    static <T> T mojo(String pom, Path dir, String execName, Class<T> clazz) throws Exception {
        return LazyHolder.INSTANCE.mojo0(pom, dir, execName, clazz);
    }

    /**
     * Create a new maven project.
     *
     * @param pom pom
     * @param dir dir
     * @return MavenProject
     * @throws IOException if an IO error occurs
     */
    static MavenProject newMavenProject(String pom, Path dir) throws IOException {
        return LazyHolder.INSTANCE.newMavenProject0(pom, dir);
    }

    private MavenProject newMavenProject0(String pom, Path dir) throws IOException {
        File pomFile = getTestFile("src/test/resources/" + pom);
        org.apache.maven.model.Model model = new DefaultModelReader().read(pomFile, Map.of());
        model.getBuild().setDirectory(dir.toAbsolutePath().toString());
        MavenProject project = new MavenProject(model);
        project.getProperties().put("project.build.sourceEncoding", "UTF-8");
        project.setFile(pomFile);
        return project;
    }

    private <T> T mojo0(String pom, Path dir, String execName, Class<T> clazz) throws Exception {
        MavenProject project = newMavenProject0(pom, dir);
        MavenSession session = newMavenSession(project);
        MojoExecution execution = newMojoExecution(execName);
        Mojo mojo = lookupConfiguredMojo(session, execution);
        assertNotNull(mojo);
        return clazz.cast(mojo);
    }
}
