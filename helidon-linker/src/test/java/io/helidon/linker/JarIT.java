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

package io.helidon.linker;

import java.nio.file.Path;

import io.helidon.build.test.TestFiles;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test for class {@link Jar}.
 * Run as integration test so jars generated during phase "pre-integration-test"
 * are available.
 */
class JarIT {

    @Test
    @Disabled
    void testSignedJar() {
        Path signed = TestFiles.signedJar();
        Jar jar = Jar.open(signed, Runtime.version());
        assertThat(jar.isSigned(), is(true));
    }

    @Test
    void testMultiReleaseJar() {
        Path multiRelease = TestFiles.targetDir()
                .resolve("it")
                .resolve("projects")
                .resolve("multi-release")
                .resolve("target")
                .resolve("multi-release.jar");
        Jar jar = Jar.open(multiRelease, Runtime.Version.parse("11"));
        assertThat(jar.isMultiRelease(), is(true));
        assertThat(jar.moduleDescriptor(), notNullValue());
    }
}
