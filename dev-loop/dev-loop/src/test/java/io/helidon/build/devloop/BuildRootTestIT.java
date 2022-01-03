/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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

package io.helidon.build.devloop;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.helidon.build.common.test.utils.ConfigurationParameterSource;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.params.ParameterizedTest;

import static io.helidon.build.common.FileUtils.delete;
import static io.helidon.build.common.FileUtils.ensureDirectory;
import static io.helidon.build.common.FileUtils.ensureFile;
import static io.helidon.build.common.FileUtils.requireDirectory;
import static io.helidon.build.common.FileUtils.touch;
import static io.helidon.build.common.FileUtils.unique;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit test for class {@link BuildRoot}.
 */
@Order(5)
class BuildRootTestIT {

    private static BuildRoot sourceDirectory(String basedir) {
        final Path projectDir = Path.of(basedir);
        final Path sources = requireDirectory(projectDir.resolve("src/main/java"));
        final BuildRoot result = BuildRoot.createBuildRoot(BuildRootType.javaSources(), sources);
        assertThat(result, is(not(nullValue())));
        assertThat(result.list().isEmpty(), is(false));
        assertThat(result.changes().isEmpty(), is(true));
        return result;
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testFileChangeDetected(String basedir) {
        final BuildRoot sourceDir = sourceDirectory(basedir);
        final Path sourceFile = new ArrayList<>(sourceDir.list()).get(0).path();

        touch(sourceFile);
        BuildRoot.Changes changes = sourceDir.changes();
        assertThat(changes.isEmpty(), is(false));
        assertThat(changes.isEmpty(), is(false));
        assertThat(changes.size(), is(1));
        assertThat(changes.modified().contains(sourceFile), is(true));

        sourceDir.update();
        assertThat(sourceDir.changes().isEmpty(), is(true));
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testFileRemovedDetected(String basedir) {
        final BuildRoot sourceDir = sourceDirectory(basedir);
        final Path sourceFile = new ArrayList<>(sourceDir.list()).get(0).path();

        delete(sourceFile);
        BuildRoot.Changes changes = sourceDir.changes();
        assertThat(changes, is(not(nullValue())));
        assertThat(changes.isEmpty(), is(false));
        assertThat(changes.size(), is(1));
        assertThat(changes.removed().contains(sourceFile), is(true));

        sourceDir.update();
        assertThat(sourceDir.changes().isEmpty(), is(true));
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testFileAdditionDetected(String basedir) {
        final BuildRoot sourceDir = sourceDirectory(basedir);
        final Path newSourceFile = ensureFile(unique(sourceDir.path(), "NewSource", ".java"));
        BuildRoot.Changes changes = sourceDir.changes();
        assertThat(changes, is(not(nullValue())));
        assertThat(changes.isEmpty(), is(false));
        assertThat(changes.size(), is(1));
        assertThat(changes.added().contains(newSourceFile), is(true));

        sourceDir.update();
        assertThat(sourceDir.changes().isEmpty(), is(true));
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testMultipleChangesDetected(String basedir) {
        final BuildRoot sourceDir = sourceDirectory(basedir);
        final List<BuildFile> sources = new ArrayList<>(sourceDir.list());
        assertThat(sources.size(), is(greaterThanOrEqualTo(2)));
        final Path added1 = ensureFile(unique(sourceDir.path(), "NewSource", ".java"));
        final Path newPackageDir = ensureDirectory(sourceDir.path().resolve("foo"));
        final Path added2 = ensureFile(unique(newPackageDir, "FooSource", ".java"));
        final Path modified1 = touch(sources.get(0).path());
        final Path modified2 = touch(sources.get(1).path());
        final Path deleted = delete(sources.get(2).path());

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
