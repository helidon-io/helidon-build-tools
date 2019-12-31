/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.linker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import io.helidon.test.util.TestFiles;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit test for class {@link ClassDataSharing}.
 */
class ClassDataSharingTest {
    private static final Path JAVA_HOME = Paths.get(System.getProperty("java.home"));
    private static final String APP_CLASS = "org/jboss/weld/environment/deployment/discovery/BeanArchiveScanner";

    @Test
    void testQuickstartMp() throws Exception {
        Path mainJar = TestFiles.helidonMpJar();
        Path archiveFile = Files.createTempFile("start","jsa");
        String exitOnStarted = TestFiles.exitOnStartedValue();
        ClassDataSharing cds = ClassDataSharing.builder()
                                               .jri(JAVA_HOME)
                                               .applicationJar(mainJar)
                                               .createArchive(false)
                                               .logOutput(false)
                                               .exitOnStartedValue(exitOnStarted)
                                               .build();
        assertThat(cds, is(not(nullValue())));
        assertThat(cds.classList(), is(not(nullValue())));
        assertThat(cds.classList(), is(not(empty())));
        assertThat(cds.applicationJar(), is(not(nullValue())));
        assertThat(cds.classListFile(), is(not(nullValue())));
        assertThat(cds.archiveFile(), is(nullValue()));

        if (Runtime.version().major() > 9) {
            // Application classes should be included in CDS archive
            assertContains(cds.classList(), APP_CLASS);
        } else {
            assertDoesNotContain(cds.classList(), APP_CLASS);
        }
        cds = ClassDataSharing.builder()
                              .jri(JAVA_HOME)
                              .applicationJar(mainJar)
                              .classListFile(cds.classListFile())
                              .archiveFile(archiveFile)
                              .logOutput(false)
                              .exitOnStartedValue(exitOnStarted)
                              .build();

        Path archive = cds.archiveFile();
        assertThat(archive, is(not(nullValue())));
        assertThat(Files.exists(archive), is(true));
        assertThat(Files.isRegularFile(archive), is(true));
    }

    private static void assertContains(List<String> list, String value) {
        assertThat(list.indexOf(value), is(greaterThan(-1)));
    }

    private static void assertDoesNotContain(List<String> list, String value) {
        assertThat(list.indexOf(value), is(-1));
    }
}
