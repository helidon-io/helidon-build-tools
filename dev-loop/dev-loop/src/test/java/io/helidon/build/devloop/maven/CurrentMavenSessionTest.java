/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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

package io.helidon.build.devloop.maven;

import java.util.List;
import java.util.Map;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.graph.DefaultProjectDependencyGraph;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests the session used for incremental builds after embedded full builds.
 */
class CurrentMavenSessionTest {

    @Test
    void testRefreshProjects() throws Exception {
        MavenSession outer = session("outer");
        CurrentMavenSession.initialize(outer);
        assertSame(outer, CurrentMavenSession.get());

        for (String artifactId : List.of("first-full-build", "second-full-build")) {
            MavenSession fullBuild = session(artifactId);
            CurrentMavenSession.set(fullBuild);
            MavenSession incremental = CurrentMavenSession.get();

            assertSame(outer.getRepositorySession(), incremental.getRepositorySession());
            assertSame(outer.getRequest(), incremental.getRequest());
            assertNotSame(fullBuild, incremental);
            assertNotSame(outer, incremental);
            assertSame(fullBuild.getCurrentProject(), incremental.getCurrentProject());
            assertSame(fullBuild.getTopLevelProject(), incremental.getTopLevelProject());
            assertSame(fullBuild.getProjects(), incremental.getProjects());
            assertSame(fullBuild.getAllProjects(), incremental.getAllProjects());
            assertSame(fullBuild.getProjectMap(), incremental.getProjectMap());
            assertSame(fullBuild.getProjectDependencyGraph(), incremental.getProjectDependencyGraph());
            assertSame(incremental, CurrentMavenSession.get());
            assertSame(outer.getAllProjects().get(0), outer.getCurrentProject());
        }
    }

    private static MavenSession session(String artifactId) throws Exception {
        Model model = new Model();
        model.setGroupId("test");
        model.setArtifactId(artifactId);
        model.setVersion("1");
        MavenProject project = new MavenProject(model);
        List<MavenProject> projects = List.of(project);
        MavenSession session = new MavenSession(null, new DefaultRepositorySystemSession(),
                                               new DefaultMavenExecutionRequest(), new DefaultMavenExecutionResult());
        session.setProjects(projects);
        session.setAllProjects(projects);
        session.setProjectMap(Map.of(project.getId(), project));
        session.setProjectDependencyGraph(new DefaultProjectDependencyGraph(projects));
        return session;
    }
}
