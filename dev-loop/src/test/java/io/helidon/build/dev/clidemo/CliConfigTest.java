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

package io.helidon.build.dev.clidemo;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.helidon.build.util.TestUtils.pathOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Class CliConfigTest.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CliConfigTest {

    static File configFile;

    @BeforeAll
    public static void configFile() {
        configFile = new File(Objects.requireNonNull(
                CliConfigTest.class.getClassLoader().getResource("helidon.properties")).getFile());
    }

    @Test
    @Order(1)
    public void testCliConfigLoad1() {
        CliConfig config = new CliConfig(configFile);
        List<String> features = config.listFeatures();
        features.forEach(f -> {
            List<CliConfig.Dependency> deps = config.featureDeps(f);
            System.out.print(f + " -> ");
            deps.forEach(System.out::println);
        });
    }

    @Test
    @Order(2)
    public void testCliConfigLoadStore1() {
        CliConfig config = new CliConfig(configFile);
        config.projectDir(Path.of("/usr/tmp"));
        assertThat(pathOf(config.projectDir().get()), is("/usr/tmp"));
        config.store();
    }

    @Test
    @Order(3)
    public void testCliConfigLoadStore2() {
        CliConfig config = new CliConfig(configFile);
        assertThat(pathOf(config.projectDir().get()), is("/usr/tmp"));
        config.clearProjectDir();
        config.store();
    }
}
