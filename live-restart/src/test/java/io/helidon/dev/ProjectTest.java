/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.dev;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;

import io.helidon.build.test.TestFiles;
import io.helidon.dev.build.BuildComponent;
import io.helidon.dev.build.Project;
import io.helidon.dev.build.ProjectDirectory;

import org.junit.jupiter.api.Test;

import static io.helidon.dev.build.ProjectFactory.createProject;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit test for class {@link Project}.
 */
class ProjectTest {

    @Test
    void testSourceFileChangeDetected() {
        final Project project = createProject(TestFiles.helidonSeProject());
        assertThat(project, is(not(nullValue())));
        final BuildComponent sources = project.components().get(0);
        assertThat(sources, is(not(nullValue())));
        final ProjectDirectory sourceDir = sources.sourceDirectory();
        assertThat(sourceDir, is(not(nullValue())));
        assertThat(sourceDir.list().isEmpty(), is(false));
        assertThat(sourceDir.changes(), is(nullValue()));
        final Path sourceFile = new ArrayList<>(sourceDir.list()).get(0).path();

        TestFiles.touch(sourceFile);
        Set<Path> changes = sourceDir.changes();
        assertThat(changes.isEmpty(), is(false));
        assertThat(changes.contains(sourceFile), is(true));

        sourceDir.update();
        assertThat(sourceDir.changes(), is(nullValue()));
    }

    @Test
    void testSourceFileDeletionDetected() {
        // TODO, need app clone
    }
}
