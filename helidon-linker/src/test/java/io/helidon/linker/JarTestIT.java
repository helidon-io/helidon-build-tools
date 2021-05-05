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

import io.helidon.build.common.test.utils.ConfigurationParameterSource;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Integration test for class {@link Jar}.
 */
@Order(3)
class JarTestIT {

    @Tag("multi-release")
    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testMultiReleaseJar(String basedir) {
        Path multiRelease = Path.of(basedir).resolve("target/multi-release.jar");
        Jar jar = Jar.open(multiRelease, Runtime.Version.parse("11"));
        assertThat(jar.isMultiRelease(), is(true));
        assertThat(jar.moduleDescriptor(), notNullValue());
    }

    @Tag("mp")
    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    @Disabled
    void testSignedJar(String basedir) {
        Path signed = Path.of(basedir).resolve("target/quickstart-mp.jar");
        Jar jar = Jar.open(signed);
        assertThat(jar.isSigned(), is(true));
    }
}
