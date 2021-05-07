/*
 * Copyright (c) 2019, 2021 Oracle and/or its affiliates.
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
package io.helidon.build.linker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.helidon.build.common.Log;
import io.helidon.build.common.SystemLogWriter;
import io.helidon.build.common.test.utils.ConfigurationParameterSource;
import io.helidon.build.common.test.utils.TestLogLevel;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;

import static io.helidon.build.common.FileUtils.javaHome;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Integration test for class {@link ClassDataSharing}.
 */
@Order(2)
class ClassDataSharingTestIT {

    private static final Path JAVA_HOME = Path.of(javaHome());
    private static final String APP_CLASS = "org/jboss/weld/environment/deployment/discovery/BeanArchiveScanner";

    @BeforeAll
    static void setup() {
        if (TestLogLevel.isDebug()) {
            Log.writer(SystemLogWriter.create(Log.Level.DEBUG));
        }
    }

    @Tag("mp")
    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testQuickstartMp(String basedir) throws Exception {
        Path mainJar = Path.of(basedir).resolve("target/quickstart-mp.jar");
        Path archiveFile = Files.createTempFile("start", "jsa");
        String exitOnStarted = "!";
        ClassDataSharing cds = ClassDataSharing.builder()
                                               .jri(JAVA_HOME)
                                               .applicationJar(mainJar)
                                               .createArchive(false)
                                               .logOutput(true)
                                               .exitOnStartedValue(exitOnStarted)
                                               .build();
        assertThat(cds, is(not(nullValue())));
        assertThat(cds.classList(), is(not(nullValue())));
        assertThat(cds.classList(), is(not(empty())));
        assertThat(cds.applicationJar(), is(not(nullValue())));
        assertThat(cds.classListFile(), is(not(nullValue())));
        assertThat(cds.archiveFile(), is(nullValue()));

        if (Runtime.version().feature() > 9) {
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
                              .logOutput(true)
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
