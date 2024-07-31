/*
 * Copyright (c) 2021, 2024 Oracle and/or its affiliates.
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
package io.helidon.build.maven.cache;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import io.helidon.build.common.test.utils.BuildLog;
import io.helidon.build.common.test.utils.ConfigurationParameterSource;
import io.helidon.build.common.test.utils.JUnitLauncher;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;

import static io.helidon.build.common.test.utils.BuildLog.assertDiffs;
import static io.helidon.build.common.test.utils.FileMatchers.fileExists;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Integration test that verifies the projects under {@code src/it/projects}.
 */
@EnabledIfSystemProperty(named = JUnitLauncher.IDENTITY_PROP, matches = "true")
class ProjectsTestIT {

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test1(String basedir) throws IOException {
        BuildLog buildLog = new BuildLog(new File(basedir, "build.log"));
        int index = buildLog.indexOf("BUILD SUCCESS", 0);
        assertThat(index > 0, is(true));
        index = buildLog.indexOf("BUILD SUCCESS", index);
        assertThat(index > 0, is(true));
        List<String> diffs = buildLog.diff(new File(basedir, "expected.log"), index);
        assertDiffs(diffs);
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test2(String basedir) throws IOException {
        BuildLog buildLog = new BuildLog(new File(basedir, "build.log"));
        int index0 = buildLog.indexOf("BUILD SUCCESS", 0);
        assertThat(index0 > 0, is(true));

        int index1 = buildLog.indexOf("Cache is disabled", index0);
        assertThat(index1 > 0, is(true));

        int index2 = buildLog.indexOf("Downstream state(s) not available, state is ignored", index1);
        assertThat(index2 > 0, is(true));
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test3(String basedir) {
        Path apidocs = Path.of(basedir).resolve("target/apidocs");
        Path greetings = apidocs.resolve("io/helidon/build/cache/test/Greeting.html");
        assertThat(greetings, fileExists());
    }
}
