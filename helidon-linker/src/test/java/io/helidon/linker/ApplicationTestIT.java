/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
import io.helidon.linker.util.JavaRuntime;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Integration test for class {@link Application}.
 */
@Order(1)
class ApplicationTestIT {

    @Tag("mp")
    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void testHelidonVersion(String basedir) {
        Path mainJar = Path.of(basedir).resolve("target/quickstart-mp.jar");
        Application app = Application.create(JavaRuntime.current(true), mainJar);
        String version = app.helidonVersion();
        assertThat(version, is(notNullValue()));
        assertThat(version, is(not("0.0.0")));
    }
}
