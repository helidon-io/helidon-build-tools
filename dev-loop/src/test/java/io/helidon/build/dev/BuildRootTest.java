/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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

package io.helidon.build.dev;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.helidon.build.test.TestFiles;
import io.helidon.build.util.FileUtils;

import org.junit.jupiter.api.Test;

import static io.helidon.build.util.FileUtils.assertDir;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit test for class {@link BuildRoot}.
 */
class BuildRootTest {

    private static BuildRoot sourceDirectory(boolean willModify) {
        final Path project = willModify ? TestFiles.helidonSeProjectCopy() : TestFiles.helidonSeProject();
        final Path sources = assertDir(project.resolve("src/main/java"));
        final BuildRoot result = BuildRoot.createBuildRoot(BuildRootType.JAVA_SOURCES, sources);
        assertThat(result, is(not(nullValue())));
        assertThat(result.list().isEmpty(), is(false));
        assertThat(result.changes().isEmpty(), is(true));
        return result;
    }

    @Test
    void testFileChangeDetected() {
        final BuildRoot sourceDir = sourceDirectory(true);
        final Path sourceFile = new ArrayList<>(sourceDir.list()).get(0).path();

        FileUtils.touch(sourceFile);
        BuildRoot.Changes changes = sourceDir.changes();
        assertThat(changes.isEmpty(), is(false));
        assertThat(changes.isEmpty(), is(false));
        assertThat(changes.size(), is(1));
        assertThat(changes.modified().contains(sourceFile), is(true));

        sourceDir.update();
        assertThat(sourceDir.changes().isEmpty(), is(true));
    }

    @Test
    void testFileRemovedDetected() throws IOException {
        final BuildRoot sourceDir = sourceDirectory(true);
        final Path sourceFile = new ArrayList<>(sourceDir.list()).get(0).path();

        FileUtils.delete(sourceFile);
        BuildRoot.Changes changes = sourceDir.changes();
        assertThat(changes, is(not(nullValue())));
        assertThat(changes.isEmpty(), is(false));
        assertThat(changes.size(), is(1));
        assertThat(changes.removed().contains(sourceFile), is(true));

        sourceDir.update();
        assertThat(sourceDir.changes().isEmpty(), is(true));
    }

    @Test
    void testFileAdditionDetected() {
        final BuildRoot sourceDir = sourceDirectory(true);

        final Path newSourceFile = FileUtils.ensureFile(sourceDir.path().resolve("NewSource.java"));
        BuildRoot.Changes changes = sourceDir.changes();
        assertThat(changes, is(not(nullValue())));
        assertThat(changes.isEmpty(), is(false));
        assertThat(changes.size(), is(1));
        assertThat(changes.added().contains(newSourceFile), is(true));

        sourceDir.update();
        assertThat(sourceDir.changes().isEmpty(), is(true));
    }

    @Test
    void testMultipleChangesDetected() throws IOException {
        final BuildRoot sourceDir = sourceDirectory(true);
        final List<BuildFile> sources = new ArrayList<>(sourceDir.list());
        assertThat(sources.size(), is(greaterThanOrEqualTo(3)));
        final Path newPackageDir = FileUtils.ensureDirectory(sourceDir.path().resolve("foo"));
        final Path added1 = FileUtils.ensureFile(sourceDir.path().resolve("NewSource.java"));
        final Path added2 = FileUtils.ensureFile(newPackageDir.resolve("FooSource.java"));
        final Path modified1 = FileUtils.touch(sources.get(0).path());
        final Path modified2 = FileUtils.touch(sources.get(1).path());
        final Path deleted = FileUtils.delete(sources.get(2).path());

        BuildRoot.Changes changes = sourceDir.changes();
        assertThat(changes, is(not(nullValue())));
        assertThat(changes.isEmpty(), is(false));
        assertThat(changes.size(), is(5));

        assertThat(changes.added().size(), is(2));
        assertThat(changes.added().contains(added1), is(true));
        assertThat(changes.added().contains(added2), is(true));

        assertThat(changes.modified().size(), is(2));
        assertThat(changes.modified().contains(modified1), is(true));
        assertThat(changes.modified().contains(modified2), is(true));

        assertThat(changes.removed().size(), is(1));
        assertThat(changes.removed().contains(deleted), is(true));

        sourceDir.update();
        assertThat(sourceDir.changes().isEmpty(), is(true));
    }
}
