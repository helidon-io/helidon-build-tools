/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
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
package io.helidon.build.maven.enforcer.nativeimage;

import java.util.LinkedList;
import java.util.List;

import io.helidon.build.common.maven.MavenVersion;
import io.helidon.build.maven.enforcer.RuleFailure;
import org.junit.jupiter.api.Test;

import static io.helidon.build.common.maven.MavenVersion.toMavenVersion;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Native-image enforcer rul tests.
 */
class NativeImageRuleTest {

    @Test
    void testWrongMatcherConfiguration() {
        VersionConfig config = createVersionConfig("dummy");
        List<RuleFailure> failures = new LinkedList<>();
        MavenVersion version = toMavenVersion("1.0.0");

        try {
            config.checkVersion(version, failures);
        } catch (EnforcerNativeImageException e) {
            assertThat(e.getMessage(), containsString("Invalid matcher"));
            return;
        }
        fail("Exception must have been thrown");
    }

    @Test
    void testGreaterThanRuleConfiguration() {
        VersionConfig config = createVersionConfig("greaterThan");
        List<RuleFailure> failures = new LinkedList<>();
        MavenVersion version = toMavenVersion("1.0.0");

        config.checkVersion(version, failures);
        assertThat(failures.size(), is(1));

        config.setVersion("2.0.0");
        config.checkVersion(version, failures);
        assertThat(failures.size(), is(2));

        config.setVersion("0.1.0");
        config.checkVersion(version, failures);
        assertThat(failures.size(), is(2));
    }

    @Test
    void testLessThanRuleConfiguration() {
        VersionConfig config = createVersionConfig("lessThan");
        List<RuleFailure> failures = new LinkedList<>();
        MavenVersion version = toMavenVersion("1.0.0");

        config.checkVersion(version, failures);
        assertThat(failures.size(), is(1));

        config.setVersion("2.0.0");
        config.checkVersion(version, failures);
        assertThat(failures.size(), is(1));

        config.setVersion("0.1.0");
        config.checkVersion(version, failures);
        assertThat(failures.size(), is(2));
    }

    @Test
    void testGreaterThanOrEqualToRuleConfiguration() {
        VersionConfig config = createVersionConfig("greaterThanOrEqualTo");
        List<RuleFailure> failures = new LinkedList<>();
        MavenVersion version = toMavenVersion("1.0.0");

        config.checkVersion(version, failures);
        assertThat(failures.size(), is(0));

        config.setVersion("2.0.0");
        config.checkVersion(version, failures);
        assertThat(failures.size(), is(1));

        config.setVersion("0.1.0");
        config.checkVersion(version, failures);
        assertThat(failures.size(), is(1));
    }

    @Test
    void testLessThanOrEqualToRuleConfiguration() {
        VersionConfig config = createVersionConfig("lessThanOrEqualTo");
        List<RuleFailure> failures = new LinkedList<>();
        MavenVersion version = toMavenVersion("1.0.0");

        config.checkVersion(version, failures);
        assertThat(failures.size(), is(0));

        config.setVersion("2.0.0");
        config.checkVersion(version, failures);
        assertThat(failures.size(), is(0));

        config.setVersion("0.1.0");
        config.checkVersion(version, failures);
        assertThat(failures.size(), is(1));
    }

    private VersionConfig createVersionConfig(String match) {
        VersionConfig config = new VersionConfig();
        config.setVersion("1.0.0");
        config.setMatcher(match);
        return config;
    }
}
