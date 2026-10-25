/*
 * Copyright (c) 2023, 2026 Oracle and/or its affiliates.
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.helidon.build.common.test.utils.ConfigurationParameterSource;
import io.helidon.build.common.test.utils.JUnitLauncher;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;

import static io.helidon.build.common.test.utils.LogMatchers.matches;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration test that verifies the projects under {@code src/it/projects}.
 */
@EnabledIfSystemProperty(named = JUnitLauncher.IDENTITY_PROP, matches = "true")
class GitIgnoreTestIT {

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test1(String basedir) throws IOException {
        Path baseDir = Path.of(basedir);
        String actual = Files.readString(baseDir.resolve("build.log"));
        assertThat(actual, matches("""
                ~...
                Excluding dir/dir6/file6
                Excluding dir/dir5/file5.txt
                Excluding dir/file.ico
                Excluding dir/dir7/file7.txt
                Excluding dir/dir3/file3
                Including dir/file.txt
                Excluding dir/dir2/file2.txt
                Excluding dir/dir4/file4
                Including dir/file.txt
                ~...
                """));
    }
}
