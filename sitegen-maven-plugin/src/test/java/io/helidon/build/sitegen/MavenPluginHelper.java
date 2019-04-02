/*
 * Copyright (c) 2018-2019 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.build.sitegen;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;

/**
 *
 * @author rgrecour
 */
public final class MavenPluginHelper extends AbstractMojoTestCase {

    private MavenPluginHelper(){
        try {
            this.setUp();
        } catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }

    private static class LazyHolder {
        static final MavenPluginHelper INSTANCE = new MavenPluginHelper();
    }

    public static MavenPluginHelper getInstance() {
        return LazyHolder.INSTANCE;
    }

    public MavenProject newMavenProject(String pom, File dir)
            throws IOException {

        File pomFile = getTestFile("src/test/resources/" + pom);
        org.apache.maven.model.Model model = new DefaultModelReader()
                .read(pomFile, Collections.emptyMap());
        model.getBuild().setDirectory(dir.getAbsolutePath());
        MavenProject project = new MavenProject(model);
        project.getProperties().put("project.build.sourceEncoding", "UTF-8");
        project.setFile(pomFile);
        return project;
    }

    public <T> T getMojo (String pom, File dir, String execName, Class<T> clazz) throws Exception {
        MavenProject project = newMavenProject(pom, dir);
        MavenSession session = newMavenSession(project);
        MojoExecution execution = newMojoExecution(execName);
        Mojo mojo = lookupConfiguredMojo(session, execution);
        assertNotNull(mojo);
        return clazz.cast(mojo);
    }
}
