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

package io.helidon.build.devloop.maven;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Optional;
import java.util.function.Predicate;

import io.helidon.build.common.FileChanges.DetectionType;
import io.helidon.build.common.Log;
import io.helidon.build.common.test.utils.ConfigurationParameterSource;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.params.ParameterizedTest;

import static io.helidon.build.common.FileChanges.DetectionType.FIRST;
import static io.helidon.build.common.FileChanges.DetectionType.LATEST;
import static io.helidon.build.common.FileChanges.changedSince;
import static io.helidon.build.common.FileUtils.lastModifiedTime;
import static io.helidon.build.common.FileUtils.touch;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit test for class {@link MavenProjectSupplier}.
 */
@Order(3)
class MavenProjectSupplierTestIT {

    private static final Predicate<Path> NOT_TEST_LOG = f -> !f.getFileName().toString().equals("test.log");
    private static final FileTime TIME_ZERO = FileTime.fromMillis(0);

    private static FileTime changedTime(Path dir, FileTime checkTime, DetectionType type) {
        Optional<FileTime> result = MavenProjectSupplier.changedSince(dir, checkTime, d -> true, NOT_TEST_LOG, type);
        assertThat(result, is(not(nullValue())));
        assertThat(result.isPresent(), is(true));
        return result.get();
    }

    private static FileTime touchFile(Path file) {
        Log.info("sleeping 1.25 seconds before touching %s, mod time is %s", file.getFileName(), lastModifiedTime(file));
        try {
            Thread.sleep(1250);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        touch(file);
        return lastModifiedTime(file);
    }

    private static FileTime lastModified(Path file) {
        FileTime result = lastModifiedTime(file);
        Log.info("%s mod time is %s", file.getFileName(), result);
        return result;
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testChangedSinceFromTimeZero(String basedir) {
        final Path projectDir = Path.of(basedir);
        FileTime pomTime = lastModifiedTime(projectDir.resolve("pom.xml"));
        FileTime fromTimeZero = changedTime(projectDir, TIME_ZERO, LATEST);
        assertThat(fromTimeZero.compareTo(pomTime), is(greaterThanOrEqualTo(0)));
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testChangedSinceClassFileNotChecked(String basedir) {
        final Path projectDir = Path.of(basedir);
        FileTime initial = changedTime(projectDir, TIME_ZERO, LATEST);
        Path classFile = projectDir.resolve("target/classes/io/helidon/build/devloop/tests/GreetService.class");
        FileTime touched = touchFile(classFile);

        // We should not find the class change since the filter won't allow visiting the target dir
        Optional<FileTime> found = MavenProjectSupplier.changedSince(projectDir, initial, d -> true, NOT_TEST_LOG, LATEST);
        assertThat(found.isPresent(), is(false));

        // Re-check that it is found without filter
        found = changedSince(projectDir, initial, d -> true, f -> true, LATEST);
        assertThat(found.isPresent(), is(true));
        assertThat(found.get(), is(touched));
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testChangedSinceOneFileChanged(String basedir) {
        final Path projectDir = Path.of(basedir);
        Path pomFile = projectDir.resolve("pom.xml");
        FileTime initial = lastModified(pomFile);
        FileTime touched = touchFile(pomFile);
        assertThat(touched.compareTo(initial), is(greaterThan(0)));

        FileTime found = changedTime(projectDir, initial, LATEST);
        assertThat(found.compareTo(initial), is(greaterThan(0)));
        assertThat(found, is(touched));
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testChangedSinceTwoFilesChanged(String basedir) {
        final Path projectDir = Path.of(basedir);
        Path pomFile = projectDir.resolve("pom.xml");
        FileTime initialPom = lastModified(pomFile);
        FileTime touchedPom = touchFile(pomFile);
        assertThat(touchedPom.compareTo(initialPom), is(greaterThan(0)));

        Path javaFile = projectDir.resolve("src/main/java/io/helidon/build/devloop/tests/GreetService.java");
        FileTime initialJava = lastModified(javaFile);
        FileTime touchedJava = touchFile(javaFile);
        assertThat(touchedJava.compareTo(initialJava), is(greaterThan(0)));

        assertThat(touchedJava, is(not(touchedPom)));
        assertThat(touchedJava.compareTo(touchedPom), is(greaterThan(0)));

        // Check LATEST should find touchedJava since it was most recent

        FileTime found = changedTime(projectDir, initialPom, LATEST);
        assertThat(found, is(touchedJava));

        // Check FIRST should find touchedPom since it was changed first, but this is
        // file system iteration order dependent so just make sure it is one of them

        found = changedTime(projectDir, initialPom, FIRST);
        if (!found.equals(touchedPom) && !found.equals(touchedJava)) {
            fail("Expected either " + touchedPom + " or " + touchedJava + ", got " + found);
        }
    }
}
