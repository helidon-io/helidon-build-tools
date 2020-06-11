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

package io.helidon.build.dev.maven;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Optional;

import io.helidon.build.util.FileUtils;
import io.helidon.build.util.Log;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.helidon.build.test.TestFiles.helidonSeProjectCopy;
import static io.helidon.build.util.FileUtils.touch;
import static io.helidon.build.util.FileUtils.ChangeDetectionType.FIRST;
import static io.helidon.build.util.FileUtils.ChangeDetectionType.LATEST;
import static io.helidon.build.util.FileUtils.assertFile;
import static io.helidon.build.util.FileUtils.lastModifiedTime;
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
class MavenProjectSupplierTest {
    private static final FileTime TIME_ZERO = FileTime.fromMillis(0);

    private static Path rootDir;
    private static Path pomFile;
    private static Path javaFile;
    private static Path classFile;

    @BeforeAll
    public static void beforeAll() {
        rootDir = helidonSeProjectCopy();
        pomFile = assertFile(rootDir.resolve("pom.xml"));
        javaFile = assertFile(rootDir.resolve("src/main/java/io/helidon/examples/se/GreetService.java"));
        classFile = assertFile(rootDir.resolve("target/classes/io/helidon/examples/se/GreetService.class"));
    }

    private static FileTime changedTime(FileTime checkTime, FileUtils.ChangeDetectionType type) {
        Optional<FileTime> result = MavenProjectSupplier.changedSince(rootDir, checkTime, type);
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

    @Test
    void testChangedSinceFromTimeZero() {
        FileTime pomTime = lastModifiedTime(pomFile);
        FileTime fromTimeZero = changedTime(TIME_ZERO, LATEST);
        assertThat(fromTimeZero.compareTo(pomTime), is(greaterThanOrEqualTo(0)));
    }

    @Test
    void testChangedSinceClassFileNotChecked() {
        FileTime initial = changedTime(TIME_ZERO, LATEST);
        FileTime touched = touchFile(classFile);

        // We should not find the class change since the filter won't allow visiting the target dir
        Optional<FileTime> found = MavenProjectSupplier.changedSince(rootDir, initial, LATEST);
        assertThat(found.isPresent(), is(false));

        // Re-check that it is found without filter
        found = FileUtils.changedSince(rootDir, initial, d -> true, f -> true, LATEST);
        assertThat(found.isPresent(), is(true));
        assertThat(found.get(), is(touched));
    }

    @Test
    void testChangedSinceOneFileChanged() {
        FileTime initial = lastModified(pomFile);
        FileTime touched = touchFile(pomFile);
        assertThat(touched.compareTo(initial), is(greaterThan(0)));

        FileTime found = changedTime(initial, LATEST);
        assertThat(found.compareTo(initial), is(greaterThan(0)));
        assertThat(found, is(touched));
    }

    @Test
    void testChangedSinceTwoFilesChanged() {

        FileTime initialPom = lastModified(pomFile);
        FileTime touchedPom = touchFile(pomFile);
        assertThat(touchedPom.compareTo(initialPom), is(greaterThan(0)));

        FileTime initialJava = lastModified(javaFile);
        FileTime touchedJava = touchFile(javaFile);
        assertThat(touchedJava.compareTo(initialJava), is(greaterThan(0)));

        assertThat(touchedJava, is(not(touchedPom)));
        assertThat(touchedJava.compareTo(touchedPom), is(greaterThan(0)));

        // Check LATEST should find touchedJava since it was most recent

        FileTime found = changedTime(initialPom, LATEST);
        assertThat(found, is(touchedJava));

        // Check FIRST should find touchedPom since it was changed first, but this is
        // file system iteration order dependent so just make sure it is one of them

        found = changedTime(initialPom, FIRST);
        if (!found.equals(touchedPom) && !found.equals(touchedJava)) {
            fail("Expected either " + touchedPom + " or " + touchedJava + ", got " + found);
        }
    }
}
