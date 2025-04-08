/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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
package io.helidon.build.maven.enforcer;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.helidon.build.common.logging.Log;

import org.junit.jupiter.api.Test;

import static io.helidon.build.common.test.utils.TestFiles.testResourcePath;
import static io.helidon.build.maven.enforcer.GitCommands.gitLog;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GitCommandTest {

    @Test
    void testGitLog() throws FileNotFoundException {
        int warnings = Log.warnings();
        Iterator<String> expected = List.of(
                "foo/bar", "bar/foo", "foo/foo4", "foo/foo3", "foo/foo2", "foo/foo1").iterator();
        InputStream is = new FileInputStream(testResourcePath(GitCommandTest.class, "git.txt").toFile());
        Map<String, Integer> files = gitLog(Path.of("/root"), is);

        for (Map.Entry<String, Integer> entry : files.entrySet()) {
            assertThat(entry.getKey(), is(expected.next()));
            assertThat(entry.getValue(), is(2000));
        }

        assertThat(expected.hasNext(), is(false));
        assertThat("Warning message must be logged", Log.warnings(), is(warnings + 1));
    }

    @Test
    void testWrongGitOperation() {
        String content = "0000-00-00" + System.lineSeparator() + "H";
        EnforcerException exception = assertThrows(EnforcerException.class,
                () -> gitLog(Path.of("/root"), new ByteArrayInputStream(content.getBytes())));
        assertThat(exception.getMessage(), is("Could not parse line H"));
    }

    @Test
    void testWrongDate() {
        String content = "000-00-00";
        EnforcerException exception = assertThrows(EnforcerException.class,
                () -> gitLog(Path.of("/root"), new ByteArrayInputStream(content.getBytes())));
        assertThat(exception.getMessage(), is("Failed to parse output, expecting date to be present"));
    }
}
