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
package io.helidon.build.cli.impl;

import java.io.IOException;
import java.nio.file.Path;

import io.helidon.build.cli.harness.Config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.helidon.build.cli.impl.InitCommand.Flavor;
import static io.helidon.build.cli.impl.TestUtils.exec;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Class CommandTest.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CommandTest extends InitCommandBaseTest {

    @BeforeEach
    public void beforeEach() {
        super.beforeEach();
        flavor(Flavor.SE.toString());
    }

    @AfterEach
    public void afterEach() throws IOException {
        super.afterEach();
    }

    @AfterAll
    public static void afterAll() {
        MetadataCommandTest.stopMetadataAccess();
    }

    @Test
    @Order(1)
    public void testInit() throws Exception {
        generate();
        assertValid();
    }

    @Test
    @Order(2)
    public void testBuild() throws Exception {
        generate();
        assertValid();
        Path projectDir = projectDir();
        exec("build", "--project ", projectDir.toString());
        assertJarExists();
    }

    @Test
    @Order(3)
    public void testInfo() throws Exception {
        Config.userConfig().clearPlugins();
        initArguments();
        Path projectDir = projectDir();
        String result = exec("info", "--project ", projectDir.toString());
        assertThat(result, containsString("plugin"));
    }

    @Test
    @Order(4)
    public void testVersion() throws Exception {
        initArguments();
        Path projectDir = projectDir();
        String result = exec("version", "--project ", projectDir.toString());
        assertThat(result, containsString("version"));
        assertThat(result, containsString("helidon.version"));
    }
}
